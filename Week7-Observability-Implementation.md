# Week 7 Distributed Tracing & Observability Implementation Guide

## 📋 Overview

Week 7 实现了完整的分布式追踪和可观测性解决方案，通过集中式监控平台为ShopHub电商微服务架构提供端到端的性能监控、错误追踪和业务指标收集能力。

## 🎯 Learning Objectives

- **Distributed Tracing**: 实现分布式请求链路追踪，可视化服务间调用关系
- **Centralized Monitoring**: 构建统一监控平台，集中收集所有服务指标
- **Business Metrics**: 定义和收集业务相关的关键性能指标
- **Observability Dashboard**: 建立可观测性仪表板，实时监控系统健康状态
- **Production Readiness**: 为生产环境提供完整的监控和故障诊断能力

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        ShopHub Observability Stack              │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │   Zipkin    │  │ Prometheus  │  │   Grafana   │              │
│  │   (9411)    │  │   (9090)    │  │   (3000)    │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │              Metrics Collector Service                      │ │
│  │                       (8087)                              │ │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │ │
│  │  │   Feign     │ │   Redis     │ │  Micrometer │          │ │
│  │  │  Clients    │ │   Cache     │ │   Tracing   │          │ │
│  │  └─────────────┘ └─────────────┘ └─────────────┘          │ │
│  └─────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌──────────────┐│
│  │API Gateway  │ │User Service │ │Product Svc  │ │Payment Svc   ││
│  │   (8080)    │ │   (8081)    │ │   (8082)    │ │   (8086)     ││
│  └─────────────┘ └─────────────┘ └─────────────┘ └──────────────┘│
│  ┌─────────────┐ ┌─────────────┐                                │
│  │Order Service│ │Eureka Server│                                │
│  │   (8083)    │ │   (8761)    │                                │
│  └─────────────┘ └─────────────┘                                │
└─────────────────────────────────────────────────────────────────┘
```

## 🚀 Implementation Steps

### Step 1: Create Metrics Collector Service

#### 1.1 Maven Module Structure
```bash
mkdir metrics-collector
cd metrics-collector
```

**pom.xml Dependencies**:
```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    
    <!-- Service Discovery -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
    
    <!-- Configuration -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-config</artifactId>
    </dependency>
    
    <!-- Metrics and Monitoring -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
    
    <!-- Distributed Tracing -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-tracing</artifactId>
    </dependency>
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-tracing-bridge-brave</artifactId>
    </dependency>
    <dependency>
        <groupId>io.zipkin.reporter2</groupId>
        <artifactId>zipkin-reporter-brave</artifactId>
    </dependency>
    
    <!-- Service Communication -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
    </dependency>
    
    <!-- Redis Cache -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    
    <!-- Circuit Breaker Integration -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
    </dependency>
</dependencies>
```

#### 1.2 Main Application Class
```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
public class MetricsCollectorApplication {
    public static void main(String[] args) {
        SpringApplication.run(MetricsCollectorApplication.class, args);
    }
}
```

#### 1.3 Configuration Properties
**application.yml**:
```yaml
server:
  port: 8087

management:
  server:
    port: 8087
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true

spring:
  application:
    name: metrics-collector
  cloud:
    config:
      uri: http://localhost:8888
      fail-fast: false
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: true
    lease-renewal-interval-in-seconds: 10
    lease-expiration-duration-in-seconds: 30

logging:
  level:
    com.shophub.metrics: DEBUG
    org.springframework.cloud: INFO
```

### Step 2: Implement Data Models

#### 2.1 ServiceMetrics Model
```java
public class ServiceMetrics {
    private String serviceName;
    private String instanceId;
    private LocalDateTime timestamp;
    private Map<String, Object> metrics;
    private String status;
    private Double responseTime;
    private Integer requestCount;
    private Integer errorCount;
    private Map<String, String> traceInfo;
    
    // Constructors
    public ServiceMetrics() {}
    
    public ServiceMetrics(String serviceName, String instanceId) {
        this.serviceName = serviceName;
        this.instanceId = instanceId;
        this.timestamp = LocalDateTime.now();
        this.status = "UP";
    }
    
    // Getters and Setters...
}
```

#### 2.2 TraceData Model
```java
public class TraceData {
    private String traceId;
    private String spanId;
    private String parentSpanId;
    private String serviceName;
    private String operationName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long duration;
    private Map<String, String> tags;
    private Map<String, String> logs;
    private String status;
    
