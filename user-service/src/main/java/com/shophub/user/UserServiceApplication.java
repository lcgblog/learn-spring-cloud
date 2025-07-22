package com.shophub.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * ShopHub User Service
 * 
 * 负责用户管理和身份认证的微服务
 * 功能包括：用户注册、登录、个人信息管理
 * 
 * 服务运行在端口 8081
 * 自动注册到 Eureka Server
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
        System.out.println("=================================");
        System.out.println("ShopHub User Service 已启动!");
        System.out.println("用户服务: http://localhost:8081");
        System.out.println("=================================");
    }
} 