package com.docbrain.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SourceCitation {

    private UUID chunkId;
    private String documentName;
    private Double relevanceScore;
    private String snippet;
    private Integer chunkIndex;
}
