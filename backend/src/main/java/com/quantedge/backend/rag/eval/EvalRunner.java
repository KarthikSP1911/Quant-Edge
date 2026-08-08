package com.quantedge.backend.rag.eval;

import com.quantedge.backend.rag.chunking.Chunker;
import com.quantedge.backend.rag.chunking.ChunkerFactory;
import com.quantedge.backend.rag.config.QdrantVectorStoreFactory;
import com.quantedge.backend.rag.ingest.CorpusLoader;
import com.quantedge.backend.rag.ingest.KnowledgeIngestionService;
import com.quantedge.backend.rag.retrieval.KnowledgeBaseService;
import com.quantedge.backend.rag.retrieval.KnowledgeChunkResult;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;

/**
 * Standalone CLI (no Spring context - the full app needs Postgres/Kafka/Redis this harness
 * doesn't) that ingests the fixture corpus into a dedicated Qdrant collection for one chunking
 * config, scores dense retrieval against the gold set, and prints Recall@1/5/10 and MRR plus a
 * ready-to-paste README table row. This is the dense-only baseline every technique in
 * phase-5/rag-technique-experiments (hybrid dense+BM25, LLM rerank) has to beat.
 *
 * <p>Usage: {@code java -cp target/classes:... com.quantedge.backend.rag.eval.EvalRunner
 * <FIXED|RECURSIVE|SEMANTIC> [chunkSize] [chunkOverlap] [label]}
 *
 * <p>Requires env vars {@code QDRANT_URL}, {@code QDRANT_API_KEY}.
 */
public final class EvalRunner {

    private static final int TOP_K = 10;

    private EvalRunner() {}

    public static void main(String[] args) throws Exception {
        EvalRunConfig config = EvalRunConfig.parse(args);

        QdrantClient qdrantClient = buildQdrantClient();
        EmbeddingModel embeddingModel = buildEmbeddingModel();
        QdrantVectorStoreFactory vectorStoreFactory = new QdrantVectorStoreFactory(qdrantClient, embeddingModel);

        try {
            qdrantClient.deleteCollectionAsync(config.collectionName()).get(30, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // Collection didn't exist yet - fine, initializeSchema below creates it.
        }
        VectorStore vectorStore = vectorStoreFactory.forCollection(config.collectionName(), true);

        ChunkerFactory chunkerFactory = new ChunkerFactory(embeddingModel);
        Chunker chunker = chunkerFactory.create(config.strategy(), config.chunkSize(), config.chunkOverlap());

        KnowledgeIngestionService ingestionService = new KnowledgeIngestionService();
        CorpusLoader corpusLoader = new CorpusLoader();
        List<Document> chunks = ingestionService.chunkCorpus(corpusLoader.loadAll(), chunker);
        ingestionService.store(chunks, vectorStore);
        System.out.printf(
                "[%s] ingested %d chunks into collection '%s'%n", config.label(), chunks.size(), config.collectionName());

        KnowledgeBaseService knowledgeBaseService = new KnowledgeBaseService();

        GoldSetLoader goldSetLoader = new GoldSetLoader();
        List<GoldExample> goldSet = goldSetLoader.load();

        List<EvalMetrics.QueryResult> results = new ArrayList<>(goldSet.size());
        for (GoldExample example : goldSet) {
            List<KnowledgeChunkResult> retrieved = knowledgeBaseService.denseSearch(vectorStore, example.question(), TOP_K);
            List<String> rankedDocIds = retrieved.stream().map(KnowledgeChunkResult::docId).toList();
            results.add(new EvalMetrics.QueryResult(example.question(), rankedDocIds, example.docId()));
        }

        printReport(config, results);
    }

    private static void printReport(EvalRunConfig config, List<EvalMetrics.QueryResult> results) {
        double recall1 = EvalMetrics.recallAtK(results, 1);
        double recall5 = EvalMetrics.recallAtK(results, 5);
        double recall10 = EvalMetrics.recallAtK(results, 10);
        double mrr = EvalMetrics.meanReciprocalRank(results);

        System.out.printf(
                "%n=== %s (chunking=%s size=%d overlap=%d) ===%n",
                config.label(), config.strategy(), config.chunkSize(), config.chunkOverlap());
        System.out.printf(Locale.ROOT, "Recall@1:  %.3f%n", recall1);
        System.out.printf(Locale.ROOT, "Recall@5:  %.3f%n", recall5);
        System.out.printf(Locale.ROOT, "Recall@10: %.3f%n", recall10);
        System.out.printf(Locale.ROOT, "MRR:       %.3f%n", mrr);
        System.out.println();
        System.out.println("README row:");
        System.out.printf(
                Locale.ROOT,
                "| %s / no-rerank / dense / all-MiniLM-L6-v2 | %.3f | %.3f | %.3f | %.3f | |%n",
                config.strategy(),
                recall1,
                recall5,
                recall10,
                mrr);
    }

    private static QdrantClient buildQdrantClient() {
        String url = requireEnv("QDRANT_URL");
        String apiKey = requireEnv("QDRANT_API_KEY");
        String host = url.replaceFirst("^https?://", "").replaceAll("/$", "");
        boolean useTls = url.startsWith("https");
        QdrantGrpcClient grpcClient =
                QdrantGrpcClient.newBuilder(host, 6334, useTls).withApiKey(apiKey).build();
        return new QdrantClient(grpcClient);
    }

    private static EmbeddingModel buildEmbeddingModel() throws Exception {
        TransformersEmbeddingModel model = new TransformersEmbeddingModel();
        model.afterPropertiesSet();
        return model;
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required environment variable not set: " + name);
        }
        return value;
    }
}
