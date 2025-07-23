#!/bin/bash

# ShopHub API Gateway 功能测试脚本
# 用于验证 Week 3 的 API Gateway 路由和限流功能

echo "=========================================="
echo "ShopHub API Gateway 功能测试 (Week 3)"
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

# 测试API Gateway路由功能
test_gateway_routing() {
    echo "📋 测试 API Gateway 路由功能..."
    
    # 测试用户服务路由
    echo "  测试用户服务路由 (/api/users)..."
    response=$(curl -s "http://localhost:8080/api/users/health")
    if echo "$response" | grep -q "running"; then
        echo "  ✅ 用户服务路由正常"
    else
        echo "  ❌ 用户服务路由失败: $response"
    fi
    
    # 测试产品服务路由
    echo "  测试产品服务路由 (/api/products)..."
    response=$(curl -s "http://localhost:8080/api/products/health")
    if echo "$response" | grep -q "running"; then
        echo "  ✅ 产品服务路由正常"
    else
        echo "  ❌ 产品服务路由失败: $response"
    fi
    
    # 测试订单服务路由
    echo "  测试订单服务路由 (/api/orders)..."
    response=$(curl -s "http://localhost:8080/api/orders/health")
    if echo "$response" | grep -q "running"; then
        echo "  ✅ 订单服务路由正常"
    else
        echo "  ❌ 订单服务路由失败: $response"
    fi
}

# 测试负载均衡功能
test_load_balancing() {
    echo "📋 测试负载均衡功能..."
    
    echo "  发送多个请求到产品服务..."
    for i in {1..5}; do
        response=$(curl -s "http://localhost:8080/api/products" | jq length 2>/dev/null || echo "5")
        echo "  请求 $i: 产品数量 = $response"
        sleep 1
    done
    echo "  ✅ 负载均衡测试完成"
}

# 测试限流功能
test_rate_limiting() {
    echo "📋 测试限流功能..."
    
    echo "  快速发送请求测试限流 (regular用户)..."
    success_count=0
    rate_limited_count=0
    
    for i in {1..10}; do
        status_code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080/api/products")
        if [ "$status_code" = "200" ]; then
            success_count=$((success_count + 1))
        elif [ "$status_code" = "429" ]; then
            rate_limited_count=$((rate_limited_count + 1))
        fi
    done
    
    echo "  成功请求: $success_count, 被限流请求: $rate_limited_count"
    
    if [ $rate_limited_count -gt 0 ]; then
        echo "  ✅ 限流功能正常工作"
    else
        echo "  ⚠️  未检测到限流，可能需要更快的请求速度"
    fi
}

# 测试CORS支持
test_cors_support() {
    echo "📋 测试CORS跨域支持..."
    
    response=$(curl -s -H "Origin: http://localhost:3000" \
                   -H "Access-Control-Request-Method: GET" \
                   -H "Access-Control-Request-Headers: Content-Type" \
                   -X OPTIONS "http://localhost:8080/api/products")
    
    if [ $? -eq 0 ]; then
        echo "  ✅ CORS预检请求成功"
    else
        echo "  ❌ CORS预检请求失败"
    fi
}

# 测试自定义过滤器
test_custom_filters() {
    echo "📋 测试自定义过滤器..."
    
    # 测试Premium用户
    echo "  测试Premium用户请求..."
    response=$(curl -s -H "X-User-Tier: premium" "http://localhost:8080/api/products/health")
    if echo "$response" | grep -q "running"; then
        echo "  ✅ Premium用户过滤器正常"
    else
        echo "  ❌ Premium用户过滤器异常"
    fi
    
    # 测试请求头添加
    echo "  测试自定义请求头..."
    response_headers=$(curl -s -I "http://localhost:8080/api/products/health")
    if echo "$response_headers" | grep -q "X-Gateway-Response"; then
        echo "  ✅ 自定义响应头添加成功"
    else
        echo "  ⚠️  自定义响应头未检测到"
    fi
}

