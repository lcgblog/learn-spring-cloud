#!/bin/bash

# ShopHub消息队列服务测试脚本
# 测试Redis和RabbitMQ的连接和基本功能

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

test_redis() {
    print_status "测试Redis连接..."
    
    # 测试Redis ping
    if docker exec shophub-redis redis-cli ping > /dev/null 2>&1; then
        print_success "Redis连接正常"
    else
        print_error "Redis连接失败"
        return 1
    fi
    
    # 测试Redis读写
    print_status "测试Redis读写操作..."
    docker exec shophub-redis redis-cli set test_key "Hello ShopHub" > /dev/null
    RESULT=$(docker exec shophub-redis redis-cli get test_key)
    
    if [ "$RESULT" = "Hello ShopHub" ]; then
        print_success "Redis读写测试通过"
        docker exec shophub-redis redis-cli del test_key > /dev/null
    else
        print_error "Redis读写测试失败"
        return 1
    fi
}

test_rabbitmq() {
    print_status "测试RabbitMQ连接..."
    
    # 测试RabbitMQ状态
    if docker exec shophub-rabbitmq rabbitmq-diagnostics ping > /dev/null 2>&1; then
        print_success "RabbitMQ连接正常"
    else
        print_error "RabbitMQ连接失败"
        return 1
    fi
    
    # 测试RabbitMQ管理API
    print_status "测试RabbitMQ管理API..."
    if curl -s -u guest:guest http://localhost:15672/api/overview > /dev/null 2>&1; then
        print_success "RabbitMQ管理API正常"
    else
        print_error "RabbitMQ管理API连接失败"
        return 1
    fi
    
    # 获取RabbitMQ版本信息
    VERSION=$(curl -s -u guest:guest http://localhost:15672/api/overview | python3 -c "import sys, json; print(json.load(sys.stdin)['rabbitmq_version'])" 2>/dev/null || echo "未知")
    print_status "RabbitMQ版本: $VERSION"
}

test_services_health() {
    print_status "检查服务健康状态..."
    
    # 检查容器状态
    REDIS_STATUS=$(docker inspect --format='{{.State.Health.Status}}' shophub-redis 2>/dev/null || echo "unknown")
    RABBITMQ_STATUS=$(docker inspect --format='{{.State.Health.Status}}' shophub-rabbitmq 2>/dev/null || echo "unknown")
    
    echo "服务健康状态："
    echo "  Redis:    $REDIS_STATUS"
    echo "  RabbitMQ: $RABBITMQ_STATUS"
    
    if [ "$REDIS_STATUS" = "healthy" ] && [ "$RABBITMQ_STATUS" = "healthy" ]; then
        print_success "所有服务健康状态正常"
    else
        print_warning "部分服务健康检查未通过，可能仍在启动中"
    fi
}

show_service_info() {
    echo
    echo "==========================================="
    echo "           服务访问信息"
    echo "==========================================="
    echo "Redis:"
    echo "  连接地址: localhost:6379"
    echo "  管理界面: http://localhost:8081 (Redis Commander)"
    echo
    echo "RabbitMQ:"
    echo "  AMQP端口: localhost:5672"
    echo "  管理界面: http://localhost:15672"
    echo "  用户名/密码: guest/guest"
    echo
    echo "Docker容器:"
    docker-compose ps
    echo
}

echo "==========================================="
echo "    ShopHub 消息队列服务测试"
echo "==========================================="
echo

# 检查服务是否运行
if ! docker ps | grep -q "shophub-redis\|shophub-rabbitmq"; then
    print_error "消息队列服务未运行，请先执行 './start-message-queue.sh'"
    exit 1
fi

# 等待服务完全启动
print_status "等待服务完全启动..."
sleep 5

# 执行测试
test_services_health
echo
test_redis
echo
test_rabbitmq
echo

print_success "所有测试通过！消息队列服务运行正常"
show_service_info