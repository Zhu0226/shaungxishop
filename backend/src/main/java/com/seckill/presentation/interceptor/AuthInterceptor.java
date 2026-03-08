package com.seckill.presentation.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.common.ApiResponse;
import com.seckill.common.annotation.RequiresPermission;
import com.seckill.common.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        if (!(handler instanceof HandlerMethod)) return true;

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return reject(response, 401, "身份凭证缺失，请重新登录");
        }

        String token = authHeader.substring(7).trim();
        String userIdStr;
        try {
            userIdStr = jwtUtils.parseUsername(token);
        } catch (Exception e) {
            return reject(response, 401, "登录已过期，请重新登录");
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequiresPermission reqPerm = handlerMethod.getMethodAnnotation(RequiresPermission.class);

        if (reqPerm != null) {
            String expectedPerm = reqPerm.value();
            String permsStr = stringRedisTemplate.opsForValue().get("auth:perms:" + userIdStr);

            if (permsStr == null) {
                return reject(response, 401, "权限信息已过期，请重新登录");
            }

            List<String> perms = Arrays.asList(permsStr.split(","));
            if (!perms.contains(expectedPerm)) {
                return reject(response, 403, "非法越权：您没有执行此操作的权限 (" + expectedPerm + ")");
            }
        }

        return true;
    }

    private boolean reject(HttpServletResponse response, int code, String msg) throws Exception {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(code, msg)));
        return false;
    }
}
