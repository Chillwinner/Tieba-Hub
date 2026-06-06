package com.aura.utils;

/** ThreadLocal 持有当前请求的登录用户 id，由 JWT 拦截器设置，业务层通过 getCurrentId() 获取 */
public class UserContext
{
    private static final ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    /** 设置当前登录用户 id */
    public static void setCurrentId(Long id)
    {
        threadLocal.set(id);
    }

    /** 获取当前登录用户 id */
    public static Long getCurrentId()
    {
        return threadLocal.get();
    }

    /** 清理，防止线程池复用时脏数据 */
    public static void remove()
    {
        threadLocal.remove();
    }
}
