package com.shophub.payment.controller;

import com.shophub.payment.entity.Payment;
import com.shophub.payment.service.PaymentService;
import com.shophub.payment.service.external.PaypalPaymentGateway;
import com.shophub.payment.service.external.StripePaymentGateway;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 支付控制器
 * 提供支付处理和监控端点
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private StripePaymentGateway stripeGateway;
    
    @Autowired
    private PaypalPaymentGateway paypalGateway;
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    /**
     * 处理支付请求
     */
    @PostMapping("/process")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> processPayment(
            @RequestBody PaymentRequest request) {
        logger.info("Received payment request for order: {}", request.getOrderId());
        
        return paymentService.processStripePayment(
                request.getOrderId(),
                request.getUserId(),
                request.getAmount(),
                request.getCurrency()
        ).thenApply(payment -> {
            Map<String, Object> response = new HashMap<>();
            response.put("paymentId", payment.getId());
            response.put("orderId", payment.getOrderId());
            response.put("status", payment.getStatus());
            response.put("gateway", payment.getGateway());
            response.put("transactionId", payment.getTransactionId());
            response.put("message", getStatusMessage(payment));
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        }).exceptionally(ex -> {
            logger.error("Payment processing failed: {}", ex.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Payment processing failed");
            errorResponse.put("message", ex.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(500).body(errorResponse);
        });
    }
    
    /**
     * 使用PayPal处理支付
     */
    @PostMapping("/process/paypal")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> processPaypalPayment(
            @RequestBody PaymentRequest request) {
        logger.info("Received PayPal payment request for order: {}", request.getOrderId());
        
        return paymentService.processPaypalPayment(
                request.getOrderId(),
                request.getUserId(),
                request.getAmount(),
                request.getCurrency()
        ).thenApply(payment -> {
            Map<String, Object> response = new HashMap<>();
            response.put("paymentId", payment.getId());
            response.put("orderId", payment.getOrderId());
            response.put("status", payment.getStatus());
            response.put("gateway", payment.getGateway());
            response.put("transactionId", payment.getTransactionId());
            response.put("message", getStatusMessage(payment));
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        });
    }
    
    /**
     * 查询支付状态
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<Map<String, Object>> getPaymentByOrderId(@PathVariable Long orderId) {
        Optional<Payment> paymentOpt = paymentService.getPaymentByOrderId(orderId);
        
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("paymentId", payment.getId());
            response.put("orderId", payment.getOrderId());
            response.put("userId", payment.getUserId());
            response.put("amount", payment.getAmount());
            response.put("currency", payment.getCurrency());
            response.put("status", payment.getStatus());
            response.put("method", payment.getMethod());
            response.put("gateway", payment.getGateway());
            response.put("transactionId", payment.getTransactionId());
            response.put("createdAt", payment.getCreatedAt());
            response.put("updatedAt", payment.getUpdatedAt());
            response.put("message", getStatusMessage(payment));
            
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 获取所有支付记录
     */
    @GetMapping("/all")
    public ResponseEntity<List<Payment>> getAllPayments() {
        List<Payment> payments = paymentService.getAllPayments();
        return ResponseEntity.ok(payments);
    }
    
    /**
     * 获取用户的支付记录
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Payment>> getPaymentsByUserId(@PathVariable Long userId) {
        List<Payment> payments = paymentService.getPaymentsByUserId(userId);
        return ResponseEntity.ok(payments);
    }
    
    /**
     * 获取支付网关统计信息
     */
    @GetMapping("/stats/{gateway}")
    public ResponseEntity<PaymentService.PaymentGatewayStats> getGatewayStats(@PathVariable String gateway) {
        PaymentService.PaymentGatewayStats stats = paymentService.getGatewayStats(gateway);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("service", "payment-service");
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("stripeGateway", stripeGateway.getGatewayStatus());
        health.put("paypalGateway", paypalGateway.getGatewayStatus());
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * 熔断器状态监控端点
     */
    @GetMapping("/circuit-breaker/status")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // Stripe支付熔断器状态
        CircuitBreaker stripePaymentCB = circuitBreakerRegistry.circuitBreaker("stripe-payment");
        status.put("stripePayment", getCircuitBreakerInfo(stripePaymentCB));
        
        // PayPal支付熔断器状态
        CircuitBreaker paypalPaymentCB = circuitBreakerRegistry.circuitBreaker("paypal-payment");
        status.put("paypalPayment", getCircuitBreakerInfo(paypalPaymentCB));
        
        // 支付查询熔断器状态
        CircuitBreaker paymentQueryCB = circuitBreakerRegistry.circuitBreaker("payment-query");
        status.put("paymentQuery", getCircuitBreakerInfo(paymentQueryCB));
        
        status.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * 演示熔断器效果的测试端点
     */
    @PostMapping("/demo/circuit-breaker")
    public ResponseEntity<Map<String, Object>> circuitBreakerDemo() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Circuit breaker demo - This will trigger multiple payment attempts");
        response.put("instruction", "Call this endpoint multiple times to see circuit breaker in action");
        response.put("timestamp", LocalDateTime.now());
        
        // 模拟支付请求来触发熔断器
        PaymentRequest demoRequest = new PaymentRequest();
        demoRequest.setOrderId(System.currentTimeMillis());
        demoRequest.setUserId(1L);
        demoRequest.setAmount(new BigDecimal("99.99"));
        demoRequest.setCurrency("USD");
        
        processPayment(demoRequest);
        
        return ResponseEntity.ok(response);
    }
    
    private String getStatusMessage(Payment payment) {
        switch (payment.getStatus()) {
            case COMPLETED:
                return "Payment completed successfully";
            case FAILED:
                return "Payment failed: " + (payment.getFailureReason() != null ? 
                        payment.getFailureReason() : "Unknown error");
            case PROCESSING:
                return "Payment is being processed";
            case PENDING:
                return "Payment is pending";
            case CANCELLED:
                return "Payment was cancelled";
            case REFUNDED:
                return "Payment was refunded";
            default:
                return "Unknown payment status";
        }
    }
    
    private Map<String, Object> getCircuitBreakerInfo(CircuitBreaker circuitBreaker) {
        Map<String, Object> info = new HashMap<>();
        info.put("state", circuitBreaker.getState().toString());
        info.put("failureRate", circuitBreaker.getMetrics().getFailureRate());
        info.put("numberOfCalls", circuitBreaker.getMetrics().getNumberOfBufferedCalls());
        info.put("numberOfFailedCalls", circuitBreaker.getMetrics().getNumberOfFailedCalls());
        info.put("numberOfSuccessfulCalls", circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
        return info;
    }
    
    /**
     * 支付请求DTO
     */
    public static class PaymentRequest {
        private Long orderId;
        private Long userId;
        private BigDecimal amount;
        private String currency = "USD";
        
        // Getters and Setters
        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
        
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
    }
}