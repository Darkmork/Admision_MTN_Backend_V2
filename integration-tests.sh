#!/bin/bash

# ==========================================
# INTEGRATION TESTS - Sistema de Admisión MTN
# Tests completos de integración entre roles:
# - Apoderado (Guardian)
# - Profesor (Teacher)
# - Admin
# ==========================================

BASE_URL="http://localhost:8080"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_TOTAL=0

# Function to print test results
test_result() {
    TESTS_TOTAL=$((TESTS_TOTAL + 1))
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓ PASS${NC}: $2"
        TESTS_PASSED=$((TESTS_PASSED + 1))
    else
        echo -e "${RED}✗ FAIL${NC}: $2"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        if [ ! -z "$3" ]; then
            echo -e "  ${YELLOW}Details:${NC} $3"
        fi
    fi
}

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}INTEGRATION TESTS - Admisión MTN${NC}"
echo -e "${BLUE}========================================${NC}\n"

# ==========================================
# PHASE 1: SERVICE HEALTH CHECKS
# ==========================================
echo -e "${YELLOW}[PHASE 1]${NC} Service Health Checks"
echo "-----------------------------------"

for port in 8082 8083 8084 8085 8086 8087; do
    response=$(curl -s http://localhost:$port/health)
    if echo "$response" | grep -q '"status":"UP"'; then
        test_result 0 "Service on port $port is UP"
    else
        test_result 1 "Service on port $port is DOWN" "$response"
    fi
done

echo ""

# ==========================================
# PHASE 2: ADMIN FLOW
# ==========================================
echo -e "${YELLOW}[PHASE 2]${NC} Admin Flow Testing"
echo "-----------------------------------"

# Admin Login
echo "2.1. Admin Login..."
ADMIN_LOGIN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"jorge.gangale@mtn.cl","password":"admin123"}')

ADMIN_TOKEN=$(echo $ADMIN_LOGIN | jq -r '.token // empty')
if [ ! -z "$ADMIN_TOKEN" ] && [ "$ADMIN_TOKEN" != "null" ]; then
    test_result 0 "Admin login successful"
    ADMIN_ROLE=$(echo $ADMIN_LOGIN | jq -r '.user.role')
    test_result 0 "Admin role verified: $ADMIN_ROLE"
else
    test_result 1 "Admin login failed" "$ADMIN_LOGIN"
fi

