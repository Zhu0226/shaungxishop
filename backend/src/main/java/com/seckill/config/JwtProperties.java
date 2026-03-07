package com.seckill.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 与后台登录配置（密钥、过期时间、演示用账号）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "admin")
public class JwtProperties {

    /** JWT 签名密钥 */
    private String jwtSecret = "flash-sale-admin-secret-change-in-production";
    /** 过期时间（分钟） */
    private int jwtExpireMinutes = 120;
    /** 演示用管理员账号 */
    private String loginUsername = "admin";
    private String loginPassword = "123456";
}
