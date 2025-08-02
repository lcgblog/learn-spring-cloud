# Week 7 Distributed Tracing & Observability Tutorial

## 🎓 Learning Path

Welcome to Week 7 of the ShopHub Spring Cloud microservices learning journey! This week focuses on implementing comprehensive observability and distributed tracing capabilities that are essential for production microservices environments.

## 🎯 What You'll Learn

By the end of this tutorial, you will understand and implement:

1. **Distributed Tracing Fundamentals**
   - Understanding trace ID and span ID concepts
   - Implementing request correlation across services
   - Visualizing service call relationships

2. **Centralized Metrics Collection**
   - Building a dedicated metrics collector service
   - Automated service discovery and metrics gathering
   - Business-specific metrics definition and collection

3. **Observability Stack Integration**
   - Zipkin for distributed tracing visualization
   - Prometheus for metrics storage and querying
   - Micrometer for application metrics export

4. **Production Monitoring Patterns**
   - Circuit breaker state monitoring
   - Service health aggregation
   - Performance bottleneck identification

## 🏗️ Architecture Deep Dive

### The Problem We're Solving

In a microservices architecture like ShopHub's e-commerce platform, a single user request might traverse multiple services:

```
User Request → API Gateway → User Service → Product Service → Order Service → Payment Service
```

Without proper observability, debugging issues becomes extremely challenging:
- Which service is causing slow responses?
- Where are errors originating?
- How are circuit breakers performing?
- What's the overall system health?

### The Solution: Comprehensive Observability

Our Week 7 implementation creates a three-tier observability stack:

```
┌─────────────────────────────────────────────────────────────────┐
│                    Observability Dashboard                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │   Zipkin    │  │ Prometheus  │  │   Grafana   │              │
│  │  (Tracing)  │  │ (Metrics)   │  │ (Visualization) │          │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
├─────────────────────────────────────────────────────────────────┤
│                    Collection Layer                             │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │              Metrics Collector Service                      │ │
│  │   • Service Discovery Integration                          │ │  
│  │   • Automated Metrics Collection                           │ │
│  │   • Distributed Trace Management                           │ │
│  │   • Circuit Breaker Monitoring                             │ │
│  └─────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────┤
│                    Application Layer                            │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌──────────────┐│
│  │API Gateway  │ │User Service │ │Product Svc  │ │Payment Svc   ││
│  │+ Tracing    │ │+ Metrics    │ │+ CB Monitor │ │+ Observ.    ││
│  └─────────────┘ └─────────────┘ └─────────────┘ └──────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

## 🛠️ Hands-on Implementation

### Phase 1: Understanding the Existing System

Before adding observability, let's understand our current ShopHub services:

1. **API Gateway (8080)**: Single entry point with rate limiting
2. **User Service (8081)**: User management with H2 database
3. **Product Service (8082)**: Product catalog with recommendations
4. **Order Service (8083)**: Order processing with Feign clients
5. **Payment Service (8086)**: Payment processing with circuit breakers

Each service already has:
- Spring Boot Actuator for health checks
- Eureka registration for service discovery
- Circuit breaker patterns for resilience

### Phase 2: Add Distributed Tracing

#### Step 1: Add Tracing Dependencies

Add to each service's `pom.xml`:

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

<!-- Bootstrap for Spring Cloud Config (Important!) -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bootstrap</artifactId>
</dependency>
```
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

#### Step 2: Configure Tracing Properties

**Important Note**: Before adding tracing properties, ensure you have the bootstrap dependency and proper config import configuration to avoid startup errors.

Add to each service's `application.yml`:

```yaml
spring:
  application:
    name: your-service-name
  config:
    import: "configserver:"  # Required for Spring Boot 2.4+
  cloud:
    config:
      uri: http://localhost:8888
      # other config properties...
```

```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% sampling for learning (reduce in production)
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
  metrics:
    export:
      prometheus:
        enabled: true
```

#### Step 3: Understand Automatic Tracing

Spring Boot's auto-configuration automatically instruments:
- HTTP requests (incoming and outgoing)
- Database queries
- Message queue operations
- Cache operations

**Try it out:**
1. Start Zipkin: `docker run -d -p 9411:9411 openzipkin/zipkin`
2. Make a request: `curl http://localhost:8080/api/users/check-product/1`
3. View trace: http://localhost:9411

### Phase 3: Create Custom Observability Filter

Let's add a custom filter to the API Gateway for enhanced tracing:

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

**Learning Points:**
- **Span Creation**: Each operation creates a span with metadata
- **Tag Addition**: Custom tags provide business context
- **Error Handling**: Exceptions are automatically tagged
- **Header Propagation**: Trace context passes between services

### Phase 4: Build the Metrics Collector Service

#### Step 1: Create the Service Structure

