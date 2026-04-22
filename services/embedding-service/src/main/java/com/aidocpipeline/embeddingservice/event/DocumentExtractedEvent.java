package com.aidocpipeline.embeddingservice.event;

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
public class DocumentExtractedEvent {
    private UUID documentId;
    private String fileName;
    private String tenantId;
    private int totalChunks;
    private LocalDateTime extractedAt;
}