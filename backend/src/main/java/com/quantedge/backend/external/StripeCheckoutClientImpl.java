package com.quantedge.backend.external;

import com.quantedge.backend.exception.ExternalApiException;
import com.quantedge.backend.external.dto.StripeCheckoutSessionResult;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StripeCheckoutClientImpl implements StripeCheckoutClient {

    private static final Logger log = LoggerFactory.getLogger(StripeCheckoutClientImpl.class);

    @Override
    public StripeCheckoutSessionResult createCheckoutSession(
            String userId, long amountUsdCents, String successUrl, String cancelUrl) {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setClientReferenceId(userId)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("usd")
                                .setUnitAmount(amountUsdCents)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("QuantEdge wallet top-up")
                                        .build())
                                .build())
                        .build())
                .build();

        try {
            Session session = Session.create(params);
            return new StripeCheckoutSessionResult(session.getId(), session.getUrl());
        } catch (StripeException ex) {
            log.warn("Stripe checkout session creation failed for userId={}", userId, ex);
            throw new ExternalApiException("Failed to create Stripe checkout session", ex);
        }
    }
}
