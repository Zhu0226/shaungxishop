package com.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.entity.SysUser;
import com.seckill.exception.BusinessException;
import com.seckill.mapper.SysMenuMapper;
import com.seckill.mapper.SysUserMapper;
import com.seckill.util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class SysUserService {

    @Autowired private SysUserMapper sysUserMapper;
    @Autowired private SysMenuMapper sysMenuMapper;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private StringRedisTemplate stringRedisTemplate;

    public Map<String, Object> login(String username, String password) {
        // 1. 查库校验密码
        LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<>();
        query.eq(SysUser::getUsername, username).eq(SysUser::getPassword, password);
        SysUser user = sysUserMapper.selectOne(query);

        if (user == null) throw new BusinessException(401, "账号或密码错误");
        if (user.getStatus() == 0) throw new BusinessException(403, "该账号已被冻结");

        // 2. 查 RBAC 权限集合
        List<String> perms = sysMenuMapper.selectPermsByUserId(user.getId());

        // 3. 权限塞入 Redis，过期时间 2 小时 (与 JWT 保持一致)
        stringRedisTemplate.opsForValue().set(
                "auth:perms:" + user.getId(),
                String.join(",", perms),
                2, TimeUnit.HOURS
        );

        // 4. 生成统一 JWT Token
        String token = jwtUtils.generateToken(String.valueOf(user.getId()));

        // 5. 返回视图数据
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("perms", perms);
        return data;
    }
}