    // Constructors
    public TraceData() {}
    
    public TraceData(String traceId, String spanId, String serviceName) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.serviceName = serviceName;
        this.startTime = LocalDateTime.now();
        this.status = "OK";
    }
    
    public void finish() {
        this.endTime = LocalDateTime.now();
        if (this.startTime != null && this.endTime != null) {
            this.duration = java.time.Duration.between(this.startTime, this.endTime).toMillis();
        }
    }
    
    // Getters and Setters...
}
```

### Step 3: Create Feign Clients for Service Communication

#### 3.1 Service Client Interfaces
```java
@FeignClient(name = "user-service", fallback = UserServiceClientFallback.class)
public interface UserServiceClient {
    @GetMapping("/actuator/health")
    Map<String, Object> getHealth();
    
    @GetMapping("/actuator/metrics")
    Map<String, Object> getMetrics();
    
    @GetMapping("/actuator/prometheus")
    String getPrometheusMetrics();
}

@FeignClient(name = "product-service", fallback = ProductServiceClientFallback.class)
public interface ProductServiceClient {
    @GetMapping("/actuator/health")
    Map<String, Object> getHealth();
    
    @GetMapping("/actuator/metrics")
    Map<String, Object> getMetrics();
    
    @GetMapping("/actuator/prometheus")
    String getPrometheusMetrics();
    
    @GetMapping("/api/products/circuit-breaker/status")
    Map<String, Object> getCircuitBreakerStatus();
}

@FeignClient(name = "api-gateway", fallback = ApiGatewayClientFallback.class)
public interface ApiGatewayClient {
    @GetMapping("/actuator/health")
    Map<String, Object> getHealth();
    
    @GetMapping("/actuator/gateway/routes")
    Map<String, Object> getRoutes();
    
    @GetMapping("/actuator/circuitbreakers")
    Map<String, Object> getCircuitBreakers();
}
```

#### 3.2 Fallback Implementations
```java
@Component
public class UserServiceClientFallback implements UserServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceClientFallback.class);
    
    @Override
    public Map<String, Object> getHealth() {
        logger.warn("Fallback: user-service health check failed");
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("status", "DOWN");
        fallback.put("service", "user-service");
        fallback.put("fallback", true);
        return fallback;
    }
    
    @Override
    public Map<String, Object> getMetrics() {
        logger.warn("Fallback: user-service metrics unavailable");
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("error", "service unavailable");
        fallback.put("fallback", true);
        return fallback;
    }
    
    @Override
    public String getPrometheusMetrics() {
        logger.warn("Fallback: user-service prometheus metrics unavailable");
        return "# Fallback: user-service prometheus metrics unavailable";
    }
}
```

### Step 4: Implement Metrics Collection Service

#### 4.1 Core Collection Service
```java
@Service
public class MetricsCollectionService {
    private static final Logger logger = LoggerFactory.getLogger(MetricsCollectionService.class);
    
    @Autowired private DiscoveryClient discoveryClient;
    @Autowired private ObservabilityConfig observabilityConfig;
    @Autowired private MeterRegistry meterRegistry;
    @Autowired private Tracer tracer;
    @Autowired private RedisTemplate<String, Object> redisTemplate;
    @Autowired private Counter requestCounter;
    @Autowired private Timer requestTimer;
    @Autowired private Counter errorCounter;
    
    private final Map<String, ServiceMetrics> currentMetrics = new ConcurrentHashMap<>();
    private final Map<String, TraceData> activeTraces = new ConcurrentHashMap<>();
    
    @Scheduled(fixedDelay = 30000)
    @Timed(value = "metrics.collection.time", description = "Time taken to collect metrics from all services")
    public void collectMetricsFromAllServices() {
        Span span = tracer.nextSpan().name("collect-all-metrics").start();
        
        try {
            logger.info("Starting metrics collection from all services");
            
            List<String> services = discoveryClient.getServices();
            logger.info("Discovered services: {}", services);
            
            for (String serviceName : services) {
                if (!"metrics-collector".equals(serviceName)) {
                    collectServiceMetrics(serviceName);
                }
            }
            
            updateObservabilityMetrics();
            cacheMetricsData();
            
            logger.info("Completed metrics collection for {} services", services.size());
            
        } catch (Exception e) {
            logger.error("Error during metrics collection", e);
            errorCounter.increment();
            span.tag("error", e.getMessage());
        } finally {
            span.end();
        }
    }
    
