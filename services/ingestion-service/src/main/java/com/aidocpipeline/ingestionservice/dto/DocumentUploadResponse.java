package com.aidocpipeline.ingestionservice.dto;

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
public class DocumentUploadResponse {

    private UUID documentId;
    private String fileName;
    private String status;
    private String message;
    private LocalDateTime uploadedAt;
}