package com.shophub.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ProductServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
        System.out.println("=================================");
        System.out.println("ShopHub Product Service 已启动!");
        System.out.println("产品服务: http://localhost:8082");
        System.out.println("=================================");
    }
} 