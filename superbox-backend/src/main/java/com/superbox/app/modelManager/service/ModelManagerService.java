package com.superbox.app.modelManager.service;

import com.superbox.app.modelManager.dto.ModelConfigDTO;
import com.superbox.app.modelManager.entity.ModelConfig;
import com.superbox.app.modelManager.enums.ApiFormat;
import com.superbox.app.modelManager.mapper.ModelConfigMapper;
import com.superbox.common.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelManagerService {

    private final ModelConfigMapper mapper;

    public List<ModelConfig> listUserModels(Long userId) {
        if (userId == null) return List.of();
        return mapper.findByUserId(userId);
    }

    public ModelConfig getModel(Long id, Long userId) {
        ModelConfig config = mapper.findById(id);
        if (config == null) throw new BusinessException(404, "模型配置不存在");
        return config;
    }

    @Transactional
    public ModelConfig createModel(ModelConfigDTO dto, Long userId) {
        validateDTO(dto);
        ModelConfig config = new ModelConfig();
        config.setUserId(userId);
        config.setProviderName(dto.getProviderName().trim());
        config.setApiKey(blankToNull(dto.getApiKey()));
        config.setBaseUrl(blankToNull(dto.getBaseUrl()));
        config.setModelName(dto.getModelName().trim());
        config.setDisplayName(dto.getDisplayName() != null ? dto.getDisplayName().trim() : dto.getModelName().trim());
        // Use user-specified apiFormat if provided, otherwise auto-detect from baseUrl
        if (dto.getApiFormat() != null && !dto.getApiFormat().isBlank()) {
            config.setApiFormat(dto.getApiFormat().trim());
        } else {
            config.setApiFormat(ApiFormat.detectFromBaseUrl(config.getBaseUrl()).name());
        }
        config.setIsDefault(Boolean.TRUE.equals(dto.getIsDefault()));
        config.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);

        if (Boolean.TRUE.equals(config.getIsDefault())) {
            mapper.clearDefaults(userId);
        }
        mapper.insert(config);
        log.info("Created model config id={}, provider={}, model={}", config.getId(), config.getProviderName(), config.getModelName());
        return mapper.findById(config.getId());
    }

    @Transactional
    public ModelConfig updateModel(Long id, ModelConfigDTO dto, Long userId) {
        ModelConfig existing = getModel(id, userId);
        if (dto.getProviderName() != null) existing.setProviderName(dto.getProviderName().trim());
        if (dto.getApiKey() != null) existing.setApiKey(blankToNull(dto.getApiKey()));
        if (dto.getBaseUrl() != null) existing.setBaseUrl(blankToNull(dto.getBaseUrl()));
        if (dto.getModelName() != null) existing.setModelName(dto.getModelName().trim());
        if (dto.getDisplayName() != null) existing.setDisplayName(dto.getDisplayName().trim());
        if (dto.getIsDefault() != null) {
            if (Boolean.TRUE.equals(dto.getIsDefault())) {
                mapper.clearDefaults(userId);
            }
            existing.setIsDefault(dto.getIsDefault());
        }
        if (dto.getIsActive() != null) existing.setIsActive(dto.getIsActive());
        // Use user-specified apiFormat if provided, otherwise re-detect from baseUrl
        if (dto.getApiFormat() != null && !dto.getApiFormat().isBlank()) {
            existing.setApiFormat(dto.getApiFormat().trim());
        } else if (dto.getBaseUrl() != null) {
            // baseUrl changed, re-detect
            existing.setApiFormat(ApiFormat.detectFromBaseUrl(existing.getBaseUrl()).name());
        }
        validateEntity(existing);
        mapper.update(existing);
        log.info("Updated model config id={}", id);
        return mapper.findById(id);
    }

    @Transactional
    public void deleteModel(Long id, Long userId) {
        getModel(id, userId);
        mapper.delete(id);
        log.info("Deleted model config id={}", id);
    }

    @Transactional
    public void setDefault(Long id, Long userId) {
        getModel(id, userId);
        mapper.clearDefaults(userId);
        mapper.setDefault(id);
        log.info("Set default model config id={}", id);
    }

    public Map<String, Object> testConnection(String providerName, String apiKey, String baseUrl, String modelName) {
        long start = System.currentTimeMillis();
        String effectiveBaseUrl = (baseUrl != null && !baseUrl.isBlank() ? baseUrl : getDefaultBaseUrl(providerName));

        // Try chat completions with a minimal message to validate the full config
        // path + whether it uses Anthropic auth/body format
        record PathEntry(String path, boolean isAnthropic) {}
        PathEntry[] pathsToTry = {
            new PathEntry("/chat/completions", false),
            new PathEntry("/v1/chat/completions", false),
            new PathEntry("/messages", true),
            new PathEntry("/v1/messages", true),
        };
        String openAiBody = "{\"model\":\"" + modelName + "\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}],\"max_tokens\":5}";
        String anthropicBody = "{\"model\":\"" + modelName + "\",\"messages\":[{\"role\":\"user\",\"content\":[{\"type\":\"text\",\"text\":\"hi\"}]}],\"max_tokens\":5}";

        String lastError = null;
        String lastTestedUrl = null;

        for (PathEntry entry : pathsToTry) {
            String fullUrl = effectiveBaseUrl + entry.path;
            lastTestedUrl = fullUrl;
            try {
                HttpURLConnection conn = (HttpURLConnection) URI.create(fullUrl).toURL().openConnection();
                conn.setRequestMethod("POST");
                if (entry.isAnthropic) {
                    conn.setRequestProperty("x-api-key", apiKey);
                    conn.setRequestProperty("anthropic-version", "2023-06-01");
                } else {
                    conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                }
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);

                String reqBody = entry.isAnthropic ? anthropicBody : openAiBody;
                byte[] body = reqBody.getBytes(StandardCharsets.UTF_8);
                OutputStream os = conn.getOutputStream();
                os.write(body);
                os.close();

                int status = conn.getResponseCode();
                long elapsed = System.currentTimeMillis() - start;

                if (status == 200) {
                    // Read a bit of the response to confirm it's valid
                    Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8).useDelimiter("\\A");
                    String responseBody = scanner.hasNext() ? scanner.next() : "";
                    scanner.close();
                    String preview = responseBody.length() > 150 ? responseBody.substring(0, 150) + "..." : responseBody;
                    return Map.of("success", true, "message", "连接成功",
                            "testedUrl", fullUrl, "responseTimeMs", elapsed, "responsePreview", preview);
                }
                if (status == 401 || status == 403) {
                    lastError = "API Key 无效";
                    continue;
                }
                if (status == 404) {
                    lastError = "端点不存在: " + fullUrl;
                    continue;
                }
                lastError = "服务器返回 HTTP " + status;
            } catch (Exception e) {
                lastError = effectiveBaseUrl + " 请求失败: " + e.getMessage();
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        return Map.of("success", false, "message",
                lastError != null ? lastError : "所有路径均无法连接",
                "testedUrl", lastTestedUrl, "responseTimeMs", elapsed);
    }

    public List<Map<String, String>> getProviderTypes() {
        return List.of(
                Map.of("providerName", "openai", "displayName", "OpenAI"),
                Map.of("providerName", "deepseek", "displayName", "DeepSeek"),
                Map.of("providerName", "anthropic", "displayName", "Anthropic"),
                Map.of("providerName", "ollama", "displayName", "Ollama"),
                Map.of("providerName", "custom", "displayName", "自定义")
        );
    }

    /** Get user's default model config for translation fallback. */
    public ModelConfig getUserDefaultModel(Long userId) {
        if (userId == null) return null;
        return mapper.findDefaultByUserId(userId);
    }

    /** Get all active user models for populating the model selector. */
    public List<ModelConfig> getUserActiveModels(Long userId) {
        if (userId == null) return List.of();
        return mapper.findByUserId(userId).stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsActive()))
                .toList();
    }

    private void validateDTO(ModelConfigDTO dto) {
        if (dto.getProviderName() == null || dto.getProviderName().isBlank()) {
            throw new BusinessException(400, "供应商不能为空");
        }
        if (dto.getModelName() == null || dto.getModelName().isBlank()) {
            throw new BusinessException(400, "模型名称不能为空");
        }
    }

    private void validateEntity(ModelConfig config) {
        if (config.getProviderName() == null || config.getProviderName().isBlank()) {
            throw new BusinessException(400, "供应商不能为空");
        }
        if (config.getModelName() == null || config.getModelName().isBlank()) {
            throw new BusinessException(400, "模型名称不能为空");
        }
    }

    private String blankToNull(String s) {
        return s != null && s.isBlank() ? null : s;
    }

    private String getDefaultBaseUrl(String providerName) {
        return switch (providerName) {
            case "openai" -> "https://api.openai.com/v1";
            case "deepseek" -> "https://api.deepseek.com/v1";
            case "anthropic" -> "https://api.anthropic.com";
            case "ollama" -> "http://localhost:11434";
            default -> "https://api.openai.com/v1";
        };
    }
}
