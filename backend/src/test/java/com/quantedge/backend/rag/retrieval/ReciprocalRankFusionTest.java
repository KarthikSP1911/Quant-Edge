package com.quantedge.backend.rag.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import com.quantedge.backend.rag.ingest.SourceType;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReciprocalRankFusionTest {

    private static KnowledgeChunkResult chunk(String docId, double score) {
        return new KnowledgeChunkResult(docId, SourceType.NEWS, "AAPL", "title-" + docId, "text-" + docId, score);
    }

    @Test
    void chunkRankedFirstInBothListsWinsFusion() {
        List<KnowledgeChunkResult> dense = List.of(chunk("doc-a", 0.9), chunk("doc-b", 0.8), chunk("doc-c", 0.7));
        List<KnowledgeChunkResult> sparse = List.of(chunk("doc-a", 5.0), chunk("doc-c", 4.0), chunk("doc-b", 3.0));

        List<KnowledgeChunkResult> fused = ReciprocalRankFusion.fuse(dense, sparse, 3);

        assertThat(fused).hasSize(3);
        assertThat(fused.get(0).docId()).isEqualTo("doc-a");
    }

    @Test
    void chunkOnlyInOneListStillAppearsInFusedResult() {
        List<KnowledgeChunkResult> dense = List.of(chunk("doc-a", 0.9));
        List<KnowledgeChunkResult> sparse = List.of(chunk("doc-b", 5.0));

        List<KnowledgeChunkResult> fused = ReciprocalRankFusion.fuse(dense, sparse, 5);

        assertThat(fused).extracting(KnowledgeChunkResult::docId).containsExactlyInAnyOrder("doc-a", "doc-b");
    }

    @Test
    void respectsTopKLimit() {
        List<KnowledgeChunkResult> dense = List.of(chunk("doc-a", 0.9), chunk("doc-b", 0.8), chunk("doc-c", 0.7));
        List<KnowledgeChunkResult> sparse = List.of();

        List<KnowledgeChunkResult> fused = ReciprocalRankFusion.fuse(dense, sparse, 2);

        assertThat(fused).hasSize(2);
    }
}
