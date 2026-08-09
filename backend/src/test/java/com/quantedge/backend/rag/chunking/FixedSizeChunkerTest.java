package com.quantedge.backend.rag.chunking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class FixedSizeChunkerTest {

    @Test
    void splitsTextIntoOverlappingWindows() {
        FixedSizeChunker chunker = new FixedSizeChunker(10, 3);
        String text = "abcdefghijklmnopqrst"; // 20 chars

        List<Chunk> chunks = chunker.chunk(text);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks.get(0).text()).hasSize(10);
        // step = chunkSize - overlap = 7, so chunk 2 starts at offset 7
        assertThat(chunks.get(1).text()).startsWith(text.substring(7, 10));
    }

    @Test
    void returnsEmptyListForBlankInput() {
        FixedSizeChunker chunker = new FixedSizeChunker(10, 2);
        assertThat(chunker.chunk("   ")).isEmpty();
        assertThat(chunker.chunk(null)).isEmpty();
    }

    @Test
    void rejectsOverlapGreaterOrEqualToChunkSize() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> new FixedSizeChunker(10, 10));
    }
}
