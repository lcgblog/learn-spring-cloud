FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 复制jar文件
COPY target/payment-service-1.0.0.jar app.jar

# 暴露端口
EXPOSE 8086

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8086/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]