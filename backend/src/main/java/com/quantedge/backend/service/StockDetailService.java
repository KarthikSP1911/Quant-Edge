package com.quantedge.backend.service;

import com.quantedge.backend.cache.ChartCache;
import com.quantedge.backend.cache.PriceCache;
import com.quantedge.backend.dto.response.CandleResponse;
import com.quantedge.backend.dto.response.QuoteResponse;
import com.quantedge.backend.dto.response.StockDetailResponse;
import com.quantedge.backend.entity.Company;
import com.quantedge.backend.external.FinnhubClient;
import com.quantedge.backend.external.TwelveDataClient;
import com.quantedge.backend.external.dto.FinnhubQuoteResponse;
import com.quantedge.backend.external.dto.TwelveDataTimeSeriesResponse;
import com.quantedge.backend.mapper.CompanyMapper;
import com.quantedge.backend.repository.CompanyRepository;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Combines a company's static profile (Postgres), live quote (PriceCache / Finnhub) and OHLCV
 * candles (ChartCache / Twelve Data) into a single stock detail read, following the cache-aside
 * pattern from the Redis cache layer: check cache, fall through to the external API on a miss,
 * then populate the cache for next time.
 */
@Service
@RequiredArgsConstructor
public class StockDetailService {

    private static final Duration CHART_TTL = Duration.ofMinutes(60);

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;
    private final PriceCache priceCache;
    private final FinnhubClient finnhubClient;
    private final ChartCache chartCache;
    private final TwelveDataClient twelveDataClient;

    public Optional<StockDetailResponse> getStockDetail(String symbol, String interval, int outputSize) {
        return companyRepository.findBySymbol(symbol).map(company -> buildStockDetail(company, interval, outputSize));
    }

    private StockDetailResponse buildStockDetail(Company company, String interval, int outputSize) {
        FinnhubQuoteResponse quote = resolveQuote(company.getSymbol());
        TwelveDataTimeSeriesResponse timeSeries = resolveTimeSeries(company.getSymbol(), interval, outputSize);

        return new StockDetailResponse(
                companyMapper.toResponse(company), toQuoteResponse(quote), toCandleResponses(timeSeries));
    }

    private FinnhubQuoteResponse resolveQuote(String symbol) {
        return priceCache.get(symbol, FinnhubQuoteResponse.class).orElseGet(() -> {
            FinnhubQuoteResponse fetched = finnhubClient.getQuote(symbol);
            priceCache.put(symbol, fetched);
            return fetched;
        });
    }

    private TwelveDataTimeSeriesResponse resolveTimeSeries(String symbol, String interval, int outputSize) {
        return chartCache
                .get(symbol, interval, TwelveDataTimeSeriesResponse.class)
                .orElseGet(() -> {
                    TwelveDataTimeSeriesResponse fetched = twelveDataClient.getTimeSeries(symbol, interval, outputSize);
                    chartCache.put(symbol, interval, fetched, CHART_TTL);
                    return fetched;
                });
    }

    private QuoteResponse toQuoteResponse(FinnhubQuoteResponse quote) {
        return new QuoteResponse(
                quote.currentPrice(),
                quote.high(),
                quote.low(),
                quote.open(),
                quote.previousClose(),
                quote.timestamp());
    }

    private List<CandleResponse> toCandleResponses(TwelveDataTimeSeriesResponse timeSeries) {
        if (timeSeries.values() == null) {
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
