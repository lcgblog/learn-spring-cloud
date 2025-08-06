#!/bin/bash

echo "🏗️  Maven多模块构建脚本"
echo "=========================="

# 检查Maven是否安装
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven未安装，请先安装Maven"
    exit 1
fi

echo ""
echo "📋 1. 清理所有模块"
echo "=================="
mvn clean

echo ""
echo "🔧 2. 编译所有模块"
echo "=================="
mvn compile

echo ""
echo "📦 3. 打包所有模块"
echo "=================="
mvn package -DskipTests

echo ""
echo "🧪 4. 运行测试 (可选)"
echo "===================="
read -p "是否要运行所有测试？(y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    mvn test
else
    echo "跳过测试阶段"
fi

echo ""
echo "📊 5. 生成项目报告"
echo "=================="
mvn site

echo ""
echo "🐳 6. Docker镜像构建 (可选)"
echo "=========================="
read -p "是否要构建Docker镜像？(y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    mvn package dockerfile:build -Pdocker
else
    echo "跳过Docker构建"
fi

echo ""
echo "📈 7. 依赖分析"
echo "=============="
echo "分析项目依赖树..."
mvn dependency:tree

echo ""
echo "🎯 构建摘要"
echo "==========="
echo "✅ 根pom管理: 统一版本控制"
echo "✅ 子模块: 8个微服务模块 (包含Week8新增)"
echo "✅ 依赖管理: Spring Boot + Spring Cloud + OAuth2 + RabbitMQ"
echo "✅ 构建工具: Maven多模块构建"
echo "✅ 配置文件: 统一的构建配置"
echo "✅ Week8新增: OAuth2授权服务器 + 事件驱动通知服务"

echo ""
echo "🚀 常用Maven命令"
echo "==============="
echo "# 清理并重新构建所有模块"
echo "mvn clean install"
echo ""
echo "# 只构建特定模块"
echo "mvn clean install -pl config-server"
echo "mvn clean install -pl authorization-server"
echo "mvn clean install -pl notification-service"
echo ""
echo "# 构建特定模块及其依赖"
echo "mvn clean install -pl config-server -am"
echo ""
echo "# 跳过测试构建"
echo "mvn clean install -DskipTests"
echo ""
echo "# 使用特定Profile构建"
echo "mvn clean install -Pprod"
echo ""
echo "# 并行构建加速"
echo "mvn clean install -T 4"

echo ""
echo "🏆 多模块构建完成!"