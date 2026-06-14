package com.superbox.app.tagManager.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TagCategory {
    private Long id;
    private String name;
    private String color;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
