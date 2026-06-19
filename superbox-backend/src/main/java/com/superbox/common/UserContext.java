package com.superbox.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class UserContext {

    private UserContext() {}

    public static Long getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Long id) return id;
        if (principal instanceof Integer id) return id.longValue();
        if (principal instanceof String str) {
            try { return Long.parseLong(str); } catch (NumberFormatException e) { /* fall through */ }
        }
        return null;
    }
}
