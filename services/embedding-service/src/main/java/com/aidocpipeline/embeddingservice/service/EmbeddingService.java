package com.aidocpipeline.embeddingservice.service;

import com.aidocpipeline.embeddingservice.entity.Document;
import com.aidocpipeline.embeddingservice.entity.DocumentChunk;
import com.aidocpipeline.embeddingservice.event.DocumentExtractedEvent;
import com.aidocpipeline.embeddingservice.repository.DocumentChunkRepository;
import com.aidocpipeline.embeddingservice.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmbeddingService {

    private final DocumentChunkRepository chunkRepository;
    private final DocumentRepository documentRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${aws.bedrock.mock-enabled:true}")
    private boolean mockEnabled;

    /**
     * Called for each document after extraction completes.
     * Generates an embedding vector for every chunk and saves to pgvector.
     */
    @Transactional
    public void generateEmbeddings(DocumentExtractedEvent event) {
        log.info("Starting embedding for document: {}", event.getDocumentId());

        // Update status to EMBEDDING
        Document document = documentRepository.findById(event.getDocumentId())
                .orElseThrow(() -> new RuntimeException(
                        "Document not found: " + event.getDocumentId()
                ));
        document.setStatus("EMBEDDING");
        documentRepository.save(document);

        try {
            // Get all chunks for this document
            List<DocumentChunk> chunks = chunkRepository
                    .findByDocumentId(event.getDocumentId());

            log.info("Found {} chunks to embed for document: {}",
                    chunks.size(), event.getDocumentId());

            // Fail if no chunks found
            if (chunks.isEmpty()) {
                log.warn("No chunks found for document: {}", event.getDocumentId());
                document.setStatus("FAILED");
                documentRepository.save(document);
                return;  // Exit without throwing exception
            }

            // Generate and save embedding for each chunk
            for (DocumentChunk chunk : chunks) {
                float[] vector = mockEnabled
                        ? generateMockEmbedding()
                        : callBedrock(chunk.getContent());

                saveEmbedding(chunk.getId(), event.getDocumentId(), vector);

                log.debug("Saved embedding for chunk: {}", chunk.getId());
            }

            // Update document status to READY — pipeline complete!
            document.setStatus("READY");
            documentRepository.save(document);

            log.info("All embeddings saved. Document {} is now READY", event.getDocumentId());

        } catch (Exception e) {
            log.error("Embedding failed for document: {}", event.getDocumentId(), e);
            document.setStatus("FAILED");
            documentRepository.save(document);
            throw new RuntimeException("Embedding failed: " + e.getMessage(), e);
        }
    }

    /**
     * Saves the embedding vector to pgvector using raw JDBC.
     * We use JdbcTemplate here because pgvector's vector type
     * is not natively supported by JPA/Hibernate without extra setup.
     */
    private void saveEmbedding(UUID chunkId, UUID documentId, float[] vector) {
        String vectorString = toVectorString(vector);
        jdbcTemplate.update(
                "INSERT INTO document_embeddings (chunk_id, document_id, embedding) " +
                        "VALUES (?, ?, ?::vector)",
                chunkId, documentId, vectorString
        );
    }

    /**
     * Converts float[] to PostgreSQL vector string format.
     * Example: [0.1, 0.2, 0.3] → "[0.1,0.2,0.3]"
     */
    private String toVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * LOCAL DEV ONLY — generates random 1024-dimension vector.
     * Replace this with callBedrock() when deploying to AWS.
     */
    private float[] generateMockEmbedding() {
        Random random = new Random();
        float[] vector = new float[1024];
        for (int i = 0; i < 1024; i++) {
            vector[i] = random.nextFloat() * 2 - 1; // values between -1 and 1
        }
        return vector;
    }

    /**
     * PRODUCTION — calls AWS Bedrock Titan Embeddings.
     * You will implement this in Phase 7 when deploying to AWS.
     */
    private float[] callBedrock(String text) {
        // TODO: Phase 7 — implement real Bedrock call
        // BedrockRuntimeClient client = BedrockRuntimeClient.builder()...
        throw new UnsupportedOperationException("Bedrock not configured locally");
    }
}