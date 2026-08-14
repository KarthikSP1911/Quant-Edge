package com.quantedge.backend.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Sets the stripe-java SDK's static API key holder at startup. The SDK is a static-client design
 * (every {@code Session.create(...)} call reads {@link Stripe#apiKey} internally), so there's no
 * bean to inject beyond this one-time assignment.
 */
@Configuration
public class StripeConfig {

    @Value("${stripe.secret-key}")
    private String secretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }
}
