package com.aura.config;

import com.aura.interceptor.MicroUserInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Web MVC 配置，注册拦截器
@Configuration
public class WebMvcConfig implements WebMvcConfigurer
{

    @Autowired
    private MicroUserInterceptor microUserInterceptor;

    // 注册拦截器，拦截 /api/** 路径，排除注册和登录（网关白名单，无 UserId 头）
    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(microUserInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/write/user/register", "/api/write/user/login");
    }
}
