package com.docbrain.service;

import com.docbrain.util.TextChunker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkingService {

    private final TextChunker textChunker;
    private final Tika tika = new Tika();

    /**
     * Extract text from a document file using Apache Tika.
     */
    public String extractText(String filePath) {
        Path path = Paths.get(filePath);

        try (InputStream stream = Files.newInputStream(path)) {
            String text = tika.parseToString(stream);
            log.info("Extracted {} characters from {}", text.length(), path.getFileName());
            return text;
        } catch (IOException | TikaException e) {
            log.error("Failed to extract text from: {}", filePath, e);
            throw new RuntimeException("Failed to extract text from document: " + e.getMessage(), e);
        }
    }

    /**
     * Extract text and split into chunks.
     */
    public List<String> extractAndChunk(String filePath) {
        String text = extractText(filePath);

        if (text == null || text.isBlank()) {
            throw new RuntimeException("No text content found in document");
        }

        List<String> chunks = textChunker.chunk(text);
        log.info("Split document into {} chunks", chunks.size());
        return chunks;
    }

    /**
     * Estimate token count for a text chunk.
     */
    public int estimateTokenCount(String text) {
        return textChunker.estimateTokenCount(text);
    }
}
