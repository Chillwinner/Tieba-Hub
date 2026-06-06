package com.aura.controller;

import com.aura.entity.User;
import com.aura.result.Result;
import com.aura.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/** 用户模块：注册、登录、查询、改资料 */
@RestController
@RequestMapping("/api/user")
public class UserController
{

    @Autowired
    private UserService userService;

    /** 用户注册，返回完整 User（含 id、密码已脱敏） */
    @PostMapping("/register")
    public Result register(@RequestBody User user)
    {
        user = userService.register(user);
        return Result.success(user);
    }

    /** 手机号+密码登录，成功返回 JWT token */
    @PostMapping("/login")
    public Result login(@RequestParam String phone,
                         @RequestParam String password)
    {
        String token = userService.login(phone, password);
        return Result.success(token);
    }

    /** 按 id 查单个用户，不存在返回错误 */
    @GetMapping("/{id}")
    public Result getUser(@PathVariable Long id)
    {
        User user = userService.getUserById(id);
        if (user == null)
        {
            return Result.error("User not found");
        }
        return Result.success(user);
    }

    /** 更新当前登录用户的昵称/邮箱，JWT 鉴权 */
    @PutMapping("/profile")
    public Result updateProfile(@RequestBody User user)
    {
        user = userService.updateProfile(user);
        return Result.success(user);
    }
}
