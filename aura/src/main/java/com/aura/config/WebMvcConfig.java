package com.aura.config;

import com.aura.interceptor.JwtTokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Web 配置：注册 JWT 拦截器到 /api/** 路径 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer
{

    @Autowired
    private JwtTokenInterceptor jwtTokenInterceptor;

    /** 注册 JWT 拦截器，拦截所有 /api/** 请求 */
    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(jwtTokenInterceptor)
                .addPathPatterns("/api/**");
    }
}
