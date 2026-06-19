package com.superbox.app.aiTranslate.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TranslateTask {
    private Long id;
    private Long userId;
    private String fileName;
    private Long fileSize;
    private String filePath;
    private String outputPath;
    private String fileType;
    private String sourceLang;
    private String targetLang;
    private String model;
    private Long promptTemplateId;
    private Long roleId;
    private String status;
    private Integer progress;
    private Integer totalSegments;
    private Integer completedSegments;
    private String errorMsg;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
