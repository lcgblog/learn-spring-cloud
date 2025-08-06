package com.shophub.notification.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shophub.notification.entity.Notification;
import com.shophub.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;
import java.util.Map;

/**
 * 事件监听器
 * 监听来自RabbitMQ的各种业务事件并创建相应的通知
 * 使用Spring Cloud Stream函数式编程模型
 * 
 * @author ShopHub Team
 * @since Week 8
 */
@Component
public class EventListener {

    private static final Logger logger = LoggerFactory.getLogger(EventListener.class);

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 监听订单事件
     */
    @Bean
    public Consumer<Message<String>> orderEvents() {
        return message -> handleOrderEvent(message.getPayload(), message.getHeaders());
    }
    
    private void handleOrderEvent(String eventData, Map<String, Object> headers) {
        try {
            logger.info("Received order event: {}", eventData);
            
            JsonNode eventNode = objectMapper.readTree(eventData);
            String eventType = eventNode.path("eventType").asText();
            Long userId = eventNode.path("userId").asLong();
            Long orderId = eventNode.path("orderId").asLong();
            
            String title = "订单通知";
            String message = "";
            
            switch (eventType) {
                case "ORDER_CREATED":
                    message = String.format("您的订单 #%d 已创建成功", orderId);
                    break;
                case "ORDER_CONFIRMED":
                    message = String.format("您的订单 #%d 已确认", orderId);
                    break;
                case "ORDER_SHIPPED":
                    message = String.format("您的订单 #%d 已发货", orderId);
                    break;
                case "ORDER_DELIVERED":
                    message = String.format("您的订单 #%d 已送达", orderId);
                    break;
                case "ORDER_CANCELLED":
                    message = String.format("您的订单 #%d 已取消", orderId);
                    break;
                default:
                    message = String.format("订单 #%d 状态更新", orderId);
            }
            
            notificationService.createNotification(
                userId, title, message, 
                Notification.NotificationType.ORDER, 
                eventType, eventData
            );
            
        } catch (Exception e) {
            logger.error("Error processing order event: {}", eventData, e);
        }
    }

    /**
     * 监听支付事件
     */
    @Bean
    public Consumer<Message<String>> paymentEvents() {
        return message -> handlePaymentEvent(message.getPayload(), message.getHeaders());
    }
    
    private void handlePaymentEvent(String eventData, Map<String, Object> headers) {
        try {
            logger.info("Received payment event: {}", eventData);
            
            JsonNode eventNode = objectMapper.readTree(eventData);
            String eventType = eventNode.path("eventType").asText();
            Long userId = eventNode.path("userId").asLong();
            String paymentId = eventNode.path("paymentId").asText();
            Double amount = eventNode.path("amount").asDouble();
            
            String title = "支付通知";
            String message = "";
            
            switch (eventType) {
                case "PAYMENT_INITIATED":
                    message = String.format("支付 %s 已发起，金额：%.2f", paymentId, amount);
                    break;
                case "PAYMENT_COMPLETED":
                    message = String.format("支付 %s 已完成，金额：%.2f", paymentId, amount);
                    break;
                case "PAYMENT_FAILED":
                    message = String.format("支付 %s 失败，金额：%.2f", paymentId, amount);
                    break;
                case "PAYMENT_REFUNDED":
                    message = String.format("支付 %s 已退款，金额：%.2f", paymentId, amount);
                    break;
                default:
                    message = String.format("支付 %s 状态更新", paymentId);
            }
            
            notificationService.createNotification(
                userId, title, message, 
                Notification.NotificationType.PAYMENT, 
                eventType, eventData
            );
            
        } catch (Exception e) {
            logger.error("Error processing payment event: {}", eventData, e);
        }
    }

    /**
     * 监听产品事件
     */
    @Bean
    public Consumer<Message<String>> productEvents() {
        return message -> handleProductEvent(message.getPayload(), message.getHeaders());
    }
    
