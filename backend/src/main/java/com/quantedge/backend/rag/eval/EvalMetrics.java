package com.quantedge.backend.rag.eval;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Recall@k and MRR against a gold set of (ranked retrieved docIds, correct docId) pairs. */
public final class EvalMetrics {

    private EvalMetrics() {}

    /** Retrieved docIds in rank order (duplicates from multiple chunks of the same doc collapsed to their first rank). */
    public static List<String> dedupePreserveOrder(List<String> rankedDocIds) {
        return new ArrayList<>(new LinkedHashSet<>(rankedDocIds));
    }

    public static boolean hitAtK(List<String> rankedDocIds, String goldDocId, int k) {
        List<String> deduped = dedupePreserveOrder(rankedDocIds);
        int limit = Math.min(k, deduped.size());
        for (int i = 0; i < limit; i++) {
            if (deduped.get(i).equals(goldDocId)) {
                return true;
            }
        }
        return false;
    }

    public static double reciprocalRank(List<String> rankedDocIds, String goldDocId) {
        List<String> deduped = dedupePreserveOrder(rankedDocIds);
        for (int i = 0; i < deduped.size(); i++) {
            if (deduped.get(i).equals(goldDocId)) {
                return 1.0 / (i + 1);
            }
        }
        return 0.0;
    }

    public static double recallAtK(List<QueryResult> queryResults, int k) {
        if (queryResults.isEmpty()) {
            return 0.0;
        }
        long hits = queryResults.stream()
                .filter(r -> hitAtK(r.rankedDocIds(), r.goldDocId(), k))
                .count();
        return (double) hits / queryResults.size();
    }

    public static double meanReciprocalRank(List<QueryResult> queryResults) {
        if (queryResults.isEmpty()) {
            return 0.0;
        }
        double sum = queryResults.stream()
                .mapToDouble(r -> reciprocalRank(r.rankedDocIds(), r.goldDocId()))
                .sum();
        return sum / queryResults.size();
    }

    /** One query's ranked retrieval result paired with its gold answer, ready for scoring. */
    public record QueryResult(String question, List<String> rankedDocIds, String goldDocId) {}
}