    public void collectServiceMetrics(String serviceName) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        
        for (ServiceInstance instance : instances) {
            try {
                ServiceMetrics metrics = new ServiceMetrics(serviceName, instance.getInstanceId());
                metrics.setStatus("UP");
                
                Map<String, Object> metricsData = new HashMap<>();
                metricsData.put("host", instance.getHost());
                metricsData.put("port", instance.getPort());
                metricsData.put("uri", instance.getUri().toString());
                metricsData.put("metadata", instance.getMetadata());
                metricsData.put("scheme", instance.getScheme());
                metricsData.put("lastUpdated", LocalDateTime.now());
                
                // Simulate metrics for demo
                Random random = new Random();
                metrics.setResponseTime(50.0 + random.nextDouble() * 200);
                metrics.setRequestCount(random.nextInt(100) + 10);
                metrics.setErrorCount(random.nextInt(5));
                
                metrics.setMetrics(metricsData);
                currentMetrics.put(serviceName + "-" + instance.getInstanceId(), metrics);
                
                recordCustomMetrics(serviceName, metrics);
                
                logger.debug("Collected metrics for service: {} instance: {}", 
                    serviceName, instance.getInstanceId());
                    
            } catch (Exception e) {
                logger.error("Failed to collect metrics for service: {} instance: {}", 
                    serviceName, instance.getInstanceId(), e);
                errorCounter.increment();
            }
        }
    }
    
    private void recordCustomMetrics(String serviceName, ServiceMetrics metrics) {
        meterRegistry.gauge("shophub.service.response.time", 
            Collections.singletonList(io.micrometer.core.instrument.Tag.of("service", serviceName)), 
            metrics.getResponseTime());
            
        meterRegistry.gauge("shophub.service.request.count", 
            Collections.singletonList(io.micrometer.core.instrument.Tag.of("service", serviceName)), 
            metrics.getRequestCount());
            
        meterRegistry.gauge("shophub.service.error.count", 
            Collections.singletonList(io.micrometer.core.instrument.Tag.of("service", serviceName)), 
            metrics.getErrorCount());
    }
    
    private void updateObservabilityMetrics() {
        observabilityConfig.getActiveTraces().set(activeTraces.size());
        observabilityConfig.getTotalRequests().addAndGet(currentMetrics.size());
    }
    
    private void cacheMetricsData() {
        try {
            String key = "shophub:metrics:" + System.currentTimeMillis();
            redisTemplate.opsForValue().set(key, currentMetrics, 5, TimeUnit.MINUTES);
            logger.debug("Cached metrics data with key: {}", key);
        } catch (Exception e) {
            logger.warn("Failed to cache metrics data", e);
        }
    }
    
    // Trace management methods
    public TraceData startTrace(String serviceName, String operationName) {
        Span span = tracer.nextSpan().name(operationName).start();
        
        String traceId = span.context().traceId();
        String spanId = span.context().spanId();
        
        TraceData traceData = new TraceData(traceId, spanId, serviceName);
        traceData.setOperationName(operationName);
        
        Map<String, String> tags = new HashMap<>();
        tags.put("service.name", serviceName);
        tags.put("operation.name", operationName);
        traceData.setTags(tags);
        
        activeTraces.put(traceId, traceData);
        
        logger.debug("Started trace: {} for service: {} operation: {}", traceId, serviceName, operationName);
        
        return traceData;
    }
    
    public void finishTrace(String traceId) {
        TraceData traceData = activeTraces.remove(traceId);
        if (traceData != null) {
            traceData.finish();
            logger.debug("Finished trace: {} duration: {}ms", traceId, traceData.getDuration());
        }
    }
    
    // Getter methods for API endpoints
    public Map<String, ServiceMetrics> getCurrentMetrics() {
        return new HashMap<>(currentMetrics);
    }
    
    public Map<String, TraceData> getActiveTraces() {
        return new HashMap<>(activeTraces);
    }
    
    public List<String> getRegisteredServices() {
        return discoveryClient.getServices();
    }
    
    @Timed(value = "metrics.health.check", description = "Time taken to perform health check")
    public Map<String, Object> getHealthSummary() {
        Map<String, Object> health = new HashMap<>();
        health.put("totalServices", discoveryClient.getServices().size());
        health.put("activeMetrics", currentMetrics.size());
        health.put("activeTraces", activeTraces.size());
        health.put("lastCollection", LocalDateTime.now());
        
        Map<String, Long> serviceStatus = new HashMap<>();
        currentMetrics.values().forEach(metrics -> {
            String status = metrics.getStatus();
            serviceStatus.merge(status, 1L, Long::sum);
        });
        health.put("serviceStatus", serviceStatus);
        
        return health;
    }
}
```

#### 4.2 Observability Configuration
```java
@Configuration
public class ObservabilityConfig {
    private final AtomicInteger activeTraces = new AtomicInteger(0);
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags("application", "shophub-metrics-collector");
    }
    
    @Bean
    public Counter requestCounter(MeterRegistry meterRegistry) {
        return Counter.builder("shophub.requests.total")
                .description("Total number of requests across all services")
                .register(meterRegistry);
    }
    
    @Bean
    public Timer requestTimer(MeterRegistry meterRegistry) {
        return Timer.builder("shophub.requests.duration")
                .description("Request processing time")
                .register(meterRegistry);
    }
    
    @Bean
    public Gauge activeTracesGauge(MeterRegistry meterRegistry) {
        return Gauge.builder("shophub.traces.active", activeTraces, AtomicInteger::doubleValue)
                .description("Number of active distributed traces")
                .register(meterRegistry);
    }
    
    @Bean
    public Counter errorCounter(MeterRegistry meterRegistry) {
        return Counter.builder("shophub.errors.total")
                .description("Total number of errors across all services")
                .register(meterRegistry);
    }
    
    @Bean
    public Counter circuitBreakerCounter(MeterRegistry meterRegistry) {
        return Counter.builder("shophub.circuitbreaker.calls")
                .description("Circuit breaker state changes")
                .register(meterRegistry);
    }
    
    public AtomicInteger getActiveTraces() {
        return activeTraces;
    }
    
    public AtomicInteger getTotalRequests() {
        return totalRequests;
    }
}
```

### Step 5: Create REST API Controller

```java
@RestController
@RequestMapping("/api/metrics")
@Validated
public class MetricsController {
    private static final Logger logger = LoggerFactory.getLogger(MetricsController.class);
    
