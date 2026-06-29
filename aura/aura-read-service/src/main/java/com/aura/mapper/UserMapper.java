package com.aura.mapper;

import com.aura.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// 用户持久层：CRUD + 按手机号查
@Mapper
public interface UserMapper
{
    // 插入新用户
    int insert(User user);

    // 按 id 查用户
    User findById(@Param("id") Long id);

    // 按手机号查用户（注册去重/登录用）
    User findByPhone(@Param("phone") String phone);

    // 查全部用户
    List<User> findAll();

    // 更新用户资料
    int update(User user);

    // 按 id 删用户
    int deleteById(@Param("id") Long id);
}
