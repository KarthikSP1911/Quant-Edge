package com.quantedge.backend.service;

import com.quantedge.backend.cache.ChartCache;
import com.quantedge.backend.external.TwelveDataClient;
import com.quantedge.backend.external.dto.TwelveDataTimeSeriesResponse;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Resolves a single symbol's closing price on (or, for non-trading days, the most recent trading
 * day at or before) a given past date. Used only by the portfolio time machine, so it caches
 * under its own key ({@code interval="1day-history"}) rather than sharing {@link ChartCache}'s
 * {@code "1day"} slot with {@link StockDetailService} - that slot is sized for a short recent
 * chart window and would starve a lookup that needs deep history.
 */
@Service
@RequiredArgsConstructor
public class HistoricalPriceService {

    private static final String HISTORY_INTERVAL = "1day-history";
    private static final Duration HISTORY_TTL = Duration.ofHours(24);
    private static final int MAX_OUTPUT_SIZE = 5000;

    private final ChartCache chartCache;
    private final TwelveDataClient twelveDataClient;

    public Optional<BigDecimal> getClosePriceAsOf(String symbol, LocalDate date) {
        TwelveDataTimeSeriesResponse timeSeries = chartCache
                .get(symbol, HISTORY_INTERVAL, TwelveDataTimeSeriesResponse.class)
                .orElseGet(() -> fetchAndCache(symbol, date));

        if (timeSeries == null || timeSeries.values() == null) {
            return Optional.empty();
        }

        return timeSeries.values().stream()
                .filter(candle ->
                        !LocalDate.parse(candle.datetime().substring(0, 10)).isAfter(date))
                .max(Comparator.comparing(candle -> candle.datetime()))
                .map(candle -> new BigDecimal(candle.close()));
    }

    private TwelveDataTimeSeriesResponse fetchAndCache(String symbol, LocalDate date) {
        long daysSinceDate = Duration.between(
                        date.atStartOfDay(), LocalDate.now().atStartOfDay())
                .toDays();
        int outputSize = (int) Math.min(MAX_OUTPUT_SIZE, Math.max(30, daysSinceDate + 30));

        TwelveDataTimeSeriesResponse fetched = twelveDataClient.getTimeSeries(symbol, "1day", outputSize);
        chartCache.put(symbol, HISTORY_INTERVAL, fetched, HISTORY_TTL);
        return fetched;
    }
}
