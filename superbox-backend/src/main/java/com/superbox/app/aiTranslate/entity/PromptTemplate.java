package com.superbox.app.aiTranslate.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PromptTemplate {
    private Long id;
    private Long userId;
    private String name;
    private String category;
    private String systemPrompt;
    private Boolean isPreset;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
