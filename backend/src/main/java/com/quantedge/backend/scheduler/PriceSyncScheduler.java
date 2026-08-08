package com.quantedge.backend.scheduler;

import com.quantedge.backend.cache.PriceCache;
import com.quantedge.backend.entity.Company;
import com.quantedge.backend.exception.ExternalApiException;
import com.quantedge.backend.external.FinnhubClient;
import com.quantedge.backend.external.dto.FinnhubQuoteResponse;
import com.quantedge.backend.kafka.producer.StockPriceProducer;
import com.quantedge.backend.repository.CompanyRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Refreshes {@link PriceCache} for every seeded company on a fixed schedule, so pages read
 * quotes from Redis instead of hitting Finnhub on every request.
 *
 * <p>Calls are spaced out by {@code price-sync.inter-call-delay-ms} to stay under Finnhub's
 * 60 requests/minute free-tier limit regardless of how many companies are seeded.
 */
@Component
public class PriceSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(PriceSyncScheduler.class);

    private final CompanyRepository companyRepository;
    private final FinnhubClient finnhubClient;
    private final PriceCache priceCache;
    private final StockPriceProducer stockPriceProducer;
    private final long interCallDelayMs;

    public PriceSyncScheduler(
            CompanyRepository companyRepository,
            FinnhubClient finnhubClient,
            PriceCache priceCache,
            StockPriceProducer stockPriceProducer,
            @Value("${price-sync.inter-call-delay-ms:1100}") long interCallDelayMs) {
        this.companyRepository = companyRepository;
        this.finnhubClient = finnhubClient;
        this.priceCache = priceCache;
        this.stockPriceProducer = stockPriceProducer;
        this.interCallDelayMs = interCallDelayMs;
    }

    @Scheduled(fixedRateString = "${price-sync.fixed-rate-ms:900000}")
    public void syncPrices() {
        List<Company> companies = companyRepository.findAll();
        log.info("Starting price sync for {} companies", companies.size());

        int succeeded = 0;
        for (Company company : companies) {
            if (syncOne(company.getSymbol())) {
                succeeded++;
            }
            sleepBetweenCalls();
        }

        log.info("Price sync complete: {}/{} companies refreshed", succeeded, companies.size());
    }

    private boolean syncOne(String symbol) {
        try {
            FinnhubQuoteResponse quote = finnhubClient.getQuote(symbol);
            priceCache.put(symbol, quote);
            stockPriceProducer.publish(
                    symbol, BigDecimal.valueOf(quote.currentPrice()).setScale(2, RoundingMode.HALF_UP));
            return true;
        } catch (ExternalApiException ex) {
            log.warn("Skipping price sync for symbol={} due to upstream failure", symbol, ex);
            return false;
        }
    }

    private void sleepBetweenCalls() {
        try {
            Thread.sleep(interCallDelayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
