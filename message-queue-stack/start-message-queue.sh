#!/bin/bash

# ShopHub消息队列服务启动脚本
# 包含Redis和RabbitMQ服务

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

echo "==========================================="
echo "    ShopHub 消息队列服务启动脚本"
echo "==========================================="
echo

# 检查Docker是否运行
if ! docker info > /dev/null 2>&1; then
    print_error "Docker未运行，请先启动Docker"
    exit 1
fi

print_status "检查Docker Compose版本..."
docker-compose --version

# 创建外部网络（如果不存在）
print_status "创建外部网络（如果不存在）..."
docker network create learn-spring-cloud_shophub-network 2>/dev/null || print_warning "网络已存在或创建失败"

# 启动消息队列服务
print_status "启动消息队列服务..."
docker-compose up -d

# 等待服务启动
print_status "等待服务启动..."
sleep 10

# 检查服务状态
print_status "检查服务状态..."
docker-compose ps

echo
print_success "消息队列服务启动完成！"
echo
echo "服务访问地址："
echo "  Redis:           localhost:6379"
echo "  RabbitMQ AMQP:   localhost:5672"
echo "  RabbitMQ 管理界面: http://localhost:15672 (guest/guest)"
echo "  Redis Commander: http://localhost:8081"
echo
print_status "使用 './stop-message-queue.sh' 停止服务"
print_status "使用 'docker-compose logs -f' 查看日志"