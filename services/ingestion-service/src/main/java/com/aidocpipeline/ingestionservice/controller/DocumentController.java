package com.aidocpipeline.ingestionservice.controller;

import com.aidocpipeline.ingestionservice.dto.DocumentStatusResponse;
import com.aidocpipeline.ingestionservice.dto.DocumentUploadResponse;
import com.aidocpipeline.ingestionservice.service.IngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final IngestionService ingestionService;

    /**
     * POST /api/v1/documents
     * Upload a document for processing.
     * Returns 202 Accepted immediately — processing happens asynchronously.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "default") String tenantId) {

        log.info("Received upload request. File: {}, Size: {} bytes, Tenant: {}",
                file.getOriginalFilename(), file.getSize(), tenantId);

        DocumentUploadResponse response = ingestionService.ingestDocument(file, tenantId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * GET /api/v1/documents
     * List all documents for a tenant.
     */
    @GetMapping
    public ResponseEntity<List<DocumentStatusResponse>> getAllDocuments(
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "default") String tenantId) {

        return ResponseEntity.ok(ingestionService.getAllDocuments(tenantId));
    }

    /**
     * GET /api/v1/documents/{id}/status
     * Get the processing status of a specific document.
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<DocumentStatusResponse> getDocumentStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(ingestionService.getDocumentStatus(id));
    }
}