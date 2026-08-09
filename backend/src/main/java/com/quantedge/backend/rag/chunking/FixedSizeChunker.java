package com.quantedge.backend.rag.chunking;

import java.util.ArrayList;
import java.util.List;

/**
 * Naive fixed-width character windows with overlap. Cuts mid-word/mid-sentence wherever the
 * window boundary happens to fall - the baseline every other chunking technique is measured
 * against.
 */
public class FixedSizeChunker implements Chunker {

    private final int chunkSize;
    private final int overlap;

    public FixedSizeChunker(int chunkSize, int overlap) {
        if (chunkSize <= 0 || overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException("chunkSize must be > 0 and overlap must be in [0, chunkSize)");
        }
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    @Override
    public List<Chunk> chunk(String text) {
        List<Chunk> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }
        String trimmed = text.strip();
        int step = chunkSize - overlap;
        int index = 0;
        for (int start = 0; start < trimmed.length(); start += step) {
            int end = Math.min(start + chunkSize, trimmed.length());
            String piece = trimmed.substring(start, end).strip();
            if (!piece.isEmpty()) {
                chunks.add(new Chunk(piece, index++));
            }
            if (end == trimmed.length()) {
                break;
            }
        }
        return chunks;
    }

    @Override
    public ChunkingStrategy strategy() {
        return ChunkingStrategy.FIXED;
    }
}
