package com.quantedge.backend.rag.chunking;

import java.util.List;

/** Splits a source document's text into retrievable chunks. */
public interface Chunker {

    List<Chunk> chunk(String text);

    ChunkingStrategy strategy();
}
