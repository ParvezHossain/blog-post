package com.parvez.blogs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class PostResponse {
    private Long id;
    private String title;
    private String slug;
    private String content;
    private String url;
    private Boolean deleted;
    private String author;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
