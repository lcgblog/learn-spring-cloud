#!/bin/bash

# Week 4: Load Balancing & Client-Side Discovery 测试脚本
# 测试多实例负载均衡和Feign客户端通信

echo "🎯 Week 4: Load Balancing & Client-Side Discovery 测试"
echo "=================================================="

# 检查必要的服务是否运行
check_service() {
    local url=$1
    local name=$2
    
    response=$(curl -s "$url" 2>/dev/null)
    if [ $? -eq 0 ]; then
        echo "✅ $name 运行正常"
        return 0
    else
        echo "❌ $name 未运行 ($url)"
        return 1
    fi
}

echo ""
echo "📋 1. 检查服务状态"
echo "=================="

services_running=true

# 检查核心服务
check_service "http://localhost:8761/actuator/health" "Eureka Server" || services_running=false
check_service "http://localhost:8080/actuator/health" "API Gateway" || services_running=false  
check_service "http://localhost:8083/actuator/health" "Order Service" || services_running=false

# 检查多个Product Service实例
echo ""
echo "检查Product Service多实例:"
for port in 8082 8084 8085; do
    check_service "http://localhost:$port/api/products/health" "Product Service ($port)" || true
done

if [ "$services_running" = false ]; then
    echo ""
    echo "⚠️  请确保所有必要的服务都在运行！"
    echo "   启动命令："
    echo "   1. cd eureka-server && mvn spring-boot:run &"
    echo "   2. cd api-gateway && mvn spring-boot:run &"  
    echo "   3. cd order-service && mvn spring-boot:run &"
    echo "   4. ./start-multiple-products.sh (启动多个产品服务实例)"
fi

echo ""
echo "🔍 2. 测试服务发现和注册"
echo "======================"

echo "检查Eureka注册的服务实例："
eureka_response=$(curl -s "http://localhost:8761/eureka/apps" 2>/dev/null)
if [ $? -eq 0 ]; then
    echo "✅ 成功获取Eureka服务注册表"
    
    # 计算product-service实例数量
    product_instances=$(echo "$eureka_response" | grep -o "PRODUCT-SERVICE" | wc -l)
    echo "📊 发现 $product_instances 个 Product Service 实例"
else
    echo "❌ 无法连接到Eureka服务器"
fi

echo ""
echo "⚖️  3. 负载均衡测试"
echo "=================="

echo "通过Order Service调用Product Service (演示负载均衡):"
echo "多次调用 /api/orders/load-balance-demo 观察不同实例响应"

for i in {1..5}; do
    echo ""
    echo "第 $i 次调用："
    response=$(curl -s "http://localhost:8080/api/orders/load-balance-demo" 2>/dev/null)
    
    if [ $? -eq 0 ]; then
        # 提取服务实例信息
        service_instance=$(echo "$response" | jq -r '.productServiceResponse.serviceInstance // .productServiceResponse.service + ":" + (.productServiceResponse.port // "unknown")' 2>/dev/null)
        message=$(echo "$response" | jq -r '.message // "调用成功"' 2>/dev/null)
        
        if [ "$service_instance" != "null" ] && [ "$service_instance" != "" ]; then
            echo "✅ $message -> $service_instance"
        else
            echo "✅ 调用成功 (无法解析实例信息)"
        fi
    else
        echo "❌ 调用失败"
    fi
    
    sleep 1
done

echo ""
echo "🔗 4. Feign客户端通信测试"  
echo "======================="

echo "测试Order Service通过Feign调用Product Service:"

# 测试产品存在性验证
for product_id in 1 2 3 999; do
    echo ""
    echo "验证产品 ID: $product_id"
    
    response=$(curl -s "http://localhost:8080/api/orders/verify-product/$product_id" 2>/dev/null)
    
    if [ $? -eq 0 ]; then
        exists=$(echo "$response" | jq -r '.productVerification.exists // true' 2>/dev/null)
        service_instance=$(echo "$response" | jq -r '.productVerification.serviceInstance // "unknown"' 2>/dev/null)
        message=$(echo "$response" | jq -r '.productVerification.message // "验证完成"' 2>/dev/null)
        
        if [ "$exists" = "true" ]; then
            echo "✅ 产品存在 -> $service_instance"
        else
            echo "❌ 产品不存在 -> $service_instance"
        fi
        echo "   消息: $message"
    else
        echo "❌ 验证请求失败"
    fi
