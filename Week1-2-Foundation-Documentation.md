# Spring Cloud 学习框架 - 第1-2周：基础设施与服务发现

## 📖 概述

这份文档详细介绍了 ShopHub 电商平台第1-2周的实现，包括 Eureka 服务注册中心和三个核心微服务的搭建。

---

## 🎯 学习目标

通过本周的学习，你将掌握：

- **服务注册与发现**：使用 Eureka Server 实现微服务的自动注册和发现
- **微服务架构基础**：理解微服务间的通信机制
- **Spring Cloud 核心组件**：Eureka Server、Eureka Client 的配置和使用
- **健康检查机制**：通过 Actuator 监控服务状态
- **服务间通信**：使用 OpenFeign 实现服务调用

---

## 🏗️ 架构设计

### 系统架构图

```
                    ┌─────────────────┐
                    │   Eureka Server │
                    │   (Port: 8761)  │
                    └─────────────────┘
                            │
                ┌───────────┼───────────┐
                │           │           │
        ┌───────▼────┐ ┌────▼─────┐ ┌───▼──────┐
        │User Service│ │Product   │ │Order     │
        │(Port: 8081)│ │Service   │ │Service   │
        │            │ │(Port:8082)│ │(Port:8083)│
        └────────────┘ └──────────┘ └──────────┘
```

### 服务详情

| 服务名称 | 端口 | 数据库 | 主要功能 |
|---------|------|--------|----------|
| eureka-server | 8761 | - | 服务注册中心 |
| user-service | 8081 | H2 (userdb) | 用户管理、认证 |
| product-service | 8082 | H2 (productdb) | 产品目录、搜索 |
| order-service | 8083 | H2 (orderdb) | 订单处理、购物车 |

---

## 📁 项目结构

```
LearnSpringCloud/
├── eureka-server/              # 服务注册中心
│   ├── src/main/java/com/shophub/eureka/
│   │   └── EurekaServerApplication.java
│   ├── src/main/resources/
│   │   └── application.yml
│   └── pom.xml
├── user-service/              # 用户服务
│   ├── src/main/java/com/shophub/user/
│   │   ├── UserServiceApplication.java
│   │   ├── entity/User.java
│   │   ├── repository/UserRepository.java
│   │   ├── service/UserService.java
│   │   └── controller/UserController.java
│   ├── src/main/resources/
│   │   └── application.yml
│   └── pom.xml
├── product-service/           # 产品服务
│   └── ...
├── order-service/            # 订单服务
│   └── ...
├── docker-compose.yml        # Docker 编排配置
└── Week1-2-Foundation-Documentation.md
```

---

## 🔧 核心技术组件

### 1. Eureka Server (服务注册中心)

**配置要点：**
```yaml
eureka:
  client:
    register-with-eureka: false  # 不向自己注册
    fetch-registry: false        # 不获取注册表
  server:
    enable-self-preservation: false  # 开发环境关闭自我保护
```

**主要功能：**
- 接收服务注册请求
- 维护服务实例列表
- 提供服务发现功能
- 健康检查和故障剔除

### 2. Eureka Client (微服务)

**配置要点：**
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true   # 向注册中心注册
    fetch-registry: true         # 获取服务列表
```

**核心注解：**
- `@EnableDiscoveryClient`：启用服务发现
- `@EnableFeignClients`：启用 Feign 客户端

---

## 🛠️ 实现细节

### 1. 用户服务 (User Service)

**核心实体：**
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String username;
    private String email;
    private String password;
    private String fullName;
    
    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.CUSTOMER;
    
    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.ACTIVE;
    
    // ... getters and setters
}
```

**API 接口：**
- `POST /api/users` - 创建用户
- `GET /api/users` - 获取所有用户
- `GET /api/users/{id}` - 根据ID查询用户
- `GET /api/users/username/{username}` - 根据用户名查询
- `PUT /api/users/{id}` - 更新用户信息
- `DELETE /api/users/{id}` - 删除用户
- `POST /api/users/authenticate` - 用户认证
- `GET /api/users/health` - 健康检查

### 2. 数据库配置

**H2 内存数据库：**
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:userdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  h2:
    console:
      enabled: true
      path: /h2-console
```

**访问 H2 控制台：**
- User Service: http://localhost:8081/h2-console
- Product Service: http://localhost:8082/h2-console
- Order Service: http://localhost:8083/h2-console

### 3. 健康检查配置

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
```

