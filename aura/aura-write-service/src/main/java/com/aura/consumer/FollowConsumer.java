package com.aura.consumer;

import com.aura.entity.UserFollow;
import com.aura.mapper.UserFollowMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// 关注 MQ 消费者，异步写库
@Component
public class FollowConsumer
{
    private static final Logger log = LoggerFactory.getLogger(FollowConsumer.class);

    @Autowired
    private UserFollowMapper mapper;

    // 监听关注创建消息，插入关注关系
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("follow.create.queue"),
            exchange = @Exchange("follow.exchange"),
            key = "follow.create"
    ))
    public void createFollow(UserFollow follow)
    {
        if (follow.getId() == null || follow.getUserId() == null || follow.getAuthorId() == null)
        {
            log.error("[MQ] Invalid follow message: id={}, userId={}, authorId={}", follow.getId(), follow.getUserId(), follow.getAuthorId());
            return;
        }
        if (mapper.findByUserAndAuthor(follow.getUserId(), follow.getAuthorId()) == null)
        {
            mapper.insert(follow);
        }
    }

    // 监听关注删除消息，删除关注关系
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