```bash
mkdir metrics-collector
cd metrics-collector
# Create Maven structure with appropriate dependencies
```

#### Step 2: Implement Service Discovery Integration

```java
@Service
public class MetricsCollectionService {
    @Autowired
    private DiscoveryClient discoveryClient;
    
    @Scheduled(fixedDelay = 30000)
    public void collectMetricsFromAllServices() {
        List<String> services = discoveryClient.getServices();
        
        for (String serviceName : services) {
            if (!"metrics-collector".equals(serviceName)) {
                collectServiceMetrics(serviceName);
            }
        }
    }
    
    private void collectServiceMetrics(String serviceName) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        
        for (ServiceInstance instance : instances) {
            // Collect metrics from each instance
            ServiceMetrics metrics = new ServiceMetrics(serviceName, instance.getInstanceId());
            // Store and process metrics
        }
    }
}
```

**Key Learning:**
- **Service Discovery**: Automatically finds all registered services
- **Scheduled Collection**: Regular metrics gathering every 30 seconds
- **Instance Awareness**: Handles multiple instances of the same service

#### Step 3: Implement Feign Clients with Fallbacks

```java
@FeignClient(name = "product-service", fallback = ProductServiceClientFallback.class)
public interface ProductServiceClient {
    @GetMapping("/actuator/health")
    Map<String, Object> getHealth();
    
    @GetMapping("/api/products/circuit-breaker/status")
    Map<String, Object> getCircuitBreakerStatus();
}

@Component
public class ProductServiceClientFallback implements ProductServiceClient {
    @Override
    public Map<String, Object> getHealth() {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("status", "DOWN");
        fallback.put("fallback", true);
        return fallback;
    }
    
    @Override
    public Map<String, Object> getCircuitBreakerStatus() {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("status", "UNKNOWN");
        fallback.put("fallback", true);
        return fallback;
    }
}
```

**Resilience Pattern:**
- When a service is unavailable, fallbacks ensure the monitoring system remains operational
- Graceful degradation maintains system observability even during failures

### Phase 5: Integrate Prometheus Metrics

#### Step 1: Configure Custom Metrics

```java
@Configuration
public class ObservabilityConfig {
    
    @Bean
    public Counter requestCounter(MeterRegistry meterRegistry) {
        return Counter.builder("shophub.requests.total")
                .description("Total number of requests across all services")
                .register(meterRegistry);
    }
    
    @Bean
    public Gauge activeTracesGauge(MeterRegistry meterRegistry) {
        return Gauge.builder("shophub.traces.active", activeTraces, AtomicInteger::doubleValue)
                .description("Number of active distributed traces")
                .register(meterRegistry);
    }
    
    @Bean
    public Timer requestTimer(MeterRegistry meterRegistry) {
        return Timer.builder("shophub.requests.duration")
                .description("Request processing time")
                .register(meterRegistry);
    }
}
```

#### Step 2: Record Business Metrics

```java
private void recordCustomMetrics(String serviceName, ServiceMetrics metrics) {
    meterRegistry.gauge("shophub.service.response.time", 
        Collections.singletonList(Tag.of("service", serviceName)), 
        metrics.getResponseTime());
        
    meterRegistry.gauge("shophub.service.request.count", 
        Collections.singletonList(Tag.of("service", serviceName)), 
        metrics.getRequestCount());
}
```

**Business Value:**
- **Performance Tracking**: Response times per service
- **Load Monitoring**: Request counts and patterns
- **Error Rates**: Failure percentages and trends

### Phase 6: Deploy the Complete Stack

#### Docker Compose Configuration

```yaml
services:
  zipkin:
    image: openzipkin/zipkin
    ports:
      - "9411:9411"
    networks:
      - shophub-network

  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
    networks:
      - shophub-network

  metrics-collector:
    build: ./metrics-collector
    ports:
      - "8087:8087"
    depends_on:
      - zipkin
      - prometheus
    networks:
      - shophub-network
```

#### Prometheus Scraping Configuration

```yaml
scrape_configs:
  - job_name: 'api-gateway'
    static_configs:
      - targets: ['api-gateway:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s

  - job_name: 'metrics-collector'
    static_configs:
      - targets: ['metrics-collector:9087']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
```

## 🧪 Practical Exercises

### Exercise 1: Trace a User Journey

**Objective**: Follow a complete user request through the system

**Steps**:
1. Start all services with Docker Compose
2. Execute: `curl http://localhost:8080/api/users/check-product/1`
3. Find the trace in Zipkin: http://localhost:9411
4. Analyze the service call sequence and timing

**Questions to Answer**:
- How many services were involved?
- Which service took the longest?
- Were there any errors or retries?

### Exercise 2: Monitor Circuit Breaker Behavior

**Objective**: Observe circuit breaker states during failures

