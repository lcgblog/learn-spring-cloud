FROM openjdk:17-jdk-slim

LABEL maintainer="ShopHub Team <dev@shophub.com>"
LABEL version="1.0.0"
LABEL description="ShopHub Metrics Collector Service with distributed tracing and observability"

# Install required packages
RUN apt-get update && apt-get install -y \
    curl \
    netcat-openbsd \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy the jar file
COPY target/metrics-collector-1.0.0.jar app.jar

# Create a non-root user
RUN groupadd -r shophub && useradd -r -g shophub shophub
RUN chown -R shophub:shophub /app
USER shophub

# Expose ports
EXPOSE 8087 9087

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8087/actuator/health || exit 1

# Environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SPRING_PROFILES_ACTIVE=docker

# Entry point
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]