    private void handleProductEvent(String eventData, Map<String, Object> headers) {
        try {
            logger.info("Received product event: {}", eventData);
            
            JsonNode eventNode = objectMapper.readTree(eventData);
            String eventType = eventNode.path("eventType").asText();
            Long productId = eventNode.path("productId").asLong();
            String productName = eventNode.path("productName").asText();
            
            String title = "商品通知";
            String message = "";
            
            switch (eventType) {
                case "PRODUCT_CREATED":
                    message = String.format("新商品 '%s' 已上架", productName);
                    break;
                case "PRODUCT_UPDATED":
                    message = String.format("商品 '%s' 信息已更新", productName);
                    break;
                case "PRODUCT_DELETED":
                    message = String.format("商品 '%s' 已下架", productName);
                    break;
                case "INVENTORY_LOW":
                    Integer stock = eventNode.path("stock").asInt();
                    message = String.format("商品 '%s' 库存不足，剩余：%d", productName, stock);
                    break;
                case "INVENTORY_OUT":
                    message = String.format("商品 '%s' 已售罄", productName);
                    break;
                default:
                    message = String.format("商品 '%s' 状态更新", productName);
            }
            
            // 对于库存相关事件，通知管理员
            Long userId = eventType.startsWith("INVENTORY") ? 1L : 
                         eventNode.path("userId").asLong(1L);
            
            notificationService.createNotification(
                userId, title, message, 
                Notification.NotificationType.INVENTORY, 
                eventType, eventData
            );
            
        } catch (Exception e) {
            logger.error("Error processing product event: {}", eventData, e);
        }
    }

    /**
     * 监听用户事件
     */
    @Bean
    public Consumer<Message<String>> userEvents() {
        return message -> handleUserEvent(message.getPayload(), message.getHeaders());
    }
    
    private void handleUserEvent(String eventData, Map<String, Object> headers) {
        try {
            logger.info("Received user event: {}", eventData);
            
            JsonNode eventNode = objectMapper.readTree(eventData);
            String eventType = eventNode.path("eventType").asText();
            Long userId = eventNode.path("userId").asLong();
            String username = eventNode.path("username").asText();
            
            String title = "账户通知";
            String message = "";
            
            switch (eventType) {
                case "USER_REGISTERED":
                    message = String.format("欢迎 %s 加入ShopHub！", username);
                    break;
                case "USER_PROFILE_UPDATED":
                    message = "您的个人资料已更新";
                    break;
                case "USER_PASSWORD_CHANGED":
                    message = "您的密码已成功修改";
                    break;
                case "USER_LOGIN":
                    message = String.format("欢迎回来，%s！", username);
                    break;
                default:
                    message = "账户状态更新";
            }
            
            notificationService.createNotification(
                userId, title, message, 
                Notification.NotificationType.USER, 
                eventType, eventData
            );
            
        } catch (Exception e) {
            logger.error("Error processing user event: {}", eventData, e);
        }
    }

    /**
     * 监听系统事件
     */
    @Bean
    public Consumer<Message<String>> systemEvents() {
        return message -> handleSystemEvent(message.getPayload(), message.getHeaders());
    }
    
    private void handleSystemEvent(String eventData, Map<String, Object> headers) {
        try {
            logger.info("Received system event: {}", eventData);
            
            JsonNode eventNode = objectMapper.readTree(eventData);
            String eventType = eventNode.path("eventType").asText();
            String message = eventNode.path("message").asText();
            
            String title = "系统通知";
            
            // 系统通知通常发送给所有用户或管理员
            Long userId = 1L; // 默认发送给管理员
            
            notificationService.createNotification(
                userId, title, message, 
                Notification.NotificationType.SYSTEM, 
                eventType, eventData
            );
            
        } catch (Exception e) {
            logger.error("Error processing system event: {}", eventData, e);
        }
    }
}