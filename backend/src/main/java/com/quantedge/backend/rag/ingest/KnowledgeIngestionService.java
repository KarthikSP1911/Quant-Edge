package com.quantedge.backend.rag.ingest;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.quantedge.backend.rag.chunking.Chunk;
import com.quantedge.backend.rag.chunking.Chunker;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

/**
 * Chunks a corpus of {@link KnowledgeDocument}s into Spring AI {@link Document}s and upserts them
 * into a Qdrant collection. Chunking and storing are separate steps so callers that also need a
 * sparse (BM25) index - the eval harness's hybrid-retrieval runs - can build it from the exact
 * same chunk list that was embedded, rather than re-chunking separately.
 */
@Service
public class KnowledgeIngestionService {

    public static final String META_DOC_ID = "docId";
    public static final String META_SOURCE_TYPE = "sourceType";
    public static final String META_SYMBOL = "symbol";
    public static final String META_TITLE = "title";
    public static final String META_CHUNK_INDEX = "chunkIndex";
    public static final String META_SOURCE = "source";
    public static final String META_URL = "url";
    public static final String META_PUBLISHED_AT = "publishedAt";

    private static final int BATCH_SIZE = 32;

    public List<Document> chunkCorpus(List<KnowledgeDocument> corpus, Chunker chunker) {
        List<Document> documents = new ArrayList<>();
        for (KnowledgeDocument doc : corpus) {
            for (Chunk chunk : chunker.chunk(doc.text())) {
                documents.add(toDocument(doc, chunk));
            }
        }
        return documents;
    }

    /**
     * Builds a Spring AI {@link Document} for one chunk with a deterministic point ID derived from
     * the source document's ID and chunk index, so re-ingesting the same article (a scheduled
     * re-run after a crash, or a retry) upserts the same Qdrant points instead of creating
     * duplicates - dedup by construction, not just by a pre-check.
     */
    private Document toDocument(KnowledgeDocument doc, Chunk chunk) {
        String pointId = UUID.nameUUIDFromBytes((doc.id() + "::" + chunk.index()).getBytes(StandardCharsets.UTF_8))
                .toString();
        Document.Builder builder = Document.builder()
                .id(pointId)
                .text(chunk.text())
                .metadata(META_DOC_ID, doc.id())
                .metadata(META_SOURCE_TYPE, doc.sourceType().name())
                .metadata(META_SYMBOL, doc.symbol())
                .metadata(META_TITLE, doc.title())
                .metadata(META_CHUNK_INDEX, chunk.index());
        if (doc.source() != null) {
            builder.metadata(META_SOURCE, doc.source());
        }
        if (doc.url() != null) {
            builder.metadata(META_URL, doc.url());
        }
        if (doc.publishedAt() != null) {
            builder.metadata(META_PUBLISHED_AT, doc.publishedAt().toEpochMilli());
        }
        return builder.build();
    }

    /** Reads the {@link #META_PUBLISHED_AT} metadata value back as epoch millis, or null if absent. */
    public static Long readPublishedAt(Map<String, Object> metadata) {
        Object value = metadata.get(META_PUBLISHED_AT);
        return value instanceof Number number ? number.longValue() : null;
    }

    public void store(List<Document> documents, VectorStore vectorStore) {
        for (int start = 0; start < documents.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, documents.size());
            vectorStore.add(documents.subList(start, end));
        }
    }

    public int ingest(List<KnowledgeDocument> corpus, Chunker chunker, VectorStore vectorStore) {
        List<Document> documents = chunkCorpus(corpus, chunker);
        store(documents, vectorStore);
        return documents.size();
    }
}
