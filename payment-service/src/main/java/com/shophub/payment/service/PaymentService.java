package com.shophub.payment.service;

import com.shophub.payment.entity.Payment;
import com.shophub.payment.repository.PaymentRepository;
import com.shophub.payment.service.external.PaypalPaymentGateway;
import com.shophub.payment.service.external.StripePaymentGateway;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 支付服务
 * 实现熔断器、重试、超时控制和舱壁模式
 */
@Service
public class PaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private StripePaymentGateway stripeGateway;
    
    @Autowired
    private PaypalPaymentGateway paypalGateway;
    
    /**
     * 使用Stripe处理支付 - 主要支付网关
     * 包含熔断器、重试和超时控制
     */
    @CircuitBreaker(name = "stripe-payment", fallbackMethod = "fallbackStripePayment")
    @Retry(name = "stripe-payment")
    @TimeLimiter(name = "stripe-payment")
    @Bulkhead(name = "stripe-payment", type = Bulkhead.Type.THREADPOOL)
    public CompletableFuture<Payment> processStripePayment(Long orderId, Long userId, BigDecimal amount, String currency) {
        logger.info("Submitting Stripe payment for order: {}, amount: {}", orderId, amount);
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Processing Stripe payment for order: {}, amount: {}", orderId, amount);
            
            // 创建支付记录
            Payment payment = new Payment(orderId, userId, amount, Payment.PaymentMethod.STRIPE, "stripe");
            payment.setStatus(Payment.PaymentStatus.PROCESSING);
            payment = paymentRepository.save(payment);
            
            try {
                // 调用Stripe API
                StripePaymentGateway.PaymentResult result = stripeGateway.processPayment(orderId, amount, currency);
                
                if (result.isSuccess()) {
                    payment.setStatus(Payment.PaymentStatus.COMPLETED);
                    payment.setTransactionId(result.getTransactionId());
                    logger.info("Stripe payment successful: {}", result.getTransactionId());
                } else {
                    payment.setStatus(Payment.PaymentStatus.FAILED);
                    payment.setFailureReason(result.getMessage());
                    logger.warn("Stripe payment failed: {}", result.getMessage());
                }
                
            } catch (Exception e) {
                payment.setStatus(Payment.PaymentStatus.FAILED);
                payment.setFailureReason(e.getMessage());
                logger.error("Stripe payment error: {}", e.getMessage());
                throw e; // 重新抛出异常以触发重试
            }
            
            return paymentRepository.save(payment);
        });
    }
    
    /**
     * Stripe支付降级方法 - 自动切换到PayPal
     */
    public CompletableFuture<Payment> fallbackStripePayment(Long orderId, Long userId, 
                                                           BigDecimal amount, String currency, Exception ex) {
        logger.warn("Stripe payment failed, falling back to PayPal. Error: {}", ex.getMessage());
        return processPaypalPayment(orderId, userId, amount, currency);
    }
    
    /**
     * 使用PayPal处理支付 - 降级支付网关
     */
    @CircuitBreaker(name = "paypal-payment", fallbackMethod = "fallbackPaypalPayment")
    @Retry(name = "paypal-payment")
    @TimeLimiter(name = "paypal-payment")
    @Bulkhead(name = "paypal-payment", type = Bulkhead.Type.THREADPOOL)
    public CompletableFuture<Payment> processPaypalPayment(Long orderId, Long userId, BigDecimal amount, String currency) {
        logger.info("Submitting PayPal payment for order: {}, amount: {}", orderId, amount);
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Processing PayPal payment for order: {}, amount: {}", orderId, amount);
            
            // 创建支付记录
            Payment payment = new Payment(orderId, userId, amount, Payment.PaymentMethod.PAYPAL, "paypal");
            payment.setStatus(Payment.PaymentStatus.PROCESSING);
            payment = paymentRepository.save(payment);
            
            try {
                // 调用PayPal API
                PaypalPaymentGateway.PaymentResult result = paypalGateway.processPayment(orderId, amount, currency);
                
                if (result.isSuccess()) {
                    payment.setStatus(Payment.PaymentStatus.COMPLETED);
                    payment.setTransactionId(result.getTransactionId());
                    logger.info("PayPal payment successful: {}", result.getTransactionId());
                } else {
                    payment.setStatus(Payment.PaymentStatus.FAILED);
                    payment.setFailureReason(result.getMessage());
                    logger.warn("PayPal payment failed: {}", result.getMessage());
                }
                
            } catch (Exception e) {
                payment.setStatus(Payment.PaymentStatus.FAILED);
                payment.setFailureReason(e.getMessage());
                logger.error("PayPal payment error: {}", e.getMessage());
                throw e;
            }
            
            return paymentRepository.save(payment);
        });
    }
    
    /**
     * PayPal支付最终降级方法
     */
    public CompletableFuture<Payment> fallbackPaypalPayment(Long orderId, Long userId, 
                                                           BigDecimal amount, String currency, Exception ex) {
        logger.error("All payment gateways failed. Order: {}, Error: {}", orderId, ex.getMessage());
        
        return CompletableFuture.supplyAsync(() -> {
            Payment payment = new Payment(orderId, userId, amount, Payment.PaymentMethod.CREDIT_CARD, "fallback");
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason("All payment gateways unavailable: " + ex.getMessage());
            return paymentRepository.save(payment);
        });
    }
    
    /**
     * 查询支付状态 - 使用熔断器保护
     */
    @CircuitBreaker(name = "payment-query", fallbackMethod = "fallbackPaymentQuery")
    @Retry(name = "payment-query")
    public Optional<Payment> getPaymentByOrderId(Long orderId) {
        logger.info("Querying payment for order: {}", orderId);
        return paymentRepository.findByOrderId(orderId);
    }
    
    /**
     * 支付查询降级方法
     */
    public Optional<Payment> fallbackPaymentQuery(Long orderId, Exception ex) {
        logger.warn("Payment query failed for order: {}, using database fallback", orderId);
        // 降级到直接查询数据库
        try {
            return paymentRepository.findByOrderId(orderId);
        } catch (Exception e) {
            logger.error("Database query also failed: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * 获取所有支付记录
     */
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }
    
    /**
     * 根据用户ID获取支付记录
     */
    public List<Payment> getPaymentsByUserId(Long userId) {
        return paymentRepository.findByUserId(userId);
    }
    
    /**
     * 获取支付网关统计信息
     */
    public PaymentGatewayStats getGatewayStats(String gateway) {
        Long successful = paymentRepository.countSuccessfulPaymentsByGateway(gateway);
        Long total = paymentRepository.countTotalPaymentsByGateway(gateway);
        
        double successRate = total > 0 ? (successful.doubleValue() / total.doubleValue()) * 100 : 0.0;
        
        return new PaymentGatewayStats(gateway, total, successful, successRate);
    }
    
    /**
     * 支付网关统计信息
     */
    public static class PaymentGatewayStats {
        private final String gateway;
        private final Long totalPayments;
        private final Long successfulPayments;
        private final Double successRate;
        
        public PaymentGatewayStats(String gateway, Long totalPayments, Long successfulPayments, Double successRate) {
            this.gateway = gateway;
            this.totalPayments = totalPayments;
            this.successfulPayments = successfulPayments;
            this.successRate = successRate;
        }
        
        public String getGateway() { return gateway; }
        public Long getTotalPayments() { return totalPayments; }
        public Long getSuccessfulPayments() { return successfulPayments; }
        public Double getSuccessRate() { return successRate; }
    }
}