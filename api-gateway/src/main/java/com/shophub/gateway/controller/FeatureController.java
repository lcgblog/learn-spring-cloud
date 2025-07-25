package com.shophub.gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/gateway")
@RefreshScope
public class FeatureController {

    @Value("${feature.recommendations.enabled:false}")
    private boolean recommendationsEnabled;

    @Value("${feature.realtime-inventory.enabled:false}")
    private boolean realtimeInventoryEnabled;

    @Value("${feature.multi-currency.enabled:false}")
    private boolean multiCurrencyEnabled;

    @Value("${payment.gateway.primary:stripe}")
    private String primaryPaymentGateway;

    @Value("${payment.gateway.fallback:paypal}")
    private String fallbackPaymentGateway;

    @GetMapping("/features")
    public Map<String, Object> getFeatureToggles() {
        Map<String, Object> features = new HashMap<>();
        features.put("recommendations", recommendationsEnabled);
        features.put("realtimeInventory", realtimeInventoryEnabled);
        features.put("multiCurrency", multiCurrencyEnabled);
        features.put("primaryPaymentGateway", primaryPaymentGateway);
        features.put("fallbackPaymentGateway", fallbackPaymentGateway);
        features.put("message", "当前功能开关配置状态");
        return features;
    }

    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "api-gateway");
        health.put("configLoaded", true);
        health.put("message", "API Gateway 配置中心连接正常");
        return health;
    }
}