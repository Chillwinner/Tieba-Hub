package com.aura.controller;

import com.aura.dto.FollowCountsDTO;
import com.aura.result.Result;
import com.aura.service.FollowReadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/read/follow")
public class FollowReadController
{

    @Autowired
    private FollowReadService followReadService;

    // 获取当前用户的关注数和粉丝数
    @GetMapping("/counts")
    public Result getCounts()
    {
        Long followers = followReadService.getFollowerCount();
        Long following = followReadService.getFollowingCount();
        return Result.success(new FollowCountsDTO(followers, following));
    }

    // 获取当前用户关注的人列表
    @GetMapping("/following")
    public Result getFollowing()
    {
        return Result.success(followReadService.getFollowing());
    }

    // 获取指定作者的粉丝列表
    @GetMapping("/{authorId}/followers")
    public Result getFollowers(@PathVariable Long authorId)
    {
        return Result.success(followReadService.getFollowers(authorId));
    }

    // 检查当前用户是否关注了指定作者
    @GetMapping("/check/{authorId}")
    public Result isFollowing(@PathVariable Long authorId)
    {
        return Result.success(followReadService.isFollowing(authorId));
    }
}
