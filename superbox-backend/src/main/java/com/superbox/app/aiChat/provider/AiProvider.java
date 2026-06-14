package com.superbox.app.aiChat.provider;

import com.superbox.app.aiChat.dto.ChatStreamEvent;
import com.superbox.app.aiChat.entity.ChatMessage;

import java.util.List;
import java.util.function.Consumer;

public interface AiProvider {
    void chatStream(String systemPrompt, List<ChatMessage> history, String model,
                    Consumer<ChatStreamEvent> onEvent);
    String getProviderName();
}
