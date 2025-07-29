package com.shophub.order.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Payment Service Feign Client
 * 用于订单服务调用支付服务
 */
@FeignClient(
    name = "payment-service",
    fallback = PaymentServiceFallback.class
)
public interface PaymentServiceClient {
    
    /**
     * 处理支付请求
     */
    @PostMapping("/api/payments/process")
    ResponseEntity<Map<String, Object>> processPayment(@RequestBody PaymentRequest request);
    
    /**
     * 使用PayPal处理支付
     */
    @PostMapping("/api/payments/process/paypal")
    ResponseEntity<Map<String, Object>> processPaypalPayment(@RequestBody PaymentRequest request);
    
    /**
     * 支付请求DTO
     */
    class PaymentRequest {
        private Long orderId;
        private Long userId;
        private BigDecimal amount;
        private String currency = "USD";
        
        public PaymentRequest() {}
        
        public PaymentRequest(Long orderId, Long userId, BigDecimal amount, String currency) {
            this.orderId = orderId;
            this.userId = userId;
            this.amount = amount;
            this.currency = currency;
        }
        
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