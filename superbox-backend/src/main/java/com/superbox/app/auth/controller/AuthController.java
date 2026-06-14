package com.superbox.app.auth.controller;

import com.superbox.app.auth.entity.User;
import com.superbox.app.auth.service.AuthService;
import com.superbox.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String token = authService.login(username, password);
        return Result.ok(Map.of("token", token));
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        authService.register(username, password);
        String token = authService.login(username, password);
        return Result.ok(Map.of("token", token));
    }

    @PostMapping("/wx-login")
    public Result<Map<String, Object>> wxLogin(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        String token = authService.wxLogin(code);
        return Result.ok(Map.of("token", token));
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) auth.getPrincipal();
        User user = authService.getCurrentUser(userId);
        return Result.ok(Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "displayName", user.getDisplayName() != null ? user.getDisplayName() : user.getUsername(),
            "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
            "preferredLanguage", user.getPreferredLanguage() != null ? user.getPreferredLanguage() : "zh-CN"
        ));
    }
}
