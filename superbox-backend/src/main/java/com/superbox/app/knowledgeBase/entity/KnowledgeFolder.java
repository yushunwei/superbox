package com.superbox.app.knowledgeBase.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class KnowledgeFolder {
    private Long id;
    private String name;
    private Long parentId;
    private Integer sortOrder;
    private Integer entryCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<KnowledgeFolder> children;
}
