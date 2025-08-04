# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**ShopHub** - A Spring Cloud microservices learning project implementing an e-commerce marketplace with service discovery, distributed architecture, and inter-service communication.

### Architecture
- **Config Server** (port 8888): Centralized configuration management with Git backend support
- **Eureka Server** (port 8761): Service discovery and registration center
- **API Gateway** (port 8080): Single entry point with routing, rate limiting, and CORS support
- **User Service** (port 8081): User management with H2 database and Feign client integration
- **Product Service** (port 8082/8084/8085): Product catalog management with load balancing support
- **Order Service** (port 8083): Order processing with Feign client for product service calls
- **Payment Service** (port 8086): Payment processing with circuit breaker protection
- **Metrics Collector** (port 8087): Centralized observability and distributed tracing service with Redis caching
- **Redis** (port 6379): Distributed rate limiting, caching, and metrics storage
- **Zipkin** (port 9411): Distributed tracing visualization and analysis
- **Prometheus** (port 9090): Metrics collection, monitoring, and alerting
- **Grafana** (port 3000): Monitoring dashboards and visualization

All services are registered with Eureka and implement health checks via Spring Boot Actuator. Week 4 added multi-instance Product Service deployment for load balancing demonstration. Week 5 introduced centralized configuration management with feature toggles and environment-specific configurations. Week 6 implemented circuit breaker and resilience patterns for fault tolerance and graceful degradation. Week 7 added comprehensive observability with distributed tracing, metrics collection, and monitoring dashboard.

### Tech Stack
- **Spring Boot 3.2.0** with Java 17
- **Spring Cloud 2023.0.0** (Eureka, Gateway, OpenFeign, LoadBalancer, Config)
- **Maven** for dependency management with multi-module structure
- **H2 Database** (user-service)
- **Redis** for distributed rate limiting, caching, and metrics storage
- **Resilience4j** for retry, circuit breaker, bulkhead, and time limiter patterns
- **Docker Compose** for containerized deployment
- **Spring Cloud Config** for centralized configuration management
- **Micrometer + OpenTelemetry** for distributed tracing and metrics collection
- **Prometheus** for metrics aggregation, monitoring, and alerting
- **Zipkin** for distributed tracing visualization and analysis
- **Grafana** for monitoring dashboards and data visualization
- **Jackson** for JSON processing and JSR310 time handling

## Development Commands

### Maven多模块项目管理
该项目使用Maven多模块结构进行统一管理：

```
learn-spring-cloud/
├── pom.xml              # 根pom - 统一版本和依赖管理
├── config-server/       # 配置中心服务
├── eureka-server/       # 服务注册中心  
├── api-gateway/         # API网关
├── user-service/        # 用户服务
├── product-service/     # 产品服务
├── order-service/       # 订单服务
├── payment-service/     # 支付服务
├── metrics-collector/   # 指标收集服务 (Week 7)
├── monitoring-stack/    # 监控技术栈配置 (Prometheus, Grafana, Zipkin)
└── build-all.sh        # 一键构建脚本
```

**统一版本管理**：
- Spring Boot: 3.2.0
- Spring Cloud: 2023.0.0
- Java: 17
- Micrometer: 1.12.0
- OpenTelemetry: 1.32.0
- Resilience4j: 3.0.1
- MySQL: 8.0.33
- H2: 2.2.224
- Maven插件版本统一管理

### Build and Run Services Locally
```bash
# Maven多模块构建 (推荐)
mvn clean install                    # 构建所有模块
mvn clean install -pl config-server # 构建单个模块
./build-all.sh                      # 使用构建脚本

# 启动服务顺序 (依赖关系)
cd config-server && mvn spring-boot:run    # 1. 配置中心 (必须首先启动)
cd eureka-server && mvn spring-boot:run    # 2. 服务注册中心
cd api-gateway && mvn spring-boot:run      # 3. API网关
cd user-service && mvn spring-boot:run     # 4. 业务服务
cd product-service && mvn spring-boot:run  # 5. 业务服务
cd order-service && mvn spring-boot:run    # 6. 业务服务
cd payment-service && mvn spring-boot:run  # 7. 支付服务
cd metrics-collector && mvn spring-boot:run # 8. 指标收集服务

# Windows batch script to start all services
./start-services.bat

# Week 4: Start multiple Product Service instances for load balancing
./start-multiple-products.sh

# Test service discovery functionality
./test-service-discovery.sh

# Test API Gateway functionality (Week 3)
./test-api-gateway.sh

# Test Load Balancing functionality (Week 4)
./test-week4-load-balancing.sh

# Test Configuration Management functionality (Week 5)
./test-week5-config-management.sh

# Test Circuit Breaker functionality (Week 6)
./test-week6-circuit-breaker.sh

# Test Observability and Distributed Tracing functionality (Week 7)
./test-week7-observability.sh
```

