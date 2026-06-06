package com.aura.service;

import com.alibaba.fastjson2.JSON;
import com.aura.entity.Comment;
import com.aura.entity.News;
import com.aura.entity.User;
import com.aura.entity.UserFollow;
import com.aura.mapper.CommentMapper;
import com.aura.mapper.NewsMapper;
import com.aura.mapper.UserFollowMapper;
import com.aura.mapper.UserMapper;
import com.aura.utils.UserContext;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** 读服务：收口所有只读业务，核心亮点为 Caffeine 三级缓存 + 评论树组装 + 个性化 Feed 流 */
@Service
public class ReadService
{

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private NewsMapper newsMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private UserFollowMapper followMapper;

    /** 新闻一级本地缓存：W-TinyLFU 淘汰，写后 5 秒物理过期，抗热点单 Key 倾斜 */
    private final Cache<Long, News> localNewsCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(5000)
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .build();

    // ==================== 用户读 ====================

    /** 按 id 查用户，Redis 优先，miss 走 PG */
    public User getUserById(Long id)
    {
        String json = redis.opsForValue().get("user:" + id);
        if (json != null)
        {
            return JSON.parseObject(json, User.class);
        }
        return userMapper.findById(id);
    }

    // ==================== 新闻读 ====================

    /** 三级缓存点查：Caffeine(5s) → Redis → PG，命中后逐级回填 */
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

    /** 个性化信息流：读用户专属 Redis List 收件箱，miss 走 PG 兜底分页 */
    public List<News> getNewsFeed(int page, int size)
    {
        Long currentUserId = UserContext.getCurrentId();
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

    /** 查某作者全部新闻，Redis ZSet 优先，miss 走 PG */
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

    // ==================== 评论读 ====================

    /** 查某新闻的评论树：Redis ZSet 拿 id 列表 → 批量点查 Redis JSON → 内存组装嵌套回复树 */
    public List<Comment> getCommentsByNews(Long newsId)
    {
        Set<String> ids = redis.opsForZSet().reverseRange("comment:news:" + newsId, 0, -1);
        if (ids == null || ids.isEmpty())
        {
            return commentMapper.findByNewsId(newsId);
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
            return commentMapper.findByNewsId(newsId);
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

    // ==================== 关注读 ====================

    /** 查当前用户是否关注了该作者，Redis ZSet score 判定 */
    public boolean isFollowing(Long authorId)
    {
        Long userId = UserContext.getCurrentId();
        String key = "follow:user:" + userId;
        return redis.opsForZSet().score(key, String.valueOf(authorId)) != null;
    }

    /** 查当前用户关注的人列表，Redis ZSet 优先，miss 走 PG */
    public List<User> getFollowing()
    {
        Long userId = UserContext.getCurrentId();
        Set<String> ids = redis.opsForZSet().reverseRange("follow:user:" + userId, 0, -1);

        if (ids != null && !ids.isEmpty())
        {
            List<User> users = new ArrayList<>();
            for (String id : ids)
            {
                String json = redis.opsForValue().get("user:" + id);
                if (json != null)
                {
                    users.add(JSON.parseObject(json, User.class));
                }
            }
            if (!users.isEmpty())
            {
                return users;
            }
        }

        List<UserFollow> follows = followMapper.findByUserId(userId);
        List<User> users = new ArrayList<>();
        for (UserFollow f : follows)
        {
            User u = userMapper.findById(f.getAuthorId());
            if (u != null)
            {
                users.add(u);
            }
        }
        return users;
    }

    /** 查某作者的粉丝列表，Redis ZSet 优先，miss 走 PG */
    public List<User> getFollowers(Long authorId)
    {
        Set<String> ids = redis.opsForZSet().reverseRange("follow:author:" + authorId, 0, -1);

        if (ids != null && !ids.isEmpty())
        {
            List<User> users = new ArrayList<>();
            for (String id : ids)
            {
                String json = redis.opsForValue().get("user:" + id);
                if (json != null)
                {
                    users.add(JSON.parseObject(json, User.class));
                }
            }
            if (!users.isEmpty())
            {
                return users;
            }
        }

        List<UserFollow> follows = followMapper.findByAuthorId(authorId);
        List<User> users = new ArrayList<>();
        for (UserFollow f : follows)
        {
            User u = userMapper.findById(f.getUserId());
            if (u != null)
            {
                users.add(u);
            }
        }
        return users;
    }

    /** 查当前用户粉丝数，Redis zCard 优先，miss 走 PG */
    public long getFollowerCount()
    {
        Long userId = UserContext.getCurrentId();
        Long count = redis.opsForZSet().zCard("follow:author:" + userId);
        if (count != null && count > 0)
        {
            return count;
        }
        return followMapper.findByAuthorId(userId).size();
    }

    /** 查当前用户关注数，Redis zCard 优先，miss 走 PG */
    public long getFollowingCount()
    {
        Long userId = UserContext.getCurrentId();
        Long count = redis.opsForZSet().zCard("follow:user:" + userId);
        if (count != null && count > 0)
        {
            return count;
        }
        return followMapper.findByUserId(userId).size();
    }
}
