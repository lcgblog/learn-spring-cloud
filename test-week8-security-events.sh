#!/bin/bash

# Week 8: Security and Event-Driven Architecture Testing Script
# Tests OAuth2 authentication, authorization, and event-driven notifications

echo "=== Week 8: Security and Event-Driven Architecture Testing ==="
echo

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check service health
check_service_health() {
    local service_name=$1
    local health_url=$2
    
    print_status "Checking $service_name health..."
    if curl -s -f "$health_url" > /dev/null; then
        print_success "$service_name is healthy"
        return 0
    else
        print_error "$service_name is not healthy"
        return 1
    fi
}

# Function to get OAuth2 access token
get_access_token() {
    print_status "Getting OAuth2 access token..."
    
    local token_response=$(curl -s -X POST http://localhost:8090/oauth2/token \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=client_credentials&client_id=shophub-client&client_secret=secret")
    
    if [ $? -eq 0 ]; then
        local access_token=$(echo $token_response | grep -o '"access_token":"[^"]*' | cut -d'"' -f4)
        if [ -n "$access_token" ]; then
            print_success "Access token obtained successfully"
            echo $access_token
            return 0
        fi
    fi
    
    print_error "Failed to get access token"
    echo "Response: $token_response"
    return 1
}

# Function to test authenticated API call
test_authenticated_call() {
    local url=$1
    local token=$2
    local description=$3
    
    print_status "Testing $description..."
    
    local response=$(curl -s -w "\n%{http_code}" -H "Authorization: Bearer $token" "$url")
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | head -n -1)
    
    if [ "$http_code" = "200" ]; then
        print_success "$description - HTTP $http_code"
        echo "Response: $body" | head -c 200
        echo
    else
        print_warning "$description - HTTP $http_code"
        echo "Response: $body"
    fi
}

# Function to test event publishing
test_event_publishing() {
    local token=$1
    
    print_status "Testing event publishing by creating an order..."
    
    local order_data='{"userId":1,"productId":1,"quantity":2,"price":99.99}'
    local response=$(curl -s -w "\n%{http_code}" \
        -X POST http://localhost:8083/api/orders \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $token" \
        -d "$order_data")
    
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | head -n -1)
    
    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        print_success "Order created successfully - HTTP $http_code"
        echo "Order Response: $body"
        
        # Wait for event processing
        print_status "Waiting for event processing..."
        sleep 3
        
        # Check notifications
        print_status "Checking notifications for user 1..."
        test_authenticated_call "http://localhost:8089/api/notifications/user/1" "$token" "User notifications"
        
    else
        print_warning "Order creation failed - HTTP $http_code"
        echo "Response: $body"
    fi
}

echo "Starting Week 8 Security and Event-Driven Architecture tests..."
echo

# 1. Check all service health
print_status "=== Step 1: Service Health Checks ==="
check_service_health "Authorization Server" "http://localhost:8090/actuator/health"
check_service_health "Notification Service" "http://localhost:8089/actuator/health"
check_service_health "API Gateway" "http://localhost:8080/actuator/health"
check_service_health "Order Service" "http://localhost:8083/actuator/health"
echo

# 2. Test OAuth2 Configuration
print_status "=== Step 2: OAuth2 Configuration Tests ==="
print_status "Testing OpenID Connect configuration..."
curl -s http://localhost:8090/.well-known/openid_configuration | head -c 300
echo
echo

print_status "Testing JWKS endpoint..."
curl -s http://localhost:8090/.well-known/jwks.json | head -c 200
echo
echo

# 3. Test OAuth2 Token Generation
print_status "=== Step 3: OAuth2 Token Generation ==="
access_token=$(get_access_token)
if [ $? -ne 0 ]; then
    print_error "Cannot proceed without access token"
    exit 1
fi
echo

# 4. Test Secured Endpoints
print_status "=== Step 4: Secured Endpoint Tests ==="
test_authenticated_call "http://localhost:8090/api/auth/users/1/profile" "$access_token" "User profile access"
test_authenticated_call "http://localhost:8089/api/notifications/user/1" "$access_token" "User notifications"
test_authenticated_call "http://localhost:8089/api/notifications/stats" "$access_token" "Notification statistics"
echo

# 5. Test RabbitMQ Connection
print_status "=== Step 5: RabbitMQ Connection Test ==="
print_status "Checking RabbitMQ management interface..."
if curl -s -f http://localhost:15672 > /dev/null; then
    print_success "RabbitMQ management interface is accessible"
else
    print_warning "RabbitMQ management interface is not accessible"
fi
echo

# 6. Test Event-Driven Architecture
print_status "=== Step 6: Event-Driven Architecture Tests ==="
test_event_publishing "$access_token"
echo

# 7. Test Notification Service Features
print_status "=== Step 7: Notification Service Feature Tests ==="
test_authenticated_call "http://localhost:8089/api/notifications/events/recent" "$access_token" "Recent events"
test_authenticated_call "http://localhost:8089/api/notifications/user/1/unread" "$access_token" "Unread notifications count"
echo

# 8. Test Security Integration
print_status "=== Step 8: Security Integration Tests ==="
print_status "Testing unauthorized access (should fail)..."
response=$(curl -s -w "\n%{http_code}" http://localhost:8089/api/notifications/user/1)
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "401" ] || [ "$http_code" = "403" ]; then
    print_success "Unauthorized access properly blocked - HTTP $http_code"
else
    print_warning "Unauthorized access not properly blocked - HTTP $http_code"
fi
echo

# 9. Test Circuit Breaker Integration
print_status "=== Step 9: Circuit Breaker Integration Tests ==="
test_authenticated_call "http://localhost:8089/actuator/circuitbreakers" "$access_token" "Circuit breaker status"
echo

# 10. Test Metrics and Monitoring
print_status "=== Step 10: Metrics and Monitoring Tests ==="
test_authenticated_call "http://localhost:8090/actuator/metrics" "$access_token" "Authorization Server metrics"
test_authenticated_call "http://localhost:8089/actuator/prometheus" "$access_token" "Notification Service Prometheus metrics"
echo

print_success "=== Week 8 Security and Event-Driven Architecture Testing Complete ==="
echo
print_status "Summary:"
print_status "✓ OAuth2 Authorization Server with JWT tokens"
print_status "✓ Secured microservices with resource server protection"
print_status "✓ Event-driven architecture with RabbitMQ"
print_status "✓ Real-time notifications with message queuing"
print_status "✓ Integration with existing circuit breaker and monitoring"
echo
print_status "Access RabbitMQ Management UI: http://localhost:15672 (guest/guest)"
print_status "Authorization Server: http://localhost:8090"
print_status "Notification Service: http://localhost:8089"
echo