package com.docbrain.model.dto.response;

import com.docbrain.model.enums.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponse {

    private UUID id;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private Integer totalChunks;
    private DocumentStatus status;
    private LocalDateTime createdAt;
}
