package com.superbox.app.aiChat.service;

import com.superbox.app.aiChat.provider.AiProvider;
import com.superbox.app.modelManager.entity.ModelConfig;
import com.superbox.app.modelManager.enums.ApiFormat;
import com.superbox.app.modelManager.service.ModelManagerService;
import com.superbox.common.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelRouterService {

    private final AiProviderRouter providerRouter;
    private final ModelManagerService modelManagerService;

    /** Get available models from user's ModelManager config. */
    public List<ModelConfig> availableModels() {
        Long userId = UserContext.getUserId();
        return modelManagerService.getUserActiveModels(userId);
    }

    /** Route to the correct provider based on model's API format in ModelConfig. */
    public AiProvider route(ModelConfig userModel) {
        String formatName = userModel.getApiFormat() != null
                ? userModel.getApiFormat()
                : ApiFormat.detectFromBaseUrl(userModel.getBaseUrl()).name();
        return providerRouter.getProvider(formatName);
    }

    /** Convenience: route by finding the model config first, then routing by format. */
    public AiProvider routeByModelName(String modelName) {
        Long userId = UserContext.getUserId();
        for (ModelConfig config : modelManagerService.getUserActiveModels(userId)) {
            if (modelName.equals(config.getModelName())) {
                return route(config);
            }
        }
        // Fallback: try apiFormat detection from model name as last resort
        String formatName = ApiFormat.detectFromBaseUrl(null).name();
        return providerRouter.getProvider(formatName);
    }
}