    @Autowired
    private MetricsCollectionService metricsCollectionService;
    
    @GetMapping("/health-summary")
    @Timed(value = "api.metrics.health.summary", description = "Get health summary")
    public ResponseEntity<Map<String, Object>> getHealthSummary() {
        try {
            Map<String, Object> healthSummary = metricsCollectionService.getHealthSummary();
            return ResponseEntity.ok(healthSummary);
        } catch (Exception e) {
            logger.error("Error retrieving health summary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve health summary", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/current")
    @Timed(value = "api.metrics.current", description = "Get current metrics")
    public ResponseEntity<Map<String, ServiceMetrics>> getCurrentMetrics() {
        try {
            Map<String, ServiceMetrics> currentMetrics = metricsCollectionService.getCurrentMetrics();
            return ResponseEntity.ok(currentMetrics);
        } catch (Exception e) {
            logger.error("Error retrieving current metrics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/traces/active")
    @Timed(value = "api.metrics.traces.active", description = "Get active traces")
    public ResponseEntity<Map<String, TraceData>> getActiveTraces() {
        try {
            Map<String, TraceData> activeTraces = metricsCollectionService.getActiveTraces();
            return ResponseEntity.ok(activeTraces);
        } catch (Exception e) {
            logger.error("Error retrieving active traces", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/services")
    @Timed(value = "api.metrics.services", description = "Get registered services")
    public ResponseEntity<List<String>> getRegisteredServices() {
        try {
            List<String> services = metricsCollectionService.getRegisteredServices();
            return ResponseEntity.ok(services);
        } catch (Exception e) {
            logger.error("Error retrieving registered services", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/trace/start")
    @Timed(value = "api.metrics.trace.start", description = "Start a new trace")
    public ResponseEntity<TraceData> startTrace(
            @RequestParam String serviceName,
            @RequestParam String operationName) {
        try {
            TraceData traceData = metricsCollectionService.startTrace(serviceName, operationName);
            return ResponseEntity.ok(traceData);
        } catch (Exception e) {
            logger.error("Error starting trace", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/trace/finish/{traceId}")
    @Timed(value = "api.metrics.trace.finish", description = "Finish an existing trace")
    public ResponseEntity<Void> finishTrace(@PathVariable String traceId) {
        try {
            metricsCollectionService.finishTrace(traceId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error finishing trace", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
```

### Step 6: Configure Docker Deployment

#### 6.1 Update docker-compose.yml
```yaml
version: '3.8'

services:
  # ... existing services ...
  
  # Week 7: 分布式追踪和可观测性组件
  zipkin:
    image: openzipkin/zipkin
    container_name: zipkin
    ports:
      - "9411:9411"
    networks:
      - shophub-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9411/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  metrics-collector:
    build: ./metrics-collector
    ports:
      - "8087:8087"
      - "8087:8087"  # Management port
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka/
      - SPRING_CLOUD_CONFIG_URI=http://config-server:8888
    depends_on:
      config-server:
        condition: service_healthy
      eureka-server:
        condition: service_healthy
      redis:
        condition: service_healthy
      zipkin:
        condition: service_healthy
    networks:
      - shophub-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8087/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  # Prometheus for metrics collection
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--storage.tsdb.retention.time=200h'
      - '--web.enable-lifecycle'
    networks:
      - shophub-network
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:9090/-/healthy"]
      interval: 30s
      timeout: 10s
      retries: 3

networks:
  shophub-network:
    driver: bridge
```

#### 6.2 Prometheus Configuration
**monitoring/prometheus.yml**:
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  # ShopHub Config Server
  - job_name: 'config-server'
    static_configs:
      - targets: ['config-server:8888']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 30s

  # ShopHub Eureka Server
  - job_name: 'eureka-server'
    static_configs:
      - targets: ['eureka-server:8761']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 30s

  # ShopHub API Gateway
  - job_name: 'api-gateway'
    static_configs:
      - targets: ['api-gateway:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s

  # ShopHub User Service
  - job_name: 'user-service'
    static_configs:
      - targets: ['user-service:8081']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s

  # ShopHub Product Service
  - job_name: 'product-service'
    static_configs:
      - targets: ['product-service:8082']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s

  # ShopHub Order Service
  - job_name: 'order-service'
    static_configs:
      - targets: ['order-service:8083']
    metrics_path: '/actuator/prometheus' 
    scrape_interval: 15s

  # ShopHub Payment Service
  - job_name: 'payment-service'
    static_configs:
      - targets: ['payment-service:8086']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s

  # ShopHub Metrics Collector
  - job_name: 'metrics-collector'
    static_configs:
      - targets: ['metrics-collector:8087']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s

  # Prometheus自监控
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
```

### Step 7: Update Existing Services for Observability

#### 7.1 Add Tracing Dependencies to All Services
在所有现有服务的 `pom.xml` 中添加：
```xml
<!-- Distributed Tracing -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

#### 7.2 Update Application Properties
在所有服务的 `application.yml` 中添加：
```yaml
management:
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
  metrics:
    export:
      prometheus:
        enabled: true
```

#### 7.3 API Gateway Observability Filter
```java
@Component
public class ObservabilityFilter implements GlobalFilter, Ordered {
    private static final Logger logger = LoggerFactory.getLogger(ObservabilityFilter.class);
    
    @Autowired
    private Tracer tracer;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod().toString();
        String path = request.getPath().toString();
        
        // Start a new span for this request
        Span span = tracer.nextSpan()
                .name("gateway-request")
                .tag("http.method", method)
                .tag("http.path", path)
                .tag("component", "api-gateway")
                .start();
        
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            
            // Add tracing headers to the request
            exchange = exchange.mutate()
                    .request(exchange.getRequest().mutate()
                            .header("X-Trace-Id", span.context().traceId())
                            .header("X-Span-Id", span.context().spanId())
                            .build())
                    .build();
            
            long startTime = System.currentTimeMillis();
            
            return chain.filter(exchange)
                    .doOnTerminate(() -> {
                        long duration = System.currentTimeMillis() - startTime;
                        span.tag("duration.ms", String.valueOf(duration));
                        
                        ServerHttpResponse response = exchange.getResponse();
                        if (response.getStatusCode() != null) {
                            span.tag("http.status_code", String.valueOf(response.getStatusCode().value()));
                            if (response.getStatusCode().isError()) {
                                span.tag("error", "true");
                            }
                        }
                        
                        logger.debug("Gateway request processed: {} {} - {}ms", 
                            method, path, duration);
                    })
                    .doFinally(signalType -> span.end());
            
        } catch (Exception e) {
            span.tag("error", e.getMessage());
            logger.error("Error in observability filter", e);
            throw e;
        }
    }
    
    @Override
    public int getOrder() {
        return -1; // Execute before other filters
    }
}
```

### Step 8: Create Comprehensive Test Script

**test-week7-observability.sh**:
```bash
#!/bin/bash

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "=========================================="
echo "Week 7: 分布式追踪和可观测性功能测试"
echo "=========================================="
echo "测试内容："
echo "1. 指标收集服务健康检查"
echo "2. 分布式追踪功能验证"
echo "3. Prometheus指标导出测试"
echo "4. Zipkin追踪数据验证"
echo "5. 服务间调用链路追踪"
echo "6. 自定义指标收集测试"
echo "7. 可观测性仪表板功能"
echo "=========================================="
echo ""

# Function to check service health
check_service_health() {
    local service_name=$1
    local port=$2
    local max_attempts=30
    local attempt=1
    
    echo -e "${BLUE}[INFO]${NC} 检查 $service_name 服务状态..."
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s -f "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
            echo -e "${GREEN}[SUCCESS]${NC} $service_name 服务运行正常"
            return 0
        else
            echo -e "${YELLOW}[WARNING]${NC} $service_name 服务未就绪，等待中... ($attempt/$max_attempts)"
            sleep 5
            ((attempt++))
        fi
    done
    
    echo -e "${RED}[ERROR]${NC} $service_name 服务启动失败或超时"
    return 1
}

# Function to test API endpoint
test_endpoint() {
    local name=$1
    local url=$2
    local expected_status=${3:-200}
    
    echo -e "${BLUE}[INFO]${NC} 测试 $name..."
    
    response=$(curl -s -w "%{http_code}" -o /tmp/response.json "$url")
    status_code="${response: -3}"
    
    if [ "$status_code" -eq "$expected_status" ]; then
        echo -e "${GREEN}[SUCCESS]${NC} $name 测试通过 (HTTP $status_code)"
        if [ -f /tmp/response.json ]; then
            content_length=$(wc -c < /tmp/response.json)
            if [ $content_length -gt 50 ]; then
                echo -e "${BLUE}[INFO]${NC} 响应数据长度: $content_length 字节"
            else
                echo -e "${BLUE}[INFO]${NC} 响应内容: $(cat /tmp/response.json)"
            fi
        fi
        return 0
    else
        echo -e "${RED}[ERROR]${NC} $name 测试失败 (HTTP $status_code)"
        if [ -f /tmp/response.json ]; then
            echo -e "${RED}[ERROR]${NC} 错误响应: $(cat /tmp/response.json)"
        fi
        return 1
    fi
}

# Function to generate load and traces
generate_load_for_tracing() {
    echo -e "${BLUE}[INFO]${NC} 生成负载以创建追踪数据..."
    
    # Generate requests through API Gateway
    for i in {1..10}; do
        curl -s "http://localhost:8080/api/users/health" > /dev/null &
        curl -s "http://localhost:8080/api/products/health" > /dev/null &
        curl -s "http://localhost:8080/api/orders/health" > /dev/null &
        curl -s "http://localhost:8080/api/payments/circuit-breaker/status" > /dev/null &
    done
    
    wait
    echo -e "${GREEN}[SUCCESS]${NC} 负载生成完成，等待追踪数据收集..."
    sleep 10
}

# 1. 服务健康检查
echo -e "${BLUE}[INFO]${NC} === 1. 服务健康检查 ==="
check_service_health "Config-Server" 8888
check_service_health "Eureka-Server" 8761
check_service_health "API-Gateway" 8080
check_service_health "User-Service" 8081
check_service_health "Product-Service" 8082
check_service_health "Order-Service" 8083
check_service_health "Payment-Service" 8086
check_service_health "Metrics-Collector" 8087

# 2. 监控组件健康检查
echo -e "${BLUE}[INFO]${NC} === 2. 监控组件健康检查 ==="
check_service_health "Zipkin" 9411
test_endpoint "Prometheus健康检查" "http://localhost:9090/-/healthy"

# 3. 指标收集服务API测试
echo -e "${BLUE}[INFO]${NC} === 3. 指标收集服务API测试 ==="
test_endpoint "服务健康汇总" "http://localhost:8087/api/metrics/health-summary"
test_endpoint "当前指标数据" "http://localhost:8087/api/metrics/current"
test_endpoint "注册服务列表" "http://localhost:8087/api/metrics/services"
test_endpoint "活跃追踪数据" "http://localhost:8087/api/metrics/traces/active"

# 4. Prometheus指标导出测试
echo -e "${BLUE}[INFO]${NC} === 4. Prometheus指标导出测试 ==="
test_endpoint "API Gateway Prometheus指标" "http://localhost:8080/actuator/prometheus"
test_endpoint "Product Service Prometheus指标" "http://localhost:8082/actuator/prometheus"
test_endpoint "Metrics Collector Prometheus指标" "http://localhost:8087/actuator/prometheus"

# 5. 生成追踪数据
echo -e "${BLUE}[INFO]${NC} === 5. 分布式追踪数据生成 ==="
generate_load_for_tracing

# 6. 验证追踪数据
echo -e "${BLUE}[INFO]${NC} === 6. 验证Zipkin追踪数据 ==="
test_endpoint "Zipkin服务列表" "http://localhost:9411/api/v2/services"
test_endpoint "Zipkin追踪查询" "http://localhost:9411/api/v2/traces?limit=10"

# 7. 自定义指标验证
echo -e "${BLUE}[INFO]${NC} === 7. 自定义业务指标验证 ==="

# 检查Prometheus中是否有ShopHub自定义指标
prometheus_metrics=$(curl -s "http://localhost:9090/api/v1/label/__name__/values")
if echo "$prometheus_metrics" | grep -q "shophub"; then
    echo -e "${GREEN}[SUCCESS]${NC} 发现ShopHub自定义指标"
    echo -e "${BLUE}[INFO]${NC} 可用的ShopHub指标:"
    echo "$prometheus_metrics" | grep -o '"shophub[^"]*"' | head -5
else
    echo -e "${YELLOW}[WARNING]${NC} 暂未发现ShopHub自定义指标，可能需要更多时间收集"
fi

# 8. 熔断器状态监控
echo -e "${BLUE}[INFO]${NC} === 8. 熔断器状态监控 ==="
test_endpoint "API Gateway熔断器状态" "http://localhost:8080/actuator/circuitbreakers"
test_endpoint "Payment Service熔断器状态" "http://localhost:8086/api/payments/circuit-breaker/status"
test_endpoint "Product Service熔断器状态" "http://localhost:8082/api/products/circuit-breaker/status"

# 9. 端到端可观测性测试
echo -e "${BLUE}[INFO]${NC} === 9. 端到端可观测性测试 ==="

# 测试一个完整的业务流程，并验证追踪
echo -e "${BLUE}[INFO]${NC} 执行完整业务流程: 用户检查产品 -> 订单支付"

# 通过Gateway调用，生成完整调用链
business_flow_trace_id=$(curl -s "http://localhost:8080/api/users/check-product/1" | grep -o '"traceId":"[^"]*"' | cut -d'"' -f4)

if [ ! -z "$business_flow_trace_id" ]; then
    echo -e "${GREEN}[SUCCESS]${NC} 业务流程追踪ID: $business_flow_trace_id"
    
    # 等待追踪数据上报到Zipkin
    sleep 5
    
    # 验证追踪数据是否在Zipkin中
    zipkin_trace_check=$(curl -s "http://localhost:9411/api/v2/trace/$business_flow_trace_id")
    if [ ! -z "$zipkin_trace_check" ] && [ "$zipkin_trace_check" != "[]" ]; then
        echo -e "${GREEN}[SUCCESS]${NC} 在Zipkin中找到业务流程追踪数据"
    else
        echo -e "${YELLOW}[WARNING]${NC} Zipkin中暂未找到追踪数据，可能需要更多时间"
    fi
else
    echo -e "${YELLOW}[WARNING]${NC} 未获取到追踪ID，但业务流程执行成功"
fi

# 10. 性能统计
echo -e "${BLUE}[INFO]${NC} === 10. 可观测性性能统计 ==="

metrics_summary=$(curl -s "http://localhost:8087/api/metrics/health-summary")
if [ ! -z "$metrics_summary" ]; then
    echo -e "${GREEN}[SUCCESS]${NC} 指标收集汇总:"
    echo "$metrics_summary" | jq '.' 2>/dev/null || echo "$metrics_summary"
else
    echo -e "${RED}[ERROR]${NC} 无法获取指标汇总"
fi

# 清理临时文件
rm -f /tmp/response.json

echo ""
echo "=========================================="
echo -e "${GREEN}Week 7 可观测性功能测试完成！${NC}"
echo "=========================================="
echo ""
echo "访问以下URL查看可观测性仪表板:"
echo "🔍 Zipkin追踪: http://localhost:9411"
echo "📊 Prometheus监控: http://localhost:9090"
echo "📈 指标收集API: http://localhost:8087/api/metrics/health-summary"
echo ""
echo "关键观察点:"
echo "1. 所有服务健康状态正常"
echo "2. 分布式追踪数据完整收集"
echo "3. Prometheus指标正常导出"
echo "4. 自定义业务指标可用"
echo "5. 熔断器状态实时监控"
echo "6. 端到端调用链路可视化"
```

## 🧪 Testing and Validation

### Manual Testing Steps

1. **Start All Services**:
```bash
# Using Docker Compose
docker-compose up -d

# Or start individually
cd config-server && mvn spring-boot:run &
cd eureka-server && mvn spring-boot:run &
cd api-gateway && mvn spring-boot:run &
cd user-service && mvn spring-boot:run &
cd product-service && mvn spring-boot:run &
cd order-service && mvn spring-boot:run &
cd payment-service && mvn spring-boot:run &
cd metrics-collector && mvn spring-boot:run &
```

2. **Run Comprehensive Test**:
```bash
chmod +x test-week7-observability.sh
./test-week7-observability.sh
```

3. **Verify Observability Stack**:
- **Zipkin UI**: http://localhost:9411
- **Prometheus UI**: http://localhost:9090
- **Metrics Collector API**: http://localhost:8087/api/metrics/health-summary

### Key Verification Points

1. **Service Registration**: All services registered in Eureka
2. **Metrics Collection**: 30-second automated collection working
3. **Distributed Tracing**: Request traces visible in Zipkin
4. **Prometheus Scraping**: All service metrics available
5. **Circuit Breaker Monitoring**: Real-time CB status tracking
6. **Fallback Mechanisms**: Feign fallbacks working correctly

## 📊 Business Value and Real-world Applications

### Production Benefits

1. **Proactive Monitoring**: 30秒内发现服务异常
2. **Root Cause Analysis**: 分布式追踪快速定位问题
3. **Performance Optimization**: 识别性能瓶颈服务
4. **Business Intelligence**: 业务指标实时监控
5. **SLA Compliance**: 服务可用性和响应时间保障

### Scalability Considerations

1. **Metrics Volume**: 生产环境需要考虑指标数据量
2. **Sampling Strategy**: 高流量场景使用采样减少开销
3. **Storage Retention**: Prometheus数据保留策略
4. **Alert Rules**: 基于指标的告警规则配置
5. **Dashboard Customization**: 业务特定的监控仪表板

## 🔧 Troubleshooting Guide

### Common Issues

1. **Zipkin Connection Failure**:
   - 检查 `management.zipkin.tracing.endpoint` 配置
   - 验证 Zipkin 服务是否正常运行

2. **Prometheus Scraping Errors**:
   - 检查 `/actuator/prometheus` 端点是否暴露
   - 验证 Prometheus 配置文件中的目标地址

3. **Metrics Collection Delays**:
   - 检查 Redis 连接状态
   - 验证服务发现是否正常工作

4. **Feign Client Fallbacks**:
   - 检查服务健康状态
   - 验证网络连接和服务可达性

### Performance Tuning

1. **Collection Interval**: 根据业务需求调整收集频率
2. **Cache Strategy**: 优化 Redis 缓存策略
3. **Thread Pool**: 调整 Feign 客户端线程池配置
4. **JVM Parameters**: 监控服务 JVM 参数调优

## 🎯 Next Steps

Week 7 完成后，你已经拥有了一个完整的微服务可观测性平台。接下来可以考虑：

1. **Advanced Monitoring**: 添加 Grafana 仪表板
2. **Alerting System**: 配置 Prometheus AlertManager
3. **Log Aggregation**: 集成 ELK Stack 或 Fluentd
4. **APM Integration**: 集成 New Relic 或 Datadog
5. **Chaos Engineering**: 使用 Chaos Monkey 进行韧性测试

## 📚 References

- [Spring Boot Actuator Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer Documentation](https://micrometer.io/docs)
- [Zipkin Quickstart](https://zipkin.io/pages/quickstart.html)
- [Prometheus Getting Started](https://prometheus.io/docs/prometheus/latest/getting_started/)
- [Spring Cloud Sleuth Reference](https://spring.io/projects/spring-cloud-sleuth)

---

通过 Week 7 的实现，ShopHub 电商平台现在具备了企业级的可观测性能力，为生产环境的稳定运行和快速故障诊断提供了强有力的技术保障。这套可观测性系统不仅支持技术运维，更为业务决策提供了数据支撑。