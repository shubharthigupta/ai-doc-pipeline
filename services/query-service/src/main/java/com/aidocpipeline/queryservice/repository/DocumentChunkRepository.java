package com.aidocpipeline.queryservice.repository;

import com.aidocpipeline.queryservice.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    /**
     * Vector similarity search using pgvector cosine distance operator <=>.
     *
     * This is the heart of RAG — it finds the top K chunks whose embeddings
     * are closest to the query embedding in 1024-dimensional vector space.
     *
     * The ::vector cast converts our string representation to pgvector type.
     * The <=> operator computes cosine distance (0 = identical, 2 = opposite).
     * We subtract from 1 to get similarity (1 = identical, -1 = opposite).
     *
     * ORDER BY distance ASC means most similar chunks come first.
     */
    @Query(value = """
            SELECT dc.id, dc.document_id, dc.chunk_index, dc.content, dc.page_number,
                   1 - (de.embedding <=> CAST(:queryVector AS vector)) AS similarity
            FROM document_embeddings de
            JOIN document_chunks dc ON dc.id = de.chunk_id
            WHERE 1 - (de.embedding <=> CAST(:queryVector AS vector)) >= :threshold
            ORDER BY de.embedding <=> CAST(:queryVector AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<Object[]> findSimilarChunks(
            @Param("queryVector") String queryVector,
            @Param("topK") int topK,
            @Param("threshold") double threshold
    );

    /**
     * Same search but filtered to specific document IDs only.
     * Used when user wants to query a subset of their documents.
     */
    @Query(value = """
            SELECT dc.id, dc.document_id, dc.chunk_index, dc.content, dc.page_number,
                   1 - (de.embedding <=> CAST(:queryVector AS vector)) AS similarity
            FROM document_embeddings de
            JOIN document_chunks dc ON dc.id = de.chunk_id
            WHERE dc.document_id IN (:documentIds)
            AND 1 - (de.embedding <=> CAST(:queryVector AS vector)) >= :threshold
            ORDER BY de.embedding <=> CAST(:queryVector AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<Object[]> findSimilarChunksInDocuments(
            @Param("queryVector") String queryVector,
            @Param("documentIds") List<UUID> documentIds,
            @Param("topK") int topK,
            @Param("threshold") double threshold
    );
}