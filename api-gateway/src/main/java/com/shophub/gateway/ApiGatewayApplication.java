package com.shophub.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * ShopHub API Gateway 应用程序
 * 
 * 功能特性:
 * - 统一入口点，路由到各个微服务
 * - 服务发现集成 (Eureka)
 * - 请求限流和负载均衡
 * - 跨域支持和安全过滤
 * - 请求/响应日志记录
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}