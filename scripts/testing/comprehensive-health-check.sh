#!/bin/bash
# ============================================================
# COMPREHENSIVE HEALTH CHECK & CI/CD VERIFICATION
# Sistema de Admisión MTN - Post-Restoration Validation
# ============================================================

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

PASS_COUNT=0
FAIL_COUNT=0
TOTAL_TESTS=0

log() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[✓ PASS]${NC} $1"
    PASS_COUNT=$((PASS_COUNT + 1))
}

log_error() {
    echo -e "${RED}[✗ FAIL]${NC} $1"
    FAIL_COUNT=$((FAIL_COUNT + 1))
}

run_test() {
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    local test_name="$1"
    local test_command="$2"

    echo -n "  Testing: $test_name... "
    if eval "$test_command" > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC}"
        PASS_COUNT=$((PASS_COUNT + 1))
    else
        echo -e "${RED}✗${NC}"
        FAIL_COUNT=$((FAIL_COUNT + 1))
    fi
}

echo -e "${BLUE}"
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║        COMPREHENSIVE HEALTH CHECK & CI/CD VERIFICATION       ║"
echo "║               Sistema de Admisión MTN v2.0                   ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# ============================================================
# SECTION 1: INFRASTRUCTURE HEALTH
# ============================================================
echo ""
log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
log "SECTION 1: Infrastructure Health Checks"
log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Database connectivity
run_test "PostgreSQL connection" "PGPASSWORD=admin123 psql -h localhost -U admin -d 'Admisión_MTN_DB' -c 'SELECT 1' -t"
run_test "Database has tables" "PGPASSWORD=admin123 psql -h localhost -U admin -d 'Admisión_MTN_DB' -t -c 'SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '\''public'\''' | grep -q '32'"
run_test "Users table exists" "PGPASSWORD=admin123 psql -h localhost -U admin -d 'Admisión_MTN_DB' -t -c 'SELECT COUNT(*) FROM users' | grep -qE '[0-9]+'"
run_test "Applications table exists" "PGPASSWORD=admin123 psql -h localhost -U admin -d 'Admisión_MTN_DB' -t -c 'SELECT COUNT(*) FROM applications' | grep -qE '[0-9]+'"

# Microservices health
log ""
log "Microservices Health Checks:"
run_test "User Service (8082)" "curl -s -f http://localhost:8082/health | grep -q '\"status\":\"UP\"'"
run_test "Application Service (8083)" "curl -s -f http://localhost:8083/health | grep -q '\"status\":\"UP\"'"
run_test "Evaluation Service (8084)" "curl -s -f http://localhost:8084/health | grep -q '\"status\":\"UP\"'"
run_test "Notification Service (8085)" "curl -s -f http://localhost:8085/health | grep -q '\"status\":\"UP\"'"
run_test "Dashboard Service (8086)" "curl -s -f http://localhost:8086/health | grep -q '\"status\":\"UP\"'"
run_test "Guardian Service (8087)" "curl -s -f http://localhost:8087/health | grep -q '\"status\":\"UP\"'"

# ============================================================
# SECTION 2: AUTHENTICATION & AUTHORIZATION
# ============================================================
echo ""
log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
log "SECTION 2: Authentication & Authorization"
log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Admin login
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8082/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"jorge.gangale@mtn.cl","password":"admin123"}')

run_test "Admin login successful" "echo '$LOGIN_RESPONSE' | grep -q '\"success\":true'"
run_test "Login returns JWT token" "echo '$LOGIN_RESPONSE' | grep -q '\"token\":'"
run_test "Login returns user data" "echo '$LOGIN_RESPONSE' | grep -q '\"role\":\"ADMIN\"'"

# Extract token
TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -n "$TOKEN" ]; then
    log_success "JWT token extracted successfully"

    # Token verification
    run_test "Token validation (/api/users/me)" "curl -s http://localhost:8082/api/users/me -H 'Authorization: Bearer $TOKEN' | grep -q '\"email\":\"jorge.gangale@mtn.cl\"'"
    run_test "Token includes role" "curl -s http://localhost:8082/api/users/me -H 'Authorization: Bearer $TOKEN' | grep -q '\"role\":\"ADMIN\"'"
else
    log_error "Failed to extract JWT token"
fi

# ============================================================
# SECTION 3: CORE API ENDPOINTS
# ============================================================
echo ""
log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
log "SECTION 3: Core API Endpoints"
log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# User Service endpoints
run_test "GET /api/users/roles" "curl -s -f http://localhost:8082/api/users/roles | grep -q '\"success\":true'"
run_test "GET /api/users (with auth)" "curl -s -f http://localhost:8082/api/users -H 'Authorization: Bearer $TOKEN' | grep -q '\"success\":true'"
run_test "GET /api/users/public/school-staff" "curl -s -f http://localhost:8082/api/users/public/school-staff | grep -q '\"success\":true'"

# Application Service endpoints
run_test "GET /api/applications (with auth)" "curl -s -f http://localhost:8083/api/applications -H 'Authorization: Bearer $TOKEN' | grep -q '\"success\":true'"
run_test "GET /api/applications/status/counts" "curl -s -f http://localhost:8083/api/applications/status/counts -H 'Authorization: Bearer $TOKEN' | grep -q '\"success\":true'"

