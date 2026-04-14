package com.aidocpipeline.ingestionservice.repository;

import com.aidocpipeline.ingestionservice.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    // Spring Data automatically generates SQL for methods named like this
    List<Document> findByTenantId(String tenantId);
    List<Document> findByStatus(String status);
}