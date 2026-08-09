package com.quantedge.backend.rag.eval;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.quantedge.backend.rag.eval.EvalMetrics.QueryResult;
import org.junit.jupiter.api.Test;

class EvalMetricsTest {

    @Test
    void hitAtKIsTrueWhenGoldDocWithinTopK() {
        List<String> ranked = List.of("doc-b", "doc-a", "doc-c");
        assertThat(EvalMetrics.hitAtK(ranked, "doc-a", 2)).isTrue();
        assertThat(EvalMetrics.hitAtK(ranked, "doc-a", 1)).isFalse();
    }

    @Test
    void reciprocalRankUsesFirstOccurrenceAfterDedupe() {
        List<String> ranked = List.of("doc-a", "doc-a", "doc-b");
        assertThat(EvalMetrics.reciprocalRank(ranked, "doc-b")).isEqualTo(0.5);
        assertThat(EvalMetrics.reciprocalRank(ranked, "doc-a")).isEqualTo(1.0);
        assertThat(EvalMetrics.reciprocalRank(ranked, "doc-missing")).isEqualTo(0.0);
    }

    @Test
    void recallAtKAveragesAcrossQueries() {
        List<QueryResult> results = List.of(
                new QueryResult("q1", List.of("doc-a", "doc-b"), "doc-a"),
                new QueryResult("q2", List.of("doc-b", "doc-c"), "doc-a"));
        assertThat(EvalMetrics.recallAtK(results, 5)).isEqualTo(0.5);
    }

    @Test
    void meanReciprocalRankAveragesAcrossQueries() {
        List<QueryResult> results = List.of(
                new QueryResult("q1", List.of("doc-a"), "doc-a"),
                new QueryResult("q2", List.of("doc-x", "doc-a"), "doc-a"));
        assertThat(EvalMetrics.meanReciprocalRank(results)).isEqualTo((1.0 + 0.5) / 2);
    }
}
