package com.aidocpipeline.queryservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@Slf4j
public class EmbeddingService {

    @Value("${aws.bedrock.mock-enabled:true}")
    private boolean mockEnabled;

    /**
     * Converts a text question into a 1024-dimension vector.
     * Locally uses a mock — Phase 7 replaces with real Bedrock Titan call.
     *
     * IMPORTANT: Must use the same embedding model as the Embedding Service
     * used when storing document chunks. If they differ, similarity search
     * will return garbage results.
     */
    public float[] embedQuery(String queryText) {
        if (mockEnabled) {
            return generateMockEmbedding(queryText);
        }
        return callBedrockTitan(queryText);
    }

    /**
     * Converts float[] to pgvector string format.
     * Example: [0.1, -0.2, 0.3, ...]
     */
    public String toVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * LOCAL MOCK — generates a deterministic-ish vector from the query text.
     * Uses the query's hashCode as the random seed so the same question
     * always produces the same vector — makes local testing predictable.
     */
    private float[] generateMockEmbedding(String text) {
        log.debug("Generating mock embedding for query: {}", text);
        Random random = new Random(text.hashCode());
        float[] vector = new float[1024];
        for (int i = 0; i < 1024; i++) {
            vector[i] = (random.nextFloat() * 2) - 1;
        }
        return vector;
    }

    /**
     * PRODUCTION — calls AWS Bedrock Titan Embeddings V2.
     * Implemented in Phase 7.
     */
    private float[] callBedrockTitan(String text) {
        throw new UnsupportedOperationException(
                "Real Bedrock not configured. Set aws.bedrock.mock-enabled=true for local dev."
        );
    }
}