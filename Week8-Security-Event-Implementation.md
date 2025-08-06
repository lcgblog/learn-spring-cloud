# Week 8: Security and Event-Driven Architecture Implementation

## 概述

Week 8 实现了 OAuth2 安全认证和事件驱动架构，为 ShopHub 微服务平台添加了企业级的安全保护和实时事件处理能力。

## 实现的功能

### 1. OAuth2 授权服务器 (Authorization Server)

#### 核心特性
- **JWT 令牌生成**: 使用 RSA 密钥对生成和验证 JWT 令牌
- **客户端认证**: 支持 client_credentials 授权流程
- **PKCE 支持**: 增强的安全性，防止授权码拦截攻击
- **OpenID Connect**: 完整的 OIDC 支持，包括用户信息端点
- **令牌内省**: 支持令牌验证和状态查询
- **JWKS 端点**: 提供公钥集合用于令牌验证

#### 技术实现
```java
// 主要依赖
- Spring Authorization Server 1.2.0
- Spring Security 6.2.0
- JWT 令牌处理
- RSA 密钥对生成
- H2 内存数据库
```

#### 关键端点
- **授权端点**: `/oauth2/authorize`
- **令牌端点**: `/oauth2/token`
- **JWKS 端点**: `/.well-known/jwks.json`
- **OpenID 配置**: `/.well-known/openid_configuration`
- **用户信息**: `/userinfo`
- **令牌内省**: `/oauth2/introspect`

### 2. 事件驱动通知服务 (Notification Service)

#### 核心特性
- **RabbitMQ 集成**: 使用 Spring Cloud Stream 进行消息处理
- **多事件类型**: 支持订单、支付、产品、用户和系统事件
- **实时通知**: 异步事件处理和通知生成
- **通知管理**: 创建、查询、标记已读、删除通知
- **死信队列**: 失败消息的重试和错误处理
- **OAuth2 保护**: 所有 API 端点都受 JWT 令牌保护

#### 事件类型
```yaml
# 支持的事件绑定
order-events:    # 订单相关事件
payment-events:  # 支付相关事件
product-events:  # 产品相关事件
user-events:     # 用户相关事件
system-events:   # 系统相关事件
```

#### 通知功能
- **用户通知**: 个人相关的业务通知
- **系统通知**: 全局系统消息和公告
- **实时处理**: 事件触发即时通知生成
- **批量操作**: 支持批量标记已读和删除
- **统计信息**: 通知数量和状态统计

### 3. 安全集成

#### 资源服务器保护
所有微服务都配置为 OAuth2 资源服务器：
- **JWT 令牌验证**: 统一的令牌验证机制
- **权限控制**: 基于角色和权限的访问控制
- **安全过滤器**: 自定义安全过滤器链
- **CORS 配置**: 跨域资源共享配置

#### 安全配置示例
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/notifications/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .build();
    }
}
```

### 4. 消息队列架构

#### RabbitMQ 配置
```yaml
# Docker Compose 配置
rabbitmq:
  image: rabbitmq:3-management-alpine
  ports:
    - "5672:5672"    # AMQP 端口
    - "15672:15672"  # 管理界面
  environment:
    - RABBITMQ_DEFAULT_USER=guest
    - RABBITMQ_DEFAULT_PASS=guest
```

#### 事件流处理
```java
// 事件监听器示例
@EventListener
public void handleOrderEvent(OrderEvent event) {
    Notification notification = Notification.builder()
        .userId(event.getUserId())
        .title("订单更新")
        .message("您的订单 #" + event.getOrderId() + " 状态已更新")
        .type(NotificationType.ORDER)
        .eventType(event.getEventType())
        .eventData(objectMapper.writeValueAsString(event))
        .build();
    
    notificationService.createNotification(notification);
}
```

## 架构图

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   API Gateway   │    │ Authorization   │    │   RabbitMQ      │
│   (OAuth2)      │◄──►│    Server       │    │   Message       │
│   Port: 8080    │    │   Port: 8090    │    │   Broker        │
└─────────────────┘    └─────────────────┘    │   Port: 5672    │
         │                       │             └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  User Service   │    │ Product Service │    │ Notification    │
│  (Protected)    │    │  (Protected)    │    │   Service       │
│  Port: 8081     │    │  Port: 8082     │    │  Port: 8089     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                                 ▼
                    ┌─────────────────┐
                    │  Order Service  │
                    │  (Protected)    │
                    │  Port: 8083     │
                    └─────────────────┘
```

