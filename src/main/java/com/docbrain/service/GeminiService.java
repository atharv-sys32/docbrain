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
    private final String geminiApiKey;
    private final String embeddingModel;
    private final String groqApiKey;
    private final String groqModel;

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final String GROQ_BASE_URL = "https://api.groq.com/openai/v1";

    public GeminiService(
            RestTemplate geminiRestTemplate,
            ObjectMapper objectMapper,
            @Value("${gemini.api-key}") String geminiApiKey,
            @Value("${gemini.embedding-model}") String embeddingModel,
            @Value("${groq.api-key}") String groqApiKey,
            @Value("${groq.model}") String groqModel) {
        this.restTemplate = geminiRestTemplate;
        this.objectMapper = objectMapper;
        this.geminiApiKey = geminiApiKey;
        this.embeddingModel = embeddingModel;
        this.groqApiKey = groqApiKey;
        this.groqModel = groqModel;
    }

    /**
     * Generate text using Groq LLM (OpenAI-compatible API).
     */
    public String generateText(String prompt) {
        String url = GROQ_BASE_URL + "/chat/completions";

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", groqModel);
        ArrayNode messages = requestBody.putArray("messages");
        ObjectNode message = messages.addObject();
        message.put("role", "user");
        message.put("content", prompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);
        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, entity, JsonNode.class);
            JsonNode body = response.getBody();

            if (body != null && body.has("choices")) {
                return body.get("choices").get(0)
                        .get("message").get("content").asText();
            }

            log.error("Unexpected Groq response: {}", body);
            throw new RuntimeException("Failed to get response from Groq");
        } catch (Exception e) {
            log.error("Groq API call failed", e);
            throw new RuntimeException("Groq API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Generate embedding for a single text using Gemini.
     */
    public float[] generateEmbedding(String text) {
        String url = GEMINI_BASE_URL + "/models/" + embeddingModel + ":embedContent?key=" + geminiApiKey;

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
        String url = GEMINI_BASE_URL + "/models/" + embeddingModel + ":batchEmbedContents?key=" + geminiApiKey;

        ObjectNode requestBody = objectMapper.createObjectNode();
        ArrayNode requests = requestBody.putArray("requests");

        for (String text : texts) {
            ObjectNode req = requests.addObject();
            req.put("model", "models/" + embeddingModel);
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
}