### Docker Deployment
```bash
# Build and start all services
docker-compose up --build

# Start in detached mode
docker-compose up -d

# Stop all services
docker-compose down
```

### Testing and Validation
```bash
# Run comprehensive service discovery tests
./test-service-discovery.sh

# Run API Gateway tests (Week 3)
./test-api-gateway.sh

# Week 4: Test Load Balancing (multiple calls show different instances)
./test-week4-load-balancing.sh

# Week 5: Test Configuration Management (feature toggles, config refresh)
./test-week5-config-management.sh

# Week 6: Test Circuit Breaker and Resilience patterns
./test-week6-circuit-breaker.sh

# Manual health checks
curl http://localhost:8888/actuator/health  # Config Server
curl http://localhost:8761/actuator/health  # Eureka Server
curl http://localhost:8080/actuator/health  # API Gateway  
curl http://localhost:8081/actuator/health  # User Service
curl http://localhost:8082/actuator/health  # Product Service
curl http://localhost:8083/actuator/health  # Order Service
curl http://localhost:8086/actuator/health  # Payment Service

# Check Eureka service registration
curl http://localhost:8761/eureka/apps

# Test inter-service communication via Gateway
curl http://localhost:8080/api/users/check-product/1

# Test Gateway routing
curl http://localhost:8080/api/users/health
curl http://localhost:8080/api/products/health
curl http://localhost:8080/api/orders/health
curl http://localhost:8080/api/payments/health

# Week 4: Test Load Balancing (multiple calls show different instances)
curl http://localhost:8080/api/orders/load-balance-demo
curl http://localhost:8080/api/orders/verify-product/1

# Test Product Service multiple instances
curl http://localhost:8082/api/products/health  # Instance 1
curl http://localhost:8084/api/products/health  # Instance 2  
curl http://localhost:8085/api/products/health  # Instance 3

# Week 5: Test Configuration Management and Feature Toggles
curl http://localhost:8888/actuator/health  # Config Server health
curl -u configuser:configpass http://localhost:8888/api-gateway/dev  # Get config
curl http://localhost:8080/api/gateway/features  # Gateway feature toggles
curl http://localhost:8082/api/products/features  # Product service features
curl http://localhost:8082/api/products/recommendations  # Test recommendation feature
curl http://localhost:8082/api/products/1/inventory  # Test inventory feature
curl -X POST http://localhost:8082/actuator/refresh  # Refresh config

# Week 6: Test Circuit Breaker and Resilience
curl http://localhost:8086/api/payments/circuit-breaker/status  # Payment service circuit breaker status
curl http://localhost:8082/api/products/circuit-breaker/status  # Product service circuit breaker status
curl http://localhost:8083/api/orders/circuit-breaker/status  # Order service circuit breaker status
curl -X POST http://localhost:8086/api/payments/process -H "Content-Type: application/json" -d '{"orderId":1,"userId":1,"amount":99.99,"currency":"USD"}'  # Test payment processing
curl -X POST http://localhost:8083/api/orders/1/payment -H "Content-Type: application/json" -d '{"amount":99.99}'  # Test order payment
curl http://localhost:8082/api/products/recommendations?userId=1&category=smartphone  # Test recommendations with circuit breaker
curl http://localhost:8082/api/products/popular?category=laptop  # Test popular products
curl http://localhost:8086/api/payments/demo/circuit-breaker  # Trigger circuit breaker demo
curl http://localhost:8080/actuator/circuitbreakers  # All circuit breaker states via Gateway

# Week 7: Test Observability and Distributed Tracing
curl http://localhost:8087/actuator/health  # Metrics Collector health
curl http://localhost:8087/api/metrics/health-summary  # Centralized health summary
curl http://localhost:8087/api/metrics/services  # All registered services
curl http://localhost:8087/api/metrics/current  # Current metrics data
curl http://localhost:8087/api/metrics/traces/active  # Active distributed traces
curl http://localhost:9411/health  # Zipkin server health
curl http://localhost:9090/-/healthy  # Prometheus server health
curl http://localhost:8087/actuator/prometheus  # Metrics Collector Prometheus metrics
curl http://localhost:8080/actuator/prometheus  # API Gateway Prometheus metrics
curl http://localhost:8082/actuator/prometheus  # Product Service Prometheus metrics
```

