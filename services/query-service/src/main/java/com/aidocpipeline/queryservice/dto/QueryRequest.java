package com.aidocpipeline.queryservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryRequest {

    @NotBlank(message = "Question cannot be empty")
    @Size(min = 3, max = 1000, message = "Question must be between 3 and 1000 characters")
    private String question;

    // Optional — filter results to specific documents only
    // If null, searches across ALL documents
    private List<UUID> documentIds;

    // Optional — which tenant's documents to search
    private String tenantId;
}