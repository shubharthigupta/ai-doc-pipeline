package com.aidocpipeline.queryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitationDto {

    private UUID documentId;
    private String documentName;
    private int chunkIndex;
    private String relevantText;    // the actual chunk text used
    private double similarityScore; // how similar this chunk was to the question
}