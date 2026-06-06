package com.aura.controller;

import com.aura.entity.Comment;
import com.aura.result.Result;
import com.aura.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/** 评论模块：发评论、查评论、改评论、点赞、删除 */
@RestController
@RequestMapping("/api/comment")
public class CommentController
{

    @Autowired
    private CommentService commentService;

    /** 发评论，异步写库 */
    @PostMapping
    public Result createComment(@RequestBody Comment comment)
    {
        comment = commentService.createComment(comment);
        return Result.success(comment);
    }

    /** 查某新闻下的评论树（Redis → PG 兜底） */
    @GetMapping("/news/{newsId}")
    public Result getComments(@PathVariable Long newsId)
    {
        return Result.success(commentService.getCommentsByNews(newsId));
    }

    /** 改评论内容，仅评论者本人可操作 */
    @PutMapping("/{id}")
    public Result updateComment(@PathVariable Long id,
                                 @RequestBody Comment comment)
    {
        comment = commentService.updateComment(id, comment);
        return Result.success(comment);
    }

    /** 点赞评论，纯内存自增，每 100 赞异步刷盘 */
    @PostMapping("/{id}/like")
    public Result likeComment(@PathVariable Long id)
    {
        commentService.likeComment(id);
        return Result.success();
    }

    /** 删除评论，仅评论者本人可操作 */
    @DeleteMapping("/{id}")
    public Result deleteComment(@PathVariable Long id)
    {
        boolean success = commentService.deleteComment(id);
        if (!success)
        {
            return Result.error("Delete failed");
        }
        return Result.success();
    }
}
