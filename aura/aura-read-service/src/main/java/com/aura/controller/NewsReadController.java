package com.aura.controller;

import com.aura.entity.News;
import com.aura.result.Result;
import com.aura.service.NewsReadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/read/news")
public class NewsReadController
{

    @Autowired
    private NewsReadService newsReadService;

    // 按 ID 查询新闻
    @GetMapping("/{id}")
    public Result getNews(@PathVariable Long id)
    {
        News news = newsReadService.getNewsById(id);
        if (news == null)
        {
            return Result.error("News not found");
        }
        return Result.success(news);
    }

    // 分页查询全部新闻
    @GetMapping("/all")
    public Result getAllNews(@RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "20") int size)
    {
        return Result.success(newsReadService.getAllNews(page, size));
    }

    // 获取当前用户的信息流
    @GetMapping("/feed")
    public Result getFeed(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int size)
    {
        return Result.success(newsReadService.getNewsFeed(page, size));
    }

    // 按作者查询新闻列表
    @GetMapping("/author/{authorId}")
    public Result getByAuthor(@PathVariable Long authorId)
    {
        return Result.success(newsReadService.getNewsByAuthor(authorId));
    }
}
