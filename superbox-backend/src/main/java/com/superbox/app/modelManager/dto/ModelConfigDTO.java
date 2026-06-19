package com.superbox.app.modelManager.dto;

import lombok.Data;

@Data
public class ModelConfigDTO {
    private String providerName;
    private String apiKey;
    private String baseUrl;
    private String modelName;
    private String apiFormat;
    private String displayName;
    private Boolean isDefault;
    private Boolean isActive;
}
