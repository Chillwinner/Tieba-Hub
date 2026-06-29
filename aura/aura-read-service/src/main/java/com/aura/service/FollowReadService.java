package com.aura.service;

import com.alibaba.fastjson2.JSON;
import com.aura.entity.User;
import com.aura.entity.UserFollow;
import com.aura.mapper.UserFollowMapper;
import com.aura.mapper.UserMapper;
import com.aura.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class FollowReadService
{

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserFollowMapper followMapper;

    // 检查当前用户是否关注了指定作者
    public boolean isFollowing(Long authorId)
    {
        Long userId = UserContext.getUserId();
        String key = "follow:user:" + userId;
        return redis.opsForZSet().score(key, String.valueOf(authorId)) != null;
    }

    // 获取当前用户关注的人列表，优先从 Redis 读取
    public List<User> getFollowing()
    {
        Long userId = UserContext.getUserId();
        Set<String> ids = redis.opsForZSet().reverseRange("follow:user:" + userId, 0, -1);

        if (ids != null && !ids.isEmpty())
        {
            List<User> users = new ArrayList<>();
            for (String id : ids)
            {
                String json = redis.opsForValue().get("user:" + id);
                if (json != null)
                {
                    users.add(JSON.parseObject(json, User.class));
                }
            }
            if (!users.isEmpty())
            {
                return users;
            }
        }

        List<UserFollow> follows = followMapper.findByUserId(userId);
        List<User> users = new ArrayList<>();
        for (UserFollow f : follows)
        {
            User u = userMapper.findById(f.getAuthorId());
            if (u != null)
            {
                users.add(u);
            }
        }
        return users;
    }

    // 获取指定作者的粉丝列表，优先从 Redis 读取
    public List<User> getFollowers(Long authorId)
    {
        Set<String> ids = redis.opsForZSet().reverseRange("follow:author:" + authorId, 0, -1);

        if (ids != null && !ids.isEmpty())
        {
            List<User> users = new ArrayList<>();
            for (String id : ids)
            {
                String json = redis.opsForValue().get("user:" + id);
                if (json != null)
                {
                    users.add(JSON.parseObject(json, User.class));
                }
            }
            if (!users.isEmpty())
            {
                return users;
            }
        }

        List<UserFollow> follows = followMapper.findByAuthorId(authorId);
        List<User> users = new ArrayList<>();
        for (UserFollow f : follows)
        {
            User u = userMapper.findById(f.getUserId());
            if (u != null)
            {
                users.add(u);
            }
        }
        return users;
    }

    // 获取当前用户的粉丝数，优先从 Redis 读取
    public long getFollowerCount()
    {
        Long userId = UserContext.getUserId();
        Long count = redis.opsForZSet().zCard("follow:author:" + userId);
        if (count != null && count > 0)
        {
            return count;
        }
        return followMapper.findByAuthorId(userId).size();
    }

    // 获取当前用户的关注数，优先从 Redis 读取
    public long getFollowingCount()
    {
        Long userId = UserContext.getUserId();
        Long count = redis.opsForZSet().zCard("follow:user:" + userId);
        if (count != null && count > 0)
        {
            return count;
        }
        return followMapper.findByUserId(userId).size();
    }
}
