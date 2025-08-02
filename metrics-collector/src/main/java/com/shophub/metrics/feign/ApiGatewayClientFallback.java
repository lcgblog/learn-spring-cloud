package com.shophub.metrics.feign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ApiGatewayClientFallback implements ApiGatewayClient {

    private static final Logger logger = LoggerFactory.getLogger(ApiGatewayClientFallback.class);

    @Override
    public Map<String, Object> getHealth() {
        logger.warn("Fallback: api-gateway health check failed");
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("status", "DOWN");
        fallback.put("service", "api-gateway");
        fallback.put("fallback", true);
        return fallback;
    }

    @Override
    public Map<String, Object> getMetrics() {
        logger.warn("Fallback: api-gateway metrics unavailable");
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("error", "service unavailable");
        fallback.put("fallback", true);
        return fallback;
    }

    @Override
    public String getPrometheusMetrics() {
        logger.warn("Fallback: api-gateway prometheus metrics unavailable");
        return "# Fallback: api-gateway prometheus metrics unavailable";
    }

    @Override
    public Map<String, Object> getRoutes() {
        logger.warn("Fallback: api-gateway routes unavailable");
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("error", "routes unavailable");
        fallback.put("fallback", true);
        return fallback;
    }

    @Override
    public Map<String, Object> getCircuitBreakers() {
        logger.warn("Fallback: api-gateway circuit breakers unavailable");
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("error", "circuit breakers unavailable");
        fallback.put("fallback", true);
        return fallback;
    }
}