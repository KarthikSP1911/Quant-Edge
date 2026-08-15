package com.quantedge.backend.rag.retrieval;

import java.util.ArrayList;
import java.util.List;

import com.quantedge.backend.rag.ingest.SourceType;

/**
 * Nudges {@link SourceType#NEWS} chunks up in ranking as they get fresher, without ever discarding
 * older ones - CLAUDE.md/product requirement is that retrieval *prefers* recent news for
 * current-events questions, not that older articles become unreachable just because a newer one
 * disagrees with them (that's what {@code publishedAt} in the prompt context is for; the model
 * decides how to weigh conflicting dated sources).
 *
 * <p>Multiplicative boost, not a hard recency filter: a highly relevant six-month-old chunk can
 * still outrank a barely-relevant one-day-old chunk. Research notes and chunks with no
 * {@code publishedAt} (the frozen fixture corpus) are left at their original score.
 */
public final class RecencyRanking {

    private static final double RECENCY_WEIGHT = 0.5;
    private static final double HALF_LIFE_DAYS = 7.0;
    private static final double MILLIS_PER_DAY = 86_400_000.0;

    private RecencyRanking() {}

    public static List<KnowledgeChunkResult> boost(List<KnowledgeChunkResult> results) {
        long now = System.currentTimeMillis();
        List<KnowledgeChunkResult> boosted = new ArrayList<>(results.size());
        for (KnowledgeChunkResult result : results) {
            double factor = recencyFactor(result, now);
            boosted.add(
                    factor == 1.0
                            ? result
                            : new KnowledgeChunkResult(
                                    result.docId(),
                                    result.sourceType(),
                                    result.symbol(),
                                    result.title(),
                                    result.text(),
                                    result.score() * factor,
                                    result.source(),
                                    result.url(),
                                    result.publishedAt()));
        }
        // Stable sort: chunks with an equal (or absent) boost factor keep their original relative
        // order, so this is a no-op for corpora with no publishedAt (e.g. the eval fixture corpus).
        boosted.sort((a, b) -> Double.compare(b.score(), a.score()));
        return boosted;
    }

    private static double recencyFactor(KnowledgeChunkResult result, long now) {
        if (result.sourceType() != SourceType.NEWS || result.publishedAt() == null) {
            return 1.0;
        }
        double ageDays = Math.max(0, (now - result.publishedAt()) / MILLIS_PER_DAY);
        return 1.0 + RECENCY_WEIGHT * Math.exp(-ageDays / HALF_LIFE_DAYS);
    }
}
