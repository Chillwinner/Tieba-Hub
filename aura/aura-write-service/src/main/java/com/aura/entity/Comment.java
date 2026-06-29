package com.aura.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

// 评论实体
@Data
public class Comment implements Serializable
{
    private Long id;
    private Long newsId;
    private Long userId;
    private String content;
    private Long parentId;
    private Integer likeCount;
    private LocalDateTime createTime;
    private String userNickname;
    private List<Comment> replies;
}
