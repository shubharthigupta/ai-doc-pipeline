# ADR-002: Vector Store Selection

## Status
Accepted

## Context
AI embeddings are arrays of 1024 numbers (called vectors) that represent the
meaning of a piece of text. To answer user questions, we need to find which
chunks of text are most similar to the question. This requires a database
that can store these vectors and search them efficiently.

## What is a Vector Store?
A normal database stores names, numbers, and text. A vector store stores
numeric arrays and can find the "closest" array to a given query in
milliseconds. "Closest" means most semantically similar.

## Decision
Use PostgreSQL with the pgvector extension (hosted on AWS RDS).

## Alternatives We Considered

| Option              | Good                              | Bad                                    |
|---------------------|-----------------------------------|----------------------------------------|
| Pinecone            | Fastest, easiest                  | External SaaS, data leaves AWS, $70+/mo|
| Weaviate            | Feature-rich, GraphQL             | Extra Kubernetes deployment, complexity|
| OpenSearch (AWS)    | Native AWS, kNN support           | Very expensive, overkill for our scale |
| pgvector on RDS     | Same DB as metadata, zero extra   | Slower than dedicated at 10M+ vectors  |

## Why pgvector Won
1. ONE DATABASE: Chunks, metadata, AND embeddings in one place — simpler ops
2. ZERO EXTRA COST: No additional SaaS subscription
3. HNSW INDEX: Modern approximate nearest-neighbour index gives ~10ms search
4. FAMILIAR: PostgreSQL is well-understood; no new system to learn
5. TRANSACTIONS: We can update chunk + embedding atomically

## Trade-offs Accepted
- Will need to migrate to dedicated vector DB if we exceed ~5 million vectors
- pgvector is slower than Pinecone at extreme scale (not our problem in v1)