package com.shophub.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * OAuth2 Authorization Server Application
 * 
 * 提供以下功能：
 * - OAuth2 标准认证服务器
 * - JWT 令牌生成和验证
 * - 用户认证和授权
 * - 客户端管理
 * - PKCE 流程支持
 * 
 * @author ShopHub Team
 * @since Week 8
 */
@SpringBootApplication
@EnableDiscoveryClient
public class AuthorizationServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthorizationServerApplication.class, args);
    }
}