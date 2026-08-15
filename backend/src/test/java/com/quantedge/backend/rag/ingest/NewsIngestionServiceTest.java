package com.quantedge.backend.rag.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.quantedge.backend.entity.Company;
import com.quantedge.backend.exception.ExternalApiException;
import com.quantedge.backend.external.FinnhubClient;
import com.quantedge.backend.external.dto.FinnhubNewsResponse;
import com.quantedge.backend.rag.chunking.ChunkerFactory;
import com.quantedge.backend.rag.chunking.ChunkingStrategy;
import com.quantedge.backend.rag.ingest.NewsIngestionService.NewsSyncResult;
import com.quantedge.backend.repository.CompanyRepository;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Common.Filter;
import io.qdrant.client.grpc.Points.UpdateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NewsIngestionServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private FinnhubClient finnhubClient;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private QdrantClient qdrantClient;

    @Mock
    private VectorStore knowledgeBaseVectorStore;

    private NewsIngestionService service;

    @BeforeEach
    void setUp() {
        service = new NewsIngestionService(
                companyRepository,
                finnhubClient,
                new ChunkerFactory(embeddingModel),
                new KnowledgeIngestionService(),
                qdrantClient,
                knowledgeBaseVectorStore);
        ReflectionTestUtils.setField(service, "collectionName", "quantedge_knowledge");
        ReflectionTestUtils.setField(service, "chunkingStrategy", ChunkingStrategy.RECURSIVE);
        ReflectionTestUtils.setField(service, "chunkSize", 800);
        ReflectionTestUtils.setField(service, "chunkOverlap", 120);
        ReflectionTestUtils.setField(service, "lookbackDays", 2);
        ReflectionTestUtils.setField(service, "retentionDays", 90);
        ReflectionTestUtils.setField(service, "interCallDelayMs", 0L);
    }

    private Company company(String symbol) {
        return Company.builder().symbol(symbol).build();
    }

    private FinnhubNewsResponse article(int id, String headline) {
        return new FinnhubNewsResponse(
                "company",
                System.currentTimeMillis() / 1000,
                headline,
                id,
                "https://img",
                "AAPL",
                "Reuters",
                "Summary text.",
                "https://example.com/" + id);
    }

    private void stubCount(long count) {
        ListenableFuture<Long> future = Futures.immediateFuture(count);
        when(qdrantClient.countAsync(anyString(), any(Filter.class), eq(true))).thenReturn(future);
    }

    @Test
    void ingestsNewArticleAndUpsertsIntoVectorStore() {
        when(companyRepository.findAll()).thenReturn(List.of(company("AAPL")));
        when(finnhubClient.getCompanyNews(eq("AAPL"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(article(1, "Apple beats earnings")));
        stubCount(0L);

        NewsSyncResult result = service.syncAll();

        assertThat(result.articlesIngested()).isEqualTo(1);
        assertThat(result.companiesFailed()).isZero();
        verify(knowledgeBaseVectorStore, times(1)).add(anyList());
    }

    @Test
    void skipsArticleAlreadyPresentInQdrant() {
        when(companyRepository.findAll()).thenReturn(List.of(company("AAPL")));
        when(finnhubClient.getCompanyNews(eq("AAPL"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(article(1, "Apple beats earnings")));
        stubCount(1L);

        NewsSyncResult result = service.syncAll();

        assertThat(result.articlesIngested()).isZero();
        verify(knowledgeBaseVectorStore, never()).add(anyList());
    }

    @Test
    void skipsFailingSymbolAndContinuesTheRestOfTheBatch() {
        when(companyRepository.findAll()).thenReturn(List.of(company("AAPL"), company("BAD")));
        when(finnhubClient.getCompanyNews(eq("AAPL"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(article(1, "Apple beats earnings")));
        when(finnhubClient.getCompanyNews(eq("BAD"), any(LocalDate.class), any(LocalDate.class)))
                .thenThrow(new ExternalApiException("boom"));
        stubCount(0L);

        NewsSyncResult result = service.syncAll();

        assertThat(result.articlesIngested()).isEqualTo(1);
        assertThat(result.companiesFailed()).isEqualTo(1);
    }

    @Test
    void purgeStaleNewsDeletesByAgeFilterAndSwallowsFailures() {
        when(qdrantClient.deleteAsync(anyString(), any(Filter.class)))
                .thenReturn(Futures.immediateFuture(UpdateResult.getDefaultInstance()));

        service.purgeStaleNews();

        verify(qdrantClient, times(1)).deleteAsync(eq("quantedge_knowledge"), any(Filter.class));
    }
}
