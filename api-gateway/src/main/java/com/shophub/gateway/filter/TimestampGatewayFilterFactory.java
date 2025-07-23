package com.shophub.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

/**
 * 时间戳过滤器工厂
 * 添加请求时间戳到请求头
 */
@Component
public class TimestampGatewayFilterFactory extends AbstractGatewayFilterFactory<TimestampGatewayFilterFactory.Config> {

    public TimestampGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // 添加时间戳到请求头
            String timestamp = String.valueOf(System.currentTimeMillis());
            exchange = exchange.mutate()
                .request(exchange.getRequest().mutate()
                    .header("X-Gateway-Timestamp", timestamp)
                    .build())
                .build();
            
            return chain.filter(exchange);
        };
    }

    public static class Config {
        // 配置类，目前为空
    }
}