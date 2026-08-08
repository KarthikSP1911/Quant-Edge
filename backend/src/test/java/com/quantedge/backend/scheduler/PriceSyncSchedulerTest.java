package com.quantedge.backend.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.quantedge.backend.cache.PriceCache;
import com.quantedge.backend.entity.Company;
import com.quantedge.backend.exception.ExternalApiException;
import com.quantedge.backend.external.FinnhubClient;
import com.quantedge.backend.external.dto.FinnhubQuoteResponse;
import com.quantedge.backend.kafka.producer.StockPriceProducer;
import com.quantedge.backend.repository.CompanyRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PriceSyncSchedulerTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private FinnhubClient finnhubClient;

    @Mock
    private PriceCache priceCache;

    @Mock
    private StockPriceProducer stockPriceProducer;

    private PriceSyncScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new PriceSyncScheduler(companyRepository, finnhubClient, priceCache, stockPriceProducer, 0L);
    }

    private Company company(String symbol) {
        return Company.builder().symbol(symbol).build();
    }

    @Test
    void syncsQuoteAndCachesItForEverySeededCompany() {
        when(companyRepository.findAll()).thenReturn(List.of(company("AAPL"), company("MSFT"), company("GOOGL")));
        FinnhubQuoteResponse quote = new FinnhubQuoteResponse(100.0, 105.0, 95.0, 98.0, 97.0, 123L);
        when(finnhubClient.getQuote(any())).thenReturn(quote);

        scheduler.syncPrices();

        verify(finnhubClient, times(1)).getQuote("AAPL");
        verify(finnhubClient, times(1)).getQuote("MSFT");
        verify(finnhubClient, times(1)).getQuote("GOOGL");
        verify(priceCache, times(1)).put(eq("AAPL"), eq(quote));
        verify(priceCache, times(1)).put(eq("MSFT"), eq(quote));
        verify(priceCache, times(1)).put(eq("GOOGL"), eq(quote));
    }

    @Test
    void skipsFailingSymbolAndContinuesTheRestOfTheBatch() {
        when(companyRepository.findAll()).thenReturn(List.of(company("AAPL"), company("BAD"), company("MSFT")));
        FinnhubQuoteResponse quote = new FinnhubQuoteResponse(100.0, 105.0, 95.0, 98.0, 97.0, 123L);
        when(finnhubClient.getQuote("AAPL")).thenReturn(quote);
        when(finnhubClient.getQuote("BAD")).thenThrow(new ExternalApiException("boom"));
        when(finnhubClient.getQuote("MSFT")).thenReturn(quote);

        scheduler.syncPrices();

        verify(priceCache, times(1)).put(eq("AAPL"), any());
        verify(priceCache, never()).put(eq("BAD"), any());
        verify(priceCache, times(1)).put(eq("MSFT"), any());
    }
}
