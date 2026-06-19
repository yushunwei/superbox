package com.superbox.app.aiTranslate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TranslateResponse {
    private String translatedText;
    private String model;
    private String sourceLang;
    private String targetLang;
}
