package com.shophub.order.controller;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.shophub.order.feign.PaymentServiceClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shophub.order.feign.ProductServiceClient;

/**
 * 订单控制器
 * 
 * 提供订单管理的 REST API 接口
 * 支持订单创建、查询等功能
 * Week 6: 添加支付服务调用和熔断器保护
 */
@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    @Autowired
    private ProductServiceClient productServiceClient;
    
    @Autowired
    private PaymentServiceClient paymentServiceClient;
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    // 模拟订单数据 (实际项目中应该连接数据库)
    private static final List<Map<String, Object>> MOCK_ORDERS = Arrays.asList(
        createOrder(1L, 1L, Arrays.asList(1L, 2L), "COMPLETED", 24998.00),
        createOrder(2L, 2L, Arrays.asList(3L), "PROCESSING", 6999.00),
        createOrder(3L, 1L, Arrays.asList(5L), "PENDING", 4299.00),
        createOrder(4L, 3L, Arrays.asList(1L, 5L), "SHIPPED", 13298.00)
    );
    
    /**
     * 获取所有订单
     * GET /api/orders
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllOrders() {
        return ResponseEntity.ok(MOCK_ORDERS);
    }
    
    /**
     * 根据订单ID获取订单详情
     * GET /api/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderById(@PathVariable Long orderId) {
        Map<String, Object> order = MOCK_ORDERS.stream()
            .filter(o -> orderId.equals(o.get("id")))
            .findFirst()
            .orElse(null);
        
        if (order != null) {
            return ResponseEntity.ok(order);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 根据用户ID获取订单列表
     * GET /api/orders/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getOrdersByUserId(@PathVariable Long userId) {
        List<Map<String, Object>> userOrders = MOCK_ORDERS.stream()
            .filter(order -> userId.equals(order.get("userId")))
            .toList();
        
        return ResponseEntity.ok(userOrders);
    }
    
    /**
     * 根据状态获取订单列表
     * GET /api/orders/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Map<String, Object>>> getOrdersByStatus(@PathVariable String status) {
        List<Map<String, Object>> statusOrders = MOCK_ORDERS.stream()
            .filter(order -> status.equalsIgnoreCase((String) order.get("status")))
            .toList();
        
        return ResponseEntity.ok(statusOrders);
    }
    
    /**
     * 创建新订单
     * POST /api/orders
     */
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> orderRequest) {
        try {
            // 模拟订单创建逻辑
            Long newOrderId = (long) (MOCK_ORDERS.size() + 1);
            Map<String, Object> newOrder = new HashMap<>();
            newOrder.put("id", newOrderId);
            newOrder.put("userId", orderRequest.get("userId"));
            newOrder.put("productIds", orderRequest.get("productIds"));
            newOrder.put("status", "PENDING");
            newOrder.put("totalAmount", orderRequest.get("totalAmount"));
            newOrder.put("createdAt", System.currentTimeMillis());
            
            return ResponseEntity.ok(Map.of(
                "message", "订单创建成功",
                "orderId", newOrderId,
                "order", newOrder
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("创建订单失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新订单状态
     * PUT /api/orders/{orderId}/status
     */
    @PutMapping("/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long orderId, @RequestBody Map<String, String> statusUpdate) {
        String newStatus = statusUpdate.get("status");
        
        // 模拟状态更新
        return ResponseEntity.ok(Map.of(
            "message", "订单状态更新成功",
            "orderId", orderId,
            "newStatus", newStatus,
            "updatedAt", System.currentTimeMillis()
        ));
    }
    
    /**
     * 获取订单统计信息
     * GET /api/orders/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getOrderStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", MOCK_ORDERS.size());
        stats.put("pendingOrders", MOCK_ORDERS.stream().filter(o -> "PENDING".equals(o.get("status"))).count());
        stats.put("completedOrders", MOCK_ORDERS.stream().filter(o -> "COMPLETED".equals(o.get("status"))).count());
        stats.put("totalRevenue", MOCK_ORDERS.stream()
            .mapToDouble(o -> (Double) o.get("totalAmount"))
            .sum());
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 健康检查
     * GET /api/orders/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "order-service");
        health.put("port", "8083");
        health.put("ordersCount", MOCK_ORDERS.size());
        health.put("timestamp", System.currentTimeMillis());
        health.put("message", "Order Service 运行正常");
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * 负载均衡演示 - 调用产品服务健康检查
     * GET /api/orders/load-balance-demo
     */
    @GetMapping("/load-balance-demo")
    public ResponseEntity<Map<String, Object>> loadBalanceDemo() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 调用产品服务 - 会通过负载均衡器路由到不同实例
            Map<String, Object> productHealth = productServiceClient.getProductServiceHealth();
            
            result.put("orderService", "order-service:8083");
            result.put("productServiceResponse", productHealth);
            result.put("timestamp", System.currentTimeMillis());
            result.put("message", "负载均衡调用成功");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("error", "负载均衡调用失败: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
    
    /**
     * 验证产品存在性演示
     * GET /api/orders/verify-product/{productId}
     */
    @GetMapping("/verify-product/{productId}")
    public ResponseEntity<Map<String, Object>> verifyProduct(@PathVariable Long productId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 通过Feign客户端调用产品服务
            Map<String, Object> productCheck = productServiceClient.checkProductExists(productId);
            
            result.put("orderService", "order-service:8083");
            result.put("productVerification", productCheck);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("error", "产品验证失败: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
    
    /**
     * 处理订单支付 - 使用熔断器保护
     * POST /api/orders/{orderId}/payment
     */
    @PostMapping("/{orderId}/payment")
    @CircuitBreaker(name = "payment-service", fallbackMethod = "fallbackPayment")
    @Retry(name = "payment-service")
    public ResponseEntity<Map<String, Object>> processOrderPayment(
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> paymentRequest) {
        
        logger.info("Processing payment for order: {}", orderId);
        
        try {
            // 查找订单
            Map<String, Object> order = MOCK_ORDERS.stream()
                .filter(o -> orderId.equals(o.get("id")))
                .findFirst()
                .orElse(null);
            
            if (order == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 准备支付请求
            PaymentServiceClient.PaymentRequest paymentReq = new PaymentServiceClient.PaymentRequest(
                orderId,
                (Long) order.get("userId"),
                BigDecimal.valueOf((Double) order.get("totalAmount")),
                "USD"
            );
            
            // 调用支付服务
            ResponseEntity<Map<String, Object>> paymentResponse = paymentServiceClient.processPayment(paymentReq);
            
            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orderId);
            response.put("orderAmount", order.get("totalAmount"));
            response.put("paymentResult", paymentResponse.getBody());
            response.put("message", "订单支付处理完成");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Payment processing failed for order: {}", orderId, e);
            throw e; // 让熔断器处理异常
        }
    }
    
    /**
     * 支付服务降级方法
     */
    public ResponseEntity<Map<String, Object>> fallbackPayment(Long orderId, Map<String, Object> paymentRequest, Exception ex) {
        logger.warn("Payment service failed for order: {}, using fallback. Error: {}", orderId, ex.getMessage());
        
        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("orderId", orderId);
        fallbackResponse.put("status", "PAYMENT_PENDING");
        fallbackResponse.put("message", "支付服务暂时不可用，订单已保存，稍后会自动重试支付");
        fallbackResponse.put("fallback", true);
        fallbackResponse.put("error", ex.getMessage());
        fallbackResponse.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(fallbackResponse);
    }
    
    /**
     * 获取熔断器状态监控
     * GET /api/orders/circuit-breaker/status
     */
    @GetMapping("/circuit-breaker/status")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // 支付服务熔断器状态
        io.github.resilience4j.circuitbreaker.CircuitBreaker paymentCB = circuitBreakerRegistry.circuitBreaker("payment-service");
        status.put("paymentService", getCircuitBreakerInfo(paymentCB));
        
        status.put("service", "order-service");
        status.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(status);
    }
    
    private Map<String, Object> getCircuitBreakerInfo(io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker) {
        Map<String, Object> info = new HashMap<>();
        info.put("state", circuitBreaker.getState().toString());
        info.put("failureRate", circuitBreaker.getMetrics().getFailureRate());
        info.put("numberOfBufferedCalls", circuitBreaker.getMetrics().getNumberOfBufferedCalls());
        info.put("numberOfFailedCalls", circuitBreaker.getMetrics().getNumberOfFailedCalls());
        info.put("numberOfSuccessfulCalls", circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
        return info;
    }
    
    /**
     * 创建模拟订单对象
     */
    private static Map<String, Object> createOrder(Long id, Long userId, List<Long> productIds, String status, Double totalAmount) {
        Map<String, Object> order = new HashMap<>();
        order.put("id", id);
        order.put("userId", userId);
        order.put("productIds", productIds);
        order.put("status", status);
        order.put("totalAmount", totalAmount);
        order.put("createdAt", System.currentTimeMillis() - (id * 86400000)); // 模拟不同的创建时间
        return order;
    }
} 