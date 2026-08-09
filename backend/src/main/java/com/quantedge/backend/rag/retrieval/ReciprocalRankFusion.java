package com.quantedge.backend.rag.retrieval;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fuses a dense-search ranking and a sparse (BM25) ranking into one ranking via Reciprocal Rank
 * Fusion: {@code score(chunk) = sum over lists containing it of 1 / (k + rank)}. Rank-based, so
 * it needs no score normalization between cosine-similarity and BM25 scores, which live on
 * different scales.
 */
public final class ReciprocalRankFusion {

    private static final int DEFAULT_K = 60;

    private ReciprocalRankFusion() {}

    public static List<KnowledgeChunkResult> fuse(
            List<KnowledgeChunkResult> denseResults, List<KnowledgeChunkResult> sparseResults, int topK) {
        Map<String, Double> fusedScores = new LinkedHashMap<>();
        Map<String, KnowledgeChunkResult> byKey = new LinkedHashMap<>();
        addRanked(denseResults, fusedScores, byKey);
        addRanked(sparseResults, fusedScores, byKey);

        List<KnowledgeChunkResult> merged = new ArrayList<>(byKey.values());
        merged.sort((a, b) -> Double.compare(fusedScores.get(key(b)), fusedScores.get(key(a))));
        if (merged.size() > topK) {
            merged = merged.subList(0, topK);
        }
        List<KnowledgeChunkResult> rescored = new ArrayList<>(merged.size());
        for (KnowledgeChunkResult chunk : merged) {
            rescored.add(new KnowledgeChunkResult(
                    chunk.docId(),
                    chunk.sourceType(),
                    chunk.symbol(),
                    chunk.title(),
                    chunk.text(),
                    fusedScores.get(key(chunk))));
        }
        return rescored;
    }

    private static void addRanked(
            List<KnowledgeChunkResult> results,
            Map<String, Double> fusedScores,
            Map<String, KnowledgeChunkResult> byKey) {
        for (int i = 0; i < results.size(); i++) {
            KnowledgeChunkResult chunk = results.get(i);
            String key = key(chunk);
            byKey.putIfAbsent(key, chunk);
            fusedScores.merge(key, 1.0 / (DEFAULT_K + i + 1), Double::sum);
        }
    }

    private static String key(KnowledgeChunkResult chunk) {
        return chunk.docId() + "::" + chunk.text();
    }
}
