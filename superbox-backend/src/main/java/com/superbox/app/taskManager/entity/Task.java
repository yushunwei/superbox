package com.superbox.app.taskManager.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Task {
    private Long id;
    private String title;
    private String description;
    private String status;
    private String priority;
    private LocalDate planStartDate;
    private LocalDate planEndDate;
    private String executor;
    private String collaborators;
    private LocalDateTime completedAt;
    private String remarks;
    private Long parentTaskId;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TagRef> tags;
}
