package com.aidocpipeline.queryservice.controller;

import com.aidocpipeline.queryservice.dto.QueryRequest;
import com.aidocpipeline.queryservice.dto.QueryResponse;
import com.aidocpipeline.queryservice.service.QueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/query")
@RequiredArgsConstructor
@Slf4j
public class QueryController {

    private final QueryService queryService;

    /**
     * POST /api/v1/query
     *
     * Accepts a natural language question and returns an AI-generated
     * answer with citations from the uploaded documents.
     *
     * Example request body:
     * {
     *   "question": "What are Jake's technical skills?",
     *   "tenantId": "default"
     * }
     */
    @PostMapping
    public ResponseEntity<QueryResponse> query(
            @Valid @RequestBody QueryRequest request) {

        log.info("Query request received: {}", request.getQuestion());
        QueryResponse response = queryService.query(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/query/health-check
     * Simple endpoint to verify the service is running.
     */
    @GetMapping("/health-check")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Query Service is running");
    }
}