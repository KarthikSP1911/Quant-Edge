package com.quantedge.backend.external;

import com.quantedge.backend.external.dto.StripeCheckoutSessionResult;

/**
 * Thin wrapper around stripe-java's static {@code Session.create(...)} call, so {@link
 * com.quantedge.backend.service.WalletService} can be unit-tested by mocking this interface
 * instead of mocking a static SDK method.
 */
public interface StripeCheckoutClient {

    StripeCheckoutSessionResult createCheckoutSession(
            String userId, long amountUsdCents, String successUrl, String cancelUrl);
}
