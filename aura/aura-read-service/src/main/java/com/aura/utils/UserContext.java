package com.aura.utils;

// 通过 ThreadLocal 持有当前请求的登录用户 ID，由拦截器设置，业务层通过 getUserId() 获取
public class UserContext
{
    private static final ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    // 设置当前线程的用户 ID
    public static void setUserId(Long id)
    {
        threadLocal.set(id);
    }

    // 获取当前线程的用户 ID
    public static Long getUserId()
    {
        return threadLocal.get();
    }

    // 清除当前线程的用户 ID，防止内存泄漏
    public static void remove()
    {
        threadLocal.remove();
    }
}
