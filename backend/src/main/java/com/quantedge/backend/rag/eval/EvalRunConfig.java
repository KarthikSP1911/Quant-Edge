package com.quantedge.backend.rag.eval;

import com.quantedge.backend.rag.chunking.ChunkingStrategy;

/**
 * One eval combo's parameters, parsed from CLI args: {@code strategy hybrid rerank [chunkSize] [chunkOverlap] [label]}.
 * Example: {@code RECURSIVE false false} (dense-only baseline) or {@code SEMANTIC true true 800 120 semantic-hybrid-rerank}.
 */
public record EvalRunConfig(
        ChunkingStrategy strategy, boolean hybrid, boolean rerank, int chunkSize, int chunkOverlap, String label) {

    public static EvalRunConfig parse(String[] args) {
        if (args.length < 3) {
            throw new IllegalArgumentException(
                    "Usage: EvalRunner <FIXED|RECURSIVE|SEMANTIC> <hybrid true|false> <rerank true|false> [chunkSize=800] [chunkOverlap=120] [label]");
        }
        ChunkingStrategy strategy = ChunkingStrategy.valueOf(args[0].toUpperCase());
        boolean hybrid = Boolean.parseBoolean(args[1]);
        boolean rerank = Boolean.parseBoolean(args[2]);
        int chunkSize = args.length > 3 ? Integer.parseInt(args[3]) : 800;
        int chunkOverlap = args.length > 4 ? Integer.parseInt(args[4]) : 120;
        String label = args.length > 5
                ? args[5]
                : (strategy.name().toLowerCase() + (hybrid ? "-hybrid" : "-dense") + (rerank ? "-rerank" : ""));
        return new EvalRunConfig(strategy, hybrid, rerank, chunkSize, chunkOverlap, label);
    }

    public String collectionName() {
        return "quantedge_eval_" + label.replaceAll("[^a-z0-9_]", "_");
    }
}
