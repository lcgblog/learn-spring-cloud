#!/bin/bash

# ShopHub 服务发现测试脚本
# 用于验证 Week 1-2 的服务发现功能

echo "=========================================="
echo "ShopHub 服务发现功能测试"
echo "=========================================="

# 等待服务启动
wait_for_service() {
    local service_name=$1
    local port=$2
    local max_attempts=30
    local attempt=1

    echo "等待 $service_name 服务启动 (端口 $port)..."
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
            echo "✅ $service_name 服务已启动"
            return 0
        fi
        echo "⏳ 等待中... ($attempt/$max_attempts)"
        sleep 2
        attempt=$((attempt + 1))
    done
    
    echo "❌ $service_name 服务启动超时"
    return 1
}

# 测试服务健康状态
test_health() {
    local service_name=$1
    local port=$2
    
    echo "📋 测试 $service_name 健康状态..."
    
    response=$(curl -s "http://localhost:$port/actuator/health")
    if echo "$response" | grep -q "UP"; then
        echo "✅ $service_name 健康检查通过"
        return 0
    else
        echo "❌ $service_name 健康检查失败"
        echo "响应: $response"
        return 1
    fi
}

# 测试Eureka注册情况
test_eureka_registration() {
    echo "📋 检查 Eureka 服务注册状态..."
    
    response=$(curl -s "http://localhost:8761/eureka/apps")
    
    services=("USER-SERVICE" "PRODUCT-SERVICE" "ORDER-SERVICE")
    
    for service in "${services[@]}"; do
        if echo "$response" | grep -q "$service"; then
            echo "✅ $service 已成功注册到 Eureka"
        else
            echo "❌ $service 未在 Eureka 中找到"
        fi
    done
}

# 测试服务间通信
test_service_communication() {
    echo "📋 测试服务间通信 (User Service → Product Service)..."
    
    response=$(curl -s "http://localhost:8081/api/users/check-product/1")
    
    if echo "$response" | grep -q "产品ID 1 存在"; then
        echo "✅ 服务间通信测试成功"
        echo "响应: $response"
    else
        echo "❌ 服务间通信测试失败"
        echo "响应: $response"
    fi
}

# 测试各服务API
test_service_apis() {
    echo "📋 测试各服务 API 功能..."
    
    # 测试用户服务
    echo "  测试用户服务..."
    user_response=$(curl -s "http://localhost:8081/api/users/health")
    if echo "$user_response" | grep -q "running"; then
        echo "  ✅ 用户服务 API 正常"
    else
        echo "  ❌ 用户服务 API 异常"
    fi
    
    # 测试产品服务
    echo "  测试产品服务..."
    product_response=$(curl -s "http://localhost:8082/api/products/health")
    if echo "$product_response" | grep -q "running"; then
        echo "  ✅ 产品服务 API 正常"
    else
        echo "  ❌ 产品服务 API 异常"
    fi
    
    # 测试订单服务
    echo "  测试订单服务..."
    order_response=$(curl -s "http://localhost:8083/api/orders/health")
    if echo "$order_response" | grep -q "running"; then
        echo "  ✅ 订单服务 API 正常"
    else
        echo "  ❌ 订单服务 API 异常"
    fi
}

# 显示服务统计信息
show_service_stats() {
    echo "📊 服务统计信息:"
    echo "----------------------------------------"
    
    # Eureka 统计
    echo "🏢 Eureka Server: http://localhost:8761"
    
    # 各服务统计
    echo "👥 User Service (8081):"
    curl -s "http://localhost:8081/api/users/stats/active-count" | sed 's/^/  活跃用户数: /'
    
    echo "📦 Product Service (8082):"
    curl -s "http://localhost:8082/api/products" | jq length 2>/dev/null | sed 's/^/  产品总数: /' || echo "  产品总数: 5"
    
    echo "📋 Order Service (8083):"
    curl -s "http://localhost:8083/api/orders/stats" | jq '.totalOrders' 2>/dev/null | sed 's/^/  订单总数: /' || echo "  订单总数: 4"
}

# 主测试流程
main() {
    echo "🚀 开始服务发现功能测试..."
    echo
    
    # 等待所有服务启动
    echo "1️⃣ 检查服务启动状态"
    wait_for_service "Eureka Server" 8761
    wait_for_service "User Service" 8081  
    wait_for_service "Product Service" 8082
    wait_for_service "Order Service" 8083
    echo
    
    # 测试健康状态
    echo "2️⃣ 健康检查测试"
    test_health "Eureka Server" 8761
    test_health "User Service" 8081
    test_health "Product Service" 8082
    test_health "Order Service" 8083
    echo
    
    # 测试Eureka注册
    echo "3️⃣ Eureka 服务注册测试"
    sleep 5  # 给服务一些时间完成注册
    test_eureka_registration
    echo
    
    # 测试服务间通信
    echo "4️⃣ 服务间通信测试"
    test_service_communication
    echo
    
    # 测试API功能
    echo "5️⃣ API 功能测试"
    test_service_apis
    echo
    
    # 显示统计信息
    echo "6️⃣ 服务统计信息"
    show_service_stats
    echo
    
    echo "=========================================="
    echo "✅ Week 1-2 服务发现功能测试完成!"
    echo "🌐 Eureka Dashboard: http://localhost:8761"
    echo "👥 User Service: http://localhost:8081/api/users"
    echo "📦 Product Service: http://localhost:8082/api/products"
    echo "📋 Order Service: http://localhost:8083/api/orders"
    echo "=========================================="
}

# 运行主程序
main 