package com.quantedge.backend.external;

import com.quantedge.backend.exception.ExternalApiException;
import com.quantedge.backend.external.dto.RazorpayOrderResult;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RazorpayOrderClientImpl implements RazorpayOrderClient {

    private static final Logger log = LoggerFactory.getLogger(RazorpayOrderClientImpl.class);

    private final RazorpayClient razorpayClient;

    @Override
    public RazorpayOrderResult createOrder(String userId, long amountUsdCents) {
        JSONObject options = new JSONObject();
        options.put("amount", amountUsdCents);
        options.put("currency", "USD");
        // Razorpay caps receipt at 56 chars, so the full userId (a 36-char UUID) doesn't fit
        // alongside a prefix and timestamp - it's only a merchant-side reference for uniqueness,
        // the actual userId lookup already goes through the notes field below.
        String userIdPrefix = userId.substring(0, Math.min(8, userId.length()));
        options.put("receipt", "wallet-topup-" + userIdPrefix + "-" + System.currentTimeMillis());
        JSONObject notes = new JSONObject();
        notes.put("userId", userId);
        options.put("notes", notes);

        try {
            Order order = razorpayClient.orders.create(options);
            return new RazorpayOrderResult(
                    order.get("id"), Long.parseLong(order.get("amount").toString()), order.get("currency"));
        } catch (RazorpayException ex) {
            log.warn("Razorpay order creation failed for userId={}", userId, ex);
            throw new ExternalApiException("Failed to create Razorpay order", ex);
        }
    }
}
