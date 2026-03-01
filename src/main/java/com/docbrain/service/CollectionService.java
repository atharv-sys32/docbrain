package com.docbrain.service;

import com.docbrain.exception.ResourceNotFoundException;
import com.docbrain.model.dto.request.CreateCollectionRequest;
import com.docbrain.model.entity.Collection;
import com.docbrain.model.entity.User;
import com.docbrain.repository.CollectionRepository;
import com.docbrain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final UserRepository userRepository;

    public Collection create(UUID userId, CreateCollectionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Collection collection = Collection.builder()
                .user(user)
                .name(request.getName())
                .description(request.getDescription())
                .build();

        return collectionRepository.save(collection);
    }

    public List<Collection> listByUser(UUID userId) {
        return collectionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Collection getById(UUID collectionId, UUID userId) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found"));

        if (!collection.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Collection not found");
        }

        return collection;
    }

    public long getDocumentCount(UUID collectionId) {
        return collectionRepository.countDocumentsByCollectionId(collectionId);
    }

    public Collection update(UUID collectionId, UUID userId, CreateCollectionRequest request) {
        Collection collection = getById(collectionId, userId);
        collection.setName(request.getName());
        collection.setDescription(request.getDescription());
        return collectionRepository.save(collection);
    }

    @Transactional
    public void delete(UUID collectionId, UUID userId) {
        Collection collection = getById(collectionId, userId);
        collectionRepository.delete(collection);
    }
}
