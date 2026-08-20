package com.quantedge.backend.service;

import java.time.Duration;
import java.util.Optional;

import com.quantedge.backend.cache.ChartCache;
import com.quantedge.backend.dto.response.StockDetailResponse;
import com.quantedge.backend.entity.Company;
import com.quantedge.backend.external.TwelveDataClient;
import com.quantedge.backend.external.dto.FinnhubQuoteResponse;
import com.quantedge.backend.external.dto.TwelveDataTimeSeriesResponse;
import com.quantedge.backend.mapper.CompanyMapper;
import com.quantedge.backend.mapper.MarketDataMapper;
import com.quantedge.backend.repository.CompanyRepository;
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
    private final MarketDataMapper marketDataMapper;
    private final QuoteService quoteService;
    private final ChartCache chartCache;
    private final TwelveDataClient twelveDataClient;

    public Optional<StockDetailResponse> getStockDetail(String symbol, String interval, int outputSize) {
        return companyRepository.findBySymbol(symbol).map(company -> buildStockDetail(company, interval, outputSize));
    }

    private StockDetailResponse buildStockDetail(Company company, String interval, int outputSize) {
        FinnhubQuoteResponse quote = quoteService.getQuote(company.getSymbol());
        TwelveDataTimeSeriesResponse timeSeries = resolveTimeSeries(company.getSymbol(), interval, outputSize);

        return new StockDetailResponse(
                companyMapper.toResponse(company),
                marketDataMapper.toQuoteResponse(quote),
                marketDataMapper.toCandleResponses(timeSeries));
    }

    private TwelveDataTimeSeriesResponse resolveTimeSeries(String symbol, String interval, int outputSize) {
        return chartCache
                .get(symbol, interval, outputSize, TwelveDataTimeSeriesResponse.class)
                .orElseGet(() -> {
                    TwelveDataTimeSeriesResponse fetched = twelveDataClient.getTimeSeries(symbol, interval, outputSize);
                    chartCache.put(symbol, interval, outputSize, fetched, CHART_TTL);
                    return fetched;
                });
    }
}
