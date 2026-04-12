# ADR-001: Message Broker Selection

## Status
Accepted

## Context
The pipeline has 3 processing stages: Ingestion → Extraction → Embedding.
Each stage can take 30-120 seconds. We need a way for stages to communicate
without waiting for each other (asynchronous), and without being tightly
coupled (if Extraction is slow, Ingestion should not be blocked).

## Decision
Use Apache Kafka via AWS MSK (Amazon's fully managed Kafka service).

## What is Kafka?
Kafka is a message queue system. Think of it like a post office.
- Ingestion service drops a letter (message) into a mailbox (Kafka topic)
- Extraction service picks up that letter when it is ready
- Neither service waits for the other — they are decoupled

## Alternatives We Considered

| Option     | Good                              | Bad                                      |
|------------|-----------------------------------|------------------------------------------|
| AWS SQS    | Very simple, cheap, fully managed | Cannot replay old messages, no ordering  |
| RabbitMQ   | Good for simple tasks             | Not designed for event streaming         |
| Kafka      | Replay, ordering, multi-consumer  | More complex setup than SQS              |

## Why Kafka Won
1. REPLAY: If Extraction crashes, we can re-read messages from the beginning
2. MULTI-CONSUMER: Both Extraction AND an Audit service can read the same message
3. SCALE: Handles millions of documents/day via partition scaling
4. AWS MSK removes all broker management — we get Kafka without the ops pain

## Trade-offs Accepted
- More complex than SQS
- Mitigation: AWS MSK is fully managed, so we never touch Kafka brokers directly