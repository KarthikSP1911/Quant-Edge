package com.quantedge.backend.rag.config;

import io.qdrant.client.QdrantClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;

/**
 * Builds a {@link VectorStore} for an arbitrary Qdrant collection name, sharing one
 * {@link QdrantClient} connection. The eval harness needs a separate collection per
 * chunking/retrieval config so results from different techniques never mix.
 */
public class QdrantVectorStoreFactory {

    private final QdrantClient qdrantClient;
    private final EmbeddingModel embeddingModel;

    public QdrantVectorStoreFactory(QdrantClient qdrantClient, EmbeddingModel embeddingModel) {
        this.qdrantClient = qdrantClient;
        this.embeddingModel = embeddingModel;
    }

    public VectorStore forCollection(String collectionName, boolean initializeSchema) {
        QdrantVectorStore store = QdrantVectorStore.builder(qdrantClient, embeddingModel)
                .collectionName(collectionName)
                .initializeSchema(initializeSchema)
                .build();
        // QdrantVectorStore only creates the collection from its InitializingBean#afterPropertiesSet
        // callback, which a Spring container invokes automatically for the @Bean-managed production
        // store - EvalRunner builds this outside any container, so it must call it explicitly.
        try {
            store.afterPropertiesSet();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize Qdrant collection: " + collectionName, e);
        }
        return store;
    }
}
