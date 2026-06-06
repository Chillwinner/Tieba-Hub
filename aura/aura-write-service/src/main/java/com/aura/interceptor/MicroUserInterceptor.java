package com.aura.interceptor;

import com.aura.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** 微服务通用拦截器：从网关透传的 X-User-Id 请求头中提取 userId，植入 UserContext */
@Component
public class MicroUserInterceptor implements HandlerInterceptor
{

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
    {
        String userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isEmpty())
        {
            UserContext.setCurrentId(Long.parseLong(userId));
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
    {
        UserContext.remove();
    }
}
