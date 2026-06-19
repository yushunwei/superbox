package com.superbox.app.aiTranslate.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ModelInfo {
    private String name;
    private String model;
    private String provider;
    private boolean available;

    @JsonProperty("isDefault")
    private boolean isDefault;
}
