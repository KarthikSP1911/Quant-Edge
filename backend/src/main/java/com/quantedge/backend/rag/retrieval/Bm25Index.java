package com.quantedge.backend.rag.retrieval;

import com.quantedge.backend.rag.ingest.KnowledgeIngestionService;
import com.quantedge.backend.rag.ingest.SourceType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.ai.document.Document;

/**
 * In-memory BM25 (Okapi) sparse index over the same chunk set that was embedded into Qdrant - the
 * sparse half of dense+sparse hybrid retrieval. Rebuilt per eval run rather than persisted, since
 * it's cheap to compute and only the eval harness's hybrid mode needs it.
 */
public class Bm25Index {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[a-z0-9]+");
    private static final double K1 = 1.5;
    private static final double B = 0.75;

    private final List<Document> documents;
    private final List<List<String>> tokensPerDoc;
    private final Map<String, Integer> documentFrequency = new HashMap<>();
    private final double averageDocLength;

    private Bm25Index(List<Document> documents) {
        this.documents = documents;
        this.tokensPerDoc = new ArrayList<>(documents.size());
        long totalLength = 0;
        for (Document doc : documents) {
            List<String> tokens = tokenize(doc.getText());
            tokensPerDoc.add(tokens);
            totalLength += tokens.size();
            for (String term : new java.util.HashSet<>(tokens)) {
                documentFrequency.merge(term, 1, Integer::sum);
            }
        }
        this.averageDocLength = documents.isEmpty() ? 0 : (double) totalLength / documents.size();
    }

    public static Bm25Index build(List<Document> documents) {
        return new Bm25Index(documents);
    }

    public List<KnowledgeChunkResult> search(String query, int topK) {
        List<String> queryTerms = tokenize(query);
        int n = documents.size();
        List<ScoredIndex> scored = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            double score = score(queryTerms, i, n);
            if (score > 0) {
                scored.add(new ScoredIndex(i, score));
            }
        }
        scored.sort((a, b) -> Double.compare(b.score, a.score));
        List<KnowledgeChunkResult> results = new ArrayList<>(Math.min(topK, scored.size()));
        for (int i = 0; i < Math.min(topK, scored.size()); i++) {
            results.add(toResult(scored.get(i)));
        }
        return results;
    }

    private double score(List<String> queryTerms, int docIndex, int n) {
        List<String> docTokens = tokensPerDoc.get(docIndex);
        int docLength = docTokens.size();
        Map<String, Integer> termFrequency = new HashMap<>();
        for (String token : docTokens) {
            termFrequency.merge(token, 1, Integer::sum);
        }
        double score = 0;
        for (String term : queryTerms) {
            int df = documentFrequency.getOrDefault(term, 0);
            if (df == 0) {
                continue;
            }
            double idf = Math.log(1 + (n - df + 0.5) / (df + 0.5));
            int tf = termFrequency.getOrDefault(term, 0);
            double denominator = tf + K1 * (1 - B + B * docLength / averageDocLength);
            score += idf * (tf * (K1 + 1)) / (denominator == 0 ? 1 : denominator);
        }
        return score;
    }

    private KnowledgeChunkResult toResult(ScoredIndex scoredIndex) {
        Document doc = documents.get(scoredIndex.index);
        Map<String, Object> metadata = doc.getMetadata();
        return new KnowledgeChunkResult(
                (String) metadata.get(KnowledgeIngestionService.META_DOC_ID),
                SourceType.valueOf((String) metadata.get(KnowledgeIngestionService.META_SOURCE_TYPE)),
                (String) metadata.get(KnowledgeIngestionService.META_SYMBOL),
                (String) metadata.get(KnowledgeIngestionService.META_TITLE),
                doc.getText(),
                scoredIndex.score);
    }

    private static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null) {
            return tokens;
        }
        var matcher = TOKEN_PATTERN.matcher(text.toLowerCase());
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    private record ScoredIndex(int index, double score) {}
}
