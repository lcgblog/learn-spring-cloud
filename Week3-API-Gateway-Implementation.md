# Spring Cloud 学习框架 - 第3周：API Gateway 与路由

## 📖 概述

这份文档详细介绍了 ShopHub 电商平台第3周的实现，包括 Spring Cloud Gateway 的搭建、路由配置、限流机制和自定义过滤器的实现。

---

## 🎯 学习目标

通过本周的学习，你将掌握：

- **API Gateway 架构**：理解微服务网关的作用和架构模式
- **智能路由**：基于路径、请求头、参数的动态路由配置
- **流量控制**：实现请求限流和用户分级限流策略
- **过滤器机制**：全局过滤器和局部过滤器的开发
- **CORS 支持**：跨域资源共享的配置和处理
- **负载均衡**：客户端负载均衡和服务发现集成

---

## 🏗️ 架构设计

### 新增架构图

```
                    ┌─────────────────┐
                    │   Eureka Server │
                    │   (Port: 8761)  │
                    └─────────────────┘
                            │
                    ┌───────▼───────┐
                    │  API Gateway  │      ┌─────────────┐
                    │  (Port: 8080) │◄────►│   Redis     │
                    └───────┬───────┘      │ (Port: 6379)│
                            │              └─────────────┘
                ┌───────────┼───────────┐
                │           │           │
        ┌───────▼────┐ ┌────▼─────┐ ┌───▼──────┐
        │User Service│ │Product   │ │Order     │
        │(Port: 8081)│ │Service   │ │Service   │
        │            │ │(Port:8082)│ │(Port:8083)│
        └────────────┘ └──────────┘ └──────────┘
```

### 服务详情

| 服务名称 | 端口 | 主要功能 | 新增特性 |
|---------|------|----------|----------|
| api-gateway | 8080 | 统一入口、路由转发 | **新增** |
| redis | 6379 | 限流存储 | **新增** |
| eureka-server | 8761 | 服务注册中心 | 保持不变 |
| user-service | 8081 | 用户管理 | 通过Gateway访问 |
| product-service | 8082 | 产品目录 | 通过Gateway访问 |
| order-service | 8083 | 订单处理 | 通过Gateway访问 |

---

## 🌐 API Gateway 实现详情

### 📍 核心组件

#### 1. **Spring Cloud Gateway 配置**
```yaml
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
```

#### 2. **路由规则配置**
- **用户服务路由**: `/api/users/**` → `lb://user-service`
- **产品服务路由**: `/api/products/**` → `lb://product-service`
- **订单服务路由**: `/api/orders/**` → `lb://order-service`
- **Eureka管理**: `/eureka/**` → `lb://eureka-server`

#### 3. **限流策略**
| 服务 | 限流速率 | 突发容量 | 目标场景 |
|------|----------|----------|----------|
| 用户服务 | 100 req/min | 200 | 用户认证、资料管理 |
| 产品服务 | 200 req/min | 400 | 商品浏览、搜索 |
| 订单服务 | 50 req/min | 100 | 订单提交、支付 |

---

## 🔧 技术实现

### 🛠️ 1. **Gateway应用主类**
```java
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
```

### 🔍 2. **核心过滤器实现**

#### **全局日志过滤器**
- **功能**: 记录所有请求和响应
- **特性**: 
  - 请求时间统计
  - 客户端IP识别
  - 详细日志记录

#### **认证过滤器工厂**
- **功能**: 模拟用户认证和等级识别
- **特性**:
  - Authorization头检查
  - 用户等级设置（regular/premium）
  - 权限验证

#### **限流键解析器**
- **基于IP限流**: 不同IP独立计数
- **用户等级支持**: Premium用户独立限流
- **API路径区分**: 不同API使用不同限流策略

### 🌍 3. **CORS跨域配置**
```java
@Bean
public CorsWebFilter corsWebFilter() {
    CorsConfiguration corsConfiguration = new CorsConfiguration();
    corsConfiguration.setAllowedOriginPatterns(Arrays.asList("*"));
    corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    corsConfiguration.setAllowedHeaders(Arrays.asList("*"));
    corsConfiguration.setAllowCredentials(true);
    corsConfiguration.setMaxAge(3600L);
    // ...
}
```

