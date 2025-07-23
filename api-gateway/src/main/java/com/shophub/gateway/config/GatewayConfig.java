package com.shophub.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.Arrays;

/**
 * Gateway 配置类
 * 
 * 包含:
 * - CORS 跨域配置
 * - 限流键解析器
 * - 路由和过滤器配置
 */
@Configuration
public class GatewayConfig {

    /**
     * CORS 跨域配置
     * 支持前端应用和移动端的跨域请求
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        
        // 允许的来源 (开发环境)
        corsConfiguration.setAllowedOriginPatterns(Arrays.asList("*"));
        
        // 允许的HTTP方法
        corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // 允许的请求头
        corsConfiguration.setAllowedHeaders(Arrays.asList("*"));
        
        // 允许携带认证信息
        corsConfiguration.setAllowCredentials(true);
        
        // 预检请求缓存时间
        corsConfiguration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        
        return new CorsWebFilter(source);
    }

    /**
     * 用户限流键解析器
     * 基于IP地址进行限流 (生产环境可基于用户ID)
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // 获取客户端IP作为限流键
            String clientIp = exchange.getRequest()
                .getRemoteAddress()
                .getAddress()
                .getHostAddress();
            
            // 检查是否有用户等级头部 (模拟premium用户)
            String userTier = exchange.getRequest()
                .getHeaders()
                .getFirst("X-User-Tier");
            
            // Premium用户使用不同的限流键前缀
            if ("premium".equalsIgnoreCase(userTier)) {
                return Mono.just("premium:" + clientIp);
            }
            
            return Mono.just("regular:" + clientIp);
        };
    }

    /**
     * API路径限流键解析器
     * 基于请求路径进行不同的限流策略
     */
    @Bean
    @Primary
    public KeyResolver apiKeyResolver() {
        return exchange -> {
            String path = exchange.getRequest().getPath().value();
            
            // 不同API路径使用不同限流策略
            if (path.startsWith("/api/orders")) {
                return Mono.just("orders");
            } else if (path.startsWith("/api/products")) {
                return Mono.just("products");
            } else if (path.startsWith("/api/users")) {
                return Mono.just("users");
            }
            
            return Mono.just("default");
        };
    }
}