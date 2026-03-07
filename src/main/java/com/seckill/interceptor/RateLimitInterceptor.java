package com.seckill.interceptor;

import com.seckill.annotation.RateLimit;
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

    // 修复：采用 Lua 脚本保证增量和设置过期时间的原子性
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
                // 修复：如果未登录，使用 IP 而不是固定的 guest，防止单点刷库导致全网真实用户被封
                userId = request.getHeader("X-Forwarded-For");
                if (userId == null) userId = request.getRemoteAddr();
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