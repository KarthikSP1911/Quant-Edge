package com.quantedge.backend.rag.retrieval;

import com.quantedge.backend.rag.ingest.SourceType;

/** A retrieved chunk, ranked by whichever retrieval mode produced it. */
public record KnowledgeChunkResult(
        String docId, SourceType sourceType, String symbol, String title, String text, double score) {}
