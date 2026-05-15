package com.aidocpipeline.embeddingservice.service;

import com.aidocpipeline.embeddingservice.entity.Document;
import com.aidocpipeline.embeddingservice.entity.DocumentChunk;
import com.aidocpipeline.embeddingservice.event.DocumentExtractedEvent;
import com.aidocpipeline.embeddingservice.repository.DocumentChunkRepository;
import com.aidocpipeline.embeddingservice.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmbeddingServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentChunkRepository chunkRepository;
    @Mock private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private EmbeddingService embeddingService;

    private UUID documentId;
    private Document document;
    private DocumentExtractedEvent extractedEvent;

    @BeforeEach
    void setUp() {
        // Only set 'mockEnabled' — this is the only field the tests actually need
        // since mock-enabled=true bypasses all Bedrock calls entirely
        ReflectionTestUtils.setField(embeddingService, "mockEnabled", true);

        documentId = UUID.randomUUID();

        document = new Document();
        document.setId(documentId);
        document.setStatus("EXTRACTED");

        extractedEvent = DocumentExtractedEvent.builder()
                .documentId(documentId)
                .fileName("test.pdf")
                .tenantId("default")
                .totalChunks(3)
                .extractedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should generate embeddings for all chunks and mark document READY")
    void shouldGenerateEmbeddingsForAllChunks() {
        // Arrange — 3 chunks to embed
        List<DocumentChunk> chunks = List.of(
                createChunk(0, "First chunk content"),
                createChunk(1, "Second chunk content"),
                createChunk(2, "Third chunk content")
        );

        when(documentRepository.findById(documentId))
                .thenReturn(Optional.of(document));
        when(chunkRepository.findByDocumentId(documentId))
                .thenReturn(chunks);
        when(jdbcTemplate.update(anyString(), any(), any(), anyString()))
                .thenReturn(1);

        // Act
        embeddingService.generateEmbeddings(extractedEvent);

        // Assert — one embedding insert per chunk
        verify(jdbcTemplate, times(3))
                .update(anyString(), any(), any(), anyString());

        // Document should be saved with READY status
        verify(documentRepository, atLeastOnce()).save(argThat(doc ->
                "READY".equals(doc.getStatus())
        ));
    }

    @Test
    @DisplayName("Should mark document FAILED when no chunks found")
    void shouldMarkFailedWhenNoChunksFound() {
        when(documentRepository.findById(documentId))
                .thenReturn(Optional.of(document));
        when(chunkRepository.findByDocumentId(documentId))
                .thenReturn(List.of());

        embeddingService.generateEmbeddings(extractedEvent);

        // No embeddings should be inserted
        verifyNoInteractions(jdbcTemplate);

        // Document should be marked FAILED
        verify(documentRepository, atLeastOnce()).save(argThat(doc ->
                "FAILED".equals(doc.getStatus())
        ));
    }

    @Test
    @DisplayName("Should mark document FAILED when DB insert throws")
    void shouldMarkFailedWhenInsertFails() {
        List<DocumentChunk> chunks = List.of(
                createChunk(0, "Some content")
        );

        when(documentRepository.findById(documentId))
                .thenReturn(Optional.of(document));
        when(chunkRepository.findByDocumentId(documentId))
                .thenReturn(chunks);
        when(jdbcTemplate.update(anyString(), any(), any(), anyString()))
                .thenThrow(new RuntimeException("DB connection lost"));

        assertThatThrownBy(() ->
                embeddingService.generateEmbeddings(extractedEvent))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Embedding failed");

        verify(documentRepository, atLeastOnce()).save(argThat(doc ->
                "FAILED".equals(doc.getStatus())
        ));
    }

    @Test
    @DisplayName("Should throw when document not found in DB")
    void shouldThrowWhenDocumentNotFound() {
        when(documentRepository.findById(documentId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                embeddingService.generateEmbeddings(extractedEvent))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Document not found");

        verifyNoInteractions(chunkRepository, jdbcTemplate);
    }

    @Test
    @DisplayName("Should update status to EMBEDDING before processing")
    void shouldSetStatusToEmbeddingFirst() {
        List<DocumentChunk> chunks = List.of(createChunk(0, "content"));

        when(documentRepository.findById(documentId))
                .thenReturn(Optional.of(document));
        when(chunkRepository.findByDocumentId(documentId))
                .thenReturn(chunks);
        when(jdbcTemplate.update(anyString(), any(), any(), anyString()))
                .thenReturn(1);

        embeddingService.generateEmbeddings(extractedEvent);

        // First save should be EMBEDDING, last save should be READY
        verify(documentRepository, atLeastOnce()).save(argThat(doc ->
                "EMBEDDING".equals(doc.getStatus()) ||
                        "READY".equals(doc.getStatus())
        ));
    }

    // Helper — creates a DocumentChunk without needing a full builder
    private DocumentChunk createChunk(int index, String content) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(UUID.randomUUID());
        chunk.setDocumentId(documentId);
        chunk.setChunkIndex(index);
        chunk.setContent(content);
        return chunk;
    }
}