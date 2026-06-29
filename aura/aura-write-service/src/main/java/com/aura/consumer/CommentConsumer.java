package com.aura.consumer;

import com.aura.entity.Comment;
import com.aura.mapper.CommentMapper;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

// 评论 MQ 消费者，异步写库
@Component
public class CommentConsumer
{

    @Autowired
    private CommentMapper mapper;

    // 监听评论创建消息，插入评论
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("comment.create.queue"),
            exchange = @Exchange("comment.exchange"),
            key = "comment.create"
    ))
    public void createComment(Comment comment)
    {
        if (mapper.findById(comment.getId()) == null)
        {
            mapper.insert(comment);
        }
    }

    // 监听评论更新消息，更新评论
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("comment.update.queue"),
            exchange = @Exchange("comment.exchange"),
            key = "comment.update"
    ))
    public void updateComment(Comment comment)
    {
        mapper.update(comment);
    }

    // 监听评论删除消息，删除评论
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("comment.delete.queue"),
            exchange = @Exchange("comment.exchange"),
            key = "comment.delete"
    ))
    public void deleteComment(Long commentId)
    {
        mapper.deleteById(commentId);
    }

    // 监听点赞消息，增量更新点赞数
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("comment.like.queue"),
            exchange = @Exchange("comment.exchange"),
            key = "comment.like"
    ))
    public void likeComment(Map<String, Object> msg)
    {
        Long commentId = ((Number) msg.get("commentId")).longValue();
        int delta = ((Number) msg.get("delta")).intValue();
        mapper.updateLikeCount(commentId, delta);
    }
}
