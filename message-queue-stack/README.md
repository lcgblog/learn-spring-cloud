# ShopHub 消息队列服务模块

这个模块提供了ShopHub微服务平台所需的消息队列和缓存服务，包括Redis和RabbitMQ。

## 服务组件

### Redis (端口 6379)
- **用途**: 分布式缓存、会话存储、限流计数器
- **版本**: Redis 7 Alpine
- **持久化**: AOF (Append Only File)
- **管理界面**: Redis Commander (端口 8081)

### RabbitMQ (端口 5672/15672)
- **用途**: 消息队列、事件驱动架构、异步通信
- **版本**: RabbitMQ 3 Management Alpine
- **管理界面**: http://localhost:15672 (guest/guest)
- **协议**: AMQP

## 快速开始

### 1. 启动服务
```bash
cd message-queue-stack
./start-message-queue.sh
```

### 2. 测试服务
```bash
./test-message-queue.sh
```

### 3. 停止服务
```bash
./stop-message-queue.sh
```

## 服务访问

### Redis
- **连接地址**: `localhost:6379`
- **管理界面**: http://localhost:8081 (Redis Commander)
- **连接测试**: `redis-cli ping`

### RabbitMQ
- **AMQP端口**: `localhost:5672`
- **管理界面**: http://localhost:15672
- **默认用户**: guest/guest
- **健康检查**: `rabbitmq-diagnostics ping`

## Docker命令

### 基本操作
```bash
# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down

# 停止并删除数据
docker-compose down -v
```

### 单独操作服务
```bash
# 重启Redis
docker-compose restart redis

# 重启RabbitMQ
docker-compose restart rabbitmq

# 查看Redis日志
docker-compose logs -f redis

# 查看RabbitMQ日志
docker-compose logs -f rabbitmq
```

## 数据持久化

### Redis
- **数据目录**: `redis_data` volume
- **配置**: AOF持久化已启用
- **备份**: 数据存储在Docker volume中

### RabbitMQ
- **数据目录**: `rabbitmq_data` volume
- **日志目录**: `rabbitmq_logs` volume
- **队列持久化**: 支持持久化队列和消息

## 网络配置

### 内部网络
- **网络名称**: `shophub-message-queue`
- **驱动**: bridge
- **用途**: 服务间内部通信

### 外部网络
- **网络名称**: `learn-spring-cloud_shophub-network`
- **用途**: 与主应用服务通信
- **自动创建**: 启动脚本会自动创建此网络

## 监控和管理

### Redis监控
- **Redis Commander**: http://localhost:8081
- **命令行工具**: `docker exec shophub-redis redis-cli`
- **性能监控**: `docker exec shophub-redis redis-cli info`

### RabbitMQ监控
- **管理界面**: http://localhost:15672
- **API接口**: http://localhost:15672/api/
- **命令行工具**: `docker exec shophub-rabbitmq rabbitmqctl`

## 故障排除

### 常见问题

1. **端口冲突**
   ```bash
   # 检查端口占用
   lsof -i :6379
   lsof -i :5672
   lsof -i :15672
   ```

2. **网络连接问题**
   ```bash
   # 检查网络
   docker network ls
   docker network inspect learn-spring-cloud_shophub-network
   ```

3. **数据丢失**
   ```bash
   # 检查数据卷
   docker volume ls
   docker volume inspect message-queue-stack_redis_data
   docker volume inspect message-queue-stack_rabbitmq_data
   ```

4. **服务无法启动**
   ```bash
   # 查看详细日志
   docker-compose logs redis
   docker-compose logs rabbitmq
   
   # 检查容器状态
   docker-compose ps
   docker inspect shophub-redis
   docker inspect shophub-rabbitmq
   ```

### 重置服务
```bash
# 完全重置（删除所有数据）
docker-compose down -v --remove-orphans
docker system prune -f
./start-message-queue.sh
```

## 集成指南

### Spring Boot应用配置

#### Redis配置
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

#### RabbitMQ配置
```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
```

### Docker Compose集成
如果需要在主应用的docker-compose.yml中引用：

```yaml
services:
  your-service:
    # ... 其他配置
    depends_on:
      - redis
      - rabbitmq
    external_links:
      - shophub-redis:redis
      - shophub-rabbitmq:rabbitmq
    networks:
      - learn-spring-cloud_shophub-network

networks:
  learn-spring-cloud_shophub-network:
    external: true
```

## 性能调优

### Redis优化
- 根据使用场景调整内存策略
- 配置合适的持久化策略
- 监控内存使用情况

### RabbitMQ优化
- 配置合适的队列参数
- 调整消费者数量
- 监控队列长度和消息处理速度

## 安全建议

1. **生产环境**：更改默认密码
2. **网络安全**：限制访问IP范围
3. **数据加密**：启用TLS/SSL
4. **访问控制**：配置用户权限

---

**注意**: 这个模块设计用于开发和测试环境。生产环境部署时，请根据实际需求调整配置和安全设置。