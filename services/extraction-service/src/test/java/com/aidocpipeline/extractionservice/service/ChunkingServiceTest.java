package com.aidocpipeline.extractionservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkingServiceTest {

    private ChunkingService chunkingService;

    @BeforeEach
    void setUp() {
        chunkingService = new ChunkingService();
        ReflectionTestUtils.setField(chunkingService, "maxChunkSize", 100);
        ReflectionTestUtils.setField(chunkingService, "overlap", 10);
    }

    @Test
    @DisplayName("Should return empty list for null input")
    void shouldReturnEmptyListForNull() {
        List<String> chunks = chunkingService.chunk(null);
        assertThat(chunks).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list for blank input")
    void shouldReturnEmptyListForBlank() {
        List<String> chunks = chunkingService.chunk("   ");
        assertThat(chunks).isEmpty();
    }

    @Test
    @DisplayName("Should return single chunk for short text")
    void shouldReturnSingleChunkForShortText() {
        String shortText = "This is a short text.";
        List<String> chunks = chunkingService.chunk(shortText);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo(shortText);
    }

    @Test
    @DisplayName("Should split long text into multiple chunks")
    void shouldSplitLongTextIntoMultipleChunks() {
        String longText = "A".repeat(350);
        List<String> chunks = chunkingService.chunk(longText);
        assertThat(chunks).hasSizeGreaterThan(1);
    }

    @Test
    @DisplayName("Should never produce infinite loop")
    void shouldNeverProduceInfiniteLoop() {
        String text = "word ".repeat(1000);
        List<String> chunks = chunkingService.chunk(text);
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.size()).isLessThan(10000);
    }

    @Test
    @DisplayName("Each chunk should not exceed max chunk size")
    void eachChunkShouldNotExceedMaxSize() {
        String text = "Hello world. ".repeat(100);
        List<String> chunks = chunkingService.chunk(text);
        chunks.forEach(chunk ->
                assertThat(chunk.length()).isLessThanOrEqualTo(150));
    }
}