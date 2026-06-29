package com.aura.config;

import com.aura.interceptor.MicroUserInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer
{

    @Autowired
    private MicroUserInterceptor microUserInterceptor;

    // 注册拦截器，拦截所有 /api/** 请求
    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(microUserInterceptor)
                .addPathPatterns("/api/**");
    }
}
