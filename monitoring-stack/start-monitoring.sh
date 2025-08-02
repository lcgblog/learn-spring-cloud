#!/bin/bash

echo "🚀 启动监控组件 (Zipkin + Prometheus + Grafana)..."

# 检查Docker是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker未运行，请先启动Docker"
    exit 1
fi

# 启动监控组件
docker-compose up -d

echo "✅ 监控组件启动完成！"
echo ""
echo "📊 访问地址："
echo "   Zipkin (分布式追踪): http://localhost:9411"
echo "   Prometheus (指标收集): http://localhost:9090"
echo "   Grafana (可视化面板): http://localhost:3000 (用户名: admin, 密码: admin)"
echo ""
echo "🔧 常用命令："
echo "   查看状态: docker-compose ps"
echo "   查看日志: docker-compose logs -f"
echo "   停止服务: docker-compose down"
echo "   重启服务: docker-compose restart" 