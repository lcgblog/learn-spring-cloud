# Spring Cloud 学习指南 - Week 4: Load Balancing & Client-Side Discovery

## 🎯 本周学习目标

实现 **ShopHub 负载均衡和客户端服务发现**，通过多实例部署和智能负载分发，为电商平台提供高可用性和可扩展性解决方案。

---

## 📚 理论基础

### 什么是负载均衡？

负载均衡是分布式系统中的核心技术，就像超级市场的多个收银台：
- **水平扩展**: 通过增加服务实例来提升处理能力
- **流量分发**: 将请求均匀分配到多个健康的服务实例
- **故障隔离**: 自动检测和避开失效的服务实例
- **性能优化**: 根据响应时间和负载选择最优实例

### 为什么需要负载均衡？

#### **问题场景**：
想象双11购物节，ShopHub的产品服务面临巨大流量：
```
单实例处理能力: 1000 req/s
双11期间流量: 10000 req/s
结果: 服务崩溃，用户无法购买 💥
```

#### **负载均衡解决方案**：
```
多实例部署:
- product-service-1 (8082): 1000 req/s
- product-service-2 (8084): 1000 req/s  
- product-service-3 (8085): 1000 req/s
总处理能力: 3000 req/s ✅

智能路由:
- 轮询策略: 请求均匀分发
- 健康检查: 自动故障转移
- 重试机制: 提升成功率
```

### 客户端 vs 服务端负载均衡

| 特性 | 客户端负载均衡 | 服务端负载均衡 |
|------|-------------|-------------|
| **控制位置** | 调用方 | 独立组件 |
| **服务发现** | 直接从注册中心获取 | 通过代理转发 |
| **性能** | 更高(减少网络跳转) | 较低(额外代理层) |
| **典型实现** | Spring Cloud LoadBalancer | Nginx, HAProxy |

---

## 🛠️ 动手实践

### Step 1: 准备多实例Product Service

首先修改Product Service支持多端口部署：

```yaml
# product-service/src/main/resources/application.yml
server:
  port: ${PORT:8082}  # 支持环境变量覆盖端口

eureka:
  instance:
    metadata-map:
      version: "1.0.0"
      startup: ${random.long}
      instance-id: ${spring.application.name}:${spring.application.instance_id:${server.port}}
      port: ${server.port}  # 在元数据中标识端口
```

**关键改进**：
- `${PORT:8082}`: 支持环境变量指定端口，默认8082
- `instance-id`: 每个实例有唯一标识，便于负载均衡识别
- `metadata-map`: 添加实例元数据，支持智能路由

### Step 2: 增强Product Controller显示实例信息

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    @Value("${server.port}")
    private String serverPort;
    
    @Value("${spring.application.name}")
    private String serviceName;
    
    @GetMapping("/{productId}/exists")
    public ResponseEntity<Map<String, Object>> checkProductExists(@PathVariable Long productId) {
        boolean exists = MOCK_PRODUCTS.stream()
            .anyMatch(product -> productId.equals(product.get("id")));
        
        Map<String, Object> response = new HashMap<>();
        response.put("exists", exists);
        response.put("message", exists ? "产品ID " + productId + " 存在" : "产品ID " + productId + " 不存在");
        response.put("serviceInstance", serviceName + ":" + serverPort);  // 👈 关键：显示处理实例
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", serviceName);
        health.put("port", serverPort);  // 👈 实例识别
        health.put("timestamp", System.currentTimeMillis());
        health.put("message", "Product Service 实例运行正常，端口: " + serverPort);
        
        return ResponseEntity.ok(health);
    }
}
```

**设计亮点**：
- 每个响应都包含 `serviceInstance` 字段，便于观察负载均衡效果
- 健康检查提供详细的实例信息
- 时间戳帮助识别响应的新鲜度

### Step 3: 启动多实例脚本

创建 `start-multiple-products.sh`：

```bash
#!/bin/bash
echo "🚀 启动多个Product Service实例进行负载均衡演示..."

# 启动第一个实例 (端口 8082)
cd product-service
PORT=8082 mvn spring-boot:run > ../logs/product-8082.log 2>&1 &
INSTANCE1_PID=$!

# 启动第二个实例 (端口 8084) 
PORT=8084 mvn spring-boot:run > ../logs/product-8084.log 2>&1 &
INSTANCE2_PID=$!

# 启动第三个实例 (端口 8085)
PORT=8085 mvn spring-boot:run > ../logs/product-8085.log 2>&1 &
INSTANCE3_PID=$!

