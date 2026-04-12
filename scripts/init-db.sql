-- Enable the pgvector extension so we can store AI embeddings
CREATE EXTENSION IF NOT EXISTS vector;

-- Stores metadata about each uploaded document
CREATE TABLE IF NOT EXISTS documents (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_name   VARCHAR(500) NOT NULL,
    s3_key      VARCHAR(1000) NOT NULL,   -- path to the file in S3/MinIO
    mime_type   VARCHAR(100),             -- e.g. application/pdf
    status      VARCHAR(50) DEFAULT 'UPLOADED',  -- UPLOADED/EXTRACTING/EMBEDDING/READY/FAILED
    tenant_id   VARCHAR(100),            -- which user/organisation owns this
    uploaded_at TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW()
);

-- Stores the text split into chunks (e.g. paragraph-sized pieces)
CREATE TABLE IF NOT EXISTS document_chunks (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id  UUID REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index  INT NOT NULL,            -- which chunk number (0, 1, 2...)
    content      TEXT NOT NULL,           -- the actual text content
    page_number  INT,                     -- which page it came from
    start_char   INT,                     -- character position start
    end_char     INT,                     -- character position end
    created_at   TIMESTAMP DEFAULT NOW()
);

-- Stores the AI embedding (1024 numbers) for each chunk
CREATE TABLE IF NOT EXISTS document_embeddings (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chunk_id    UUID REFERENCES document_chunks(id) ON DELETE CASCADE,
    document_id UUID NOT NULL,
    embedding   vector(1024),            -- 1024-dimension vector from Titan Embeddings V2
    created_at  TIMESTAMP DEFAULT NOW()
);

-- HNSW index: makes similarity search fast (finds top-5 matches in ~10ms)
CREATE INDEX IF NOT EXISTS idx_embeddings_hnsw
    ON document_embeddings
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- Outbox table: ensures Kafka messages are never lost even if service crashes
CREATE TABLE IF NOT EXISTS outbox_events (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id UUID NOT NULL,
    event_type   VARCHAR(200) NOT NULL,
    payload      JSONB NOT NULL,
    published    BOOLEAN DEFAULT FALSE,
    created_at   TIMESTAMP DEFAULT NOW()
);