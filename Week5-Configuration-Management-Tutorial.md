# Spring Cloud 学习框架 - 第5周：配置管理

## 📖 概述

这份文档详细介绍了 ShopHub 电商平台第5周的实现，包括 Spring Cloud Config Server 的搭建、集中化配置管理、功能开关(Feature Toggles)实现和动态配置刷新机制。

---

## 🎯 学习目标

通过本周的学习，你将掌握：

- **集中化配置管理**：理解配置中心的架构和作用
- **功能开关设计**：实现渐进式功能发布和快速回滚
- **环境配置隔离**：开发、测试、生产环境差异化管理
- **动态配置更新**：运行时配置热更新机制
- **配置安全管理**：认证、加密和权限控制
- **容器化配置**：Docker环境下的配置管理

---

## 🏗️ 架构设计

### 新增架构图

```
                    ┌─────────────────┐
                    │  Config Server  │
                    │  (Port: 8888)   │ ◄── Git Repository
                    └─────────┬───────┘     │ Local Files
                              │             └─ Encrypted Properties
                    ┌─────────▼───────┐
                    │   Eureka Server │
                    │   (Port: 8761)  │
                    └─────────┬───────┘
                              │
                    ┌─────────▼───────┐
                    │   API Gateway   │      ┌─────────────┐
                    │   (Port: 8080)  │◄────►│   Redis     │
                    └─────────┬───────┘      │ (Port: 6379)│
                              │              └─────────────┘
                ┌─────────────┼─────────────┐
                │             │             │
        ┌───────▼────┐ ┌──────▼─────┐ ┌────▼──────┐
        │User Service│ │Product     │ │Order      │
        │(Port: 8081)│ │Service     │ │Service    │
        │@RefreshScope│ │@RefreshScope│ │@RefreshScope│
        └────────────┘ └────────────┘ └───────────┘
               ▲              ▲              ▲
               │              │              │
               └──── Bootstrap Config ───────┘
```

### 服务详情

| 服务名称 | 端口 | 主要功能 | 新增特性 |
|---------|------|----------|----------|
| config-server | 8888 | 集中配置管理 | **新增** |
| eureka-server | 8761 | 服务注册中心 | 依赖Config Server |
| api-gateway | 8080 | 统一入口、路由转发 | 功能开关、配置刷新 |
| user-service | 8081 | 用户管理 | 配置中心集成 |
| product-service | 8082 | 产品目录 | 功能开关控制 |
| order-service | 8083 | 订单处理 | 支付配置管理 |

---

## 🌐 Config Server 实现详情

### 📍 核心组件

#### 1. **Spring Cloud Config Server 配置**
```yaml
spring:
  cloud:
    config:
      server:
        native:
          search-locations: classpath:/config-repo
        git:
          uri: file://${user.home}/config-repo
          clone-on-start: true
          force-pull: true
  security:
    user:
      name: configuser
      password: configpass
```

#### 2. **配置存储结构**
```
config-repo/
├── api-gateway.yml              # Gateway基础配置
├── api-gateway-dev.yml          # Gateway开发环境配置
├── api-gateway-prod.yml         # Gateway生产环境配置
├── product-service.yml          # Product Service基础配置
├── product-service-dev.yml      # Product Service开发环境配置
├── product-service-prod.yml     # Product Service生产环境配置
├── user-service.yml             # User Service配置
└── order-service.yml            # Order Service配置
```

#### 3. **功能开关配置**
| 功能开关 | 默认状态 | 开发环境 | 生产环境 | 用途 |
|---------|----------|----------|----------|------|
| feature.recommendations.enabled | true | true | true | 推荐系统 |
| feature.realtime-inventory.enabled | false | false | true | 实时库存 |
| feature.multi-currency.enabled | false | false | true | 多币种支持 |

---

## 🔧 技术实现

