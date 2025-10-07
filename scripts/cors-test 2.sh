#!/bin/bash

# CORS Test Script for Both Development Ports
# Tests CORS configuration for both port 5173 and 5174

echo "=== CORS Test Script for Sistema de Admisión MTN ==="
echo "Testing CORS configuration for both development ports..."
echo

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test function
test_cors() {
    local origin=$1
    local endpoint=$2
    local name=$3

    echo -e "${YELLOW}Testing $name with origin: $origin${NC}"

    # Test OPTIONS preflight request
    response=$(curl -s -i -H "Origin: $origin" \
        -H "Access-Control-Request-Method: POST" \
        -H "Access-Control-Request-Headers: Content-Type,Authorization" \
        -X OPTIONS "http://localhost:8080$endpoint")

    if echo "$response" | grep -q "Access-Control-Allow-Origin: $origin"; then
        echo -e "${GREEN}✓ CORS preflight successful for $origin${NC}"
    else
        echo -e "${RED}✗ CORS preflight failed for $origin${NC}"
        echo "Response headers:"
        echo "$response" | head -10
    fi

    # Test actual GET request
    get_response=$(curl -s -i -H "Origin: $origin" "http://localhost:8080$endpoint")

    if echo "$get_response" | grep -q "Access-Control-Allow-Origin: $origin"; then
        echo -e "${GREEN}✓ CORS GET request successful for $origin${NC}"
    else
        echo -e "${RED}✗ CORS GET request failed for $origin${NC}"
    fi

    echo
}

# Test gateway health endpoint
echo "=== Testing Gateway Health Endpoint ==="
test_cors "http://localhost:5173" "/health" "Health Endpoint (5173)"
test_cors "http://localhost:5174" "/health" "Health Endpoint (5174)"
test_cors "http://127.0.0.1:5173" "/health" "Health Endpoint (127.0.0.1:5173)"
test_cors "http://127.0.0.1:5174" "/health" "Health Endpoint (127.0.0.1:5174)"

# Test API endpoints
echo "=== Testing API Endpoints ==="
test_cors "http://localhost:5173" "/api/auth/login" "Auth Endpoint (5173)"
test_cors "http://localhost:5174" "/api/auth/login" "Auth Endpoint (5174)"

# Test invalid origin (should fail)
echo "=== Testing Invalid Origin (Should Fail) ==="
invalid_response=$(curl -s -i -H "Origin: http://localhost:3000" \
    -H "Access-Control-Request-Method: POST" \
    -X OPTIONS "http://localhost:8080/api/auth/login")

if echo "$invalid_response" | grep -q "Access-Control-Allow-Origin: http://localhost:3000"; then
    echo -e "${RED}✗ SECURITY ISSUE: Invalid origin was allowed${NC}"
else
    echo -e "${GREEN}✓ Security check passed: Invalid origin rejected${NC}"
fi

echo
echo "=== Gateway Status Check ==="
gateway_status=$(curl -s "http://localhost:8080/gateway/status")
echo "Gateway Status: $gateway_status"

echo
echo "=== CORS Test Complete ==="
echo "If all tests show ✓, CORS is properly configured for both development ports."
echo "Frontend should work on both http://localhost:5173 and http://localhost:5174"