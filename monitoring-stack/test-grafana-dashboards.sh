#!/bin/bash

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "=========================================="
echo "Grafana 仪表板测试脚本"
echo "=========================================="
echo ""

# Function to check service health
check_service_health() {
    local service_name=$1
    local port=$2
    local max_attempts=30
    local attempt=1
    
    echo -e "${BLUE}[INFO]${NC} 检查 $service_name 服务状态..."
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s -f "http://localhost:$port/api/health" > /dev/null 2>&1; then
            echo -e "${GREEN}[SUCCESS]${NC} $service_name 服务运行正常"
            return 0
        else
            echo -e "${YELLOW}[WARNING]${NC} $service_name 服务未就绪，等待中... ($attempt/$max_attempts)"
            sleep 5
            ((attempt++))
        fi
    done
    
    echo -e "${RED}[ERROR]${NC} $service_name 服务启动失败或超时"
    return 1
}

# Function to test API endpoint
test_endpoint() {
    local name=$1
    local url=$2
    local expected_status=${3:-200}
    
    echo -e "${BLUE}[INFO]${NC} 测试 $name..."
    
    response=$(curl -s -w "%{http_code}" -o /tmp/response.json "$url")
    status_code="${response: -3}"
    
    if [ "$status_code" -eq "$expected_status" ]; then
        echo -e "${GREEN}[SUCCESS]${NC} $name 测试通过 (HTTP $status_code)"
        return 0
    else
        echo -e "${RED}[ERROR]${NC} $name 测试失败 (HTTP $status_code)"
        if [ -f /tmp/response.json ]; then
            echo -e "${RED}[ERROR]${NC} 错误响应: $(cat /tmp/response.json)"
        fi
        return 1
    fi
}

# 1. 检查Grafana服务状态
echo -e "${BLUE}[INFO]${NC} === 1. 检查Grafana服务状态 ==="
check_service_health "Grafana" 3000

# 2. 测试Grafana API
echo -e "${BLUE}[INFO]${NC} === 2. 测试Grafana API ==="
test_endpoint "Grafana健康检查" "http://localhost:3000/api/health"
test_endpoint "Grafana数据源列表" "http://localhost:3000/api/datasources"

# 3. 检查仪表板是否自动加载
echo -e "${BLUE}[INFO]${NC} === 3. 检查仪表板自动加载 ==="
test_endpoint "仪表板列表" "http://localhost:3000/api/search"

# 4. 检查Prometheus数据源
echo -e "${BLUE}[INFO]${NC} === 4. 检查Prometheus数据源 ==="
test_endpoint "Prometheus数据源" "http://localhost:3000/api/datasources/name/Prometheus"

# 5. 检查特定仪表板
echo -e "${BLUE}[INFO]${NC} === 5. 检查特定仪表板 ==="

# 检查Spring Boot仪表板
spring_boot_dashboard=$(curl -s "http://localhost:3000/api/search?query=Spring%20Boot%20应用监控" | grep -o '"uid":"[^"]*"' | head -1)
if [ ! -z "$spring_boot_dashboard" ]; then
    echo -e "${GREEN}[SUCCESS]${NC} Spring Boot仪表板已加载: $spring_boot_dashboard"
else
    echo -e "${YELLOW}[WARNING]${NC} Spring Boot仪表板未找到"
fi

# 检查业务指标仪表板
business_dashboard=$(curl -s "http://localhost:3000/api/search?query=ShopHub%20业务指标监控" | grep -o '"uid":"[^"]*"' | head -1)
if [ ! -z "$business_dashboard" ]; then
    echo -e "${GREEN}[SUCCESS]${NC} 业务指标仪表板已加载: $business_dashboard"
else
    echo -e "${YELLOW}[WARNING]${NC} 业务指标仪表板未找到"
fi

# 6. 检查Prometheus数据
echo -e "${BLUE}[INFO]${NC} === 6. 检查Prometheus数据 ==="
test_endpoint "Prometheus健康检查" "http://localhost:9090/-/healthy"

# 检查是否有ShopHub相关指标
prometheus_targets=$(curl -s "http://localhost:9090/api/v1/targets")
if echo "$prometheus_targets" | grep -q "shophub\|spring"; then
    echo -e "${GREEN}[SUCCESS]${NC} 发现ShopHub/Spring相关监控目标"
else
    echo -e "${YELLOW}[WARNING]${NC} 暂未发现ShopHub/Spring相关监控目标"
fi

# 7. 显示访问信息
echo ""
echo "=========================================="
echo -e "${GREEN}Grafana仪表板测试完成！${NC}"
echo "=========================================="
echo ""
echo "📊 访问地址："
echo "   Grafana: http://localhost:3000"
echo "   - 用户名: admin"
echo "   - 密码: admin"
echo ""
echo "📈 预配置的仪表板："
echo "   1. ShopHub Spring Boot 应用监控"
echo "   2. ShopHub 业务指标监控"
echo ""
echo "🔍 手动检查步骤："
echo "   1. 登录Grafana (admin/admin)"
echo "   2. 进入Dashboards页面"
echo "   3. 查看是否自动加载了两个仪表板"
echo "   4. 检查Prometheus数据源是否正确配置"
echo ""
echo "⚠️  注意事项："
echo "   - 仪表板需要Prometheus中有数据才能显示图表"
echo "   - 确保所有Spring Boot服务都已启动并配置了指标导出"
echo "   - 首次加载可能需要几分钟时间"

# 清理临时文件
rm -f /tmp/response.json 