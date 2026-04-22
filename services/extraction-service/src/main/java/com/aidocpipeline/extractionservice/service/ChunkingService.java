package com.aidocpipeline.extractionservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ChunkingService {

    @Value("${chunking.max-chunk-size:500}")
    private int maxChunkSize;

    @Value("${chunking.overlap:50}")
    private int overlap;

    /**
     * Splits text into overlapping chunks safely.
     *
     * Key fixes over previous version:
     * 1. start always advances by at least 1 — prevents infinite loop
     * 2. overlap cannot be >= maxChunkSize — prevents negative/zero step
     * 3. Hard cap of 10000 chunks — safety net against runaway loops
     */
    public List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) {
            log.warn("Empty text provided to chunker — returning empty list");
            return chunks;
        }

        // Clean up excessive whitespace
        String cleanText = text.replaceAll("\\s+", " ").trim();
        int textLength = cleanText.length();

        // Safety check — overlap must be smaller than chunk size
        // otherwise the window never moves forward
        int safeOverlap = Math.min(overlap, maxChunkSize / 2);

        // Step size — how far we advance each iteration
        // Must always be positive
        int step = maxChunkSize - safeOverlap;
        if (step <= 0) step = maxChunkSize;

        log.info("Chunking text. Length: {}, ChunkSize: {}, Overlap: {}, Step: {}",
                textLength, maxChunkSize, safeOverlap, step);

        int start = 0;
        int chunkCount = 0;
        int maxChunks = 10000; // hard safety cap

        while (start < textLength && chunkCount < maxChunks) {
            int end = Math.min(start + maxChunkSize, textLength);

            // Try to break at a sentence boundary (period or newline)
            // but only if we're not already at the end of the text
            if (end < textLength) {
                int lastPeriod = cleanText.lastIndexOf('.', end);
                int lastNewline = cleanText.lastIndexOf('\n', end);
                int breakPoint = Math.max(lastPeriod, lastNewline);

                // Only use the break point if it's close enough
                // and actually ahead of where we started
                if (breakPoint > start + (maxChunkSize / 2)) {
                    end = breakPoint + 1;
                }
            }

            // Extract and add chunk
            String chunk = cleanText.substring(start, end).trim();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
                chunkCount++;
            }

            // Advance start by step — ALWAYS moves forward
            int nextStart = start + step;

            // Extra safety — if somehow nextStart didn't advance, force it
            if (nextStart <= start) {
                nextStart = start + 1;
            }

            start = nextStart;
        }

        if (chunkCount >= maxChunks) {
            log.warn("Hit max chunk limit of {}. Text may have been truncated.", maxChunks);
        }

        log.info("Chunking complete. Total chunks: {}", chunks.size());
        return chunks;
    }
}