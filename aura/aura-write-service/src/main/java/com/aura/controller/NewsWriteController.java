package com.aura.controller;

import com.aura.entity.News;
import com.aura.result.Result;
import com.aura.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/** 新闻写操作 */
@RestController
@RequestMapping("/api/write/news")
public class NewsWriteController
{

    @Autowired
    private NewsService newsService;

    /** 发布新闻 */
    @PostMapping
    public Result createNews(@RequestBody News news)
    {
        news = newsService.createNews(news);
        return Result.success(news);
    }

    /** 修改新闻 */
    @PutMapping("/{id}")
    public Result updateNews(@PathVariable Long id, @RequestBody News news)
    {
        news = newsService.updateNews(id, news);
        return Result.success(news);
    }

    /** 删除新闻 */
    @DeleteMapping("/{id}")
    public Result deleteNews(@PathVariable Long id)
    {
        boolean success = newsService.deleteNews(id);
        if (!success)
        {
            return Result.error("Delete failed");
        }
        return Result.success();
    }

    /** 点赞新闻 */
    @PostMapping("/{id}/like")
    public Result likeNews(@PathVariable Long id)
    {
        newsService.likeNews(id);
        return Result.success();
    }
}
