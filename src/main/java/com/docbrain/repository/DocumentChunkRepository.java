package com.docbrain.repository;

import com.docbrain.model.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    List<DocumentChunk> findByDocumentIdOrderByChunkIndex(UUID documentId);

    void deleteByDocumentId(UUID documentId);

    @Query(value = """
            SELECT dc.id, dc.content, dc.chunk_index, d.file_name,
                   1 - (dc.embedding <=> cast(:queryEmbedding AS vector)) AS similarity
            FROM document_chunks dc
            JOIN documents d ON dc.document_id = d.id
            WHERE d.collection_id = :collectionId
            ORDER BY dc.embedding <=> cast(:queryEmbedding AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<Object[]> findSimilarChunks(
            @Param("collectionId") UUID collectionId,
            @Param("queryEmbedding") String queryEmbedding,
            @Param("topK") int topK
    );
}
