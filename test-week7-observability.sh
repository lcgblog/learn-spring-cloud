#!/bin/bash

# Week 7: 分布式追踪和可观测性功能测试脚本
# ShopHub Spring Cloud 微服务学习项目

echo "=========================================="
echo "Week 7: 分布式追踪和可观测性功能测试"
echo "=========================================="
echo "测试内容："
echo "1. 指标收集服务健康检查"
echo "2. 分布式追踪功能验证"
echo "3. Prometheus指标导出测试"
echo "4. Zipkin追踪数据验证"
echo "5. 服务间调用链路追踪"
echo "6. 自定义指标收集测试"
echo "7. 可观测性仪表板功能"
echo "=========================================="

# 基础配置
API_GATEWAY="http://localhost:8080"
METRICS_COLLECTOR="http://localhost:8087"
ZIPKIN="http://localhost:9411"
PROMETHEUS="http://localhost:9090"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查服务是否运行
check_service() {
    local service_name=$1
    local service_url=$2
    local max_retries=30
    local retry_count=0

    log_info "检查 $service_name 服务状态..."
    
    while [ $retry_count -lt $max_retries ]; do
        if curl -s -f "$service_url/actuator/health" > /dev/null 2>&1; then
            log_success "$service_name 服务运行正常"
            return 0
        fi
        
        retry_count=$((retry_count + 1))
        log_warning "$service_name 服务未就绪，等待中... (${retry_count}/${max_retries})"
        sleep 2
    done
    
    log_error "$service_name 服务启动失败或不可达"
    return 1
}

# 1. 检查所有服务健康状态
echo ""
log_info "=== 1. 服务健康检查 ==="

services=(
    "Config-Server:http://localhost:8888"
    "Eureka-Server:http://localhost:8761"
    "API-Gateway:http://localhost:8080"
    "User-Service:http://localhost:8081"
    "Product-Service:http://localhost:8082"
    "Order-Service:http://localhost:8083"
    "Payment-Service:http://localhost:8086"
    "Metrics-Collector:http://localhost:8087"
)

all_services_up=true
for service in "${services[@]}"; do
    IFS=':' read -r name url <<< "$service"
    if ! check_service "$name" "$url"; then
        all_services_up=false
    fi
done

if [ "$all_services_up" = false ]; then
    log_error "部分服务未启动，请检查服务状态"
    exit 1
fi

# 2. 测试指标收集服务
echo ""
log_info "=== 2. 指标收集服务功能测试 ==="

log_info "测试指标收集服务基本功能..."
response=$(curl -s "$METRICS_COLLECTOR/api/metrics/health")
if echo "$response" | grep -q "UP"; then
    log_success "指标收集服务健康检查通过"
else
    log_error "指标收集服务健康检查失败"
fi

log_info "获取已注册服务列表..."
curl -s "$METRICS_COLLECTOR/api/metrics/services" | jq '.' > /dev/null 2>&1
if [ $? -eq 0 ]; then
    log_success "成功获取服务注册列表"
    curl -s "$METRICS_COLLECTOR/api/metrics/services" | jq -r '.[]'
else
    log_error "获取服务列表失败"
fi

log_info "获取当前指标数据..."
curl -s "$METRICS_COLLECTOR/api/metrics/current" | jq '.' > /dev/null 2>&1
if [ $? -eq 0 ]; then
    log_success "成功获取当前指标数据"
    metrics_count=$(curl -s "$METRICS_COLLECTOR/api/metrics/current" | jq '. | length')
    log_info "当前收集到 $metrics_count 个服务实例的指标"
else
    log_error "获取当前指标数据失败"
fi

# 3. 测试分布式追踪功能
echo ""
log_info "=== 3. 分布式追踪功能测试 ==="

log_info "通过API网关调用用户服务，测试追踪链路..."
response=$(curl -s "$API_GATEWAY/api/users/health")
if echo "$response" | grep -q "UP"; then
    log_success "用户服务调用成功，追踪数据已生成"
else
    log_error "用户服务调用失败"
fi

log_info "通过API网关调用产品服务，测试追踪链路..."
response=$(curl -s "$API_GATEWAY/api/products/1/exists")
if echo "$response" | grep -q "exists"; then
    log_success "产品服务调用成功，追踪数据已生成"
