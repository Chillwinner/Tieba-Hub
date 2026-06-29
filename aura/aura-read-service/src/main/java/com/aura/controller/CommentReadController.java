package com.aura.controller;

import com.aura.result.Result;
import com.aura.service.CommentReadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/read/comment")
public class CommentReadController
{

    @Autowired
    private CommentReadService commentReadService;

    // 按新闻 ID 查询评论列表
    @GetMapping("/news/{newsId}")
    public Result getComments(@PathVariable Long newsId)
    {
        return Result.success(commentReadService.getCommentsByNews(newsId));
    }
}
