package com.aura.controller;

import com.aura.entity.News;
import com.aura.result.Result;
import com.aura.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/** 新闻模块：发帖、查帖、信息流、编辑、点赞、删除 */
@RestController
@RequestMapping("/api/news")
public class NewsController
{

    @Autowired
    private NewsService newsService;

    /** 发帖，异步写库+扇出粉丝收件箱 */
    @PostMapping
    public Result createNews(@RequestBody News news)
    {
        news = newsService.createNews(news);
        return Result.success(news);
    }

    /** 按 id 查单条新闻，Caffeine → Redis → PG 三级缓存 */
    @GetMapping("/{id}")
    public Result getNews(@PathVariable Long id)
    {
        News news = newsService.getNewsById(id);
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
        return Result.success(newsService.getNewsFeed(page, size));
    }

    /** 查某作者发布的全部新闻 */
    @GetMapping("/author/{authorId}")
    public Result getByAuthor(@PathVariable Long authorId)
    {
        return Result.success(newsService.getNewsByAuthor(authorId));
    }

    /** 编辑新闻标题/内容，仅作者可操作 */
    @PutMapping("/{id}")
    public Result updateNews(@PathVariable Long id,
                              @RequestBody News news)
    {
        news = newsService.updateNews(id, news);
        return Result.success(news);
    }

    /** 点赞，内存蓄水池定量异步刷盘 */
    @PostMapping("/{id}/like")
    public Result likeNews(@PathVariable Long id)
    {
        newsService.likeNews(id);
        return Result.success();
    }

    /** 删除新闻，仅作者可操作，同时清缓存 */
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
}
