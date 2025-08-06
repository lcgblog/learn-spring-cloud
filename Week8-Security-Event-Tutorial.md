# Week 8: Security & Event-Driven Architecture Tutorial

## 📚 学习路径

本周教程将引导您完成Spring Cloud微服务架构中的两个关键高级主题：

1. **企业级安全架构** - OAuth2 + JWT认证授权体系
2. **事件驱动架构** - 基于消息队列的异步处理模式

### 🎯 学习目标

完成本周学习后，您将能够：

- **安全架构设计**：实现OAuth2授权服务器和JWT令牌管理
- **权限控制**：构建基于角色的访问控制(RBAC)系统
- **事件驱动设计**：使用Spring Cloud Stream和RabbitMQ实现异步事件处理
- **实时通知**：构建事件驱动的通知系统
- **消息队列集成**：掌握RabbitMQ在微服务中的应用模式
- **安全集成**：在现有微服务中集成安全认证和授权
- **系统监控**：监控安全和事件处理的关键指标

## 🏗️ 架构深度解析

### 问题与解决方案

#### 安全挑战

**传统问题**：
- 每个微服务独立管理用户认证
- 服务间调用缺乏统一的安全机制
- 权限控制分散，难以统一管理
- 缺乏细粒度的访问控制

**Week 8解决方案**：
```
┌─────────────────────────────────────────────────────────────┐
│                    OAuth2 + JWT 安全架构                      │
├─────────────────────────────────────────────────────────────┤
│  Authorization Server (8090)                               │
│  ├── OAuth2 Authorization Server                           │
│  ├── JWT Token Generation & Validation                     │
│  ├── User Management (Customer/Vendor/Admin)               │
│  └── Client Registration & Management                      │
├─────────────────────────────────────────────────────────────┤
│  API Gateway (8080) - Security Gateway                     │
│  ├── JWT Token Validation                                  │
│  ├── Route-based Authorization                             │
│  ├── Token Propagation to Services                         │
│  └── Security Headers Management                           │
├─────────────────────────────────────────────────────────────┤
│  Microservices - Resource Servers                          │
│  ├── JWT Token Parsing                                     │
│  ├── Method-level Security (@PreAuthorize)                 │
│  ├── Role-based Access Control                             │
│  └── Service-to-Service Authentication                     │
└─────────────────────────────────────────────────────────────┘
```

#### 事件驱动挑战

**传统问题**：
- 服务间紧耦合的同步调用
- 业务流程复杂时的级联调用
- 系统可用性受单点故障影响
- 难以实现实时通知和异步处理

**Week 8解决方案**：
```
┌─────────────────────────────────────────────────────────────┐
│                   事件驱动架构 (Event-Driven)                  │
├─────────────────────────────────────────────────────────────┤
│  Event Publishers (业务服务)                                │
│  ├── Order Service → Order Events                          │
│  ├── Product Service → Inventory Events                    │
│  ├── User Service → User Events                            │
│  └── Payment Service → Payment Events                      │
├─────────────────────────────────────────────────────────────┤
│  Message Broker - RabbitMQ (5672)                          │
│  ├── order.events Queue                                    │
│  ├── inventory.events Queue                                │
│  ├── user.events Queue                                     │
│  ├── payment.events Queue                                  │
│  └── Dead Letter Queues (DLQ)                              │
├─────────────────────────────────────────────────────────────┤
│  Event Consumers                                            │
│  ├── Notification Service (8088)                           │
│  │   ├── User Notifications                                 │
│  │   ├── System Alerts                                      │
│  │   └── Real-time Updates                                 │
│  ├── Inventory Management                                   │
│  ├── Analytics & Reporting                                 │
│  └── External System Integration                           │
└─────────────────────────────────────────────────────────────┘
```

### 三层安全架构

#### 1. 认证层 (Authentication Layer)
```
OAuth2 Authorization Server
├── 用户认证 (Username/Password)
├── 客户端认证 (Client Credentials)
├── JWT令牌生成
└── 令牌刷新机制
```

#### 2. 授权层 (Authorization Layer)
```
API Gateway Security
├── JWT令牌验证
├── 路由级权限控制
├── 令牌传播
└── 安全头管理
```

#### 3. 资源保护层 (Resource Protection Layer)
```
Microservice Security
├── 方法级权限控制
├── 角色权限验证
├── 资源访问控制
└── 审计日志
```

