package com.seckill.presentation.controller;

import com.seckill.application.service.impl.SysUserService;
import com.seckill.common.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private SysUserService sysUserService;

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        return ApiResponse.success("登录成功", sysUserService.login(username, password));
    }
}
