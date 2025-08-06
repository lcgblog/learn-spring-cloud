#!/bin/bash

# ShopHub消息队列服务停止脚本

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
echo "    ShopHub 消息队列服务停止脚本"
echo "==========================================="
echo

# 检查Docker是否运行
if ! docker info > /dev/null 2>&1; then
    print_error "Docker未运行"
    exit 1
fi

# 停止服务
print_status "停止消息队列服务..."
docker-compose down

# 选择性清理
read -p "是否要删除数据卷？(y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    print_warning "删除数据卷..."
    docker-compose down -v
    print_warning "所有数据已删除！"
else
    print_status "保留数据卷"
fi

print_success "消息队列服务已停止！"
echo
print_status "如需完全清理，请运行："
echo "  docker-compose down -v --remove-orphans"
echo "  docker system prune -f"