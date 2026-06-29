package com.aura.utils;

// ThreadLocal 存储当前请求的用户 ID
public class UserContext
{
    private static final ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    // 设置当前用户 ID
    public static void setUserId(Long id)
    {
        threadLocal.set(id);
    }

    // 获取当前用户 ID
    public static Long getUserId()
    {
        return threadLocal.get();
    }

    // 清理当前线程的用户 ID
    public static void remove()
    {
        threadLocal.remove();
    }
}
