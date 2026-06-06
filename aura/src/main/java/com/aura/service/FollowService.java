package com.aura.service;

import com.alibaba.fastjson2.JSON;
import com.aura.entity.User;
import com.aura.entity.UserFollow;
import com.aura.mapper.UserFollowMapper;
import com.aura.mapper.UserMapper;
import com.aura.utils.UserContext;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** 关注业务：关注/取关（Redis ZSet + MQ 异步落库）、查关注状态、列表、粉丝数 */
@Service
public class FollowService
{

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private RabbitTemplate mq;

    @Autowired
    private UserFollowMapper followMapper;

    @Autowired
    private UserMapper userMapper;

    /** 关注：Redis ZSet 双写 + MQ 异步落库，防重复关注 */
    public void follow(Long authorId)
    {
        Long userId = UserContext.getCurrentId();

        if (userId.equals(authorId))
        {
            throw new RuntimeException("Cannot follow yourself");
        }

        String key = "follow:user:" + userId;
        Double score = redis.opsForZSet().score(key, String.valueOf(authorId));
        if (score != null)
        {
            throw new RuntimeException("Already following");
        }

        User target = userMapper.findById(authorId);
        if (target == null)
        {
            throw new RuntimeException("User not found");
        }

        double now = System.currentTimeMillis();
        redis.opsForZSet().add(key, String.valueOf(authorId), now);
        redis.opsForZSet().add("follow:author:" + authorId, String.valueOf(userId), now);

        UserFollow uf = new UserFollow();
        uf.setUserId(userId);
        uf.setAuthorId(authorId);
        uf.setCreateTime(LocalDateTime.now());
        mq.convertAndSend("follow.exchange", "follow.create", uf);
    }

    /** 取关：Redis ZSet 双删 + MQ 异步落库 */
    public void unfollow(Long authorId)
    {
        Long userId = UserContext.getCurrentId();

        String key = "follow:user:" + userId;
        Double score = redis.opsForZSet().score(key, String.valueOf(authorId));
        if (score == null)
        {
            throw new RuntimeException("Not following");
        }

        redis.opsForZSet().remove(key, String.valueOf(authorId));
        redis.opsForZSet().remove("follow:author:" + authorId, String.valueOf(userId));

        UserFollow uf = new UserFollow();
        uf.setUserId(userId);
        uf.setAuthorId(authorId);
        mq.convertAndSend("follow.exchange", "follow.delete", uf);
    }

    /** 查当前用户是否关注了该作者，Redis ZSet score 判定 */
    public boolean isFollowing(Long authorId)
    {
        Long userId = UserContext.getCurrentId();
        String key = "follow:user:" + userId;
        return redis.opsForZSet().score(key, String.valueOf(authorId)) != null;
    }

    /** 查当前用户关注的人列表，Redis ZSet 优先，miss 走 PG */
    public List<User> getFollowing()
    {
        Long userId = UserContext.getCurrentId();
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

    /** 查某作者的粉丝列表，Redis ZSet 优先，miss 走 PG */
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

    /** 查当前用户粉丝数，Redis zCard 优先，miss 走 PG */
    public long getFollowerCount()
    {
        Long userId = UserContext.getCurrentId();
        Long count = redis.opsForZSet().zCard("follow:author:" + userId);
        if (count != null && count > 0)
        {
            return count;
        }
        return followMapper.findByAuthorId(userId).size();
    }

    /** 查当前用户关注数，Redis zCard 优先，miss 走 PG */
    public long getFollowingCount()
    {
        Long userId = UserContext.getCurrentId();
        Long count = redis.opsForZSet().zCard("follow:user:" + userId);
        if (count != null && count > 0)
        {
            return count;
        }
        return followMapper.findByUserId(userId).size();
    }
}
