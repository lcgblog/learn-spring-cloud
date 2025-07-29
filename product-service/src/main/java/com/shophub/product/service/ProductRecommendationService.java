package com.shophub.product.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 产品推荐服务
 * 模拟外部推荐API调用，展示熔断器、重试和超时控制
 */
@Service
public class ProductRecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(ProductRecommendationService.class);
    
    private final Random random = new Random();
    private int callCount = 0;
    
    // 模拟产品数据
    private static final List<Map<String, Object>> ALL_PRODUCTS = Arrays.asList(
        createProduct(1L, "iPhone 15 Pro", "Apple", 8999.00, "smartphone"),
        createProduct(2L, "MacBook Pro M3", "Apple", 15999.00, "laptop"),
        createProduct(3L, "Samsung Galaxy S24", "Samsung", 6999.00, "smartphone"),
        createProduct(4L, "ThinkPad X1 Carbon", "Lenovo", 12999.00, "laptop"),
        createProduct(5L, "iPad Air", "Apple", 4299.00, "tablet"),
        createProduct(6L, "AirPods Pro", "Apple", 1899.00, "accessory"),
        createProduct(7L, "Dell XPS 13", "Dell", 8999.00, "laptop"),
        createProduct(8L, "Surface Pro", "Microsoft", 7999.00, "tablet"),
        createProduct(9L, "Galaxy Watch", "Samsung", 2299.00, "accessory"),
        createProduct(10L, "MacBook Air", "Apple", 8999.00, "laptop")
    );
    
    /**
     * 获取个性化推荐 - 使用熔断器、重试和超时控制
     */
    @CircuitBreaker(name = "recommendation-service", fallbackMethod = "fallbackRecommendations")
    @Retry(name = "recommendation-service")
    @TimeLimiter(name = "recommendation-service")
    @Bulkhead(name = "recommendation-service", type = Bulkhead.Type.THREADPOOL)
    public CompletableFuture<List<Map<String, Object>>> getPersonalizedRecommendations(Long userId, String category) {
        return CompletableFuture.supplyAsync(() -> {
            callCount++;
            logger.info("Fetching personalized recommendations for user: {}, category: {}, call: {}", 
                       userId, category, callCount);
            
            // 模拟网络延迟
            simulateNetworkDelay();
            
            // 模拟不同的失败场景
            simulateFailureScenarios();
            
            // 基于用户和类别的推荐算法
            List<Map<String, Object>> recommendations = generateRecommendations(userId, category, 5);
            
            logger.info("Successfully generated {} recommendations for user: {}", 
                       recommendations.size(), userId);
            return recommendations;
        });
    }
    
    /**
     * 获取热门商品推荐 - 作为降级方案
     */
    @CircuitBreaker(name = "popular-products", fallbackMethod = "fallbackPopularProducts")
    public CompletableFuture<List<Map<String, Object>>> getPopularProducts(String category, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Fetching popular products for category: {}, limit: {}", category, limit);
            
            // 模拟较短的处理时间
            try {
                Thread.sleep(100 + random.nextInt(200));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 模拟偶尔的失败
            if (random.nextInt(10) == 0) {
                throw new RuntimeException("Popular products service temporarily unavailable");
            }
            
            List<Map<String, Object>> popularProducts = ALL_PRODUCTS.stream()
                .filter(product -> category == null || category.equals(product.get("category")))
                .sorted((p1, p2) -> Double.compare((Double) p2.get("price"), (Double) p1.get("price")))
                .limit(limit)
                .toList();
            
            logger.info("Retrieved {} popular products", popularProducts.size());
            return popularProducts;
        });
    }
    
    /**
     * 获取相似商品推荐
     */
    @CircuitBreaker(name = "similar-products", fallbackMethod = "fallbackSimilarProducts")
    @Retry(name = "similar-products")
    public List<Map<String, Object>> getSimilarProducts(Long productId, int limit) {
        logger.info("Fetching similar products for product: {}, limit: {}", productId, limit);
        
        // 模拟处理延迟
        try {
            Thread.sleep(200 + random.nextInt(300));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 模拟失败场景
        if (random.nextInt(5) == 0) {
            throw new RuntimeException("Similar products algorithm failed");
        }
        
        // 查找目标产品
        Optional<Map<String, Object>> targetProduct = ALL_PRODUCTS.stream()
            .filter(p -> productId.equals(p.get("id")))
            .findFirst();
        
        if (targetProduct.isEmpty()) {
            return Collections.emptyList();
        }
        
        String targetCategory = (String) targetProduct.get().get("category");
        String targetBrand = (String) targetProduct.get().get("brand");
        
        // 基于类别和品牌查找相似产品
        List<Map<String, Object>> similarProducts = ALL_PRODUCTS.stream()
            .filter(p -> !productId.equals(p.get("id")))
            .filter(p -> targetCategory.equals(p.get("category")) || targetBrand.equals(p.get("brand")))
            .limit(limit)
            .toList();
        
        logger.info("Found {} similar products for product: {}", similarProducts.size(), productId);
        return similarProducts;
    }
    
    /**
     * 个性化推荐的降级方法
     */
    public CompletableFuture<List<Map<String, Object>>> fallbackRecommendations(Long userId, String category, Exception ex) {
        logger.warn("Personalized recommendations failed for user: {}, falling back to popular products. Error: {}", 
                   userId, ex.getMessage());
        return getPopularProducts(category, 3);
    }
    
    /**
     * 热门商品的降级方法
     */
    public CompletableFuture<List<Map<String, Object>>> fallbackPopularProducts(String category, int limit, Exception ex) {
        logger.warn("Popular products service failed, using static fallback. Error: {}", ex.getMessage());
        
        return CompletableFuture.supplyAsync(() -> {
            // 静态降级数据
            List<Map<String, Object>> fallbackProducts = Arrays.asList(
                createProduct(1L, "iPhone 15 Pro", "Apple", 8999.00, "smartphone"),
                createProduct(2L, "MacBook Pro M3", "Apple", 15999.00, "laptop"),
                createProduct(5L, "iPad Air", "Apple", 4299.00, "tablet")
            );
            
            return fallbackProducts.stream()
                .filter(product -> category == null || category.equals(product.get("category")))
                .limit(limit)
                .toList();
        });
    }
    
    /**
     * 相似商品的降级方法
     */
    public List<Map<String, Object>> fallbackSimilarProducts(Long productId, int limit, Exception ex) {
        logger.warn("Similar products service failed for product: {}, using random fallback. Error: {}", 
                   productId, ex.getMessage());
        
        // 随机返回一些产品作为降级
        Collections.shuffle(ALL_PRODUCTS.subList(0, Math.min(ALL_PRODUCTS.size(), limit)));
        return ALL_PRODUCTS.subList(0, Math.min(ALL_PRODUCTS.size(), limit));
    }
    
    /**
     * 获取推荐服务统计信息
     */
    public Map<String, Object> getRecommendationStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCalls", callCount);
        stats.put("simulatedFailureRate", "~30%");
        stats.put("availableProducts", ALL_PRODUCTS.size());
        stats.put("supportedCategories", Arrays.asList("smartphone", "laptop", "tablet", "accessory"));
        stats.put("timestamp", System.currentTimeMillis());
        return stats;
    }
    
    private void simulateNetworkDelay() {
        try {
            // 模拟网络延迟 0.5-2秒
            Thread.sleep(500 + random.nextInt(1500));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void simulateFailureScenarios() {
        int scenario = callCount % 10;
        
        switch (scenario) {
            case 1, 2: // 20% 超时异常
                throw new RuntimeException("Recommendation service timeout");
            case 3: // 10% 服务不可用
                throw new RuntimeException("Recommendation service temporarily unavailable");
            case 4: // 10% 算法异常
                throw new RuntimeException("Recommendation algorithm failed");
            default:
                // 60% 正常执行
                break;
        }
    }
    
    private List<Map<String, Object>> generateRecommendations(Long userId, String category, int limit) {
        // 基于用户ID的简单推荐算法
        List<Map<String, Object>> candidates = ALL_PRODUCTS.stream()
            .filter(product -> category == null || category.equals(product.get("category")))
            .toList();
        
        // 基于用户ID进行个性化排序
        Collections.shuffle(candidates, new Random(userId));
        
        return candidates.stream()
            .limit(limit)
            .toList();
    }
    
    private static Map<String, Object> createProduct(Long id, String name, String brand, Double price, String category) {
        Map<String, Object> product = new HashMap<>();
        product.put("id", id);
        product.put("name", name);
        product.put("brand", brand);
        product.put("price", price);
        product.put("category", category);
        product.put("available", true);
        product.put("rating", 4.0 + new Random().nextDouble());
        product.put("reviewCount", new Random().nextInt(1000) + 100);
        return product;
    }
}