package com.aura.controller;

import com.aura.entity.FollowCounts;
import com.aura.result.Result;
import com.aura.service.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/** 关注模块：关注、取关、查关注状态、查列表、查粉丝数 */
@RestController
@RequestMapping("/api/follow")
public class FollowController
{

    @Autowired
    private FollowService followService;

    /** 关注某作者，Redis ZSet + 异步写库 */
    @PostMapping("/{authorId}")
    public Result follow(@PathVariable Long authorId)
    {
        followService.follow(authorId);
        return Result.success();
    }

    /** 取消关注 */
    @PostMapping("/unfollow/{authorId}")
    public Result unfollow(@PathVariable Long authorId)
    {
        followService.unfollow(authorId);
        return Result.success();
    }

    /** 查询当前用户是否关注了该作者 */
    @GetMapping("/check/{authorId}")
    public Result isFollowing(@PathVariable Long authorId)
    {
        return Result.success(followService.isFollowing(authorId));
    }

    /** 查当前用户关注的人列表 */
    @GetMapping("/following")
    public Result getFollowing()
    {
        return Result.success(followService.getFollowing());
    }

    /** 查某作者的粉丝列表 */
    @GetMapping("/{authorId}/followers")
    public Result getFollowers(@PathVariable Long authorId)
    {
        return Result.success(followService.getFollowers(authorId));
    }

    /** 查当前用户的粉丝数和关注数 */
    @GetMapping("/counts")
    public Result getCounts()
    {
        Long followers = followService.getFollowerCount();
        Long following = followService.getFollowingCount();
        return Result.success(new FollowCounts(followers, following));
    }
}
