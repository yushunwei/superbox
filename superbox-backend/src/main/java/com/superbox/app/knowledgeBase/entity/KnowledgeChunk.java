package com.superbox.app.knowledgeBase.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KnowledgeChunk {
    private Long id;
    private Long entryId;
    private Integer chunkIndex;
    private String content;
    private Integer tokenCount;
    private LocalDateTime createdAt;
}
