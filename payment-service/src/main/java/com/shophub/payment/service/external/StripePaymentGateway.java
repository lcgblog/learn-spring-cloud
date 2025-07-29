package com.shophub.payment.service.external;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

/**
 * 模拟Stripe支付网关
 * 用于演示熔断器和重试机制
 */
@Service
public class StripePaymentGateway {
    
    private final Random random = new Random();
    private int callCount = 0;
    
    /**
     * 处理支付 - 模拟网络延迟和故障
     */
    public PaymentResult processPayment(Long orderId, BigDecimal amount, String currency) {
        callCount++;
        
        // 模拟网络延迟
        try {
            Thread.sleep(random.nextInt(1000) + 500); // 0.5-1.5秒延迟
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 模拟不同的失败场景
        int scenario = callCount % 10;
        
        switch (scenario) {
            case 1, 2: // 20% 网络超时
                throw new PaymentTimeoutException("Stripe payment gateway timeout");
            case 3: // 10% 服务不可用
                throw new PaymentServiceUnavailableException("Stripe service temporarily unavailable");
            case 4: // 10% 支付被拒绝
                return new PaymentResult(false, null, "Payment declined by bank");
            default: // 60% 成功
                String transactionId = "stripe_" + UUID.randomUUID().toString().substring(0, 8);
                return new PaymentResult(true, transactionId, "Payment successful");
        }
    }
    
    /**
     * 查询支付状态
     */
    public PaymentStatusResult queryPaymentStatus(String transactionId) {
        // 模拟网络延迟
        try {
            Thread.sleep(random.nextInt(300) + 200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 模拟状态查询结果
        if (random.nextBoolean()) {
            return new PaymentStatusResult("COMPLETED", "Payment completed successfully");
        } else {
            return new PaymentStatusResult("PROCESSING", "Payment is being processed");
        }
    }
    
    /**
     * 获取网关状态信息
     */
    public String getGatewayStatus() {
        return String.format("Stripe Gateway - Calls: %d, Simulated failure rate: ~40%%", callCount);
    }
    
    // 支付结果类
    public static class PaymentResult {
        private final boolean success;
        private final String transactionId;
        private final String message;
        
        public PaymentResult(boolean success, String transactionId, String message) {
            this.success = success;
            this.transactionId = transactionId;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getTransactionId() { return transactionId; }
        public String getMessage() { return message; }
    }
    
    // 支付状态查询结果
    public static class PaymentStatusResult {
        private final String status;
        private final String message;
        
        public PaymentStatusResult(String status, String message) {
            this.status = status;
            this.message = message;
        }
        
        public String getStatus() { return status; }
        public String getMessage() { return message; }
    }
    
    // 自定义异常
    public static class PaymentTimeoutException extends RuntimeException {
        public PaymentTimeoutException(String message) {
            super(message);
        }
    }
    
    public static class PaymentServiceUnavailableException extends RuntimeException {
        public PaymentServiceUnavailableException(String message) {
            super(message);
        }
    }
}