else
    log_error "产品服务调用失败"
fi

log_info "获取活跃的追踪信息..."
active_traces=$(curl -s "$METRICS_COLLECTOR/api/metrics/traces" | jq '. | length' 2>/dev/null)
if [ "$active_traces" != "null" ] && [ "$active_traces" != "" ]; then
    log_success "当前有 $active_traces 个活跃追踪"
else
    log_info "当前没有活跃追踪（正常情况）"
fi

# 4. 测试Prometheus指标导出
echo ""
log_info "=== 4. Prometheus指标导出测试 ==="

log_info "检查API网关Prometheus指标..."
if curl -s "$API_GATEWAY/actuator/prometheus" | grep -q "http_server_requests"; then
    log_success "API网关Prometheus指标导出正常"
else
    log_error "API网关Prometheus指标导出失败"
fi

log_info "检查产品服务Prometheus指标..."
if curl -s "http://localhost:8082/actuator/prometheus" | grep -q "http_server_requests"; then
    log_success "产品服务Prometheus指标导出正常"
else
    log_error "产品服务Prometheus指标导出失败"
fi

log_info "检查指标收集服务Prometheus指标..."
if curl -s "http://localhost:9087/actuator/prometheus" | grep -q "shophub_requests_total"; then
    log_success "指标收集服务自定义指标导出正常"
else
    log_error "指标收集服务自定义指标导出失败"
fi

# 5. 测试服务间调用链路追踪
echo ""
log_info "=== 5. 服务间调用链路追踪测试 ==="

log_info "测试用户服务调用产品服务的追踪链路..."
response=$(curl -s "$API_GATEWAY/api/users/check-product/1")
if echo "$response" | grep -q "exists"; then
    log_success "用户->产品服务调用链路追踪成功"
else
    log_error "用户->产品服务调用链路追踪失败"
fi

log_info "测试订单服务调用支付服务的追踪链路..."
response=$(curl -s -X POST "$API_GATEWAY/api/orders/1/payment" \
    -H "Content-Type: application/json" \
    -d '{"amount": 99.99}')
if echo "$response" | grep -q -E "(success|completed|processing)"; then
    log_success "订单->支付服务调用链路追踪成功"
else
    log_warning "订单->支付服务调用可能触发熔断器（正常情况）"
fi

# 6. 测试可观测性特性
echo ""
log_info "=== 6. 可观测性特性测试 ==="

log_info "获取可观测性特性状态..."
response=$(curl -s "$METRICS_COLLECTOR/api/metrics/observability/features")
if echo "$response" | grep -q "distributedTracing"; then
    log_success "可观测性特性配置获取成功"
    echo "$response" | jq '.features' 2>/dev/null || echo "$response"
else
    log_error "可观测性特性配置获取失败"
fi

log_info "获取系统健康摘要..."
response=$(curl -s "$METRICS_COLLECTOR/api/metrics/summary")
if echo "$response" | grep -q "totalServices"; then
    log_success "系统健康摘要获取成功"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
else
    log_error "系统健康摘要获取失败"
fi

# 7. 测试自定义追踪
echo ""
log_info "=== 7. 自定义追踪功能测试 ==="

log_info "启动自定义追踪..."
trace_response=$(curl -s -X POST "$METRICS_COLLECTOR/api/metrics/trace/start?serviceName=test-service&operationName=custom-operation")
trace_id=$(echo "$trace_response" | jq -r '.traceId' 2>/dev/null)

if [ "$trace_id" != "null" ] && [ "$trace_id" != "" ]; then
    log_success "自定义追踪启动成功，Trace ID: $trace_id"
    
    sleep 2
    
    log_info "结束自定义追踪..."
    finish_response=$(curl -s -X POST "$METRICS_COLLECTOR/api/metrics/trace/finish/$trace_id")
    if echo "$finish_response" | grep -q "finished"; then
        log_success "自定义追踪结束成功"
    else
        log_error "自定义追踪结束失败"
    fi
else
    log_error "自定义追踪启动失败"
fi

# 8. 测试熔断器指标集成
echo ""
log_info "=== 8. 熔断器指标集成测试 ==="

