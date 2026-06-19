package com.superbox.app.modelManager.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ModelConfig {
    private Long id;
    private Long userId;
    private String providerName;
    private String apiKey;
    private String baseUrl;
    private String modelName;
    private String displayName;
    private String apiFormat;
    private Boolean isDefault;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
