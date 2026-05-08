package com.aidocpipeline.queryservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Data
public class Document {

    @Id
    private UUID id;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "status")
    private String status;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;
}