package com.seckill.presentation.config;

import com.seckill.presentation.interceptor.AuthInterceptor;
import com.seckill.presentation.interceptor.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;
    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/seckill/preheat/**");

        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",
                        "/error",
                        "/seckill/goods/**",
                        "/seckill/status/**"
                );
    }
}