### 事件驱动模式

#### 1. 事件发布模式
```
Business Operation → Event Publishing
├── 订单创建 → OrderCreatedEvent
├── 库存更新 → InventoryUpdatedEvent
├── 用户注册 → UserRegisteredEvent
└── 支付完成 → PaymentCompletedEvent
```

#### 2. 事件消费模式
```
Event Consumption → Business Logic
├── OrderCreatedEvent → 发送订单确认通知
├── InventoryUpdatedEvent → 库存预警检查
├── UserRegisteredEvent → 发送欢迎邮件
└── PaymentCompletedEvent → 更新订单状态
```

## 🛠️ 动手实现指南

### 第一步：理解现有系统

在开始Week 8之前，让我们回顾现有的微服务架构：

```bash
# 检查现有服务状态
curl http://localhost:8761/eureka/apps

# 当前服务列表：
# - Config Server (8888)
# - Eureka Server (8761)
# - API Gateway (8080)
# - User Service (8081)
# - Product Service (8082)
# - Order Service (8083)
# - Payment Service (8086)
# - Metrics Collector (8087)
```

**现有架构特点**：
- ✅ 服务发现和注册
- ✅ API网关和路由
- ✅ 负载均衡
- ✅ 配置管理
- ✅ 熔断器和韧性
- ✅ 分布式追踪和监控
- ❌ 统一安全认证
- ❌ 事件驱动架构

### 第二步：OAuth2安全架构设计

#### 2.1 OAuth2流程理解

**授权码流程 (Authorization Code Flow)**：
```
1. 客户端重定向用户到授权服务器
   GET /oauth2/authorize?response_type=code&client_id=api-gateway&redirect_uri=...

2. 用户登录并授权
   POST /login (username/password)

3. 授权服务器返回授权码
   HTTP 302 redirect_uri?code=AUTH_CODE

4. 客户端用授权码换取访问令牌
   POST /oauth2/token
   {
     "grant_type": "authorization_code",
     "code": "AUTH_CODE",
     "client_id": "api-gateway",
     "client_secret": "gateway-secret"
   }

5. 授权服务器返回JWT访问令牌
   {
     "access_token": "eyJhbGciOiJSUzI1NiIs...",
     "token_type": "Bearer",
     "expires_in": 3600
   }
```

#### 2.2 JWT令牌结构

**JWT Header**：
```json
{
  "alg": "RS256",
  "typ": "JWT"
}
```

**JWT Payload**：
```json
{
  "sub": "customer",
  "aud": ["api-gateway"],
  "scope": ["read", "write"],
  "authorities": ["ROLE_CUSTOMER"],
  "iss": "http://localhost:8090",
  "exp": 1640995200,
  "iat": 1640991600
}
```

**JWT Signature**：
```
RSA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), private_key)
```

#### 2.3 角色权限设计

**用户角色定义**：
```java
public enum UserRole {
    CUSTOMER("ROLE_CUSTOMER", Arrays.asList(
        "user:read", "order:create", "order:read"
    )),
    VENDOR("ROLE_VENDOR", Arrays.asList(
        "product:create", "product:update", "product:read",
        "inventory:update", "order:read"
    )),
    ADMIN("ROLE_ADMIN", Arrays.asList(
        "user:*", "product:*", "order:*", "payment:*", "system:*"
    ));
}
```

**权限控制示例**：
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER') and #id == authentication.principal.userId or hasRole('ADMIN')")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
    }
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getAllUsers() {
        return userService.findAll();
    }
}
```

### 第三步：事件驱动架构设计

#### 3.1 事件定义

**订单事件**：
```java
public class OrderEvent {
    private String eventType; // CREATED, UPDATED, CANCELLED
    private Long orderId;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private BigDecimal amount;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
}
```

**库存事件**：
```java
public class InventoryEvent {
    private String eventType; // UPDATED, LOW_STOCK, OUT_OF_STOCK
    private Long productId;
    private Integer currentStock;
    private Integer threshold;
    private LocalDateTime timestamp;
}
```

#### 3.2 Spring Cloud Stream配置

**事件发布者配置**：
```yaml
spring:
  cloud:
    stream:
      bindings:
        orderEvents-out-0:
          destination: order.events
          content-type: application/json
        inventoryEvents-out-0:
          destination: inventory.events
          content-type: application/json
      rabbit:
        bindings:
          orderEvents-out-0:
            producer:
              routing-key-expression: "'order.' + payload.eventType.toLowerCase()"
