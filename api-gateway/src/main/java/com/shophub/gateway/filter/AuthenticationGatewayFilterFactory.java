package com.shophub.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 自定义认证过滤器工厂
 * 
 * 功能:
 * - 模拟用户认证检查
 * - 用户等级识别 (regular/premium)
 * - API访问权限控制
 */
@Component
public class AuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthenticationGatewayFilterFactory.Config> {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationGatewayFilterFactory.class);

    public AuthenticationGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            var request = exchange.getRequest();
            var response = exchange.getResponse();

            // 获取认证头
            String authHeader = request.getHeaders().getFirst("Authorization");
            String userTier = request.getHeaders().getFirst("X-User-Tier");

            logger.info("🔐 Authentication check for path: {} - Auth: {} - UserTier: {}", 
                request.getPath(), authHeader != null ? "Present" : "Missing", userTier);

            // 模拟认证逻辑 (开发环境简化)
            if (config.isRequired() && (authHeader == null || authHeader.isEmpty())) {
                logger.warn("❌ Authentication required but missing for path: {}", request.getPath());
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }

            // 设置用户等级 (如果未指定，默认为regular)
            if (userTier == null || userTier.isEmpty()) {
                exchange = exchange.mutate()
                    .request(request.mutate()
                        .header("X-User-Tier", "regular")
                        .build())
                    .build();
            }

            logger.info("✅ Authentication passed for path: {}", request.getPath());
            return chain.filter(exchange);
        };
    }

    public static class Config {
        private boolean required = false;

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }
    }
}