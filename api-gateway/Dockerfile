FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 复制已构建的JAR文件
COPY target/api-gateway-1.0.0.jar app.jar

# 运行应用
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]