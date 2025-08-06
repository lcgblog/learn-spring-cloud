FROM openjdk:17-jdk-slim

VOLUME /tmp

COPY target/eureka-server-1.0.0.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java","-jar","/app.jar"]