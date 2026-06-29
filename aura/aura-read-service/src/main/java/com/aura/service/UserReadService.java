package com.aura.service;

import com.alibaba.fastjson2.JSON;
import com.aura.entity.User;
import com.aura.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserReadService
{

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private UserMapper userMapper;

    // 按 ID 查询用户，优先从 Redis 缓存读取
    public User getUserById(Long id)
    {
        String json = redis.opsForValue().get("user:" + id);
        if (json != null)
        {
            return JSON.parseObject(json, User.class);
        }
        return userMapper.findById(id);
    }
}
