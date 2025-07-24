#!/bin/bash

# Week 4: 启动多个Product Service实例演示负载均衡
# 启动3个product-service实例在不同端口

echo "🚀 启动多个Product Service实例进行负载均衡演示..."

# 杀死可能存在的实例
echo "清理现有实例..."
pkill -f "product-service"
sleep 2

# 启动第一个实例 (端口 8082)
echo "启动Product Service实例1 (端口8082)..."
cd product-service
PORT=8082 mvn spring-boot:run > ../logs/product-8082.log 2>&1 &
INSTANCE1_PID=$!
cd ..

# 等待第一个实例启动
sleep 15

# 启动第二个实例 (端口 8084) 
echo "启动Product Service实例2 (端口8084)..."
cd product-service
PORT=8084 mvn spring-boot:run > ../logs/product-8084.log 2>&1 &
INSTANCE2_PID=$!
cd ..

# 等待第二个实例启动
sleep 15

# 启动第三个实例 (端口 8085)
echo "启动Product Service实例3 (端口8085)..."
cd product-service  
PORT=8085 mvn spring-boot:run > ../logs/product-8085.log 2>&1 &
INSTANCE3_PID=$!
cd ..

# 创建日志目录
mkdir -p logs

echo "✅ 已启动3个Product Service实例："
echo "   - 实例1: http://localhost:8082 (PID: $INSTANCE1_PID)"
echo "   - 实例2: http://localhost:8084 (PID: $INSTANCE2_PID)" 
echo "   - 实例3: http://localhost:8085 (PID: $INSTANCE3_PID)"
echo ""
echo "📝 日志文件位置："
echo "   - logs/product-8082.log"
echo "   - logs/product-8084.log"
echo "   - logs/product-8085.log"
echo ""
echo "🛑 停止所有实例: kill $INSTANCE1_PID $INSTANCE2_PID $INSTANCE3_PID"

# 保存PID以便后续停止
echo "$INSTANCE1_PID $INSTANCE2_PID $INSTANCE3_PID" > product-instances.pid

echo "⏳ 等待30秒让所有实例完全启动..."
sleep 30

echo "🧪 测试实例是否启动成功..."
for port in 8082 8084 8085; do
    response=$(curl -s "http://localhost:$port/api/products/health" 2>/dev/null)
    if [ $? -eq 0 ]; then
        echo "✅ Port $port: $response"
    else
        echo "❌ Port $port: 启动失败"
    fi
done

echo ""
echo "🎯 现在可以通过API Gateway测试负载均衡："
echo "   curl http://localhost:8080/api/products/health"
echo "   (多次执行会看到不同的serviceInstance)"