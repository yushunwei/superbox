package com.superbox.app.knowledgeBase.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingService {

    private static final int CHUNK_SIZE = 500;
    private static final int OVERLAP = 50;

    public List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;

        String cleaned = text.replaceAll("\\s+", " ").trim();
        int start = 0;
        while (start < cleaned.length()) {
            int end = Math.min(start + CHUNK_SIZE, cleaned.length());
            if (end < cleaned.length()) {
                int boundary = findBreakPoint(cleaned, end, Math.max(start + CHUNK_SIZE / 2, end - 100));
                if (boundary > start) end = boundary;
            }
            chunks.add(cleaned.substring(start, end).trim());
            start = end - OVERLAP;
            if (start >= cleaned.length()) break;
        }
        return chunks;
    }

    private int findBreakPoint(String text, int preferred, int min) {
        for (int i = preferred; i >= min; i--) {
            char c = text.charAt(i);
            if (c == '。' || c == '！' || c == '？' || c == '\n' || c == '；') {
                return i + 1;
            }
        }
        return preferred;
    }

    public int estimateTokens(String text) {
        if (text == null) return 0;
        int chineseChars = 0;
        int englishWords = 0;
        boolean inWord = false;
        for (char c : text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
                chineseChars++;
                inWord = false;
            } else if (Character.isWhitespace(c)) {
                if (inWord) englishWords++;
                inWord = false;
            } else {
                inWord = true;
            }
        }
        if (inWord) englishWords++;
        return chineseChars + (int)(englishWords * 1.3);
    }
}
