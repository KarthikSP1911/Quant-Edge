package com.quantedge.backend.rag.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Wires the Qdrant Cloud client and the default production knowledge-base collection.
 *
 * <p>The eval harness needs a fresh Qdrant collection per chunking/retrieval config, so it uses
 * {@link QdrantVectorStoreFactory} directly instead of this bean - this bean only covers the
 * single collection the live {@code queryKnowledgeBase} chat tool queries.
 */
@Configuration
public class RagConfig {

    @Value("${quantedge.rag.qdrant.url}")
    private String qdrantUrl;

    @Value("${quantedge.rag.qdrant.api-key}")
    private String qdrantApiKey;

    @Value("${quantedge.rag.qdrant.grpc-port}")
    private int grpcPort;

    @Value("${quantedge.rag.qdrant.collection}")
    private String defaultCollection;

    @Bean(destroyMethod = "close")
    public QdrantClient qdrantClient() {
        String host = qdrantUrl.replaceFirst("^https?://", "").replaceAll("/$", "");
        boolean useTls = qdrantUrl.startsWith("https");
        QdrantGrpcClient grpcClient = QdrantGrpcClient.newBuilder(host, grpcPort, useTls)
                .withApiKey(qdrantApiKey)
                .build();
        return new QdrantClient(grpcClient);
    }

    @Bean
    public QdrantVectorStoreFactory qdrantVectorStoreFactory(QdrantClient qdrantClient, EmbeddingModel embeddingModel) {
        return new QdrantVectorStoreFactory(qdrantClient, embeddingModel);
    }

    @Bean
    @Primary
    public VectorStore knowledgeBaseVectorStore(QdrantVectorStoreFactory factory) {
        return factory.forCollection(defaultCollection, true);
    }
}
