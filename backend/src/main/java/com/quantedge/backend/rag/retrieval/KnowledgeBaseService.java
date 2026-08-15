package com.quantedge.backend.rag.retrieval;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.quantedge.backend.rag.ingest.KnowledgeIngestionService;
import com.quantedge.backend.rag.ingest.SourceType;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

/**
 * Retrieval over a Qdrant collection: dense-only, or dense+sparse (BM25) fused with Reciprocal
 * Rank Fusion, optionally followed by an LLM rerank pass. The eval harness runs every combination
 * of these against the gold set; the production {@code queryKnowledgeBase} tool uses dense-only.
 */
@Service
public class KnowledgeBaseService {

    private static final int HYBRID_FETCH_MULTIPLIER = 4;

    // Overfetch before applying the recency boost so a fresher-but-slightly-less-similar news
    // chunk outside the raw top-K can still surface once boosted, then trim back to topK.
    private static final int RECENCY_FETCH_MULTIPLIER = 3;

    private final LlmRerankService llmRerankService;

    public KnowledgeBaseService(LlmRerankService llmRerankService) {
        this.llmRerankService = llmRerankService;
    }

    public List<KnowledgeChunkResult> denseSearch(VectorStore vectorStore, String query, int topK) {
        List<Document> results = vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(topK * RECENCY_FETCH_MULTIPLIER)
                .build());
        List<KnowledgeChunkResult> boosted =
                RecencyRanking.boost(results.stream().map(this::toResult).toList());
        return boosted.size() > topK ? boosted.subList(0, topK) : boosted;
    }

    public List<KnowledgeChunkResult> hybridSearch(
            VectorStore vectorStore, Bm25Index bm25Index, String query, int topK) {
        int fetchK = topK * HYBRID_FETCH_MULTIPLIER;
        List<KnowledgeChunkResult> dense = denseSearch(vectorStore, query, fetchK);
        List<KnowledgeChunkResult> sparse = bm25Index.search(query, fetchK);
        return ReciprocalRankFusion.fuse(dense, sparse, topK);
    }

    public List<KnowledgeChunkResult> search(RetrievalRequest request) {
        List<KnowledgeChunkResult> candidates = request.hybrid()
                ? hybridSearch(
                        request.vectorStore(), request.bm25Index().orElseThrow(), request.query(), request.candidateK())
                : denseSearch(request.vectorStore(), request.query(), request.candidateK());
        if (!request.rerank()) {
            return candidates.size() > request.topK() ? candidates.subList(0, request.topK()) : candidates;
        }
        return llmRerankService.rerank(request.query(), candidates, request.topK());
    }

    private KnowledgeChunkResult toResult(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        return new KnowledgeChunkResult(
                (String) metadata.get(KnowledgeIngestionService.META_DOC_ID),
                SourceType.valueOf((String) metadata.get(KnowledgeIngestionService.META_SOURCE_TYPE)),
                (String) metadata.get(KnowledgeIngestionService.META_SYMBOL),
                (String) metadata.get(KnowledgeIngestionService.META_TITLE),
                document.getText(),
                document.getScore() == null ? 0.0 : document.getScore(),
                (String) metadata.get(KnowledgeIngestionService.META_SOURCE),
                (String) metadata.get(KnowledgeIngestionService.META_URL),
                KnowledgeIngestionService.readPublishedAt(metadata));
    }

    /**
     * @param candidateK how many candidates to fetch before an optional rerank narrows down to topK - a
     *     wider candidate pool for rerank to choose from than the final topK returned to the caller.
     */
    public record RetrievalRequest(
            VectorStore vectorStore,
            Optional<Bm25Index> bm25Index,
            String query,
            boolean hybrid,
            boolean rerank,
            int candidateK,
            int topK) {}
}
