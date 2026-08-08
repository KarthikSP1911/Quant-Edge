package com.quantedge.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Optional;

import com.quantedge.backend.cache.PriceCache;
import com.quantedge.backend.external.FinnhubClient;
import com.quantedge.backend.external.dto.FinnhubQuoteResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuoteServiceTest {

    @Mock
    private PriceCache priceCache;

    @Mock
    private FinnhubClient finnhubClient;

    private QuoteService quoteService;

    @Test
    void returnsCachedQuoteWithoutCallingFinnhub() {
        quoteService = new QuoteService(priceCache, finnhubClient);
        FinnhubQuoteResponse quote = new FinnhubQuoteResponse(190.0, 195.0, 185.0, 188.0, 187.0, 1000L);
        when(priceCache.get("AAPL", FinnhubQuoteResponse.class)).thenReturn(Optional.of(quote));

        assertThat(quoteService.getQuote("AAPL")).isEqualTo(quote);
        verifyNoInteractions(finnhubClient);
    }

    @Test
    void onCacheMissFetchesFromFinnhubAndPopulatesCache() {
        quoteService = new QuoteService(priceCache, finnhubClient);
        FinnhubQuoteResponse quote = new FinnhubQuoteResponse(190.0, 195.0, 185.0, 188.0, 187.0, 1000L);
        when(priceCache.get("AAPL", FinnhubQuoteResponse.class)).thenReturn(Optional.empty());
        when(finnhubClient.getQuote("AAPL")).thenReturn(quote);

        assertThat(quoteService.getQuote("AAPL")).isEqualTo(quote);
        verify(priceCache).put("AAPL", quote);
    }
}