# Evaluation Service endpoints
run_test "GET /api/evaluations/metadata/types" "curl -s -f http://localhost:8084/api/evaluations/metadata/types | grep -q '\"success\":true'"
run_test "GET /api/interviews/public/interviewers" "curl -s -f http://localhost:8084/api/interviews/public/interviewers | grep -q '\"success\":true'"

# Dashboard Service endpoints
run_test "GET /api/dashboard/stats" "curl -s -f http://localhost:8086/api/dashboard/stats -H 'Authorization: Bearer $TOKEN' | grep -q '\"success\":true'"

# Guardian Service endpoints
run_test "GET /api/guardians (with auth)" "curl -s -f http://localhost:8087/api/guardians -H 'Authorization: Bearer $TOKEN' | grep -q '\"success\":true'"

# ============================================================
# SECTION 4: DATA INTEGRITY
# ============================================================
echo ""
log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
log "SECTION 4: Data Integrity Checks"
log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Check data counts
USERS_COUNT=$(PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -t -c "SELECT COUNT(*) FROM users" | xargs)
APPLICATIONS_COUNT=$(PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -t -c "SELECT COUNT(*) FROM applications" | xargs)
STUDENTS_COUNT=$(PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -t -c "SELECT COUNT(*) FROM students" | xargs)
EVALUATIONS_COUNT=$(PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -t -c "SELECT COUNT(*) FROM evaluations" | xargs)

echo "  Database Record Counts:"
echo "    Users: $USERS_COUNT"
echo "    Applications: $APPLICATIONS_COUNT"
echo "    Students: $STUDENTS_COUNT"
echo "    Evaluations: $EVALUATIONS_COUNT"

run_test "Users table has data" "[ $USERS_COUNT -gt 0 ]"
run_test "Applications table has data" "[ $APPLICATIONS_COUNT -gt 0 ]"
run_test "Students table has data" "[ $STUDENTS_COUNT -gt 0 ]"

# Check admin user
run_test "Admin user exists" "PGPASSWORD=admin123 psql -h localhost -U admin -d 'Admisión_MTN_DB' -t -c 'SELECT COUNT(*) FROM users WHERE email = '\''jorge.gangale@mtn.cl'\'' AND role = '\''ADMIN'\''' | grep -q '1'"
run_test "Admin user is active" "PGPASSWORD=admin123 psql -h localhost -U admin -d 'Admisión_MTN_DB' -t -c 'SELECT active FROM users WHERE email = '\''jorge.gangale@mtn.cl'\''' | grep -q 't'"

# ============================================================
# SECTION 5: PERFORMANCE & RESILIENCE
# ============================================================
echo ""
log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
log "SECTION 5: Performance & Resilience Features"
log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Connection pooling
run_test "User Service has connection pool" "grep -q 'Connection pooling enabled' /tmp/user-service.log"
run_test "Application Service has connection pool" "grep -q 'Connection pooling enabled' /tmp/application-service.log"

# Caching
run_test "User Service cache enabled" "curl -s http://localhost:8082/api/users/cache/stats | grep -q '\"success\":true'"
run_test "Evaluation Service cache enabled" "curl -s http://localhost:8084/api/evaluations/cache/stats | grep -q '\"success\":true'"
run_test "Dashboard Service cache enabled" "curl -s http://localhost:8086/api/dashboard/cache/stats | grep -q '\"success\":true'"

# Circuit breakers (check logs for initialization)
run_test "Circuit breakers initialized" "grep -q 'Circuit Breaker' /tmp/*-service.log"

# ============================================================
# SECTION 6: SECURITY
# ============================================================
echo ""
log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
log "SECTION 6: Security Checks"
log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Password security
run_test "Passwords are BCrypt hashed" "PGPASSWORD=admin123 psql -h localhost -U admin -d 'Admisión_MTN_DB' -t -c 'SELECT password FROM users LIMIT 1' | grep -q '^\$2'"

# Unauthorized access
run_test "Protected endpoint blocks unauth access" "! curl -s -f http://localhost:8082/api/users -H 'Authorization: Bearer invalid-token' | grep -q '\"success\":true'"

# Credential encryption
run_test "RSA encryption keys generated" "grep -q 'RSA key pair generated' /tmp/user-service.log"

# ============================================================
# FINAL SUMMARY
# ============================================================
echo ""
log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
log "TEST EXECUTION SUMMARY"
log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

PASS_RATE=$((PASS_COUNT * 100 / TOTAL_TESTS))

echo -e "  Total Tests:  ${BLUE}$TOTAL_TESTS${NC}"
echo -e "  Passed:       ${GREEN}$PASS_COUNT${NC}"
echo -e "  Failed:       ${RED}$FAIL_COUNT${NC}"
echo -e "  Pass Rate:    ${BLUE}$PASS_RATE%${NC}"
echo ""

if [ $FAIL_COUNT -eq 0 ]; then
    echo -e "${GREEN}"
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║                  ✅ ALL TESTS PASSED                         ║"
    echo "║          System is fully operational and healthy!            ║"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    exit 0
else
    echo -e "${YELLOW}"
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║              ⚠️  SOME TESTS FAILED ($FAIL_COUNT/$TOTAL_TESTS)                    ║"
    echo "║           Review logs for detailed information               ║"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    exit 1
fi
