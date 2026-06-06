package com.aura.service;

import com.alibaba.fastjson2.JSON;
import com.aura.entity.Comment;
import com.aura.mapper.CommentMapper;
import com.aura.utils.UserContext;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/** 评论业务：CRUD + 点赞（纯内存蓄水池） + 评论树组装 */
@Service
public class CommentService
{

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private RabbitTemplate mq;

    @Autowired
    private CommentMapper mapper;

    /** 评论点赞本地蓄水池，每 100 赞异步刷盘到 PG */
    private final ConcurrentHashMap<Long, Integer> commentLikeCounterMap = new ConcurrentHashMap<>();

    /** 发评论：INCR 生成 id → 写 Redis + ZSet → MQ 异步落库 */
    public Comment createComment(Comment comment)
    {
        Long id = redis.opsForValue().increment("comment:id:gen");

        comment.setId(id);
        comment.setUserId(UserContext.getCurrentId());
        comment.setLikeCount(0);
        comment.setCreateTime(LocalDateTime.now());

        String json = JSON.toJSONString(comment);
        redis.opsForValue().set("comment:" + id, json);
        redis.opsForZSet().add("comment:news:" + comment.getNewsId(), String.valueOf(id), System.currentTimeMillis());

        mq.convertAndSend("comment.exchange", "comment.create", comment);

        return comment;
    }

    /** 查某新闻的评论树：Redis ZSet 拿 id 列表 → 批量取 JSON → 内存组装嵌套回复树 */
    public List<Comment> getCommentsByNews(Long newsId)
    {
        Set<String> ids = redis.opsForZSet().reverseRange("comment:news:" + newsId, 0, -1);
        if (ids == null || ids.isEmpty())
        {
            return mapper.findByNewsId(newsId);
        }

        List<Comment> all = new ArrayList<>();
        for (String id : ids)
        {
            String json = redis.opsForValue().get("comment:" + id);
            if (json != null)
            {
                all.add(JSON.parseObject(json, Comment.class));
            }
        }
        if (all.isEmpty())
        {
            return mapper.findByNewsId(newsId);
        }

        Map<Long, Comment> map = new HashMap<>();
        List<Comment> top = new ArrayList<>();
        for (Comment c : all)
        {
            map.put(c.getId(), c);
            if (c.getParentId() == 0)
            {
                c.setReplies(new ArrayList<>());
                top.add(c);
            }
        }
        for (Comment c : all)
        {
            if (c.getParentId() != 0)
            {
                Comment parent = map.get(c.getParentId());
                if (parent != null)
                {
                    parent.getReplies().add(c);
                }
            }
        }

        return top;
    }

    /** 改评论内容，校验评论者权限，写 Redis + MQ 异步落库 */
    public Comment updateComment(Long id, Comment incoming)
    {
        String json = redis.opsForValue().get("comment:" + id);
        if (json == null)
        {
            throw new RuntimeException("not found");
        }

        Comment comment = JSON.parseObject(json, Comment.class);
        if (!comment.getUserId().equals(UserContext.getCurrentId()))
        {
            throw new RuntimeException("no auth");
        }

        comment.setContent(incoming.getContent());
        redis.opsForValue().set("comment:" + id, JSON.toJSONString(comment));
        mq.convertAndSend("comment.exchange", "comment.update", comment);

        return comment;
    }

    /** 点赞评论：纯内存 merge 自增，每 100 赞 DCL + 异步刷盘到 PG，零 Redis I/O */
    public boolean likeComment(Long commentId)
    {
        int count = commentLikeCounterMap.merge(commentId, 1, Integer::sum);

        if (count >= 100)
        {
            synchronized (commentLikeCounterMap)
            {
                Integer remain = commentLikeCounterMap.get(commentId);
                if (remain != null && remain >= 100)
                {
                    commentLikeCounterMap.put(commentId, 0);
                    Runnable flush = new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            mapper.updateLikeCount(commentId, remain);
                        }
                    };
                    CompletableFuture.runAsync(flush);
                }
            }
        }

        return true;
    }

    /** 删评论，校验评论者权限，清 Redis + ZSet，MQ 异步落库 */
    public boolean deleteComment(Long id)
    {
        String json = redis.opsForValue().get("comment:" + id);
        if (json == null)
        {
            return false;
        }

        Comment comment = JSON.parseObject(json, Comment.class);
        if (!comment.getUserId().equals(UserContext.getCurrentId()))
        {
            return false;
        }

        redis.delete("comment:" + id);
        redis.opsForZSet().remove("comment:news:" + comment.getNewsId(), String.valueOf(id));
        mq.convertAndSend("comment.exchange", "comment.delete", id);

        return true;
    }
}
