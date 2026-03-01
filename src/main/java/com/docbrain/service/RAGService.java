package com.docbrain.service;

import com.docbrain.model.dto.response.QAResponse;
import com.docbrain.model.dto.response.SourceCitation;
import com.docbrain.model.entity.Message;
import com.docbrain.model.enums.MessageRole;
import com.docbrain.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RAGService {

    private final EmbeddingService embeddingService;
    private final GeminiService geminiService;
    private final DocumentChunkRepository documentChunkRepository;

    private static final int TOP_K = 5;

    private static final String SYSTEM_PROMPT = """
            You are DocBrain, an AI assistant that answers questions based ONLY on the
            provided document context. If the answer is not in the context, say
            "I couldn't find this information in the uploaded documents."

            Always cite your sources using [Source: document_name, chunk X] format.

            CONTEXT:
            ---
            %s
            ---

            CONVERSATION HISTORY:
            %s

            USER QUESTION: %s

            Provide a detailed, accurate answer based only on the context above.
            """;

    /**
     * Full RAG pipeline: embed question → vector search → build prompt → LLM → response with citations.
     */
    public QAResponse ask(UUID collectionId, String question, List<Message> conversationHistory) {
        // Step 1: Embed the question
        float[] queryEmbedding = embeddingService.embed(question);
        String vectorString = embeddingService.toVectorString(queryEmbedding);

        // Step 2: Vector similarity search
        List<Object[]> results = documentChunkRepository.findSimilarChunks(collectionId, vectorString, TOP_K);

        if (results.isEmpty()) {
            return QAResponse.builder()
                    .answer("No relevant documents found in this collection. Please upload documents first.")
                    .sources(List.of())
                    .build();
        }

        // Step 3: Build context from retrieved chunks and collect citations
        StringBuilder contextBuilder = new StringBuilder();
        List<SourceCitation> citations = new ArrayList<>();

        for (Object[] row : results) {
            UUID chunkId = UUID.fromString(row[0].toString());
            String content = (String) row[1];
            int chunkIndex = ((Number) row[2]).intValue();
            String fileName = (String) row[3];
            double similarity = ((Number) row[4]).doubleValue();

            contextBuilder.append(String.format("[Source: %s, chunk %d]\n%s\n\n", fileName, chunkIndex, content));

            citations.add(SourceCitation.builder()
                    .chunkId(chunkId)
                    .documentName(fileName)
                    .chunkIndex(chunkIndex)
                    .relevanceScore(similarity)
                    .snippet(content.length() > 200 ? content.substring(0, 200) + "..." : content)
                    .build());
        }

        // Step 4: Build conversation history string
        String historyString = buildConversationHistory(conversationHistory);

        // Step 5: Build full prompt and call LLM
        String prompt = String.format(SYSTEM_PROMPT, contextBuilder.toString(), historyString, question);
        String answer = geminiService.generateText(prompt);

        log.info("RAG query completed for collection {} with {} sources", collectionId, citations.size());

        return QAResponse.builder()
                .answer(answer)
                .sources(citations)
                .build();
    }

    private String buildConversationHistory(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return "(No previous messages)";
        }

        StringBuilder sb = new StringBuilder();
        // Use last 10 messages max for context window
        List<Message> recent = messages.size() > 10 ? messages.subList(messages.size() - 10, messages.size()) : messages;

        for (Message msg : recent) {
            String role = msg.getRole() == MessageRole.USER ? "User" : "Assistant";
            sb.append(role).append(": ").append(msg.getContent()).append("\n");
        }

        return sb.toString();
    }
}
