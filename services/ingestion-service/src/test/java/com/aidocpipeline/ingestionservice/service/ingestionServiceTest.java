package com.aidocpipeline.ingestionservice.service;

import com.aidocpipeline.ingestionservice.dto.DocumentUploadResponse;
import com.aidocpipeline.ingestionservice.entity.Document;
import com.aidocpipeline.ingestionservice.event.DocumentUploadedEvent;
import com.aidocpipeline.ingestionservice.repository.DocumentRepository;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IngestionServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private S3StorageService s3StorageService;
    @Mock private KafkaTemplate<String, DocumentUploadedEvent> kafkaTemplate;
    @Mock private IngestionMetrics metrics;      // ADD THIS
    @Mock private Timer.Sample timerSample;      // ADD THIS

    @InjectMocks
    private IngestionService ingestionService;

    private MockMultipartFile validPdf;
    private Document savedDocument;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ingestionService, "bucketName", "doc-pipeline-local");
        ReflectionTestUtils.setField(ingestionService, "documentUploadedTopic", "document.uploaded");

        validPdf = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "PDF content".getBytes());

        savedDocument = Document.builder()
                .id(UUID.randomUUID())
                .fileName("test.pdf")
                .s3Key("default/uuid-test.pdf")
                .mimeType("application/pdf")
                .status("UPLOADED")
                .tenantId("default")
                .uploadedAt(LocalDateTime.now())
                .build();

        // REMOVED: when(metrics.startUploadTimer()).thenReturn(timerSample);
        // This line is now only in tests that actually call it
    }

    @Test
    @DisplayName("Should successfully ingest a valid PDF document")
    void shouldIngestValidDocument() throws Exception {
        when(metrics.startUploadTimer()).thenReturn(timerSample);  // ADD HERE
        when(s3StorageService.uploadFile(any(), anyString()))
                .thenReturn("default/uuid-test.pdf");
        when(documentRepository.save(any())).thenReturn(savedDocument);

        DocumentUploadResponse response =
                ingestionService.ingestDocument(validPdf, "default");

        assertThat(response).isNotNull();
        assertThat(response.getFileName()).isEqualTo("test.pdf");
        assertThat(response.getStatus()).isEqualTo("UPLOADED");
        assertThat(response.getDocumentId()).isNotNull();

        verify(s3StorageService, times(1)).uploadFile(any(), anyString());
        verify(documentRepository, times(1)).save(any());
        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any());
        verify(metrics, times(1)).incrementUploaded();
        verify(metrics, times(1)).stopUploadTimer(timerSample);
    }

    @Test
    @DisplayName("Should reject empty files")
    void shouldRejectEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() ->
                ingestionService.ingestDocument(emptyFile, "default"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");

        // Validation failed — metrics timer should NOT have started
        verify(metrics, never()).startUploadTimer();
        // But validation failure counter should have incremented
        verify(metrics, times(1)).incrementValidationFailed();
        verifyNoInteractions(s3StorageService, kafkaTemplate);
    }

    @Test
    @DisplayName("Should reject unsupported file types")
    void shouldRejectUnsupportedFileType() {
        MockMultipartFile excelFile = new MockMultipartFile(
                "file", "data.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "excel content".getBytes());

        assertThatThrownBy(() ->
                ingestionService.ingestDocument(excelFile, "default"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported file type");

        verify(metrics, never()).startUploadTimer();
        verify(metrics, times(1)).incrementValidationFailed();
        verifyNoInteractions(s3StorageService, kafkaTemplate);
    }

    @Test
    @DisplayName("Should reject files over 50MB")
    void shouldRejectOversizedFiles() {
        byte[] largeContent = new byte[51 * 1024 * 1024];
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "large.pdf", "application/pdf", largeContent);

        assertThatThrownBy(() ->
                ingestionService.ingestDocument(largeFile, "default"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("too large");

        verify(metrics, never()).startUploadTimer();
        verify(metrics, times(1)).incrementValidationFailed();
        verifyNoInteractions(s3StorageService, kafkaTemplate);
    }

    @Test
    @DisplayName("Should accept DOCX files")
    void shouldAcceptDocxFiles() throws Exception {
        when(metrics.startUploadTimer()).thenReturn(timerSample);  // ADD HERE

        MockMultipartFile docxFile = new MockMultipartFile(
                "file", "report.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "DOCX content".getBytes());

        Document docxDocument = Document.builder()
                .id(UUID.randomUUID())
                .fileName("report.docx")
                .s3Key("default/uuid-report.docx")
                .mimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .status("UPLOADED")
                .tenantId("default")
                .uploadedAt(LocalDateTime.now())
                .build();

        when(s3StorageService.uploadFile(any(), anyString()))
                .thenReturn("default/uuid-report.docx");
        when(documentRepository.save(any())).thenReturn(docxDocument);

        DocumentUploadResponse response =
                ingestionService.ingestDocument(docxFile, "default");

        assertThat(response).isNotNull();
        assertThat(response.getFileName()).isEqualTo("report.docx");
        verify(metrics, times(1)).incrementUploaded();
    }

}