# Week 6: Circuit Breaker & Resilience - 实现教程

## 🎯 学习目标

通过本周的学习，你将掌握：
- Spring Cloud Circuit Breaker (Resilience4j) 的完整实现
- 熔断器、重试、超时、舱壁等韧性模式
- 服务降级和故障恢复策略
- 多支付网关的熔断器保护实现
- 推荐系统的韧性架构设计
- 熔断器监控和运维实践

---

## 📚 理论基础

### 1. 韧性模式 (Resilience Patterns)

#### 🛡️ 熔断器模式 (Circuit Breaker)
**核心思想**: 监控服务调用的失败率，当失败率超过阈值时自动"熔断"，避免故障级联传播。

**三种状态**:
- **CLOSED (关闭)**: 正常状态，允许请求通过
- **OPEN (开启)**: 熔断状态，直接返回错误或降级响应
- **HALF-OPEN (半开)**: 测试状态，允许少量请求验证服务是否恢复

#### 🔄 重试模式 (Retry)
**应用场景**: 处理瞬时故障，如网络抖动、临时服务过载
**关键配置**: 最大重试次数、重试间隔、指数退避

#### ⏱️ 超时控制 (Timeout)
**目的**: 避免长时间等待，快速失败并释放资源
**策略**: 不同服务类型设置不同的超时时间

#### 🏗️ 舱壁模式 (Bulkhead)
**隔离思想**: 将系统资源隔离，防止一个服务的问题影响其他服务
**实现方式**: 线程池隔离、信号量隔离

---

## 🛠️ 实现步骤

### Step 1: 项目依赖配置

#### Maven依赖添加
```xml
<!-- Resilience4j Circuit Breaker -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>

<!-- Resilience4j Spring Boot Actuator -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>

<!-- Resilience4j Micrometer (可选，用于指标收集) -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-micrometer</artifactId>
</dependency>
```

### Step 2: 创建Payment Service

#### 2.1 支付服务主类
```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
```

#### 2.2 支付实体设计
```java
@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false)
    private PaymentMethod method;
    
    // 其他字段...
    
    public enum PaymentStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED, REFUNDED
    }
    
    public enum PaymentMethod {
        CREDIT_CARD, DEBIT_CARD, PAYPAL, STRIPE, ALIPAY, WECHAT_PAY
    }
}
```

#### 2.3 模拟支付网关
```java
@Service
public class StripePaymentGateway {
    private final Random random = new Random();
    private int callCount = 0;
    
    public PaymentResult processPayment(Long orderId, BigDecimal amount, String currency) {
        callCount++;
        
        // 模拟网络延迟
        simulateNetworkDelay();
        
        // 模拟不同的失败场景 (40%失败率)
        int scenario = callCount % 10;
        switch (scenario) {
            case 1, 2: // 20% 网络超时
                throw new PaymentTimeoutException("Stripe payment gateway timeout");
            case 3: // 10% 服务不可用
                throw new PaymentServiceUnavailableException("Stripe service temporarily unavailable");
            case 4: // 10% 支付被拒绝
                return new PaymentResult(false, null, "Payment declined by bank");
            default: // 60% 成功
                String transactionId = "stripe_" + UUID.randomUUID().toString().substring(0, 8);
                return new PaymentResult(true, transactionId, "Payment successful");
        }
    }
    
    private void simulateNetworkDelay() {
        try {
            Thread.sleep(random.nextInt(1000) + 500); // 0.5-1.5秒延迟
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### Step 3: 实现熔断器保护的支付服务

#### 3.1 支付服务类
```java
@Service
public class PaymentService {
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private StripePaymentGateway stripeGateway;
    
    @Autowired
    private PaypalPaymentGateway paypalGateway;
    