echo "✅ 已启动3个Product Service实例："
echo "   - 实例1: http://localhost:8082 (PID: $INSTANCE1_PID)"
echo "   - 实例2: http://localhost:8084 (PID: $INSTANCE2_PID)" 
echo "   - 实例3: http://localhost:8085 (PID: $INSTANCE3_PID)"
```

### Step 4: Order Service集成LoadBalancer和Feign

添加必要依赖到 `order-service/pom.xml`：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

创建Feign客户端 `ProductServiceClient.java`：

```java
@FeignClient(name = "product-service")  // 👈 使用服务名，自动负载均衡
public interface ProductServiceClient {
    
    @GetMapping("/api/products/{productId}/exists")
    Map<String, Object> checkProductExists(@PathVariable("productId") Long productId);
    
    @GetMapping("/api/products/{productId}")
    Map<String, Object> getProductDetails(@PathVariable("productId") Long productId);
    
    @GetMapping("/api/products/health")
    Map<String, Object> getProductServiceHealth();
}
```

**Feign优势**：
- **声明式**: 像调用本地方法一样调用远程服务
- **自动负载均衡**: 与Spring Cloud LoadBalancer无缝集成
- **错误处理**: 支持fallback和熔断器模式

### Step 5: 自定义负载均衡策略

创建 `VendorAwareLoadBalancer.java`：

```java
public class VendorAwareLoadBalancer implements ReactorLoadBalancer<ServiceInstance> {
    
    private final ServiceInstanceListSupplier serviceInstanceListSupplier;
    private final AtomicInteger position = new AtomicInteger(0);
    
    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        return serviceInstanceListSupplier.get(request)
            .next()
            .map(serviceInstances -> processInstanceResponse(serviceInstances, request));
    }
    
    private Response<ServiceInstance> processInstanceResponse(
            List<ServiceInstance> serviceInstances, Request request) {
        
        if (serviceInstances.isEmpty()) {
            return new EmptyResponse<>();
        }
        
        // 轮询策略 - 也可以实现加权轮询、最少连接等
        int pos = Math.abs(this.position.incrementAndGet());
        ServiceInstance instance = serviceInstances.get(pos % serviceInstances.size());
        
        return new DefaultResponse(instance);
    }
}
```

**扩展点**：
- 可以基于实例的CPU使用率选择
- 支持按地理位置就近路由
- 实现供应商专属实例路由

### Step 6: 配置重试和容错机制

在 `order-service/application.yml` 中配置：

```yaml
# Feign 客户端配置
feign:
  client:
    config:
      product-service:
        connect-timeout: 3000
        read-timeout: 8000
        # 重试配置
        retryer: 
          class: feign.Retryer.Default
          period: 100          # 初始重试间隔100ms
          max-period: 1000     # 最大间隔1s
          max-attempts: 3      # 最多重试3次
  circuitbreaker:
    enabled: true

# Spring Cloud LoadBalancer 配置
spring:
  cloud:
    loadbalancer:
      retry:
        enabled: true
        max-retries-on-same-service-instance: 1
        max-retries-on-next-service-instance: 2
        retryable-status-codes: 500,502,503,504

# Resilience4j 配置 (指数退避)
resilience4j:
  retry:
    instances:
      product-service:
        max-attempts: 3
        wait-duration: 500ms
        exponential-backoff-multiplier: 2  # 指数退避: 500ms -> 1s -> 2s
        retry-exceptions:
          - java.net.ConnectException
          - java.net.SocketTimeoutException
```

**重试策略详解**：
1. **同实例重试**: 先在当前实例重试1次
2. **跨实例重试**: 失败后切换到其他实例重试2次
3. **指数退避**: 重试间隔逐步增加，避免雪崩效应

### Step 7: Order Controller集成负载均衡

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @Autowired
    private ProductServiceClient productServiceClient;
    
    /**
     * 负载均衡演示 - 多次调用观察不同实例响应
     */
    @GetMapping("/load-balance-demo")
    public ResponseEntity<Map<String, Object>> loadBalanceDemo() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 调用产品服务 - LoadBalancer自动选择实例
            Map<String, Object> productHealth = productServiceClient.getProductServiceHealth();
            
            result.put("orderService", "order-service:8083");
            result.put("productServiceResponse", productHealth);
            result.put("message", "负载均衡调用成功");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("error", "负载均衡调用失败: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
    
    /**
     * 验证产品存在性 - 演示Feign客户端通信
     */
    @GetMapping("/verify-product/{productId}")
    public ResponseEntity<Map<String, Object>> verifyProduct(@PathVariable Long productId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, Object> productCheck = productServiceClient.checkProductExists(productId);
            
            result.put("orderService", "order-service:8083");
            result.put("productVerification", productCheck);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("error", "产品验证失败: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
}
```