**Steps**:
1. Trigger circuit breaker: `curl http://localhost:8086/api/payments/demo/circuit-breaker`
2. Check metrics collector: `curl http://localhost:8087/api/metrics/health-summary`
3. View Prometheus metrics: http://localhost:9090

**Analysis**:
- How quickly did the circuit breaker open?
- What metrics changed during the failure?
- How did fallback mechanisms respond?

### Exercise 3: Create Custom Business Metrics

**Objective**: Add order success rate tracking

**Implementation**:
```java
@Component
public class OrderMetrics {
    private final Counter orderSuccessCounter;
    private final Counter orderFailureCounter;
    
    public OrderMetrics(MeterRegistry meterRegistry) {
        this.orderSuccessCounter = Counter.builder("shophub.orders.success")
                .description("Successful order count")
                .register(meterRegistry);
                
        this.orderFailureCounter = Counter.builder("shophub.orders.failure")
                .description("Failed order count")
                .register(meterRegistry);
    }
    
    public void recordOrderSuccess() {
        orderSuccessCounter.increment();
    }
    
    public void recordOrderFailure() {
        orderFailureCounter.increment();
    }
}
```

**Integration**: Add to Order Service and observe in Prometheus

## 🎯 Real-world Scenarios

### Scenario 1: Black Friday Traffic Spike

**Situation**: Sudden 10x traffic increase at midnight

**Observability Response**:
1. **Metrics Collector** detects increased request rates
2. **Zipkin** shows slower response times in Product Service
3. **Prometheus** alerts trigger for high error rates
4. **Circuit Breakers** protect Payment Service from overload

**Business Value**: Rapid identification and mitigation of performance bottlenecks

### Scenario 2: Database Connection Pool Exhaustion

**Situation**: Order Service database connections depleted

**Detection Path**:
1. **User complaints** about order failures
2. **Zipkin traces** show timeouts in Order Service
3. **Custom metrics** reveal high database connection usage
4. **Circuit breaker** in Order → Product calls activates

**Resolution**: Increase connection pool size based on metrics data

### Scenario 3: Silent Data Corruption

**Situation**: Product recommendation algorithm returns invalid results

**Discovery Through Observability**:
1. **Business metrics** show decreased click-through rates
2. **Distributed tracing** reveals all services responding normally
3. **Custom recommendation metrics** show algorithm performance degradation
4. **A/B testing metrics** confirm recommendation quality issues

## 📊 Key Performance Indicators (KPIs)

### Technical KPIs

1. **Response Time Percentiles**
   - P50, P95, P99 response times per service
   - Target: < 100ms P95 for API Gateway

2. **Error Rates**
   - 4xx and 5xx error percentages
   - Target: < 1% error rate overall

3. **Circuit Breaker Health**
   - Time spent in OPEN state
   - Fallback activation frequency

### Business KPIs

1. **User Journey Completion**
   - End-to-end order completion rates
   - Cart abandonment at each service boundary

2. **Service Availability**
   - Uptime percentage per service
   - Mean Time To Recovery (MTTR)

3. **Resource Utilization**
   - CPU, memory, and network usage
   - Cost per transaction

## 🚀 Production Deployment Considerations

### Sampling Strategy

In production, 100% trace sampling creates too much overhead:

```yaml
management:
  tracing:
    sampling:
      probability: 0.1  # 10% sampling in production
```

### Data Retention

Configure appropriate retention policies:

```yaml
# Prometheus
--storage.tsdb.retention.time=30d

# Zipkin
STORAGE_TYPE=mysql
MYSQL_HOST=mysql-server
```

### Alerting Rules

Create Prometheus alerting rules:

```yaml
groups:
- name: shophub.rules
  rules:
  - alert: HighErrorRate
    expr: rate(shophub_errors_total[5m]) > 0.05
    for: 2m
    annotations:
      summary: "High error rate detected"
```

### Security Considerations

1. **Metrics Endpoint Security**: Secure `/actuator/prometheus` endpoints
2. **Trace Data Sanitization**: Remove sensitive data from traces  
3. **Network Isolation**: Use internal networks for metrics collection
4. **Access Control**: Implement RBAC for monitoring dashboards

## 🔧 Troubleshooting Common Issues

### Problem: Missing Traces in Zipkin

**Symptoms**: Services running but no traces visible

**Diagnosis Steps**:
1. Check Zipkin endpoint configuration
2. Verify network connectivity
3. Examine application logs for trace export errors
4. Confirm sampling probability > 0

**Solution**:
```yaml
logging:
  level:
    io.micrometer.tracing: DEBUG
    zipkin2: DEBUG
```

### Problem: Prometheus Not Scraping Metrics

**Symptoms**: Targets down in Prometheus UI

**Diagnosis**:
1. Verify `/actuator/prometheus` endpoint accessibility
2. Check Prometheus configuration syntax
3. Confirm network connectivity between containers
4. Validate service discovery configuration

