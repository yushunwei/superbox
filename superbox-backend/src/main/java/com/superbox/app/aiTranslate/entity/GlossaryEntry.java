package com.superbox.app.aiTranslate.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GlossaryEntry {
    private Long id;
    private Long userId;
    private String sourceLang;
    private String targetLang;
    private String sourceTerm;
    private String targetTerm;
    private String category;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
