package com.superbox.app.aiTranslate.provider;

import com.superbox.app.aiChat.dto.ChatStreamEvent;
import com.superbox.app.aiChat.entity.ChatMessage;
import com.superbox.app.aiChat.provider.AiProvider;
import com.superbox.common.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Consumer;

/**
 * Helper that adapts the streaming AiProvider.chatStream interface to a synchronous
 * translate call. Since chatStream processes the entire SSE stream synchronously
 * before returning, we simply collect the answer text from the callback events.
 */
@Slf4j
public class TranslateProviderAdapter {

    /**
     * Call the given AiProvider synchronously and collect the full translation result.
     */
    public static String translate(AiProvider aiProvider, String systemPrompt, String text,
                                   String sourceLang, String targetLang, String model) {
        return translateWithConfig(aiProvider, systemPrompt, text, sourceLang, targetLang, model, null, null);
    }

    /** Call with user-configured apiKey/baseUrl overrides. */
    public static String translateWithConfig(AiProvider aiProvider, String systemPrompt, String text,
                                             String sourceLang, String targetLang, String model,
                                             String apiKeyOverride, String baseUrlOverride) {
        StringBuilder result = new StringBuilder();
        StringBuilder error = new StringBuilder();

        ChatMessage userMsg = new ChatMessage();
        userMsg.setRole("user");
        userMsg.setContent(text);

        Consumer<ChatStreamEvent> callback = event -> {
            if ("answer".equals(event.getType())) {
                if (event.getDelta() != null) {
                    result.append(event.getDelta());
                }
            } else if ("error".equals(event.getType())) {
                if (event.getDelta() != null) {
                    error.append(event.getDelta());
                }
            }
        };

        aiProvider.chatStreamWithConfig(systemPrompt, List.of(userMsg), model, callback, apiKeyOverride, baseUrlOverride);

        if (error.length() > 0) {
            log.error("Translation provider error: {}", error);
            throw new BusinessException(500, "翻译服务调用失败: " + error);
        }

        if (result.isEmpty()) {
            throw new BusinessException(500, "翻译结果为空，请稍后重试");
        }

        return result.toString();
    }
}
