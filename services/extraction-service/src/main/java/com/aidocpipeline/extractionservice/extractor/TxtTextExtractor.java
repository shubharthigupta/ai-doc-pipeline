package com.aidocpipeline.extractionservice.extractor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class TxtTextExtractor implements DocumentTextExtractor {

    @Override
    public String extract(InputStream inputStream) throws Exception {
        String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        log.info("TXT extraction complete. Characters extracted: {}", text.length());
        return text;
    }

    @Override
    public String supportedMimeType() {
        return "text/plain";
    }
}