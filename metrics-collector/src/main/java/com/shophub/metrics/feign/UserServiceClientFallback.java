package com.shophub.metrics.feign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class UserServiceClientFallback implements UserServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceClientFallback.class);

    @Override
    public Map<String, Object> getHealth() {
        logger.warn("Fallback: user-service health check failed");
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("status", "DOWN");
        fallback.put("service", "user-service");
        fallback.put("fallback", true);
        return fallback;
    }

    @Override
    public Map<String, Object> getMetrics() {
        logger.warn("Fallback: user-service metrics unavailable");
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("error", "service unavailable");
        fallback.put("fallback", true);
        return fallback;
    }

    @Override
    public String getPrometheusMetrics() {
        logger.warn("Fallback: user-service prometheus metrics unavailable");
        return "# Fallback: user-service prometheus metrics unavailable";
    }
}