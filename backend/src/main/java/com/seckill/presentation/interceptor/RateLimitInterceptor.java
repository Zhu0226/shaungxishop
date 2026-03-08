package com.seckill.presentation.interceptor;

import com.seckill.common.annotation.RateLimit;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collections;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String LUA_SCRIPT =
            "local c = redis.call('incr', KEYS[1]) " +
            "if tonumber(c) == 1 then redis.call('expire', KEYS[1], ARGV[1]) end " +
            "return c";

    private final DefaultRedisScript<Long> limitScript = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
            if (rateLimit == null) return true;

            int time = rateLimit.time();
            int count = rateLimit.count();

            String userId = request.getHeader("userId");
            if (userId == null) {
                // Nginx/网关场景：getRemoteAddr 为代理 IP，需取 X-Forwarded-For 首个（真实客户端）
                // 由网关覆盖该 Header 确保可信；无代理时 fallback 到 getRemoteAddr
                String forwarded = request.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isEmpty()) {
                    int comma = forwarded.indexOf(',');
                    userId = comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
                }
                if (userId == null || userId.isEmpty()) {
                    userId = request.getHeader("X-Real-IP");
                }
                if (userId == null || userId.isEmpty()) {
                    userId = request.getRemoteAddr();
                }
            }

            String uri = request.getRequestURI();
            String key = "rate_limit:" + uri + ":" + userId;

            Long currentCount = stringRedisTemplate.execute(limitScript, Collections.singletonList(key), String.valueOf(time));

            if (currentCount != null && currentCount > count) {
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":429,\"msg\":\"请求太频繁，请稍后再试\",\"data\":null}");
                return false;
            }
        }
        return true;
    }
}
