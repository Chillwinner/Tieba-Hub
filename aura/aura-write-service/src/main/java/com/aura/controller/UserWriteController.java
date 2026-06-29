package com.aura.controller;

import com.aura.entity.User;
import com.aura.result.Result;
import com.aura.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

// 用户写操作接口
@RestController
@RequestMapping("/api/write/user")
public class UserWriteController
{

    @Autowired
    private UserService userService;

    // 注册新用户
    @PostMapping("/register")
    public Result register(@RequestBody User user)
    {
        user = userService.register(user);
        return Result.success(user);
    }

    // 用户登录
    @PostMapping("/login")
    public Result login(@RequestParam String phone, @RequestParam String password)
    {
        String token = userService.login(phone, password);
        return Result.success(token);
    }

    // 修改个人信息
    @PutMapping("/profile")
    public Result updateProfile(@RequestBody User user)
    {
        user = userService.updateProfile(user);
        return Result.success(user);
    }
}