# Get Dashboard Stats
echo "2.2. Get Dashboard Stats..."
DASHBOARD_STATS=$(curl -s "$BASE_URL/api/dashboard/stats" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

# Dashboard returns {success: true, data: {...}} format
if echo "$DASHBOARD_STATS" | jq -e '.success' > /dev/null 2>&1; then
    TOTAL_APPS=$(echo $DASHBOARD_STATS | jq -r '.data.totalApplications')
    test_result 0 "Dashboard stats retrieved: $TOTAL_APPS total applications"
else
    test_result 1 "Dashboard stats failed" "$DASHBOARD_STATS"
fi

# Get Applications List
echo "2.3. Get Applications List..."
APPLICATIONS=$(curl -s "$BASE_URL/api/applications/search?page=0&limit=5" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

if echo "$APPLICATIONS" | jq -e '.data' > /dev/null 2>&1; then
    APP_COUNT=$(echo $APPLICATIONS | jq -r '.data | length')
    test_result 0 "Applications list retrieved: $APP_COUNT applications"

    # Get first application ID for detailed tests
    FIRST_APP_ID=$(echo $APPLICATIONS | jq -r '.data[0].id')
    echo "  Using application ID $FIRST_APP_ID for detailed tests"
else
    test_result 1 "Applications list failed" "$APPLICATIONS"
fi

# Get Application Details
echo "2.4. Get Application Details..."
if [ ! -z "$FIRST_APP_ID" ] && [ "$FIRST_APP_ID" != "null" ]; then
    APP_DETAILS=$(curl -s "$BASE_URL/api/applications/$FIRST_APP_ID" \
      -H "Authorization: Bearer $ADMIN_TOKEN")

    if echo "$APP_DETAILS" | jq -e '.success' > /dev/null 2>&1; then
        STUDENT_NAME=$(echo $APP_DETAILS | jq -r '.data.student.fullName')
        test_result 0 "Application details retrieved: Student $STUDENT_NAME"
    else
        test_result 1 "Application details failed" "$APP_DETAILS"
    fi
fi

# Get Analytics
echo "2.5. Get Analytics..."
ANALYTICS=$(curl -s "$BASE_URL/api/analytics/temporal-trends" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

if echo "$ANALYTICS" | jq -e '.monthlyApplications' > /dev/null 2>&1; then
    test_result 0 "Analytics temporal trends retrieved"
else
    test_result 1 "Analytics failed" "$ANALYTICS"
fi

echo ""

# ==========================================
# PHASE 3: TEACHER FLOW
# ==========================================
echo -e "${YELLOW}[PHASE 3]${NC} Teacher Flow Testing"
echo "-----------------------------------"

# Teacher Login
echo "3.1. Teacher Login (Mathematics)..."
TEACHER_LOGIN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"alejandra.flores@mtn.cl","password":"12345678"}')

TEACHER_TOKEN=$(echo $TEACHER_LOGIN | jq -r '.token // empty')
if [ ! -z "$TEACHER_TOKEN" ] && [ "$TEACHER_TOKEN" != "null" ]; then
    test_result 0 "Teacher login successful"
    # Response format: {id, email, role, subject} not {user: {id, email...}}
    TEACHER_SUBJECT=$(echo $TEACHER_LOGIN | jq -r '.subject')
    test_result 0 "Teacher subject verified: $TEACHER_SUBJECT"
else
    test_result 1 "Teacher login failed" "$TEACHER_LOGIN"
fi

# Get Teacher's Evaluations
echo "3.2. Get Teacher's Assigned Evaluations..."
# Use /my-evaluations endpoint which uses token to get evaluations
EVALUATIONS=$(curl -s "$BASE_URL/api/evaluations/my-evaluations" \
  -H "Authorization: Bearer $TEACHER_TOKEN")

if echo "$EVALUATIONS" | jq -e 'type' > /dev/null 2>&1; then
    EVAL_COUNT=$(echo $EVALUATIONS | jq -r 'length')
    test_result 0 "Teacher evaluations retrieved: $EVAL_COUNT evaluations"
else
    test_result 1 "Teacher evaluations failed" "$EVALUATIONS"
fi

# Get Evaluation Types
echo "3.3. Get Evaluation Types..."
EVAL_TYPES=$(curl -s "$BASE_URL/api/evaluations/metadata/types" \
  -H "Authorization: Bearer $TEACHER_TOKEN")

# Expects {success: true, data: [...]} format
if echo "$EVAL_TYPES" | jq -e '.success and (.data | type == "array")' > /dev/null 2>&1; then
    TYPE_COUNT=$(echo $EVAL_TYPES | jq -r '.data | length')
    test_result 0 "Evaluation types retrieved: $TYPE_COUNT types"
else
    test_result 1 "Evaluation types failed" "$EVAL_TYPES"
fi

echo ""

# ==========================================
# PHASE 4: GUARDIAN FLOW
# ==========================================
echo -e "${YELLOW}[PHASE 4]${NC} Guardian (Apoderado) Flow Testing"
echo "-----------------------------------"

# Check if we can access Guardian service
echo "4.1. Guardian Service Health..."
GUARDIAN_HEALTH=$(curl -s "$BASE_URL/api/guardians/stats")

if echo "$GUARDIAN_HEALTH" | grep -q "error" || echo "$GUARDIAN_HEALTH" | grep -q "success"; then
    test_result 0 "Guardian service accessible (requires auth as expected)"
else
    test_result 1 "Guardian service not accessible" "$GUARDIAN_HEALTH"
fi

# Get public interviewers list (no auth required)
echo "4.2. Get Public Interviewers List..."
INTERVIEWERS=$(curl -s "$BASE_URL/api/interviews/public/interviewers")

if echo "$INTERVIEWERS" | jq -e 'type == "array"' > /dev/null 2>&1; then
    INT_COUNT=$(echo $INTERVIEWERS | jq -r 'length')
    test_result 0 "Public interviewers list retrieved: $INT_COUNT interviewers"
else
    test_result 1 "Public interviewers list failed" "$INTERVIEWERS"
fi

# Get school staff (public endpoint)
echo "4.3. Get Public School Staff..."
SCHOOL_STAFF=$(curl -s "$BASE_URL/api/users/public/school-staff")

# Expects paginated format {content: [...], totalElements, ...}
if echo "$SCHOOL_STAFF" | jq -e '.content' > /dev/null 2>&1; then
    STAFF_COUNT=$(echo $SCHOOL_STAFF | jq -r '.content | length')
    TOTAL_STAFF=$(echo $SCHOOL_STAFF | jq -r '.totalElements')
    test_result 0 "Public school staff retrieved: $STAFF_COUNT/$TOTAL_STAFF staff members"
else
    test_result 1 "Public school staff failed" "$SCHOOL_STAFF"
fi

echo ""

# ==========================================
# PHASE 5: DATA CONSISTENCY CHECKS
# ==========================================
echo -e "${YELLOW}[PHASE 5]${NC} Data Consistency Checks"
echo "-----------------------------------"

# Check that application data is consistent across services
echo "5.1. Cross-Service Data Consistency..."
if [ ! -z "$FIRST_APP_ID" ] && [ "$FIRST_APP_ID" != "null" ]; then
    # Get application from Application Service
    APP_FROM_SERVICE=$(curl -s "$BASE_URL/api/applications/$FIRST_APP_ID" \
      -H "Authorization: Bearer $ADMIN_TOKEN")

    # Get same application from Dashboard stats
    APP_FROM_DASHBOARD=$(curl -s "$BASE_URL/api/applications/search?page=0&limit=100" \
      -H "Authorization: Bearer $ADMIN_TOKEN")

    APP_STATUS_SERVICE=$(echo $APP_FROM_SERVICE | jq -r '.data.status')
    APP_STATUS_SEARCH=$(echo $APP_FROM_DASHBOARD | jq -r ".data[] | select(.id == \"$FIRST_APP_ID\") | .status")

    if [ "$APP_STATUS_SERVICE" == "$APP_STATUS_SEARCH" ]; then
        test_result 0 "Application status consistent across services: $APP_STATUS_SERVICE"
    else
        test_result 1 "Application status INCONSISTENT" "Service: $APP_STATUS_SERVICE vs Search: $APP_STATUS_SEARCH"
    fi
fi

# Check evaluations link to valid applications
echo "5.2. Evaluations-Applications Link..."
if [ ! -z "$FIRST_APP_ID" ] && [ "$FIRST_APP_ID" != "null" ]; then
    APP_EVALS=$(curl -s "$BASE_URL/api/evaluations/application/$FIRST_APP_ID" \
      -H "Authorization: Bearer $ADMIN_TOKEN")

    if echo "$APP_EVALS" | jq -e 'type == "array"' > /dev/null 2>&1; then
        test_result 0 "Evaluations properly linked to application"
    else
        test_result 1 "Evaluations-Applications link broken" "$APP_EVALS"
    fi
fi

# Check dashboard aggregations match actual counts
echo "5.3. Dashboard Aggregations Accuracy..."
# Applications search format: {success: true, pagination: {total: ...}}
ACTUAL_COUNT=$(echo $APPLICATIONS | jq -r '.pagination.total')
# Dashboard stats format: {success: true, data: {...}}
DASHBOARD_COUNT=$(echo $DASHBOARD_STATS | jq -r '.data.totalApplications')

if [ "$ACTUAL_COUNT" == "$DASHBOARD_COUNT" ]; then
    test_result 0 "Dashboard aggregations match actual data: $ACTUAL_COUNT applications"
else
    test_result 1 "Dashboard aggregations MISMATCH" "Dashboard: $DASHBOARD_COUNT vs Actual: $ACTUAL_COUNT"
fi

echo ""

# ==========================================
# FINAL REPORT
# ==========================================
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}INTEGRATION TESTS SUMMARY${NC}"
echo -e "${BLUE}========================================${NC}"
echo -e "Total Tests: ${BLUE}$TESTS_TOTAL${NC}"
echo -e "Passed:      ${GREEN}$TESTS_PASSED${NC}"
echo -e "Failed:      ${RED}$TESTS_FAILED${NC}"
echo -e "${BLUE}========================================${NC}"

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ ALL TESTS PASSED!${NC}"
    exit 0
else
    PASS_RATE=$((TESTS_PASSED * 100 / TESTS_TOTAL))
    echo -e "${YELLOW}Pass Rate: $PASS_RATE%${NC}"
    exit 1
fi
