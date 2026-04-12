# AI-Augmented Document Processing Pipeline

A production-grade system for uploading enterprise documents (PDF, DOCX, TXT),
extracting their content using AI, and querying them with natural language.

Built as a Solution Architect portfolio project demonstrating:
- Event-driven microservices architecture (Java 21 + Spring Boot)
- RAG (Retrieval-Augmented Generation) with AWS Bedrock
- Kubernetes deployment on AWS EKS via GitOps (ArgoCD)
- OAuth 2.0 security with Amazon Cognito
- Full observability: tracing, metrics, structured logging

## Architecture
See docs/architecture/ for C4 diagrams.

## Tech Stack
| Layer        | Technology                          |
|-------------|-------------------------------------|
| Backend     | Java 21, Spring Boot 3.3            |
| AI / LLM    | AWS Bedrock (Titan Embeddings, Claude) |
| Messaging   | Apache Kafka (AWS MSK)              |
| Database    | PostgreSQL + pgvector (AWS RDS)     |
| Storage     | Amazon S3                           |
| Auth        | Amazon Cognito (OAuth 2.0 / OIDC)   |
| Container   | Docker, Kubernetes (AWS EKS)        |
| IaC         | Terraform                           |
| CD          | ArgoCD (GitOps)                     |
| Frontend    | React 18 + TypeScript + Vite        |

## Running locally
```bash
docker-compose up -d
```
See docs/ for full setup instructions.