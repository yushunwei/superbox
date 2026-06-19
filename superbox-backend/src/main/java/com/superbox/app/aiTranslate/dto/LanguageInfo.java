package com.superbox.app.aiTranslate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LanguageInfo {
    private String code;
    private String name;
    private String nativeName;
}
