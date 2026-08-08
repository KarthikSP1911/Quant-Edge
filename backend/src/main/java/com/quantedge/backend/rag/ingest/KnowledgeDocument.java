package com.quantedge.backend.rag.ingest;

/** A single source document before chunking - one news article or one research note. */
public record KnowledgeDocument(String id, SourceType sourceType, String symbol, String title, String text) {}
