package com.aura.consumer;

import com.aura.entity.User;
import com.aura.mapper.UserMapper;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 用户 MQ 消费者：监听 user.exchange，异步写库 */
@Component
public class UserConsumer
{

    @Autowired
    private UserMapper mapper;

    /** 监听 user.create.queue → 插入新用户到 PG */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("user.create.queue"),
            exchange = @Exchange("user.exchange"),
            key = "user.create"
    ))
    public void createUser(User user)
    {
        mapper.insert(user);
    }

    /** 监听 user.update.queue → 更新用户资料到 PG */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("user.update.queue"),
            exchange = @Exchange("user.exchange"),
            key = "user.update"
    ))
    public void updateUser(User user)
    {
        mapper.update(user);
    }
}
