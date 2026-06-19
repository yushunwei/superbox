package com.superbox.app.aiChat.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superbox.app.aiChat.dto.ChatStreamEvent;
import com.superbox.app.aiChat.entity.ChatMessage;
import com.superbox.config.AiConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Component
public class OpenAiCompatibleProvider implements AiProvider {

    private final AiConfig aiConfig;
    private final ObjectMapper mapper = new ObjectMapper();

    public OpenAiCompatibleProvider(AiConfig aiConfig) {
        this.aiConfig = aiConfig;
    }

    @Override
    public String getProviderName() { return "openai-compatible"; }

    @Override
    public void chatStream(String systemPrompt, List<ChatMessage> history, String model,
                           Consumer<ChatStreamEvent> onEvent) {
        doChatStream(systemPrompt, history, model, onEvent, null, null);
    }

    @Override
    public void chatStreamWithConfig(String systemPrompt, List<ChatMessage> history, String model,
                              Consumer<ChatStreamEvent> onEvent, String apiKeyOverride, String baseUrlOverride) {
        doChatStream(systemPrompt, history, model, onEvent, apiKeyOverride, baseUrlOverride);
    }

    private void doChatStream(String systemPrompt, List<ChatMessage> history, String model,
                              Consumer<ChatStreamEvent> onEvent, String apiKeyOverride, String baseUrlOverride) {
        AiConfig.ProviderConfig cfg = aiConfig.getProviders().get("openai-compatible");
        String apiKey = apiKeyOverride != null && !apiKeyOverride.isBlank() ? apiKeyOverride : cfg.getApiKey();
        String baseUrl = baseUrlOverride != null && !baseUrlOverride.isBlank() ? baseUrlOverride : cfg.getBaseUrl();
        String url = baseUrl + "/chat/completions";
        if (model == null || model.isEmpty()) model = cfg.getChatModel();

        try {
            var body = buildRequestBody(systemPrompt, history, model);
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "text/event-stream");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(300000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(mapper.writeValueAsBytes(body));
            }

            int status = conn.getResponseCode();
            if (status != 200) {
                onEvent.accept(new ChatStreamEvent("error", "Provider error: HTTP " + status, null, null, null));
                return;
            }

            try (var reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder reasoningBuf = new StringBuilder();
                StringBuilder answerBuf = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String json = line.substring(6);
                        if ("[DONE]".equals(json)) break;
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> chunk = mapper.readValue(json, Map.class);
                            var choices = (List<Map<String, Object>>) chunk.get("choices");
                            if (choices != null && !choices.isEmpty()) {
                                var delta = (Map<String, Object>) choices.get(0).get("delta");
                                if (delta != null) {
                                    String content = (String) delta.get("content");
                                    String reasoning = (String) delta.get("reasoning_content");
                                    if (reasoning != null) {
                                        reasoningBuf.append(reasoning);
                                        onEvent.accept(new ChatStreamEvent("reasoning", reasoning, null, null, null));
                                    } else if (content != null) {
                                        answerBuf.append(content);
                                        onEvent.accept(new ChatStreamEvent("answer", content, null, null, null));
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.debug("Skip chunk: {}", e.getMessage());
                        }
                    }
                }
                onEvent.accept(new ChatStreamEvent("done", null, null, null,
                        reasoningBuf.length() + answerBuf.length()));
            }
        } catch (Exception e) {
            log.error("OpenAI-compatible stream error", e);
            onEvent.accept(new ChatStreamEvent("error", e.getMessage(), null, null, null));
        }
    }

    private Map<String, Object> buildRequestBody(String systemPrompt, List<ChatMessage> history, String model) {
        var messages = new java.util.ArrayList<Map<String, String>>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        for (var msg : history) {
            messages.add(Map.of("role", msg.getRole(), "content",
                    msg.getContent() != null ? msg.getContent() : ""));
        }
        return Map.of("model", model, "messages", messages, "stream", true);
    }
}
