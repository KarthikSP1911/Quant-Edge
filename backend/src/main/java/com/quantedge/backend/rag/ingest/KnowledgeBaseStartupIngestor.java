package com.quantedge.backend.rag.ingest;

import com.quantedge.backend.rag.chunking.Chunker;
import com.quantedge.backend.rag.chunking.ChunkerFactory;
import com.quantedge.backend.rag.chunking.ChunkingStrategy;
import io.qdrant.client.QdrantClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Ingests the frozen fixture corpus into the default {@code queryKnowledgeBase} collection on
 * startup, once - subsequent restarts see a non-zero point count and skip re-ingesting.
 */
@Component
public class KnowledgeBaseStartupIngestor implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseStartupIngestor.class);

    private final CorpusLoader corpusLoader;
    private final ChunkerFactory chunkerFactory;
    private final KnowledgeIngestionService ingestionService;
    private final VectorStore knowledgeBaseVectorStore;
    private final QdrantClient qdrantClient;

    @Value("${quantedge.rag.auto-ingest:true}")
    private boolean autoIngest;

    @Value("${quantedge.rag.qdrant.collection}")
    private String collectionName;

    @Value("${quantedge.rag.chunking.strategy}")
    private ChunkingStrategy chunkingStrategy;

    @Value("${quantedge.rag.chunking.chunk-size}")
    private int chunkSize;

    @Value("${quantedge.rag.chunking.chunk-overlap}")
    private int chunkOverlap;

    public KnowledgeBaseStartupIngestor(
            CorpusLoader corpusLoader,
            ChunkerFactory chunkerFactory,
            KnowledgeIngestionService ingestionService,
            @Lazy VectorStore knowledgeBaseVectorStore,
            QdrantClient qdrantClient) {
        this.corpusLoader = corpusLoader;
        this.chunkerFactory = chunkerFactory;
        this.ingestionService = ingestionService;
        this.knowledgeBaseVectorStore = knowledgeBaseVectorStore;
        this.qdrantClient = qdrantClient;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!autoIngest) {
            return;
        }
        // Touching the lazy VectorStore bean forces collection creation before we count it.
        knowledgeBaseVectorStore.getName();
        long existingCount = qdrantClient.countAsync(collectionName).get();
        if (existingCount > 0) {
            log.info(
                    "Knowledge base collection '{}' already has {} points, skipping ingestion",
                    collectionName,
                    existingCount);
            return;
        }
        Chunker chunker = chunkerFactory.create(chunkingStrategy, chunkSize, chunkOverlap);
        int chunks = ingestionService.ingest(corpusLoader.loadAll(), chunker, knowledgeBaseVectorStore);
        log.info(
                "Ingested {} chunks into knowledge base collection '{}' using {}",
                chunks,
                collectionName,
                chunkingStrategy);
    }
}