    /**
     * 使用Stripe处理支付 - 包含完整的韧性模式
     */
    @CircuitBreaker(name = "stripe-payment", fallbackMethod = "fallbackStripePayment")
    @Retry(name = "stripe-payment")
    @TimeLimiter(name = "stripe-payment")
    @Bulkhead(name = "stripe-payment", type = Bulkhead.Type.THREADPOOL)
    public CompletableFuture<Payment> processStripePayment(Long orderId, Long userId, 
                                                           BigDecimal amount, String currency) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Processing Stripe payment for order: {}, amount: {}", orderId, amount);
            
            // 创建支付记录
            Payment payment = new Payment(orderId, userId, amount, Payment.PaymentMethod.STRIPE, "stripe");
            payment.setStatus(Payment.PaymentStatus.PROCESSING);
            payment = paymentRepository.save(payment);
            
            try {
                // 调用Stripe API
                StripePaymentGateway.PaymentResult result = stripeGateway.processPayment(orderId, amount, currency);
                
                if (result.isSuccess()) {
                    payment.setStatus(Payment.PaymentStatus.COMPLETED);
                    payment.setTransactionId(result.getTransactionId());
                } else {
                    payment.setStatus(Payment.PaymentStatus.FAILED);
                    payment.setFailureReason(result.getMessage());
                }
                
            } catch (Exception e) {
                payment.setStatus(Payment.PaymentStatus.FAILED);
                payment.setFailureReason(e.getMessage());
                throw e; // 重新抛出异常以触发重试和熔断器
            }
            
            return paymentRepository.save(payment);
        });
    }
    
    /**
     * Stripe支付降级方法 - 自动切换到PayPal
     */
    public CompletableFuture<Payment> fallbackStripePayment(Long orderId, Long userId, 
                                                           BigDecimal amount, String currency, Exception ex) {
        logger.warn("Stripe payment failed, falling back to PayPal. Error: {}", ex.getMessage());
        return processPaypalPayment(orderId, userId, amount, currency);
    }
}
```

#### 3.2 关键注解说明
- `@CircuitBreaker`: 熔断器保护，指定降级方法
- `@Retry`: 重试机制，自动重试失败的调用
- `@TimeLimiter`: 超时控制，防止长时间等待
- `@Bulkhead`: 舱壁模式，线程池隔离

### Step 4: 配置Resilience4j

#### 4.1 应用配置文件
```yaml
# Resilience4j配置
resilience4j:
  # 熔断器配置
  circuitbreaker:
    instances:
      stripe-payment:
        register-health-indicator: true
        sliding-window-type: count_based
        sliding-window-size: 10              # 滑动窗口大小
        minimum-number-of-calls: 5           # 最小调用次数
        permitted-number-of-calls-in-half-open-state: 3
        failure-rate-threshold: 50           # 失败率阈值 50%
        wait-duration-in-open-state: 10s     # 熔断器开启等待时间
        automatic-transition-from-open-to-half-open-enabled: true
        record-exceptions:                   # 记录的异常类型
          - com.shophub.payment.service.external.StripePaymentGateway$PaymentTimeoutException
          - java.util.concurrent.TimeoutException
          - java.io.IOException
        ignore-exceptions:                   # 忽略的异常类型
          - java.lang.IllegalArgumentException
      
      paypal-payment:
        register-health-indicator: true
        sliding-window-size: 10
        minimum-number-of-calls: 5
        failure-rate-threshold: 60           # PayPal容忍更高的失败率
        wait-duration-in-open-state: 15s
  
  # 重试配置
  retry:
    instances:
      stripe-payment:
        max-attempts: 3                      # 最大重试次数
        wait-duration: 1s                    # 重试间隔
        exponential-backoff-multiplier: 2    # 指数退避倍数
        retry-exceptions:
          - com.shophub.payment.service.external.StripePaymentGateway$PaymentTimeoutException
          - java.util.concurrent.TimeoutException
        ignore-exceptions:
          - java.lang.IllegalArgumentException
  
  # 超时控制配置
  timelimiter:
    instances:
      stripe-payment:
        timeout-duration: 3s                 # 3秒超时
        cancel-running-future: true
      
      paypal-payment:
        timeout-duration: 5s                 # PayPal允许更长时间
  
  # 舱壁模式配置
  bulkhead:
    instances:
      stripe-payment:
        max-concurrent-calls: 5              # 最大并发调用数
        max-wait-duration: 1s
  
  # 线程池舱壁配置
  thread-pool-bulkhead:
    instances:
      stripe-payment:
        max-thread-pool-size: 4
        core-thread-pool-size: 2
        queue-capacity: 20
        keep-alive-duration: 20ms
