package com.aura.consumer;

import com.aura.entity.Comment;
import com.aura.mapper.CommentMapper;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 评论 MQ 消费者：监听 comment.exchange，异步写库（create/update/delete） */
@Component
public class CommentConsumer
{

    @Autowired
    private CommentMapper mapper;

    /** 监听 comment.create.queue → 插入评论到 PG */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("comment.create.queue"),
            exchange = @Exchange("comment.exchange"),
            key = "comment.create"
    ))
    public void createComment(Comment comment)
    {
        mapper.insert(comment);
    }

    /** 监听 comment.update.queue → 更新评论到 PG */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("comment.update.queue"),
            exchange = @Exchange("comment.exchange"),
            key = "comment.update"
    ))
    public void updateComment(Comment comment)
    {
        mapper.update(comment);
    }

    /** 监听 comment.delete.queue → 删除评论到 PG */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("comment.delete.queue"),
            exchange = @Exchange("comment.exchange"),
            key = "comment.delete"
    ))
    public void deleteComment(Long commentId)
    {
        mapper.deleteById(commentId);
    }
}
