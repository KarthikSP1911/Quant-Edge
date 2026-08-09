package com.quantedge.backend.rag.chunking;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

/** Builds a {@link Chunker} for a given strategy so the ingestion pipeline and eval harness share one place that knows how to construct each. */
@Component
public class ChunkerFactory {

    private final EmbeddingModel embeddingModel;

    public ChunkerFactory(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public Chunker create(ChunkingStrategy strategy, int chunkSize, int chunkOverlap) {
        return switch (strategy) {
            case FIXED -> new FixedSizeChunker(chunkSize, chunkOverlap);
            case RECURSIVE -> new RecursiveChunker(chunkSize, chunkOverlap);
            case SEMANTIC -> new SemanticChunker(embeddingModel, chunkSize, 1.0);
        };
    }
}
