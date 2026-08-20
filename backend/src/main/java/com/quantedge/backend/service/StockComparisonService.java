package com.quantedge.backend.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.quantedge.backend.cache.ChartCache;
import com.quantedge.backend.cache.FundamentalsCache;
import com.quantedge.backend.dto.response.CandleResponse;
import com.quantedge.backend.dto.response.ComparisonEntryResponse;
import com.quantedge.backend.dto.response.FundamentalsResponse;
import com.quantedge.backend.dto.response.StockComparisonResponse;
import com.quantedge.backend.entity.Company;
import com.quantedge.backend.exception.CompanyNotFoundException;
import com.quantedge.backend.exception.InvalidComparisonRequestException;
import com.quantedge.backend.external.FinnhubClient;
import com.quantedge.backend.external.TwelveDataClient;
import com.quantedge.backend.external.dto.FinnhubBasicFinancialsResponse;
import com.quantedge.backend.external.dto.TwelveDataTimeSeriesResponse;
import com.quantedge.backend.mapper.CompanyMapper;
import com.quantedge.backend.mapper.MarketDataMapper;
import com.quantedge.backend.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Builds a 2-3 stock side-by-side comparison: aligned fundamentals plus a shared-axis candle
 * series for the normalized price-overlay chart.
 *
 * <p>Every input is cache-first and reuses the Phase 2 read paths — {@link QuoteService} (PriceCache
 * / Finnhub), {@link ChartCache} (Twelve Data) and {@link FundamentalsCache} (Finnhub basic
 * financials). Nothing here calls an external API that a warm cache could have answered, which
 * matters because a comparison fans out over up to three symbols at once.
 */
@Service
@RequiredArgsConstructor
public class StockComparisonService {

    private static final int MIN_SYMBOLS = 2;
    private static final int MAX_SYMBOLS = 3;
    private static final Duration CHART_TTL = Duration.ofMinutes(60);

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;
    private final MarketDataMapper marketDataMapper;
    private final QuoteService quoteService;
    private final ChartCache chartCache;
    private final FundamentalsCache fundamentalsCache;
    private final TwelveDataClient twelveDataClient;
    private final FinnhubClient finnhubClient;

    public StockComparisonResponse compare(List<String> symbols, String interval, int outputSize) {
        List<String> requested = normalize(symbols);
        List<Company> companies = requested.stream().map(this::requireCompany).toList();

        Map<String, List<CandleResponse>> candlesBySymbol = new LinkedHashMap<>();
        for (Company company : companies) {
            candlesBySymbol.put(
                    company.getSymbol(),
                    marketDataMapper.toCandleResponses(resolveTimeSeries(company.getSymbol(), interval, outputSize)));
        }
        Set<String> sharedAxis = sharedDatetimes(candlesBySymbol.values());

        List<ComparisonEntryResponse> entries = companies.stream()
                .map(company -> new ComparisonEntryResponse(
                        companyMapper.toResponse(company),
                        marketDataMapper.toQuoteResponse(quoteService.getQuote(company.getSymbol())),
                        resolveFundamentals(company.getSymbol()),
                        alignToAxis(candlesBySymbol.get(company.getSymbol()), sharedAxis)))
                .toList();

        return new StockComparisonResponse(entries);
    }

    /**
     * Upper-cases, trims and de-duplicates the requested symbols while preserving request order —
     * the frontend renders one column per entry in the order it asked for them. De-duplication runs
     * before the count check so comparing a stock against itself reads as "too few stocks" rather
     * than silently rendering the same column twice.
     */
    private List<String> normalize(List<String> symbols) {
        if (symbols == null) {
            throw new InvalidComparisonRequestException("symbols is required");
        }
        List<String> normalized = symbols.stream()
                .filter(symbol -> symbol != null && !symbol.isBlank())
                .map(symbol -> symbol.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();

        if (normalized.size() < MIN_SYMBOLS || normalized.size() > MAX_SYMBOLS) {
            throw new InvalidComparisonRequestException("Compare between " + MIN_SYMBOLS + " and " + MAX_SYMBOLS
                    + " distinct stocks, got " + normalized.size());
        }
        return normalized;
    }

    private Company requireCompany(String symbol) {
        return companyRepository
                .findBySymbol(symbol)
                .orElseThrow(() -> new CompanyNotFoundException("No company found for symbol " + symbol));
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

    private FundamentalsResponse resolveFundamentals(String symbol) {
        FinnhubBasicFinancialsResponse financials = fundamentalsCache
                .get(symbol, FinnhubBasicFinancialsResponse.class)
                .orElseGet(() -> {
                    FinnhubBasicFinancialsResponse fetched = finnhubClient.getBasicFinancials(symbol);
                    fundamentalsCache.put(symbol, fetched);
                    return fetched;
                });

        if (financials == null || financials.metric() == null) {
            return FundamentalsResponse.empty();
        }
        FinnhubBasicFinancialsResponse.Metric metric = financials.metric();
        return new FundamentalsResponse(
                metric.marketCapitalization(), metric.peRatio(), metric.fiftyTwoWeekHigh(), metric.fiftyTwoWeekLow());
    }

    /**
     * The overlay chart plots every symbol against one x-axis, so the series must agree on their
     * datetimes. Intersecting them drops bars a symbol is missing (a trading halt, a later listing
     * date, a provider gap) instead of letting one series silently shift against the others.
     */
    private Set<String> sharedDatetimes(Iterable<List<CandleResponse>> candleLists) {
        Set<String> shared = null;
        for (List<CandleResponse> candles : candleLists) {
            Set<String> datetimes =
                    candles.stream().map(CandleResponse::datetime).collect(Collectors.toCollection(LinkedHashSet::new));
            if (shared == null) {
                shared = datetimes;
            } else {
                shared.retainAll(datetimes);
            }
        }
        return shared == null ? Set.of() : shared;
    }

    /**
     * Filters a symbol's candles to the shared axis and sorts them oldest-first. Twelve Data returns
     * newest-first, and lightweight-charts requires ascending time, so the ordering is fixed here
     * rather than left to each caller.
     */
    private List<CandleResponse> alignToAxis(List<CandleResponse> candles, Set<String> sharedAxis) {
        List<CandleResponse> aligned = new ArrayList<>(candles.stream()
                .filter(candle -> sharedAxis.contains(candle.datetime()))
                .toList());
        aligned.sort(Comparator.comparing(CandleResponse::datetime));
        return List.copyOf(aligned);
    }
}
