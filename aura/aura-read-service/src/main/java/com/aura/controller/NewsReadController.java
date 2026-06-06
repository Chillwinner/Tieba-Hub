package com.aura.controller;

import com.aura.entity.News;
import com.aura.result.Result;
import com.aura.service.ReadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 新闻读操作 */
@RestController
@RequestMapping("/api/read/news")
public class NewsReadController
{

    @Autowired
    private ReadService readService;

    /** 新闻详情点查：Caffeine → Redis → PG 三级缓存 */
    @GetMapping("/{id}")
    public Result getNews(@PathVariable Long id)
    {
        News news = readService.getNewsById(id);
        if (news == null)
        {
            return Result.error("News not found");
        }
        return Result.success(news);
    }

    /** 当前用户的个性化信息流（分页） */
    @GetMapping("/feed")
    public Result getFeed(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "20") int size)
    {
        return Result.success(readService.getNewsFeed(page, size));
    }

    /** 查某作者发布的全部新闻 */
    @GetMapping("/author/{authorId}")
    public Result getByAuthor(@PathVariable Long authorId)
    {
        return Result.success(readService.getNewsByAuthor(authorId));
    }
}
