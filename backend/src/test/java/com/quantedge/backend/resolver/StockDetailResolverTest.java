package com.quantedge.backend.resolver;

import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.quantedge.backend.dto.response.CandleResponse;
import com.quantedge.backend.dto.response.CompanyResponse;
import com.quantedge.backend.dto.response.QuoteResponse;
import com.quantedge.backend.dto.response.StockDetailResponse;
import com.quantedge.backend.service.StockDetailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@GraphQlTest(StockDetailResolver.class)
class StockDetailResolverTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private StockDetailService stockDetailService;

    private StockDetailResponse appleDetail() {
        CompanyResponse company = new CompanyResponse(
                UUID.randomUUID(), "AAPL", "Apple Inc.", "Technology", "Consumer Electronics", null, null, "NASDAQ");
        QuoteResponse quote = new QuoteResponse(190.0, 195.0, 185.0, 188.0, 187.0, 1000L);
        List<CandleResponse> candles = List.of(new CandleResponse("2026-08-06", 188.0, 195.0, 185.0, 190.0, 1000000.0));
        return new StockDetailResponse(company, quote, candles);
    }

    @Test
    void stockDetailReturnsCombinedCompanyQuoteAndCandles() {
        when(stockDetailService.getStockDetail("AAPL", "1day", 30)).thenReturn(Optional.of(appleDetail()));

        graphQlTester
                .document(
                        "{ stockDetail(symbol: \"AAPL\") { company { symbol } quote { currentPrice } candles { close } } }")
                .execute()
                .path("stockDetail.company.symbol")
                .entity(String.class)
                .isEqualTo("AAPL")
                .path("stockDetail.quote.currentPrice")
                .entity(Double.class)
                .isEqualTo(190.0)
                .path("stockDetail.candles")
                .entityList(Object.class)
                .hasSize(1);
    }

    @Test
    void stockDetailReturnsNullWhenCompanyNotFound() {
        when(stockDetailService.getStockDetail("NOPE", "1day", 30)).thenReturn(Optional.empty());

        graphQlTester
                .document("{ stockDetail(symbol: \"NOPE\") { company { symbol } } }")
                .execute()
                .path("stockDetail")
                .valueIsNull();
    }
}
