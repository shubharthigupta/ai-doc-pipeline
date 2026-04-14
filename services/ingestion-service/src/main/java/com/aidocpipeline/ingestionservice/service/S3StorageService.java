package com.aidocpipeline.ingestionservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j                          // Lombok: gives us a log variable for free
public class S3StorageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public S3StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Uploads a file to S3/MinIO.
     * Returns the S3 key (path) where the file was stored.
     */
    public String uploadFile(MultipartFile file, String tenantId) throws IOException {
        // Build a unique key: tenantId/uuid-originalfilename
        // e.g. "user123/a1b2c3-report.pdf"
        String key = tenantId + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

        log.info("Uploading file to S3. Bucket: {}, Key: {}", bucketName, key);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(
                file.getInputStream(), file.getSize()
        ));

        log.info("File uploaded successfully. Key: {}", key);
        return key;
    }
}