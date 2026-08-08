package com.quantedge.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.quantedge.backend.cache.ChartCache;
import com.quantedge.backend.dto.response.StockDetailResponse;
import com.quantedge.backend.entity.Company;
import com.quantedge.backend.external.TwelveDataClient;
import com.quantedge.backend.external.dto.FinnhubQuoteResponse;
import com.quantedge.backend.external.dto.TwelveDataTimeSeriesResponse;
import com.quantedge.backend.mapper.CompanyMapper;
import com.quantedge.backend.mapper.MarketDataMapper;
import com.quantedge.backend.repository.CompanyRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockDetailServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private QuoteService quoteService;

    @Mock
    private ChartCache chartCache;

    @Mock
    private TwelveDataClient twelveDataClient;

    private StockDetailService service;

    private final Company apple = Company.builder()
            .symbol("AAPL")
            .name("Apple Inc.")
            .sector("Technology")
            .industry("Consumer Electronics")
            .exchange("NASDAQ")
            .build();

    private final FinnhubQuoteResponse quote = new FinnhubQuoteResponse(190.0, 195.0, 185.0, 188.0, 187.0, 1000L);

    private final TwelveDataTimeSeriesResponse timeSeries = new TwelveDataTimeSeriesResponse(
            new TwelveDataTimeSeriesResponse.Meta("AAPL", "1day", "NASDAQ", "USD"),
            List.of(new TwelveDataTimeSeriesResponse.Candle(
                    "2026-08-06", "188.0", "195.0", "185.0", "190.0", "1000000")),
            "ok",
            null);

    @BeforeEach
    void setUp() {
        service = new StockDetailService(
                companyRepository,
                new CompanyMapper(),
                new MarketDataMapper(),
                quoteService,
                chartCache,
                twelveDataClient);
    }

    @Test
    void returnsEmptyWhenCompanyNotFound() {
        when(companyRepository.findBySymbol("NOPE")).thenReturn(Optional.empty());

        assertThat(service.getStockDetail("NOPE", "1day", 30)).isEmpty();
        verifyNoInteractions(quoteService, chartCache, twelveDataClient);
    }

    @Test
    void onCacheHitUsesCachedCandlesWithoutCallingTwelveData() {
        when(companyRepository.findBySymbol("AAPL")).thenReturn(Optional.of(apple));
        when(quoteService.getQuote("AAPL")).thenReturn(quote);
        when(chartCache.get("AAPL", "1day", TwelveDataTimeSeriesResponse.class)).thenReturn(Optional.of(timeSeries));

        Optional<StockDetailResponse> result = service.getStockDetail("AAPL", "1day", 30);

        assertThat(result).isPresent();
        assertThat(result.get().quote().currentPrice()).isEqualTo(190.0);
        assertThat(result.get().candles()).hasSize(1);
        verifyNoInteractions(twelveDataClient);
    }

    @Test
    void onCacheMissFetchesFromTwelveDataAndPopulatesCache() {
        when(companyRepository.findBySymbol("AAPL")).thenReturn(Optional.of(apple));
        when(quoteService.getQuote("AAPL")).thenReturn(quote);
        when(chartCache.get("AAPL", "1day", TwelveDataTimeSeriesResponse.class)).thenReturn(Optional.empty());
        when(twelveDataClient.getTimeSeries("AAPL", "1day", 30)).thenReturn(timeSeries);

        Optional<StockDetailResponse> result = service.getStockDetail("AAPL", "1day", 30);

        assertThat(result).isPresent();
        verify(chartCache).put(eq("AAPL"), eq("1day"), eq(timeSeries), any());
    }
}
