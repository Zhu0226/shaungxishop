package com.seckill.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "admin")
public class JwtProperties {

    private String jwtSecret = "flash-sale-admin-secret-change-in-production";
    private int jwtExpireMinutes = 120;
    private String loginUsername = "admin";
    private String loginPassword = "123456";
}
