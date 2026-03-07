package com.seckill.config;

import com.seckill.interceptor.AuthInterceptor;
import com.seckill.interceptor.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired private RateLimitInterceptor rateLimitInterceptor;
    @Autowired private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 限流拦截器
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/seckill/preheat/**");

        // 核心安全拦截器
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login", // 登录接口放行
                        "/error",
                        "/seckill/goods/**", // C端商品列表和详情允许未登录查看
                        "/seckill/status/**"
                );
    }
}