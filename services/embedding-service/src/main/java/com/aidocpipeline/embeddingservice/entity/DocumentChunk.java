package com.aidocpipeline.embeddingservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "document_chunks")
@Data
public class DocumentChunk {

    @Id
    private UUID id;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "chunk_index")
    private int chunkIndex;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
}