package com.aura.service;

import com.alibaba.fastjson2.JSON;
import com.aura.entity.User;
import com.aura.mapper.UserMapper;
import com.aura.utils.JwtUtils;
import com.aura.utils.Sha256Utils;
import com.aura.utils.UserContext;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** 用户写业务：注册（Redis 幂等+MQ 异步写库）、登录（JWT）、改资料 */
@Service
public class UserService
{

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private RabbitTemplate mq;

    @Autowired
    private UserMapper mapper;

    /** 注册：Redis 手机号去重 → INCR 生成 id → 写 Redis + MQ 异步落库 */
    public User register(User user)
    {
        String phone = user.getPhone();
        String phoneKey = "user:phone:" + phone;

        String exists = redis.opsForValue().get(phoneKey);
        if (exists != null)
        {
            throw new RuntimeException("Phone already registered");
        }
        if (exists == null && mapper.findByPhone(phone) != null)
        {
            throw new RuntimeException("Phone already registered");
        }

        Long id = redis.opsForValue().increment("user:id:gen");
        String nickname = user.getNickname();
        if (nickname == null)
        {
            nickname = "User" + phone.substring(phone.length() - 4);
        }

        user.setId(id);
        user.setPassword(Sha256Utils.hash(user.getPassword()));
        user.setNickname(nickname);
        user.setCreateTime(LocalDateTime.now());

        String json = JSON.toJSONString(user);
        redis.opsForValue().set("user:" + id, json);
        redis.opsForValue().set(phoneKey, String.valueOf(id));

        mq.convertAndSend("user.exchange", "user.create", user);

        return user;
    }

    /** 登录：Redis 优先查用户，miss 走 PG；校验密码后签发 JWT */
    public String login(String phone, String password)
    {
        User user = null;
        String phoneKey = "user:phone:" + phone;
        String userIdStr = redis.opsForValue().get(phoneKey);

        if (userIdStr != null)
        {
            String json = redis.opsForValue().get("user:" + userIdStr);
            if (json != null)
            {
                user = JSON.parseObject(json, User.class);
            }
        }

        if (user == null)
        {
            user = mapper.findByPhone(phone);
            if (user == null)
            {
                throw new RuntimeException("User not found");
            }
        }

        if (!Sha256Utils.verify(password, user.getPassword()))
        {
            throw new RuntimeException("Invalid password");
        }

        return JwtUtils.generate(user.getId(), user.getPhone());
    }

    /** 改当前用户的昵称/邮箱，写 Redis + MQ 异步落库 */
    public User updateProfile(User incoming)
    {
        Long id = UserContext.getCurrentId();

        String json = redis.opsForValue().get("user:" + id);
        User user = null;
        if (json != null)
        {
            user = JSON.parseObject(json, User.class);
        }
        else
        {
            user = mapper.findById(id);
        }

        if (user == null)
        {
            throw new RuntimeException("User not found");
        }

        user.setNickname(incoming.getNickname());
        user.setEmail(incoming.getEmail());
        redis.opsForValue().set("user:" + id, JSON.toJSONString(user));
        mq.convertAndSend("user.exchange", "user.update", user);

        return user;
    }
}
