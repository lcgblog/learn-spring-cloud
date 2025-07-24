package com.shophub.order.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * 产品服务 Feign 客户端
 * 用于订单服务调用产品服务，演示负载均衡
 */
@FeignClient(name = "product-service")
public interface ProductServiceClient {
    
    /**
     * 检查产品是否存在
     */
    @GetMapping("/api/products/{productId}/exists")
    Map<String, Object> checkProductExists(@PathVariable("productId") Long productId);
    
    /**
     * 获取产品详情
     */
    @GetMapping("/api/products/{productId}")
    Map<String, Object> getProductDetails(@PathVariable("productId") Long productId);
    
    /**
     * 获取产品服务健康状态 
     */
    @GetMapping("/api/products/health")
    Map<String, Object> getProductServiceHealth();
}