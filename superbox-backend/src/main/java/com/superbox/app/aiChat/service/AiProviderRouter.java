package com.superbox.app.aiChat.service;

import com.superbox.app.aiChat.provider.AiProvider;
import com.superbox.app.modelManager.enums.ApiFormat;
import com.superbox.common.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AiProviderRouter {

    private final Map<ApiFormat, AiProvider> providerMap;

    public AiProviderRouter(AiProvider openAiCompatibleProvider,
                            AiProvider anthropicCompatibleProvider,
                            AiProvider ollamaProvider) {
        this.providerMap = Map.of(
                ApiFormat.OPENAI_COMPATIBLE, openAiCompatibleProvider,
                ApiFormat.ANTHROPIC_COMPATIBLE, anthropicCompatibleProvider,
                ApiFormat.OLLAMA, ollamaProvider
        );
    }

    public AiProvider getProvider(ApiFormat format) {
        AiProvider provider = providerMap.get(format);
        if (provider == null) {
            throw new BusinessException(500, "不支持的 API 格式: " + format);
        }
        return provider;
    }

    public AiProvider getProvider(String formatName) {
        return getProvider(ApiFormat.valueOf(formatName));
    }
}
