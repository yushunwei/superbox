package com.superbox.app.aiTranslate.controller;

import com.superbox.app.aiTranslate.service.TranslateSettingsService;
import com.superbox.common.Result;
import com.superbox.common.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai-translate")
@RequiredArgsConstructor
public class SettingsController {

    private final TranslateSettingsService settingsService;

    @GetMapping("/settings")
    public Result<Map<String, Object>> getSettings() {
        Long userId = UserContext.getUserId();
        return Result.ok(settingsService.getSettings(userId));
    }

    @PutMapping("/settings")
    public Result<Map<String, Object>> saveSettings(@RequestBody Map<String, Object> settings) {
        Long userId = UserContext.getUserId();
        return Result.ok(settingsService.saveSettings(userId, settings));
    }
}
