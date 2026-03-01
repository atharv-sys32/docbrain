package com.docbrain.service;

import com.docbrain.model.enums.DocumentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentProcessingService {

    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final DocumentService documentService;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Async pipeline: Extract text → Chunk → Embed → Store in pgvector.
     * Called after document upload.
     */
    @Async
    public void processDocument(UUID documentId, String filePath) {
        log.info("Starting async processing for document: {}", documentId);

        try {
            // Step 1 & 2: Extract text and chunk
            List<String> chunks = chunkingService.extractAndChunk(filePath);
            log.info("Document {} split into {} chunks", documentId, chunks.size());

            if (chunks.isEmpty()) {
                documentService.updateStatus(documentId, DocumentStatus.FAILED, 0);
                log.warn("No chunks generated for document: {}", documentId);
                return;
            }

            // Step 3: Generate embeddings in batches
            List<float[]> embeddings = embeddingService.embedBatch(chunks);
            log.info("Generated {} embeddings for document {}", embeddings.size(), documentId);

            // Step 4: Store chunks + embeddings in pgvector via native SQL
            String insertSql = """
                    INSERT INTO document_chunks (id, document_id, chunk_index, content, token_count, embedding)
                    VALUES (gen_random_uuid(), ?::uuid, ?, ?, ?, ?::vector)
                    """;

            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                int tokenCount = chunkingService.estimateTokenCount(chunk);
                String vectorString = embeddingService.toVectorString(embeddings.get(i));

                jdbcTemplate.update(insertSql,
                        documentId.toString(),
                        i,
                        chunk,
                        tokenCount,
                        vectorString
                );
            }

            // Step 5: Mark document as READY
            documentService.updateStatus(documentId, DocumentStatus.READY, chunks.size());
            log.info("Document {} processing complete. {} chunks stored.", documentId, chunks.size());

        } catch (Exception e) {
            log.error("Document processing failed for: {}", documentId, e);
            documentService.updateStatus(documentId, DocumentStatus.FAILED, 0);
        }
    }
}