### 🛠️ 1. **Config Server应用主类**
```java
@SpringBootApplication
@EnableConfigServer
@EnableDiscoveryClient
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

### 🔍 2. **客户端配置集成**

#### **Bootstrap配置加载**
```yaml
# bootstrap.yml - 在所有微服务中
spring:
  application:
    name: product-service
  profiles:
    active: dev
  cloud:
    config:
      uri: http://localhost:8888
      username: configuser
      password: configpass
      fail-fast: true
      retry:
        initial-interval: 1000
        max-attempts: 6
```

#### **功能开关控制器**
```java
@RestController
@RefreshScope  // 关键注解：支持配置热更新
public class FeatureController {

    @Value("${feature.recommendations.enabled:false}")
    private boolean recommendationsEnabled;

    @GetMapping("/features")
    public Map<String, Object> getFeatureToggles() {
        Map<String, Object> features = new HashMap<>();
        features.put("recommendations", recommendationsEnabled);
        return features;
    }
}
```

### 🌍 3. **环境配置差异化**

#### **开发环境特点**
```yaml
# api-gateway-dev.yml
feature:
  recommendations:
    enabled: true
  realtime-inventory:
    enabled: false    # 开发环境关闭实时库存
  multi-currency:
    enabled: false    # 开发环境关闭多币种

payment:
  gateway:
    primary: stripe
    stripe:
      api-key: sk_test_dev_dummy    # 测试环境密钥

logging:
  level:
    com.shophub: DEBUG    # 开发环境详细日志
```

#### **生产环境特点**
```yaml
# api-gateway-prod.yml  
feature:
  recommendations:
    enabled: true
  realtime-inventory:
    enabled: true     # 生产环境启用实时库存
  multi-currency:
    enabled: true     # 生产环境启用多币种

payment:
  gateway:
    primary: stripe
    stripe:
      api-key: ${STRIPE_PROD_API_KEY}    # 环境变量注入

logging:
  level:
    root: WARN        # 生产环境精简日志
    com.shophub: INFO
```

### 🔄 4. **动态配置刷新机制**

#### **@RefreshScope应用**
```java
@RestController
@RequestMapping("/api/products")
@RefreshScope    // 支持配置属性动态刷新
public class ProductController {
    
    @Value("${feature.recommendations.enabled:false}")
    private boolean recommendationsEnabled;    // 可动态更新
    
    @GetMapping("/recommendations")
    public ResponseEntity<?> getRecommendations() {
        if (!recommendationsEnabled) {
            return ResponseEntity.ok(Map.of(
                "enabled", false,
                "message", "产品推荐功能已关闭"
            ));
        }
        // 推荐逻辑...
    }
}
```

#### **配置刷新触发**
```bash
# 手动触发单个服务配置刷新
curl -X POST http://localhost:8082/actuator/refresh

# 批量刷新所有服务
curl -X POST http://localhost:8080/actuator/refresh
curl -X POST http://localhost:8081/actuator/refresh  
curl -X POST http://localhost:8083/actuator/refresh
```

---

## 🧪 测试验证

### 📋 自动化测试脚本

创建了综合测试脚本 `test-week5-config-management.sh`，包含：

#### **测试覆盖范围**
1. **✅ 服务启动验证**: 检查Config Server和所有微服务健康状态
2. **✅ 配置获取测试**: 验证Config Server配置分发功能
3. **✅ 功能开关测试**: 验证各服务功能开关状态和控制逻辑
4. **✅ 配置刷新测试**: 验证@RefreshScope动态配置更新
5. **✅ 环境配置对比**: 展示开发/生产环境配置差异
6. **✅ 支付配置测试**: 验证支付网关的环境适配
7. **✅ 监控端点测试**: Config Server和客户端监控信息
8. **✅ 安全认证测试**: 配置访问权限验证

#### **运行测试**
```bash
./test-week5-config-management.sh
```

---

## 📊 关键特性展示

### 🎯 **集中化配置管理**
- 统一配置存储和版本控制
- 多环境配置隔离和继承
- 配置变更审计和回滚

### ⚡ **功能开关系统**
- 渐进式功能发布控制
- 生产环境快速功能回滚
- A/B测试和用户分组支持

### 🔒 **配置安全管理**
- Spring Security集成认证
- 敏感配置加密存储
- 配置访问权限控制

### 📈 **运维监控能力**
- Config Server健康检查
- 配置拉取统计和监控
- 客户端配置状态追踪

---

## 🚦 访问端点

### 🌐 **配置中心管理**
- **Config Server**: `http://localhost:8888`
- **配置获取**: `http://configuser:configpass@localhost:8888/{service}/{profile}`
- **健康检查**: `http://localhost:8888/actuator/health`

