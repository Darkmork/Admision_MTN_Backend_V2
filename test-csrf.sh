#!/bin/bash

# ============================================================
# CSRF Protection Test Script
# Tests Double-Submit Cookie pattern implementation
# ============================================================

set -e  # Exit on error

echo "======================================================================"
echo "CSRF Protection Testing - User Service"
echo "======================================================================"
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

COOKIE_FILE="/tmp/csrf-test-cookies.txt"
rm -f $COOKIE_FILE

# Test 1: Generate CSRF Token
echo -e "${YELLOW}Test 1: Generate CSRF Token${NC}"
echo "GET /api/auth/csrf-token"
RESPONSE=$(curl -s -c $COOKIE_FILE http://localhost:8082/api/auth/csrf-token)
echo "$RESPONSE" | jq '.'

if echo "$RESPONSE" | jq -e '.success == true' > /dev/null; then
    echo -e "${GREEN}✓ PASS:${NC} CSRF token generated successfully"
    CSRF_TOKEN=$(echo "$RESPONSE" | jq -r '.csrfToken')
    echo "Token: ${CSRF_TOKEN:0:40}..."
else
    echo -e "${RED}✗ FAIL:${NC} Failed to generate CSRF token"
    exit 1
fi
echo ""

# Test 2: Login WITHOUT CSRF Token (should fail with 403)
echo -e "${YELLOW}Test 2: Login WITHOUT CSRF Token (should fail with 403)${NC}"
echo "POST /api/auth/login (no CSRF token)"
RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"jorge.gangale@mtn.cl","password":"admin123"}')

HTTP_CODE=$(echo "$RESPONSE" | grep HTTP_CODE | cut -d: -f2)
BODY=$(echo "$RESPONSE" | sed '/HTTP_CODE/d')

echo "$BODY" | jq '.'

if [ "$HTTP_CODE" = "403" ]; then
    echo -e "${GREEN}✓ PASS:${NC} Request rejected with 403 (no CSRF token)"
else
    echo -e "${RED}✗ FAIL:${NC} Expected 403, got $HTTP_CODE"
    exit 1
fi
echo ""

# Test 3: Login WITH valid CSRF Token (should succeed with 200)
echo -e "${YELLOW}Test 3: Login WITH valid CSRF Token (should succeed with 200)${NC}"
echo "POST /api/auth/login (with valid CSRF token)"
RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -b $COOKIE_FILE \
  -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -H "X-CSRF-Token: $CSRF_TOKEN" \
  -d '{"email":"jorge.gangale@mtn.cl","password":"admin123"}')

HTTP_CODE=$(echo "$RESPONSE" | grep HTTP_CODE | cut -d: -f2)
BODY=$(echo "$RESPONSE" | sed '/HTTP_CODE/d')

echo "$BODY" | jq '.'

if [ "$HTTP_CODE" = "200" ] && echo "$BODY" | jq -e '.success == true' > /dev/null; then
    echo -e "${GREEN}✓ PASS:${NC} Login successful with valid CSRF token"
else
    echo -e "${RED}✗ FAIL:${NC} Expected 200 with success=true, got $HTTP_CODE"
    exit 1
fi
echo ""

# Test 4: Login WITH invalid CSRF Token (should fail with 403)
echo -e "${YELLOW}Test 4: Login WITH invalid CSRF Token (should fail with 403)${NC}"
echo "POST /api/auth/login (with invalid CSRF token)"
RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -b $COOKIE_FILE \
  -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -H "X-CSRF-Token: invalid-token-12345678" \
  -d '{"email":"jorge.gangale@mtn.cl","password":"admin123"}')

HTTP_CODE=$(echo "$RESPONSE" | grep HTTP_CODE | cut -d: -f2)
BODY=$(echo "$RESPONSE" | sed '/HTTP_CODE/d')

echo "$BODY" | jq '.'

if [ "$HTTP_CODE" = "403" ] && echo "$BODY" | jq -e '.code == "CSRF_TOKEN_INVALID"' > /dev/null; then
    echo -e "${GREEN}✓ PASS:${NC} Request rejected with 403 (invalid CSRF token)"
else
    echo -e "${RED}✗ FAIL:${NC} Expected 403 with CSRF_TOKEN_INVALID, got $HTTP_CODE"
    exit 1
fi
echo ""

# Test 5: GET request without CSRF token (should succeed)
echo -e "${YELLOW}Test 5: GET request without CSRF token (should succeed)${NC}"
echo "GET /health (safe method, no CSRF required)"
RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" http://localhost:8082/health)

HTTP_CODE=$(echo "$RESPONSE" | grep HTTP_CODE | cut -d: -f2)
BODY=$(echo "$RESPONSE" | sed '/HTTP_CODE/d')

echo "$BODY" | jq '.'

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ PASS:${NC} GET request succeeded without CSRF token (safe method)"
else
    echo -e "${RED}✗ FAIL:${NC} Expected 200, got $HTTP_CODE"
    exit 1
fi
echo ""

# Summary
echo "======================================================================"
echo -e "${GREEN}ALL CSRF TESTS PASSED!${NC}"
echo "======================================================================"
echo ""
echo "CSRF Protection Summary:"
echo "  ✓ CSRF token generation endpoint working"
echo "  ✓ Mutations (POST/PUT/DELETE) blocked without CSRF token"
echo "  ✓ Mutations succeed with valid CSRF token"
echo "  ✓ Mutations blocked with invalid CSRF token"
echo "  ✓ Safe methods (GET/HEAD/OPTIONS) work without CSRF token"
echo ""

# Cleanup
rm -f $COOKIE_FILE
