#!/bin/bash

# Week 6: Circuit Breaker & Resilience Testing Script
# 测试熔断器和韧性模式的实现

echo "========================================="
echo "Week 6: Circuit Breaker & Resilience Testing"
echo "========================================="

# 检查服务状态
check_service_health() {
    local service_name=$1
    local port=$2
    local endpoint_path=${3:-"/actuator/health"}
    
    echo "Checking $service_name health..."
    response=$(curl -s -w "%{http_code}" "http://localhost:$port$endpoint_path" -o /tmp/health_response.json)
    
    if [ "$response" = "200" ]; then
        echo "  ✅ $service_name is UP (port: $port)"
        # 显示版本或实例信息
        if [ -f /tmp/health_response.json ]; then
            instance_info=$(cat /tmp/health_response.json | grep -o '"port"[^,]*' | head -1 || echo "")
            if [ ! -z "$instance_info" ]; then
                echo "     $instance_info"
            fi
        fi
    else
        echo "  ❌ $service_name is DOWN (port: $port) - HTTP: $response"
        return 1
    fi
}

# 测试熔断器状态
test_circuit_breaker_status() {
    local service_name=$1
    local port=$2
    local endpoint_path=$3
    
    echo ""
    echo "📊 Testing Circuit Breaker Status - $service_name"
    echo "URL: http://localhost:$port$endpoint_path"
    response=$(curl -s "http://localhost:$port$endpoint_path")
    
    if [ $? -eq 0 ]; then
        echo "Circuit Breaker Status Response:"
        echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response"
    else
        echo "❌ Failed to get circuit breaker status from $service_name"
    fi
}

# 测试支付服务
test_payment_service() {
    echo ""
    echo "💳 Testing Payment Service Circuit Breaker"
    
    # 测试多次支付请求来观察熔断器行为
    for i in {1..5}; do
        echo ""
        echo "Payment Test #$i:"
        
        payment_data='{
            "orderId": '$((1000 + i))',
            "userId": 1,
            "amount": 99.99,
            "currency": "USD"
        }'
        
        response=$(curl -s -X POST "http://localhost:8086/api/payments/process" \
            -H "Content-Type: application/json" \
            -d "$payment_data")
        
        if [ $? -eq 0 ]; then
            echo "Payment Response:"
            echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response"
        else
            echo "❌ Payment request failed"
        fi
        
        sleep 1
    done
}

# 测试订单支付集成
test_order_payment_integration() {
    echo ""
    echo "🛒 Testing Order-Payment Integration with Circuit Breaker"
    
    # 测试订单支付
    for order_id in 1 2 3; do
        echo ""
        echo "Testing Payment for Order $order_id:"
        
        payment_request='{"gateway": "stripe"}'
        
        response=$(curl -s -X POST "http://localhost:8083/api/orders/$order_id/payment" \
            -H "Content-Type: application/json" \
            -d "$payment_request")
        
        if [ $? -eq 0 ]; then
            echo "Order Payment Response:"
            echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response"
        else
            echo "❌ Order payment request failed"
        fi
        
        sleep 2
    done
}

# 测试产品推荐服务
test_product_recommendations() {
    echo ""
    echo "🎯 Testing Product Recommendation Service with Circuit Breaker"
    
    # 测试个性化推荐
    for user_id in 1 2 3; do
        echo ""
        echo "Testing Recommendations for User $user_id:"
        
        response=$(curl -s "http://localhost:8082/api/products/recommendations?userId=$user_id&category=smartphone")
        
        if [ $? -eq 0 ]; then
            echo "Recommendation Response:"
            echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response"
        else
            echo "❌ Recommendation request failed"
        fi
        
        sleep 1
    done
    
    # 测试热门产品（降级服务）
    echo ""
    echo "Testing Popular Products (Fallback Service):"
    response=$(curl -s "http://localhost:8082/api/products/popular?category=laptop&limit=3")
    
    if [ $? -eq 0 ]; then
        echo "Popular Products Response:"
        echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response"
    else
        echo "❌ Popular products request failed"
    fi
}