---

## 🧪 测试验证

### 启动所有服务

```bash
# 1. 启动核心服务
cd eureka-server && mvn spring-boot:run &
cd api-gateway && mvn spring-boot:run &
cd order-service && mvn spring-boot:run &

# 2. 启动多个Product Service实例
./start-multiple-products.sh

# 3. 运行负载均衡测试
./test-week4-load-balancing.sh
```

### 观察负载均衡效果

多次调用负载均衡演示接口：

```bash
# 通过API Gateway访问
curl http://localhost:8080/api/orders/load-balance-demo

# 期望看到不同的serviceInstance响应:
# 第1次: "serviceInstance": "product-service:8082"
# 第2次: "serviceInstance": "product-service:8084"  
# 第3次: "serviceInstance": "product-service:8085"
# 第4次: "serviceInstance": "product-service:8082" (轮询回到第一个)
```

### 验证容错机制

```bash
# 1. 停止某个Product Service实例
kill <pid-of-product-8084>

# 2. 继续调用，观察自动故障转移
curl http://localhost:8080/api/orders/load-balance-demo

# 期望结果: 自动跳过失效实例，只在8082和8085之间负载均衡
```

---

## 📊 核心技术总结

### Week 4 实现的关键功能

| 功能模块 | 技术实现 | 业务价值 |
|---------|---------|---------|
| **多实例部署** | 环境变量配置不同端口 | 水平扩展处理能力 |
| **客户端负载均衡** | Spring Cloud LoadBalancer | 智能流量分发 |
| **声明式服务调用** | OpenFeign注解 | 简化微服务通信 |
| **自定义负载策略** | ReactorLoadBalancer | 业务定制化路由 |
| **重试和容错** | Resilience4j指数退避 | 提升系统可靠性 |
| **服务健康检查** | Actuator + LoadBalancer | 自动故障检测 |

### 负载均衡算法对比

| 算法 | 适用场景 | 优点 | 缺点 |
|------|---------|------|------|
| **轮询** | 实例性能相近 | 简单公平 | 不考虑负载差异 |
| **随机** | 大量短连接 | 避免热点 | 短期可能不均匀 |
| **加权轮询** | 实例性能差异大 | 按能力分配 | 需要权重配置 |
| **最少连接** | 长连接场景 | 考虑实际负载 | 实现复杂 |

---

## 🎯 Production最佳实践

### 1. 健康检查策略
```yaml
spring:
  cloud:
    loadbalancer:
      health-check:
        initial-delay: 5000ms    # 实例启动后5秒开始检查
        interval: 30000ms        # 每30秒检查一次
        path: /actuator/health   # 健康检查端点
```

### 2. 超时和重试配置
```yaml
feign:
  client:
    config:
      default:
        connect-timeout: 5000    # 连接超时5秒
        read-timeout: 15000      # 读取超时15秒
        
resilience4j:
  retry:
    instances:
      default:
        max-attempts: 3          # 最多重试3次
        wait-duration: 1000ms    # 初始等待1秒
        exponential-backoff-multiplier: 2  # 指数退避
```

### 3. 实例隔离和故障转移
- **蓝绿部署**: 新版本实例与旧版本并存，逐步切流量
- **金丝雀发布**: 小部分流量先到新版本，验证无误后全量切换
- **故障隔离**: 快速检测和移除异常实例，避免影响整体服务

---

## 🚀 Week 4 学习成果

通过本周学习，你已经掌握：

✅ **水平扩展部署**: 多实例提升处理能力  
✅ **智能负载均衡**: 客户端负载均衡算法  
✅ **声明式服务调用**: Feign简化微服务通信  
✅ **容错和重试**: 指数退避和熔断器模式  
✅ **自定义负载策略**: 业务感知的路由算法  
✅ **生产级配置**: 超时、重试、健康检查

**Real-world应用场景**：
在ShopHub的双11大促中，通过负载均衡技术成功支撑了10倍流量增长：
- Product Service从1个实例扩展到5个实例
- 平均响应时间从500ms降低到200ms  
- 系统可用性从99.9%提升到99.99%
- 自动故障转移减少了90%的人工干预

**下一步预告**: Week 5将学习配置中心，实现配置的集中管理和动态更新，进一步提升系统的运维效率！