## Service Endpoints

### Config Server (8888) - **Configuration Management Center**
- Server: `http://localhost:8888`
- Health: `http://localhost:8888/actuator/health`
- Config Access: `http://configuser:configpass@localhost:8888/{service}/{profile}`
- Examples:
  - `http://configuser:configpass@localhost:8888/api-gateway/dev`
  - `http://configuser:configpass@localhost:8888/product-service/prod`

### API Gateway (8080) - **Primary Entry Point**
- Gateway: `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`
- Routes: `http://localhost:8080/actuator/gateway/routes`
- Metrics: `http://localhost:8080/actuator/metrics`
- Feature Toggles: `http://localhost:8080/api/gateway/features`
- Config Refresh: `POST http://localhost:8080/actuator/refresh`

**Routed Services** (accessed via Gateway):
- Users: `http://localhost:8080/api/users`
- Products: `http://localhost:8080/api/products`
- Orders: `http://localhost:8080/api/orders`
- Payments: `http://localhost:8080/api/payments`
- Eureka: `http://localhost:8080/eureka`

### Eureka Server (8761)
- Dashboard: `http://localhost:8761`
- Registry API: `http://localhost:8761/eureka/apps`

### Direct Service Access (Development Only)
- User Service: `http://localhost:8081/api/users`
- Product Service: `http://localhost:8082/api/products`
- Order Service: `http://localhost:8083/api/orders`
- Payment Service: `http://localhost:8086/api/payments`
- Metrics Collector: `http://localhost:8087/api/metrics`

### Payment Service (port 8086)
- **POST** `/api/payments/process` - Process payment with circuit breaker protection
- **GET** `/api/payments/{id}` - Get payment details
- **GET** `/api/payments/health` - Health check endpoint
- **GET** `/api/payments/circuit-breaker/status` - Circuit breaker status
- **GET** `/api/payments/demo/circuit-breaker` - Trigger circuit breaker demo
- **GET** `/actuator/health` - Actuator health endpoint
- **GET** `/actuator/circuitbreakers` - Circuit breaker status
- **GET** `/actuator/circuitbreakerevents` - Circuit breaker events

### Metrics Collector (port 8087)
- **GET** `/api/metrics/collect` - Trigger metrics collection from all services
- **GET** `/api/metrics/health-summary` - Get aggregated health summary
- **GET** `/api/metrics/current` - Get current metrics data
- **GET** `/api/metrics/services` - Get all registered services
- **GET** `/api/metrics/traces/active` - Get active distributed traces
- **POST** `/api/metrics/cache/clear` - Clear metrics cache
- **GET** `/actuator/health` - Actuator health endpoint
- **GET** `/actuator/prometheus` - Prometheus metrics endpoint
- **GET** `/actuator/metrics` - Micrometer metrics endpoint

**Week 5: Configuration and Feature Toggle Endpoints**
- Product Features: `http://localhost:8082/api/products/features`
- Product Recommendations: `http://localhost:8082/api/products/recommendations`
- Product Inventory: `http://localhost:8082/api/products/{id}/inventory`
- Config Refresh: `POST http://localhost:8082/actuator/refresh`

**Week 6: Circuit Breaker and Resilience Endpoints**
- Payment Circuit Breaker: `http://localhost:8086/api/payments/demo/circuit-breaker`
- Circuit Breaker Status: `http://localhost:8086/actuator/circuitbreakers`
- Metrics Collector: `http://localhost:8087/api/metrics`

**Week 7: Observability and Monitoring Endpoints**
- Zipkin Tracing UI: `http://localhost:9411`
- Prometheus Metrics: `http://localhost:9090`
- Grafana Dashboard: `http://localhost:3000` (admin/admin)
- Metrics Collection: `http://localhost:8087/api/metrics/collect`
- Health Summary: `http://localhost:8087/api/metrics/health-summary`
- Active Traces: `http://localhost:8087/api/metrics/traces/active`
- Service Registry: `http://localhost:8087/api/metrics/services`



### Monitoring Stack (Week 7)
- **Zipkin Server** (port 9411): Distributed tracing visualization and analysis
- **Prometheus** (port 9090): Metrics collection, aggregation, and alerting
- **Grafana** (port 3000): Monitoring dashboards and data visualization
- **Metrics Collector** (port 8087): Centralized observability service with:
  - Service health monitoring
  - Distributed tracing coordination
  - Metrics aggregation and caching
  - OpenTelemetry integration

