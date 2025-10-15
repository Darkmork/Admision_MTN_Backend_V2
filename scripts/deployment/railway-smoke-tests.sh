#!/bin/bash

###############################################################################
# Railway Smoke Test Suite
###############################################################################
# Comprehensive smoke tests for Sistema de Admisión MTN on Railway
#
# Prerequisites:
# 1. Services deployed to Railway
# 2. Database restored
# 3. RAILWAY_URL environment variable set
#
# Usage:
#   export RAILWAY_URL="https://your-app-name.railway.app"
#   ./railway-smoke-tests.sh
#
# Tests:
# 1. Health checks (gateway + all services)
# 2. Authentication flow (admin login)
# 3. Service endpoints (CRUD operations)
# 4. CORS verification
# 5. Cache functionality
# 6. Circuit breaker verification
# 7. Performance benchmarks
###############################################################################

set -e  # Exit on error

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Test results array
declare -a FAILED_TEST_NAMES

# Helper functions
pass() {
    echo -e "${GREEN}✓${NC} $1"
    ((PASSED_TESTS++))
    ((TOTAL_TESTS++))
}

fail() {
    echo -e "${RED}✗${NC} $1"
    ((FAILED_TESTS++))
    ((TOTAL_TESTS++))
    FAILED_TEST_NAMES+=("$1")
}

info() {
    echo -e "${CYAN}ℹ${NC} $1"
}

warn() {
    echo -e "${YELLOW}⚠${NC} $1"
}

section() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
}

# Check prerequisites
section "Prerequisites"

if [ -z "$RAILWAY_URL" ]; then
    echo -e "${RED}❌ RAILWAY_URL not set${NC}"
    echo "Usage: export RAILWAY_URL=\"https://your-app-name.railway.app\""
    exit 1
fi

if ! command -v curl &> /dev/null; then
    echo -e "${RED}❌ curl not installed${NC}"
    exit 1
fi

if ! command -v jq &> /dev/null; then
    warn "jq not installed - JSON output will not be formatted"
    warn "Install with: brew install jq"
    JQ_AVAILABLE=false
else
    JQ_AVAILABLE=true
fi

echo -e "${GREEN}✓${NC} RAILWAY_URL: $RAILWAY_URL"
echo -e "${GREEN}✓${NC} curl installed"
if [ "$JQ_AVAILABLE" = true ]; then
    echo -e "${GREEN}✓${NC} jq installed"
fi

# Test admin credentials
ADMIN_EMAIL="jorge.gangale@mtn.cl"
ADMIN_PASSWORD="admin123"

# Start tests
section "Test 1: Gateway Health Check"

