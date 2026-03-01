package com.docbrain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class GeminiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String embeddingModel;

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta"; // Reverted to v1beta

    public GeminiService(
            RestTemplate geminiRestTemplate,
            ObjectMapper objectMapper,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String model,
            @Value("${gemini.embedding-model}") String embeddingModel) {
        this.restTemplate = geminiRestTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.embeddingModel = embeddingModel;
    }

    /**
     * Generate text using Gemini LLM.
     */
    public String generateText(String prompt) {
        String url = GEMINI_BASE_URL + "/models/" + model + ":generateContent?key=" + apiKey;

        ObjectNode requestBody = objectMapper.createObjectNode();
        ArrayNode contents = requestBody.putArray("contents");
        ObjectNode content = contents.addObject();
        ArrayNode parts = content.putArray("parts");
        parts.addObject().put("text", prompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, entity, JsonNode.class);
            JsonNode body = response.getBody();

            if (body != null && body.has("candidates")) {
                return body.get("candidates").get(0)
                        .get("content").get("parts").get(0)
                        .get("text").asText();
            }

            log.error("Unexpected Gemini response: {}", body);
            throw new RuntimeException("Failed to get response from Gemini");
        } catch (Exception e) {
            log.error("Gemini API call failed", e);
            throw new RuntimeException("Gemini API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Generate embedding for a single text.
     */
    public float[] generateEmbedding(String text) {
        String url = GEMINI_BASE_URL + "/models/" + embeddingModel + ":embedContent?key=" + apiKey;

        ObjectNode requestBody = objectMapper.createObjectNode();
        ObjectNode content = requestBody.putObject("content");
        ArrayNode parts = content.putArray("parts");
        parts.addObject().put("text", text);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, entity, JsonNode.class);
            JsonNode body = response.getBody();

            if (body != null && body.has("embedding")) {
                JsonNode values = body.get("embedding").get("values");
                float[] embedding = new float[values.size()];
                for (int i = 0; i < values.size(); i++) {
                    embedding[i] = (float) values.get(i).asDouble();
                }
                return embedding;
            }

            log.error("Unexpected embedding response: {}", body);
            throw new RuntimeException("Failed to get embedding from Gemini");
        } catch (Exception e) {
            log.error("Gemini embedding API call failed", e);
            throw new RuntimeException("Gemini embedding API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Batch generate embeddings (processes in batches of 10).
     */
    public List<float[]> generateEmbeddingsBatch(List<String> texts) {
        List<float[]> results = new ArrayList<>();
        int batchSize = 10;

        for (int i = 0; i < texts.size(); i += batchSize) {
            List<String> batch = texts.subList(i, Math.min(i + batchSize, texts.size()));
            results.addAll(generateBatch(batch));
        }

        return results;
    }

    private List<float[]> generateBatch(List<String> texts) {
        String url = GEMINI_BASE_URL + "/models/" + embeddingModel + ":batchEmbedContents?key=" + apiKey;

        ObjectNode requestBody = objectMapper.createObjectNode();
        ArrayNode requests = requestBody.putArray("requests");

        for (String text : texts) {
            ObjectNode req = requests.addObject();
            // Removed: req.put("model", "models/" + embeddingModel); // This was incorrectly added
            ObjectNode content = req.putObject("content");
            ArrayNode parts = content.putArray("parts");
            parts.addObject().put("text", text);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, entity, JsonNode.class);
            JsonNode body = response.getBody();

            List<float[]> embeddings = new ArrayList<>();
            if (body != null && body.has("embeddings")) {
                for (JsonNode embeddingNode : body.get("embeddings")) {
                    JsonNode values = embeddingNode.get("values");
                    float[] embedding = new float[values.size()];
                    for (int i = 0; i < values.size(); i++) {
                        embedding[i] = (float) values.get(i).asDouble();
                    }
                    embeddings.add(embedding);
                }
            }

            return embeddings;
        } catch (Exception e) {
            log.error("Gemini batch embedding API call failed", e);
            throw new RuntimeException("Gemini batch embedding API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Convert float array to pgvector string format: [0.1,0.2,0.3,...]
     */
    public static String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Lists all available Gemini models. For debugging purposes.
     */
    public String listAvailableModels() {
        String url = GEMINI_BASE_URL + "/models?key=" + apiKey;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to list Gemini models", e);
            return "Error listing models: " + e.getMessage();
        }
    }
}
