package com.aidocpipeline.extractionservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    private UUID id;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "s3_key")
    private String s3Key;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "status")
    private String status;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}