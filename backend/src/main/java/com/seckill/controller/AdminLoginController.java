package com.seckill.controller;

import com.seckill.common.ApiResponse;
import com.seckill.config.JwtProperties;
import com.seckill.util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 后台登录：校验账号密码后返回 JWT，请求 /admin/** 时在 Header 中携带 Authorization: Bearer &lt;token&gt;。
 */
@RestController
@RequestMapping("/admin")
public class AdminLoginController {

    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/login")
    public ApiResponse<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String username = body != null ? body.get("username") : null;
        String password = body != null ? body.get("password") : null;
        if (!jwtProperties.getLoginUsername().equals(username) || !jwtProperties.getLoginPassword().equals(password)) {
            return ApiResponse.error(401, "用户名或密码错误");
        }
        String token = jwtUtils.generateToken(username);
        return ApiResponse.success(Map.of("token", token, "username", username));
    }
}
