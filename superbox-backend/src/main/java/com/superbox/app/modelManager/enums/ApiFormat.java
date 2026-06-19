package com.superbox.app.modelManager.enums;

public enum ApiFormat {
    OPENAI_COMPATIBLE,
    ANTHROPIC_COMPATIBLE,
    OLLAMA;

    public static ApiFormat detectFromBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) return OPENAI_COMPATIBLE;
        String lower = baseUrl.toLowerCase();
        if (lower.contains(":11434") || lower.contains("ollama")) return OLLAMA;
        if (lower.contains("/anthropic")) return ANTHROPIC_COMPATIBLE;
        return OPENAI_COMPATIBLE;
    }
}
