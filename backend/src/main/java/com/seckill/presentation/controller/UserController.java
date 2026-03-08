package com.seckill.presentation.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.common.ApiResponse;
import com.seckill.common.util.JwtUtils;
import com.seckill.domain.entity.User;
import com.seckill.persistence.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestBody User user) {
        if (user.getUsername() == null || user.getPassword() == null) {
            return ApiResponse.error(400, "用户名和密码不能为空");
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            return ApiResponse.error(400, "用户名已存在，请直接登录");
        }
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return ApiResponse.success("注册成功", null);
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody User user) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername())
                .eq(User::getPassword, user.getPassword());
        User dbUser = userMapper.selectOne(wrapper);
        if (dbUser == null) {
            return ApiResponse.error(401, "用户名或密码错误");
        }

        String token = jwtUtils.generateToken(String.valueOf(dbUser.getId()));

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", dbUser.getId());
        data.put("username", dbUser.getUsername());
        return ApiResponse.success("登录成功", data);
    }
}