**Solution**:
```bash
# Test endpoint manually
curl http://localhost:8080/actuator/prometheus

# Check Prometheus config
docker exec prometheus promtool check config /etc/prometheus/prometheus.yml
```

### Problem: High Memory Usage in Metrics Collector

**Symptoms**: OutOfMemoryError in metrics collector

**Root Cause**: Unbounded metrics storage

**Solution**:
```java
@Scheduled(fixedDelay = 300000) // 5 minutes
public void cleanupOldMetrics() {
    long cutoff = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5);
    currentMetrics.entrySet().removeIf(entry -> 
        entry.getValue().getTimestamp().isBefore(
            LocalDateTime.now().minusMinutes(5)));
}
```

### Problem: Spring Boot 2.4+ Configuration Import Error

**Error Message**:
```
Add a spring.config.import=configserver: property to your configuration.
If configuration is not required add spring.config.import=optional:configserver: instead.
To disable this check, set spring.cloud.config.enabled=false or 
spring.cloud.config.import-check.enabled=false.
```

**Root Cause**: Spring Boot 2.4+ requires explicit config import declaration

**Solution**:
1. Add bootstrap dependency to `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bootstrap</artifactId>
</dependency>
```

2. Add config import to `application.yml`:
```yaml
spring:
  config:
    import: "configserver:"
```

### Problem: Redis Configuration Deprecation Warnings

**Warning**: `spring.redis.*` properties are deprecated

**Solution**: Update to `spring.data.redis.*`:
```yaml
# Old configuration
spring:
  redis:
    host: localhost
    port: 6379

# New configuration
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

### Problem: Prometheus Metrics Export Deprecation

**Warning**: `management.metrics.export.prometheus.enabled` is deprecated

**Solution**: Update to new configuration:
```yaml
# Old configuration
management:
  metrics:
    export:
      prometheus:
        enabled: true

# New configuration
management:
  prometheus:
    metrics:
      export:
        enabled: true
```

## 🎓 Key Takeaways

### Technical Learnings

1. **Distributed Tracing** provides end-to-end request visibility
2. **Centralized Metrics Collection** enables system-wide monitoring
3. **Circuit Breaker Monitoring** ensures resilience visibility
4. **Fallback Mechanisms** maintain observability during failures

### Architectural Patterns

1. **Observer Pattern**: Metrics collection doesn't impact business logic
2. **Circuit Breaker Pattern**: Monitoring includes resilience state
3. **Bulkhead Pattern**: Separate monitoring infrastructure
4. **Saga Pattern**: Transaction monitoring across services

### Production Readiness

1. **Monitoring Infrastructure** is as critical as business services
2. **Observability Strategy** must be designed from day one
3. **Alert Fatigue** can be prevented with proper threshold tuning
4. **Business Metrics** are often more valuable than technical metrics

## 🔮 Next Steps and Advanced Topics

### Immediate Enhancements

1. **Add Grafana Dashboards**
   - Visual metrics representation
   - Business KPI tracking
   - Alert status visualization

2. **Implement Log Aggregation**
   - ELK Stack integration
   - Structured logging
   - Log correlation with traces

3. **Advanced Alerting**
   - AlertManager configuration
   - PagerDuty integration
   - Escalation policies

### Future Advanced Topics

1. **Machine Learning Integration**
   - Anomaly detection algorithms
   - Predictive scaling
   - Intelligent alerting

2. **Chaos Engineering**
   - Chaos Monkey integration
   - Failure injection testing
   - Resilience validation

3. **Multi-Region Observability**
   - Global metrics aggregation
   - Cross-region tracing
   - Federated monitoring

## 📚 Additional Resources

### Official Documentation
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer Application Metrics](https://micrometer.io/docs)
- [Zipkin Distributed Tracing](https://zipkin.io/pages/quickstart.html)
- [Prometheus Monitoring](https://prometheus.io/docs/prometheus/latest/getting_started/)

### Best Practices Guides
- [Google SRE Book - Monitoring](https://sre.google/sre-book/monitoring-distributed-systems/)
- [The Three Pillars of Observability](https://www.oreilly.com/library/view/distributed-systems-observability/9781492033431/)
- [Microservices Observability Patterns](https://microservices.io/patterns/observability/)

### Community Resources
- [CNCF OpenTelemetry](https://opentelemetry.io/)
- [Jaeger Tracing](https://www.jaegertracing.io/)
- [Grafana Observability](https://grafana.com/products/)

---

Congratulations! You've successfully implemented a comprehensive observability solution for the ShopHub microservices platform. This foundation provides the visibility and insights needed for production operations, performance optimization, and reliable service delivery.

The observability capabilities you've built will serve as the foundation for advanced topics like chaos engineering, machine learning-powered operations, and enterprise-scale monitoring solutions.