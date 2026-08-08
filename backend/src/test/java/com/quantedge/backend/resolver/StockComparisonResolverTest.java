package com.quantedge.backend.resolver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import com.quantedge.backend.dto.response.CandleResponse;
import com.quantedge.backend.dto.response.CompanyResponse;
import com.quantedge.backend.dto.response.ComparisonEntryResponse;
import com.quantedge.backend.dto.response.FundamentalsResponse;
import com.quantedge.backend.dto.response.QuoteResponse;
import com.quantedge.backend.dto.response.StockComparisonResponse;
import com.quantedge.backend.exception.InvalidComparisonRequestException;
import com.quantedge.backend.service.StockComparisonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@GraphQlTest(StockComparisonResolver.class)
class StockComparisonResolverTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private StockComparisonService stockComparisonService;

    private static ComparisonEntryResponse entry(String symbol, Double peRatio) {
        CompanyResponse company = new CompanyResponse(
                UUID.randomUUID(), symbol, symbol + " Inc.", "Technology", "Software", null, null, "NASDAQ");
        QuoteResponse quote = new QuoteResponse(190.0, 195.0, 185.0, 188.0, 187.0, 1000L);
        FundamentalsResponse fundamentals = new FundamentalsResponse(3.0e12, peRatio, 250.0, 150.0);
        List<CandleResponse> candles = List.of(new CandleResponse("2026-08-06", 188.0, 195.0, 185.0, 190.0, 1.0e6));
        return new ComparisonEntryResponse(company, quote, fundamentals, candles);
    }

    @Test
    void stockComparisonReturnsOneEntryPerRequestedSymbol() {
        when(stockComparisonService.compare(List.of("AAPL", "MSFT"), "1day", 90))
                .thenReturn(new StockComparisonResponse(List.of(entry("AAPL", 30.5), entry("MSFT", 35.0))));

        graphQlTester
                .document("{ stockComparison(symbols: [\"AAPL\", \"MSFT\"])"
                        + " { entries { company { symbol } quote { currentPrice }"
                        + " fundamentals { marketCap peRatio } candles { close } } } }")
                .execute()
                .path("stockComparison.entries[*].company.symbol")
                .entityList(String.class)
                .containsExactly("AAPL", "MSFT")
                .path("stockComparison.entries[0].fundamentals.peRatio")
                .entity(Double.class)
                .isEqualTo(30.5);
    }

    @Test
    void defaultsIntervalAndOutputSizeWhenNotSupplied() {
        when(stockComparisonService.compare(any(), anyString(), anyInt()))
                .thenReturn(new StockComparisonResponse(List.of(entry("AAPL", 30.5), entry("MSFT", 35.0))));

        graphQlTester
                .document("{ stockComparison(symbols: [\"AAPL\", \"MSFT\"]) { entries { company { symbol } } } }")
                .execute()
                .errors()
                .verify();

        verify(stockComparisonService).compare(List.of("AAPL", "MSFT"), "1day", 90);
    }

    @Test
    void aNullMetricIsExposedAsNullNotZero() {
        when(stockComparisonService.compare(any(), anyString(), anyInt()))
                .thenReturn(new StockComparisonResponse(List.of(entry("AAPL", null), entry("MSFT", 35.0))));

        graphQlTester
                .document("{ stockComparison(symbols: [\"AAPL\", \"MSFT\"])"
                        + " { entries { fundamentals { peRatio } } } }")
                .execute()
                .path("stockComparison.entries[0].fundamentals.peRatio")
                .valueIsNull();
    }

    @Test
    void surfacesValidationFailureAsAGraphQlError() {
        when(stockComparisonService.compare(any(), anyString(), anyInt()))
                .thenThrow(new InvalidComparisonRequestException("Compare between 2 and 3 distinct stocks, got 1"));

        graphQlTester
                .document("{ stockComparison(symbols: [\"AAPL\"]) { entries { company { symbol } } } }")
                .execute()
                .errors()
                .satisfy(errors ->
                        org.assertj.core.api.Assertions.assertThat(errors).isNotEmpty());
    }
}
