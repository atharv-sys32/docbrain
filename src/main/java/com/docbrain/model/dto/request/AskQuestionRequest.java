package com.docbrain.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AskQuestionRequest {

    @NotBlank(message = "Question is required")
    @Size(max = 2000, message = "Question must be at most 2000 characters")
    private String question;

    private String conversationId;
}
