package com.superbox.app.taskManager.service;

import com.superbox.app.taskManager.entity.SpreadsheetSettings;
import com.superbox.app.taskManager.mapper.SpreadsheetSettingsMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SpreadsheetSettingsService {

    private final SpreadsheetSettingsMapper mapper;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public SpreadsheetSettings getSettings(Long userId, String pageKey) {
        return mapper.findByUserAndPage(userId, pageKey);
    }

    @Transactional
    public void saveSettings(Long userId, String pageKey,
                             Map<String, Object> colWidths,
                             Map<String, Object> colAligns,
                             Map<String, Object> colWraps,
                             Map<String, Object> rowHeights) {
        try {
            SpreadsheetSettings s = new SpreadsheetSettings();
            s.setUserId(userId);
            s.setPageKey(pageKey);
            s.setColWidths(colWidths != null ? objectMapper.writeValueAsString(colWidths) : null);
            s.setColAligns(colAligns != null ? objectMapper.writeValueAsString(colAligns) : null);
            s.setColWraps(colWraps != null ? objectMapper.writeValueAsString(colWraps) : null);
            s.setRowHeights(rowHeights != null ? objectMapper.writeValueAsString(rowHeights) : null);

            mapper.upsert(s);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save spreadsheet settings", e);
        }
    }
}
