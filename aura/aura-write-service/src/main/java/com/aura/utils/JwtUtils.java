package com.aura.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

// JWT 工具类，负责签发、解析、校验 token
@Component
public class JwtUtils
{

    @Value("${jwt.secret}")
    private String secret;

    private static final long EXPIRATION = 86400000;

    // 签发 JWT，有效期 24 小时
    public String generate(Long userId)
    {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    // 解析 JWT 返回 Claims
    public Claims parse(String token)
    {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }

    // 从 token 提取 userId
    public Long getUserId(String token)
    {
        return Long.parseLong(parse(token).getSubject());
    }

    // 校验 token 是否有效
    public boolean validate(String token)
    {
        try
        {
            parse(token);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }
}
