package com.aidocpipeline.extractionservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadedEvent {
    private UUID documentId;
    private String s3Bucket;
    private String s3Key;
    private String fileName;
    private String mimeType;
    private String tenantId;
    private LocalDateTime uploadedAt;
}