package com.superbox.app.knowledgeBase.entity;

import com.superbox.app.taskManager.entity.TagRef;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class KnowledgeEntry {
    private Long id;
    private Long folderId;
    private String title;
    private String sourceType;
    private Long sourceId;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private String contentText;
    private Integer chunkCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TagRef> tags;
    private List<KnowledgeChunk> chunks;
}
