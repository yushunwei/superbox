package com.superbox.app.aiChat.provider;

import com.superbox.app.aiChat.dto.ChatStreamEvent;
import com.superbox.app.aiChat.entity.ChatMessage;

import java.util.List;
import java.util.function.Consumer;

public interface AiProvider {
    void chatStream(String systemPrompt, List<ChatMessage> history, String model,
                    Consumer<ChatStreamEvent> onEvent);

    /** Call with user-configured apiKey/baseUrl overrides. Default delegates to chatStream. */
    default void chatStreamWithConfig(String systemPrompt, List<ChatMessage> history, String model,
                                      Consumer<ChatStreamEvent> onEvent, String apiKeyOverride, String baseUrlOverride) {
        chatStream(systemPrompt, history, model, onEvent);
    }

    String getProviderName();
}