### 📊 **功能开关查看**
- **Gateway功能状态**: `http://localhost:8080/api/gateway/features`
- **Product功能状态**: `http://localhost:8082/api/products/features`
- **推荐功能测试**: `http://localhost:8082/api/products/recommendations`
- **库存功能测试**: `http://localhost:8082/api/products/1/inventory`

### 🔄 **配置刷新端点**
- **Gateway配置刷新**: `POST http://localhost:8080/actuator/refresh`
- **Product配置刷新**: `POST http://localhost:8082/actuator/refresh`
- **Order配置刷新**: `POST http://localhost:8083/actuator/refresh`

---

## 🎬 **实际应用场景**

### **场景1: 新功能渐进式发布**
```yaml
# 第一阶段：内测用户启用
feature:
  new-payment-method:
    enabled: false
    beta-users: user1,user2,user3
```

```java
@Value("${feature.new-payment-method.enabled:false}")
private boolean newPaymentEnabled;

@Value("${feature.new-payment-method.beta-users:}")
private List<String> betaUsers;

public boolean isNewPaymentEnabledForUser(String userId) {
    return newPaymentEnabled || betaUsers.contains(userId);
}
```

### **场景2: 生产紧急问题回滚**
```bash
# 发现推荐算法有问题，立即关闭推荐功能
# 1. 修改配置文件
echo "feature.recommendations.enabled: false" >> api-gateway.yml

# 2. 触发配置刷新
curl -X POST http://localhost:8080/actuator/refresh

# 3. 验证功能已关闭
curl http://localhost:8082/api/products/recommendations
```

### **场景3: 多环境支付配置**
```yaml
# 开发环境 - 使用测试支付网关
payment:
  gateway:
    primary: stripe
    stripe:
      api-key: sk_test_dev_key
      webhook-secret: whsec_test_secret

# 生产环境 - 使用生产支付网关  
payment:
  gateway:
    primary: stripe
    stripe:
      api-key: ${STRIPE_PROD_API_KEY}
      webhook-secret: ${STRIPE_PROD_WEBHOOK_SECRET}
```

---

## 🏆 **Week 5 成果总结**

### ✅ **已完成任务**
- [x] 设置Config Server (端口8888)
- [x] 移动所有application.yml到集中配置仓库
- [x] 实现功能开关：推荐、实时库存、多币种支持
- [x] 配置不同环境的支付网关
- [x] 添加@RefreshScope配置刷新功能
- [x] 集成Spring Security配置认证
- [x] Docker容器化配置管理

### 📈 **核心指标**
- **配置服务数**: 6个 (config-server + 5个微服务)
- **功能开关数**: 3个主要功能开关
- **环境配置**: 2套环境配置 (dev/prod)
- **支付网关**: 2个支付渠道配置
- **刷新端点**: 5个配置刷新端点
- **认证方式**: 基础认证 + Bearer Token支持

### 🌟 **业务价值**
- **配置统一管理**: 消除配置分散和不一致问题
- **功能灵活控制**: 支持渐进式发布和快速回滚
- **环境隔离保护**: 防止环境间配置混乱
- **零停机更新**: 配置变更无需重启服务
- **安全合规**: 敏感配置加密和访问控制

---

## 🔗 **下周预告: Week 6**

**主题**: Circuit Breaker & Resilience (熔断器和容错机制)
**重点**: 
- Resilience4j集成和配置
- 熔断器模式实现
- 服务降级和回退机制
- 舱壁隔离模式

---

*本周实现了完整的配置管理解决方案，为ShopHub电商平台提供了集中化、安全、灵活的配置管理能力，支持功能的渐进式发布和生产环境的快速响应。*