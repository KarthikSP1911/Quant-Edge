package com.quantedge.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import com.quantedge.backend.cache.ChartCache;
import com.quantedge.backend.cache.FundamentalsCache;
import com.quantedge.backend.dto.response.CandleResponse;
import com.quantedge.backend.dto.response.ComparisonEntryResponse;
import com.quantedge.backend.dto.response.StockComparisonResponse;
import com.quantedge.backend.entity.Company;
import com.quantedge.backend.exception.CompanyNotFoundException;
import com.quantedge.backend.exception.InvalidComparisonRequestException;
import com.quantedge.backend.external.FinnhubClient;
import com.quantedge.backend.external.TwelveDataClient;
import com.quantedge.backend.external.dto.FinnhubBasicFinancialsResponse;
import com.quantedge.backend.external.dto.FinnhubQuoteResponse;
import com.quantedge.backend.external.dto.TwelveDataTimeSeriesResponse;
import com.quantedge.backend.mapper.CompanyMapper;
import com.quantedge.backend.mapper.MarketDataMapper;
import com.quantedge.backend.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockComparisonServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private QuoteService quoteService;

    @Mock
    private ChartCache chartCache;

    @Mock
    private FundamentalsCache fundamentalsCache;

    @Mock
    private TwelveDataClient twelveDataClient;

    @Mock
    private FinnhubClient finnhubClient;

    private StockComparisonService service;

    @BeforeEach
    void setUp() {
        service = new StockComparisonService(
                companyRepository,
                new CompanyMapper(),
                new MarketDataMapper(),
                quoteService,
                chartCache,
                fundamentalsCache,
                twelveDataClient,
                finnhubClient);
    }

    private static Company company(String symbol) {
        return Company.builder()
                .symbol(symbol)
                .name(symbol + " Inc.")
                .sector("Technology")
                .industry("Software")
                .exchange("NASDAQ")
                .build();
    }

    private static FinnhubQuoteResponse quote(double price) {
        return new FinnhubQuoteResponse(price, price, price, price, price, 1000L);
    }

    /** Twelve Data returns newest-first; these fixtures mirror that so ordering is really tested. */
    private static TwelveDataTimeSeriesResponse series(String symbol, String... datetimes) {
        List<TwelveDataTimeSeriesResponse.Candle> candles = java.util.Arrays.stream(datetimes)
                .map(dt -> new TwelveDataTimeSeriesResponse.Candle(dt, "10.0", "12.0", "9.0", "11.0", "100"))
                .toList();
        return new TwelveDataTimeSeriesResponse(
                new TwelveDataTimeSeriesResponse.Meta(symbol, "1day", "NASDAQ", "USD"), candles, "ok", null);
    }

    private void stubSymbol(String symbol, TwelveDataTimeSeriesResponse series) {
        when(companyRepository.findBySymbol(symbol)).thenReturn(Optional.of(company(symbol)));
        when(quoteService.getQuote(symbol)).thenReturn(quote(100.0));
        when(chartCache.get(symbol, "1day", TwelveDataTimeSeriesResponse.class)).thenReturn(Optional.of(series));
        when(fundamentalsCache.get(symbol, FinnhubBasicFinancialsResponse.class))
                .thenReturn(Optional.of(new FinnhubBasicFinancialsResponse(
                        new FinnhubBasicFinancialsResponse.Metric(3.0e12, 30.5, 250.0, 150.0))));
    }

    @Test
    void rejectsFewerThanTwoSymbols() {
        assertThatThrownBy(() -> service.compare(List.of("AAPL"), "1day", 90))
                .isInstanceOf(InvalidComparisonRequestException.class);
        verifyNoInteractions(companyRepository, twelveDataClient, finnhubClient);
    }

    @Test
    void rejectsMoreThanThreeSymbols() {
        assertThatThrownBy(() -> service.compare(List.of("AAPL", "MSFT", "GOOGL", "AMZN"), "1day", 90))
                .isInstanceOf(InvalidComparisonRequestException.class);
        verifyNoInteractions(companyRepository, twelveDataClient, finnhubClient);
    }

    @Test
    void rejectsDuplicateSymbolsThatCollapseBelowTheMinimum() {
        assertThatThrownBy(() -> service.compare(List.of("AAPL", "aapl"), "1day", 90))
                .isInstanceOf(InvalidComparisonRequestException.class);
    }

    @Test
    void rejectsUnknownSymbol() {
        when(companyRepository.findBySymbol("AAPL")).thenReturn(Optional.of(company("AAPL")));
        when(companyRepository.findBySymbol("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.compare(List.of("AAPL", "NOPE"), "1day", 90))
                .isInstanceOf(CompanyNotFoundException.class);
    }

    @Test
    void onCacheHitServesComparisonWithoutCallingAnyExternalApi() {
        stubSymbol("AAPL", series("AAPL", "2026-08-07", "2026-08-06"));
        stubSymbol("MSFT", series("MSFT", "2026-08-07", "2026-08-06"));

        StockComparisonResponse result = service.compare(List.of("AAPL", "MSFT"), "1day", 90);

        assertThat(result.entries()).hasSize(2);
        verifyNoInteractions(twelveDataClient, finnhubClient);
    }

    @Test
    void preservesRequestedSymbolOrder() {
        stubSymbol("MSFT", series("MSFT", "2026-08-06"));
        stubSymbol("AAPL", series("AAPL", "2026-08-06"));

        StockComparisonResponse result = service.compare(List.of("MSFT", "AAPL"), "1day", 90);

        assertThat(result.entries())
                .extracting(entry -> entry.company().symbol())
                .containsExactly("MSFT", "AAPL");
    }

    @Test
    void lowercaseSymbolsAreNormalizedBeforeLookup() {
        stubSymbol("AAPL", series("AAPL", "2026-08-06"));
        stubSymbol("MSFT", series("MSFT", "2026-08-06"));

        StockComparisonResponse result = service.compare(List.of(" aapl ", "msft"), "1day", 90);

        assertThat(result.entries())
                .extracting(entry -> entry.company().symbol())
                .containsExactly("AAPL", "MSFT");
    }

    @Test
    void alignsCandlesToTheSharedAxisAndSortsThemOldestFirst() {
        // AAPL has an extra bar (08-08) MSFT is missing; the overlay must drop it from both.
        stubSymbol("AAPL", series("AAPL", "2026-08-08", "2026-08-07", "2026-08-06"));
        stubSymbol("MSFT", series("MSFT", "2026-08-07", "2026-08-06"));

        StockComparisonResponse result = service.compare(List.of("AAPL", "MSFT"), "1day", 90);

        for (ComparisonEntryResponse entry : result.entries()) {
            assertThat(entry.candles())
                    .extracting(CandleResponse::datetime)
                    .containsExactly("2026-08-06", "2026-08-07");
        }
    }

    @Test
    void onCacheMissFetchesCandlesAndFundamentalsOncePerSymbolAndPopulatesCaches() {
        TwelveDataTimeSeriesResponse aapl = series("AAPL", "2026-08-06");
        TwelveDataTimeSeriesResponse msft = series("MSFT", "2026-08-06");

        when(companyRepository.findBySymbol("AAPL")).thenReturn(Optional.of(company("AAPL")));
        when(companyRepository.findBySymbol("MSFT")).thenReturn(Optional.of(company("MSFT")));
        when(quoteService.getQuote(anyString())).thenReturn(quote(100.0));
        when(chartCache.get(anyString(), eq("1day"), eq(TwelveDataTimeSeriesResponse.class)))
                .thenReturn(Optional.empty());
        when(fundamentalsCache.get(anyString(), eq(FinnhubBasicFinancialsResponse.class)))
                .thenReturn(Optional.empty());
        when(twelveDataClient.getTimeSeries("AAPL", "1day", 90)).thenReturn(aapl);
        when(twelveDataClient.getTimeSeries("MSFT", "1day", 90)).thenReturn(msft);
        when(finnhubClient.getBasicFinancials(anyString()))
                .thenReturn(new FinnhubBasicFinancialsResponse(
                        new FinnhubBasicFinancialsResponse.Metric(1.0e12, 20.0, 200.0, 100.0)));

        service.compare(List.of("AAPL", "MSFT"), "1day", 90);

        // Exactly one call per symbol - a 3-way comparison must not fan out beyond its symbol count.
        verify(twelveDataClient, times(1)).getTimeSeries("AAPL", "1day", 90);
        verify(twelveDataClient, times(1)).getTimeSeries("MSFT", "1day", 90);
        verify(finnhubClient, times(1)).getBasicFinancials("AAPL");
        verify(finnhubClient, times(1)).getBasicFinancials("MSFT");
        verify(chartCache).put(eq("AAPL"), eq("1day"), eq(aapl), any());
        verify(fundamentalsCache).put(eq("AAPL"), any());
    }

    @Test
    void missingMetricsSurfaceAsNullRatherThanZero() {
        stubSymbol("AAPL", series("AAPL", "2026-08-06"));
        when(companyRepository.findBySymbol("MSFT")).thenReturn(Optional.of(company("MSFT")));
        when(quoteService.getQuote("MSFT")).thenReturn(quote(100.0));
        when(chartCache.get("MSFT", "1day", TwelveDataTimeSeriesResponse.class))
                .thenReturn(Optional.of(series("MSFT", "2026-08-06")));
        // Finnhub gates some metrics behind paid tiers and omits others entirely.
        when(fundamentalsCache.get("MSFT", FinnhubBasicFinancialsResponse.class))
                .thenReturn(Optional.of(new FinnhubBasicFinancialsResponse(
                        new FinnhubBasicFinancialsResponse.Metric(2.0e12, null, null, 100.0))));

        StockComparisonResponse result = service.compare(List.of("AAPL", "MSFT"), "1day", 90);

        assertThat(result.entries().get(1).fundamentals().peRatio()).isNull();
        assertThat(result.entries().get(1).fundamentals().fiftyTwoWeekHigh()).isNull();
        assertThat(result.entries().get(1).fundamentals().marketCap()).isEqualTo(2.0e12);
    }

    @Test
    void emptyMetricBlockDegradesToEmptyFundamentals() {
        stubSymbol("AAPL", series("AAPL", "2026-08-06"));
        when(companyRepository.findBySymbol("MSFT")).thenReturn(Optional.of(company("MSFT")));
        when(quoteService.getQuote("MSFT")).thenReturn(quote(100.0));
        when(chartCache.get("MSFT", "1day", TwelveDataTimeSeriesResponse.class))
                .thenReturn(Optional.of(series("MSFT", "2026-08-06")));
        when(fundamentalsCache.get("MSFT", FinnhubBasicFinancialsResponse.class))
                .thenReturn(Optional.of(new FinnhubBasicFinancialsResponse(null)));

        StockComparisonResponse result = service.compare(List.of("AAPL", "MSFT"), "1day", 90);

        assertThat(result.entries().get(1).fundamentals().marketCap()).isNull();
        assertThat(result.entries().get(1).fundamentals().peRatio()).isNull();
    }

    @Test
    void nonOverlappingHistoriesYieldEmptyAlignedSeriesRatherThanMisalignedOnes() {
        stubSymbol("AAPL", series("AAPL", "2026-08-06"));
        stubSymbol("MSFT", series("MSFT", "2026-07-01"));

        StockComparisonResponse result = service.compare(List.of("AAPL", "MSFT"), "1day", 90);

        assertThat(result.entries())
                .allSatisfy(entry -> assertThat(entry.candles()).isEmpty());
    }
}
