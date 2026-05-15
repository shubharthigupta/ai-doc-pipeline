package com.aidocpipeline.ingestionservice.service;

import com.aidocpipeline.ingestionservice.dto.DocumentStatusResponse;
import com.aidocpipeline.ingestionservice.dto.DocumentUploadResponse;
import com.aidocpipeline.ingestionservice.entity.Document;
import com.aidocpipeline.ingestionservice.event.DocumentUploadedEvent;
import com.aidocpipeline.ingestionservice.repository.DocumentRepository;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor        // Lombok: generates constructor for all final fields
public class IngestionService {

    private final DocumentRepository documentRepository;
    private final S3StorageService s3StorageService;
    private final KafkaTemplate<String, DocumentUploadedEvent> kafkaTemplate;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${kafka.topics.document-uploaded}")
    private String documentUploadedTopic;

    // Add to the class fields
    private final IngestionMetrics metrics;

    /**
     * Main method — called when user uploads a file.
     * Step 1: Validate
     * Step 2: Upload to S3
     * Step 3: Save to DB
     * Step 4: Publish to Kafka
     * Step 5: Return response
     */
    public DocumentUploadResponse ingestDocument(MultipartFile file, String tenantId) {
        log.info("Starting ingestion for file: {}, tenant: {}",
                file.getOriginalFilename(), tenantId);

        // Validate FIRST — before starting timer or touching any external service
        // This way validation failures are fast and don't need metrics
        try {
            validateFile(file);
        } catch (IllegalArgumentException e) {
            metrics.incrementValidationFailed();
            throw e;
        }

        // Only start timer after validation passes
        Timer.Sample timerSample = metrics.startUploadTimer();

        try {
            String s3Key = s3StorageService.uploadFile(file, tenantId);

            Document document = Document.builder()
                    .fileName(file.getOriginalFilename())
                    .s3Key(s3Key)
                    .mimeType(file.getContentType())
                    .status("UPLOADED")
                    .tenantId(tenantId)
                    .build();

            Document saved = documentRepository.save(document);
            log.info("Document saved to DB with ID: {}", saved.getId());

            DocumentUploadedEvent event = DocumentUploadedEvent.builder()
                    .documentId(saved.getId())
                    .s3Bucket(bucketName)
                    .s3Key(s3Key)
                    .fileName(file.getOriginalFilename())
                    .mimeType(file.getContentType())
                    .tenantId(tenantId)
                    .uploadedAt(saved.getUploadedAt())
                    .build();

            kafkaTemplate.send(documentUploadedTopic,
                    saved.getId().toString(), event);
            log.info("Event published to Kafka topic: {}", documentUploadedTopic);

            metrics.incrementUploaded();
            metrics.stopUploadTimer(timerSample);

            return DocumentUploadResponse.builder()
                    .documentId(saved.getId())
                    .fileName(saved.getFileName())
                    .status(saved.getStatus())
                    .message("Document uploaded successfully. Processing started.")
                    .uploadedAt(saved.getUploadedAt())
                    .build();

        } catch (Exception e) {
            log.error("Failed to ingest document: {}", file.getOriginalFilename(), e);
            metrics.stopUploadTimer(timerSample);
            throw new RuntimeException("Failed to process document: " + e.getMessage(), e);
        }
    }

    public List<DocumentStatusResponse> getAllDocuments(String tenantId) {
        return documentRepository.findByTenantId(tenantId)
                .stream()
                .map(this::toStatusResponse)
                .toList();
    }

    public DocumentStatusResponse getDocumentStatus(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
        return toStatusResponse(document);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Max 50MB
        long maxSize = 50 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File too large. Maximum size is 50MB");
        }

        // Only allow PDF, DOCX, TXT
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("application/pdf") &&
                !contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") &&
                !contentType.equals("text/plain"))) {
            throw new IllegalArgumentException("Unsupported file type. Allowed: PDF, DOCX, TXT");
        }
    }

    private DocumentStatusResponse toStatusResponse(Document doc) {
        return DocumentStatusResponse.builder()
                .documentId(doc.getId())
                .fileName(doc.getFileName())
                .status(doc.getStatus())
                .uploadedAt(doc.getUploadedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}