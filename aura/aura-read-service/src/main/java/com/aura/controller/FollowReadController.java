package com.aura.controller;

import com.aura.entity.FollowCounts;
import com.aura.result.Result;
import com.aura.service.ReadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/** 关注读操作 */
@RestController
@RequestMapping("/api/read/follow")
public class FollowReadController
{

    @Autowired
    private ReadService readService;

    /** 查当前用户的粉丝数和关注数 */
    @GetMapping("/counts")
    public Result getCounts()
    {
        Long followers = readService.getFollowerCount();
        Long following = readService.getFollowingCount();
        return Result.success(new FollowCounts(followers, following));
    }

    /** 查当前用户关注的人列表 */
    @GetMapping("/following")
    public Result getFollowing()
    {
        return Result.success(readService.getFollowing());
    }

    /** 查某作者的粉丝列表 */
    @GetMapping("/{authorId}/followers")
    public Result getFollowers(@PathVariable Long authorId)
    {
        return Result.success(readService.getFollowers(authorId));
    }

    /** 检查当前用户是否关注了该作者 */
    @GetMapping("/check/{authorId}")
    public Result isFollowing(@PathVariable Long authorId)
    {
        return Result.success(readService.isFollowing(authorId));
    }
}
