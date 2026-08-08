package com.quantedge.backend.rag.retrieval;

import com.quantedge.backend.rag.ingest.KnowledgeIngestionService;
import com.quantedge.backend.rag.ingest.SourceType;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

/**
 * Dense (embedding cosine-similarity) retrieval over a Qdrant collection. This is the baseline
 * every technique in phase-5/rag-technique-experiments (hybrid dense+BM25, LLM rerank) has to
 * beat - see that branch and the README's Retrieval Evaluation section for the comparison.
 */
@Service
public class KnowledgeBaseService {

    public List<KnowledgeChunkResult> denseSearch(VectorStore vectorStore, String query, int topK) {
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(topK).build());
        return results.stream().map(this::toResult).toList();
    }

    private KnowledgeChunkResult toResult(Document document) {
        return new KnowledgeChunkResult(
                (String) document.getMetadata().get(KnowledgeIngestionService.META_DOC_ID),
                SourceType.valueOf((String) document.getMetadata().get(KnowledgeIngestionService.META_SOURCE_TYPE)),
                (String) document.getMetadata().get(KnowledgeIngestionService.META_SYMBOL),
                (String) document.getMetadata().get(KnowledgeIngestionService.META_TITLE),
                document.getText(),
                document.getScore() == null ? 0.0 : document.getScore());
    }
}
