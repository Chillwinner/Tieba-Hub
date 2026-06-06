package com.aura.config;

import com.aura.interceptor.MicroUserInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 注册微服务通用拦截器，拦截所有 /api/** 请求 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer
{

    @Autowired
    private MicroUserInterceptor microUserInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(microUserInterceptor)
                .addPathPatterns("/api/**");
    }
}
