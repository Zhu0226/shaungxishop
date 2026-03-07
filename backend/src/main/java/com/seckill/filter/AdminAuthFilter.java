package com.seckill.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.common.ApiResponse;
import com.seckill.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 对 /admin/** 请求做 JWT 鉴权，放行 /admin/login。
 */
@Component
public class AdminAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || !path.startsWith("/admin")) {
            return true;
        }
        return path.equals("/admin/login");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            writeUnauthorized(response, "缺少或无效的 Authorization 头");
            return;
        }
        String token = auth.substring(7).trim();
        if (!jwtUtils.validateToken(token)) {
            writeUnauthorized(response, "Token 无效或已过期");
            return;
        }
        try {
            String username = jwtUtils.parseUsername(token);
            request.setAttribute("adminUsername", username);
        } catch (Exception e) {
            writeUnauthorized(response, "Token 解析失败");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<Void> body = ApiResponse.error(401, msg);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
