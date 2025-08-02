package com.shophub.metrics.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@FeignClient(name = "order-service", fallback = OrderServiceClientFallback.class)
public interface OrderServiceClient {

    @GetMapping("/actuator/health")
    Map<String, Object> getHealth();

    @GetMapping("/actuator/metrics")
    Map<String, Object> getMetrics();

    @GetMapping("/actuator/prometheus")
    String getPrometheusMetrics();

    @GetMapping("/api/orders/circuit-breaker/status")
    Map<String, Object> getCircuitBreakerStatus();
}