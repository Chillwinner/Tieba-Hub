package com.aura.controller;

import com.aura.result.Result;
import com.aura.service.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

// 关注写操作接口
@RestController
@RequestMapping("/api/write/follow")
public class FollowWriteController
{

    @Autowired
    private FollowService followService;

    // 关注作者
    @PostMapping("/{authorId}")
    public Result follow(@PathVariable Long authorId)
    {
        followService.follow(authorId);
        return Result.success();
    }

    // 取消关注
    @PostMapping("/unfollow/{authorId}")
    public Result unfollow(@PathVariable Long authorId)
    {
        followService.unfollow(authorId);
        return Result.success();
    }
}
