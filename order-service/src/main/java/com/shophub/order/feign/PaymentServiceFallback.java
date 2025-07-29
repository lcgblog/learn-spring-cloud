package com.shophub.order.feign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Payment Service Fallback
 * 支付服务降级处理
 */
@Component
public class PaymentServiceFallback implements PaymentServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceFallback.class);
    
    @Override
    public ResponseEntity<Map<String, Object>> processPayment(PaymentRequest request) {
        logger.warn("Payment service unavailable, using fallback for order: {}", request.getOrderId());
        
        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("paymentId", -1L);
        fallbackResponse.put("orderId", request.getOrderId());
        fallbackResponse.put("status", "PENDING");
        fallbackResponse.put("gateway", "fallback");
        fallbackResponse.put("transactionId", null);
        fallbackResponse.put("message", "Payment service unavailable - payment queued for retry");
        fallbackResponse.put("fallback", true);
        fallbackResponse.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(fallbackResponse);
    }
    
    @Override
    public ResponseEntity<Map<String, Object>> processPaypalPayment(PaymentRequest request) {
        logger.warn("PayPal payment service unavailable, using fallback for order: {}", request.getOrderId());
        
        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("paymentId", -1L);
        fallbackResponse.put("orderId", request.getOrderId());
        fallbackResponse.put("status", "PENDING");
        fallbackResponse.put("gateway", "paypal-fallback");
        fallbackResponse.put("transactionId", null);
        fallbackResponse.put("message", "PayPal service unavailable - payment queued for retry");
        fallbackResponse.put("fallback", true);
        fallbackResponse.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(fallbackResponse);
    }
}