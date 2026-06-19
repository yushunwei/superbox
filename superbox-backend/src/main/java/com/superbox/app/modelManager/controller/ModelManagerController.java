package com.superbox.app.modelManager.controller;

import com.superbox.app.modelManager.dto.ModelConfigDTO;
import com.superbox.app.modelManager.entity.ModelConfig;
import com.superbox.app.modelManager.service.ModelManagerService;
import com.superbox.common.Result;
import com.superbox.common.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/model-manager")
@RequiredArgsConstructor
public class ModelManagerController {

    private final ModelManagerService service;

    @GetMapping("/models")
    public Result<Map<String, Object>> listModels() {
        Long userId = UserContext.getUserId();
        List<ModelConfig> userModels = service.listUserModels(userId);
        List<Map<String, String>> providerTypes = service.getProviderTypes();
        return Result.ok(Map.of("userModels", userModels, "providerTypes", providerTypes));
    }

    @GetMapping("/models/{id}")
    public Result<ModelConfig> getModel(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        return Result.ok(service.getModel(id, userId));
    }

    @PostMapping("/models")
    public Result<ModelConfig> createModel(@RequestBody ModelConfigDTO dto) {
        Long userId = UserContext.getUserId();
        return Result.ok(service.createModel(dto, userId));
    }

    @PutMapping("/models/{id}")
    public Result<ModelConfig> updateModel(@PathVariable Long id, @RequestBody ModelConfigDTO dto) {
        Long userId = UserContext.getUserId();
        return Result.ok(service.updateModel(id, dto, userId));
    }

    @DeleteMapping("/models/{id}")
    public Result<Void> deleteModel(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        service.deleteModel(id, userId);
        return Result.ok();
    }

    @PutMapping("/models/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        service.setDefault(id, userId);
        return Result.ok();
    }

    @PostMapping("/models/test")
    public Result<Map<String, Object>> testConnection(@RequestBody Map<String, Object> body) {
        String providerName = (String) body.get("providerName");
        String apiKey = (String) body.get("apiKey");
        String baseUrl = (String) body.get("baseUrl");
        String modelName = (String) body.get("modelName");
        return Result.ok(service.testConnection(providerName, apiKey, baseUrl, modelName));
    }
}
