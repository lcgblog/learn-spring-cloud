package com.shophub.user.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 产品服务 Feign 客户端
 * 
 * 用于用户服务调用产品服务的接口
 * 演示微服务间的通信机制
 */
@FeignClient(name = "product-service")
public interface ProductServiceClient {
    
    /**
     * 检查产品是否存在
     * @param productId 产品ID
     * @return 产品信息字符串
     */
    @GetMapping("/api/products/{productId}/exists")
    String checkProductExists(@PathVariable("productId") Long productId);
    
    /**
     * 获取产品详情
     * @param productId 产品ID  
     * @return 产品详情字符串
     */
    @GetMapping("/api/products/{productId}")
    String getProductDetails(@PathVariable("productId") Long productId);
} 