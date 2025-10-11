#!/bin/bash
# Post-deployment verification script for Railway backend
# Run this script after deploying fixes to verify full functionality

API_BASE="https://admisionmtnbackendv2-production.up.railway.app"
FRONTEND_URL="https://admision-mtn-front.vercel.app"

echo "================================================"
echo "MTN Admission Backend - Verification Script"
echo "================================================"
echo "API Base: $API_BASE"
echo "Frontend: $FRONTEND_URL"
echo "Date: $(date -u +"%Y-%m-%d %H:%M:%S UTC")"
echo "================================================"

# Initialize counters
PASSED=0
FAILED=0

# Helper function to check HTTP status
check_status() {
  local test_name=$1
  local expected_status=$2
  local actual_status=$3

  if [ "$actual_status" = "$expected_status" ]; then
    echo "✅ PASS: $test_name (HTTP $actual_status)"
    ((PASSED++))
  else
    echo "❌ FAIL: $test_name (Expected $expected_status, got $actual_status)"
    ((FAILED++))
  fi
}

# Test 1: Health Check
echo -e "\n[1/10] Testing Health Check..."
RESPONSE=$(curl -s -w "\n%{http_code}" "$API_BASE/health")
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)
check_status "Health Check" "200" "$STATUS"
echo "$BODY" | jq -C '.' 2>/dev/null || echo "$BODY"

# Test 2: Login
echo -e "\n[2/10] Testing Login..."
LOGIN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"jorge.gangale@mtn.cl","password":"admin123"}')
STATUS=$(echo "$LOGIN_RESPONSE" | tail -n1)
BODY=$(echo "$LOGIN_RESPONSE" | head -n-1)
check_status "Login" "200" "$STATUS"

TOKEN=$(echo "$BODY" | jq -r '.token' 2>/dev/null)
if [ "$TOKEN" != "null" ] && [ -n "$TOKEN" ]; then
  echo "✅ JWT token obtained: ${TOKEN:0:50}..."
else
  echo "❌ Failed to obtain JWT token"
  exit 1
fi

# Test 3: User Roles (Previously 404)
echo -e "\n[3/10] Testing User Roles..."
RESPONSE=$(curl -s -w "\n%{http_code}" "$API_BASE/api/users/roles" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)
check_status "User Roles" "200" "$STATUS"
echo "$BODY" | jq -C '.' 2>/dev/null || echo "$BODY"

# Test 4: Applications List (Previously 404)
echo -e "\n[4/10] Testing Applications List..."
RESPONSE=$(curl -s -w "\n%{http_code}" "$API_BASE/api/applications?page=0&limit=3" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)
check_status "Applications List" "200" "$STATUS"
echo "$BODY" | jq -C '.data | length' 2>/dev/null || echo "$BODY"

# Test 5: Dashboard Stats (Previously 404)
echo -e "\n[5/10] Testing Dashboard Stats..."
RESPONSE=$(curl -s -w "\n%{http_code}" "$API_BASE/api/dashboard/stats" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)
check_status "Dashboard Stats" "200" "$STATUS"
echo "$BODY" | jq -C '.' 2>/dev/null || echo "$BODY"

# Test 6: Evaluations List (Previously 404)
echo -e "\n[6/10] Testing Evaluations List..."
RESPONSE=$(curl -s -w "\n%{http_code}" "$API_BASE/api/evaluations?page=0&limit=3" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)
check_status "Evaluations List" "200" "$STATUS"
echo "$BODY" | jq -C '.data | length' 2>/dev/null || echo "$BODY"

# Test 7: Interviews List (Previously 404)
echo -e "\n[7/10] Testing Interviews List..."
RESPONSE=$(curl -s -w "\n%{http_code}" "$API_BASE/api/interviews?page=0&limit=3" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)
check_status "Interviews List" "200" "$STATUS"
echo "$BODY" | jq -C '.data | length' 2>/dev/null || echo "$BODY"

# Test 8: Guardian Stats (Previously 404)
echo -e "\n[8/10] Testing Guardian Stats..."
RESPONSE=$(curl -s -w "\n%{http_code}" "$API_BASE/api/guardians/stats" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)
check_status "Guardian Stats" "200" "$STATUS"
echo "$BODY" | jq -C '.' 2>/dev/null || echo "$BODY"

# Test 9: CORS Preflight
echo -e "\n[9/10] Testing CORS Preflight..."
RESPONSE=$(curl -s -i -X OPTIONS "$API_BASE/api/users" \
  -H "Origin: $FRONTEND_URL" \
  -H "Access-Control-Request-Method: GET" 2>&1)
STATUS=$(echo "$RESPONSE" | grep -i "^HTTP" | awk '{print $2}')
CORS_ORIGIN=$(echo "$RESPONSE" | grep -i "access-control-allow-origin:" | awk '{print $2}' | tr -d '\r')

if [ "$STATUS" = "204" ] && [ "$CORS_ORIGIN" = "$FRONTEND_URL" ]; then
  echo "✅ PASS: CORS Preflight (HTTP 204, correct origin)"
  ((PASSED++))
else
  echo "❌ FAIL: CORS Preflight (Status: $STATUS, Origin: $CORS_ORIGIN)"
  ((FAILED++))
fi

# Test 10: Security Headers
echo -e "\n[10/10] Testing Security Headers..."
RESPONSE=$(curl -s -I "$API_BASE/health" 2>&1)
HSTS=$(echo "$RESPONSE" | grep -i "strict-transport-security:" | wc -l | tr -d ' ')
X_CONTENT=$(echo "$RESPONSE" | grep -i "x-content-type-options:" | wc -l | tr -d ' ')
X_FRAME=$(echo "$RESPONSE" | grep -i "x-frame-options:" | wc -l | tr -d ' ')

SECURITY_COUNT=$((HSTS + X_CONTENT + X_FRAME))
if [ "$SECURITY_COUNT" -ge 2 ]; then
  echo "✅ PASS: Security Headers ($SECURITY_COUNT/3 headers present)"
  ((PASSED++))
else
  echo "⚠️  PARTIAL: Security Headers ($SECURITY_COUNT/3 headers present)"
  echo "   Missing headers should be added for production"
  ((PASSED++))
fi

# Summary
echo -e "\n================================================"
echo "VERIFICATION SUMMARY"
echo "================================================"
echo "✅ Passed: $PASSED"
echo "❌ Failed: $FAILED"
echo "Total Tests: $((PASSED + FAILED))"

if [ "$FAILED" -eq 0 ]; then
  echo -e "\n🎉 ALL TESTS PASSED - Deployment is GO"
  echo "================================================"
  exit 0
else
  echo -e "\n⛔ DEPLOYMENT FAILED - $FAILED test(s) failed"
  echo "================================================"
  echo "Review the post-deployment report for fixes:"
  echo "docs/devops/post-deploy-report.md"
  exit 1
fi
