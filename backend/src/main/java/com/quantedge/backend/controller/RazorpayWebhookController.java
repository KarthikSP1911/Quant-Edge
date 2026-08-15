package com.quantedge.backend.controller;

import com.quantedge.backend.service.WalletService;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reconciliation backup for {@link WalletController#verifyPayment}: if the browser closes before
 * the verify-payment call fires, this webhook still credits the wallet once Razorpay confirms the
 * capture. Not authenticated with a QuantEdge JWT (Razorpay has no session) - trust is established
 * purely via {@code X-Razorpay-Signature} verification against the raw request body, so the body
 * must arrive unparsed. {@link WalletService#creditWallet} is idempotent, so it's safe for both
 * this webhook and the verify-payment endpoint to race for the same order.
 *
 * <p>{@code razorpay.webhook-secret} is optional: Razorpay Test Mode only hands out a key ID and
 * key secret up front, and creating a webhook (to get the secret) requires a public HTTPS URL
 * pointed at this endpoint, which most local dev setups don't have. When the secret is unset,
 * this endpoint refuses all requests rather than skip verification - the primary verify-payment
 * flow is unaffected either way.
 */
@RestController
@RequestMapping("/api/webhooks/razorpay")
@RequiredArgsConstructor
@Slf4j
public class RazorpayWebhookController {

    private final WalletService walletService;

    @Value("${razorpay.webhook-secret:}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload, @RequestHeader("X-Razorpay-Signature") String signature) {
        if (!StringUtils.hasText(webhookSecret)) {
            log.warn("Rejected Razorpay webhook: RAZORPAY_WEBHOOK_SECRET is not configured, "
                    + "so signatures can't be verified. The verify-payment endpoint still "
                    + "credits wallets without this backup.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        try {
            if (!Utils.verifyWebhookSignature(payload, signature, webhookSecret)) {
                log.warn("Rejected Razorpay webhook with invalid signature");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        } catch (RazorpayException ex) {
            log.warn("Rejected Razorpay webhook with invalid signature", ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        JSONObject event = new JSONObject(payload);
        if ("payment.captured".equals(event.optString("event"))) {
            JSONObject payment =
                    event.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
            walletService.creditWallet(payment.getString("order_id"), payment.getString("id"));
        }

        return ResponseEntity.ok().build();
    }
}
