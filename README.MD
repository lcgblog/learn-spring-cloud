# Spring Cloud Learning Framework
## E-commerce Marketplace Implementation

> **Business Context**: Building "ShopHub" - a multi-vendor e-commerce marketplace with separate services for users, products, orders, payments, notifications, and vendor management.

---

## 🎯 Learning Objectives
By the end of this framework, you'll build a production-ready microservices architecture using Spring Cloud to handle:
- **100k+ products** across multiple vendors
- **High-traffic scenarios** (Black Friday, flash sales)
- **Multi-region deployments**
- **Real-time inventory updates**
- **Payment processing workflows**

---

## 📋 Prerequisites Checklist
- [ ] Java 17+ installed
- [ ] Spring Boot 3.x experience
- [ ] Docker & Docker Compose
- [ ] Maven/Gradle familiarity
- [ ] Basic REST API knowledge
- [ ] Git version control

---

## 🗺️ Learning Path (8-Week Plan)

### **Week 1-2: Foundation & Service Discovery**
**Spring Cloud Components**: Eureka Server, Eureka Client

**Business Scenario**: Setting up core services that need to find each other dynamically

**Hands-on Project**: 
- Create Eureka Server (Service Registry)
- Build 3 core services:
  - `user-service` (user management, authentication)
  - `product-service` (product catalog, search)
  - `order-service` (order processing, cart management)

**Specific Tasks**:
- [x] Set up Eureka Server on port 8761
- [x] Register all services with Eureka
- [x] Implement health checks for each service
- [x] Create service-to-service communication (user-service → product-service)
- [x] Test service discovery with multiple instances

**✅ 实现完成详情**:

### 🏢 Eureka Server 配置
- **端口**: 8761
- **功能**: 服务注册中心，提供服务发现能力
- **配置亮点**: 
  - 开发环境关闭自我保护模式，便于测试
  - 自定义清理间隔时间 (5秒)
  - 详细的健康检查和日志配置
- **访问地址**: http://localhost:8761

### 🔗 服务注册配置
所有微服务都成功注册到 Eureka：

**👥 User Service (端口 8081)**:
- ✅ @EnableDiscoveryClient + @EnableFeignClients
- ✅ H2内存数据库，带用户管理功能
- ✅ 完整的 RESTful API (CRUD操作)
- ✅ Feign客户端调用产品服务

**📦 Product Service (端口 8082)**:
- ✅ @EnableDiscoveryClient 
- ✅ 产品目录管理，支持搜索和筛选
- ✅ 模拟5个产品数据 (Apple、Samsung、Lenovo等)
- ✅ 服务间通信接口 (/api/products/{id}/exists)

**📋 Order Service (端口 8083)**:
- ✅ @EnableDiscoveryClient + @EnableFeignClients
- ✅ 订单管理系统，支持状态跟踪
- ✅ 订单统计功能和报表接口
- ✅ 模拟订单数据，包含多种状态

### 🏥 健康检查实现
- **Spring Boot Actuator**: 所有服务启用健康检查端点
- **监控端点**: `/actuator/health`, `/actuator/info`, `/actuator/metrics`
- **详细状态**: 显示数据库连接、Eureka注册状态等
- **自定义健康检查**: 各服务提供业务层面的健康状态

### 🔄 服务间通信
**通信模式**: User Service → Product Service
- **技术**: Spring Cloud OpenFeign
- **接口**: `/api/users/check-product/{productId}`
- **功能**: 用户服务调用产品服务验证产品存在性
- **容错**: 包含异常处理，服务不可用时优雅降级

### 🧪 测试验证
创建了自动化测试脚本 `test-service-discovery.sh`:
- ✅ 服务启动状态检查
- ✅ 健康检查验证
- ✅ Eureka注册状态确认  
- ✅ 服务间通信测试
- ✅ API功能完整性测试
- ✅ 服务统计信息展示

**运行测试**:
```bash
./test-service-discovery.sh
```

### 📊 核心指标
- **注册服务数**: 3个 (user-service, product-service, order-service)
- **服务发现时间**: < 30秒
- **健康检查频率**: 每10秒
- **服务下线检测**: 30秒超时
- **模拟数据**: 用户、产品、订单完整业务数据

**Real-world Learning**: 模拟供应商入驻场景，新的产品服务自动注册并可被其他服务发现和调用

---

### **Week 3: API Gateway & Routing**
**Spring Cloud Components**: Spring Cloud Gateway

**Business Scenario**: Single entry point for mobile app, web frontend, and partner APIs

**Hands-on Project**:
- Implement API Gateway as the single entry point
- Route requests to appropriate microservices
- Add rate limiting for different user tiers (premium vs. regular customers)

**Specific Tasks**:
- [ ] Set up Spring Cloud Gateway on port 8080
- [ ] Configure routing rules:
  - `/api/users/**` → user-service
  - `/api/products/**` → product-service  
  - `/api/orders/**` → order-service
- [ ] Implement custom filters for request logging
- [ ] Add rate limiting (100 req/min for regular, 1000 req/min for premium)
- [ ] Set up CORS for web frontend

**Real-world Learning**: Handle traffic spikes during flash sales with intelligent routing

---

### **Week 4: Load Balancing & Client-Side Discovery**
**Spring Cloud Components**: Spring Cloud LoadBalancer, OpenFeign

**Business Scenario**: Distribute load across multiple instances during high traffic

**Hands-on Project**:
- Scale services horizontally
- Implement client-side load balancing
- Add Feign clients for clean service-to-service communication

**Specific Tasks**:
- [ ] Configure multiple instances of product-service (ports 8081, 8082, 8083)
- [ ] Implement custom load balancing strategy for product search
- [ ] Create Feign client in order-service to call product-service
- [ ] Add retry logic with exponential backoff
- [ ] Implement custom load balancer for vendor-specific routing

