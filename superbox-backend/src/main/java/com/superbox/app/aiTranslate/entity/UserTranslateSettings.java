package com.superbox.app.aiTranslate.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserTranslateSettings {
    private Long id;
    private Long userId;
    private String defaultModel;
    private String defaultSourceLang;
    private String defaultTargetLang;
    private Double temperature;
    private Integer maxTokens;
    private String pdfFormat;
    private String wordFormat;
    private Boolean doc2xEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
