package com.aidocpipeline.queryservice.service;

import com.aidocpipeline.queryservice.dto.QueryRequest;
import com.aidocpipeline.queryservice.dto.QueryResponse;
import com.aidocpipeline.queryservice.entity.Document;
import com.aidocpipeline.queryservice.repository.DocumentChunkRepository;
import com.aidocpipeline.queryservice.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QueryServiceTest {

    @Mock private DocumentChunkRepository chunkRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private EmbeddingService embeddingService;
    @Mock private LlmService llmService;
    @Mock private QueryMetrics queryMetrics;
    @Mock private io.micrometer.core.instrument.Timer.Sample timerSample;

    @InjectMocks
    private QueryService queryService;

    private UUID documentId;
    private Document document;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(queryService, "topK", 5);
        ReflectionTestUtils.setField(queryService, "similarityThreshold", 0.0);

        documentId = UUID.randomUUID();

        document = new Document();
        document.setId(documentId);
        document.setFileName("test.pdf");
        document.setStatus("READY");

        // Timer mock setup
        when(queryMetrics.startQueryTimer()).thenReturn(timerSample);
    }

    @Test
    @DisplayName("Should return a valid query response with citations")
    void shouldReturnQueryResponseWithCitations() {
        // Arrange
        QueryRequest request = QueryRequest.builder()
                .question("What are the technical skills?")
                .tenantId("default")
                .build();

        float[] mockVector = new float[1024];
        String mockVectorString = "[0.1,0.2,0.3]";

        // Simulate one matching chunk returned from pgvector
        Object[] rawRow = new Object[]{
                UUID.randomUUID(),  // chunk id
                documentId,         // document id
                0,                  // chunk index
                "Java, Spring Boot, Kubernetes are the main skills.",  // content
                1,                  // page number
                0.87                // similarity score
        };

        List<Object[]> rawRowList = new ArrayList<>();
        rawRowList.add(rawRow);

        when(embeddingService.embedQuery(anyString())).thenReturn(mockVector);
        when(embeddingService.toVectorString(mockVector)).thenReturn(mockVectorString);
        when(chunkRepository.findSimilarChunks(anyString(), anyInt(), anyDouble()))
                .thenReturn(rawRowList);
        when(documentRepository.findById(documentId))
                .thenReturn(Optional.of(document));
        when(llmService.generateAnswer(anyString(), anyList()))
                .thenReturn("The technical skills include Java, Spring Boot, and Kubernetes.");

        // Act
        QueryResponse response = queryService.query(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getQuestion())
                .isEqualTo("What are the technical skills?");
        assertThat(response.getAnswer()).isNotBlank();
        assertThat(response.getCitations()).hasSize(1);
        assertThat(response.getCitations().get(0).getDocumentName())
                .isEqualTo("test.pdf");
        assertThat(response.getCitations().get(0).getSimilarityScore())
                .isGreaterThan(0.0);
        assertThat(response.getProcessingTimeMs()).isGreaterThanOrEqualTo(0);

        verify(queryMetrics, times(1)).incrementQueries();
        verify(queryMetrics, times(1)).recordCitations(1);
    }

    @Test
    @DisplayName("Should return empty citations when no matching chunks found")
    void shouldReturnEmptyCitationsWhenNoMatches() {
        QueryRequest request = QueryRequest.builder()
                .question("What is the meaning of life?")
                .tenantId("default")
                .build();

        float[] mockVector = new float[1024];

        when(embeddingService.embedQuery(anyString())).thenReturn(mockVector);
        when(embeddingService.toVectorString(mockVector)).thenReturn("[0.1,0.2]");
        when(chunkRepository.findSimilarChunks(anyString(), anyInt(), anyDouble()))
                .thenReturn(List.of());
        when(llmService.generateAnswer(anyString(), anyList()))
                .thenReturn("I could not find relevant information.");

        QueryResponse response = queryService.query(request);

        assertThat(response.getCitations()).isEmpty();
        assertThat(response.getTotalChunksSearched()).isZero();
        verify(queryMetrics, times(1)).incrementNoResults();
    }

    @Test
    @DisplayName("Should filter by document IDs when provided")
    void shouldFilterByDocumentIdsWhenProvided() {
        QueryRequest request = QueryRequest.builder()
                .question("What is the salary?")
                .documentIds(List.of(documentId))
                .tenantId("default")
                .build();

        float[] mockVector = new float[1024];

        when(embeddingService.embedQuery(anyString())).thenReturn(mockVector);
        when(embeddingService.toVectorString(mockVector)).thenReturn("[0.1]");
        when(chunkRepository.findSimilarChunksInDocuments(
                anyString(), anyList(), anyInt(), anyDouble()))
                .thenReturn(List.of());
        when(llmService.generateAnswer(anyString(), anyList()))
                .thenReturn("No salary information found.");

        queryService.query(request);

        // Should use the filtered query, not the global one
        verify(chunkRepository, times(1))
                .findSimilarChunksInDocuments(anyString(), anyList(), anyInt(), anyDouble());
        verify(chunkRepository, never())
                .findSimilarChunks(anyString(), anyInt(), anyDouble());
    }

    @Test
    @DisplayName("Should always increment query counter regardless of results")
    void shouldAlwaysIncrementQueryCounter() {
        QueryRequest request = QueryRequest.builder()
                .question("Any question")
                .build();

        when(embeddingService.embedQuery(anyString())).thenReturn(new float[1024]);
        when(embeddingService.toVectorString(any())).thenReturn("[0.1]");
        when(chunkRepository.findSimilarChunks(anyString(), anyInt(), anyDouble()))
                .thenReturn(List.of());
        when(llmService.generateAnswer(anyString(), anyList()))
                .thenReturn("No results.");

        queryService.query(request);

        verify(queryMetrics, times(1)).incrementQueries();
    }
}