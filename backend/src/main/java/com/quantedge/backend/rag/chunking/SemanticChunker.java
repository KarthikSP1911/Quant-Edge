package com.quantedge.backend.rag.chunking;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.ai.embedding.EmbeddingModel;

/**
 * Embeds each sentence and breaks between sentences whose cosine similarity to the previous
 * sentence drops more than {@code breakpointStdDevMultiplier} standard deviations below the
 * document's mean sentence-to-sentence similarity - a topic-shift signal, rather than a fixed
 * character count. {@code maxChunkSize} is a hard cap so one unusually uniform document can't
 * produce one giant chunk.
 */
public class SemanticChunker implements Chunker {

    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[.!?])\\s+");

    private final EmbeddingModel embeddingModel;
    private final int maxChunkSize;
    private final double breakpointStdDevMultiplier;

    public SemanticChunker(EmbeddingModel embeddingModel, int maxChunkSize, double breakpointStdDevMultiplier) {
        this.embeddingModel = embeddingModel;
        this.maxChunkSize = maxChunkSize;
        this.breakpointStdDevMultiplier = breakpointStdDevMultiplier;
    }

    @Override
    public List<Chunk> chunk(String text) {
        List<Chunk> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }
        List<String> sentences = splitSentences(text.strip());
        if (sentences.size() <= 1) {
            chunks.add(new Chunk(text.strip(), 0));
            return chunks;
        }

        List<float[]> embeddings = embeddingModel.embed(sentences);
        double[] similarities = new double[sentences.size() - 1];
        for (int i = 0; i < similarities.length; i++) {
            similarities[i] = cosineSimilarity(embeddings.get(i), embeddings.get(i + 1));
        }
        double threshold = mean(similarities) - breakpointStdDevMultiplier * stdDev(similarities);

        List<String> current = new ArrayList<>();
        int currentLength = 0;
        int index = 0;
        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            boolean isBreakpoint =
                    i > 0 && (similarities[i - 1] < threshold || currentLength + sentence.length() > maxChunkSize);
            if (isBreakpoint && !current.isEmpty()) {
                chunks.add(new Chunk(String.join(" ", current), index++));
                current = new ArrayList<>();
                currentLength = 0;
            }
            current.add(sentence);
            currentLength += sentence.length() + 1;
        }
        if (!current.isEmpty()) {
            chunks.add(new Chunk(String.join(" ", current), index));
        }
        return chunks;
    }

    private List<String> splitSentences(String text) {
        List<String> sentences = new ArrayList<>();
        for (String s : SENTENCE_BOUNDARY.split(text)) {
            if (!s.isBlank()) {
                sentences.add(s.strip());
            }
        }
        return sentences;
    }

    private static double cosineSimilarity(float[] a, float[] b) {
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private static double mean(double[] values) {
        double sum = 0;
        for (double v : values) {
            sum += v;
        }
        return values.length == 0 ? 0 : sum / values.length;
    }

    private static double stdDev(double[] values) {
        double mean = mean(values);
        double sumSquares = 0;
        for (double v : values) {
            sumSquares += (v - mean) * (v - mean);
        }
        return values.length == 0 ? 0 : Math.sqrt(sumSquares / values.length);
    }

    @Override
    public ChunkingStrategy strategy() {
        return ChunkingStrategy.SEMANTIC;
    }
}