### Redis (6379)
- **Rate Limiting**: Distributed rate limiting for API Gateway
- **Caching**: Application-level caching for improved performance
- **Metrics Storage**: Temporary storage for collected metrics data
- **Session Storage**: Distributed session management (if needed)
- **Tracing Data**: Temporary storage for distributed tracing spans
- Connection: `redis://localhost:6379`

## Key Configuration

### API Gateway (Week 3)
- **Routing**: All requests route through Gateway on port 8080
- **Rate Limiting**: Redis-based distributed rate limiting with user tier support
- **CORS**: Full cross-origin support for web frontends
- **Filters**: Global logging, authentication, and custom request/response headers
- **Load Balancing**: Automatic service discovery via Eureka integration

### Service Discovery
- All services register with Eureka at `http://localhost:8761/eureka/`
- Health check intervals: 30s (Docker), 10s (local)
- Self-preservation disabled in development for faster instance detection

### Inter-Service Communication
- **User Service → Product Service**: Implemented via Spring Cloud OpenFeign
- **Order Service → Payment Service**: Implemented via Spring Cloud OpenFeign with circuit breaker protection
- Feign client configuration includes fallback handling for service unavailability
- Load balancing handled automatically by Spring Cloud LoadBalancer
- Circuit breaker patterns protect service calls from cascading failures

### Development Profile Settings
- **Local**: Services run on individual ports with direct Eureka connection
- **Docker**: Services use container networking with docker-compose service discovery
- Profile-specific configurations in `application.yml` files

## Testing Framework

Comprehensive test scripts provide end-to-end validation:
1. **Service Discovery**: `test-service-discovery.sh` - Service startup, health checks, Eureka registration
2. **API Gateway**: `test-api-gateway.sh` - Routing, rate limiting, CORS, filters
3. **Load Balancing**: `test-week4-load-balancing.sh` - Multi-instance deployment, client-side load balancing
4. **Configuration Management**: `test-week5-config-management.sh` - Feature toggles, config refresh, environment profiles
5. **Circuit Breaker**: `test-week6-circuit-breaker.sh` - Resilience patterns, fault tolerance, graceful degradation
6. **Observability**: `test-week7-observability.sh` - Distributed tracing, metrics collection, monitoring dashboard

Each script includes:
- Service startup verification
- Health check validation
- Feature-specific testing
- Error scenario simulation
- Performance and reliability metrics

## Common Development Tasks

### Adding a New Service
1. Create Maven module with Spring Boot + Eureka Client dependencies
2. Add `@EnableDiscoveryClient` annotation
3. Configure `application.yml` with unique service name and port
4. Add health check endpoints via Spring Boot Actuator
5. Update `docker-compose.yml` with new service configuration
6. Add routing rules to API Gateway configuration (Week 3+)

### API Gateway Configuration (Week 3+)
- Configure routing rules in `api-gateway/src/main/resources/application.yml`
- Add custom filters in `com.shophub.gateway.filter` package
- Implement rate limiting with Redis key resolver strategies
- Use `@EnableDiscoveryClient` for service discovery integration

### Inter-Service Communication
- Use `@FeignClient` with service name (not URL) for automatic load balancing
- Implement fallback methods for circuit breaker pattern
- Enable `@EnableFeignClients` on main application class
- Configure Resilience4j for circuit breaker, retry, timeout, and bulkhead patterns

### Circuit Breaker and Resilience (Week 6)
- Add `@CircuitBreaker`, `@Retry`, `@TimeLimiter`, `@Bulkhead` annotations to service methods
- Configure Resilience4j properties in `application.yml` for each service
- Implement fallback methods with same signature + Exception parameter
- Use CompletableFuture for asynchronous processing with circuit breakers
- Monitor circuit breaker states via Actuator endpoints
- Implement graceful degradation strategies for user experience

### Observability and Distributed Tracing (Week 7)
- Create dedicated Metrics Collector Service with Spring Boot + Eureka Client
- Add Micrometer tracing dependencies and Zipkin integration
- Configure `@EnableDiscoveryClient` and `@EnableFeignClients` for service communication
- Implement Feign clients for all services with fallback mechanisms
- Set up Prometheus metrics export via `/actuator/prometheus` endpoints
- Configure Docker deployment with Zipkin and Prometheus containers
- Use `@Scheduled` for automated metrics collection every 30 seconds
- Monitor circuit breaker states and service health via centralized dashboard