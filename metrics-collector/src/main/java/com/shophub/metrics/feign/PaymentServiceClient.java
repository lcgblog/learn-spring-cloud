package com.shophub.metrics.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@FeignClient(name = "payment-service", fallback = PaymentServiceClientFallback.class)
public interface PaymentServiceClient {

    @GetMapping("/actuator/health")
    Map<String, Object> getHealth();

    @GetMapping("/actuator/metrics")
    Map<String, Object> getMetrics();

    @GetMapping("/actuator/prometheus")
    String getPrometheusMetrics();

    @GetMapping("/api/payments/circuit-breaker/status")
    Map<String, Object> getCircuitBreakerStatus();
}