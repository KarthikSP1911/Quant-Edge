package com.quantedge.backend.rag.ingest;

import java.time.Instant;

/**
 * A single source document before chunking - one news article or one research note.
 *
 * <p>{@code source}, {@code url} and {@code publishedAt} are only populated for live-ingested
 * news (see {@code NewsIngestionService}); the frozen fixture corpus loaded by {@link CorpusLoader}
 * leaves them {@code null} since it has no real publish timestamps or article URLs.
 */
public record KnowledgeDocument(
        String id,
        SourceType sourceType,
        String symbol,
        String title,
        String text,
        String source,
        String url,
        Instant publishedAt) {

    /** Convenience constructor for the static corpus, which has no source/url/publishedAt. */
    public KnowledgeDocument(String id, SourceType sourceType, String symbol, String title, String text) {
        this(id, sourceType, symbol, title, text, null, null, null);
    }
}
