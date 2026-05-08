package com.aidocpipeline.queryservice.service;

import com.aidocpipeline.queryservice.dto.CitationDto;
import com.aidocpipeline.queryservice.dto.QueryRequest;
import com.aidocpipeline.queryservice.dto.QueryResponse;
import com.aidocpipeline.queryservice.entity.Document;
import com.aidocpipeline.queryservice.repository.DocumentChunkRepository;
import com.aidocpipeline.queryservice.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class QueryService {

    private final DocumentChunkRepository chunkRepository;
    private final DocumentRepository documentRepository;
    private final EmbeddingService embeddingService;
    private final LlmService llmService;

    @Value("${rag.top-k:5}")
    private int topK;

    @Value("${rag.similarity-threshold:0.0}")
    private double similarityThreshold;

    /**
     * Main RAG pipeline — called when user submits a question.
     *
     * Step 1: Embed the question into a vector
     * Step 2: Search pgvector for the most similar document chunks
     * Step 3: Build citations from the retrieved chunks
     * Step 4: Generate an answer using the LLM with retrieved context
     * Step 5: Return the answer with full citations
     */
    public QueryResponse query(QueryRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Processing query: {}", request.getQuestion());

        // Step 1: Convert question to vector
        float[] queryVector = embeddingService.embedQuery(request.getQuestion());
        String vectorString = embeddingService.toVectorString(queryVector);

        log.info("Query embedded. Searching pgvector for top {} similar chunks", topK);

        // Step 2: Search pgvector for similar chunks
        List<Object[]> rawResults;
        if (request.getDocumentIds() != null && !request.getDocumentIds().isEmpty()) {
            // Search within specific documents only
            rawResults = chunkRepository.findSimilarChunksInDocuments(
                    vectorString, request.getDocumentIds(), topK, similarityThreshold
            );
        } else {
            // Search across all documents
            rawResults = chunkRepository.findSimilarChunks(
                    vectorString, topK, similarityThreshold
            );
        }

        log.info("Found {} similar chunks", rawResults.size());

        // Step 3: Build citation objects from raw query results
        List<CitationDto> citations = buildCitations(rawResults);

        // Step 4: Generate answer using LLM
        String answer = llmService.generateAnswer(request.getQuestion(), citations);

        long processingTime = System.currentTimeMillis() - startTime;
        log.info("Query processed in {}ms. Citations: {}", processingTime, citations.size());

        // Step 5: Return complete response
        return QueryResponse.builder()
                .queryId(UUID.randomUUID())
                .question(request.getQuestion())
                .answer(answer)
                .citations(citations)
                .totalChunksSearched(rawResults.size())
                .processingTimeMs(processingTime)
                .answeredAt(LocalDateTime.now())
                .build();
    }

    /**
     * Converts raw SQL results from pgvector query into CitationDto objects.
     *
     * The raw Object[] from the native query contains:
     * [0] id (UUID)
     * [1] document_id (UUID)
     * [2] chunk_index (int)
     * [3] content (String)
     * [4] page_number (Integer, nullable)
     * [5] similarity (double) — cosine similarity score
     */
    private List<CitationDto> buildCitations(List<Object[]> rawResults) {
        List<CitationDto> citations = new ArrayList<>();

        for (Object[] row : rawResults) {
            UUID documentId = (UUID) row[1];
            int chunkIndex = (int) row[2];
            String content = (String) row[3];
            double similarity = ((Number) row[5]).doubleValue();

            // Look up the document name for the citation
            String documentName = documentRepository.findById(documentId)
                    .map(Document::getFileName)
                    .orElse("Unknown Document");

            citations.add(CitationDto.builder()
                    .documentId(documentId)
                    .documentName(documentName)
                    .chunkIndex(chunkIndex)
                    .relevantText(content)
                    .similarityScore(similarity)
                    .build());
        }

        return citations;
    }
}