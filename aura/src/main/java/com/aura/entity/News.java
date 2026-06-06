package com.aura.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class News
{
    private Long id;
    private String title;
    private String content;
    private Long authorId;
    private Integer likeCount;
    private LocalDateTime createTime;
}
