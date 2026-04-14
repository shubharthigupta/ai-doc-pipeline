package com.aidocpipeline.ingestionservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Data                    // Lombok: generates getters, setters, toString
@Builder                 // Lombok: lets us use Document.builder().fileName("x").build()
@NoArgsConstructor       // Lombok: generates empty constructor
@AllArgsConstructor      // Lombok: generates constructor with all fields
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;           // path to file inside MinIO/S3

    @Column(name = "mime_type")
    private String mimeType;        // e.g. "application/pdf"

    @Column(name = "status")
    private String status;          // UPLOADED / EXTRACTING / EMBEDDING / READY / FAILED

    @Column(name = "tenant_id")
    private String tenantId;        // which user owns this document

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist                     // runs automatically before first save
    public void prePersist() {
        uploadedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "UPLOADED";
    }

    @PreUpdate                      // runs automatically before every update
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}