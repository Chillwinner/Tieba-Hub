package com.aura.interceptor;

import com.aura.utils.UserContext;
import com.alibaba.fastjson2.JSON;
import com.aura.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

// 从请求头提取 UserId 并存入 UserContext
@Component
public class MicroUserInterceptor implements HandlerInterceptor
{

    // 从 Header 取出 UserId 设置到 ThreadLocal，缺失则拒绝请求
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception
    {
        String userId = request.getHeader("UserId");
        if (userId == null || userId.isEmpty())
        {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSON.toJSONString(Result.error("未授权：缺少用户身份信息")));
            return false;
        }
        UserContext.setUserId(Long.parseLong(userId));
        return true;
    }

    // 请求结束清理 ThreadLocal
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
    {
        UserContext.remove();
    }
}
