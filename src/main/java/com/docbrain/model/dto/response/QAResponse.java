package com.docbrain.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QAResponse {

    private String answer;
    private List<SourceCitation> sources;
    private String conversationId;
}
