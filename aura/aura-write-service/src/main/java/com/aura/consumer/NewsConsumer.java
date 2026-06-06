package com.aura.consumer;

import com.aura.entity.News;
import com.aura.mapper.NewsMapper;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 新闻 MQ 消费者：监听 news.exchange，异步写库（create/update/delete） */
@Component
public class NewsConsumer
{

    @Autowired
    private NewsMapper mapper;

    /** 监听 news.create.queue → 插入新闻到 PG */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("news.create.queue"),
            exchange = @Exchange("news.exchange"),
            key = "news.create"
    ))
    public void createNews(News news)
    {
        mapper.insert(news);
    }

    /** 监听 news.update.queue → 更新新闻到 PG */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("news.update.queue"),
            exchange = @Exchange("news.exchange"),
            key = "news.update"
    ))
    public void updateNews(News news)
    {
        mapper.update(news);
    }

    /** 监听 news.delete.queue → 删除新闻到 PG */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("news.delete.queue"),
            exchange = @Exchange("news.exchange"),
            key = "news.delete"
    ))
    public void deleteNews(Long newsId)
    {
        mapper.deleteById(newsId);
    }
}
