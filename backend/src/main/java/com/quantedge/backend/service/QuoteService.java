package com.quantedge.backend.service;

import com.quantedge.backend.cache.PriceCache;
import com.quantedge.backend.external.FinnhubClient;
import com.quantedge.backend.external.dto.FinnhubQuoteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Cache-aside real-time quote lookup, shared by the stock detail query and market order flow. */
@Service
@RequiredArgsConstructor
public class QuoteService {

    private final PriceCache priceCache;
    private final FinnhubClient finnhubClient;

    public FinnhubQuoteResponse getQuote(String symbol) {
        return priceCache.get(symbol, FinnhubQuoteResponse.class).orElseGet(() -> {
            FinnhubQuoteResponse fetched = finnhubClient.getQuote(symbol);
            priceCache.put(symbol, fetched);
            return fetched;
        });
    }
}
