package com.superbox.app.aiChat.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatMessage {
    private Long id;
    private Long conversationId;
    private String role;
    private String content;
    private String reasoningContent;
    private String sources;
    private Integer tokenCount;
    private String model;
    private LocalDateTime createdAt;
}