```

### Step 5: 产品推荐服务韧性升级

#### 5.1 推荐服务实现
```java
@Service
public class ProductRecommendationService {
    
    /**
     * 获取个性化推荐 - 使用熔断器保护
     */
    @CircuitBreaker(name = "recommendation-service", fallbackMethod = "fallbackRecommendations")
    @Retry(name = "recommendation-service")
    @TimeLimiter(name = "recommendation-service")
    @Bulkhead(name = "recommendation-service", type = Bulkhead.Type.THREADPOOL)
    public CompletableFuture<List<Map<String, Object>>> getPersonalizedRecommendations(
            Long userId, String category) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Fetching personalized recommendations for user: {}, category: {}", userId, category);
            
            // 模拟网络延迟和故障
            simulateNetworkDelay();
            simulateFailureScenarios();
            
            // 个性化推荐算法
            return generateRecommendations(userId, category, 5);
        });
    }
    
    /**
     * 个性化推荐的降级方法 - 切换到热门商品
     */
    public CompletableFuture<List<Map<String, Object>>> fallbackRecommendations(
            Long userId, String category, Exception ex) {
        logger.warn("Personalized recommendations failed, falling back to popular products. Error: {}", ex.getMessage());
        return getPopularProducts(category, 3);
    }
    
    /**
     * 获取热门商品推荐 - 作为降级方案
     */
    @CircuitBreaker(name = "popular-products", fallbackMethod = "fallbackPopularProducts")
    public CompletableFuture<List<Map<String, Object>>> getPopularProducts(String category, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            // 更稳定的服务，10%失败率
            if (random.nextInt(10) == 0) {
                throw new RuntimeException("Popular products service temporarily unavailable");
            }
            
            return getAllProducts().stream()
                .filter(product -> category == null || category.equals(product.get("category")))
                .sorted((p1, p2) -> Double.compare((Double) p2.get("price"), (Double) p1.get("price")))
                .limit(limit)
                .toList();
        });
    }
    
    /**
     * 热门商品的最终降级方法 - 静态数据
     */
    public CompletableFuture<List<Map<String, Object>>> fallbackPopularProducts(
            String category, int limit, Exception ex) {
        logger.warn("Popular products service failed, using static fallback");
        
        return CompletableFuture.supplyAsync(() -> {
            // 返回静态的推荐数据
            List<Map<String, Object>> fallbackProducts = Arrays.asList(
                createProduct(1L, "iPhone 15 Pro", "Apple", 8999.00, "smartphone"),
                createProduct(2L, "MacBook Pro M3", "Apple", 15999.00, "laptop"),
                createProduct(5L, "iPad Air", "Apple", 4299.00, "tablet")
            );
            
            return fallbackProducts.stream()
                .filter(product -> category == null || category.equals(product.get("category")))
                .limit(limit)
                .toList();
        });
    }
}
```

### Step 6: Order Service集成支付服务

#### 6.1 Feign客户端定义
```java
@FeignClient(
    name = "payment-service",
    fallback = PaymentServiceFallback.class
)
public interface PaymentServiceClient {
    
    @PostMapping("/api/payments/process")
    ResponseEntity<Map<String, Object>> processPayment(@RequestBody PaymentRequest request);
    
    @PostMapping("/api/payments/process/paypal")
    ResponseEntity<Map<String, Object>> processPaypalPayment(@RequestBody PaymentRequest request);
}
```

#### 6.2 Feign降级处理
```java
@Component
public class PaymentServiceFallback implements PaymentServiceClient {
    
