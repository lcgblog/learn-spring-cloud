# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**ShopHub** - A Spring Cloud microservices learning project implementing an e-commerce marketplace with service discovery, distributed architecture, and inter-service communication.

### Architecture
- **Eureka Server** (port 8761): Service discovery and registration center
- **API Gateway** (port 8080): Single entry point with routing, rate limiting, and CORS support
- **User Service** (port 8081): User management with H2 database and Feign client integration
- **Product Service** (port 8082): Product catalog management with in-memory data storage
- **Order Service** (port 8083): Order processing and management system
- **Redis** (port 6379): Distributed rate limiting and caching

All services are registered with Eureka and implement health checks via Spring Boot Actuator.

### Tech Stack
- **Spring Boot 3.2.0** with Java 17
- **Spring Cloud 2023.0.0** (Eureka, Gateway, OpenFeign)
- **Maven** for dependency management
- **H2 Database** (user-service)
- **Redis** for distributed rate limiting
- **Docker Compose** for containerized deployment

## Development Commands

### Build and Run Services Locally
```bash
# Start individual services with Maven
cd eureka-server && mvn spring-boot:run
cd api-gateway && mvn spring-boot:run
cd user-service && mvn spring-boot:run
cd product-service && mvn spring-boot:run
cd order-service && mvn spring-boot:run

# Windows batch script to start all services
./start-services.bat

# Test service discovery functionality
./test-service-discovery.sh

# Test API Gateway functionality (Week 3)
./test-api-gateway.sh
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

# Manual health checks
curl http://localhost:8761/actuator/health  # Eureka Server
curl http://localhost:8080/actuator/health  # API Gateway  
curl http://localhost:8081/actuator/health  # User Service
curl http://localhost:8082/actuator/health  # Product Service
curl http://localhost:8083/actuator/health  # Order Service

# Check Eureka service registration
curl http://localhost:8761/eureka/apps

# Test inter-service communication via Gateway
curl http://localhost:8080/api/users/check-product/1

# Test Gateway routing
curl http://localhost:8080/api/users/health
curl http://localhost:8080/api/products/health
curl http://localhost:8080/api/orders/health
```

## Service Endpoints

### API Gateway (8080) - **Primary Entry Point**
- Gateway: `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`
- Routes: `http://localhost:8080/actuator/gateway/routes`
- Metrics: `http://localhost:8080/actuator/metrics`

**Routed Services** (accessed via Gateway):
- Users: `http://localhost:8080/api/users`
- Products: `http://localhost:8080/api/products`
- Orders: `http://localhost:8080/api/orders`
- Eureka: `http://localhost:8080/eureka`

### Eureka Server (8761)
- Dashboard: `http://localhost:8761`
- Registry API: `http://localhost:8761/eureka/apps`

### Direct Service Access (Development Only)
- User Service: `http://localhost:8081/api/users`
- Product Service: `http://localhost:8082/api/products`  
- Order Service: `http://localhost:8083/api/orders`

### Redis (6379)
- Used for distributed rate limiting
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
- Feign client configuration includes fallback handling for service unavailability
- Load balancing handled automatically by Spring Cloud LoadBalancer

### Development Profile Settings
- **Local**: Services run on individual ports with direct Eureka connection
- **Docker**: Services use container networking with docker-compose service discovery
- Profile-specific configurations in `application.yml` files

## Testing Framework

The `test-service-discovery.sh` and `test-api-gateway.sh` scripts provide comprehensive validation:
1. Service startup verification
2. Health check validation  
3. Eureka registration confirmation
4. Inter-service communication testing
5. API functionality verification
6. Gateway routing and rate limiting tests (Week 3)
7. CORS and filter validation (Week 3)
8. Service statistics display

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

### Service Configuration
- Environment-specific configs use Spring profiles (default, docker)
- Eureka client settings configured per service in `application.yml`
- Docker networking uses service names as hostnames