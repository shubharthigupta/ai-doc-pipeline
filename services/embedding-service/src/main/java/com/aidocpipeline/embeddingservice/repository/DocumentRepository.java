package com.aidocpipeline.embeddingservice.repository;

import com.aidocpipeline.embeddingservice.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
}