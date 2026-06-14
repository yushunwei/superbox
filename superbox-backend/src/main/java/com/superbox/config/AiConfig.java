package com.superbox.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "superbox.ai")
public class AiConfig {
    private String defaultProvider = "openai";
    private Map<String, ProviderConfig> providers;

    @Data
    public static class ProviderConfig {
        private String apiKey;
        private String baseUrl;
        private String chatModel;
        private String reasonerModel;
        private String embeddingModel;
    }
}
