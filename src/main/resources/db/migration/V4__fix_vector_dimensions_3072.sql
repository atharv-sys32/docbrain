-- Update embedding column from vector(768) to vector(3072) for gemini-embedding-001 model
ALTER TABLE document_chunks
    ALTER COLUMN embedding TYPE vector(3072);

-- Add HNSW index for vector similarity search (works on empty tables unlike IVFFlat)
CREATE INDEX IF NOT EXISTS idx_document_chunks_embedding
    ON document_chunks USING hnsw (embedding vector_cosine_ops);