## 部署配置

### 1. Docker Compose 更新

新增服务：
- **RabbitMQ**: 消息代理服务
- **Authorization Server**: OAuth2 授权服务器
- **Notification Service**: 事件驱动通知服务

### 2. 端口分配

| 服务 | 端口 | 描述 |
|------|------|------|
| Authorization Server | 8090 | OAuth2 授权服务器 |
| Notification Service | 8089 | 通知服务 |
| RabbitMQ AMQP | 5672 | 消息队列协议端口 |
| RabbitMQ Management | 15672 | Web 管理界面 |

### 3. 环境变量

```yaml
# Authorization Server
environment:
  - SPRING_PROFILES_ACTIVE=docker
  - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka/
  - SERVER_PORT=8090

# Notification Service
environment:
  - SPRING_PROFILES_ACTIVE=docker
  - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka/
  - SPRING_CLOUD_CONFIG_URI=http://config-server:8888
```

## 测试验证

### 1. OAuth2 令牌测试

```bash
# 运行 OAuth2 令牌测试脚本
./test-oauth2-tokens.sh

# 手动获取访问令牌
curl -X POST http://localhost:8090/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&client_id=shophub-client&client_secret=secret"
```

### 2. 事件驱动架构测试

```bash
# 运行完整的 Week8 测试脚本
./test-week8-security-events.sh

# 创建订单触发事件
curl -X POST http://localhost:8083/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {access_token}" \
  -d '{"userId":1,"productId":1,"quantity":2}'

# 查看通知
curl http://localhost:8089/api/notifications/user/1 \
  -H "Authorization: Bearer {access_token}"
```

### 3. 安全验证

```bash
# 测试未授权访问（应该失败）
curl http://localhost:8089/api/notifications/user/1

# 测试无效令牌（应该失败）
curl http://localhost:8089/api/notifications/user/1 \
  -H "Authorization: Bearer invalid-token"
```

## 监控和管理

### 1. RabbitMQ 管理界面
- **URL**: http://localhost:15672
- **用户名**: guest
- **密码**: guest
- **功能**: 队列监控、消息追踪、性能指标

### 2. 健康检查端点

```bash
# Authorization Server
curl http://localhost:8090/actuator/health

# Notification Service
curl http://localhost:8089/actuator/health

# OpenID Connect 配置
curl http://localhost:8090/.well-known/openid_configuration
```

### 3. 指标监控

所有服务都集成了 Prometheus 指标：
- **OAuth2 指标**: 令牌生成、验证统计
- **消息队列指标**: 消息处理速度、队列长度
- **通知指标**: 通知创建、发送统计
- **安全指标**: 认证成功/失败率

## 最佳实践

### 1. 安全配置
- **密钥管理**: 使用环境变量管理敏感信息
- **令牌过期**: 合理设置令牌过期时间
- **权限控制**: 实施最小权限原则
- **HTTPS**: 生产环境必须使用 HTTPS

### 2. 事件处理
- **幂等性**: 确保事件处理的幂等性
- **错误处理**: 实施重试和死信队列机制
- **监控**: 监控消息队列的健康状态
- **性能**: 合理配置消费者数量和批处理大小

### 3. 可观测性
- **日志**: 结构化日志记录
- **追踪**: 分布式追踪集成
- **指标**: 业务和技术指标监控
- **告警**: 关键指标的告警配置

## 总结

Week 8 成功实现了：

1. **企业级安全**: OAuth2 + JWT 的完整安全解决方案
2. **事件驱动架构**: 基于 RabbitMQ 的异步消息处理
3. **实时通知**: 业务事件触发的实时通知系统
4. **安全集成**: 所有微服务的统一安全保护
5. **可扩展性**: 支持新事件类型和通知渠道的扩展

这为 ShopHub 平台提供了生产级别的安全保护和事件处理能力，为后续的业务扩展奠定了坚实的基础。