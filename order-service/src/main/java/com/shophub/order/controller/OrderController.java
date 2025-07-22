package com.shophub.order.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单控制器
 * 
 * 提供订单管理的 REST API 接口
 * 支持订单创建、查询等功能
 */
@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {
    
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
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Order Service is running! 当前有 " + MOCK_ORDERS.size() + " 个订单");
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