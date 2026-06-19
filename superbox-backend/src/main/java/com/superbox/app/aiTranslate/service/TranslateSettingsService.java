package com.superbox.app.aiTranslate.service;

import com.superbox.app.aiTranslate.entity.UserTranslateSettings;
import com.superbox.app.aiTranslate.mapper.TranslateSettingsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranslateSettingsService {

    private final TranslateSettingsMapper mapper;

    public Map<String, Object> getSettings(Long userId) {
        if (userId == null) return getDefaults();
        UserTranslateSettings s = mapper.findByUserId(userId);
        if (s == null) return getDefaults();
        return toMap(s);
    }

    @Transactional
    public Map<String, Object> saveSettings(Long userId, Map<String, Object> settings) {
        UserTranslateSettings s = new UserTranslateSettings();
        s.setUserId(userId);
        setIfPresent(settings, "defaultModel", v -> s.setDefaultModel((String) v));
        setIfPresent(settings, "defaultSourceLang", v -> s.setDefaultSourceLang((String) v));
        setIfPresent(settings, "defaultTargetLang", v -> s.setDefaultTargetLang((String) v));
        setIfPresent(settings, "temperature", v -> s.setTemperature(toDouble(v)));
        setIfPresent(settings, "maxTokens", v -> s.setMaxTokens(toInt(v)));
        setIfPresent(settings, "pdfFormat", v -> s.setPdfFormat((String) v));
        setIfPresent(settings, "wordFormat", v -> s.setWordFormat((String) v));
        setIfPresent(settings, "doc2xEnabled", v -> s.setDoc2xEnabled((Boolean) v));

        UserTranslateSettings existing = mapper.findByUserId(userId);
        if (existing != null) {
            // merge with existing
            if (s.getDefaultModel() == null) s.setDefaultModel(existing.getDefaultModel());
            if (s.getDefaultSourceLang() == null) s.setDefaultSourceLang(existing.getDefaultSourceLang());
            if (s.getDefaultTargetLang() == null) s.setDefaultTargetLang(existing.getDefaultTargetLang());
            if (s.getTemperature() == null) s.setTemperature(existing.getTemperature());
            if (s.getMaxTokens() == null) s.setMaxTokens(existing.getMaxTokens());
            if (s.getPdfFormat() == null) s.setPdfFormat(existing.getPdfFormat());
            if (s.getWordFormat() == null) s.setWordFormat(existing.getWordFormat());
            if (s.getDoc2xEnabled() == null) s.setDoc2xEnabled(existing.getDoc2xEnabled());
            mapper.update(s);
        } else {
            if (s.getDefaultModel() == null) s.setDefaultModel("gpt-4o-mini");
            if (s.getDefaultSourceLang() == null) s.setDefaultSourceLang("zh");
            if (s.getDefaultTargetLang() == null) s.setDefaultTargetLang("en");
            if (s.getTemperature() == null) s.setTemperature(0.3);
            if (s.getMaxTokens() == null) s.setMaxTokens(4096);
            if (s.getPdfFormat() == null) s.setPdfFormat("overlay");
            if (s.getWordFormat() == null) s.setWordFormat("keep_style");
            if (s.getDoc2xEnabled() == null) s.setDoc2xEnabled(false);
            mapper.insert(s);
        }
        log.info("Saved translate settings for user {}", userId);
        return toMap(mapper.findByUserId(userId));
    }

    private Map<String, Object> getDefaults() {
        return Map.of(
            "defaultModel", "gpt-4o-mini",
            "defaultSourceLang", "zh",
            "defaultTargetLang", "en",
            "temperature", 0.3,
            "maxTokens", 4096,
            "pdfFormat", "overlay",
            "wordFormat", "keep_style",
            "doc2xEnabled", false
        );
    }

    private Map<String, Object> toMap(UserTranslateSettings s) {
        return Map.of(
            "defaultModel", s.getDefaultModel() != null ? s.getDefaultModel() : "gpt-4o-mini",
            "defaultSourceLang", s.getDefaultSourceLang() != null ? s.getDefaultSourceLang() : "zh",
            "defaultTargetLang", s.getDefaultTargetLang() != null ? s.getDefaultTargetLang() : "en",
            "temperature", (Object) (s.getTemperature() != null ? s.getTemperature() : 0.3),
            "maxTokens", (Object) (int) (s.getMaxTokens() != null ? s.getMaxTokens() : 4096),
            "pdfFormat", s.getPdfFormat() != null ? s.getPdfFormat() : "overlay",
            "wordFormat", s.getWordFormat() != null ? s.getWordFormat() : "keep_style",
            "doc2xEnabled", (Object) (s.getDoc2xEnabled() != null ? s.getDoc2xEnabled() : false)
        );
    }

    @FunctionalInterface
    private interface Setter<T> { void set(T value); }

    @SuppressWarnings("unchecked")
    private <T> void setIfPresent(Map<String, Object> map, String key, Setter<T> setter) {
        Object value = map.get(key);
        if (value != null) setter.set((T) value);
    }

    private Double toDouble(Object v) {
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) return Double.parseDouble(s);
        return null;
    }

    private Integer toInt(Object v) {
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) return Integer.parseInt(s);
        return null;
    }
}
