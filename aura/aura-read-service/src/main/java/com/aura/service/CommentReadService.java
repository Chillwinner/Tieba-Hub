package com.aura.service;

import com.alibaba.fastjson2.JSON;
import com.aura.entity.Comment;
import com.aura.mapper.CommentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentReadService
{

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private CommentMapper commentMapper;

    // 按新闻 ID 查询评论列表，优先从 Redis 缓存读取
    public List<Comment> getCommentsByNews(Long newsId)
    {
        String json = redis.opsForValue().get("comment:news:" + newsId);
        if (json != null)
        {
            return JSON.parseArray(json, Comment.class);
        }
        return commentMapper.findByNewsId(newsId);
    }
}
