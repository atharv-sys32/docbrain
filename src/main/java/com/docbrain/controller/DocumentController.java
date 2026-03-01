package com.docbrain.controller;

import com.docbrain.model.dto.response.DocumentResponse;
import com.docbrain.model.entity.Document;
import com.docbrain.service.CacheService;
import com.docbrain.service.DocumentProcessingService;
import com.docbrain.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentProcessingService documentProcessingService;
    private final CacheService cacheService;

    @PostMapping("/api/v1/collections/{collectionId}/documents")
    public ResponseEntity<Map<String, Object>> upload(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID collectionId,
            @RequestParam("file") MultipartFile file) {
        UUID userId = UUID.fromString(userDetails.getUsername());

        // Rate limit check
        cacheService.checkUploadRateLimit(userId);

        Document document = documentService.upload(userId, collectionId, file);

        // Trigger async processing pipeline
        documentProcessingService.processDocument(document.getId(), document.getFilePath());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", document.getId(),
                "fileName", document.getFileName(),
                "status", document.getStatus().name(),
                "message", "Document uploaded. Processing started."
        ));
    }

    @GetMapping("/api/v1/collections/{collectionId}/documents")
    public ResponseEntity<List<DocumentResponse>> listByCollection(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID collectionId) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        List<DocumentResponse> documents = documentService.listByCollection(collectionId, userId);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/api/v1/documents/{id}")
    public ResponseEntity<DocumentResponse> getById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        DocumentResponse document = documentService.getResponseById(id, userId);
        return ResponseEntity.ok(document);
    }

    @GetMapping("/api/v1/documents/{id}/status")
    public ResponseEntity<Map<String, Object>> getStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        DocumentResponse document = documentService.getStatus(id, userId);

        return ResponseEntity.ok(Map.of(
                "id", document.getId(),
                "status", document.getStatus().name(),
                "totalChunks", document.getTotalChunks() != null ? document.getTotalChunks() : 0
        ));
    }

    @DeleteMapping("/api/v1/documents/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        documentService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
}
