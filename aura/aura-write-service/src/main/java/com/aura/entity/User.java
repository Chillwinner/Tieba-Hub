package com.aura.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

// 用户实体
@Data
public class User implements Serializable
{
    private Long id;
    private String phone;
    private String password;
    private String nickname;
    private String email;
    private LocalDateTime createTime;
}
