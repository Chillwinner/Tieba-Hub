package com.aura.consumer;

import com.aura.entity.News;
import com.aura.mapper.NewsMapper;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

// 新闻 MQ 消费者，异步写库
@Component
public class NewsConsumer
{

    @Autowired
    private NewsMapper mapper;

    // 监听新闻创建消息，插入新闻
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("news.create.queue"),
            exchange = @Exchange("news.exchange"),
            key = "news.create"
    ))
    public void createNews(News news)
    {
        if (mapper.findById(news.getId()) == null)
        {
            mapper.insert(news);
        }
    }

    // 监听新闻更新消息，更新新闻
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("news.update.queue"),
            exchange = @Exchange("news.exchange"),
            key = "news.update"
    ))
    public void updateNews(News news)
    {
        mapper.update(news);
    }

    // 监听新闻删除消息，删除新闻
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("news.delete.queue"),
            exchange = @Exchange("news.exchange"),
            key = "news.delete"
    ))
    public void deleteNews(Long newsId)
    {
        mapper.deleteById(newsId);
    }

    // 监听点赞消息，增量更新点赞数
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("news.like.queue"),
            exchange = @Exchange("news.exchange"),
            key = "news.like"
    ))
    public void likeNews(Map<String, Object> msg)
    {
        Long newsId = ((Number) msg.get("newsId")).longValue();
        int delta = ((Number) msg.get("delta")).intValue();
        mapper.updateLikeCount(newsId, delta);
    }
}
