package com.shophub.order.feign;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 产品服务降级处理
 * 当产品服务不可用时提供默认响应
 */
@Component
public class ProductServiceFallback implements ProductServiceClient {
    
    @Override
    public Map<String, Object> checkProductExists(Long productId) {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("exists", false);
        fallback.put("message", "产品服务暂时不可用，无法验证产品: " + productId);
        fallback.put("serviceInstance", "fallback");
        fallback.put("timestamp", System.currentTimeMillis());
        return fallback;
    }
    
    @Override
    public Map<String, Object> getProductDetails(Long productId) {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("error", "Product service unavailable");
        fallback.put("productId", productId);
        fallback.put("serviceInstance", "fallback");
        fallback.put("message", "产品服务暂时不可用");
        return fallback;
    }
    
    @Override
    public Map<String, Object> getProductServiceHealth() {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("status", "DOWN");
        fallback.put("service", "product-service");
        fallback.put("message", "产品服务健康检查失败，使用降级响应");
        fallback.put("serviceInstance", "fallback");
        fallback.put("timestamp", System.currentTimeMillis());
        return fallback;
    }
}