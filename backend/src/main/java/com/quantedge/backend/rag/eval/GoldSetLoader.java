package com.quantedge.backend.rag.eval;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Loads the hand-labeled gold set ({@code rag/gold_set.json}) the eval harness scores retrieval against. */
@Component
public class GoldSetLoader {

    private static final String GOLD_SET_PATH = "rag/gold_set.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<GoldExample> load() {
        try (InputStream in = new ClassPathResource(GOLD_SET_PATH).getInputStream()) {
            return objectMapper.readValue(
                    in, objectMapper.getTypeFactory().constructCollectionType(List.class, GoldExample.class));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load RAG gold set: " + GOLD_SET_PATH, e);
        }
    }
}