    @Override
    public ResponseEntity<Map<String, Object>> processPayment(PaymentRequest request) {
        logger.warn("Payment service unavailable, using fallback for order: {}", request.getOrderId());
        
        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("status", "PENDING");
        fallbackResponse.put("message", "Payment service unavailable - payment queued for retry");
        fallbackResponse.put("fallback", true);
        
        return ResponseEntity.ok(fallbackResponse);
    }
}
```

#### 6.3 订单支付处理
```java
@RestController
public class OrderController {
    
    @Autowired
    private PaymentServiceClient paymentServiceClient;
    
    /**
     * 处理订单支付 - 使用熔断器保护
     */
    @PostMapping("/{orderId}/payment")
    @CircuitBreaker(name = "payment-service", fallbackMethod = "fallbackPayment")
    @Retry(name = "payment-service")
    public ResponseEntity<Map<String, Object>> processOrderPayment(
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> paymentRequest) {
        
        // 查找订单信息
        Map<String, Object> order = findOrderById(orderId);
        
        // 准备支付请求
        PaymentServiceClient.PaymentRequest paymentReq = new PaymentServiceClient.PaymentRequest(
            orderId,
            (Long) order.get("userId"),
            BigDecimal.valueOf((Double) order.get("totalAmount")),
            "USD"
        );
        
        // 调用支付服务
        ResponseEntity<Map<String, Object>> paymentResponse = paymentServiceClient.processPayment(paymentReq);
        
        // 构建响应
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", orderId);
        response.put("orderAmount", order.get("totalAmount"));
        response.put("paymentResult", paymentResponse.getBody());
        response.put("message", "订单支付处理完成");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 支付服务降级方法
     */
    public ResponseEntity<Map<String, Object>> fallbackPayment(Long orderId, 
                                                              Map<String, Object> paymentRequest, Exception ex) {
        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("orderId", orderId);
        fallbackResponse.put("status", "PAYMENT_PENDING");
        fallbackResponse.put("message", "支付服务暂时不可用，订单已保存，稍后会自动重试支付");
        fallbackResponse.put("fallback", true);
        fallbackResponse.put("error", ex.getMessage());
        
        return ResponseEntity.ok(fallbackResponse);
    }
}
```

### Step 7: 监控和运维

#### 7.1 健康检查配置
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,circuitbreakers,circuitbreakerevents
  endpoint:
    health:
      show-details: always
  health:
    circuitbreakers:
      enabled: true
```

#### 7.2 熔断器状态监控
```java
@RestController
public class PaymentController {
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    /**
     * 获取熔断器状态监控
     */
    @GetMapping("/circuit-breaker/status")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // Stripe支付熔断器状态
        CircuitBreaker stripePaymentCB = circuitBreakerRegistry.circuitBreaker("stripe-payment");
        status.put("stripePayment", getCircuitBreakerInfo(stripePaymentCB));
        
        // PayPal支付熔断器状态
        CircuitBreaker paypalPaymentCB = circuitBreakerRegistry.circuitBreaker("paypal-payment");
        status.put("paypalPayment", getCircuitBreakerInfo(paypalPaymentCB));
        
        return ResponseEntity.ok(status);
    }
    
    private Map<String, Object> getCircuitBreakerInfo(CircuitBreaker circuitBreaker) {
        Map<String, Object> info = new HashMap<>();
        info.put("state", circuitBreaker.getState().toString());
        info.put("failureRate", circuitBreaker.getMetrics().getFailureRate());
        info.put("numberOfBufferedCalls", circuitBreaker.getMetrics().getNumberOfBufferedCalls());
        info.put("numberOfFailedCalls", circuitBreaker.getMetrics().getNumberOfFailedCalls());
        info.put("numberOfSuccessfulCalls", circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
        return info;
    }
}
```

---

## 🧪 测试和验证

### 测试策略

#### 1. 单元测试
```java
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    
    @Mock
    private StripePaymentGateway stripeGateway;
    
    @Mock
    private PaymentRepository paymentRepository;
    
    @InjectMocks
    private PaymentService paymentService;
    
    @Test
    void shouldRetryOnTimeout() {
        // Given
        when(stripeGateway.processPayment(any(), any(), any()))
            .thenThrow(new PaymentTimeoutException("Timeout"))
            .thenThrow(new PaymentTimeoutException("Timeout"))
            .thenReturn(new PaymentResult(true, "tx123", "Success"));
        
        // When
        CompletableFuture<Payment> result = paymentService.processStripePayment(1L, 1L, new BigDecimal("99.99"), "USD");
        
        // Then
        Payment payment = result.join();
        assertEquals(Payment.PaymentStatus.COMPLETED, payment.getStatus());
        verify(stripeGateway, times(3)).processPayment(any(), any(), any());
    }
}
```

#### 2. 集成测试
```java
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yml")
class CircuitBreakerIntegrationTest {
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    @Test
    void shouldOpenCircuitBreakerAfterFailures() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("stripe-payment");
        
        // 触发多次失败
        for (int i = 0; i < 6; i++) {
            assertThrows(Exception.class, () -> {
                paymentService.processStripePayment(1L, 1L, new BigDecimal("99.99"), "USD").join();
            });
        }
        
        // 验证熔断器状态
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
    }
}
```

#### 3. 端到端测试脚本
```bash
#!/bin/bash
# test-circuit-breaker.sh

echo "Testing Circuit Breaker Behavior..."

# 1. 测试正常支付
echo "Testing normal payment..."
curl -X POST http://localhost:8086/api/payments/process \
  -H "Content-Type: application/json" \
  -d '{"orderId":1,"userId":1,"amount":99.99,"currency":"USD"}'

# 2. 连续调用触发熔断器
echo "Triggering circuit breaker..."
for i in {1..10}; do
  echo "Call $i:"
  curl -s http://localhost:8086/api/payments/demo/circuit-breaker
  sleep 0.5
done

# 3. 检查熔断器状态
echo "Checking circuit breaker status..."
curl http://localhost:8086/api/payments/circuit-breaker/status | jq
```

---

## 🎯 最佳实践

### 1. 熔断器配置策略

#### 服务分类配置
```yaml
# 金融服务 - 更严格的熔断策略
payment-service:
  failure-rate-threshold: 40%    # 更低的失败率阈值
  wait-duration: 20s             # 更长的恢复时间

# 推荐服务 - 更宽松的熔断策略  
recommendation-service:
  failure-rate-threshold: 60%    # 更高的失败率阈值
  wait-duration: 10s             # 更短的恢复时间
```

#### 异常分类处理
```java
// 业务异常 - 不应该触发熔断器
@Component
public class BusinessException extends RuntimeException {
    // 业务逻辑异常，如库存不足、用户不存在等
}

// 技术异常 - 应该触发熔断器
public class TechnicalException extends RuntimeException {
    // 网络异常、超时、服务不可用等
}
```

### 2. 降级策略设计

#### 多层降级
```java
// 第一层：主服务
@CircuitBreaker(name = "primary-service", fallbackMethod = "fallbackToSecondary")
public Data getPrimaryData() { ... }

// 第二层：备用服务
@CircuitBreaker(name = "secondary-service", fallbackMethod = "fallbackToCache")
public Data fallbackToSecondary(Exception ex) { ... }

// 第三层：缓存数据
public Data fallbackToCache(Exception ex) {
    return cacheService.getCachedData();
}
```

#### 用户体验优化
```java
public class GracefulDegradation {
    
    // 推荐服务降级：个性化 → 热门 → 默认
    public List<Product> getRecommendations(Long userId) {
        try {
            return personalizedRecommendationService.getRecommendations(userId);
        } catch (Exception e) {
            try {
                return popularProductService.getPopularProducts();
            } catch (Exception e2) {
                return defaultRecommendations();
            }
        }
    }
}
```

### 3. 监控和告警

#### 关键指标监控
```yaml
# Prometheus指标
resilience4j_circuitbreaker_calls_total
resilience4j_circuitbreaker_state
resilience4j_retry_calls_total
resilience4j_timelimiter_calls_total
```

#### 告警规则
```yaml
# 熔断器开启告警
- alert: CircuitBreakerOpen
  expr: resilience4j_circuitbreaker_state{state="open"} == 1
  for: 30s
  labels:
    severity: warning
  annotations:
    summary: "Circuit breaker {{ $labels.name }} is open"
    
# 失败率过高告警  
- alert: HighFailureRate
  expr: rate(resilience4j_circuitbreaker_calls_total{outcome="failure"}[5m]) > 0.5
  for: 2m
  labels:
    severity: critical
```

---

## 🔍 故障排查

### 常见问题

#### 1. 熔断器未触发
**可能原因**:
- 调用次数未达到最小阈值 `minimum-number-of-calls`
- 异常类型未在 `record-exceptions` 中配置
- 失败率未达到阈值 `failure-rate-threshold`

**解决方法**:
```yaml
resilience4j:
  circuitbreaker:
    instances:
      my-service:
        minimum-number-of-calls: 3     # 降低最小调用次数
        failure-rate-threshold: 30     # 降低失败率阈值
        record-exceptions:             # 确保异常类型正确
          - java.lang.RuntimeException
```

#### 2. 降级方法未执行
**可能原因**:
- 降级方法签名不匹配
- 降级方法未在同一个类中
- `@CircuitBreaker` 注解配置错误

**解决方法**:
```java
// 正确的降级方法签名
@CircuitBreaker(name = "my-service", fallbackMethod = "fallbackMethod")
public String businessMethod(String param) { ... }

// 降级方法必须有相同的参数 + Exception参数
public String fallbackMethod(String param, Exception ex) { ... }
```

#### 3. 重试次数过多
**可能原因**:
- 重试配置不合理
- 没有区分可重试和不可重试异常

**解决方法**:
```yaml
resilience4j:
  retry:
    instances:
      my-service:
        max-attempts: 3                # 合理的重试次数
        retry-exceptions:              # 只重试特定异常
          - java.net.ConnectException
          - java.util.concurrent.TimeoutException
        ignore-exceptions:             # 忽略业务异常
          - com.myapp.BusinessException
```

---

## 📊 性能优化

### 1. 线程池配置优化

```yaml
resilience4j:
  thread-pool-bulkhead:
    instances:
      payment-service:
        max-thread-pool-size: 10      # 基于CPU核心数 * 2
        core-thread-pool-size: 5      # 基于平均并发量
        queue-capacity: 50            # 基于峰值并发量
        keep-alive-duration: 60s      # 线程存活时间
```

### 2. 超时时间调优

```yaml
# 基于服务SLA设置超时时间
resilience4j:
  timelimiter:
    instances:
      fast-service:
        timeout-duration: 1s          # 快速服务
      slow-service:
        timeout-duration: 10s         # 慢速服务
      external-api:
        timeout-duration: 30s         # 外部API
```

### 3. 熔断器参数调优

```yaml
resilience4j:
  circuitbreaker:
    instances:
      critical-service:
        sliding-window-size: 20       # 更大的滑动窗口
        minimum-number-of-calls: 10   # 更高的最小调用数
        failure-rate-threshold: 30    # 更严格的失败率
```

---

## 🎯 总结

通过本教程，你已经学会了：

1. **韧性模式的理论基础**：熔断器、重试、超时、舱壁等模式的原理和应用场景

2. **Resilience4j的实际应用**：从依赖配置到具体实现的完整流程

3. **支付服务的韧性设计**：多网关支持、故障切换、降级策略

4. **推荐系统的可靠性保障**：多层降级、用户体验优化

5. **服务间调用保护**：Feign客户端熔断器集成

6. **监控和运维实践**：指标收集、状态监控、故障排查

这些韧性模式不仅提高了系统的可用性和稳定性，更重要的是在故障发生时保障了用户体验和业务连续性。在实际生产环境中，合理的韧性设计是高可用系统的基石。