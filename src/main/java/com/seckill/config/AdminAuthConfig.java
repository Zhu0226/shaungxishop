package com.seckill.config;

import com.seckill.filter.AdminAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 注册 /admin/** 的 JWT 鉴权过滤器。
 */
@Configuration
public class AdminAuthConfig {

    @Bean
    public FilterRegistrationBean<AdminAuthFilter> adminAuthFilterRegistration(AdminAuthFilter filter) {
        FilterRegistrationBean<AdminAuthFilter> reg = new FilterRegistrationBean<>(filter);
        reg.addUrlPatterns("/admin/*");
        reg.setOrder(1);
        return reg;
    }
}
