#!/bin/bash
# =========================================================================
# CI Health Check Script
# =========================================================================
# Robust health checking for all microservices + NGINX gateway
# Features:
#   - Retry logic (up to 30 attempts = 60 seconds)
#   - Individual service verification
#   - NGINX gateway verification
#   - Proper exit codes
#   - Colored output for readability
# =========================================================================

set -e

# Configuration
MAX_RETRIES=30
RETRY_INTERVAL=2
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"

# Service definitions
declare -A SERVICES=(
    ["user-service"]="8082"
    ["application-service"]="8083"
    ["evaluation-service"]="8084"
    ["notification-service"]="8085"
    ["dashboard-service"]="8086"
    ["guardian-service"]="8087"
)

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if running in CI
if [ -n "$CI" ]; then
    # Disable colors in CI
    RED=''
    GREEN=''
    YELLOW=''
    BLUE=''
    NC=''
fi

echo ""
echo "=========================================="
echo "  CI Health Check - Microservices"
echo "=========================================="
echo ""

# Function to check service health
check_service_health() {
    local service_name=$1
    local port=$2
    local url="http://localhost:${port}/health"

    echo -n "${BLUE}Checking ${service_name}...${NC} "

    for i in $(seq 1 $MAX_RETRIES); do
        if curl -f -s -m 2 "$url" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ OK${NC} (attempt $i/${MAX_RETRIES})"
            return 0
        fi

        if [ $i -eq $MAX_RETRIES ]; then
            echo -e "${RED}❌ FAILED${NC} (after ${MAX_RETRIES} attempts)"
            echo "  URL: $url"
            echo "  Trying to connect..."
            curl -v "$url" 2>&1 | head -20 || true
            return 1
        fi

        sleep $RETRY_INTERVAL
    done

    return 1
}

# Function to check NGINX gateway
check_nginx_gateway() {
    local url="${GATEWAY_URL}/health"

    echo -n "${BLUE}Checking NGINX Gateway...${NC} "

    for i in $(seq 1 $MAX_RETRIES); do
        if curl -f -s -m 2 "$url" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ OK${NC} (attempt $i/${MAX_RETRIES})"
            return 0
        fi

        if [ $i -eq $MAX_RETRIES ]; then
            echo -e "${RED}❌ FAILED${NC} (after ${MAX_RETRIES} attempts)"
            echo "  Gateway URL: $url"
            echo "  Trying to connect..."
            curl -v "$url" 2>&1 | head -20 || true
            return 1
        fi

        sleep $RETRY_INTERVAL
    done

    return 1
}

# Function to check gateway routing
check_gateway_routing() {
    local service_name=$1
    local endpoint=$2
    local url="${GATEWAY_URL}${endpoint}"

    echo -n "${BLUE}Checking gateway routing to ${service_name}...${NC} "

    if curl -f -s -m 3 "$url" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ OK${NC}"
        return 0
    else
        echo -e "${YELLOW}⚠️  WARNING${NC} (routing may not be configured)"
        return 0  # Don't fail, just warn
    fi
}

# Main health check flow
ALL_HEALTHY=true

echo "1️⃣  Checking Individual Services"
echo "-----------------------------------"
for service in "${!SERVICES[@]}"; do
    if ! check_service_health "$service" "${SERVICES[$service]}"; then
        ALL_HEALTHY=false
    fi
done

echo ""
echo "2️⃣  Checking NGINX Gateway"
echo "-----------------------------------"
if ! check_nginx_gateway; then
    ALL_HEALTHY=false
fi

echo ""
echo "3️⃣  Checking Gateway Routing"
echo "-----------------------------------"
check_gateway_routing "user-service" "/api/auth/public-key"
check_gateway_routing "application-service" "/api/applications"
check_gateway_routing "evaluation-service" "/api/evaluations"
check_gateway_routing "notification-service" "/api/notifications"
check_gateway_routing "dashboard-service" "/api/dashboard/stats"
check_gateway_routing "guardian-service" "/api/guardians"

echo ""
echo "=========================================="

if [ "$ALL_HEALTHY" = true ]; then
    echo -e "${GREEN}✅ All services are healthy!${NC}"
    echo "=========================================="
    echo ""
    exit 0
else
    echo -e "${RED}❌ Some services are not healthy${NC}"
    echo "=========================================="
    echo ""
    echo "Debugging Information:"
    echo "----------------------"
    echo "Running processes:"
    ps aux | grep -E "(node|nginx)" | grep -v grep || echo "No Node.js or NGINX processes found"
    echo ""
    echo "Port usage:"
    lsof -i:8080,8082,8083,8084,8085,8086,8087 2>/dev/null || netstat -an | grep -E "(8080|8082|8083|8084|8085|8086|8087)" || echo "No services listening"
    echo ""
    exit 1
fi
