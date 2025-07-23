# Spring Cloud 学习指南 - Week 3: API Gateway 与路由

## 🎯 本周学习目标

构建 **ShopHub API Gateway**，实现统一入口、智能路由、流量控制和安全过滤，为电商平台提供企业级的网关解决方案。

---

## 📚 理论基础

### 什么是API Gateway？

API Gateway是微服务架构中的**统一入口点**，就像商场的总服务台：
- **统一入口**: 所有外部请求都通过Gateway进入系统
- **路由转发**: 根据请求路径将流量分发到对应的微服务
- **横切关注点**: 认证、限流、日志、监控等功能统一处理

### 为什么需要API Gateway？

#### **问题场景**：
想象你是ShopHub的前端开发者：
```javascript
// 没有Gateway时，前端需要记住所有服务地址
const users = await fetch('http://user-service:8081/api/users');
const products = await fetch('http://product-service:8082/api/products');  
const orders = await fetch('http://order-service:8083/api/orders');
```

**问题**：
- 前端需要管理多个服务地址
- 跨域配置复杂
- 认证逻辑分散在各个服务
- 难以统一限流和监控

#### **Gateway解决方案**：
```javascript
// 有了Gateway，前端只需要一个地址
const users = await fetch('http://api-gateway:8080/api/users');
const products = await fetch('http://api-gateway:8080/api/products');
const orders = await fetch('http://api-gateway:8080/api/orders');
```

---

## 🛠️ 动手实践

### Step 1: 创建Gateway项目结构

```bash
# 创建项目目录
mkdir -p api-gateway/src/main/java/com/shophub/gateway
mkdir -p api-gateway/src/main/resources

# 项目结构预览
api-gateway/
├── pom.xml
├── Dockerfile
└── src/main/
    ├── java/com/shophub/gateway/
    │   ├── ApiGatewayApplication.java
    │   ├── config/GatewayConfig.java
    │   └── filter/
    │       ├── GlobalLoggingFilter.java
    │       └── AuthenticationGatewayFilterFactory.java
    └── resources/application.yml
```

### Step 2: 配置Maven依赖

核心依赖理解：
```xml
<!-- WebFlux: Gateway基于响应式编程 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>

<!-- Gateway: 核心网关功能 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>

<!-- Redis: 分布式限流存储 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
```

### Step 3: 理解Gateway配置

