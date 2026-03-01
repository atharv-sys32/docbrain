package com.docbrain.controller;

import com.docbrain.model.dto.request.AskQuestionRequest;
import com.docbrain.model.dto.response.QAResponse;
import com.docbrain.model.entity.Conversation;
import com.docbrain.model.entity.Message;
import com.docbrain.service.CacheService;
import com.docbrain.service.CollectionService;
import com.docbrain.service.ConversationService;
import com.docbrain.service.RAGService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class QAController {

    private final RAGService ragService;
    private final ConversationService conversationService;
    private final CollectionService collectionService;
    private final CacheService cacheService;

    @PostMapping("/api/v1/collections/{collectionId}/ask")
    public ResponseEntity<QAResponse> ask(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID collectionId,
            @Valid @RequestBody AskQuestionRequest request) {
        UUID userId = UUID.fromString(userDetails.getUsername());

        // Rate limit check
        cacheService.checkQuestionRateLimit(userId);

        // Validate collection ownership
        collectionService.getById(collectionId, userId);

        // Get or create conversation
        Conversation conversation;
        List<Message> history;

        if (request.getConversationId() != null && !request.getConversationId().isBlank()) {
            UUID convId = UUID.fromString(request.getConversationId());
            conversation = conversationService.getById(convId, userId);
            history = conversationService.getMessages(convId, userId);
        } else {
            // Auto-create conversation, title from first question
            String title = request.getQuestion().length() > 50
                    ? request.getQuestion().substring(0, 50) + "..."
                    : request.getQuestion();
            conversation = conversationService.create(userId, collectionId, title);
            history = List.of();
        }

        // Save user message
        conversationService.saveUserMessage(conversation, request.getQuestion());

        // Run RAG pipeline
        QAResponse response = ragService.ask(collectionId, request.getQuestion(), history);
        response.setConversationId(conversation.getId().toString());

        // Save assistant message
        conversationService.saveAssistantMessage(conversation, response.getAnswer(), response.getSources());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/v1/collections/{collectionId}/conversations")
    public ResponseEntity<Map<String, Object>> createConversation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID collectionId,
            @RequestBody(required = false) Map<String, String> body) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        String title = body != null ? body.get("title") : null;
        Conversation conversation = conversationService.create(userId, collectionId, title);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", conversation.getId(),
                "title", conversation.getTitle(),
                "createdAt", conversation.getCreatedAt().toString()
        ));
    }

    @GetMapping("/api/v1/collections/{collectionId}/conversations")
    public ResponseEntity<List<Map<String, Object>>> listConversations(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID collectionId) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        List<Conversation> conversations = conversationService.listByCollection(collectionId, userId);

        List<Map<String, Object>> response = conversations.stream()
                .map(c -> Map.<String, Object>of(
                        "id", c.getId(),
                        "title", c.getTitle(),
                        "createdAt", c.getCreatedAt().toString(),
                        "updatedAt", c.getUpdatedAt().toString()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/v1/conversations/{conversationId}/messages")
    public ResponseEntity<List<Map<String, Object>>> getMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID conversationId) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        List<Message> messages = conversationService.getMessages(conversationId, userId);

        List<Map<String, Object>> response = messages.stream()
                .map(m -> Map.<String, Object>of(
                        "id", m.getId(),
                        "role", m.getRole().name(),
                        "content", m.getContent(),
                        "sources", m.getSources() != null ? m.getSources() : "",
                        "createdAt", m.getCreatedAt().toString()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/v1/conversations/{conversationId}")
    public ResponseEntity<Void> deleteConversation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID conversationId) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        conversationService.delete(conversationId, userId);
        return ResponseEntity.noContent().build();
    }
}
