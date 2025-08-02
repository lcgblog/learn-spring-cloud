package com.shophub.metrics.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@FeignClient(name = "api-gateway", fallback = ApiGatewayClientFallback.class)
public interface ApiGatewayClient {

    @GetMapping("/actuator/health")
    Map<String, Object> getHealth();

    @GetMapping("/actuator/metrics")
    Map<String, Object> getMetrics();

    @GetMapping("/actuator/prometheus")
    String getPrometheusMetrics();

    @GetMapping("/actuator/gateway/routes")
    Map<String, Object> getRoutes();

    @GetMapping("/actuator/circuitbreakers")
    Map<String, Object> getCircuitBreakers();
}