package com.superbox.app.aiTranslate.controller;

import com.superbox.app.aiTranslate.dto.*;
import com.superbox.app.aiTranslate.entity.TranslateTask;
import com.superbox.app.aiTranslate.service.TranslateService;
import com.superbox.app.modelManager.entity.ModelConfig;
import com.superbox.app.modelManager.service.ModelManagerService;
import com.superbox.common.PageResult;
import com.superbox.common.Result;
import com.superbox.common.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ai-translate")
@RequiredArgsConstructor
public class TranslateController {

    private final TranslateService translateService;
    private final ModelManagerService modelManagerService;

    /** Synchronously translate text. */
    @PostMapping("/text")
    public Result<TranslateResponse> translateText(@RequestBody TranslateRequest request) {
        return Result.ok(translateService.textTranslate(request));
    }

    /** Upload document for async translation (skeleton for Phase 1-2). */
    @PostMapping("/document")
    public Result<DocumentTranslateResponse> translateDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam String sourceLang,
            @RequestParam String targetLang,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) Long promptTemplateId,
            @RequestParam(required = false) Long roleId) {
        return Result.ok(translateService.documentTranslate(file, sourceLang, targetLang, model, promptTemplateId, roleId));
    }

    /** List translation tasks with pagination. */
    @GetMapping("/tasks")
    public Result<PageResult<TranslateTask>> listTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = UserContext.getUserId();
        return Result.ok(translateService.listTasks(userId, page, size));
    }

    /** Get a single translation task. */
    @GetMapping("/tasks/{id}")
    public Result<TranslateTask> getTask(@PathVariable Long id) {
        return Result.ok(translateService.getTask(id));
    }

    /** Download the translated file. */
    @GetMapping("/tasks/{id}/download")
    public ResponseEntity<Resource> downloadTask(@PathVariable Long id) {
        Path filePath = translateService.getDownloadPath(id);
        Resource resource = new FileSystemResource(filePath);
        String fileName = filePath.getFileName().toString();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /** Delete a translation task and its files. */
    @DeleteMapping("/tasks/{id}")
    public Result<Void> deleteTask(@PathVariable Long id) {
        translateService.deleteTask(id);
        return Result.ok();
    }

    /** Retry a failed translation task. */
    @PostMapping("/tasks/{id}/retry")
    public Result<Void> retryTask(@PathVariable Long id) {
        translateService.retryTask(id);
        return Result.ok();
    }

    /** Get available translation models from user configuration. */
    @GetMapping("/models")
    public Result<List<ModelInfo>> getModels() {
        Long userId = UserContext.getUserId();
        List<ModelInfo> models = new java.util.ArrayList<>();

        // Add user-configured models from modelManager
        for (ModelConfig config : modelManagerService.getUserActiveModels(userId)) {
            String displayName = config.getDisplayName() != null ? config.getDisplayName() : config.getModelName();
            models.add(new ModelInfo(displayName, config.getModelName(), config.getProviderName(),
                    true, Boolean.TRUE.equals(config.getIsDefault())));
        }

        return Result.ok(models);
    }

    /** Get supported languages for translation. */
    @GetMapping("/languages")
    public Result<List<LanguageInfo>> getLanguages() {
        List<LanguageInfo> languages = List.of(
                new LanguageInfo("de", "German", "Deutsch"),
                new LanguageInfo("es", "Spanish", "Español"),
                new LanguageInfo("it", "Italian", "Italiano"),
                new LanguageInfo("en", "English", "English"),
                new LanguageInfo("pt", "Portuguese", "Português"),
                new LanguageInfo("zh", "Chinese", "中文"),
                new LanguageInfo("fr", "French", "Français"),
                new LanguageInfo("hu", "Hungarian", "Magyar"),
                new LanguageInfo("nl", "Dutch", "Nederlands"),
                new LanguageInfo("no", "Norwegian", "Norsk"),
                new LanguageInfo("ro", "Romanian", "Română"),
                new LanguageInfo("ru", "Russian", "Русский")
        );
        return Result.ok(languages);
    }
}
