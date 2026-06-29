package com.aura.consumer;

import com.aura.entity.User;
import com.aura.mapper.UserMapper;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// 用户 MQ 消费者，异步写库
@Component
public class UserConsumer
{

    @Autowired
    private UserMapper mapper;

    // 监听用户创建消息，插入新用户
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("user.create.queue"),
            exchange = @Exchange("user.exchange"),
            key = "user.create"
    ))
    public void createUser(User user)
    {
        if (mapper.findById(user.getId()) == null)
        {
            mapper.insert(user);
        }
    }

    // 监听用户更新消息，更新用户资料
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
