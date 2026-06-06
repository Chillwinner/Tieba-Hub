package com.aura.consumer;

import com.aura.entity.UserFollow;
import com.aura.mapper.UserFollowMapper;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 关注 MQ 消费者：监听 follow.exchange，异步写库（create/delete） */
@Component
public class FollowConsumer
{

    @Autowired
    private UserFollowMapper mapper;

    /** 监听 follow.create.queue → 插入关注关系到 PG */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("follow.create.queue"),
            exchange = @Exchange("follow.exchange"),
            key = "follow.create"
    ))
    public void createFollow(UserFollow follow)
    {
        mapper.insert(follow);
    }

    /** 监听 follow.delete.queue → 删除关注关系到 PG */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("follow.delete.queue"),
            exchange = @Exchange("follow.exchange"),
            key = "follow.delete"
    ))
    public void deleteFollow(UserFollow follow)
    {
        mapper.delete(follow.getUserId(), follow.getAuthorId());
    }
}
