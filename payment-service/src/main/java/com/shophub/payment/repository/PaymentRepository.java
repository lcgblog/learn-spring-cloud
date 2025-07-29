package com.shophub.payment.repository;

import com.shophub.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 支付Repository
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    /**
     * 根据订单ID查找支付记录
     */
    Optional<Payment> findByOrderId(Long orderId);
    
    /**
     * 根据用户ID查找支付记录
     */
    List<Payment> findByUserId(Long userId);
    
    /**
     * 根据支付状态查找记录
     */
    List<Payment> findByStatus(Payment.PaymentStatus status);
    
    /**
     * 根据交易ID查找支付记录
     */
    Optional<Payment> findByTransactionId(String transactionId);
    
    /**
     * 统计某个网关的支付成功率
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.gateway = :gateway AND p.status = 'COMPLETED'")
    Long countSuccessfulPaymentsByGateway(@Param("gateway") String gateway);
    
    /**
     * 统计某个网关的总支付次数
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.gateway = :gateway")
    Long countTotalPaymentsByGateway(@Param("gateway") String gateway);
}