package com.aura.interceptor;

import com.aura.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class MicroUserInterceptor implements HandlerInterceptor
{

    // 从请求头 UserId 中提取用户 ID，存入 UserContext
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
    {
        String userId = request.getHeader("UserId");
        if (userId != null && !userId.isEmpty())
        {
            UserContext.setUserId(Long.parseLong(userId));
        }
        return true;
    }

    // 请求结束后清理 ThreadLocal，防止内存泄漏
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
    {
        UserContext.remove();
    }
}
