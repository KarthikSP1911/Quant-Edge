package com.quantedge.backend.rag.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ChunkerFactory and QdrantVectorStoreFactory require an EmbeddingModel bean unconditionally, so
 * @SpringBootTest classes (WatchlistControllerTest, SecurityIntegrationTest, etc.) boot it too.
 * The real bean downloads a ~90MB ONNX model from GitHub, which the "fast, no-infra" test
 * profile must not depend on. This stub replaces it with a deterministic, offline vector
 * generator - RAG/embedding correctness is covered separately by the eval harness.
 */
@Configuration
class FakeEmbeddingModelConfig {

    private static final int DIMENSION = 384;

    @Bean
    EmbeddingModel embeddingModel() {
        return new EmbeddingModel() {

            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                List<Embedding> embeddings = new ArrayList<>();
                List<String> inputs = request.getInstructions();
                for (int i = 0; i < inputs.size(); i++) {
                    embeddings.add(new Embedding(fakeVector(inputs.get(i)), i));
                }
                return new EmbeddingResponse(embeddings);
            }

            @Override
            public float[] embed(Document document) {
                return fakeVector(document.getText());
            }

            private float[] fakeVector(String text) {
                int seed = text == null ? 0 : text.hashCode();
                float[] vector = new float[DIMENSION];
                for (int i = 0; i < DIMENSION; i++) {
                    vector[i] = (float) Math.sin(seed + i);
                }
                return vector;
            }
        };
    }
}
