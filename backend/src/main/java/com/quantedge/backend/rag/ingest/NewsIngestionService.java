package com.quantedge.backend.rag.ingest;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.quantedge.backend.entity.Company;
import com.quantedge.backend.exception.ExternalApiException;
import com.quantedge.backend.external.FinnhubClient;
import com.quantedge.backend.external.dto.FinnhubNewsResponse;
import com.quantedge.backend.rag.chunking.Chunker;
import com.quantedge.backend.rag.chunking.ChunkerFactory;
import com.quantedge.backend.rag.chunking.ChunkingStrategy;
import com.quantedge.backend.repository.CompanyRepository;
import io.qdrant.client.ConditionFactory;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Common.Filter;
import io.qdrant.client.grpc.Common.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Daily news ingestion: Finnhub company news -> dedupe against Qdrant -> embed -> upsert into the
 * same {@code queryKnowledgeBase} collection the static corpus lives in, tagged {@code
 * sourceType=NEWS} with a real {@code publishedAt} so retrieval can prefer it (see
 * {@link com.quantedge.backend.rag.retrieval.RecencyRanking}).
 *
 * <p>Safety properties, by design rather than by a side table:
 * <ul>
 *   <li><b>Dedup</b> - each article gets a stable {@code docId} ({@code symbol:finnhubArticleId}),
 *       checked against Qdrant by a payload filter before spending an embedding call on it, and
 *       each chunk additionally gets a point ID deterministically derived from that {@code docId}
 *       (see {@link KnowledgeIngestionService}), so even a duplicate ingest attempt upserts the
 *       same point instead of creating a new one.
 *   <li><b>Finnhub / Qdrant failures</b> - a failure for one symbol or one article is caught and
 *       logged; the run continues with the next symbol/article rather than aborting the whole job
 *       (same pattern as {@link com.quantedge.backend.scheduler.PriceSyncScheduler}).
 *   <li><b>Restarts</b> - there is no in-memory or DB-tracked "last run" cursor. A restart mid-run
 *       just means the next scheduled run re-checks Qdrant for each article; already-ingested
 *       articles are skipped (or harmlessly re-upserted to the same point), so nothing duplicates
 *       and nothing is lost.
 * </ul>
 */
@Service
public class NewsIngestionService {

    private static final Logger log = LoggerFactory.getLogger(NewsIngestionService.class);

    private final CompanyRepository companyRepository;
    private final FinnhubClient finnhubClient;
    private final ChunkerFactory chunkerFactory;
    private final KnowledgeIngestionService knowledgeIngestionService;
    private final QdrantClient qdrantClient;
    private final VectorStore knowledgeBaseVectorStore;

    @Value("${quantedge.rag.qdrant.collection}")
    private String collectionName;

    @Value("${quantedge.rag.chunking.strategy}")
    private ChunkingStrategy chunkingStrategy;

    @Value("${quantedge.rag.chunking.chunk-size}")
    private int chunkSize;

    @Value("${quantedge.rag.chunking.chunk-overlap}")
    private int chunkOverlap;

    @Value("${quantedge.rag.news.lookback-days:2}")
    private int lookbackDays;

    @Value("${quantedge.rag.news.retention-days:90}")
    private int retentionDays;

    @Value("${finnhub.news-sync.inter-call-delay-ms:1100}")
    private long interCallDelayMs;

    public NewsIngestionService(
            CompanyRepository companyRepository,
            FinnhubClient finnhubClient,
            ChunkerFactory chunkerFactory,
            KnowledgeIngestionService knowledgeIngestionService,
            QdrantClient qdrantClient,
            @Lazy VectorStore knowledgeBaseVectorStore) {
        this.companyRepository = companyRepository;
        this.finnhubClient = finnhubClient;
        this.chunkerFactory = chunkerFactory;
        this.knowledgeIngestionService = knowledgeIngestionService;
        this.qdrantClient = qdrantClient;
        this.knowledgeBaseVectorStore = knowledgeBaseVectorStore;
    }

