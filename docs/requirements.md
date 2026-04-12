# System Requirements — AI Document Processing Pipeline

## Functional Requirements
- FR-01: Upload PDF, DOCX, TXT documents via drag-and-drop UI or REST API
- FR-02: Extract and chunk text from uploaded documents
- FR-03: Generate semantic embeddings per chunk using AWS Bedrock Titan
- FR-04: Accept natural-language queries; return AI-generated answers with citations
- FR-05: Show real-time document processing status (Uploaded → Extracted → Embedded → Ready)
- FR-06: Manage documents: list, delete, re-process

## Non-Functional Requirements
- NFR-01 Throughput: 100 concurrent document uploads without degradation
- NFR-02 Availability: 99.9% uptime; multi-AZ deployment on AWS
- NFR-03 Latency: Query P95 < 3 seconds end-to-end
- NFR-04 Security: OAuth 2.0 (PKCE) for users; Client Credentials for services; Zero Trust network
- NFR-05 Observability: Distributed tracing, Prometheus metrics, structured JSON logs
- NFR-06 Cost: Monthly AWS spend target < $150 for dev environment (document actuals)
- NFR-07 Scalability: Horizontal scaling via Kubernetes HPA on CPU + Kafka consumer lag

## Out of Scope (v1)
- Multi-language document support
- OCR for scanned images (v2)
- Fine-tuning custom models