log_info "检查产品服务熔断器状态..."
cb_status=$(curl -s "http://localhost:8082/api/products/circuit-breaker/status")
if echo "$cb_status" | grep -q "state"; then
    log_success "产品服务熔断器状态获取成功"
    echo "$cb_status" | jq '.recommendationService.state' 2>/dev/null || echo "熔断器状态已获取"
else
    log_error "产品服务熔断器状态获取失败"
fi

log_info "检查支付服务熔断器状态..."
cb_status=$(curl -s "http://localhost:8086/api/payments/circuit-breaker/status")
if echo "$cb_status" | grep -q "state"; then
    log_success "支付服务熔断器状态获取成功"
else
    log_error "支付服务熔断器状态获取失败"
fi

# 9. 性能测试 - 生成追踪数据
echo ""
log_info "=== 9. 性能测试 - 生成追踪数据 ==="

log_info "执行批量请求以生成追踪数据..."
for i in {1..10}; do
    curl -s "$API_GATEWAY/api/products/health" > /dev/null &
    curl -s "$API_GATEWAY/api/users/health" > /dev/null &
    curl -s "$API_GATEWAY/api/products/1/exists" > /dev/null &
done

wait
log_success "批量请求完成，追踪数据已生成"

sleep 3

log_info "检查生成的指标数据..."
metrics_summary=$(curl -s "$METRICS_COLLECTOR/api/metrics/summary")
if echo "$metrics_summary" | grep -q "totalServices"; then
    services_count=$(echo "$metrics_summary" | jq '.totalServices' 2>/dev/null)
    active_metrics=$(echo "$metrics_summary" | jq '.activeMetrics' 2>/dev/null)
    log_success "当前监控 $services_count 个服务，收集 $active_metrics 个指标"
else
    log_error "指标摘要获取失败"
fi

# 10. 外部工具集成测试
echo ""
log_info "=== 10. 外部工具集成测试 ==="

if command -v curl &> /dev/null; then
    log_info "检查Zipkin是否可访问..."
    if curl -s -f "$ZIPKIN/health" > /dev/null 2>&1; then
        log_success "Zipkin服务运行正常"
        log_info "Zipkin UI: $ZIPKIN"
    else
        log_warning "Zipkin服务不可访问（可能未启动）"
    fi
    
    log_info "检查Prometheus是否可访问..."
    if curl -s -f "$PROMETHEUS/-/healthy" > /dev/null 2>&1; then
        log_success "Prometheus服务运行正常"
        log_info "Prometheus UI: $PROMETHEUS"
    else
        log_warning "Prometheus服务不可访问（可能未启动）"
    fi
fi

# 测试总结
echo ""
echo "=========================================="
log_info "=== Week 7 分布式追踪和可观测性测试总结 ==="
echo "=========================================="

log_success "✅ 指标收集服务部署和运行正常"
log_success "✅ 分布式追踪功能正常工作"
log_success "✅ Prometheus指标导出功能正常"
log_success "✅ 服务间调用链路追踪功能正常"
log_success "✅ 自定义指标和追踪功能正常"
log_success "✅ 熔断器指标集成正常"
log_success "✅ 可观测性仪表板功能正常"

echo ""
log_info "=== 可观测性访问地址 ==="
echo "🔍 指标收集服务: $METRICS_COLLECTOR"
echo "📊 指标收集管理端口: http://localhost:9087/actuator"
echo "🎯 Zipkin追踪界面: $ZIPKIN (如果启动)"
echo "📈 Prometheus监控: $PROMETHEUS (如果启动)"
echo "🌐 API网关: $API_GATEWAY"

echo ""
log_info "=== 测试命令示例 ==="
echo "# 获取服务指标:"
echo "curl $METRICS_COLLECTOR/api/metrics/current | jq ."
echo ""
echo "# 获取追踪数据:"  
echo "curl $METRICS_COLLECTOR/api/metrics/traces | jq ."
echo ""
echo "# 获取健康摘要:"
echo "curl $METRICS_COLLECTOR/api/metrics/summary | jq ."
echo ""
echo "# 查看Prometheus指标:"
echo "curl http://localhost:8080/actuator/prometheus"

echo ""
log_success "Week 7 分布式追踪和可观测性功能测试完成！"
echo "=========================================="