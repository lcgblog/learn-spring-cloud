package com.shophub.payment.service.external;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

/**
 * 模拟PayPal支付网关
 * 作为Stripe的备用网关，展示降级策略
 */
@Service
public class PaypalPaymentGateway {
    
    private final Random random = new Random();
    private int callCount = 0;
    
    /**
     * 处理支付 - 相比Stripe更稳定，但处理时间更长
     */
    public PaymentResult processPayment(Long orderId, BigDecimal amount, String currency) {
        callCount++;
        
        // 模拟较长的处理时间
        try {
            Thread.sleep(random.nextInt(1500) + 1000); // 1-2.5秒延迟
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // PayPal更稳定，90%成功率
        if (random.nextInt(10) < 9) {
            String transactionId = "paypal_" + UUID.randomUUID().toString().substring(0, 8);
            return new PaymentResult(true, transactionId, "PayPal payment successful");
        } else {
            return new PaymentResult(false, null, "PayPal payment failed");
        }
    }
    
    /**
     * 查询支付状态
     */
    public PaymentStatusResult queryPaymentStatus(String transactionId) {
        try {
            Thread.sleep(random.nextInt(500) + 300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return new PaymentStatusResult("COMPLETED", "Payment completed via PayPal");
    }
    
    /**
     * 获取网关状态信息
     */
    public String getGatewayStatus() {
        return String.format("PayPal Gateway - Calls: %d, Simulated failure rate: ~10%%", callCount);
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
}