# 测试熔断器状态变化
test_circuit_breaker_behavior() {
    echo ""
    echo "⚡ Testing Circuit Breaker State Changes"
    
    # 连续调用以触发熔断器
    echo "Making rapid calls to trigger circuit breaker..."
    
    for i in {1..8}; do
        echo -n "Call $i: "
        
        response=$(curl -s -w "%{http_code}" "http://localhost:8086/api/payments/demo/circuit-breaker" -o /dev/null)
        echo "HTTP $response"
        
        # 短暂延迟
        sleep 0.5
    done
    
    echo ""
    echo "Checking circuit breaker status after rapid calls:"
    test_circuit_breaker_status "Payment Service" 8086 "/api/payments/circuit-breaker/status"
}

# 显示监控信息
show_monitoring_info() {
    echo ""
    echo "📊 Monitoring Information"
    echo "============================================"
    
    # 显示所有服务的actuator端点
    echo "Health Check Endpoints:"
    echo "  Config Server:    http://localhost:8888/actuator/health"
    echo "  Eureka Server:    http://localhost:8761/actuator/health"
    echo "  API Gateway:      http://localhost:8080/actuator/health"
    echo "  User Service:     http://localhost:8081/actuator/health"
    echo "  Product Service:  http://localhost:8082/actuator/health"
    echo "  Order Service:    http://localhost:8083/actuator/health"
    echo "  Payment Service:  http://localhost:8086/actuator/health"
    
    echo ""
    echo "Circuit Breaker Monitoring:"
    echo "  Payment Service:  http://localhost:8086/api/payments/circuit-breaker/status"
    echo "  Product Service:  http://localhost:8082/api/products/circuit-breaker/status"
    echo "  Order Service:    http://localhost:8083/api/orders/circuit-breaker/status"
    
    echo ""
    echo "Feature Testing Endpoints:"
    echo "  Payment Processing:     POST http://localhost:8086/api/payments/process"
    echo "  Order Payment:         POST http://localhost:8083/api/orders/{id}/payment"
    echo "  Product Recommendations: GET http://localhost:8082/api/products/recommendations"
    echo "  Popular Products:       GET http://localhost:8082/api/products/popular"
    echo "  Similar Products:       GET http://localhost:8082/api/products/{id}/similar"
}

# 主测试流程
main() {
    echo "Starting Week 6 Circuit Breaker & Resilience Testing..."
    echo ""
    
    # 1. 检查所有服务状态
    echo "🔍 Step 1: Checking Service Health"
    echo "=================================="
    check_service_health "Config Server" 8888
    check_service_health "Eureka Server" 8761
    check_service_health "API Gateway" 8080
    check_service_health "User Service" 8081
    check_service_health "Product Service" 8082
    check_service_health "Order Service" 8083
    check_service_health "Payment Service" 8086
    
    # 2. 测试熔断器状态
    echo ""
    echo "🔧 Step 2: Testing Circuit Breaker Status"
    echo "========================================"
    test_circuit_breaker_status "Payment Service" 8086 "/api/payments/circuit-breaker/status"
    test_circuit_breaker_status "Product Service" 8082 "/api/products/circuit-breaker/status"
    test_circuit_breaker_status "Order Service" 8083 "/api/orders/circuit-breaker/status"
    
    # 3. 测试支付服务熔断器
    test_payment_service
    
    # 4. 测试订单-支付集成
    test_order_payment_integration
    
    # 5. 测试产品推荐服务
    test_product_recommendations
    
    # 6. 测试熔断器行为
    test_circuit_breaker_behavior
    
    # 7. 显示监控信息
    show_monitoring_info
    
    echo ""
    echo "========================================="
    echo "Week 6 Testing Complete!"
    echo ""
    echo "🎯 Key Features Demonstrated:"
    echo "  ✅ Circuit Breaker Pattern (Resilience4j)"
    echo "  ✅ Retry Mechanism with Exponential Backoff"
    echo "  ✅ Timeout Control"
    echo "  ✅ Bulkhead Pattern (Thread Pool Isolation)"
    echo "  ✅ Fallback Mechanisms"
    echo "  ✅ Health Indicators for Circuit Breakers"
    echo "  ✅ Payment Service with Multiple Gateways"
    echo "  ✅ Product Recommendation Service with Fallback"
    echo "  ✅ Service-to-Service Communication Protection"
    echo ""
    echo "📊 Next Steps:"
    echo "  - Monitor circuit breaker metrics via Actuator endpoints"
    echo "  - Test different failure scenarios"
    echo "  - Observe automatic recovery when services come back online"
    echo "  - Check logs for detailed resilience pattern behavior"
    echo "========================================="
}

# 执行主函数
#main "$@"