    public record NewsSyncResult(int articlesIngested, int companiesSynced, int companiesFailed) {}

    /** Fetches and ingests new company news for every seeded company. Never throws. */
    public NewsSyncResult syncAll() {
        List<Company> companies = companyRepository.findAll();
        Chunker chunker = chunkerFactory.create(chunkingStrategy, chunkSize, chunkOverlap);
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(lookbackDays);

        int ingested = 0;
        int failed = 0;
        for (Company company : companies) {
            try {
                ingested += syncSymbol(company.getSymbol(), from, to, chunker);
            } catch (ExternalApiException ex) {
                log.warn("Skipping news sync for symbol={} due to upstream failure", company.getSymbol(), ex);
                failed++;
            } catch (Exception ex) {
                log.warn("Skipping news sync for symbol={} due to unexpected error", company.getSymbol(), ex);
                failed++;
            }
            sleepBetweenCalls();
        }
        log.info(
                "News sync complete: {} new articles ingested across {} companies ({} failed)",
                ingested,
                companies.size(),
                failed);
        return new NewsSyncResult(ingested, companies.size(), failed);
    }

    private int syncSymbol(String symbol, LocalDate from, LocalDate to, Chunker chunker) {
        List<FinnhubNewsResponse> articles = finnhubClient.getCompanyNews(symbol, from, to);
        int ingested = 0;
        for (FinnhubNewsResponse article : articles) {
            try {
                if (ingestIfNew(symbol, article, chunker)) {
                    ingested++;
                }
            } catch (Exception ex) {
                log.warn("Skipping article id={} for symbol={} due to ingestion failure", article.id(), symbol, ex);
            }
        }
        return ingested;
    }

    private boolean ingestIfNew(String symbol, FinnhubNewsResponse article, Chunker chunker) throws Exception {
        if (article.headline() == null || article.headline().isBlank() || article.datetime() <= 0) {
            return false;
        }
        String docId = symbol + ":" + article.id();
        if (alreadyIngested(docId)) {
            return false;
        }
        String text = article.summary() != null && !article.summary().isBlank()
                ? article.headline() + "\n\n" + article.summary()
                : article.headline();
        KnowledgeDocument document = new KnowledgeDocument(
                docId,
                SourceType.NEWS,
                symbol,
                article.headline(),
                text,
                article.source(),
                article.url(),
                Instant.ofEpochSecond(article.datetime()));
        knowledgeIngestionService.ingest(List.of(document), chunker, knowledgeBaseVectorStore);
        return true;
    }

    /** Checks Qdrant (not an in-memory/DB cache) for an existing chunk of this article. */
    private boolean alreadyIngested(String docId) throws ExecutionException, InterruptedException {
        Filter filter = Filter.newBuilder()
                .addMust(ConditionFactory.matchKeyword(KnowledgeIngestionService.META_DOC_ID, docId))
                .build();
        long count = qdrantClient.countAsync(collectionName, filter, true).get();
        return count > 0;
    }

    /**
     * Deletes NEWS chunks older than {@code retentionDays} to keep the collection from growing
     * unbounded. This is purely age-based (never triggered by a newer article "contradicting" an
     * older one) - retrieval instead prefers recent news via {@code publishedAt}-aware ranking, so
     * an article only disappears once it's genuinely stale.
     */
    public void purgeStaleNews() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        Filter filter = Filter.newBuilder()
                .addMust(ConditionFactory.matchKeyword(
                        KnowledgeIngestionService.META_SOURCE_TYPE, SourceType.NEWS.name()))
                .addMust(ConditionFactory.range(
                        KnowledgeIngestionService.META_PUBLISHED_AT,
                        Range.newBuilder().setLt(cutoff.toEpochMilli()).build()))
                .build();
        try {
            qdrantClient.deleteAsync(collectionName, filter).get();
            log.info("Purged news chunks older than {} days (before {})", retentionDays, cutoff);
        } catch (Exception ex) {
            log.warn("Failed to purge stale news from collection '{}'", collectionName, ex);
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
