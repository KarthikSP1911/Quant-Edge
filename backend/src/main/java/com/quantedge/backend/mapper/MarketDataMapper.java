package com.quantedge.backend.mapper;

import java.util.List;

import com.quantedge.backend.dto.response.CandleResponse;
import com.quantedge.backend.dto.response.QuoteResponse;
import com.quantedge.backend.external.dto.FinnhubQuoteResponse;
import com.quantedge.backend.external.dto.TwelveDataTimeSeriesResponse;
import org.springframework.stereotype.Component;

/**
 * Maps raw external market-data payloads (Finnhub quotes, Twelve Data OHLCV) onto the response
 * DTOs. Shared by the stock detail and stock comparison reads so the two can't drift in how they
 * parse Twelve Data's all-strings candle format.
 */
@Component
public class MarketDataMapper {

    public QuoteResponse toQuoteResponse(FinnhubQuoteResponse quote) {
        return new QuoteResponse(
                quote.currentPrice(),
                quote.high(),
                quote.low(),
                quote.open(),
                quote.previousClose(),
                quote.timestamp());
    }

    public List<CandleResponse> toCandleResponses(TwelveDataTimeSeriesResponse timeSeries) {
        if (timeSeries == null || timeSeries.values() == null) {
            return List.of();
        }
        return timeSeries.values().stream()
                .map(candle -> new CandleResponse(
                        candle.datetime(),
                        Double.parseDouble(candle.open()),
                        Double.parseDouble(candle.high()),
                        Double.parseDouble(candle.low()),
                        Double.parseDouble(candle.close()),
                        Double.parseDouble(candle.volume())))
                .toList();
    }
}
