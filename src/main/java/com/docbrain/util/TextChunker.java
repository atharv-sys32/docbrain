package com.docbrain.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {

    private static final int DEFAULT_CHUNK_SIZE = 2000;  // ~500 tokens
    private static final int DEFAULT_OVERLAP = 200;      // ~50 tokens

    public List<String> chunk(String text) {
        return chunk(text, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }

    public List<String> chunk(String text, int chunkSize, int overlap) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        // First, try splitting by paragraphs
        String[] paragraphs = text.split("\n\n+");

        StringBuilder currentChunk = new StringBuilder();

        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) continue;

            if (currentChunk.length() + trimmed.length() + 1 <= chunkSize) {
                if (!currentChunk.isEmpty()) {
                    currentChunk.append("\n\n");
                }
                currentChunk.append(trimmed);
            } else {
                // Current chunk is full
                if (!currentChunk.isEmpty()) {
                    chunks.add(currentChunk.toString());

                    // Create overlap from the end of the current chunk
                    String overlapText = getOverlapText(currentChunk.toString(), overlap);
                    currentChunk = new StringBuilder(overlapText);
                }

                // If a single paragraph is larger than chunk size, split by sentences
                if (trimmed.length() > chunkSize) {
                    List<String> sentenceChunks = splitBySentences(trimmed, chunkSize, overlap);
                    for (int i = 0; i < sentenceChunks.size(); i++) {
                        if (i < sentenceChunks.size() - 1) {
                            chunks.add(sentenceChunks.get(i));
                        } else {
                            currentChunk = new StringBuilder(sentenceChunks.get(i));
                        }
                    }
                } else {
                    if (!currentChunk.isEmpty()) {
                        currentChunk.append("\n\n");
                    }
                    currentChunk.append(trimmed);
                }
            }
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
    }

    private List<String> splitBySentences(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        String[] sentences = text.split("(?<=[.!?])\\s+");
        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() + 1 <= chunkSize) {
                if (!currentChunk.isEmpty()) {
                    currentChunk.append(" ");
                }
                currentChunk.append(sentence);
            } else {
                if (!currentChunk.isEmpty()) {
                    chunks.add(currentChunk.toString());
                    String overlapText = getOverlapText(currentChunk.toString(), overlap);
                    currentChunk = new StringBuilder(overlapText);
                }

                // If a single sentence is still too large, force split by words
                if (sentence.length() > chunkSize) {
                    List<String> wordChunks = splitByWords(sentence, chunkSize, overlap);
                    chunks.addAll(wordChunks.subList(0, wordChunks.size() - 1));
                    currentChunk = new StringBuilder(wordChunks.getLast());
                } else {
                    if (!currentChunk.isEmpty()) {
                        currentChunk.append(" ");
                    }
                    currentChunk.append(sentence);
                }
            }
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
    }

    private List<String> splitByWords(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder currentChunk = new StringBuilder();

        for (String word : words) {
            if (currentChunk.length() + word.length() + 1 <= chunkSize) {
                if (!currentChunk.isEmpty()) {
                    currentChunk.append(" ");
                }
                currentChunk.append(word);
            } else {
                if (!currentChunk.isEmpty()) {
                    chunks.add(currentChunk.toString());
                    String overlapText = getOverlapText(currentChunk.toString(), overlap);
                    currentChunk = new StringBuilder(overlapText);
                    currentChunk.append(" ").append(word);
                } else {
                    currentChunk.append(word);
                }
            }
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
    }

    private String getOverlapText(String text, int overlapSize) {
        if (text.length() <= overlapSize) {
            return text;
        }
        return text.substring(text.length() - overlapSize);
    }

    public int estimateTokenCount(String text) {
        // Rough estimation: ~4 characters per token
        return text.length() / 4;
    }
}
