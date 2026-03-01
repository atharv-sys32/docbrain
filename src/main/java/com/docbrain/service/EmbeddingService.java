package com.docbrain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final GeminiService geminiService;

    /**
     * Generate embedding for a single text (used for query embedding).
     */
    public float[] embed(String text) {
        log.debug("Generating embedding for text of length {}", text.length());
        return geminiService.generateEmbedding(text);
    }

    /**
     * Generate embeddings for multiple chunks in batches.
     */
    public List<float[]> embedBatch(List<String> chunks) {
        log.info("Generating embeddings for {} chunks", chunks.size());
        return geminiService.generateEmbeddingsBatch(chunks);
    }

    /**
     * Convert embedding to pgvector-compatible string.
     */
    public String toVectorString(float[] embedding) {
        return GeminiService.toVectorString(embedding);
    }
}
