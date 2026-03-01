package com.docbrain.controller;

import com.docbrain.model.dto.request.CreateCollectionRequest;
import com.docbrain.model.entity.Collection;
import com.docbrain.service.CollectionService;
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
@RequestMapping("/api/v1/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateCollectionRequest request) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        Collection collection = collectionService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(collection, 0));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        List<Collection> collections = collectionService.listByUser(userId);

        List<Map<String, Object>> response = collections.stream()
                .map(c -> toResponse(c, collectionService.getDocumentCount(c.getId())))
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        Collection collection = collectionService.getById(id, userId);
        long docCount = collectionService.getDocumentCount(id);
        return ResponseEntity.ok(toResponse(collection, docCount));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody CreateCollectionRequest request) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        Collection collection = collectionService.update(id, userId, request);
        long docCount = collectionService.getDocumentCount(id);
        return ResponseEntity.ok(toResponse(collection, docCount));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        collectionService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> toResponse(Collection collection, long documentCount) {
        return Map.of(
                "id", collection.getId(),
                "name", collection.getName(),
                "description", collection.getDescription() != null ? collection.getDescription() : "",
                "documentCount", documentCount,
                "createdAt", collection.getCreatedAt().toString(),
                "updatedAt", collection.getUpdatedAt().toString()
        );
    }
}
