package com.quantedge.backend.rag.chunking;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits on a priority list of separators (paragraph, then line, then sentence, then word),
 * greedily merging pieces back up to {@code chunkSize} so chunk boundaries land on natural
 * text breaks instead of arbitrary character offsets. Falls back to a hard character cut only
 * if a single unsplittable piece still exceeds {@code chunkSize}.
 */
public class RecursiveChunker implements Chunker {

    private static final String[] SEPARATORS = {"\n\n", "\n", ". ", " "};

    private final int chunkSize;
    private final int overlap;

    public RecursiveChunker(int chunkSize, int overlap) {
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
        List<String> pieces = split(text.strip(), 0);
        List<String> merged = mergeWithOverlap(pieces);
        int index = 0;
        for (String piece : merged) {
            String stripped = piece.strip();
            if (!stripped.isEmpty()) {
                chunks.add(new Chunk(stripped, index++));
            }
        }
        return chunks;
    }

    private List<String> split(String text, int separatorIndex) {
        if (text.length() <= chunkSize) {
            return List.of(text);
        }
        if (separatorIndex >= SEPARATORS.length) {
            return hardSplit(text);
        }
        String separator = SEPARATORS[separatorIndex];
        if (!text.contains(separator)) {
            return split(text, separatorIndex + 1);
        }
        List<String> result = new ArrayList<>();
        for (String part : text.split(java.util.regex.Pattern.quote(separator))) {
            if (part.isBlank()) {
                continue;
            }
            if (part.length() > chunkSize) {
                result.addAll(split(part, separatorIndex + 1));
            } else {
                result.add(part);
            }
        }
        return result;
    }

    private List<String> hardSplit(String text) {
        List<String> result = new ArrayList<>();
        for (int start = 0; start < text.length(); start += chunkSize) {
            result.add(text.substring(start, Math.min(start + chunkSize, text.length())));
        }
        return result;
    }

    /** Greedily packs adjacent pieces up to chunkSize, then repeats the tail of each chunk as the head of the next for overlap. */
    private List<String> mergeWithOverlap(List<String> pieces) {
        List<String> merged = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String piece : pieces) {
            if (current.isEmpty()) {
                current.append(piece);
            } else if (current.length() + 1 + piece.length() <= chunkSize) {
                current.append(' ').append(piece);
            } else {
                merged.add(current.toString());
                String tail = overlapTail(current.toString());
                current = new StringBuilder(tail.isEmpty() ? piece : tail + " " + piece);
            }
        }
        if (!current.isEmpty()) {
            merged.add(current.toString());
        }
        return merged;
    }

    private String overlapTail(String chunkText) {
        if (overlap == 0 || chunkText.length() <= overlap) {
            return "";
        }
        return chunkText.substring(chunkText.length() - overlap);
    }

    @Override
    public ChunkingStrategy strategy() {
        return ChunkingStrategy.RECURSIVE;
    }
}