#### **路由配置解析**：
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service              # 路由唯一标识
          uri: lb://user-service         # lb = LoadBalancer，基于服务名负载均衡
          predicates:                    # 匹配条件
            - Path=/api/users/**         # 路径匹配
          filters:                       # 过滤器处理
            - name: RequestRateLimiter   # 限流过滤器
              args:
                redis-rate-limiter.replenishRate: 100    # 每秒补充100个令牌
                redis-rate-limiter.burstCapacity: 200     # 令牌桶容量200
```

#### **关键概念解释**：

**1. Predicates (断言)**：
- 决定请求是否匹配这条路由
- 常用断言：Path、Method、Header、Query等

**2. Filters (过滤器)**：
- 对匹配的请求进行处理
- 分为Pre和Post两个阶段

**3. URI (目标地址)**：
- `lb://service-name`：通过服务发现进行负载均衡
- `http://localhost:8081`：直接指定地址

### Step 4: 实现自定义过滤器

#### **全局日志过滤器**：
```java
@Component
public class GlobalLoggingFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Pre处理：记录请求信息
        ServerHttpRequest request = exchange.getRequest();
        long startTime = System.currentTimeMillis();
        
        return chain.filter(exchange).then(
            // Post处理：记录响应信息
            Mono.fromRunnable(() -> {
                long duration = System.currentTimeMillis() - startTime;
                logger.info("Request processed in {}ms", duration);
            })
        );
    }
}
```

**理解要点**：
- `Mono<Void>`：响应式编程，异步非阻塞
- `then()`：在主流程完成后执行
- `ServerWebExchange`：包含请求和响应的上下文

### Step 5: 配置限流功能

#### **限流原理** - 令牌桶算法：
```
                Token Bucket (容量: 200)
                ┌─────────────────────┐
                │  🪙 🪙 🪙 🪙 🪙     │  每秒补充100个令牌
                │  🪙 🪙 🪙 🪙 🪙     │  
                └─────────────────────┘
                         │
                    请求到达时取走1个令牌
                         │
                    ┌────▼────┐
                    │  请求   │  有令牌 → 通过
                    │        │  没令牌 → 限流(429)
                    └─────────┘
```

#### **不同服务的限流策略**：
```yaml
# 用户服务：认证频率较低，限制宽松
user-service:
  replenishRate: 100    # 每秒100个
  burstCapacity: 200    # 突发200个

# 产品服务：浏览频率高，限制较宽松  
product-service:
  replenishRate: 200    # 每秒200个
  burstCapacity: 400    # 突发400个

# 订单服务：涉及支付，限制严格
order-service:
  replenishRate: 50     # 每秒50个
  burstCapacity: 100    # 突发100个
```

### Step 6: 实现用户分级限流

```java
@Bean
public KeyResolver userKeyResolver() {
    return exchange -> {
        String userTier = exchange.getRequest()
            .getHeaders()
            .getFirst("X-User-Tier");
        
        String clientIp = getClientIp(exchange.getRequest());
        
        if ("premium".equalsIgnoreCase(userTier)) {
            return Mono.just("premium:" + clientIp);  // Premium用户独立限流
        }
        
        return Mono.just("regular:" + clientIp);      // 普通用户限流
    };
}
```

**业务场景**：
- **普通用户**: 每分钟100个请求
- **VIP用户**: 每分钟1000个请求
- **API合作伙伴**: 无限制或更高限制

---

## 🧪 测试与验证

### 测试脚本使用

```bash
# 1. 启动所有服务
docker-compose up -d

# 2. 等待服务就绪
sleep 30

# 3. 运行Gateway测试
./test-api-gateway.sh
```

### 手动测试场景

#### **场景1: 基本路由测试**
```bash
# 通过Gateway访问各个服务
curl http://localhost:8080/api/users/health
curl http://localhost:8080/api/products/health  
curl http://localhost:8080/api/orders/health
```

#### **场景2: 限流测试**
```bash
# 快速发送请求触发限流
for i in {1..20}; do
  curl -s http://localhost:8080/api/products &
done
wait

# 查看是否有429状态码
```

#### **场景3: Premium用户测试**
```bash
# 普通用户请求
curl -H "X-User-Tier: regular" http://localhost:8080/api/products

# Premium用户请求
curl -H "X-User-Tier: premium" http://localhost:8080/api/products
```

#### **场景4: CORS跨域测试**
```bash
# 模拟前端跨域请求
curl -H "Origin: http://localhost:3000" \
     -H "Access-Control-Request-Method: GET" \
     -X OPTIONS http://localhost:8080/api/products
```

---

## 🎯 实际应用场景

### 场景1: 黑五促销活动

**挑战**: 流量突增10倍，保护后端服务
**解决方案**:
```yaml
# 临时调整限流策略
product-service:
  replenishRate: 500    # 临时提升到500/秒
  burstCapacity: 1000   # 突发处理1000个
```

### 场景2: 移动App上线

**挑战**: 移动端需要跨域支持
**解决方案**:
```yaml
# CORS配置
cors:
  allowedOrigins: 
    - "https://app.shophub.com"
    - "https://m.shophub.com"
  allowedMethods: ["GET", "POST", "PUT", "DELETE"]
```

### 场景3: 第三方API集成

**挑战**: 外部合作伙伴需要API访问
**解决方案**:
```java
// 基于API Key的不同限流策略
@Bean
public KeyResolver partnerKeyResolver() {
    return exchange -> {
        String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
        return Mono.just("partner:" + apiKey);
    };
}
```

---

## 🚨 常见问题与解决

### 问题1: Gateway启动失败
**症状**: Gateway无法连接Redis
**解决**:
```bash
# 检查Redis是否启动
docker ps | grep redis

# 检查网络连接
docker-compose logs redis
```

### 问题2: 路由不生效
**症状**: 请求返回404
**调试**:
```bash
# 查看路由配置
curl http://localhost:8080/actuator/gateway/routes

# 检查Eureka注册状态
curl http://localhost:8761/eureka/apps
```

### 问题3: 限流过于严格
**症状**: 正常请求被限流
**调整**:
```yaml
# 适当放宽限制
redis-rate-limiter:
  replenishRate: 200    # 增加补充速率
  burstCapacity: 500    # 增加突发容量
```

---

## 📈 性能优化建议

### 1. Redis连接池优化
```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 16    # 最大连接数
          max-idle: 8       # 最大空闲连接
          min-idle: 2       # 最小空闲连接
```

### 2. Gateway内存调优
```bash
# Docker启动参数
java -Xmx512m -Xms256m -jar api-gateway.jar
```

### 3. 监控关键指标
- **响应时间**: P95应该 < 200ms
- **错误率**: < 0.1%
- **限流触发率**: 根据业务调整

---

## ✅ 学习检查清单

- [ ] 理解API Gateway的作用和架构模式
- [ ] 能够配置基本的路由规则
- [ ] 掌握自定义过滤器的开发
- [ ] 理解限流算法和配置方法
- [ ] 能够处理CORS跨域问题
- [ ] 掌握Gateway的监控和调试方法
- [ ] 了解生产环境的优化策略

---

## 🎓 课后作业

### 基础作业
1. 为Gateway添加健康检查路由
2. 实现基于请求头的A/B测试路由
3. 添加API响应时间统计

### 进阶作业
1. 实现基于JWT的认证过滤器
2. 添加请求重试机制
3. 实现动态路由配置

### 实战作业
设计一个完整的API网关方案，支持：
- 多租户路由
- 细粒度权限控制
- 实时流量监控
- 自动熔断降级

---

*通过本周学习，你已经掌握了企业级API Gateway的核心技能。下周我们将学习负载均衡和客户端服务发现，进一步提升系统的可用性和性能。*