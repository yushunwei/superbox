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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Component
public class OllamaProvider implements AiProvider {

    private final AiConfig aiConfig;
    private final ObjectMapper mapper = new ObjectMapper();

    public OllamaProvider(AiConfig aiConfig) {
        this.aiConfig = aiConfig;
    }

    @Override
    public String getProviderName() { return "ollama"; }

    @Override
    public void chatStream(String systemPrompt, List<ChatMessage> history, String model,
                           Consumer<ChatStreamEvent> onEvent) {
        AiConfig.ProviderConfig cfg = aiConfig.getProviders().get("ollama");
        String url = cfg.getBaseUrl() + "/api/chat";
        if (model == null || model.isEmpty()) model = cfg.getChatModel();

        try {
            var body = buildRequestBody(systemPrompt, history, model);
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(300000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(mapper.writeValueAsBytes(body));
            }

            int status = conn.getResponseCode();
            if (status != 200) {
                onEvent.accept(new ChatStreamEvent("error", "Ollama error: HTTP " + status, null, null, null));
                return;
            }

            try (var reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder answerBuf = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> chunk = mapper.readValue(line, Map.class);
                        var msg = (Map<String, Object>) chunk.get("message");
                        if (msg != null) {
                            String content = (String) msg.get("content");
                            if (content != null) {
                                answerBuf.append(content);
                                onEvent.accept(new ChatStreamEvent("answer", content, null, null, null));
                            }
                        }
                        if (Boolean.TRUE.equals(chunk.get("done"))) break;
                    } catch (Exception e) {
                        log.debug("Skip chunk: {}", e.getMessage());
                    }
                }
                onEvent.accept(new ChatStreamEvent("done", null, null, null, answerBuf.length()));
            }
        } catch (Exception e) {
            log.error("Ollama stream error", e);
            onEvent.accept(new ChatStreamEvent("error", e.getMessage(), null, null, null));
        }
    }

    private Map<String, Object> buildRequestBody(String systemPrompt, List<ChatMessage> history, String model) {
        var messages = new ArrayList<Map<String, String>>();
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
