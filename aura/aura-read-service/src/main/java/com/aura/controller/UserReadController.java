package com.aura.controller;

import com.aura.entity.User;
import com.aura.result.Result;
import com.aura.service.UserReadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/read/user")
public class UserReadController
{

    @Autowired
    private UserReadService userReadService;

    // 按 ID 查询用户信息
    @GetMapping("/{id}")
    public Result getUser(@PathVariable Long id)
    {
        User user = userReadService.getUserById(id);
        if (user == null)
        {
            return Result.error("User not found");
        }
        return Result.success(user);
    }
}
