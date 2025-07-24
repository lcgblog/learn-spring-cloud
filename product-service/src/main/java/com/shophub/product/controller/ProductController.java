package com.shophub.product.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 产品控制器
 * 
 * 提供产品管理的 REST API 接口
 * 支持与其他微服务的通信
 */
@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {
    
    @Value("${server.port}")
    private String serverPort;
    
    @Value("${spring.application.name}")
    private String serviceName;
    
    // 模拟产品数据 (实际项目中应该连接数据库)
    private static final List<Map<String, Object>> MOCK_PRODUCTS = Arrays.asList(
        createProduct(1L, "iPhone 15 Pro", "Apple", 8999.00, true),
        createProduct(2L, "MacBook Pro M3", "Apple", 15999.00, true),
        createProduct(3L, "Samsung Galaxy S24", "Samsung", 6999.00, true),
        createProduct(4L, "ThinkPad X1 Carbon", "Lenovo", 12999.00, false),
        createProduct(5L, "iPad Air", "Apple", 4299.00, true)
    );
    
    /**
     * 检查产品是否存在 (用于服务间通信)
     * GET /api/products/{productId}/exists
     */
    @GetMapping("/{productId}/exists")
    public ResponseEntity<Map<String, Object>> checkProductExists(@PathVariable Long productId) {
        boolean exists = MOCK_PRODUCTS.stream()
            .anyMatch(product -> productId.equals(product.get("id")));
        
        Map<String, Object> response = new HashMap<>();
        response.put("exists", exists);
        response.put("message", exists ? "产品ID " + productId + " 存在" : "产品ID " + productId + " 不存在");
        response.put("serviceInstance", serviceName + ":" + serverPort);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取产品详情 (用于服务间通信)
     * GET /api/products/{productId}
     */
    @GetMapping("/{productId}")
    public ResponseEntity<?> getProductDetails(@PathVariable Long productId) {
        Map<String, Object> product = MOCK_PRODUCTS.stream()
            .filter(p -> productId.equals(p.get("id")))
            .findFirst()
            .orElse(null);
        
        if (product != null) {
            Map<String, Object> enrichedProduct = new HashMap<>(product);
            enrichedProduct.put("serviceInstance", serviceName + ":" + serverPort);
            enrichedProduct.put("responseTime", System.currentTimeMillis());
            return ResponseEntity.ok(enrichedProduct);
        } else {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Product not found");
            errorResponse.put("serviceInstance", serviceName + ":" + serverPort);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 获取所有产品
     * GET /api/products
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllProducts() {
        return ResponseEntity.ok(MOCK_PRODUCTS);
    }
    
    /**
     * 获取可用产品
     * GET /api/products/available
     */
    @GetMapping("/available")
    public ResponseEntity<List<Map<String, Object>>> getAvailableProducts() {
        List<Map<String, Object>> availableProducts = MOCK_PRODUCTS.stream()
            .filter(product -> (Boolean) product.get("available"))
            .toList();
        
        return ResponseEntity.ok(availableProducts);
    }
    
    /**
     * 搜索产品
     * GET /api/products/search?keyword=xxx
     */
    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchProducts(@RequestParam String keyword) {
        List<Map<String, Object>> searchResults = MOCK_PRODUCTS.stream()
            .filter(product -> {
                String name = (String) product.get("name");
                String brand = (String) product.get("brand");
                return name.toLowerCase().contains(keyword.toLowerCase()) ||
                       brand.toLowerCase().contains(keyword.toLowerCase());
            })
            .toList();
        
        return ResponseEntity.ok(searchResults);
    }
    
    /**
     * 健康检查
     * GET /api/products/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", serviceName);
        health.put("port", serverPort);
        health.put("productsCount", MOCK_PRODUCTS.size());
        health.put("timestamp", System.currentTimeMillis());
        health.put("message", "Product Service 实例运行正常，端口: " + serverPort);
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * 创建模拟产品对象
     */
    private static Map<String, Object> createProduct(Long id, String name, String brand, Double price, Boolean available) {
        Map<String, Object> product = new HashMap<>();
        product.put("id", id);
        product.put("name", name);
        product.put("brand", brand);
        product.put("price", price);
        product.put("available", available);
        product.put("createdAt", System.currentTimeMillis());
        return product;
    }
} 