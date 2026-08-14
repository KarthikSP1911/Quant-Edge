package com.quantedge.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.quantedge.backend.aop.Auditable;
import com.quantedge.backend.dto.response.CheckoutSessionResponse;
import com.quantedge.backend.entity.AuditLog;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.entity.WalletTransaction;
import com.quantedge.backend.enums.WalletTransactionStatus;
import com.quantedge.backend.exception.InvalidWalletRequestException;
import com.quantedge.backend.external.StripeCheckoutClient;
import com.quantedge.backend.external.dto.StripeCheckoutSessionResult;
import com.quantedge.backend.repository.AuditLogRepository;
import com.quantedge.backend.repository.UserRepository;
import com.quantedge.backend.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Real-money -> virtual-balance wallet top-up. {@link #createCheckoutSession} is a normal
 * authenticated write; {@link #creditWallet} is called only from the Stripe webhook, which has no
 * {@code SecurityContext}, so it can't use {@link Auditable} (that reads the current principal) -
 * it writes its own {@link AuditLog} row using the user resolved from the {@link WalletTransaction}
 * instead.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final StripeCheckoutClient stripeCheckoutClient;

    @Value("${quantedge.wallet.exchange-rate}")
    private BigDecimal exchangeRate;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Auditable(action = "WALLET_TOPUP_INITIATED", entityType = "WALLET_TRANSACTION", entityId = "#result.sessionId()")
    @Transactional
    public CheckoutSessionResponse createCheckoutSession(User user, BigDecimal amountUsd) {
        if (amountUsd == null || amountUsd.compareTo(BigDecimal.ONE) < 0) {
            throw new InvalidWalletRequestException("Top-up amount must be at least $1.00");
        }

        long amountUsdCents = amountUsd
                .setScale(2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .longValueExact();
        BigDecimal creditsAwarded = amountUsd.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);

        String successUrl = frontendUrl + "/wallet?status=success";
        String cancelUrl = frontendUrl + "/wallet?status=cancelled";
        StripeCheckoutSessionResult session = stripeCheckoutClient.createCheckoutSession(
                user.getId().toString(), amountUsdCents, successUrl, cancelUrl);

        walletTransactionRepository.save(WalletTransaction.builder()
                .user(user)
                .stripeSessionId(session.sessionId())
                .amountUsdCents(amountUsdCents)
                .creditsAwarded(creditsAwarded)
                .status(WalletTransactionStatus.PENDING)
                .build());

        return new CheckoutSessionResponse(session.checkoutUrl(), session.sessionId());
    }

    /**
     * Credits {@code user.balance} for a confirmed Stripe payment. Idempotent: the repository
     * update only transitions PENDING -> COMPLETED, so a redelivered webhook event for a session
     * that's already COMPLETED updates zero rows and this method credits nothing on the retry.
     */
    @Transactional
    public void creditWallet(String stripeSessionId, String paymentIntentId) {
        int updated = walletTransactionRepository.markCompletedIfPending(
                stripeSessionId, paymentIntentId, WalletTransactionStatus.COMPLETED);

        if (updated == 0) {
            log.info(
                    "Ignoring Stripe webhook for session {} - not PENDING (already processed or unknown)",
                    stripeSessionId);
            return;
        }

        WalletTransaction transaction = walletTransactionRepository
                .findByStripeSessionId(stripeSessionId)
                .orElseThrow(() -> new IllegalStateException(
                        "wallet_transactions row disappeared after successful update for session " + stripeSessionId));

        User user = transaction.getUser();
        user.setBalance(user.getBalance().add(transaction.getCreditsAwarded()));
        userRepository.save(user);

        // Manual audit log: this runs from the Stripe webhook, which has no authenticated
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
