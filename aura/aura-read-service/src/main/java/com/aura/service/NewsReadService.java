package com.aura.service;

import com.alibaba.fastjson2.JSON;
import com.aura.entity.News;
import com.aura.mapper.NewsMapper;
import com.aura.utils.UserContext;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class NewsReadService
{

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private NewsMapper newsMapper;

    // 本地 Caffeine 缓存，容量 5000，写入后 5 秒过期
    private final Cache<Long, News> localNewsCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(5000)
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .build();

    // 按 ID 查询新闻，三级缓存：本地缓存 -> Redis -> 数据库
    public News getNewsById(Long id)
    {
        News cached = localNewsCache.getIfPresent(id);
        if (cached != null)
        {
            return cached;
        }

        String json = redis.opsForValue().get("news:" + id);
        if (json != null)
        {
            News news = JSON.parseObject(json, News.class);
            localNewsCache.put(id, news);
            return news;
        }

        News dbNews = newsMapper.findById(id);
        if (dbNews != null)
        {
            redis.opsForValue().set("news:" + id, JSON.toJSONString(dbNews));
            localNewsCache.put(id, dbNews);
        }
        return dbNews;
    }

    // 分页查询全部新闻，优先从 Redis 有序集合读取
    public List<News> getAllNews(int page, int size)
    {
        long offset = (long) (page - 1) * size;
        Set<String> ids = redis.opsForZSet().reverseRange("news:all", offset, offset + size - 1);

        if (ids != null && !ids.isEmpty())
        {
            List<News> list = new ArrayList<>();
            for (String id : ids)
            {
                list.add(getNewsById(Long.parseLong(id)));
            }
            return list;
        }
        return newsMapper.findPage((page - 1) * size, size);
    }

    // 获取当前用户的信息流，优先从 Redis 列表读取
    public List<News> getNewsFeed(int page, int size)
    {
        Long currentUserId = UserContext.getUserId();
        long offset = (long) (page - 1) * size;
        String key = "user:feed:" + currentUserId;
        List<String> ids = redis.opsForList().range(key, offset, offset + size - 1);

        if (ids != null && !ids.isEmpty())
        {
            List<News> list = new ArrayList<>();
            for (String id : ids)
            {
                list.add(getNewsById(Long.parseLong(id)));
            }
            return list;
        }
        return newsMapper.findPage((page - 1) * size, size);
    }

    // 按作者查询新闻，优先从 Redis 有序集合读取
    public List<News> getNewsByAuthor(Long authorId)
    {
        Set<String> ids = redis.opsForZSet().reverseRange("news:author:" + authorId, 0, -1);

        if (ids != null && !ids.isEmpty())
        {
            List<News> list = new ArrayList<>();
            for (String id : ids)
            {
                list.add(getNewsById(Long.parseLong(id)));
            }
            return list;
        }
        return newsMapper.findByAuthorId(authorId);
    }
}
