package com.quantedge.backend.scheduler;

import java.util.concurrent.atomic.AtomicBoolean;

import com.quantedge.backend.rag.ingest.NewsIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily entry point for the news-ingestion pipeline: Finnhub -> dedupe -> embed -> Qdrant, then a
 * retention sweep. Defaults to once a day (06:00 server time); {@code quantedge.rag.news.sync-cron}
 * overrides it.
 *
 * <p>{@code running} guards against a scheduled trigger firing while the previous run is still
 * going (a large company list plus Finnhub's free-tier pacing can make one run take a while) -
 * {@code @Scheduled} alone doesn't prevent overlap.
 */
@Component
public class NewsSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(NewsSyncScheduler.class);

    private final NewsIngestionService newsIngestionService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public NewsSyncScheduler(NewsIngestionService newsIngestionService) {
        this.newsIngestionService = newsIngestionService;
    }

    @Scheduled(cron = "${quantedge.rag.news.sync-cron:0 0 6 * * *}")
    public void syncNews() {
        if (!running.compareAndSet(false, true)) {
            log.warn("News sync already in progress, skipping this trigger");
            return;
        }
        try {
            newsIngestionService.syncAll();
            newsIngestionService.purgeStaleNews();
        } finally {
            running.set(false);
        }
    }
}
