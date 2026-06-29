package com.aura.service;

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

// 关注业务：关注和取关
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

    // 关注作者，Redis ZSet 双写 + 异步落库
    public void follow(Long authorId)
    {
        Long userId = UserContext.getUserId();
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

        Long id = redis.opsForValue().increment("follow:id:gen");

        UserFollow uf = new UserFollow();
        uf.setId(id);
        uf.setUserId(userId);
        uf.setAuthorId(authorId);
        uf.setCreateTime(LocalDateTime.now());
        mq.convertAndSend("follow.exchange", "follow.create", uf);
    }

    // 取消关注，Redis ZSet 双删 + 异步落库
    public void unfollow(Long authorId)
    {
        Long userId = UserContext.getUserId();

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
}
