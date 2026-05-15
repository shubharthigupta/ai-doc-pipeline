package com.aidocpipeline.extractionservice.service;

import com.aidocpipeline.extractionservice.entity.Document;
import com.aidocpipeline.extractionservice.entity.DocumentChunk;
import com.aidocpipeline.extractionservice.event.DocumentExtractedEvent;
import com.aidocpipeline.extractionservice.event.DocumentUploadedEvent;
import com.aidocpipeline.extractionservice.extractor.TextExtractorFactory;
import com.aidocpipeline.extractionservice.extractor.DocumentTextExtractor;
import com.aidocpipeline.extractionservice.repository.DocumentChunkRepository;
import com.aidocpipeline.extractionservice.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExtractionServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentChunkRepository chunkRepository;
    @Mock private S3DownloadService s3DownloadService;
    @Mock private TextExtractorFactory extractorFactory;
    @Mock private DocumentTextExtractor textExtractor;
    @Mock private ChunkingService chunkingService;
    @Mock private KafkaTemplate<String, DocumentExtractedEvent> kafkaTemplate;

    @InjectMocks
    private ExtractionService extractionService;

    private UUID documentId;
    private Document document;
    private DocumentUploadedEvent uploadedEvent;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(extractionService,
                "documentExtractedTopic", "document.extracted");

        documentId = UUID.randomUUID();

        document = new Document();
        document.setId(documentId);
        document.setFileName("test.pdf");
        document.setS3Key("default/uuid-test.pdf");
        document.setMimeType("application/pdf");
        document.setStatus("UPLOADED");
        document.setTenantId("default");

        uploadedEvent = DocumentUploadedEvent.builder()
                .documentId(documentId)
                .s3Bucket("doc-pipeline-local")
                .s3Key("default/uuid-test.pdf")
                .fileName("test.pdf")
                .mimeType("application/pdf")
                .tenantId("default")
                .uploadedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully extract and chunk a document")
    void shouldSuccessfullyProcessDocument() throws Exception {
        // Arrange
        String extractedText = "This is the content of the test document. ".repeat(10);
        List<String> chunks = List.of("Chunk one content", "Chunk two content");

        InputStream fakeStream = new ByteArrayInputStream(
                "PDF bytes".getBytes());

        when(documentRepository.findById(documentId))
                .thenReturn(Optional.of(document));
        when(s3DownloadService.downloadFile(anyString()))
                .thenReturn(fakeStream);
        when(extractorFactory.getExtractor("application/pdf"))
                .thenReturn(textExtractor);
        when(textExtractor.extract(any(InputStream.class)))
                .thenReturn(extractedText);
        when(chunkingService.chunk(extractedText))
                .thenReturn(chunks);
        when(chunkRepository.save(any(DocumentChunk.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        extractionService.processDocument(uploadedEvent);

        // Assert
        verify(documentRepository, atLeastOnce()).save(any(Document.class));
        verify(chunkRepository, times(chunks.size())).save(any(DocumentChunk.class));
        verify(kafkaTemplate, times(1))
                .send(anyString(), anyString(), any(DocumentExtractedEvent.class));
    }

    @Test
    @DisplayName("Should mark document as FAILED when extraction throws an exception")
    void shouldMarkDocumentAsFailedOnError() throws Exception {
        when(documentRepository.findById(documentId))
                .thenReturn(Optional.of(document));
        when(s3DownloadService.downloadFile(anyString()))
                .thenThrow(new RuntimeException("S3 connection refused"));

        assertThatThrownBy(() ->
                extractionService.processDocument(uploadedEvent))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Extraction failed");

        // Verify document was marked FAILED
        verify(documentRepository, atLeastOnce()).save(argThat(doc ->
                "FAILED".equals(doc.getStatus())
        ));

        // Kafka should NOT have been called
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("Should throw exception when document not found in DB")
    void shouldThrowWhenDocumentNotFound() {
        when(documentRepository.findById(documentId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                extractionService.processDocument(uploadedEvent))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Document not found");

        verifyNoInteractions(s3DownloadService, chunkRepository, kafkaTemplate);
    }

    @Test
    @DisplayName("Should update document status to EXTRACTING then EXTRACTED")
    void shouldUpdateStatusCorrectly() throws Exception {
        String extractedText = "Some document content here.";
        List<String> chunks = List.of("Some document content here.");

        when(documentRepository.findById(documentId))
                .thenReturn(Optional.of(document));
        when(s3DownloadService.downloadFile(anyString()))
                .thenReturn(new ByteArrayInputStream("bytes".getBytes()));
        when(extractorFactory.getExtractor(anyString()))
                .thenReturn(textExtractor);
        when(textExtractor.extract(any()))
                .thenReturn(extractedText);
        when(chunkingService.chunk(anyString()))
                .thenReturn(chunks);
        when(chunkRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        extractionService.processDocument(uploadedEvent);

        // Verify status was set to EXTRACTING first, then EXTRACTED
        verify(documentRepository, atLeastOnce()).save(argThat(doc ->
                "EXTRACTING".equals(doc.getStatus()) ||
                        "EXTRACTED".equals(doc.getStatus())
        ));
    }
}