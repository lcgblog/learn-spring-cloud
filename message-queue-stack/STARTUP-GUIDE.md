# ShopHub 消息队列服务启动指南

## 概述

从Week8开始，Redis和RabbitMQ服务已从主docker-compose.yml中分离出来，形成独立的消息队列模块。这样做的好处是：

- **模块化管理**: 消息队列服务可以独立启动、停止和管理
- **资源优化**: 开发时可以选择性启动需要的服务
- **环境隔离**: 不同的开发环境可以共享同一套消息队列服务
- **维护便利**: 消息队列服务的配置和管理更加集中

## 启动顺序

### 方式一：完整启动（推荐用于完整测试）

```bash
# 1. 启动消息队列服务（必须首先启动）
cd message-queue-stack
./start-message-queue.sh

# 2. 等待服务完全启动（约30秒）
./test-message-queue.sh

# 3. 返回根目录启动主应用
cd ..
docker-compose up -d

# 4. 验证所有服务
./test-week8-security-events.sh
```

### 方式二：选择性启动（推荐用于开发）

```bash
# 1. 仅启动Redis（如果只需要缓存功能）
cd message-queue-stack
docker-compose up -d redis redis-commander

# 2. 或仅启动RabbitMQ（如果只需要消息队列）
docker-compose up -d rabbitmq

# 3. 或启动所有消息队列服务
./start-message-queue.sh
```

### 方式三：本地开发启动

```bash
# 1. 启动消息队列服务
cd message-queue-stack
./start-message-queue.sh

# 2. 启动核心服务（Config Server + Eureka）
cd ..
docker-compose up -d config-server eureka-server

# 3. 根据需要启动其他服务
docker-compose up -d authorization-server
docker-compose up -d api-gateway
# ... 其他服务
```

## 服务依赖关系

```
消息队列服务 (独立模块)
├── Redis (6379)
├── RabbitMQ (5672/15672)
└── Redis Commander (8081)

主应用服务
├── Config Server (8888) - 基础服务
├── Eureka Server (8761) - 依赖Config Server
├── Authorization Server (8090) - 依赖Eureka
├── API Gateway (8080) - 依赖Config Server, Eureka, Redis
├── User Service (8081) - 依赖Config Server, Eureka
├── Product Service (8082) - 依赖Config Server, Eureka
├── Order Service (8083) - 依赖Config Server, Eureka
├── Payment Service (8086) - 依赖Config Server, Eureka
├── Notification Service (8089) - 依赖Config Server, Eureka, RabbitMQ, Authorization Server
└── Metrics Collector (8087) - 依赖Config Server, Eureka, Zipkin
```

## 网络配置

### 自动网络创建
启动脚本会自动创建必要的Docker网络：

```bash
# 主应用网络
learn-spring-cloud_shophub-network

# 消息队列内部网络
shophub-message-queue
```

### 网络连接
- 消息队列服务通过外部网络连接到主应用
- 主应用通过external_links引用消息队列服务
- 服务间可以通过容器名称进行通信

## 常用操作

### 查看服务状态
```bash
# 查看消息队列服务
cd message-queue-stack
docker-compose ps

# 查看主应用服务
cd ..
docker-compose ps

# 查看所有容器
docker ps
```

### 查看日志
```bash
# 消息队列服务日志
cd message-queue-stack
docker-compose logs -f redis
docker-compose logs -f rabbitmq

# 主应用服务日志
cd ..
docker-compose logs -f api-gateway
docker-compose logs -f notification-service
```

### 重启服务
```bash
# 重启消息队列服务
cd message-queue-stack
docker-compose restart

# 重启主应用服务
cd ..
docker-compose restart
```

### 停止服务
```bash
# 停止主应用（保留消息队列）
docker-compose down

# 停止消息队列服务
cd message-queue-stack
./stop-message-queue.sh

# 或者停止所有服务
docker-compose down
cd ..
docker-compose down
```

## 故障排除

### 1. 网络连接问题
```bash
# 检查网络是否存在
docker network ls | grep shophub

# 重新创建网络
docker network create learn-spring-cloud_shophub-network
```

### 2. 服务无法连接
```bash
# 检查容器是否在同一网络
docker network inspect learn-spring-cloud_shophub-network

# 测试网络连接
docker exec -it api-gateway ping shophub-redis
docker exec -it notification-service ping shophub-rabbitmq
```

### 3. 端口冲突
```bash
# 检查端口占用
lsof -i :6379  # Redis
lsof -i :5672  # RabbitMQ AMQP
lsof -i :15672 # RabbitMQ Management
lsof -i :8081  # Redis Commander
```

### 4. 数据持久化问题
```bash
# 检查数据卷
docker volume ls | grep message-queue

# 备份数据
docker run --rm -v message-queue-stack_redis_data:/data -v $(pwd):/backup alpine tar czf /backup/redis-backup.tar.gz -C /data .
docker run --rm -v message-queue-stack_rabbitmq_data:/data -v $(pwd):/backup alpine tar czf /backup/rabbitmq-backup.tar.gz -C /data .
```

## 开发建议

### 1. 开发环境
- 始终先启动消息队列服务
- 使用`./test-message-queue.sh`验证服务状态
- 根据开发需要选择性启动主应用服务

### 2. 测试环境
- 使用完整启动方式
- 定期运行测试脚本验证功能
- 监控服务健康状态

### 3. 生产环境
- 考虑使用外部Redis和RabbitMQ服务
- 配置适当的资源限制和监控
- 实施备份和恢复策略

## 性能监控

### Redis监控
- **管理界面**: http://localhost:8081 (Redis Commander)
- **命令行**: `docker exec shophub-redis redis-cli info`
- **内存使用**: `docker exec shophub-redis redis-cli info memory`

### RabbitMQ监控
- **管理界面**: http://localhost:15672 (guest/guest)
- **队列状态**: 在管理界面查看队列长度和消费速度
- **连接数**: 监控连接数和通道数

---

**提示**: 这种模块化架构使得服务管理更加灵活，但也要求开发者了解服务间的依赖关系。建议在开发过程中始终遵循正确的启动顺序。