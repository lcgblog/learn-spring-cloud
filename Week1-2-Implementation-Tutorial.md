# Spring Cloud 微服务实战教程
## ShopHub 电商平台开发指南

*从零开始构建企业级微服务架构*

---

## 📚 目录

- [第1章 Spring Cloud 微服务架构概述](#第1章-spring-cloud-微服务架构概述)
- [第2章 服务注册与发现 - Eureka Server](#第2章-服务注册与发现---eureka-server)
- [第3章 用户服务实现 - 完整的微服务开发](#第3章-用户服务实现---完整的微服务开发)
- [第4章 服务间通信 - OpenFeign](#第4章-服务间通信---openfeign)
- [第5章 健康检查与监控 - Actuator](#第5章-健康检查与监控---actuator)
- [第6章 容器化部署 - Docker](#第6章-容器化部署---docker)
- [第7章 实战演练与测试](#第7章-实战演练与测试)
- [第8章 常见问题与解决方案](#第8章-常见问题与解决方案)

---

## 第1章 Spring Cloud 微服务架构概述

### 1.1 什么是微服务架构

微服务架构是一种将单一应用程序开发为一套小型服务的方法，每个服务运行在自己的进程中，并使用轻量级机制（通常是HTTP资源API）进行通信。

### 1.2 ShopHub 项目架构

我们将构建一个名为 ShopHub 的电商平台，包含以下微服务：

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
        │(Port:8081)│ │Service   │ │Service   │
        │            │ │(Port:8082)│ │(Port:8083)│
        └────────────┘ └──────────┘ └──────────┘
```

### 1.3 技术栈选择

- **Spring Boot 3.2.0**: 微服务基础框架
- **Spring Cloud 2023.0.0**: 微服务组件套件
- **Eureka**: 服务注册与发现
- **OpenFeign**: 服务间通信
- **H2 Database**: 开发环境数据库
- **Docker**: 容器化部署

---

## 第2章 服务注册与发现 - Eureka Server

### 2.1 理论基础

在微服务架构中，服务实例会动态变化，我们需要一个服务注册中心来管理服务的注册、发现和健康检查。

### 2.2 Eureka Server 实现

#### 2.2.1 Maven 依赖配置

```xml
<!-- eureka-server/pom.xml -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
</dependencies>
```

**关键点解析：**
- `eureka-server`: 提供服务注册中心功能
- `actuator`: 提供健康检查和监控端点

#### 2.2.2 启动类配置

```java
// eureka-server/src/main/java/com/shophub/eureka/EurekaServerApplication.java
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
        System.out.println("ShopHub Eureka Server 已启动!");
        System.out.println("服务发现中心: http://localhost:8761");
    }
}
```

**关键注解说明：**
- `@EnableEurekaServer`: 启用 Eureka 服务器功能

#### 2.2.3 配置文件详解

```yaml
# eureka-server/src/main/resources/application.yml
server:
  port: 8761

spring:
  application:
    name: eureka-server

eureka:
  instance:
    hostname: localhost
  client:
    # 关键配置：作为服务发现中心，不需要向自己注册
    register-with-eureka: false
    fetch-registry: false
    service-url:
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
  server:
    # 开发环境关闭自我保护模式
    enable-self-preservation: false
    eviction-interval-timer-in-ms: 5000
```

**配置项详解：**
- `register-with-eureka: false`: 注册中心不向自己注册
- `fetch-registry: false`: 不获取服务注册表
- `enable-self-preservation: false`: 开发环境关闭自我保护

---

## 第3章 用户服务实现 - 完整的微服务开发

### 3.1 微服务设计原则

每个微服务应该：
- 单一职责：只负责用户相关功能
- 数据库独立：拥有自己的数据存储
- 接口明确：提供清晰的API接口

### 3.2 实体层设计

#### 3.2.1 用户实体类

```java
// user-service/src/main/java/com/shophub/user/entity/User.java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    @Column(unique = true)
    private String username;
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Column(unique = true)
    private String email;
    
    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.CUSTOMER;
    
    // 用户角色枚举
    public enum UserRole {
        CUSTOMER, VENDOR, ADMIN
    }
    
    // 用户状态枚举
    public enum UserStatus {
        ACTIVE, INACTIVE, SUSPENDED
    }
}
```

**设计亮点：**
- 使用 JPA 注解进行 ORM 映射
- Bean Validation 进行数据验证
- 枚举类型管理用户角色和状态

### 3.3 数据访问层

```java
// user-service/src/main/java/com/shophub/user/repository/UserRepository.java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameOrEmail(String username, String email);
    List<User> findByRole(User.UserRole role);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = 'ACTIVE'")
    long countActiveUsers();
}
```

**Repository 模式优势：**
- Spring Data JPA 自动实现基础 CRUD 操作
- 方法名约定自动生成查询语句
- `@Query` 注解支持自定义 JPQL 查询

### 3.4 业务逻辑层

```java
// user-service/src/main/java/com/shophub/user/service/UserService.java
@Service
@Transactional
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    public User createUser(User user) {
        // 业务规则验证
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("用户名已存在: " + user.getUsername());
        }
        
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("邮箱已存在: " + user.getEmail());
        }
        
        // 密码加密（简化版，生产环境应使用 BCrypt）
        user.setPassword("encrypted_" + user.getPassword());
        
        return userRepository.save(user);
    }
    
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public boolean authenticateUser(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return ("encrypted_" + password).equals(user.getPassword()) 
                   && user.getStatus() == User.UserStatus.ACTIVE;
        }
        return false;
    }
}
```

**业务层最佳实践：**
- `@Transactional` 确保数据一致性
- 业务规则验证在服务层进行
- 读操作使用 `readOnly = true` 优化性能

### 3.5 控制器层

```java
// user-service/src/main/java/com/shophub/user/controller/UserController.java
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody User user) {
        try {
            User savedUser = userService.createUser(user);
            savedUser.setPassword(null); // 安全：不返回密码
            return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("创建用户失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        Optional<User> userOpt = userService.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(null); // 安全处理
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        boolean isAuthenticated = userService.authenticateUser(
            loginRequest.getUsername(), 
            loginRequest.getPassword()
        );
        
        return isAuthenticated 
            ? ResponseEntity.ok().body("认证成功")
            : ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户名或密码错误");
    }
}
```

**RESTful API 设计原则：**
- 使用标准 HTTP 状态码
- 统一的响应格式
- 安全处理敏感信息（如密码）

---

## 第4章 服务间通信 - OpenFeign

### 4.1 为什么需要 Feign

在微服务架构中，服务间需要频繁通信。Feign 提供了声明式的 HTTP 客户端，简化了服务调用。

### 4.2 Feign 客户端实现

```java
// user-service/src/main/java/com/shophub/user/feign/ProductServiceClient.java
@FeignClient(name = "product-service")
public interface ProductServiceClient {
    
    @GetMapping("/api/products/{productId}/exists")
    String checkProductExists(@PathVariable("productId") Long productId);
    
    @GetMapping("/api/products/{productId}")
    String getProductDetails(@PathVariable("productId") Long productId);
}
```

### 4.3 在控制器中使用 Feign

```java
@RestController
public class UserController {
    
    @Autowired
    private ProductServiceClient productServiceClient;
    
    @GetMapping("/check-product/{productId}")
    public ResponseEntity<?> checkProduct(@PathVariable Long productId) {
        try {
            String result = productServiceClient.checkProductExists(productId);
            return ResponseEntity.ok("用户服务调用产品服务成功: " + result);
        } catch (Exception e) {
            return ResponseEntity.ok("产品服务暂时不可用: " + e.getMessage());
        }
    }
}
```

**Feign 的优势：**
- 声明式编程，代码简洁
- 集成 Eureka 自动服务发现
- 支持负载均衡
- 容错处理

---

## 第5章 健康检查与监控 - Actuator

### 5.1 Actuator 简介

Spring Boot Actuator 提供了生产就绪的功能，如健康检查、指标收集、HTTP 跟踪等。

### 5.2 配置 Actuator

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
```

### 5.3 自定义健康检查

```java
@GetMapping("/health")
public ResponseEntity<String> healthCheck() {
    return ResponseEntity.ok("User Service is running!");
}
```

**监控端点说明：**
- `/actuator/health`: 健康状态
- `/actuator/info`: 应用信息
- `/actuator/metrics`: 运行指标

---

## 第6章 容器化部署 - Docker

### 6.1 Docker Compose 配置

```yaml
# docker-compose.yml
version: '3.8'

services:
  eureka-server:
    build: ./eureka-server
    ports:
      - "8761:8761"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    networks:
      - shophub-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8761/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  user-service:
    build: ./user-service
    ports:
      - "8081:8081"
    environment:
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka/
    depends_on:
      eureka-server:
        condition: service_healthy
    networks:
      - shophub-network

networks:
  shophub-network:
    driver: bridge
```

**Docker 部署优势：**
- 环境一致性
- 快速部署
- 资源隔离
- 横向扩展

---

## 第7章 实战演练与测试

### 7.1 启动系统

```bash
# 方式一：使用脚本
start-services.bat

# 方式二：Docker Compose
docker-compose up --build

# 方式三：手动启动
cd eureka-server && mvn spring-boot:run
cd user-service && mvn spring-boot:run
```

### 7.2 API 测试

```bash
# 创建用户
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "password123",
    "fullName": "John Doe"
  }'

# 用户认证
curl -X POST http://localhost:8081/api/users/authenticate \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "password123"
  }'

# 测试服务间通信
curl http://localhost:8081/api/users/check-product/1
```

### 7.3 验证服务注册

访问 Eureka Dashboard: http://localhost:8761

应该看到所有服务已成功注册。

---

## 第8章 常见问题与解决方案

### 8.1 服务注册失败

**问题现象：**
服务启动但未在 Eureka 中显示

**解决方案：**
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
```

### 8.2 端口冲突

**问题现象：**
```
Port 8081 was already in use
```

**解决方案：**
```bash
# 查找占用端口的进程
netstat -ano | findstr 8081

# 结束进程
taskkill /PID [进程ID] /F
```

### 8.3 Feign 调用失败

**问题现象：**
```
com.netflix.client.ClientException: Load balancer does not have available server
```

**解决方案：**
1. 确保目标服务已注册到 Eureka
2. 检查服务名称是否正确
3. 验证网络连接

### 8.4 数据库连接问题

**问题现象：**
```
Failed to configure a DataSource
```

**解决方案：**
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
```

---

## 🎯 总结与展望

通过本教程，你已经学会了：

✅ **核心概念**
- 微服务架构原理
- 服务注册与发现机制
- 服务间通信模式

✅ **技术实现**
- Eureka Server 搭建
- Spring Boot 微服务开发
- OpenFeign 服务调用
- Actuator 健康监控

✅ **部署运维**
- Docker 容器化
- 服务编排
- 故障排查

**下一步学习方向：**
- API Gateway (Spring Cloud Gateway)
- 配置中心 (Spring Cloud Config)
- 断路器 (Circuit Breaker)
- 分布式链路追踪 (Sleuth + Zipkin)

---

## 📖 参考资料

- [Spring Cloud 官方文档](https://spring.io/projects/spring-cloud)
- [Spring Boot 官方指南](https://spring.io/guides)
- [Docker 官方文档](https://docs.docker.com/)

---

**🌟 恭喜完成 Spring Cloud 微服务实战教程！继续探索微服务的无限可能！** 