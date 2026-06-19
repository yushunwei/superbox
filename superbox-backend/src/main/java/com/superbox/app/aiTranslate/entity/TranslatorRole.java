package com.superbox.app.aiTranslate.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TranslatorRole {
    private Long id;
    private Long userId;
    private String name;
    private String description;
    private Long defaultPromptId;
    private String model;
    private Double temperature;
    private Integer maxTokens;
    private Boolean isPreset;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
