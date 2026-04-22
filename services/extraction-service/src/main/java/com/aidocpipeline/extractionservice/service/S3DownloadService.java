package com.aidocpipeline.extractionservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.InputStream;

@Service
@Slf4j
public class S3DownloadService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public S3DownloadService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Downloads a file from S3/MinIO and returns it as an InputStream.
     * The caller is responsible for closing the stream.
     */
    public InputStream downloadFile(String s3Key) {
        log.info("Downloading file from S3. Bucket: {}, Key: {}", bucketName, s3Key);

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
        log.info("File downloaded successfully. Key: {}", s3Key);
        return response;
    }
}