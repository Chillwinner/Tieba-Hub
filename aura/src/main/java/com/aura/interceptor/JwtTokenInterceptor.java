package com.aura.interceptor;
import com.aura.utils.UserContext;
import com.aura.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** JWT 拦截器：从 Header 解析 token，设置 UserContext，请求结束清理 */
@Component
public class JwtTokenInterceptor implements HandlerInterceptor
{

    /** 解析 Authorization: Bearer xxx，成功则写入 UserContext，失败放行（由业务层判空） */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
    {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer "))
        {
            try
            {
                Claims claims = JwtUtils.parse(token.substring(7));
                Long userId = Long.parseLong(claims.getSubject());
                UserContext.setCurrentId(userId);
            }
            catch (Exception ignored)
            {
            }
        }
        return true;
    }

    /** 请求结束，清理 ThreadLocal 防止内存泄漏 */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
    {
        UserContext.remove();
    }
}
