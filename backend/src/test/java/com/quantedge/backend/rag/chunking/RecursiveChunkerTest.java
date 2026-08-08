package com.quantedge.backend.rag.chunking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RecursiveChunkerTest {

    @Test
    void keepsShortTextAsSingleChunk() {
        RecursiveChunker chunker = new RecursiveChunker(500, 50);
        List<Chunk> chunks = chunker.chunk("A short paragraph that fits in one chunk.");
        assertThat(chunks).hasSize(1);
    }

    @Test
    void splitsOnParagraphBoundariesBeforeHardCutting() {
        RecursiveChunker chunker = new RecursiveChunker(60, 0);
        String text = "First paragraph is reasonably long here.\n\n"
                + "Second paragraph is also fairly long here.\n\n"
                + "Third paragraph closes things out nicely.";

        List<Chunk> chunks = chunker.chunk(text);

        assertThat(chunks).hasSizeGreaterThan(1);
        for (Chunk chunk : chunks) {
            assertThat(chunk.text()).doesNotContain("\n\n");
        }
    }

    @Test
    void hardSplitsSingleUnbreakableTokenLongerThanChunkSize() {
        RecursiveChunker chunker = new RecursiveChunker(10, 0);
        String unbreakable = "a".repeat(35);
        List<Chunk> chunks = chunker.chunk(unbreakable);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.text().length()).isLessThanOrEqualTo(10));
    }
}
