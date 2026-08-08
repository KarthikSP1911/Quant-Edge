package com.quantedge.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quantedge.backend.cache.RedisCacheClient;
import com.quantedge.backend.external.FinnhubClient;
import com.quantedge.backend.external.TwelveDataClient;
import com.quantedge.backend.external.dto.FinnhubBasicFinancialsResponse;
import com.quantedge.backend.external.dto.FinnhubQuoteResponse;
import com.quantedge.backend.external.dto.TwelveDataTimeSeriesResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Exercises the stockComparison query end to end against real Postgres and the real GraphQL schema:
 * Flyway-migrated companies are resolved from the database, the schema is parsed and validated for
 * real, and the cache-aside path runs unmocked.
 *
 * <p>Only the outermost edges are stubbed — the external HTTP clients and the Redis REST client —
 * so no test ever reaches Finnhub, Twelve Data or Upstash. Stubbing RedisCacheClient rather than the
 * individual cache components keeps the real ChartCache / FundamentalsCache / PriceCache logic
 * (key namespacing, TTLs, cache-miss fallthrough) under test.
 */
@SpringBootTest
@AutoConfigureGraphQlTester
@Testcontainers
class StockComparisonIT {

    private static final String COMPARISON_QUERY =
            """
            query Compare($symbols: [String!]!) {
              stockComparison(symbols: $symbols) {
                entries {
                  company { symbol name }
                  quote { currentPrice }
                  fundamentals { marketCap peRatio fiftyTwoWeekHigh fiftyTwoWeekLow }
                  candles { datetime close }
                }
              }
            }
            """;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private FinnhubClient finnhubClient;

    @MockitoBean
    private TwelveDataClient twelveDataClient;

    @MockitoBean
    private RedisCacheClient redisCacheClient;

    private static TwelveDataTimeSeriesResponse series(String symbol, String... datetimes) {
        List<TwelveDataTimeSeriesResponse.Candle> candles = java.util.Arrays.stream(datetimes)
                .map(dt -> new TwelveDataTimeSeriesResponse.Candle(dt, "10.0", "12.0", "9.0", "11.0", "100"))
                .toList();
        return new TwelveDataTimeSeriesResponse(
                new TwelveDataTimeSeriesResponse.Meta(symbol, "1day", "NASDAQ", "USD"), candles, "ok", null);
    }

    @BeforeEach
    void setUp() {
        // Every Redis read misses, so the cache-aside path falls through to the external clients.
        when(redisCacheClient.get(anyString(), any())).thenReturn(Optional.empty());
        when(finnhubClient.getQuote(anyString()))
                .thenReturn(new FinnhubQuoteResponse(190.0, 195.0, 185.0, 188.0, 187.0, 1000L));
        when(finnhubClient.getBasicFinancials(anyString()))
                .thenReturn(new FinnhubBasicFinancialsResponse(
                        new FinnhubBasicFinancialsResponse.Metric(3.0e12, 30.5, 250.0, 150.0)));
    }

    @Test
    void comparesTwoFlywaySeededCompaniesFromPostgres() {
        when(twelveDataClient.getTimeSeries(eq("AAPL"), anyString(), anyInt()))
                .thenReturn(series("AAPL", "2026-08-07", "2026-08-06"));
        when(twelveDataClient.getTimeSeries(eq("MSFT"), anyString(), anyInt()))
                .thenReturn(series("MSFT", "2026-08-07", "2026-08-06"));

        graphQlTester
                .document(COMPARISON_QUERY)
                .variable("symbols", List.of("AAPL", "MSFT"))
                .execute()
                .path("stockComparison.entries[*].company.symbol")
                .entityList(String.class)
                .containsExactly("AAPL", "MSFT")
                // Names come from V6__seed_companies.sql, proving the read really hit Postgres.
                .path("stockComparison.entries[0].company.name")
                .entity(String.class)
                .isEqualTo("Apple Inc.")
                .path("stockComparison.entries[0].fundamentals.peRatio")
                .entity(Double.class)
                .isEqualTo(30.5);
    }

    @Test
    void alignsCandlesOntoASharedOldestFirstAxis() {
        // AAPL carries a bar MSFT lacks; the overlay axis must intersect them.
        when(twelveDataClient.getTimeSeries(eq("AAPL"), anyString(), anyInt()))
                .thenReturn(series("AAPL", "2026-08-08", "2026-08-07", "2026-08-06"));
        when(twelveDataClient.getTimeSeries(eq("MSFT"), anyString(), anyInt()))
                .thenReturn(series("MSFT", "2026-08-07", "2026-08-06"));

        GraphQlTester.Response response = graphQlTester
                .document(COMPARISON_QUERY)
                .variable("symbols", List.of("AAPL", "MSFT"))
                .execute();

        for (int i = 0; i < 2; i++) {
            response.path("stockComparison.entries[" + i + "].candles[*].datetime")
                    .entityList(String.class)
                    .containsExactly("2026-08-06", "2026-08-07");
        }
    }

    @Test
    void warmCacheServesTheComparisonWithoutTouchingAnyExternalApi() {
        // A populated Redis short-circuits both the chart and fundamentals lookups.
        when(redisCacheClient.get(anyString(), eq(TwelveDataTimeSeriesResponse.class)))
                .thenReturn(Optional.of(series("CACHED", "2026-08-06")));
        when(redisCacheClient.get(anyString(), eq(FinnhubQuoteResponse.class)))
                .thenReturn(Optional.of(new FinnhubQuoteResponse(190.0, 195.0, 185.0, 188.0, 187.0, 1000L)));
        when(redisCacheClient.get(anyString(), eq(FinnhubBasicFinancialsResponse.class)))
                .thenReturn(Optional.of(new FinnhubBasicFinancialsResponse(
                        new FinnhubBasicFinancialsResponse.Metric(3.0e12, 30.5, 250.0, 150.0))));

        graphQlTester
                .document(COMPARISON_QUERY)
                .variable("symbols", List.of("AAPL", "MSFT"))
                .execute()
                .errors()
                .verify();

        verify(twelveDataClient, never()).getTimeSeries(anyString(), anyString(), anyInt());
        verify(finnhubClient, never()).getQuote(anyString());
        verify(finnhubClient, never()).getBasicFinancials(anyString());
    }

    @Test
    void aColdCacheCostsExactlyOneCallPerSymbolPerUpstream() {
        when(twelveDataClient.getTimeSeries(anyString(), anyString(), anyInt()))
                .thenReturn(series("ANY", "2026-08-06"));

        graphQlTester
                .document(COMPARISON_QUERY)
                .variable("symbols", List.of("AAPL", "MSFT", "GOOGL"))
                .execute()
                .errors()
                .verify();

        // Rate limits are a hard constraint (CLAUDE.md): a 3-way comparison must not fan out
        // beyond one call per symbol against each upstream.
        verify(twelveDataClient, times(3)).getTimeSeries(anyString(), anyString(), anyInt());
        verify(finnhubClient, times(3)).getBasicFinancials(anyString());
    }

    @Test
    void rejectsASingleSymbolAsAGraphQlError() {
        graphQlTester
                .document(COMPARISON_QUERY)
                .variable("symbols", List.of("AAPL"))
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors).isNotEmpty());
    }

    @Test
    void rejectsASymbolThatIsNotInPostgres() {
        graphQlTester
                .document(COMPARISON_QUERY)
                .variable("symbols", List.of("AAPL", "NOTREAL"))
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors).isNotEmpty());
    }
}
