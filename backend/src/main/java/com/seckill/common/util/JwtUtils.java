package com.seckill.common.util;

import com.seckill.infrastructure.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {

    @Autowired
    private JwtProperties jwtProperties;

    public String generateToken(String username) {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getJwtExpireMinutes() * 60_000L);
        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String parseUsername(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            parseUsername(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