**健康检查端点：**
- Eureka Server: http://localhost:8761/actuator/health
- User Service: http://localhost:8081/actuator/health
- Product Service: http://localhost:8082/actuator/health
- Order Service: http://localhost:8083/actuator/health

---

## 🚀 启动指南

### 方式一：手动启动

1. **启动 Eureka Server**
```bash
cd eureka-server
mvn spring-boot:run
```

2. **启动 User Service**
```bash
cd user-service
mvn spring-boot:run
```

3. **启动其他服务**
```bash
cd product-service
mvn spring-boot:run

cd order-service
mvn spring-boot:run
```

### 方式二：Docker Compose（推荐）

```bash
# 构建并启动所有服务
docker-compose up --build

# 后台运行
docker-compose up -d --build

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

---

## 🧪 测试验证

### 1. 验证服务注册

**访问 Eureka Dashboard:**
http://localhost:8761

**应该看到：**
- USER-SERVICE (1 instance)
- PRODUCT-SERVICE (1 instance)
- ORDER-SERVICE (1 instance)

### 2. 测试用户服务 API

**创建用户：**
```bash
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "fullName": "测试用户"
  }'
```

**获取用户列表：**
```bash
curl http://localhost:8081/api/users
```

**用户认证：**
```bash
curl -X POST http://localhost:8081/api/users/authenticate \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

### 3. 健康检查验证

```bash
# 检查所有服务健康状态
curl http://localhost:8761/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
```

---

## 📝 作业练习

### 基础练习

1. **服务注册验证**
   - 启动所有服务
   - 访问 Eureka Dashboard 确认所有服务已注册
   - 关闭一个服务，观察注册中心的变化

2. **API 测试**
   - 使用 Postman 或 curl 测试用户服务的所有 API
   - 创建至少 3 个不同角色的用户
   - 验证用户认证功能

3. **数据库操作**
   - 访问 H2 控制台查看数据库表结构
   - 执行 SQL 查询验证数据存储

### 进阶练习

1. **服务扩展**
   - 为 Product Service 和 Order Service 添加基本的实体类和 API
   - 实现产品的增删改查功能
   - 实现订单的基本管理功能

2. **服务间通信**
   - 在 Order Service 中使用 Feign 调用 User Service
   - 实现下单时验证用户是否存在的功能

---

## 🔍 故障排查

### 常见问题

1. **服务无法注册到 Eureka**
   - 检查 `application.yml` 中的 `defaultZone` 配置
   - 确认 Eureka Server 已启动并可访问
   - 查看应用日志中的错误信息

2. **端口冲突**
   - 确认各服务使用不同端口 (8761, 8081, 8082, 8083)
   - 检查端口是否被其他程序占用

3. **数据库连接问题**
   - H2 内存数据库会在应用重启后丢失数据
   - 检查 H2 控制台访问路径和数据库 URL

### 日志分析

**查看关键日志：**
```bash
# 服务注册日志
grep "Registering application" logs/application.log

# 健康检查日志
grep "Health check" logs/application.log

# Eureka 客户端日志
grep "DiscoveryClient" logs/application.log
```

---

## 📚 延伸学习

### 推荐阅读

1. **Spring Cloud 官方文档**
   - [Spring Cloud Netflix](https://docs.spring.io/spring-cloud-netflix/docs/current/reference/html/)
   - [Spring Cloud Eureka](https://cloud.spring.io/spring-cloud-netflix/reference/html/#service-discovery-eureka-clients)

2. **微服务架构模式**
   - 服务注册与发现模式
   - 健康检查模式
   - 客户端负载均衡

### 下周预告

**第3周：API Gateway & 路由**
- Spring Cloud Gateway 配置
- 请求路由和过滤器
- 跨域处理和认证集成
- 限流和熔断保护

---

## ✅ 检查清单

完成以下检查项确认第1-2周学习目标达成：

- [ ] Eureka Server 成功启动并可访问 Dashboard
- [ ] 三个微服务成功注册到 Eureka
- [ ] 所有服务的健康检查端点正常响应
- [ ] 用户服务的所有 API 功能正常
- [ ] 能够通过 H2 控制台查看数据库
- [ ] 理解服务注册与发现的基本原理
- [ ] 掌握 Spring Cloud 基础配置
- [ ] 能够使用 Docker Compose 一键启动所有服务

**恭喜！你已经完成了 Spring Cloud 微服务架构的基础搭建！** 🎉 