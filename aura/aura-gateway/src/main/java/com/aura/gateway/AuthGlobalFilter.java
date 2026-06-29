package com.aura.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
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

// 全局鉴权过滤器，验证JWT并传递userId到下游微服务
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered
{

    @Value("${jwt.secret}")
    private String secret;

    // 白名单路径，不需要鉴权（必须精确匹配）
    private static final String[] WHITE_LIST = {
            "/api/write/user/register",
            "/api/write/user/login"
    };

    // 公开读取路径前缀，不需要鉴权
    private static final String[] PUBLIC_READ_PREFIXES = {
            "/api/read/news/",
            "/api/read/comment/",
            "/api/read/user/",
            "/api/read/follow/"
    };

    // 需要鉴权的路径前缀（白名单和公开读取都不匹配时，默认需要鉴权）
    private static final String[] AUTH_REQUIRED_READ_PREFIXES = {
            "/api/read/news/feed",
            "/api/read/follow/counts",
            "/api/read/follow/following"
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 清理客户端传入的 UserId，防止伪造
        ServerHttpRequest cleanedRequest = request.mutate()
                .headers(headers -> headers.remove("UserId"))
                .build();
        ServerWebExchange cleanedExchange = exchange.mutate().request(cleanedRequest).build();

        // 白名单精确匹配直接放行
        for (String white : WHITE_LIST)
        {
            if (path.equals(white))
            {
                return chain.filter(cleanedExchange);
            }
        }

        // 检查是否需要鉴权的读路径
        boolean needAuth = false;
        for (String prefix : AUTH_REQUIRED_READ_PREFIXES)
        {
            if (path.startsWith(prefix))
            {
                needAuth = true;
                break;
            }
        }

        // 不在强制鉴权列表中，检查是否为公开读取
        if (!needAuth)
        {
            for (String prefix : PUBLIC_READ_PREFIXES)
            {
                if (path.startsWith(prefix))
                {
                    return chain.filter(cleanedExchange);
                }
            }
            needAuth = true;
        }

        // 获取Authorization请求头
        String authHeader = cleanedRequest.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer "))
        {
            ServerHttpResponse response = cleanedExchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        // 解析JWT
        try
        {
            String token = authHeader.substring(7);
            Claims claims = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
            String userId = claims.getSubject();

            // 将userId透传到下游服务
            ServerHttpRequest mutatedRequest = cleanedRequest.mutate()
                    .header("UserId", userId)
                    .build();

            return chain.filter(cleanedExchange.mutate().request(mutatedRequest).build());
        }
        catch (Exception e)
        {
            ServerHttpResponse response = cleanedExchange.getResponse();
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