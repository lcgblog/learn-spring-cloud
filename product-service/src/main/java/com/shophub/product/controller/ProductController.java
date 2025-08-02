package com.shophub.product.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.shophub.product.service.ProductRecommendationService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.annotation.NewSpan;
import io.micrometer.tracing.annotation.SpanTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
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
 * 支持与其他微服务的通信和配置动态刷新
 * Week 6: 添加熔断器和韧性模式支持
 * Week 7: 添加分布式追踪和可观测性支持
 */
@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
@RefreshScope
public class ProductController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    
    @Value("${server.port}")
    private String serverPort;
    
    @Value("${spring.application.name}")
    private String serviceName;
    
    // Feature toggles from Config Server
    @Value("${feature.recommendations.enabled:false}")
    private boolean recommendationsEnabled;
    
    @Value("${feature.realtime-inventory.enabled:false}")
    private boolean realtimeInventoryEnabled;
    
    @Value("${feature.multi-currency.enabled:false}")
    private boolean multiCurrencyEnabled;
    
    @Value("${product.search.max-results:100}")
    private int maxSearchResults;
    
    @Value("${product.catalog.default-category:electronics}")
    private String defaultCategory;
    
    @Autowired
    private ProductRecommendationService recommendationService;
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    private final Counter requestCounter;
    private final Counter errorCounter;

    public ProductController(MeterRegistry meterRegistry) {
        this.requestCounter = Counter.builder("product.requests.total")
                .description("Total product service requests")
                .register(meterRegistry);
        this.errorCounter = Counter.builder("product.errors.total")
                .description("Total product service errors")
                .register(meterRegistry);
    }
    
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
    @NewSpan("product-exists-check")
    @Timed(value = "product.exists.check", description = "Time taken to check if product exists")
    public ResponseEntity<Map<String, Object>> checkProductExists(@SpanTag("productId") @PathVariable Long productId) {
        requestCounter.increment();
        logger.info("Checking if product exists: {}", productId);
        
        boolean exists = MOCK_PRODUCTS.stream()
            .anyMatch(product -> productId.equals(product.get("id")));
        
        Map<String, Object> response = new HashMap<>();
        response.put("exists", exists);
        response.put("message", exists ? "产品ID " + productId + " 存在" : "产品ID " + productId + " 不存在");
        response.put("serviceInstance", serviceName + ":" + serverPort);
        response.put("timestamp", System.currentTimeMillis());
        
        logger.info("Product {} exists check result: {}", productId, exists);
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
    @NewSpan("product-health-check")
    @Timed(value = "product.health.check", description = "Time taken for product service health check")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        requestCounter.increment();
        logger.info("Product service health check requested");
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", serviceName);
        health.put("port", serverPort);
        health.put("productsCount", MOCK_PRODUCTS.size());
        health.put("timestamp", System.currentTimeMillis());
        health.put("message", "Product Service 实例运行正常，端口: " + serverPort);
        
        logger.info("Product service health check completed: UP");
        return ResponseEntity.ok(health);
    }
    
    /**
     * 获取功能开关配置
     * GET /api/products/features
     */
    @GetMapping("/features")
    public ResponseEntity<Map<String, Object>> getFeatureToggles() {
        Map<String, Object> features = new HashMap<>();
        features.put("recommendationsEnabled", recommendationsEnabled);
        features.put("realtimeInventoryEnabled", realtimeInventoryEnabled);
        features.put("multiCurrencyEnabled", multiCurrencyEnabled);
        features.put("maxSearchResults", maxSearchResults);
        features.put("defaultCategory", defaultCategory);
        features.put("serviceInstance", serviceName + ":" + serverPort);
        features.put("message", "当前产品服务功能开关状态");
        
        return ResponseEntity.ok(features);
    }
    
    /**
     * 获取产品推荐 (基于功能开关，使用熔断器保护)
     * GET /api/products/recommendations
     */
    @GetMapping("/recommendations")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getRecommendations(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(required = false) String category) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (!recommendationsEnabled) {
            response.put("enabled", false);
            response.put("message", "产品推荐功能已关闭");
            response.put("serviceInstance", serviceName + ":" + serverPort);
            return CompletableFuture.completedFuture(ResponseEntity.ok(response));
        }
        
        // 使用推荐服务（包含熔断器保护）
        return recommendationService.getPersonalizedRecommendations(userId, category)
            .thenApply(recommendations -> {
                response.put("enabled", true);
                response.put("recommendations", recommendations);
                response.put("algorithm", "personalized-with-circuit-breaker");
                response.put("userId", userId);
                response.put("category", category);
                response.put("serviceInstance", serviceName + ":" + serverPort);
                response.put("message", "基于用户偏好的个性化推荐（熔断器保护）");
                response.put("count", recommendations.size());
                
                return ResponseEntity.ok(response);
            })
            .exceptionally(ex -> {
                response.put("enabled", true);
                response.put("error", "推荐服务异常");
                response.put("message", ex.getMessage());
                response.put("serviceInstance", serviceName + ":" + serverPort);
                response.put("fallback", "使用默认推荐");
                
                // 降级到简单推荐
                List<Map<String, Object>> fallbackRecommendations = MOCK_PRODUCTS.stream()
                    .filter(product -> (Boolean) product.get("available"))
                    .limit(3)
                    .toList();
                response.put("recommendations", fallbackRecommendations);
                
                return ResponseEntity.ok(response);
            });
    }
    
    /**
     * 获取热门商品推荐
     * GET /api/products/popular
     */
    @GetMapping("/popular")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getPopularProducts(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "5") int limit) {
        
        return recommendationService.getPopularProducts(category, limit)
            .thenApply(popularProducts -> {
                Map<String, Object> response = new HashMap<>();
                response.put("popular", popularProducts);
                response.put("category", category);
                response.put("limit", limit);
                response.put("count", popularProducts.size());
                response.put("serviceInstance", serviceName + ":" + serverPort);
                response.put("message", "热门商品推荐（熔断器保护）");
                
                return ResponseEntity.ok(response);
            })
            .exceptionally(ex -> {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "热门商品服务异常");
                errorResponse.put("message", ex.getMessage());
                errorResponse.put("serviceInstance", serviceName + ":" + serverPort);
                return ResponseEntity.status(500).body(errorResponse);
            });
    }
    
    /**
     * 获取相似商品推荐
     * GET /api/products/{productId}/similar
     */
    @GetMapping("/{productId}/similar")
    public ResponseEntity<Map<String, Object>> getSimilarProducts(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "4") int limit) {
        
        try {
            List<Map<String, Object>> similarProducts = recommendationService.getSimilarProducts(productId, limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("productId", productId);
            response.put("similar", similarProducts);
            response.put("limit", limit);
            response.put("count", similarProducts.size());
            response.put("serviceInstance", serviceName + ":" + serverPort);
            response.put("message", "相似商品推荐（熔断器保护）");
            
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("productId", productId);
            errorResponse.put("error", "相似商品服务异常");
            errorResponse.put("message", ex.getMessage());
            errorResponse.put("serviceInstance", serviceName + ":" + serverPort);
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 获取推荐服务统计信息
     * GET /api/products/recommendation-stats
     */
    @GetMapping("/recommendation-stats")
    public ResponseEntity<Map<String, Object>> getRecommendationStats() {
        Map<String, Object> stats = recommendationService.getRecommendationStats();
        stats.put("serviceInstance", serviceName + ":" + serverPort);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 获取熔断器状态监控
     * GET /api/products/circuit-breaker/status
     */
    @GetMapping("/circuit-breaker/status")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // 推荐服务熔断器状态
        CircuitBreaker recommendationCB = circuitBreakerRegistry.circuitBreaker("recommendation-service");
        status.put("recommendationService", getCircuitBreakerInfo(recommendationCB));
        
        // 热门商品熔断器状态
        CircuitBreaker popularProductsCB = circuitBreakerRegistry.circuitBreaker("popular-products");
        status.put("popularProducts", getCircuitBreakerInfo(popularProductsCB));
        
        // 相似商品熔断器状态
        CircuitBreaker similarProductsCB = circuitBreakerRegistry.circuitBreaker("similar-products");
        status.put("similarProducts", getCircuitBreakerInfo(similarProductsCB));
        
        status.put("serviceInstance", serviceName + ":" + serverPort);
        status.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * 获取实时库存 (基于功能开关)
     * GET /api/products/{productId}/inventory
     */
    @GetMapping("/{productId}/inventory")
    public ResponseEntity<Map<String, Object>> getInventory(@PathVariable Long productId) {
        Map<String, Object> response = new HashMap<>();
        
        // 检查产品是否存在
        boolean productExists = MOCK_PRODUCTS.stream()
            .anyMatch(product -> productId.equals(product.get("id")));
        
        if (!productExists) {
            response.put("error", "产品不存在");
            return ResponseEntity.notFound().build();
        }
        
        if (!realtimeInventoryEnabled) {
            response.put("enabled", false);
            response.put("message", "实时库存功能已关闭，使用缓存数据");
            response.put("inventory", 100); // 模拟缓存库存
            response.put("lastUpdated", System.currentTimeMillis() - 300000); // 5分钟前
        } else {
            response.put("enabled", true);
            response.put("message", "实时库存数据");
            response.put("inventory", (int)(Math.random() * 50) + 10); // 模拟实时库存
            response.put("lastUpdated", System.currentTimeMillis());
        }
        
        response.put("productId", productId);
        response.put("serviceInstance", serviceName + ":" + serverPort);
        
        return ResponseEntity.ok(response);
    }
    
    private Map<String, Object> getCircuitBreakerInfo(CircuitBreaker circuitBreaker) {
        Map<String, Object> info = new HashMap<>();
        info.put("state", circuitBreaker.getState().toString());
        info.put("failureRate", circuitBreaker.getMetrics().getFailureRate());
        info.put("numberOfBufferedCalls", circuitBreaker.getMetrics().getNumberOfBufferedCalls());
        info.put("numberOfFailedCalls", circuitBreaker.getMetrics().getNumberOfFailedCalls());
        info.put("numberOfSuccessfulCalls", circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
        info.put("numberOfSlowCalls", circuitBreaker.getMetrics().getNumberOfSlowCalls());
        return info;
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