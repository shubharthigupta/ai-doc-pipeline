# ADR-003: LLM and Embedding Provider

## Status
Accepted

## Context
We need two AI capabilities:
1. EMBEDDINGS: Convert text chunks into numeric vectors (for storage + search)
2. GENERATION: Accept a user question + context chunks, generate a human answer

## What is AWS Bedrock?
AWS Bedrock is Amazon's managed AI service. Instead of running AI models
ourselves (which requires expensive GPU servers), we call an API and pay
only for what we use. Bedrock hosts models from Anthropic, Amazon, and others.

## Decision
Use AWS Bedrock for both capabilities:
- Embeddings : amazon.titan-embed-text-v2:0   ($0.00002 per 1,000 tokens)
- Generation : anthropic.claude-3-5-sonnet    (best quality, fast response)

## Alternatives We Considered

| Option              | Good                        | Bad                                         |
|---------------------|-----------------------------|---------------------------------------------|
| OpenAI API          | Best quality, easy to use   | Data leaves AWS, GDPR issues, separate bill |
| Google Vertex AI    | Strong models               | Adds GCP dependency to AWS architecture     |
| Self-hosted (Ollama)| Free, fully private         | Needs GPU nodes ($$$), major ops burden     |
| AWS Bedrock         | Native AWS, IAM auth, cheap | Slightly higher latency than direct OpenAI  |

## Why Bedrock Won
1. DATA STAYS IN AWS: Critical for enterprise/BFSI clients — no data egress
2. IAM AUTH: No API keys to manage or rotate — uses AWS roles automatically
3. ZERO IDLE COST: Pay only per token used, nothing when system is idle
4. ONE BILL: Everything on the AWS invoice — no separate vendor management
5. COMPLIANCE: Bedrock has SOC2, HIPAA, and GDPR certifications built in

## Trade-offs Accepted
- Bedrock adds ~100ms latency vs direct OpenAI API
- Mitigation: We use streaming responses so user sees text appear immediately