done

echo ""
echo "📊 5. 负载均衡策略验证"
echo "===================="

echo "收集10次调用的实例分布情况："

declare -A instance_count
total_calls=0
successful_calls=0

for i in {1..10}; do
    response=$(curl -s "http://localhost:8080/api/orders/load-balance-demo" 2>/dev/null)
    
    if [ $? -eq 0 ]; then
        service_instance=$(echo "$response" | jq -r '.productServiceResponse.serviceInstance // .productServiceResponse.service + ":" + (.productServiceResponse.port // "unknown")' 2>/dev/null)
        
        if [ "$service_instance" != "null" ] && [ "$service_instance" != "" ]; then
            instance_count["$service_instance"]=$((${instance_count["$service_instance"]} + 1))
            successful_calls=$((successful_calls + 1))
        fi
    fi
    
    total_calls=$((total_calls + 1))
    printf "."
done

echo ""
echo ""
echo "📈 负载分布统计:"
echo "==============="
echo "总调用次数: $total_calls"  
echo "成功次数: $successful_calls"
echo ""

for instance in "${!instance_count[@]}"; do
    count=${instance_count[$instance]}
    percentage=$(( count * 100 / successful_calls ))
    echo "$instance: $count 次 ($percentage%)"
done

echo ""
echo "🔧 6. 重试和容错测试"
echo "=================="

echo "模拟服务故障场景..."
echo "注意: 这需要手动停止一个Product Service实例来观察重试机制"

# 尝试访问可能不存在的服务实例
echo ""
echo "测试重试机制 (如果某个实例不可用):"
for i in {1..3}; do
    echo ""
    echo "第 $i 次重试测试："
    
    start_time=$(date +%s%3N)
    response=$(curl -s "http://localhost:8080/api/orders/load-balance-demo" 2>/dev/null)
    end_time=$(date +%s%3N)
    
    duration=$((end_time - start_time))
    
    if [ $? -eq 0 ]; then
        service_instance=$(echo "$response" | jq -r '.productServiceResponse.serviceInstance // "unknown"' 2>/dev/null)
        echo "✅ 调用成功 -> $service_instance (耗时: ${duration}ms)"
    else
        echo "❌ 调用失败 (耗时: ${duration}ms)"
    fi
done

echo ""
echo "🎯 7. Week 4 学习要点总结"
echo "======================="

echo "✅ 已实现的功能:"
echo "   - ✓ 多实例Product Service部署 (端口8082, 8084, 8085)"
echo "   - ✓ Spring Cloud LoadBalancer客户端负载均衡"
echo "   - ✓ OpenFeign声明式服务调用"
echo "   - ✓ 自定义负载均衡策略 (轮询算法)"
echo "   - ✓ 重试机制和指数退避 (Resilience4j)"
echo "   - ✓ 服务间通信容错处理"

echo ""
echo "📚 关键技术点:"
echo "   - ReactorLoadBalancer: 自定义负载均衡策略"
echo "   - @FeignClient: 声明式HTTP客户端"
echo "   - Resilience4j: 重试和熔断器模式"
echo "   - ServiceInstanceListSupplier: 服务实例发现"

echo ""  
echo "🚀 建议的进一步测试:"
echo "   1. 停止某个Product Service实例，观察负载均衡自动切换"
echo "   2. 启动更多实例，验证动态负载分布"
echo "   3. 模拟网络延迟，测试重试机制的表现"
echo "   4. 查看Eureka Dashboard观察实例注册状态"

echo ""
echo "📖 Week 4 测试完成！"
echo "下一步: Week 5 - Configuration Management (配置中心)"