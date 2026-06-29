package com.aura.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

// 用户关注关系实体
@Data
public class UserFollow implements Serializable
{
    private Long id;
    private Long userId;
    private Long authorId;
    private LocalDateTime createTime;
}
