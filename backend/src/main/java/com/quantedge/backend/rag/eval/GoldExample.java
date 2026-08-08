package com.quantedge.backend.rag.eval;

/**
 * One hand-labeled gold-set entry: a question and the single source document that answers it.
 * {@code sourceType} ("NEWS" or "NOTE") is bookkeeping for the README's per-domain breakdown, not
 * matched against {@link com.quantedge.backend.rag.ingest.SourceType} - kept as a plain string so
 * the gold set's labels aren't coupled to the corpus enum's exact constant names.
 */
public record GoldExample(String id, String sourceType, String docId, String question) {}
