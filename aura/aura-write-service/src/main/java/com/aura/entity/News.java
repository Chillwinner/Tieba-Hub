package com.aura.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

// 新闻实体
@Data
public class News implements Serializable
{
    private Long id;
    private String title;
    private String content;
    private Long authorId;
    private Integer likeCount;
    private LocalDateTime createTime;
}
