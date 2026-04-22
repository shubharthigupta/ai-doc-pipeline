package com.aidocpipeline.extractionservice.extractor;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
@Slf4j
public class DocxTextExtractor implements DocumentTextExtractor {

    @Override
    public String extract(InputStream inputStream) throws Exception {
        // XWPFDocument reads the DOCX file
        try (XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String text = extractor.getText();
            log.info("DOCX extraction complete. Characters extracted: {}", text.length());
            return text;
        }
    }

    @Override
    public String supportedMimeType() {
        return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    }
}