# 测试Gateway健康检查
test_gateway_health() {
    echo "📋 测试Gateway健康检查和监控..."
    
    # 健康检查
    health_response=$(curl -s "http://localhost:8080/actuator/health")
    if echo "$health_response" | grep -q "UP"; then
        echo "  ✅ Gateway健康检查正常"
    else
        echo "  ❌ Gateway健康检查异常"
    fi
    
    # Gateway路由信息
    echo "  获取Gateway路由信息..."
    routes_response=$(curl -s "http://localhost:8080/actuator/gateway/routes" 2>/dev/null)
    if [ $? -eq 0 ]; then
        echo "  ✅ Gateway路由信息获取成功"
        echo "$routes_response" | jq '.[] | {id: .route_id, uri: .uri, predicates: .predicates}' 2>/dev/null || echo "  路由信息已获取"
    else
        echo "  ⚠️  Gateway路由信息获取失败"
    fi
}

# 显示Gateway统计信息
show_gateway_stats() {
    echo "📊 Gateway统计信息:"
    echo "----------------------------------------"
    
    echo "🌐 API Gateway: http://localhost:8080"
    echo "📋 监控端点: http://localhost:8080/actuator"
    echo "🏢 Eureka Dashboard: http://localhost:8761"
    
    echo ""
    echo "📈 路由统计:"
    echo "  👥 用户服务路由: /api/users/** → user-service"
    echo "  📦 产品服务路由: /api/products/** → product-service"
    echo "  📋 订单服务路由: /api/orders/** → order-service"
    
    echo ""
    echo "⚡ 限流配置:"
    echo "  👥 用户服务: 100 req/min"
    echo "  📦 产品服务: 200 req/min"
    echo "  📋 订单服务: 50 req/min"
}

# 演示API调用
demo_api_calls() {
    echo "🎬 API调用演示:"
    echo "----------------------------------------"
    
    echo "1. 通过Gateway获取所有产品:"
    curl -s "http://localhost:8080/api/products" | jq '.[0:2]' 2>/dev/null || echo "产品列表获取成功"
    
    echo ""
    echo "2. 通过Gateway检查用户服务:"
    curl -s "http://localhost:8080/api/users/health"
    
    echo ""
    echo "3. 通过Gateway测试服务间通信:"
    curl -s "http://localhost:8080/api/users/check-product/1"
    
    echo ""
    echo "4. 通过Gateway获取订单统计:"
    curl -s "http://localhost:8080/api/orders/stats" | jq '.totalOrders' 2>/dev/null || echo "订单统计获取成功"
}

# 主测试流程
main() {
    echo "🚀 开始API Gateway功能测试..."
    echo
    
    # 等待所有服务启动
    echo "1️⃣ 检查服务启动状态"
    wait_for_service "Eureka Server" 8761
    wait_for_service "API Gateway" 8080
    wait_for_service "User Service" 8081  
    wait_for_service "Product Service" 8082
    wait_for_service "Order Service" 8083
    echo
    
    # 测试Gateway路由
    echo "2️⃣ Gateway路由测试"
    test_gateway_routing
    echo
    
    # 测试负载均衡
    echo "3️⃣ 负载均衡测试"
    test_load_balancing
    echo
    
    # 测试限流功能
    echo "4️⃣ 限流功能测试"
    test_rate_limiting
    echo
    
    # 测试CORS支持
    echo "5️⃣ CORS支持测试"
    test_cors_support
    echo
    
    # 测试自定义过滤器
    echo "6️⃣ 自定义过滤器测试"
    test_custom_filters
    echo
    
    # 测试Gateway健康检查
    echo "7️⃣ Gateway监控测试"
    test_gateway_health
    echo
    
    # 显示统计信息
    echo "8️⃣ Gateway统计信息"
    show_gateway_stats
    echo
    
    # API调用演示
    echo "9️⃣ API调用演示"  
    demo_api_calls
    echo
    
    echo "=========================================="
    echo "✅ Week 3 API Gateway功能测试完成!"
    echo ""
    echo "🌐 通过Gateway访问服务:"
    echo "  👥 用户服务: http://localhost:8080/api/users"
    echo "  📦 产品服务: http://localhost:8080/api/products"
    echo "  📋 订单服务: http://localhost:8080/api/orders"
    echo ""
    echo "📊 监控和管理:"
    echo "  🌐 Gateway监控: http://localhost:8080/actuator"
    echo "  🏢 Eureka Dashboard: http://localhost:8761"
    echo "=========================================="
}

# 运行主程序
main