```

**事件消费者配置**：
```yaml
spring:
  cloud:
    stream:
      bindings:
        orderEvents-in-0:
          destination: order.events
          group: notification-service
          content-type: application/json
        inventoryEvents-in-0:
          destination: inventory.events
          group: notification-service
          content-type: application/json
```

#### 3.3 事件处理模式

**发布事件**：
```java
@Service
public class OrderService {
    
    @Autowired
    private StreamBridge streamBridge;
    
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        // 1. 创建订单
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setProductId(request.getProductId());
        order.setQuantity(request.getQuantity());
        order = orderRepository.save(order);
        
        // 2. 发布事件
        OrderEvent event = new OrderEvent();
        event.setEventType("CREATED");
        event.setOrderId(order.getId());
        event.setUserId(order.getUserId());
        event.setProductId(order.getProductId());
        event.setQuantity(order.getQuantity());
        event.setTimestamp(LocalDateTime.now());
        
        streamBridge.send("orderEvents-out-0", event);
        
        return order;
    }
}
```

**消费事件**：
```java
@Component
public class NotificationEventHandler {
    
    @EventListener
    public void handleOrderEvent(OrderEvent event) {
        switch (event.getEventType()) {
            case "CREATED":
                sendOrderConfirmation(event);
                break;
            case "CANCELLED":
                sendOrderCancellation(event);
                break;
        }
    }
    
    private void sendOrderConfirmation(OrderEvent event) {
        Notification notification = new Notification();
        notification.setUserId(event.getUserId());
        notification.setType("ORDER_CONFIRMATION");
        notification.setMessage("您的订单 #" + event.getOrderId() + " 已创建成功");
        notification.setTimestamp(LocalDateTime.now());
        
        notificationService.save(notification);
    }
}
```

### 第四步：RabbitMQ集成

#### 4.1 RabbitMQ架构

```
RabbitMQ Broker
├── Exchanges
│   ├── order.events (Topic Exchange)
│   ├── inventory.events (Topic Exchange)
│   └── user.events (Topic Exchange)
├── Queues
│   ├── order.events.notification
│   ├── order.events.analytics
│   ├── inventory.events.notification
│   └── inventory.events.warehouse
└── Dead Letter Queues
    ├── order.events.dlq
    └── inventory.events.dlq
```

#### 4.2 消息路由策略

**路由键设计**：
```
order.created    → 订单创建事件
order.updated    → 订单更新事件
order.cancelled  → 订单取消事件
order.completed  → 订单完成事件

inventory.updated    → 库存更新事件
inventory.low_stock  → 库存不足事件
inventory.out_stock  → 库存耗尽事件

user.registered  → 用户注册事件
user.updated     → 用户信息更新事件
```

#### 4.3 错误处理和重试

**死信队列配置**：
```yaml
spring:
  cloud:
    stream:
      rabbit:
        bindings:
          orderEvents-in-0:
            consumer:
              auto-bind-dlq: true
              dlq-ttl: 86400000  # 24小时
              max-attempts: 3
              back-off-initial-interval: 1000
              back-off-max-interval: 10000
              back-off-multiplier: 2.0
