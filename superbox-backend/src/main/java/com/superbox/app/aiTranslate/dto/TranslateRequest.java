package com.superbox.app.aiTranslate.dto;

import lombok.Data;

@Data
public class TranslateRequest {
    private String text;
    private String sourceLang;
    private String targetLang;
    private String model;
    private Long promptTemplateId;
    private Long roleId;
}
