package com.quantedge.backend.rag.eval;

import com.quantedge.backend.rag.chunking.ChunkingStrategy;

/**
 * One eval combo's parameters, parsed from CLI args: {@code strategy [chunkSize] [chunkOverlap] [label]}.
 * Example: {@code RECURSIVE} (defaults) or {@code SEMANTIC 800 120 semantic-baseline}.
 */
public record EvalRunConfig(ChunkingStrategy strategy, int chunkSize, int chunkOverlap, String label) {

    public static EvalRunConfig parse(String[] args) {
        if (args.length < 1) {
            throw new IllegalArgumentException(
                    "Usage: EvalRunner <FIXED|RECURSIVE|SEMANTIC> [chunkSize=800] [chunkOverlap=120] [label]");
        }
        ChunkingStrategy strategy = ChunkingStrategy.valueOf(args[0].toUpperCase());
        int chunkSize = args.length > 1 ? Integer.parseInt(args[1]) : 800;
        int chunkOverlap = args.length > 2 ? Integer.parseInt(args[2]) : 120;
        String label = args.length > 3 ? args[3] : (strategy.name().toLowerCase() + "-dense");
        return new EvalRunConfig(strategy, chunkSize, chunkOverlap, label);
    }

    public String collectionName() {
        return "quantedge_eval_" + label.replaceAll("[^a-z0-9_]", "_");
    }
}
