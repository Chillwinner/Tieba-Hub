package com.aura.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserFollow
{
    private Long id;
    private Long userId;
    private Long authorId;
    private LocalDateTime createTime;
}
