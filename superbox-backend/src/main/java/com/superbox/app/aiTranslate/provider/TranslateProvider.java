package com.superbox.app.aiTranslate.provider;

public interface TranslateProvider {

    /** Translate text synchronously. Returns translated text. */
    String translate(String systemPrompt, String text, String sourceLang, String targetLang, String model);

    /** Translate with optional API key and base URL override (from user model config). */
    default String translate(String systemPrompt, String text, String sourceLang, String targetLang,
                             String model, String apiKeyOverride, String baseUrlOverride) {
        return translate(systemPrompt, text, sourceLang, targetLang, model);
    }

    /** Get the provider name identifier. */
    String getProviderName();
}
