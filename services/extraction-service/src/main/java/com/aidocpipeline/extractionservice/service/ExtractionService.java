package com.aidocpipeline.extractionservice.service;

import com.aidocpipeline.extractionservice.entity.Document;
import com.aidocpipeline.extractionservice.entity.DocumentChunk;
import com.aidocpipeline.extractionservice.event.DocumentExtractedEvent;
import com.aidocpipeline.extractionservice.event.DocumentUploadedEvent;
import com.aidocpipeline.extractionservice.extractor.TextExtractorFactory;
import com.aidocpipeline.extractionservice.repository.DocumentChunkRepository;
import com.aidocpipeline.extractionservice.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExtractionService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final S3DownloadService s3DownloadService;
    private final TextExtractorFactory extractorFactory;
    private final ChunkingService chunkingService;
    private final KafkaTemplate<String, DocumentExtractedEvent> kafkaTemplate;

    @Value("${kafka.topics.document-extracted}")
    private String documentExtractedTopic;

    /**
     * Main method — called when a DocumentUploadedEvent arrives from Kafka.
     *
     * Step 1: Update document status to EXTRACTING
     * Step 2: Download file from MinIO/S3
     * Step 3: Extract text using the right extractor (PDF/DOCX/TXT)
     * Step 4: Split text into chunks
     * Step 5: Save chunks to PostgreSQL
     * Step 6: Update document status to EXTRACTED
     * Step 7: Publish DocumentExtractedEvent to Kafka
     */
    @Transactional
    public void processDocument(DocumentUploadedEvent event) {
        log.info("Starting extraction for document: {}", event.getDocumentId());

        // Step 1: Find document and update status to EXTRACTING
        Document document = documentRepository.findById(event.getDocumentId())
                .orElseThrow(() -> new RuntimeException(
                        "Document not found: " + event.getDocumentId()
                ));

        document.setStatus("EXTRACTING");
        documentRepository.save(document);

        try {
            // Step 2: Download the file from MinIO
            InputStream fileStream = s3DownloadService.downloadFile(event.getS3Key());

            // Step 3: Pick the right extractor based on MIME type and extract text
            String rawText = extractorFactory
                    .getExtractor(event.getMimeType())
                    .extract(fileStream);

            log.info("Text extracted. Document: {}, Characters: {}",
                    event.getDocumentId(), rawText.length());

            // Step 4: Split the text into chunks
            List<String> chunks = chunkingService.chunk(rawText);
            log.info("Text chunked. Document: {}, Total chunks: {}",
                    event.getDocumentId(), chunks.size());

            // Step 5: Save each chunk to PostgreSQL
            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk chunk = DocumentChunk.builder()
                        .documentId(event.getDocumentId())
                        .chunkIndex(i)
                        .content(chunks.get(i))
                        .build();
                chunkRepository.save(chunk);
            }

            log.info("Chunks saved to DB. Document: {}", event.getDocumentId());

            // Step 6: Update document status to EXTRACTED
            document.setStatus("EXTRACTED");
            documentRepository.save(document);

            // Step 7: Publish event so Embedding Service can pick up
            DocumentExtractedEvent extractedEvent = DocumentExtractedEvent.builder()
                    .documentId(event.getDocumentId())
                    .fileName(event.getFileName())
                    .tenantId(event.getTenantId())
                    .totalChunks(chunks.size())
                    .extractedAt(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(documentExtractedTopic,
                    event.getDocumentId().toString(), extractedEvent);

            log.info("DocumentExtractedEvent published. Document: {}",
                    event.getDocumentId());

        } catch (Exception e) {
            // If anything fails, mark the document as FAILED
            log.error("Extraction failed for document: {}", event.getDocumentId(), e);
            document.setStatus("FAILED");
            documentRepository.save(document);
            throw new RuntimeException("Extraction failed: " + e.getMessage(), e);
        }
    }
}