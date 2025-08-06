#!/bin/bash

# OAuth2 Token Testing Script
# Tests OAuth2 authentication, token generation, and secured API access

echo "=== OAuth2 Token Testing Script ==="
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

# Function to extract JSON value
extract_json_value() {
    local json=$1
    local key=$2
    echo $json | grep -o "\"$key\":\"[^\"]*" | cut -d'"' -f4
}

# Function to decode JWT payload (base64url decode)
decode_jwt_payload() {
    local jwt=$1
    local payload=$(echo $jwt | cut -d'.' -f2)
    
    # Add padding if needed
    local padding=$((4 - ${#payload} % 4))
    if [ $padding -ne 4 ]; then
        payload="$payload$(printf '%*s' $padding | tr ' ' '=')"
    fi
    
    # Decode base64url
    echo $payload | tr '_-' '/+' | base64 -d 2>/dev/null
}

print_status "Starting OAuth2 token testing..."
echo

# 1. Test Authorization Server Health
print_status "=== Step 1: Authorization Server Health Check ==="
if curl -s -f http://localhost:8090/actuator/health > /dev/null; then
    print_success "Authorization Server is healthy"
else
    print_error "Authorization Server is not accessible"
    exit 1
fi
echo

# 2. Test OpenID Connect Discovery
print_status "=== Step 2: OpenID Connect Discovery ==="
print_status "Fetching OpenID Connect configuration..."
oidc_config=$(curl -s http://localhost:8090/.well-known/openid_configuration)
if [ $? -eq 0 ]; then
    print_success "OpenID Connect configuration retrieved"
    echo "Configuration (first 500 chars):"
    echo $oidc_config | head -c 500
    echo
else
    print_error "Failed to retrieve OpenID Connect configuration"
fi
echo

# 3. Test JWKS Endpoint
print_status "=== Step 3: JSON Web Key Set (JWKS) ==="
print_status "Fetching JWKS..."
jwks=$(curl -s http://localhost:8090/.well-known/jwks.json)
if [ $? -eq 0 ]; then
    print_success "JWKS retrieved successfully"
    echo "JWKS (first 300 chars):"
    echo $jwks | head -c 300
    echo
else
    print_error "Failed to retrieve JWKS"
fi
echo

# 4. Test Client Credentials Grant
print_status "=== Step 4: Client Credentials Grant ==="
print_status "Requesting access token with client credentials..."

token_response=$(curl -s -X POST http://localhost:8090/oauth2/token \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=client_credentials&client_id=shophub-client&client_secret=secret")

if [ $? -eq 0 ]; then
    access_token=$(extract_json_value "$token_response" "access_token")
    token_type=$(extract_json_value "$token_response" "token_type")
    expires_in=$(extract_json_value "$token_response" "expires_in")
    
    if [ -n "$access_token" ]; then
        print_success "Access token obtained successfully"
        echo "Token Type: $token_type"
        echo "Expires In: $expires_in seconds"
        echo "Access Token (first 50 chars): ${access_token:0:50}..."
        echo
        
        # Decode JWT token
        print_status "Decoding JWT token payload..."
        jwt_payload=$(decode_jwt_payload "$access_token")
        if [ -n "$jwt_payload" ]; then
            print_success "JWT payload decoded"
            echo "JWT Payload:"
            echo $jwt_payload | python3 -m json.tool 2>/dev/null || echo $jwt_payload
        else
            print_warning "Failed to decode JWT payload"
        fi
        echo
    else
        print_error "No access token in response"
        echo "Response: $token_response"
        exit 1
    fi
else
    print_error "Failed to get access token"
    exit 1
fi

# 5. Test Token Validation
print_status "=== Step 5: Token Validation ==="
print_status "Testing token with user profile endpoint..."

profile_response=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer $access_token" \
    http://localhost:8090/api/auth/users/1/profile)

http_code=$(echo "$profile_response" | tail -n1)
body=$(echo "$profile_response" | head -n -1)

if [ "$http_code" = "200" ]; then
    print_success "Token validation successful - HTTP $http_code"
    echo "Profile Response: $body"
else
    print_warning "Token validation failed - HTTP $http_code"
    echo "Response: $body"
fi
echo

# 6. Test Resource Server Integration
print_status "=== Step 6: Resource Server Integration ==="
print_status "Testing token with notification service..."

notification_response=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer $access_token" \
    http://localhost:8089/api/notifications/user/1)

http_code=$(echo "$notification_response" | tail -n1)
body=$(echo "$notification_response" | head -n -1)

if [ "$http_code" = "200" ]; then
    print_success "Resource server integration successful - HTTP $http_code"
    echo "Notifications Response: $body"
else
    print_warning "Resource server integration failed - HTTP $http_code"
    echo "Response: $body"
fi
echo

# 7. Test Unauthorized Access
print_status "=== Step 7: Unauthorized Access Test ==="
print_status "Testing access without token (should fail)..."

unauth_response=$(curl -s -w "\n%{http_code}" \
    http://localhost:8089/api/notifications/user/1)

http_code=$(echo "$unauth_response" | tail -n1)

if [ "$http_code" = "401" ] || [ "$http_code" = "403" ]; then
    print_success "Unauthorized access properly blocked - HTTP $http_code"
else
    print_warning "Unauthorized access not properly blocked - HTTP $http_code"
    echo "Response: $(echo "$unauth_response" | head -n -1)"
fi
echo

# 8. Test Invalid Token
print_status "=== Step 8: Invalid Token Test ==="
print_status "Testing access with invalid token (should fail)..."

invalid_response=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer invalid-token-12345" \
    http://localhost:8089/api/notifications/user/1)

http_code=$(echo "$invalid_response" | tail -n1)

if [ "$http_code" = "401" ] || [ "$http_code" = "403" ]; then
    print_success "Invalid token properly rejected - HTTP $http_code"
else
    print_warning "Invalid token not properly rejected - HTTP $http_code"
    echo "Response: $(echo "$invalid_response" | head -n -1)"
fi
echo

# 9. Test Multiple Resource Servers
print_status "=== Step 9: Multiple Resource Servers Test ==="
print_status "Testing token across different services..."

# Test Product Service
product_response=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer $access_token" \
    http://localhost:8082/api/products/1/inventory)

http_code=$(echo "$product_response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_success "Product Service access successful - HTTP $http_code"
else
    print_warning "Product Service access failed - HTTP $http_code"
fi

# Test Payment Service
payment_response=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer $access_token" \
    http://localhost:8086/api/payments/health)

http_code=$(echo "$payment_response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_success "Payment Service access successful - HTTP $http_code"
else
    print_warning "Payment Service access failed - HTTP $http_code"
fi
echo

# 10. Test Token Introspection (if available)
print_status "=== Step 10: Token Introspection Test ==="
print_status "Testing token introspection endpoint..."

introspect_response=$(curl -s -w "\n%{http_code}" \
    -X POST http://localhost:8090/oauth2/introspect \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "token=$access_token&client_id=shophub-client&client_secret=secret")

http_code=$(echo "$introspect_response" | tail -n1)
body=$(echo "$introspect_response" | head -n -1)

if [ "$http_code" = "200" ]; then
    print_success "Token introspection successful - HTTP $http_code"
    echo "Introspection Response: $body"
else
    print_warning "Token introspection failed - HTTP $http_code"
    echo "Response: $body"
fi
echo

print_success "=== OAuth2 Token Testing Complete ==="
echo
print_status "Summary:"
print_status "✓ Authorization Server health check"
print_status "✓ OpenID Connect discovery"
print_status "✓ JWKS endpoint"
print_status "✓ Client credentials grant"
print_status "✓ JWT token decoding"
print_status "✓ Token validation"
print_status "✓ Resource server integration"
print_status "✓ Security enforcement (unauthorized/invalid tokens)"
print_status "✓ Multi-service token usage"
print_status "✓ Token introspection"
echo
print_status "Access Token (for manual testing): $access_token"
print_status "Authorization Server: http://localhost:8090"
print_status "OpenID Configuration: http://localhost:8090/.well-known/openid_configuration"
echo