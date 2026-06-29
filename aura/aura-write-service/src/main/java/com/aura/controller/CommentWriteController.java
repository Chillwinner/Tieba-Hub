package com.aura.controller;

import com.aura.entity.Comment;
import com.aura.result.Result;
import com.aura.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

// 评论写操作接口
@RestController
@RequestMapping("/api/write/comment")
public class CommentWriteController
{

    @Autowired
    private CommentService commentService;

    // 发表评论
    @PostMapping
    public Result createComment(@RequestBody Comment comment)
    {
        comment = commentService.createComment(comment);
        return Result.success(comment);
    }

    // 修改评论
    @PutMapping("/{id}")
    public Result updateComment(@PathVariable Long id, @RequestBody Comment comment)
    {
        comment = commentService.updateComment(id, comment);
        return Result.success(comment);
    }

    // 删除评论
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

    // 点赞评论
    @PostMapping("/{id}/like")
    public Result likeComment(@PathVariable Long id)
    {
        commentService.likeComment(id);
        return Result.success();
    }
}
