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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Component
public class AnthropicProvider implements AiProvider {

    private final AiConfig aiConfig;
    private final ObjectMapper mapper = new ObjectMapper();

    public AnthropicProvider(AiConfig aiConfig) {
        this.aiConfig = aiConfig;
    }

    @Override
    public String getProviderName() { return "anthropic"; }

    @Override
    public void chatStream(String systemPrompt, List<ChatMessage> history, String model,
                           Consumer<ChatStreamEvent> onEvent) {
        AiConfig.ProviderConfig cfg = aiConfig.getProviders().get("anthropic");
        String apiKey = cfg.getApiKey();
        String url = cfg.getBaseUrl() + "/messages";
        if (model == null || model.isEmpty()) model = cfg.getChatModel();

        try {
            var body = buildRequestBody(systemPrompt, history, model);
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("x-api-key", apiKey);
            conn.setRequestProperty("anthropic-version", "2023-06-01");
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
                onEvent.accept(new ChatStreamEvent("error", "Anthropic error: HTTP " + status, null, null, null));
                return;
            }

            try (var reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder reasoningBuf = new StringBuilder();
                StringBuilder answerBuf = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String json = line.substring(6);
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> event = mapper.readValue(json, Map.class);
                            String type = (String) event.get("type");
                            if ("content_block_delta".equals(type)) {
                                var delta = (Map<String, Object>) event.get("delta");
                                if (delta != null) {
                                    String text = (String) delta.get("text");
                                    String thinking = (String) delta.get("thinking");
                                    if (thinking != null) {
                                        reasoningBuf.append(thinking);
                                        onEvent.accept(new ChatStreamEvent("reasoning", thinking, null, null, null));
                                    } else if (text != null) {
                                        answerBuf.append(text);
                                        onEvent.accept(new ChatStreamEvent("answer", text, null, null, null));
                                    }
                                }
                            } else if ("message_stop".equals(type)) {
                                break;
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
            log.error("Anthropic stream error", e);
            onEvent.accept(new ChatStreamEvent("error", e.getMessage(), null, null, null));
        }
    }

    private Map<String, Object> buildRequestBody(String systemPrompt, List<ChatMessage> history, String model) {
        var messages = new ArrayList<Map<String, Object>>();
        for (var msg : history) {
            messages.add(Map.of("role", msg.getRole(), "content", (Object)
                    List.of(Map.of("type", "text", "text", msg.getContent() != null ? msg.getContent() : ""))));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", 4096);
        body.put("messages", messages);
        body.put("stream", true);
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            body.put("system", systemPrompt);
        }
        return body;
    }
}