```

### 第五步：通知服务实现

#### 5.1 通知类型设计

```java
public enum NotificationType {
    ORDER_CONFIRMATION("订单确认", "您的订单已创建成功"),
    ORDER_SHIPPED("订单发货", "您的订单已发货"),
    INVENTORY_LOW("库存预警", "商品库存不足"),
    PAYMENT_SUCCESS("支付成功", "支付已完成"),
    SYSTEM_ALERT("系统警告", "系统异常");
}
```

#### 5.2 实时通知推送

**WebSocket集成**：
```java
@Controller
public class NotificationWebSocketController {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    public void sendNotificationToUser(Long userId, Notification notification) {
        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/queue/notifications",
            notification
        );
    }
}
```

**前端WebSocket连接**：
```javascript
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    stompClient.subscribe('/user/queue/notifications', function(notification) {
        const data = JSON.parse(notification.body);
        showNotification(data.message);
    });
});
```

### 第六步：安全集成实现

#### 6.1 API Gateway安全配置

```java
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {
    
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtDecoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/api/users/**").hasAnyRole("CUSTOMER", "ADMIN")
                .pathMatchers("/api/products/**").permitAll()
                .pathMatchers("/api/orders/**").hasAnyRole("CUSTOMER", "VENDOR", "ADMIN")
                .pathMatchers("/api/admin/**").hasRole("ADMIN")
                .anyExchange().authenticated()
            )
            .build();
    }
}
```

#### 6.2 微服务安全配置

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class ResourceServerConfig {
    
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
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .build();
    }
}
```

### 第七步：测试和验证

#### 7.1 安全测试

**获取访问令牌**：
```bash
# 1. 获取授权码
curl "http://localhost:8090/oauth2/authorize?response_type=code&client_id=api-gateway&redirect_uri=http://localhost:8080/login/oauth2/code/api-gateway&scope=openid%20read%20write"

# 2. 用授权码换取令牌
curl -X POST \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code&code=YOUR_CODE&redirect_uri=http://localhost:8080/login/oauth2/code/api-gateway&client_id=api-gateway&client_secret=gateway-secret" \
  "http://localhost:8090/oauth2/token"
```

**测试受保护的端点**：
```bash
# 使用令牌访问用户信息
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  "http://localhost:8080/api/users/1"

# 测试权限控制
curl -H "Authorization: Bearer CUSTOMER_TOKEN" \
  "http://localhost:8080/api/admin/users"  # 应该返回403
```

#### 7.2 事件驱动测试

**创建订单触发事件**：
```bash
# 创建订单
curl -X POST \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"productId":1,"quantity":2}' \
  "http://localhost:8080/api/orders"

# 检查通知
curl -H "Authorization: Bearer YOUR_TOKEN" \
  "http://localhost:8080/api/notifications/user/1"
```

**监控消息队列**：
```bash
# 检查RabbitMQ队列状态
curl -u guest:guest "http://localhost:15672/api/queues"

# 查看队列消息数量
curl -u guest:guest "http://localhost:15672/api/queues/%2F/order.events"
```

## 🔧 核心概念深入

### OAuth2 vs JWT

**OAuth2**：
- 授权框架，定义了获取访问令牌的流程
- 支持多种授权模式（授权码、客户端凭证、密码等）
- 关注的是"如何获取访问权限"

**JWT**：
- 令牌格式，定义了令牌的结构和内容
- 自包含，包含用户信息和权限
- 关注的是"访问令牌的格式"

**结合使用**：
```
OAuth2流程 → 生成JWT令牌 → 微服务验证JWT → 授权访问
```

### 事件驱动 vs 请求响应

**请求响应模式**：
```
Client → Service A → Service B → Service C → Response
```
- 优点：简单直接，易于理解
- 缺点：紧耦合，级联故障，性能瓶颈

**事件驱动模式**：
```
Service A → Event → Message Broker → Service B, C, D
```
- 优点：松耦合，高可用，可扩展
- 缺点：复杂性增加，最终一致性

### 消息队列模式

#### 1. 点对点模式 (Point-to-Point)
```
Producer → Queue → Consumer
```
- 一个消息只能被一个消费者处理
- 适用于任务分发场景

#### 2. 发布订阅模式 (Publish-Subscribe)
```
Publisher → Topic → Subscriber1, Subscriber2, Subscriber3
```
- 一个消息可以被多个订阅者处理
- 适用于事件通知场景

#### 3. 路由模式 (Routing)
```
Publisher → Exchange → Queue1, Queue2 (based on routing key)
```
- 基于路由键分发消息
- 适用于条件分发场景

## 🎯 实践练习

### 练习1：扩展用户角色

**任务**：添加新的用户角色"MANAGER"，具有以下权限：
- 可以查看所有订单
- 可以更新产品信息
- 不能删除用户

**实现步骤**：
1. 在Authorization Server中添加MANAGER角色
2. 更新JWT令牌生成逻辑
3. 在各微服务中添加相应的权限控制
4. 测试新角色的权限

### 练习2：实现支付事件

**任务**：当支付完成时，发送事件通知相关服务：
- 更新订单状态
- 发送支付确认通知
- 触发发货流程

**实现步骤**：
1. 定义PaymentEvent事件
2. 在Payment Service中发布事件
3. 在Order Service中消费事件更新状态
4. 在Notification Service中发送通知

### 练习3：实现库存预警

**任务**：当库存低于阈值时，自动发送预警通知：
- 监控库存变化
- 设置库存阈值
- 发送预警通知给管理员
- 记录预警历史

**实现步骤**：
1. 在Product Service中添加库存监控
2. 定义InventoryAlertEvent事件
3. 实现预警逻辑和通知发送
4. 添加预警历史记录功能

## 🔍 故障排除指南

### 常见OAuth2问题

#### 1. 令牌验证失败
**症状**：返回401 Unauthorized
**排查步骤**：
```bash
# 检查令牌格式
echo "YOUR_JWT_TOKEN" | cut -d'.' -f2 | base64 -d

# 检查令牌是否过期
curl http://localhost:8090/.well-known/openid_configuration

# 检查公钥配置
curl http://localhost:8090/.well-known/jwks.json
```

#### 2. 权限不足
**症状**：返回403 Forbidden
**排查步骤**：
```bash
# 检查用户角色
echo "YOUR_JWT_TOKEN" | cut -d'.' -f2 | base64 -d | jq '.authorities'

# 检查方法权限配置
# 查看@PreAuthorize注解是否正确
```

### 常见事件驱动问题

#### 1. 消息未被消费
**症状**：队列中消息堆积
**排查步骤**：
```bash
# 检查消费者状态
curl -u guest:guest "http://localhost:15672/api/consumers"

# 检查队列绑定
curl -u guest:guest "http://localhost:15672/api/queues/%2F/order.events/bindings"

# 检查消费者日志
docker-compose logs notification-service
```

#### 2. 消息重复消费
**症状**：同一事件被处理多次
**解决方案**：
```java
@Component
public class IdempotentEventHandler {
    
    private final Set<String> processedEvents = new ConcurrentHashMap<>();
    
    @EventListener
    public void handleOrderEvent(OrderEvent event) {
        String eventId = event.getOrderId() + "-" + event.getEventType();
        
        if (processedEvents.contains(eventId)) {
            log.warn("Event already processed: {}", eventId);
            return;
        }
        
        // 处理事件
        processEvent(event);
        
        // 记录已处理
        processedEvents.add(eventId);
    }
}
```

## 📊 性能优化建议

### JWT令牌优化

1. **令牌大小控制**：
   - 避免在JWT中存储大量数据
   - 使用短的claim名称
   - 考虑使用引用令牌

2. **缓存策略**：
   - 缓存公钥以避免重复获取
   - 缓存用户权限信息
   - 使用Redis存储令牌黑名单

### 消息队列优化

1. **批量处理**：
```java
@RabbitListener(queues = "order.events")
public void handleOrderEvents(List<OrderEvent> events) {
    // 批量处理事件
    events.forEach(this::processEvent);
}
```

2. **预取配置**：
```yaml
spring:
  rabbitmq:
    listener:
      simple:
        prefetch: 10  # 预取10条消息
        concurrency: 5  # 5个并发消费者
```

## 🚀 生产部署考虑

### 安全生产配置

1. **密钥管理**：
   - 使用外部密钥管理服务
   - 定期轮换密钥
   - 分离公钥和私钥存储

2. **HTTPS配置**：
   - 所有服务启用HTTPS
   - 配置SSL证书
   - 禁用HTTP重定向

### 消息队列生产配置

1. **高可用部署**：
   - RabbitMQ集群配置
   - 镜像队列设置
   - 负载均衡配置

2. **监控告警**：
   - 队列长度监控
   - 消费者健康检查
   - 死信队列告警

## 📚 扩展学习资源

### 官方文档
- [Spring Security OAuth2](https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html)
- [Spring Cloud Stream](https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/)
- [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html)

### 最佳实践
- [OAuth2 Security Best Practices](https://tools.ietf.org/html/rfc6749#section-10)
- [Event-Driven Architecture Patterns](https://microservices.io/patterns/data/event-driven-architecture.html)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)

### 进阶主题
- CQRS (Command Query Responsibility Segregation)
- Event Sourcing
- Saga Pattern
- OAuth2 PKCE Extension
- OpenID Connect

通过完成Week 8的学习，您将掌握构建安全、可扩展的事件驱动微服务架构的核心技能，为企业级应用开发奠定坚实基础。