---

## 🧪 测试验证

### 📋 自动化测试脚本

创建了综合测试脚本 `test-api-gateway.sh`，包含：

#### **测试覆盖范围**
1. **✅ 服务启动验证**: 检查所有服务健康状态
2. **✅ Gateway路由测试**: 验证路由规则正确性
3. **✅ 负载均衡测试**: 多次请求验证负载分布
4. **✅ 限流功能测试**: 快速请求验证限流机制
5. **✅ CORS支持测试**: 跨域预检请求验证
6. **✅ 自定义过滤器测试**: Premium用户和请求头测试
7. **✅ 监控端点测试**: Gateway健康检查和路由信息
8. **✅ API演示**: 完整的业务流程演示

#### **运行测试**
```bash
./test-api-gateway.sh
```

---

## 📊 关键特性展示

### 🎯 **智能路由**
- 基于服务名的动态路由
- 路径重写和前缀处理
- 负载均衡集成

### ⚡ **高级限流**
- 基于Redis的分布式限流
- 用户等级差异化限流
- 突发流量处理

### 🔒 **安全过滤**
- 请求认证检查
- 自定义请求头添加
- 响应头安全设置

### 📈 **监控能力**
- 详细的请求日志
- 性能时间统计
- 健康检查端点
- 路由信息暴露

---

## 🚦 访问端点

### 🌐 **通过Gateway访问**
- **统一入口**: `http://localhost:8080`
- **用户服务**: `http://localhost:8080/api/users`
- **产品服务**: `http://localhost:8080/api/products`
- **订单服务**: `http://localhost:8080/api/orders`

### 📊 **监控和管理**
- **Gateway健康检查**: `http://localhost:8080/actuator/health`
- **路由信息**: `http://localhost:8080/actuator/gateway/routes`
- **指标监控**: `http://localhost:8080/actuator/metrics`
- **Eureka控制台**: `http://localhost:8080/eureka`

---

## 🎬 **实际应用场景**

### **场景1: 闪购活动流量管理**
```bash
# 模拟高并发请求
for i in {1..50}; do
  curl -H "X-User-Tier: premium" http://localhost:8080/api/products &
done
```

### **场景2: 移动端跨域支持**
```bash
# 前端CORS预检请求
curl -H "Origin: http://localhost:3000" \
     -H "Access-Control-Request-Method: POST" \
     -X OPTIONS http://localhost:8080/api/orders
```

### **场景3: API合作伙伴接入**
```bash
# 外部系统通过Gateway访问
curl -H "Authorization: Bearer partner-token" \
     -H "X-User-Tier: premium" \
     http://localhost:8080/api/products/search?keyword=phone
```

---

## 🏆 **Week 3 成果总结**

### ✅ **已完成任务**
- [x] 设置Spring Cloud Gateway (端口8080)
- [x] 配置所有服务路由规则
- [x] 实现请求日志自定义过滤器
- [x] 添加基于Redis的限流功能
- [x] 设置CORS跨域支持
- [x] 集成Eureka服务发现
- [x] Docker容器化部署支持

### 📈 **核心指标**
- **路由服务数**: 4个 (user, product, order, eureka)
- **限流策略**: 3种不同速率限制
- **过滤器类型**: 2个全局 + 1个局部过滤器
- **监控端点**: 4个管理端点
- **CORS支持**: 完整的跨域配置
- **负载均衡**: 基于Eureka的客户端负载均衡

### 🌟 **业务价值**
- **统一入口**: 简化客户端集成复杂性
- **流量控制**: 保护后端服务免受过载
- **安全增强**: 统一的认证和授权检查点
- **运维便利**: 集中化的监控和日志记录
- **扩展性**: 支持动态路由和服务发现

---

## 🔗 **下周预告: Week 4**

**主题**: Load Balancing & Client-Side Discovery
**重点**: 
- 多实例部署和水平扩展
- 自定义负载均衡策略
- Feign客户端高级配置
- 容错和重试机制

---

*本周实现了完整的API Gateway解决方案，为ShopHub电商平台提供了统一、安全、高性能的服务入口，同时具备了企业级的流量管理和监控能力。*