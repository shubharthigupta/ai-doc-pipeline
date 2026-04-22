package com.aidocpipeline.extractionservice.extractor;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
@Slf4j
public class PdfTextExtractor implements DocumentTextExtractor {

    @Override
    public String extract(InputStream inputStream) throws Exception {
        // Write to a temp file first — this lets PDFBox use disk
        // instead of loading the entire PDF into heap memory
        Path tempFile = Files.createTempFile("pdf-extract-", ".pdf");

        try {
            // Copy stream to temp file on disk
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

            // Load from temp file using scratch file (disk-backed memory)
            // This prevents FontAwesome and other large fonts from
            // blowing up the Java heap
            try (PDDocument document = Loader.loadPDF(
                    tempFile.toFile(),
                    MemoryUsageSetting.setupTempFileOnly().streamCache)) {

                PDFTextStripper stripper = new PDFTextStripper();

                // Tell stripper to skip non-text content
                stripper.setSortByPosition(true);

                String text = stripper.getText(document);
                log.info("PDF extraction complete. Characters extracted: {}",
                        text.length());
                return text;
            }
        } finally {
            // Always delete the temp file — even if extraction fails
            Files.deleteIfExists(tempFile);
        }
    }

    @Override
    public String supportedMimeType() {
        return "application/pdf";
    }
}