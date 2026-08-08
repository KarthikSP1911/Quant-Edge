package com.quantedge.backend.rag.eval;

import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientAsync;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.client.okhttp.OpenAIOkHttpClientAsync;
import com.quantedge.backend.rag.chunking.Chunker;
import com.quantedge.backend.rag.chunking.ChunkerFactory;
import com.quantedge.backend.rag.config.QdrantVectorStoreFactory;
import com.quantedge.backend.rag.ingest.CorpusLoader;
import com.quantedge.backend.rag.ingest.KnowledgeIngestionService;
import com.quantedge.backend.rag.retrieval.Bm25Index;
import com.quantedge.backend.rag.retrieval.KnowledgeBaseService;
import com.quantedge.backend.rag.retrieval.KnowledgeChunkResult;
import com.quantedge.backend.rag.retrieval.LlmRerankService;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;

/**
 * Standalone CLI (no Spring context - the full app needs Postgres/Kafka/Redis this harness
 * doesn't) that ingests the fixture corpus into a dedicated Qdrant collection for one
 * chunking/retrieval config, scores it against the gold set, and prints Recall@1/5/10 and MRR
 * plus a ready-to-paste README table row.
 *
 * <p>Usage: {@code java -cp target/classes:... com.quantedge.backend.rag.eval.EvalRunner
 * <FIXED|RECURSIVE|SEMANTIC> <hybrid true|false> <rerank true|false> [chunkSize] [chunkOverlap] [label]}
 *
 * <p>Requires env vars {@code QDRANT_URL}, {@code QDRANT_API_KEY}, and (only when rerank=true)
 * {@code GROQ_API_KEY} / {@code GROQ_MODEL}.
 */
public final class EvalRunner {

    private static final int TOP_K = 10;
    private static final int RERANK_CANDIDATE_K = 20;

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
                "[%s] ingested %d chunks into collection '%s'%n",
                config.label(), chunks.size(), config.collectionName());

        Bm25Index bm25Index = config.hybrid() ? Bm25Index.build(chunks) : null;
        LlmRerankService rerankService = new LlmRerankService(config.rerank() ? buildChatClient() : null);
        KnowledgeBaseService knowledgeBaseService = new KnowledgeBaseService(rerankService);

        GoldSetLoader goldSetLoader = new GoldSetLoader();
        List<GoldExample> goldSet = goldSetLoader.load();

        List<EvalMetrics.QueryResult> results = new ArrayList<>(goldSet.size());
        int candidateK = config.rerank() ? RERANK_CANDIDATE_K : TOP_K;
        int queryIndex = 0;
        for (GoldExample example : goldSet) {
            List<KnowledgeChunkResult> retrieved =
                    knowledgeBaseService.search(new KnowledgeBaseService.RetrievalRequest(
                            vectorStore,
                            Optional.ofNullable(bm25Index),
                            example.question(),
                            config.hybrid(),
                            config.rerank(),
                            candidateK,
                            TOP_K));
            List<String> rankedDocIds =
                    retrieved.stream().map(KnowledgeChunkResult::docId).toList();
            results.add(new EvalMetrics.QueryResult(example.question(), rankedDocIds, example.docId()));
            queryIndex++;
            if (config.rerank()) {
                System.out.printf("  [%s] scored %d/%d%n", config.label(), queryIndex, goldSet.size());
                // Groq's free tier caps at 30 requests/min (CLAUDE.md) - one LLM-rerank call per
                // query, so throttle to ~24/min rather than burn the budget on retry backoff.
                Thread.sleep(2500);
            }
        }

        printReport(config, results);
    }

    private static void printReport(EvalRunConfig config, List<EvalMetrics.QueryResult> results) {
        double recall1 = EvalMetrics.recallAtK(results, 1);
        double recall5 = EvalMetrics.recallAtK(results, 5);
        double recall10 = EvalMetrics.recallAtK(results, 10);
        double mrr = EvalMetrics.meanReciprocalRank(results);

        System.out.printf(
                "%n=== %s (chunking=%s hybrid=%s rerank=%s size=%d overlap=%d) ===%n",
                config.label(),
                config.strategy(),
                config.hybrid(),
                config.rerank(),
                config.chunkSize(),
                config.chunkOverlap());
        System.out.printf(Locale.ROOT, "Recall@1:  %.3f%n", recall1);
        System.out.printf(Locale.ROOT, "Recall@5:  %.3f%n", recall5);
        System.out.printf(Locale.ROOT, "Recall@10: %.3f%n", recall10);
        System.out.printf(Locale.ROOT, "MRR:       %.3f%n", mrr);
        System.out.println();
        System.out.println("README row:");
        System.out.printf(
                Locale.ROOT,
                "| %s / %s / %s / %s | %.3f | %.3f | %.3f | %.3f | |%n",
                config.strategy(),
                config.rerank() ? "llm-rerank" : "no-rerank",
                config.hybrid() ? "hybrid (dense+BM25)" : "dense",
                "all-MiniLM-L6-v2",
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
        QdrantGrpcClient grpcClient = QdrantGrpcClient.newBuilder(host, 6334, useTls)
                .withApiKey(apiKey)
                .build();
        return new QdrantClient(grpcClient);
    }

    private static EmbeddingModel buildEmbeddingModel() throws Exception {
        TransformersEmbeddingModel model = new TransformersEmbeddingModel();
        model.afterPropertiesSet();
        return model;
    }

    private static ChatClient buildChatClient() {
        String apiKey = System.getenv("GROQ_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        String model = System.getenv().getOrDefault("GROQ_MODEL", "openai/gpt-oss-120b");
        OpenAIClient openAiClient = OpenAIOkHttpClient.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .apiKey(apiKey)
                .build();
        OpenAIClientAsync openAiClientAsync = OpenAIOkHttpClientAsync.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .apiKey(apiKey)
                .build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiClient(openAiClient)
                .openAiClientAsync(openAiClientAsync)
                .options(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(0.0)
                        .maxTokens(512)
                        .build())
                .build();
        return ChatClient.builder(chatModel).build();
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required environment variable not set: " + name);
        }
        return value;
    }
}
