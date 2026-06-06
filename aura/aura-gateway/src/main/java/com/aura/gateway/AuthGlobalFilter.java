package com.aura.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** 网关全局鉴权过滤器：解析 JWT → 透传 X-User-Id 到下游微服务 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered
{

    private static final String SECRET = "aura-secret-key-2024-news-platform";

    /** 放行白名单，不需要鉴权的路径 */
    private static final String[] WHITE_LIST = {
            "/api/user/register",
            "/api/user/login"
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 白名单路径直接放行
        for (String white : WHITE_LIST)
        {
            if (path.contains(white))
            {
                return chain.filter(exchange);
            }
        }

        // 获取 Authorization 请求头
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer "))
        {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        // 解析 JWT
        try
        {
            String token = authHeader.substring(7);
            Claims claims = Jwts.parser()
                    .setSigningKey(SECRET)
                    .parseClaimsJws(token)
                    .getBody();
            String userId = claims.getSubject();

            // 将 userId 透传到下游微服务
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }
        catch (Exception e)
        {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
    }

    @Override
    public int getOrder()
    {
        return -100;
    }
}
