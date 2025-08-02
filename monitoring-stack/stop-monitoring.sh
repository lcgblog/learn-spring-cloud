#!/bin/bash

echo "🛑 停止监控组件..."

# 停止并移除容器
docker-compose down

echo "✅ 监控组件已停止！"
echo ""
echo "💾 数据已保留在Docker volumes中"
echo "   如需完全清理数据，请运行: docker-compose down -v" 