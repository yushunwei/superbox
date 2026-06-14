package com.superbox.app.auth.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String passwordHash;
    private String displayName;
    private String avatarUrl;
    private String wxOpenid;
    private String preferredLanguage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
