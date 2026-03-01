package com.docbrain.repository;

import com.docbrain.model.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByCollectionIdOrderByCreatedAtDesc(UUID collectionId);

    long countByCollectionId(UUID collectionId);
}
