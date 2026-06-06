package com.aura.controller;

import com.aura.entity.User;
import com.aura.result.Result;
import com.aura.service.ReadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/** 用户读操作 */
@RestController
@RequestMapping("/api/read/user")
public class UserReadController
{

    @Autowired
    private ReadService readService;

    /** 按 id 查用户 */
    @GetMapping("/{id}")
    public Result getUser(@PathVariable Long id)
    {
        User user = readService.getUserById(id);
        if (user == null)
        {
            return Result.error("User not found");
        }
        return Result.success(user);
    }
}
