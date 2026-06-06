package com.aura.service;

import com.alibaba.fastjson2.JSON;
import com.aura.entity.News;
import com.aura.entity.UserFollow;
import com.aura.mapper.NewsMapper;
import com.aura.mapper.UserFollowMapper;
import com.aura.utils.UserContext;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** 新闻写业务：发帖（扇出粉丝收件箱）、改帖、删帖、点赞（内存蓄水池刷盘） */
@Service
public class NewsService
{

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private RabbitTemplate mq;

    @Autowired
    private NewsMapper mapper;

    @Autowired
    private UserFollowMapper followMapper;

    /** 异步广播线程池，发帖时扇出粉丝收件箱用，CallerRunsPolicy 保证不丢 */
    private final ThreadPoolExecutor broadcastExecutor = new ThreadPoolExecutor(
            8, 16, 60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1000),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    /** 新闻点赞本地蓄水池，每 100 赞异步刷盘到 PG */
    private final ConcurrentHashMap<Long, Integer> likeCounterMap = new ConcurrentHashMap<>();

    /** 发帖：INCR 生成 id → 写 Redis + ZSet → 异步扇出粉丝收件箱 → MQ 异步落库 */
    public News createNews(News news)
    {
        Long id = redis.opsForValue().increment("news:id:gen");
        Long authorId = UserContext.getCurrentId();

        news.setId(id);
        news.setAuthorId(authorId);
        news.setLikeCount(0);
        news.setCreateTime(LocalDateTime.now());

        String json = JSON.toJSONString(news);
        redis.opsForValue().set("news:" + id, json);
        redis.opsForZSet().add("news:author:" + authorId, String.valueOf(id), System.currentTimeMillis());
        redis.opsForZSet().add("news:like", String.valueOf(id), 0);

        List<UserFollow> followers = followMapper.findByAuthorId(authorId);
        String newsIdStr = String.valueOf(id);
        for (UserFollow f : followers)
        {
            Long followerId = f.getUserId();
            Runnable task = new Runnable()
            {
                @Override
                public void run()
                {
                    redis.opsForList().leftPush("user:feed:" + followerId, newsIdStr);
                }
            };
            broadcastExecutor.execute(task);
        }

        mq.convertAndSend("news.exchange", "news.create", news);

        return news;
    }

    /** 编辑新闻，校验作者权限，写 Redis + MQ 异步落库 */
    public News updateNews(Long id, News incoming)
    {
        String json = redis.opsForValue().get("news:" + id);
        if (json == null)
        {
            throw new RuntimeException("not found");
        }

        News news = JSON.parseObject(json, News.class);
        if (!news.getAuthorId().equals(UserContext.getCurrentId()))
        {
            throw new RuntimeException("no auth");
        }

        news.setTitle(incoming.getTitle());
        news.setContent(incoming.getContent());
        redis.opsForValue().set("news:" + id, JSON.toJSONString(news));
        mq.convertAndSend("news.exchange", "news.update", news);

        return news;
    }

    /** 点赞：内存蓄水池自增，每 100 赞 DCL + 异步刷盘到 PG，同时更新 Redis 排行榜 */
    public boolean likeNews(Long newsId)
    {
        String json = redis.opsForValue().get("news:" + newsId);
        if (json != null)
        {
            News news = JSON.parseObject(json, News.class);
            news.setLikeCount(news.getLikeCount() + 1);
            redis.opsForValue().set("news:" + newsId, JSON.toJSONString(news));
        }

        redis.opsForZSet().incrementScore("news:like", String.valueOf(newsId), 1);

        int count = likeCounterMap.merge(newsId, 1, Integer::sum);

        if (count >= 100)
        {
            synchronized (likeCounterMap)
            {
                Integer remain = likeCounterMap.get(newsId);
                if (remain != null && remain >= 100)
                {
                    likeCounterMap.put(newsId, 0);
                    Runnable flush = new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            mapper.updateLikeCount(newsId, remain);
                        }
                    };
                    CompletableFuture.runAsync(flush);
                }
            }
        }

        return true;
    }

    /** 删新闻：校验作者权限，清 Redis + ZSet，MQ 异步落库 */
    public boolean deleteNews(Long id)
    {
        String json = redis.opsForValue().get("news:" + id);
        if (json == null)
        {
            return false;
        }

        News news = JSON.parseObject(json, News.class);
        if (!news.getAuthorId().equals(UserContext.getCurrentId()))
        {
            return false;
        }

        redis.delete("news:" + id);
        redis.opsForZSet().remove("news:author:" + news.getAuthorId(), String.valueOf(id));
        redis.opsForZSet().remove("news:like", String.valueOf(id));
        mq.convertAndSend("news.exchange", "news.delete", id);

        return true;
    }
}
