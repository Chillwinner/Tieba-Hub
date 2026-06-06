package com.aura.controller;

import com.aura.result.Result;
import com.aura.service.ReadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/** 评论读操作 */
@RestController
@RequestMapping("/api/read/comment")
public class CommentReadController
{

    @Autowired
    private ReadService readService;

    /** 查某新闻下的嵌套评论树 */
    @GetMapping("/news/{newsId}")
    public Result getComments(@PathVariable Long newsId)
    {
        return Result.success(readService.getCommentsByNews(newsId));
    }
}
