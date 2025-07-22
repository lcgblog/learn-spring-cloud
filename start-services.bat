@echo off
echo ===========================================
echo        ShopHub 微服务启动脚本
echo ===========================================
echo.

echo 正在启动 Eureka Server...
cd eureka-server
start "Eureka Server" cmd /k "mvn spring-boot:run"
timeout /t 30 /nobreak

echo.
echo 正在启动 User Service...
cd ..\user-service
start "User Service" cmd /k "mvn spring-boot:run"
timeout /t 15 /nobreak

echo.
echo 正在启动 Product Service...
cd ..\product-service  
start "Product Service" cmd /k "mvn spring-boot:run"
timeout /t 15 /nobreak

echo.
echo 正在启动 Order Service...
cd ..\order-service
start "Order Service" cmd /k "mvn spring-boot:run"

cd ..
echo.
echo ===========================================
echo 所有服务启动完成！
echo.
echo 访问以下地址查看服务状态：
echo.
echo Eureka Dashboard: http://localhost:8761
echo User Service: http://localhost:8081/api/users/health
echo Product Service: http://localhost:8082/actuator/health  
echo Order Service: http://localhost:8083/actuator/health
echo.
echo 按任意键退出...
echo ===========================================
pause 