**Real-world Learning**: Handle vendor-specific product queries efficiently

---

### **Week 5: Configuration Management**
**Spring Cloud Components**: Spring Cloud Config Server, Config Client

**Business Scenario**: Manage configurations across environments (dev, staging, prod) and feature toggles

**Hands-on Project**:
- Centralize configuration management
- Implement feature toggles for new marketplace features
- Manage environment-specific settings

**Specific Tasks**:
- [ ] Set up Config Server with Git backend
- [ ] Move all application.yml to centralized config repo
- [ ] Implement feature toggles:
  - `feature.recommendations.enabled`
  - `feature.realtime-inventory.enabled`
  - `feature.multi-currency.enabled`
- [ ] Configure different payment gateways per environment
- [ ] Add configuration refresh without restart (@RefreshScope)

**Real-world Learning**: Deploy new features gradually with feature toggles

---

### **Week 6: Circuit Breaker & Resilience**
**Spring Cloud Components**: Spring Cloud Circuit Breaker (Resilience4j)

**Business Scenario**: Handle payment service outages and external API failures gracefully

**Hands-on Project**:
- Implement circuit breakers for external dependencies
- Add fallback mechanisms for critical user journeys
- Monitor and alert on circuit breaker state changes

**Specific Tasks**:
- [ ] Add circuit breaker to payment-service calls
- [ ] Implement fallback for product recommendations (show popular items)
- [ ] Configure bulkhead pattern for different operation types
- [ ] Add retry mechanism for inventory updates
- [ ] Implement timeout configurations for external APIs
- [ ] Create health indicators for circuit breaker states

**Real-world Learning**: Ensure checkout process works even when recommendation service fails

---

### **Week 7: Distributed Tracing & Monitoring**
**Spring Cloud Components**: Spring Cloud Sleuth, Micrometer

**Business Scenario**: Debug performance issues across the order fulfillment workflow

**Hands-on Project**:
- Implement distributed tracing
- Add custom metrics for business KPIs
- Set up monitoring dashboards

**Specific Tasks**:
- [ ] Add Spring Cloud Sleuth to all services
- [ ] Configure Zipkin for trace visualization
- [ ] Implement custom metrics:
  - Order completion rate
  - Average product search time
  - Payment processing duration
- [ ] Add correlation IDs for order tracking
- [ ] Set up Micrometer with Prometheus
- [ ] Create Grafana dashboards for business metrics

**Real-world Learning**: Track user journey from product search to order completion

---

### **Week 8: Security & Advanced Patterns**
**Spring Cloud Components**: Spring Cloud Security, Spring Cloud Stream

**Business Scenario**: Secure multi-tenant marketplace with real-time notifications

**Hands-on Project**:
- Implement OAuth2 security across services
- Add event-driven architecture for real-time updates
- Deploy to cloud environment

**Specific Tasks**:
- [ ] Set up OAuth2 authorization server
- [ ] Implement JWT token propagation between services
- [ ] Add role-based access (CUSTOMER, VENDOR, ADMIN)
- [ ] Implement Spring Cloud Stream for order events
- [ ] Create event-driven inventory updates
- [ ] Add message queues (RabbitMQ) for order processing
- [ ] Deploy entire system to AWS/Azure using Docker

**Real-world Learning**: Handle multi-vendor permissions and real-time inventory sync

---

## 🛠️ Development Environment Setup

### **Required Tools**
```bash
# Core tools
java --version  # Java 17+
docker --version
docker-compose --version

# Optional but recommended
kubectl version  # For Kubernetes deployment
terraform --version  # For infrastructure as code
```

### **Project Structure**
```
shophub-microservices/
├── eureka-server/
├── api-gateway/
├── config-server/
├── user-service/
├── product-service/
├── order-service/
├── payment-service/
├── notification-service/
├── vendor-service/
├── docker-compose.yml
└── kubernetes/
```

---

## 📊 Success Metrics & Checkpoints

### **Weekly Checkpoints**
- **Week 2**: All services register with Eureka, can communicate
- **Week 3**: API Gateway routes all requests correctly
- **Week 4**: Load balancing works under simulated load
- **Week 5**: Configuration changes without code deployment
- **Week 6**: Circuit breakers activate during simulated failures
- **Week 7**: End-to-end request tracing visible in Zipkin
- **Week 8**: Full security implementation with multi-tenant support

### **Final Project Validation**
- [ ] Handle 1000 concurrent users
- [ ] 99.9% uptime during payment service restart
- [ ] Sub-200ms response time for product search
- [ ] Successful order processing during partial service failures
- [ ] Real-time inventory updates across all vendors

---

## 🔗 Key Resources

### **Official Documentation**
- [Spring Cloud Reference Guide](https://docs.spring.io/spring-cloud/docs/current/reference/html/)
- [Spring Cloud Examples](https://github.com/spring-cloud-samples)

### **Hands-on Labs**
- [Spring Cloud Workshop](https://github.com/spring-cloud/spring-cloud-workshop)
- [Microservices with Spring Boot](https://spring.io/guides/gs/centralized-configuration/)

### **Advanced Topics**
- Spring Cloud Kubernetes integration
- Service Mesh with Istio + Spring Cloud
- Multi-cloud deployment strategies

---

## 🎯 Next Steps After Completion
1. **Advanced Patterns**: Event sourcing, CQRS with Spring Cloud
2. **Performance Optimization**: Caching strategies, database sharding
3. **Production Readiness**: Monitoring, alerting, chaos engineering
4. **Certification**: Spring Professional certification

---

*This framework emphasizes hands-on learning with a realistic business context that naturally requires all major Spring Cloud features. Each week builds upon the previous, creating a comprehensive e-commerce marketplace by the end.*