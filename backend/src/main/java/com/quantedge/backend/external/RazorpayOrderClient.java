package com.quantedge.backend.external;

import com.quantedge.backend.external.dto.RazorpayOrderResult;

/**
 * Thin wrapper around razorpay-java's {@code Orders.create(...)} call, so {@link
 * com.quantedge.backend.service.WalletService} can be unit-tested by mocking this interface
 * instead of mocking the SDK directly.
 */
public interface RazorpayOrderClient {

    RazorpayOrderResult createOrder(String userId, long amountUsdCents);
}
