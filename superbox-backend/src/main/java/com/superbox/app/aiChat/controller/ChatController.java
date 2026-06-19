package com.superbox.app.aiChat.controller;

import com.superbox.app.aiChat.entity.ChatMessage;
import com.superbox.app.aiChat.entity.Conversation;
import com.superbox.app.aiChat.service.ChatService;
import com.superbox.common.Result;
import com.superbox.common.UserContext;
import com.superbox.config.RateLimiterService;
import com.superbox.app.modelManager.service.ModelManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai-chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ModelManagerService modelManagerService;
    private final RateLimiterService rateLimiter;

    @GetMapping("/conversations")
    public Result<List<Conversation>> listConversations() {
        return Result.ok(chatService.listConversations());
    }

    @GetMapping("/conversations/{id}")
    public Result<Conversation> getConversation(@PathVariable Long id) {
        return Result.ok(chatService.getConversation(id));
    }

    @PostMapping("/conversations")
    public Result<Conversation> createConversation(@RequestBody Map<String, String> body) {
        return Result.ok(chatService.createConversation(
                body.get("title"), body.get("model")));
    }

    @DeleteMapping("/conversations/{id}")
    public Result<Void> deleteConversation(@PathVariable Long id) {
        chatService.deleteConversation(id);
        return Result.ok();
    }

    @GetMapping("/conversations/{id}/messages")
    public Result<List<ChatMessage>> getMessages(@PathVariable Long id) {
        return Result.ok(chatService.getMessages(id));
    }

    @PostMapping(value = "/conversations/{id}/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessage(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) auth.getPrincipal();
        rateLimiter.checkChatLimit(userId);

        String prompt = (String) body.get("prompt");
        String model = (String) body.get("model");
        boolean reasoningEnabled = Boolean.TRUE.equals(body.get("reasoningEnabled"));
        boolean ragEnabled = Boolean.TRUE.equals(body.get("ragEnabled"));
        return chatService.streamChat(id, prompt, model, reasoningEnabled, ragEnabled);
    }

    @GetMapping("/models")
    public Result<List<Map<String, Object>>> getModels() {
        Long userId = UserContext.getUserId();
        var models = new ArrayList<Map<String, Object>>();
        for (var config : modelManagerService.getUserActiveModels(userId)) {
            var m = new LinkedHashMap<String, Object>();
            m.put("model", config.getModelName());
            m.put("name", config.getDisplayName() != null ? config.getDisplayName() : config.getModelName());
            m.put("provider", config.getProviderName());
            m.put("isDefault", Boolean.TRUE.equals(config.getIsDefault()));
            m.put("available", true);
            models.add(m);
        }
        return Result.ok(models);
    }
}
