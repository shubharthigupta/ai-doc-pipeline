package com.aidocpipeline.extractionservice.extractor;

import java.io.InputStream;

public interface DocumentTextExtractor {

    /**
     * Extract all text from the document input stream.
     * Returns the full raw text as a single string.
     */
    String extract(InputStream inputStream) throws Exception;

    /**
     * Which MIME type this extractor handles.
     */
    String supportedMimeType();
}