package com.docbrain.repository;

import com.docbrain.model.entity.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CollectionRepository extends JpaRepository<Collection, UUID> {

    List<Collection> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT COUNT(d) FROM Document d WHERE d.collection.id = :collectionId")
    long countDocumentsByCollectionId(UUID collectionId);
}
