package com.aidocpipeline.extractionservice.extractor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TextExtractorFactory {

    // Spring automatically injects all 3 extractors into this list
    private final Map<String, DocumentTextExtractor> extractors;

    public TextExtractorFactory(List<DocumentTextExtractor> extractorList) {
        // Build a map: "application/pdf" → PdfTextExtractor, etc.
        this.extractors = extractorList.stream()
                .collect(Collectors.toMap(
                        DocumentTextExtractor::supportedMimeType,
                        Function.identity()
                ));
        log.info("Registered extractors: {}", extractors.keySet());
    }

    /**
     * Returns the right extractor for the given MIME type.
     * Throws an exception if the file type is not supported.
     */
    public DocumentTextExtractor getExtractor(String mimeType) {
        DocumentTextExtractor extractor = extractors.get(mimeType);
        if (extractor == null) {
            throw new IllegalArgumentException(
                    "No extractor found for MIME type: " + mimeType +
                            ". Supported types: " + extractors.keySet()
            );
        }
        return extractor;
    }
}