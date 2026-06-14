package com.superbox.app.aiChat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatStreamEvent {
    private String type;
    private String delta;
    private List<SourceRef> sources;
    private Long messageId;
    private Integer tokenCount;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SourceRef {
        private Long entryId;
        private String title;
        private Float score;
    }
}
