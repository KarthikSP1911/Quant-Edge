package com.quantedge.backend.rag.chunking;

/** A single piece of a source document after chunking, before embedding. */
public record Chunk(String text, int index) {}
