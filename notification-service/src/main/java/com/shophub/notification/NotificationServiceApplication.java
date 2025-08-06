package com.shophub.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Notification Service Application
 * 
 * 提供以下功能：
 * - 事件驱动的通知服务
 * - RabbitMQ 消息队列集成
 * - 用户通知管理
 * - 系统通知处理
 * - 实时事件处理
 * 
 * @author ShopHub Team
 * @since Week 8
 */
@SpringBootApplication
@EnableDiscoveryClient
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}