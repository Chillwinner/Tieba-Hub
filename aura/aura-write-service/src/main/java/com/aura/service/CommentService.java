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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

// 评论业务：发评论、改评论、删评论、点赞
@Service
public class CommentService
{

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private RabbitTemplate mq;

    @Autowired
    private CommentMapper mapper;

    // 评论点赞计数蓄水池，每 100 赞异步刷盘
    private final ConcurrentHashMap<Long, Integer> commentLikeCounterMap = new ConcurrentHashMap<>();

    private final ReentrantLock commentLikeLock = new ReentrantLock();

    // 发表评论，先写 Redis 再 MQ 写 DB
    public Comment createComment(Comment comment)
    {
        Long id = redis.opsForValue().increment("comment:id:gen");

        comment.setId(id);
        comment.setUserId(UserContext.getUserId());
        comment.setLikeCount(0);
        comment.setCreateTime(LocalDateTime.now());

        addToCommentTree(comment);

        mq.convertAndSend("comment.exchange", "comment.create", comment);

        return comment;
    }

    // 修改评论内容，先写 Redis 再 MQ 写 DB
    public Comment updateComment(Long id, Comment incoming)
    {
        Comment comment = mapper.findById(id);
        if (comment == null)
        {
            throw new RuntimeException("not found");
        }
        if (!comment.getUserId().equals(UserContext.getUserId()))
        {
            throw new RuntimeException("no auth");
        }

        comment.setContent(incoming.getContent());

        updateInCommentTree(comment);

        mq.convertAndSend("comment.exchange", "comment.update", comment);

        return comment;
    }

    // 点赞评论，Redis 实时计数，蓄水池每 100 赞异步刷盘
    public boolean likeComment(Long commentId)
    {
        redis.opsForValue().increment("comment:like:" + commentId);

        int count = commentLikeCounterMap.merge(commentId, 1, Integer::sum);

        if (count >= 100)
        {
            commentLikeLock.lock();
            try
            {
                Integer remain = commentLikeCounterMap.get(commentId);
                if (remain != null && remain >= 100)
                {
                    commentLikeCounterMap.put(commentId, 0);
                    Map<String, Object> msg = new HashMap<>();
                    msg.put("commentId", commentId);
                    msg.put("delta", remain);
                    mq.convertAndSend("comment.exchange", "comment.like", msg);
                }
            }
            finally
            {
                commentLikeLock.unlock();
            }
        }

        return true;
    }

    // 删除评论，先删 Redis 再 MQ 删 DB
    public boolean deleteComment(Long id)
    {
        Comment comment = mapper.findById(id);
        if (comment == null)
        {
            return false;
        }
        if (!comment.getUserId().equals(UserContext.getUserId()))
        {
            return false;
        }

        removeFromCommentTree(comment.getNewsId(), id);

        mq.convertAndSend("comment.exchange", "comment.delete", id);

        return true;
    }

    // ========== Redis 评论树操作 ==========

    // 添加评论到 Redis 评论树
    private void addToCommentTree(Comment comment)
    {
        String json = redis.opsForValue().get("comment:news:" + comment.getNewsId());
        List<Comment> tree;
        if (json == null || json.isEmpty())
        {
            tree = new ArrayList<>();
        }
        else
        {
            tree = JSON.parseArray(json, Comment.class);
        }

        if (comment.getParentId() == null || comment.getParentId() == 0)
        {
            comment.setReplies(new ArrayList<>());
            tree.add(comment);
        }
        else
        {
            addReply(tree, comment);
        }

        redis.opsForValue().set("comment:news:" + comment.getNewsId(), JSON.toJSONString(tree));
    }

    // 递归找到父评论并添加回复
    private void addReply(List<Comment> comments, Comment reply)
    {
        for (Comment c : comments)
        {
            if (c.getId().equals(reply.getParentId()))
            {
                if (c.getReplies() == null)
                {
                    c.setReplies(new ArrayList<>());
                }
                c.getReplies().add(reply);
                return;
            }
            if (c.getReplies() != null && !c.getReplies().isEmpty())
            {
                addReply(c.getReplies(), reply);
            }
        }
        // 父评论不在树里，降级为一级评论
        reply.setReplies(new ArrayList<>());
        comments.add(reply);
    }

    // 更新 Redis 评论树中的内容
    private void updateInCommentTree(Comment comment)
    {
        String json = redis.opsForValue().get("comment:news:" + comment.getNewsId());
        if (json == null || json.isEmpty())
        {
            return;
        }

        List<Comment> tree = JSON.parseArray(json, Comment.class);
        updateCommentInTree(tree, comment);
        redis.opsForValue().set("comment:news:" + comment.getNewsId(), JSON.toJSONString(tree));
    }

    private void updateCommentInTree(List<Comment> comments, Comment updated)
    {
        for (Comment c : comments)
        {
            if (c.getId().equals(updated.getId()))
            {
                c.setContent(updated.getContent());
                return;
            }
            if (c.getReplies() != null && !c.getReplies().isEmpty())
            {
                updateCommentInTree(c.getReplies(), updated);
            }
        }
    }

    // 从 Redis 评论树中删除评论
    private void removeFromCommentTree(Long newsId, Long commentId)
    {
        String json = redis.opsForValue().get("comment:news:" + newsId);
        if (json == null || json.isEmpty())
        {
            return;
        }

        List<Comment> tree = JSON.parseArray(json, Comment.class);
        removeCommentFromTree(tree, commentId);
        redis.opsForValue().set("comment:news:" + newsId, JSON.toJSONString(tree));
    }

    private void removeCommentFromTree(List<Comment> comments, Long commentId)
    {
        Iterator<Comment> it = comments.iterator();
        while (it.hasNext())
        {
            Comment c = it.next();
            if (c.getId().equals(commentId))
            {
                it.remove();
                return;
            }
            if (c.getReplies() != null && !c.getReplies().isEmpty())
            {
                removeCommentFromTree(c.getReplies(), commentId);
            }
        }
    }
}
