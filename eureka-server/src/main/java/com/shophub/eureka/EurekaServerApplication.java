package com.shophub.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * ShopHub Eureka Server
 * 
 * 这是 ShopHub 电商平台的服务发现注册中心
 * 所有微服务都将在此注册，实现服务发现和负载均衡
 * 
 * 服务运行在端口 8761
 * 访问地址: http://localhost:8761
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
        System.out.println("=================================");
        System.out.println("ShopHub Eureka Server 已启动!");
        System.out.println("服务发现中心: http://localhost:8761");
        System.out.println("=================================");
    }
} 