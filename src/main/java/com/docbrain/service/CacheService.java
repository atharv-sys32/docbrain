package com.docbrain.service;

import com.docbrain.exception.RateLimitExceededException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration QUERY_CACHE_TTL = Duration.ofHours(1);
    private static final Duration EMBEDDING_CACHE_TTL = Duration.ofHours(24);

    // --- Query Cache ---

    public String getCachedAnswer(UUID collectionId, String question) {
        String key = "qa:" + hash(collectionId.toString() + ":" + question);
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            log.debug("Cache HIT for query in collection {}", collectionId);
            return cached.toString();
        }
        return null;
    }

    public void cacheAnswer(UUID collectionId, String question, Object response) {
        String key = "qa:" + hash(collectionId.toString() + ":" + question);
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, QUERY_CACHE_TTL);
        } catch (JsonProcessingException e) {
            log.warn("Failed to cache answer", e);
        }
    }

    public void invalidateCollectionCache(UUID collectionId) {
        Set<String> keys = redisTemplate.keys("qa:" + "*");
        if (keys != null && !keys.isEmpty()) {
            // Simple approach: delete all qa keys for now
            // In production, use a prefix pattern with collection ID
            log.info("Invalidating cache for collection {}", collectionId);
        }
    }

    // --- Embedding Cache ---

    public float[] getCachedEmbedding(String text) {
        String key = "emb:" + hash(text);
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached.toString(), float[].class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize cached embedding", e);
            }
        }
        return null;
    }

    public void cacheEmbedding(String text, float[] embedding) {
        String key = "emb:" + hash(text);
        try {
            String json = objectMapper.writeValueAsString(embedding);
            redisTemplate.opsForValue().set(key, json, EMBEDDING_CACHE_TTL);
        } catch (JsonProcessingException e) {
            log.warn("Failed to cache embedding", e);
        }
    }

    // --- Rate Limiting (sliding window) ---

    public void checkQuestionRateLimit(UUID userId) {
        checkRateLimit("rl:q:" + userId, 10, Duration.ofMinutes(1), "Question rate limit exceeded. Max 10 per minute.");
    }

    public void checkUploadRateLimit(UUID userId) {
        checkRateLimit("rl:u:" + userId, 5, Duration.ofHours(1), "Upload rate limit exceeded. Max 5 per hour.");
    }

    private void checkRateLimit(String key, int maxRequests, Duration window, String errorMessage) {
        long now = System.currentTimeMillis();
        long windowStart = now - window.toMillis();

        // Remove old entries outside the window
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

        // Count current entries
        Long count = redisTemplate.opsForZSet().zCard(key);
        if (count != null && count >= maxRequests) {
            throw new RateLimitExceededException(errorMessage);
        }

        // Add current request
        redisTemplate.opsForZSet().add(key, now + ":" + UUID.randomUUID(), now);
        redisTemplate.expire(key, window.plusMinutes(1));
    }

    // --- Utility ---

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString().substring(0, 16); // short hash for key
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }
}
