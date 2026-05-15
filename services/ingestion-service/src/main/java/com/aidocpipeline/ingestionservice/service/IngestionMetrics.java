package com.aidocpipeline.ingestionservice.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class IngestionMetrics {

    private final Counter documentsUploaded;
    private final Counter documentsFailedValidation;
    private final Timer uploadDuration;

    public IngestionMetrics(MeterRegistry registry) {
        // Count every successful upload
        this.documentsUploaded = Counter.builder("docpipeline.documents.uploaded.total")
                .description("Total number of documents successfully uploaded")
                .tag("service", "ingestion")
                .register(registry);

        // Count every validation failure
        this.documentsFailedValidation = Counter.builder("docpipeline.documents.validation.failed")
                .description("Number of documents that failed validation")
                .tag("service", "ingestion")
                .register(registry);

        // Measure how long each upload takes
        this.uploadDuration = Timer.builder("docpipeline.upload.duration")
                .description("Time taken to upload and persist a document")
                .tag("service", "ingestion")
                .register(registry);
    }

    public void incrementUploaded() {
        documentsUploaded.increment();
    }

    public void incrementValidationFailed() {
        documentsFailedValidation.increment();
    }

    public Timer.Sample startUploadTimer() {
        return Timer.start();
    }

    public void stopUploadTimer(Timer.Sample sample) {
        sample.stop(uploadDuration);
    }
}