# 监控组件 (Monitoring Stack)

这个目录包含了独立的监控组件配置，包括Zipkin、Prometheus和Grafana。

## 🚀 快速启动

### 方法1: 使用启动脚本（推荐）
```bash
./start-monitoring.sh
```

### 方法2: 直接使用docker-compose
```bash
docker-compose up -d
```

## 🛑 停止服务

### 方法1: 使用停止脚本
```bash
./stop-monitoring.sh
```

### 方法2: 直接使用docker-compose
```bash
docker-compose down
```

## 📊 访问地址

启动成功后，可以通过以下地址访问各个组件：

- **Zipkin (分布式追踪)**: http://localhost:9411
- **Prometheus (指标收集)**: http://localhost:9090
- **Grafana (可视化面板)**: http://localhost:3000
  - 用户名: `admin`
  - 密码: `admin`

## 🔧 常用命令

```bash
# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 查看特定服务的日志
docker-compose logs -f zipkin
docker-compose logs -f prometheus
docker-compose logs -f grafana

# 重启服务
docker-compose restart

# 完全清理（包括数据）
docker-compose down -v
```

## 📁 目录结构

```
monitoring-stack/
├── docker-compose.yml          # Docker Compose配置
├── prometheus.yml              # Prometheus配置
├── start-monitoring.sh         # 启动脚本
├── stop-monitoring.sh          # 停止脚本
├── README.md                   # 说明文档
└── grafana/
    ├── provisioning/
    │   ├── datasources/
    │   │   └── prometheus.yml  # Grafana数据源配置
    │   └── dashboards/
    │       └── dashboard.yml   # Grafana仪表板配置
    └── dashboards/
        ├── spring-boot-dashboard.json      # Spring Boot技术监控仪表板
        └── shopHub-business-metrics.json   # ShopHub业务指标监控仪表板
```

## 🔍 功能说明

### Zipkin
- 分布式追踪系统
- 用于跟踪微服务之间的调用链路
- 支持Spring Cloud Sleuth集成

### Prometheus
- 指标收集和存储系统
- 支持Spring Boot Actuator指标
- 提供强大的查询语言PromQL

### Grafana
- 数据可视化和监控面板
- 预配置了Prometheus数据源
- 包含两个预配置的监控仪表板：
  - **Spring Boot技术监控仪表板**: 监控JVM、HTTP、熔断器等技术指标
  - **ShopHub业务指标监控仪表板**: 监控各服务的业务调用情况和性能指标

## 🎯 与Spring Boot应用集成

确保您的Spring Boot应用配置了以下依赖：

```xml
<!-- Spring Boot Actuator -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Micrometer Prometheus Registry -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<!-- Spring Cloud Sleuth (可选，用于分布式追踪) -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>
```

在`application.yml`中配置：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
``` 