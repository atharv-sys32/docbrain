package com.docbrain.service;

import com.docbrain.exception.ResourceNotFoundException;
import com.docbrain.model.entity.Collection;
import com.docbrain.model.entity.Conversation;
import com.docbrain.model.entity.Message;
import com.docbrain.model.entity.User;
import com.docbrain.model.enums.MessageRole;
import com.docbrain.repository.ConversationRepository;
import com.docbrain.repository.MessageRepository;
import com.docbrain.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final CollectionService collectionService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public Conversation create(UUID userId, UUID collectionId, String title) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Collection collection = collectionService.getById(collectionId, userId);

        Conversation conversation = Conversation.builder()
                .user(user)
                .collection(collection)
                .title(title != null ? title : "New Chat")
                .build();

        return conversationRepository.save(conversation);
    }

    public List<Conversation> listByCollection(UUID collectionId, UUID userId) {
        collectionService.getById(collectionId, userId);
        return conversationRepository.findByCollectionIdAndUserIdOrderByUpdatedAtDesc(collectionId, userId);
    }

    public Conversation getById(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!conversation.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Conversation not found");
        }

        return conversation;
    }

    public List<Message> getMessages(UUID conversationId, UUID userId) {
        getById(conversationId, userId); // ownership check
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    @Transactional
    public void delete(UUID conversationId, UUID userId) {
        Conversation conversation = getById(conversationId, userId);
        conversationRepository.delete(conversation);
    }

    public Message saveUserMessage(Conversation conversation, String content) {
        Message message = Message.builder()
                .conversation(conversation)
                .role(MessageRole.USER)
                .content(content)
                .build();

        return messageRepository.save(message);
    }

    public Message saveAssistantMessage(Conversation conversation, String content, Object sources) {
        String sourcesJson = null;
        if (sources != null) {
            try {
                sourcesJson = objectMapper.writeValueAsString(sources);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize sources to JSON", e);
            }
        }

        Message message = Message.builder()
                .conversation(conversation)
                .role(MessageRole.ASSISTANT)
                .content(content)
                .sources(sourcesJson)
                .build();

        return messageRepository.save(message);
    }

    public void updateTitle(Conversation conversation, String title) {
        conversation.setTitle(title);
        conversationRepository.save(conversation);
    }
}
