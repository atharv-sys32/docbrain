-- Update embedding column from vector(768) to vector(3072) for gemini-embedding-001 model
ALTER TABLE document_chunks
    ALTER COLUMN embedding TYPE vector(3072);

-- Note: HNSW index has a 2000 dimension limit in pgvector, so no index is created.
-- Sequential scan will be used for similarity search (fine for small-to-medium datasets).