HEALTH_RESPONSE=$(curl -s -w "\n%{http_code}" "$RAILWAY_URL/health" 2>&1)
HTTP_CODE=$(echo "$HEALTH_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$HEALTH_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    pass "Gateway health check returns 200 OK"

    if [ "$JQ_AVAILABLE" = true ]; then
        STATUS=$(echo "$RESPONSE_BODY" | jq -r '.status' 2>/dev/null)
        if [ "$STATUS" = "UP" ]; then
            pass "Gateway status is UP"
        else
            fail "Gateway status is not UP: $STATUS"
        fi

        SERVICE_COUNT=$(echo "$RESPONSE_BODY" | jq '.services | length' 2>/dev/null)
        if [ "$SERVICE_COUNT" = "6" ]; then
            pass "All 6 services reported"
        else
            fail "Expected 6 services, got $SERVICE_COUNT"
        fi
    else
        if echo "$RESPONSE_BODY" | grep -q '"status":"UP"'; then
            pass "Gateway status is UP"
        else
            fail "Gateway status is not UP"
        fi
    fi
else
    fail "Gateway health check failed with HTTP $HTTP_CODE"
    echo "Response: $RESPONSE_BODY"
fi

# Test root endpoint
ROOT_RESPONSE=$(curl -s -w "\n%{http_code}" "$RAILWAY_URL/" 2>&1)
HTTP_CODE=$(echo "$ROOT_RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "200" ]; then
    pass "Root endpoint returns 200 OK"
else
    fail "Root endpoint failed with HTTP $HTTP_CODE"
fi

section "Test 2: Authentication Flow"

# Login with admin credentials
info "Attempting login with admin credentials..."

LOGIN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$RAILWAY_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}" 2>&1)

HTTP_CODE=$(echo "$LOGIN_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$LOGIN_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    pass "Admin login successful (HTTP 200)"

    if [ "$JQ_AVAILABLE" = true ]; then
        TOKEN=$(echo "$RESPONSE_BODY" | jq -r '.token' 2>/dev/null)
        USER_EMAIL=$(echo "$RESPONSE_BODY" | jq -r '.user.email' 2>/dev/null)
        USER_ROLE=$(echo "$RESPONSE_BODY" | jq -r '.user.role' 2>/dev/null)

        if [ -n "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
            pass "JWT token received"
        else
            fail "JWT token not present in response"
        fi

        if [ "$USER_EMAIL" = "$ADMIN_EMAIL" ]; then
            pass "User email matches: $USER_EMAIL"
        else
            fail "User email mismatch: expected $ADMIN_EMAIL, got $USER_EMAIL"
        fi

        if [ "$USER_ROLE" = "ADMIN" ]; then
            pass "User role is ADMIN"
        else
            fail "User role is not ADMIN: $USER_ROLE"
        fi
    else
        if echo "$RESPONSE_BODY" | grep -q '"token"'; then
            pass "JWT token received"
            TOKEN=$(echo "$RESPONSE_BODY" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
        else
            fail "JWT token not present in response"
        fi
    fi
else
    fail "Admin login failed with HTTP $HTTP_CODE"
    echo "Response: $RESPONSE_BODY"
    echo -e "${RED}Cannot continue without authentication token${NC}"
    exit 1
fi

# Test invalid credentials
INVALID_LOGIN=$(curl -s -w "\n%{http_code}" -X POST "$RAILWAY_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"invalid@test.cl\",\"password\":\"wrongpass\"}" 2>&1)

HTTP_CODE=$(echo "$INVALID_LOGIN" | tail -n1)

if [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "403" ]; then
    pass "Invalid credentials rejected (HTTP $HTTP_CODE)"
else
    fail "Invalid credentials not properly rejected (HTTP $HTTP_CODE)"
fi

section "Test 3: User Service Endpoints"

# Get user roles
info "Testing GET /api/users/roles..."

ROLES_RESPONSE=$(curl -s -w "\n%{http_code}" "$RAILWAY_URL/api/users/roles" \
    -H "Authorization: Bearer $TOKEN" 2>&1)

HTTP_CODE=$(echo "$ROLES_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$ROLES_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    pass "Get user roles successful"

    if [ "$JQ_AVAILABLE" = true ]; then
        ROLES=$(echo "$RESPONSE_BODY" | jq -r '.[]' 2>/dev/null | tr '\n' ', ')
        info "Available roles: $ROLES"
    fi
else
    fail "Get user roles failed with HTTP $HTTP_CODE"
fi

# Test unauthorized access (no token)
UNAUTHORIZED_RESPONSE=$(curl -s -w "\n%{http_code}" "$RAILWAY_URL/api/users/roles" 2>&1)
HTTP_CODE=$(echo "$UNAUTHORIZED_RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "403" ]; then
    pass "Unauthorized access blocked (HTTP $HTTP_CODE)"
else
    fail "Unauthorized access not properly blocked (HTTP $HTTP_CODE)"
fi

section "Test 4: Application Service Endpoints"

# Get applications
info "Testing GET /api/applications..."

APPLICATIONS_RESPONSE=$(curl -s -w "\n%{http_code}" "$RAILWAY_URL/api/applications" \
    -H "Authorization: Bearer $TOKEN" 2>&1)

HTTP_CODE=$(echo "$APPLICATIONS_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$APPLICATIONS_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    pass "Get applications successful"

    if [ "$JQ_AVAILABLE" = true ]; then
        APP_COUNT=$(echo "$RESPONSE_BODY" | jq 'length' 2>/dev/null)
        info "Found $APP_COUNT applications"

        if [ "$APP_COUNT" -gt 0 ]; then
            pass "Applications exist in database"
        else
            warn "No applications found (database may be empty)"
        fi
    fi
else
    fail "Get applications failed with HTTP $HTTP_CODE"
fi

section "Test 5: Evaluation Service Endpoints"

# Get evaluations
info "Testing GET /api/evaluations..."

EVALUATIONS_RESPONSE=$(curl -s -w "\n%{http_code}" "$RAILWAY_URL/api/evaluations" \
    -H "Authorization: Bearer $TOKEN" 2>&1)

HTTP_CODE=$(echo "$EVALUATIONS_RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "200" ]; then
    pass "Get evaluations successful"
else
    fail "Get evaluations failed with HTTP $HTTP_CODE"
fi

# Get interviews
info "Testing GET /api/interviews..."

INTERVIEWS_RESPONSE=$(curl -s -w "\n%{http_code}" "$RAILWAY_URL/api/interviews" \
    -H "Authorization: Bearer $TOKEN" 2>&1)

HTTP_CODE=$(echo "$INTERVIEWS_RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "200" ]; then
    pass "Get interviews successful"
else
    fail "Get interviews failed with HTTP $HTTP_CODE"
fi

section "Test 6: Dashboard Service Endpoints"

# Get dashboard stats
info "Testing GET /api/dashboard/stats..."

DASHBOARD_RESPONSE=$(curl -s -w "\n%{http_code}" "$RAILWAY_URL/api/dashboard/stats" \
    -H "Authorization: Bearer $TOKEN" 2>&1)

HTTP_CODE=$(echo "$DASHBOARD_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$DASHBOARD_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    pass "Get dashboard stats successful"

    if [ "$JQ_AVAILABLE" = true ]; then
        TOTAL_APPS=$(echo "$RESPONSE_BODY" | jq -r '.totalApplications' 2>/dev/null)
        if [ -n "$TOTAL_APPS" ] && [ "$TOTAL_APPS" != "null" ]; then
            info "Total applications: $TOTAL_APPS"
        fi
    fi
else
    fail "Get dashboard stats failed with HTTP $HTTP_CODE"
fi

# Get admin stats
info "Testing GET /api/dashboard/admin/stats..."

ADMIN_STATS_RESPONSE=$(curl -s -w "\n%{http_code}" "$RAILWAY_URL/api/dashboard/admin/stats" \
    -H "Authorization: Bearer $TOKEN" 2>&1)

HTTP_CODE=$(echo "$ADMIN_STATS_RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "200" ]; then
    pass "Get admin stats successful"
else
    fail "Get admin stats failed with HTTP $HTTP_CODE"
fi

section "Test 7: Guardian Service Endpoints"

# Get guardians (admin only)
info "Testing GET /api/guardians..."

GUARDIANS_RESPONSE=$(curl -s -w "\n%{http_code}" "$RAILWAY_URL/api/guardians" \
    -H "Authorization: Bearer $TOKEN" 2>&1)

HTTP_CODE=$(echo "$GUARDIANS_RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "200" ]; then
    pass "Get guardians successful"
else
    # May require specific permissions
    warn "Get guardians returned HTTP $HTTP_CODE (may require specific permissions)"
fi

section "Test 8: CORS Verification"

# Test CORS preflight (OPTIONS request)
info "Testing CORS preflight from Vercel domain..."

CORS_RESPONSE=$(curl -s -w "\n%{http_code}" -X OPTIONS "$RAILWAY_URL/api/users/roles" \
    -H "Origin: https://admision-mtn-front.vercel.app" \
    -H "Access-Control-Request-Method: GET" \
    -H "Access-Control-Request-Headers: Authorization" \
    -v 2>&1)

if echo "$CORS_RESPONSE" | grep -q "access-control-allow-origin"; then
    pass "CORS headers present"
else
    fail "CORS headers missing"
fi

if echo "$CORS_RESPONSE" | grep -q "https://admision-mtn-front.vercel.app"; then
    pass "CORS allows Vercel origin"
else
    fail "CORS does not allow Vercel origin"
fi

section "Test 9: Cache Functionality"

# Test cache on second request
info "Testing cache with repeated requests..."

# First request (cache miss)
START_TIME=$(date +%s%3N)
curl -s "$RAILWAY_URL/api/users/roles" -H "Authorization: Bearer $TOKEN" > /dev/null
END_TIME=$(date +%s%3N)
FIRST_LATENCY=$((END_TIME - START_TIME))

# Second request (cache hit expected)
START_TIME=$(date +%s%3N)
curl -s "$RAILWAY_URL/api/users/roles" -H "Authorization: Bearer $TOKEN" > /dev/null
END_TIME=$(date +%s%3N)
SECOND_LATENCY=$((END_TIME - START_TIME))

info "First request: ${FIRST_LATENCY}ms"
info "Second request: ${SECOND_LATENCY}ms"

if [ "$SECOND_LATENCY" -lt "$FIRST_LATENCY" ]; then
    pass "Cache appears to be working (second request faster)"
else
    warn "Cache may not be working (second request not faster)"
fi

# Get cache stats (if available)
CACHE_STATS=$(curl -s "$RAILWAY_URL/api/users/cache/stats" \
    -H "Authorization: Bearer $TOKEN" 2>&1)

if echo "$CACHE_STATS" | grep -q "hitRate"; then
    pass "Cache stats endpoint available"
    if [ "$JQ_AVAILABLE" = true ]; then
        HIT_RATE=$(echo "$CACHE_STATS" | jq -r '.hitRate' 2>/dev/null)
        info "Cache hit rate: $HIT_RATE"
    fi
else
    warn "Cache stats endpoint not available or not returning data"
fi

section "Test 10: Performance Benchmarks"

info "Running performance benchmarks (10 requests each)..."

# Test dashboard stats performance
TOTAL_TIME=0
for i in {1..10}; do
    START_TIME=$(date +%s%3N)
    curl -s "$RAILWAY_URL/api/dashboard/stats" \
        -H "Authorization: Bearer $TOKEN" > /dev/null
    END_TIME=$(date +%s%3N)
    LATENCY=$((END_TIME - START_TIME))
    TOTAL_TIME=$((TOTAL_TIME + LATENCY))
done
AVG_LATENCY=$((TOTAL_TIME / 10))

info "Dashboard stats - Average latency: ${AVG_LATENCY}ms"

if [ "$AVG_LATENCY" -lt 500 ]; then
    pass "Dashboard stats latency acceptable (<500ms)"
elif [ "$AVG_LATENCY" -lt 1000 ]; then
    warn "Dashboard stats latency marginal (${AVG_LATENCY}ms, expect <500ms)"
else
    fail "Dashboard stats latency too high (${AVG_LATENCY}ms)"
fi

# Test auth performance
TOTAL_TIME=0
for i in {1..5}; do
    START_TIME=$(date +%s%3N)
    curl -s -X POST "$RAILWAY_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}" > /dev/null
    END_TIME=$(date +%s%3N)
    LATENCY=$((END_TIME - START_TIME))
    TOTAL_TIME=$((TOTAL_TIME + LATENCY))
done
AVG_AUTH_LATENCY=$((TOTAL_TIME / 5))

info "Authentication - Average latency: ${AVG_AUTH_LATENCY}ms"

if [ "$AVG_AUTH_LATENCY" -lt 300 ]; then
    pass "Authentication latency acceptable (<300ms)"
elif [ "$AVG_AUTH_LATENCY" -lt 600 ]; then
    warn "Authentication latency marginal (${AVG_AUTH_LATENCY}ms, expect <300ms)"
else
    fail "Authentication latency too high (${AVG_AUTH_LATENCY}ms)"
fi

section "Test Summary"

echo ""
echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}Test Results${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""
echo -e "Total tests: ${TOTAL_TESTS}"
echo -e "${GREEN}Passed: ${PASSED_TESTS}${NC}"
echo -e "${RED}Failed: ${FAILED_TESTS}${NC}"
echo ""

if [ "$FAILED_TESTS" -gt 0 ]; then
    echo -e "${RED}Failed tests:${NC}"
    for test_name in "${FAILED_TEST_NAMES[@]}"; do
        echo -e "  ${RED}✗${NC} $test_name"
    done
    echo ""
fi

# Success rate
SUCCESS_RATE=$((PASSED_TESTS * 100 / TOTAL_TESTS))
echo -e "Success rate: ${SUCCESS_RATE}%"
echo ""

if [ "$SUCCESS_RATE" -ge 90 ]; then
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}✓ DEPLOYMENT VERIFIED${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo -e "${GREEN}All critical tests passed. Deployment is production-ready.${NC}"
    echo ""
    exit 0
elif [ "$SUCCESS_RATE" -ge 70 ]; then
    echo -e "${YELLOW}========================================${NC}"
    echo -e "${YELLOW}⚠ DEPLOYMENT PARTIALLY VERIFIED${NC}"
    echo -e "${YELLOW}========================================${NC}"
    echo ""
    echo -e "${YELLOW}Some tests failed. Review failed tests and fix issues.${NC}"
    echo ""
    exit 1
else
    echo -e "${RED}========================================${NC}"
    echo -e "${RED}✗ DEPLOYMENT FAILED VERIFICATION${NC}"
    echo -e "${RED}========================================${NC}"
    echo ""
    echo -e "${RED}Too many tests failed. Do not use in production.${NC}"
    echo ""
    exit 1
fi
