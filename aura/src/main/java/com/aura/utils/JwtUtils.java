package com.aura.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;

/** JWT 工具类：签发、解析、校验 */
public class JwtUtils
{

    private static final String SECRET = "aura-secret-key-2024-news-platform";
    private static final long EXPIRATION = 86400000;

    /** 签发 JWT，载荷含 userId + phone，有效期 24h */
    public static String generate(Long userId, String phone)
    {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("phone", phone)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .compact();
    }

    /** 解析 JWT，返回 Claims，失败抛异常 */
    public static Claims parse(String token)
    {
        return Jwts.parser()
                .setSigningKey(SECRET)
                .parseClaimsJws(token)
                .getBody();
    }

    /** 从 token 中提取 userId */
    public static Long getUserId(String token)

    {
        return Long.parseLong(parse(token).getSubject());
    }

    /** 校验 token 是否有效，true=有效 false=过期/伪造 */
    public static boolean validate(String token)
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
