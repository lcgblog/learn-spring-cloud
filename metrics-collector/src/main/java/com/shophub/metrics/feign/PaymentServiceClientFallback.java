package com.shophub.metrics.feign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class PaymentServiceClientFallback implements PaymentServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceClientFallback.class);

    @Override
    public Map<String, Object> getHealth() {
        logger.warn("Fallback: payment-service health check failed");
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("status", "DOWN");
        fallback.put("service", "payment-service");
        fallback.put("fallback", true);
        return fallback;
    }

    @Override
    public Map<String, Object> getMetrics() {
        logger.warn("Fallback: payment-service metrics unavailable");
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("error", "service unavailable");
        fallback.put("fallback", true);
        return fallback;
    }

    @Override
    public String getPrometheusMetrics() {
        logger.warn("Fallback: payment-service prometheus metrics unavailable");
        return "# Fallback: payment-service prometheus metrics unavailable";
    }

    @Override
    public Map<String, Object> getCircuitBreakerStatus() {
        logger.warn("Fallback: payment-service circuit breaker status unavailable");
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("status", "UNKNOWN");
        fallback.put("fallback", true);
        return fallback;
    }
}