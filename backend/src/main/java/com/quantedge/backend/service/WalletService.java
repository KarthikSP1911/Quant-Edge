package com.quantedge.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.quantedge.backend.aop.Auditable;
import com.quantedge.backend.dto.response.RazorpayOrderResponse;
import com.quantedge.backend.entity.AuditLog;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.entity.WalletTransaction;
import com.quantedge.backend.enums.WalletTransactionStatus;
import com.quantedge.backend.exception.InvalidWalletRequestException;
import com.quantedge.backend.external.RazorpayOrderClient;
import com.quantedge.backend.external.dto.RazorpayOrderResult;
import com.quantedge.backend.repository.AuditLogRepository;
import com.quantedge.backend.repository.UserRepository;
import com.quantedge.backend.repository.WalletTransactionRepository;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Real-money -> virtual-balance wallet top-up. {@link #createCheckoutSession} is a normal
 * authenticated write; {@link #creditWallet} is called from both the authenticated
 * verify-payment endpoint and the unauthenticated Razorpay webhook (which has no {@code
 * SecurityContext}), so it can't use {@link Auditable} (that reads the current principal) - it
 * writes its own {@link AuditLog} row using the user resolved from the {@link WalletTransaction}
 * instead.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final RazorpayOrderClient razorpayOrderClient;

    @Value("${quantedge.wallet.exchange-rate}")
    private BigDecimal exchangeRate;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Auditable(action = "WALLET_TOPUP_INITIATED", entityType = "WALLET_TRANSACTION", entityId = "#result.orderId()")
    @Transactional
    public RazorpayOrderResponse createCheckoutSession(User user, BigDecimal amountUsd) {
        if (amountUsd == null || amountUsd.compareTo(BigDecimal.ONE) < 0) {
            throw new InvalidWalletRequestException("Top-up amount must be at least $1.00");
        }

        long amountUsdCents = amountUsd
                .setScale(2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .longValueExact();
        BigDecimal creditsAwarded = amountUsd.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);

        RazorpayOrderResult order = razorpayOrderClient.createOrder(user.getId().toString(), amountUsdCents);

        walletTransactionRepository.save(WalletTransaction.builder()
                .user(user)
                .razorpayOrderId(order.orderId())
                .amountUsdCents(amountUsdCents)
                .creditsAwarded(creditsAwarded)
                .status(WalletTransactionStatus.PENDING)
                .build());

        return new RazorpayOrderResponse(order.orderId(), order.amount(), order.currency());
    }

    /**
     * Verifies the HMAC-SHA256 signature Razorpay's Checkout.js modal hands back to the frontend
     * after a successful payment, then credits the wallet. Called from the authenticated
     * verify-payment endpoint.
     */
    @Transactional
    public void verifyAndCreditWallet(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        JSONObject attributes = new JSONObject();
        attributes.put("razorpay_order_id", razorpayOrderId);
        attributes.put("razorpay_payment_id", razorpayPaymentId);
        attributes.put("razorpay_signature", razorpaySignature);

        try {
            Utils.verifyPaymentSignature(attributes, keySecret);
        } catch (RazorpayException ex) {
            log.warn("Rejected Razorpay payment with invalid signature for order {}", razorpayOrderId, ex);
            throw new InvalidWalletRequestException("Payment signature verification failed");
        }

        creditWallet(razorpayOrderId, razorpayPaymentId);
    }

    /**
     * Credits {@code user.balance} for a confirmed Razorpay payment. Idempotent: the repository
     * update only transitions PENDING -> COMPLETED, so a second call for an order that's already
     * COMPLETED (e.g. the verify-payment call already ran, and the webhook fires afterward, or
     * vice versa) updates zero rows and this method credits nothing on the retry.
     */
    @Transactional
    public void creditWallet(String razorpayOrderId, String razorpayPaymentId) {
        int updated = walletTransactionRepository.markCompletedIfPending(
                razorpayOrderId, razorpayPaymentId, WalletTransactionStatus.COMPLETED);

        if (updated == 0) {
            log.info(
                    "Ignoring Razorpay credit for order {} - not PENDING (already processed or unknown)",
                    razorpayOrderId);
            return;
        }

        WalletTransaction transaction = walletTransactionRepository
                .findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new IllegalStateException(
                        "wallet_transactions row disappeared after successful update for order " + razorpayOrderId));

        User user = transaction.getUser();
        user.setBalance(user.getBalance().add(transaction.getCreditsAwarded()));
        userRepository.save(user);

        // Manual audit log: this may run from the Razorpay webhook, which has no authenticated
        // SecurityContext for AuditAspect/@Auditable to read the user from.
        auditLogRepository.save(AuditLog.builder()
                .user(user)
                .action("WALLET_TOPUP_COMPLETED")
                .entityType("WALLET_TRANSACTION")
                .entityId(transaction.getId().toString())
                .build());
    }

    public List<WalletTransaction> getTransactionHistory(User user) {
        return walletTransactionRepository.findByUserOrderByCreatedAtDesc(user);
    }
}
