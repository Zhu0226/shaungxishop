package com.seckill.presentation.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.common.ApiResponse;
import com.seckill.common.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");
        String headerUserId = request.getHeader("userId");

        if (authHeader == null || !authHeader.startsWith("Bearer ") || headerUserId == null) {
            return reject(response, "未登录或缺少身份凭证，请登录后重试");
        }

        String token = authHeader.substring(7).trim();
        try {
            String tokenUserId = jwtUtils.parseUsername(token);
            if (!headerUserId.equals(tokenUserId)) {
                return reject(response, "非法请求：身份不匹配");
            }
            return true;
        } catch (Exception e) {
            return reject(response, "登录已过期，请重新登录");
        }
    }

    private boolean reject(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        ObjectMapper mapper = new ObjectMapper();
        response.getWriter().write(mapper.writeValueAsString(ApiResponse.error(401, msg)));
        return false;
    }
}
