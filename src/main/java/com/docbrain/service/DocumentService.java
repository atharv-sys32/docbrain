package com.docbrain.service;

import com.docbrain.exception.DocumentProcessingException;
import com.docbrain.exception.ResourceNotFoundException;
import com.docbrain.model.dto.response.DocumentResponse;
import com.docbrain.model.entity.Collection;
import com.docbrain.model.entity.Document;
import com.docbrain.model.enums.DocumentStatus;
import com.docbrain.repository.DocumentChunkRepository;
import com.docbrain.repository.DocumentRepository;
import com.docbrain.util.FileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final CollectionService collectionService;
    private final FileValidator fileValidator;

    @Value("${upload.dir}")
    private String uploadDir;

    @Value("${upload.max-documents-per-collection}")
    private int maxDocumentsPerCollection;

    public Document upload(UUID userId, UUID collectionId, MultipartFile file) {
        // Validate ownership
        Collection collection = collectionService.getById(collectionId, userId);

        // Validate file
        fileValidator.validate(file);

        // Check document limit
        long currentCount = documentRepository.countByCollectionId(collectionId);
        if (currentCount >= maxDocumentsPerCollection) {
            throw new IllegalArgumentException(
                    "Collection has reached the maximum of " + maxDocumentsPerCollection + " documents");
        }

        // Save file to disk
        String storedPath = saveFile(file, collectionId);

        // Create document record
        String extension = fileValidator.getFileExtension(file.getOriginalFilename()).toUpperCase();
        Document document = Document.builder()
                .collection(collection)
                .fileName(file.getOriginalFilename())
                .fileType(extension)
                .fileSize(file.getSize())
                .filePath(storedPath)
                .status(DocumentStatus.PROCESSING)
                .build();

        return documentRepository.save(document);
    }

    public List<DocumentResponse> listByCollection(UUID collectionId, UUID userId) {
        // Validate ownership
        collectionService.getById(collectionId, userId);

        return documentRepository.findByCollectionIdOrderByCreatedAtDesc(collectionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Document getById(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
    }

    public DocumentResponse getResponseById(UUID documentId, UUID userId) {
        Document document = getById(documentId);
        // Validate ownership through collection
        collectionService.getById(document.getCollection().getId(), userId);
        return toResponse(document);
    }

    public DocumentResponse getStatus(UUID documentId, UUID userId) {
        return getResponseById(documentId, userId);
    }

    @Transactional
    public void delete(UUID documentId, UUID userId) {
        Document document = getById(documentId);
        // Validate ownership through collection
        collectionService.getById(document.getCollection().getId(), userId);

        // Delete file from disk
        try {
            Files.deleteIfExists(Paths.get(document.getFilePath()));
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", document.getFilePath(), e);
        }

        // Delete chunks then document (cascade handles chunks, but explicit for clarity)
        documentChunkRepository.deleteByDocumentId(documentId);
        documentRepository.delete(document);
    }

    public void updateStatus(UUID documentId, DocumentStatus status, int totalChunks) {
        Document document = getById(documentId);
        document.setStatus(status);
        document.setTotalChunks(totalChunks);
        documentRepository.save(document);
    }

    private String saveFile(MultipartFile file, UUID collectionId) {
        try {
            Path collectionDir = Paths.get(uploadDir, collectionId.toString());
            Files.createDirectories(collectionDir);

            String uniqueFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = collectionDir.resolve(uniqueFileName);
            file.transferTo(filePath.toFile());

            return filePath.toString();
        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to save uploaded file", e);
        }
    }

    private DocumentResponse toResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .totalChunks(document.getTotalChunks())
                .status(document.getStatus())
                .createdAt(document.getCreatedAt())
                .build();
    }
}
