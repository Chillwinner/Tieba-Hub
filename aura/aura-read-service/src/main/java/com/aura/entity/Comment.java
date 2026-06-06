package com.aura.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class Comment
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
