package com.superbox.app.aiTranslate.service;

import com.superbox.app.aiTranslate.dto.DocumentTranslateResponse;
import com.superbox.app.aiTranslate.dto.TranslateRequest;
import com.superbox.app.aiTranslate.dto.TranslateResponse;
import com.superbox.app.aiTranslate.entity.PromptTemplate;
import com.superbox.app.aiTranslate.entity.TranslateTask;
import com.superbox.app.aiTranslate.entity.TranslatorRole;
import com.superbox.app.aiTranslate.mapper.PromptMapper;
import com.superbox.app.aiTranslate.mapper.RoleMapper;
import com.superbox.app.aiTranslate.mapper.TranslateTaskMapper;
import com.superbox.app.aiChat.service.AiProviderRouter;
import com.superbox.app.aiTranslate.provider.TranslateProviderAdapter;
import com.superbox.app.modelManager.entity.ModelConfig;
import com.superbox.app.modelManager.enums.ApiFormat;
import com.superbox.app.modelManager.service.ModelManagerService;
import com.superbox.common.BusinessException;
import com.superbox.common.PageResult;
import com.superbox.common.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranslateService {

    private final AiProviderRouter aiProviderRouter;
    private final TranslateTaskMapper taskMapper;
    private final PromptMapper promptMapper;
    private final RoleMapper roleMapper;
    private final ModelManagerService modelManagerService;

    @Value("${superbox.storage.root-path:./storage}")
    private String storageRoot;

    /**
     * Synchronously translate text using the configured provider and prompt.
     */
    public TranslateResponse textTranslate(TranslateRequest request) {
        // validate input
        if (request.getText() == null || request.getText().isBlank()) {
            throw new BusinessException(400, "翻译文本不能为空");
        }
        if (request.getSourceLang() == null || request.getSourceLang().isBlank()) {
            throw new BusinessException(400, "源语言不能为空");
        }
        if (request.getTargetLang() == null || request.getTargetLang().isBlank()) {
            throw new BusinessException(400, "目标语言不能为空");
        }
        if (request.getSourceLang().equals(request.getTargetLang())) {
            throw new BusinessException(400, "源语言和目标语言不能相同");
        }

        String modelName = resolveModel(request);
        if (modelName == null || modelName.isBlank()) {
            throw new BusinessException(400, "请先在模型管理中配置翻译模型");
        }

        // Look up user's model config to get provider, apiKey, baseUrl
        Long userId = UserContext.getUserId();
        ModelConfig userModel = null;
        for (ModelConfig config : modelManagerService.getUserActiveModels(userId)) {
            if (modelName.equals(config.getModelName())) {
                userModel = config;
                break;
            }
        }
        if (userModel == null) {
            throw new BusinessException(400, "未找到模型配置: " + modelName);
        }

        String systemPrompt = buildSystemPrompt(request);

        String formatName = userModel.getApiFormat() != null
                ? userModel.getApiFormat()
                : ApiFormat.detectFromBaseUrl(userModel.getBaseUrl()).name();
        var aiProvider = aiProviderRouter.getProvider(formatName);
        log.info("Using {} provider with model {} for {}->{} translation",
                formatName, modelName, request.getSourceLang(), request.getTargetLang());

        String translated = TranslateProviderAdapter.translateWithConfig(aiProvider, systemPrompt, request.getText(),
                request.getSourceLang(), request.getTargetLang(), modelName,
                userModel.getApiKey(), userModel.getBaseUrl());

        return new TranslateResponse(translated, modelName, request.getSourceLang(), request.getTargetLang());
    }

    /**
     * Accept document upload and create an async translation task (skeleton for Phase 1-2).
     * The actual async processing will be implemented in Phase 3.
     */
    @Transactional
    public DocumentTranslateResponse documentTranslate(MultipartFile file, String sourceLang,
                                                        String targetLang, String model,
                                                        Long promptTemplateId, Long roleId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上传文件不能为空");
        }
        if (sourceLang == null || sourceLang.isBlank()) {
            throw new BusinessException(400, "源语言不能为空");
        }
        if (targetLang == null || targetLang.isBlank()) {
            throw new BusinessException(400, "目标语言不能为空");
        }

        String originalName = file.getOriginalFilename();
        String ext = getExtension(originalName);
        if (!isSupportedFormat(ext)) {
            throw new BusinessException(400, "不支持的文件格式: " + ext + "，支持: pdf, docx, xlsx, txt");
        }

        // store file
        String filePath = storeFile(file, ext);

        // create task record
        String resolvedModel = resolveModelName(model);
        if (resolvedModel == null || resolvedModel.isBlank()) {
            throw new BusinessException(400, "请先在模型管理中配置翻译模型");
        }
        TranslateTask task = new TranslateTask();
        task.setUserId(UserContext.getUserId());
        task.setFileName(originalName);
        task.setFileSize(file.getSize());
        task.setFilePath(filePath);
        task.setFileType(ext);
        task.setSourceLang(sourceLang);
        task.setTargetLang(targetLang);
        task.setModel(resolvedModel);
        task.setPromptTemplateId(promptTemplateId);
        task.setRoleId(roleId);
        task.setStatus("queued");
        task.setProgress(0);
        task.setCompletedSegments(0);
        taskMapper.insert(task);

        log.info("Created translate task id={}, file={}, {}->{}", task.getId(), originalName, sourceLang, targetLang);
        return new DocumentTranslateResponse(task.getId(), "queued");
    }

    public TranslateTask getTask(Long id) {
        TranslateTask task = taskMapper.findById(id);
        if (task == null) {
            throw new BusinessException(404, "翻译任务不存在");
        }
        return task;
    }

    public PageResult<TranslateTask> listTasks(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        List<TranslateTask> records = taskMapper.findByUserId(userId, offset, size);
        long total = taskMapper.countByUserId(userId);
        return new PageResult<>(records, total, page, size);
    }

    public Path getDownloadPath(Long id) {
        TranslateTask task = getTask(id);
        if (task.getOutputPath() == null || task.getOutputPath().isBlank()) {
            throw new BusinessException(404, "翻译结果文件不存在");
        }
        Path path = Paths.get(task.getOutputPath());
        if (!Files.exists(path)) {
            throw new BusinessException(404, "翻译结果文件已被清理");
        }
        return path;
    }

    @Transactional
    public void deleteTask(Long id) {
        TranslateTask task = getTask(id);
        // delete associated files
        deleteFileIfExists(task.getFilePath());
        deleteFileIfExists(task.getOutputPath());
        taskMapper.delete(id);
        log.info("Deleted translate task id={}", id);
    }

    @Transactional
    public void retryTask(Long id) {
        TranslateTask task = getTask(id);
        if (!"failed".equals(task.getStatus())) {
            throw new BusinessException(400, "只有失败的任务才能重试");
        }
        taskMapper.resetTask(id);
        log.info("Reset translate task id={} for retry", id);
    }

    /**
     * Build the system prompt from configured template or role, with variable substitution.
     */
    private String buildSystemPrompt(TranslateRequest request) {
        String systemPrompt = null;

        // If prompt template specified, use it
        if (request.getPromptTemplateId() != null) {
            PromptTemplate template = promptMapper.findById(request.getPromptTemplateId());
            if (template == null) {
                throw new BusinessException(404, "提示词模板不存在");
            }
            systemPrompt = template.getSystemPrompt();
        }

        // If role specified, use role's default prompt (overrides explicit template)
        if (request.getRoleId() != null) {
            TranslatorRole role = roleMapper.findById(request.getRoleId());
            if (role == null) {
                throw new BusinessException(404, "翻译角色不存在");
            }
            if (role.getDefaultPromptId() != null) {
                PromptTemplate template = promptMapper.findById(role.getDefaultPromptId());
                if (template != null) {
                    systemPrompt = template.getSystemPrompt();
                }
            }
        }

        // Default system prompt if none configured
        if (systemPrompt == null || systemPrompt.isBlank()) {
            systemPrompt = "你是一个专业的翻译助手。请将以下{source_lang}文本准确翻译成{target_lang}。\n\n待翻译文本：\n{text}";
        }

        // Variable substitution
        systemPrompt = systemPrompt
                .replace("{source_lang}", request.getSourceLang())
                .replace("{target_lang}", request.getTargetLang())
                .replace("{text}", request.getText());

        return systemPrompt;
    }

    private String resolveModel(TranslateRequest request) {
        if (request.getModel() != null && !request.getModel().isBlank()) {
            return request.getModel();
        }
        if (request.getRoleId() != null) {
            TranslatorRole role = roleMapper.findById(request.getRoleId());
            if (role != null && role.getModel() != null && !role.getModel().isBlank()) {
                return role.getModel();
            }
        }
        return null;
    }

    private String resolveModelName(String model) {
        return (model != null && !model.isBlank()) ? model : null;
    }

    private String storeFile(MultipartFile file, String ext) {
        try {
            String datePath = LocalDate.now().toString().replace("-", "/");
            Path dir = Paths.get(storageRoot, "ait", datePath);
            Files.createDirectories(dir);
            String storedName = UUID.randomUUID() + "." + ext;
            Path target = dir.resolve(storedName);
            file.transferTo(target);
            return target.toString().replace('\\', '/');
        } catch (IOException e) {
            log.error("File storage failed", e);
            throw new BusinessException(500, "文件存储失败: " + e.getMessage());
        }
    }

    private void deleteFileIfExists(String filePath) {
        if (filePath != null && !filePath.isBlank()) {
            try {
                Files.deleteIfExists(Paths.get(filePath));
            } catch (IOException e) {
                log.warn("Failed to delete file: {}", filePath, e);
            }
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int i = filename.lastIndexOf('.');
        return i >= 0 ? filename.substring(i + 1).toLowerCase() : "";
    }

    private boolean isSupportedFormat(String ext) {
        return "pdf".equals(ext) || "docx".equals(ext) || "xlsx".equals(ext) || "txt".equals(ext);
    }
}
