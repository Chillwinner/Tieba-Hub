package com.aura.controller;

import com.aura.entity.User;
import com.aura.result.Result;
import com.aura.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/** 用户写操作 */
@RestController
@RequestMapping("/api/write/user")
public class UserWriteController
{

    @Autowired
    private UserService userService;

    /** 注册 */
    @PostMapping("/register")
    public Result register(@RequestBody User user)
    {
        user = userService.register(user);
        return Result.success(user);
    }

    /** 修改个人信息 */
    @PutMapping("/profile")
    public Result updateProfile(@RequestBody User user)
    {
        user = userService.updateProfile(user);
        return Result.success(user);
    }
}
