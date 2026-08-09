package com.quantedge.backend.rag.ingest;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Loads the frozen fixture corpora ({@code rag/news_corpus.json}, {@code rag/research_notes_corpus.json})
 * used by the retrieval eval harness and by the demo {@code queryKnowledgeBase} tool. The corpus is
 * frozen (not pulled live from Finnhub/Postgres) so gold-set labels stay valid across runs - see
 * README's Retrieval Evaluation section.
 */
@Component
public class CorpusLoader {

    private static final String NEWS_PATH = "rag/news_corpus.json";
    private static final String NOTES_PATH = "rag/research_notes_corpus.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<KnowledgeDocument> loadAll() {
        List<KnowledgeDocument> documents = new ArrayList<>();
        documents.addAll(load(NEWS_PATH, SourceType.NEWS));
        documents.addAll(load(NOTES_PATH, SourceType.RESEARCH_NOTE));
        return documents;
    }

    private List<KnowledgeDocument> load(String classpathLocation, SourceType sourceType) {
        try (InputStream in = new ClassPathResource(classpathLocation).getInputStream()) {
            List<RawEntry> raw = objectMapper.readValue(
                    in, objectMapper.getTypeFactory().constructCollectionType(List.class, RawEntry.class));
            return raw.stream()
                    .map(entry -> new KnowledgeDocument(entry.id, sourceType, entry.symbol, entry.title, entry.text))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load RAG corpus fixture: " + classpathLocation, e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RawEntry(String id, String symbol, String title, String text) {}
}
