package com.superbox.app.aiTranslate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocumentTranslateResponse {
    private Long taskId;
    private String status;
}
