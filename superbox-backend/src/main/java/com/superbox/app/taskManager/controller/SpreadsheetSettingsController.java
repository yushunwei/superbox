package com.superbox.app.taskManager.controller;

import com.superbox.app.taskManager.entity.SpreadsheetSettings;
import com.superbox.app.taskManager.service.SpreadsheetSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/spreadsheet")
@RequiredArgsConstructor
public class SpreadsheetSettingsController {

    private final SpreadsheetSettingsService service;

    private Long getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth.getPrincipal();
        if (principal instanceof Long id) return id;
        if (principal instanceof Integer id) return id.longValue();
        if (principal instanceof String str) {
            try { return Long.parseLong(str); } catch (NumberFormatException e) { /* fall through */ }
        }
        throw new RuntimeException("Unable to determine user ID from principal: " + principal);
    }

    @GetMapping("/settings")
    public ResponseEntity<?> getSettings(@RequestParam String pageKey) {
        Long userId = getUserId();
        SpreadsheetSettings settings = service.getSettings(userId, pageKey);
        Map<String, Object> result = new HashMap<>();
        result.put("data", settings);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/settings")
    public ResponseEntity<?> saveSettings(@RequestBody Map<String, Object> body) {
        Long userId = getUserId();

        String pageKey = (String) body.get("pageKey");
        @SuppressWarnings("unchecked")
        Map<String, Object> colWidths = (Map<String, Object>) body.get("colWidths");
        @SuppressWarnings("unchecked")
        Map<String, Object> colAligns = (Map<String, Object>) body.get("colAligns");
        @SuppressWarnings("unchecked")
        Map<String, Object> colWraps = (Map<String, Object>) body.get("colWraps");
        @SuppressWarnings("unchecked")
        Map<String, Object> rowHeights = (Map<String, Object>) body.get("rowHeights");

        service.saveSettings(userId, pageKey, colWidths, colAligns, colWraps, rowHeights);
        Map<String, Object> result = new HashMap<>();
        result.put("message", "ok");
        return ResponseEntity.ok(result);
    }
}
