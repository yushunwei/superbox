package com.superbox.app.aiChat.service;

import com.superbox.app.aiChat.provider.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelRouterService {

    private final OpenAiProvider openAiProvider;
    private final DeepSeekProvider deepSeekProvider;
    private final AnthropicProvider anthropicProvider;
    private final OllamaProvider ollamaProvider;

    public List<String> availableModels() {
        return List.of(
                "gpt-4o", "gpt-4o-mini",
                "deepseek-chat", "deepseek-reasoner",
                "claude-sonnet-4-6",
                "qwen2.5"
        );
    }

    public AiProvider route(String modelId) {
        if (modelId == null) return openAiProvider;
        if (modelId.startsWith("gpt") || modelId.startsWith("o1")) {
            return openAiProvider;
        }
        if (modelId.startsWith("deepseek")) {
            return deepSeekProvider;
        }
        if (modelId.startsWith("claude")) {
            return anthropicProvider;
        }
        if (modelId.contains("qwen") || modelId.contains("llama") || modelId.contains("gemma")) {
            return ollamaProvider;
        }
        return openAiProvider;
    }
}
