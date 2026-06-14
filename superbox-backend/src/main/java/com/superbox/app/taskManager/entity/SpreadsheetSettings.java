package com.superbox.app.taskManager.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SpreadsheetSettings {
    private Long id;
    private Long userId;
    private String pageKey;
    private String colWidths;
    private String colAligns;
    private String colWraps;
    private String rowHeights;
    private LocalDateTime updatedAt;
}
