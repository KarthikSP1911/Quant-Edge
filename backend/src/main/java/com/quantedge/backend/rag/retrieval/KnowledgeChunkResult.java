package com.quantedge.backend.rag.retrieval;

import com.quantedge.backend.rag.ingest.SourceType;

/**
 * A retrieved chunk, ranked by whichever retrieval mode produced it. {@code source}, {@code url}
 * and {@code publishedAt} (epoch millis) are only populated for {@link SourceType#NEWS} chunks -
 * {@code publishedAt} drives the recency boost in {@link RecencyRanking} and lets the chat model
 * see how fresh a news chunk is.
 */
public record KnowledgeChunkResult(
        String docId,
        SourceType sourceType,
        String symbol,
        String title,
        String text,
        double score,
        String source,
        String url,
        Long publishedAt) {}
