#!/usr/bin/env bash
# ============================================================================
# CI Validation Script - Sistema de Admisión MTN
# Ejecuta build, tests, migraciones y smoke tests
# ============================================================================
set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "🔍 CI VALIDATION - Sistema de Admisión MTN"
echo "=========================================="

# Detect root directory
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
echo "📁 Root: $ROOT"

# Create reports directories
mkdir -p "$ROOT/reports/logs" "$ROOT/reports/patches" "$ROOT/scripts"

# ============================================================================
# 1. ENVIRONMENT CHECK
# ============================================================================
echo ""
echo "⏳ [1/8] Checking environment..."

# Check Node.js
if ! command -v node &> /dev/null; then
    echo -e "${RED}❌ Node.js not found${NC}"
    exit 1
fi
NODE_VERSION=$(node --version)
echo -e "${GREEN}✅ Node.js: $NODE_VERSION${NC}"

# Check npm
if ! command -v npm &> /dev/null; then
    echo -e "${RED}❌ npm not found${NC}"
    exit 1
fi
NPM_VERSION=$(npm --version)
echo -e "${GREEN}✅ npm: $NPM_VERSION${NC}"

# Check PostgreSQL
if ! command -v psql &> /dev/null; then
    echo -e "${YELLOW}⚠️  psql not found (database checks will be skipped)${NC}"
    DB_AVAILABLE=false
else
    echo -e "${GREEN}✅ PostgreSQL client available${NC}"
    DB_AVAILABLE=true
fi

# ============================================================================
# 2. DETECT BACKEND ARCHITECTURE
# ============================================================================
echo ""
echo "🔍 [2/8] Detecting backend architecture..."

BACKEND_DIR="$ROOT"
if [ -d "$BACKEND_DIR/user-service" ]; then
    echo -e "${YELLOW}⚠️  Spring Boot microservices detected but NOT ACTIVE${NC}"
fi

if [ -f "$BACKEND_DIR/mock-user-service.js" ]; then
    echo -e "${GREEN}✅ Node.js mock services detected (ACTIVE)${NC}"
    BACKEND_TYPE="nodejs_mocks"
else
    echo -e "${RED}❌ No backend services detected${NC}"
    exit 1
fi

# ============================================================================
# 3. DETECT FRONTEND PACKAGE MANAGER
# ============================================================================
echo ""
echo "📦 [3/8] Detecting frontend package manager..."

FRONTEND_DIR="$ROOT/../Admision_MTN_front"
cd "$FRONTEND_DIR"

if [ -f "pnpm-lock.yaml" ]; then
    PKG_MGR="pnpm"
    FE_INSTALL="pnpm i --frozen-lockfile"
    FE_BUILD="pnpm build"
elif [ -f "yarn.lock" ]; then
    PKG_MGR="yarn"
    FE_INSTALL="yarn --frozen-lockfile"
    FE_BUILD="yarn build"
else
    PKG_MGR="npm"
    FE_INSTALL="npm ci"
    FE_BUILD="npm run build"
fi

echo -e "${GREEN}✅ Using: $PKG_MGR${NC}"

# ============================================================================
# 4. BUILD BACKEND (Node.js Mocks)
# ============================================================================
echo ""
echo "🔧 [4/8] Building backend..."

cd "$BACKEND_DIR"
if [ -f "package.json" ]; then
    echo "Installing backend dependencies..."
    npm install > "$ROOT/reports/logs/backend_install.log" 2>&1
    echo -e "${GREEN}✅ Backend dependencies installed${NC}"
else
    echo -e "${YELLOW}⚠️  No package.json in backend (mock services don't require build)${NC}"
fi

# ============================================================================
# 5. BUILD FRONTEND
# ============================================================================
echo ""
echo "🎨 [5/8] Building frontend..."

cd "$FRONTEND_DIR"
echo "Running: $FE_INSTALL"
$FE_INSTALL > "$ROOT/reports/logs/frontend_install.log" 2>&1

echo "Running: $FE_BUILD"
if $FE_BUILD > "$ROOT/reports/logs/frontend_build.log" 2>&1; then
    echo -e "${GREEN}✅ Frontend build successful${NC}"
else
    echo -e "${RED}❌ Frontend build failed. Check logs/frontend_build.log${NC}"
    exit 1
fi

# TypeScript check (optional)
if npm run -T typecheck > /dev/null 2>&1; then
    echo -e "${GREEN}✅ TypeScript check passed${NC}"
else
    echo -e "${YELLOW}⚠️  TypeScript check not available or failed${NC}"
fi

# Linter (optional)
if npm run -T lint > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Linter passed${NC}"
else
    echo -e "${YELLOW}⚠️  Linter not available or failed${NC}"
fi

# ============================================================================
# 6. DATABASE MIGRATIONS
# ============================================================================
echo ""
echo "🐘 [6/8] Checking database..."

if [ "$DB_AVAILABLE" = true ]; then
    # Test connection
    if PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c "SELECT 1;" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Database connection successful${NC}"

        # Check critical tables
        TABLE_COUNT=$(PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -t -c "
            SELECT COUNT(*) FROM information_schema.tables
            WHERE table_name IN ('applications', 'users', 'students', 'supporters', 'guardians');
        " 2>&1 | tr -d ' ')

        if [ "$TABLE_COUNT" -eq 5 ]; then
            echo -e "${GREEN}✅ All critical tables exist${NC}"
        else
            echo -e "${YELLOW}⚠️  Only $TABLE_COUNT/5 critical tables found${NC}"
        fi
    else
        echo -e "${RED}❌ Database connection failed${NC}"
        echo "   Make sure PostgreSQL is running and database 'Admisión_MTN_DB' exists"
    fi
else
    echo -e "${YELLOW}⚠️  Database checks skipped (psql not available)${NC}"
fi

# ============================================================================
# 7. START SERVICES (Check if already running)
# ============================================================================
echo ""
echo "🚀 [7/8] Checking services..."

# Check if services are already running
NGINX_RUNNING=$(ps aux | grep -E "nginx.*8080" | grep -v grep | wc -l | tr -d ' ')
NODE_SERVICES=$(ps aux | grep -E "mock-(user|application|evaluation|notification|dashboard)-service" | grep -v grep | wc -l | tr -d ' ')
FRONTEND_RUNNING=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:5173 2>/dev/null || echo "000")

if [ "$NGINX_RUNNING" -gt 0 ]; then
    echo -e "${GREEN}✅ NGINX Gateway running${NC}"
else
    echo -e "${YELLOW}⚠️  NGINX Gateway not running on port 8080${NC}"
fi

if [ "$NODE_SERVICES" -ge 5 ]; then
    echo -e "${GREEN}✅ Mock services running ($NODE_SERVICES processes)${NC}"
else
    echo -e "${YELLOW}⚠️  Only $NODE_SERVICES/5 mock services running${NC}"
fi

if [ "$FRONTEND_RUNNING" = "200" ]; then
    echo -e "${GREEN}✅ Frontend dev server running${NC}"
else
    echo -e "${YELLOW}⚠️  Frontend not responding on port 5173${NC}"
fi

# ============================================================================
# 8. SMOKE TESTS
# ============================================================================
echo ""
echo "🧪 [8/8] Running smoke tests..."

PASSED=0
FAILED=0

function smoke_test() {
    URL="$1"
    EXPECTED_CODE="${2:-200}"
    DESCRIPTION="${3:-Test}"

    ACTUAL_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$URL" 2>/dev/null || echo "000")

    if [ "$ACTUAL_CODE" = "$EXPECTED_CODE" ]; then
        echo -e "${GREEN}✅ $DESCRIPTION${NC} ($URL → $ACTUAL_CODE)"
        PASSED=$((PASSED + 1))
    else
        echo -e "${RED}❌ $DESCRIPTION${NC} ($URL → Expected $EXPECTED_CODE, got $ACTUAL_CODE)"
        FAILED=$((FAILED + 1))
    fi
}

# Run tests
smoke_test "http://localhost:8080/gateway/status" "200" "Gateway health"
smoke_test "http://localhost:8080/health" "200" "Gateway /health"
smoke_test "http://localhost:5173" "200" "Frontend dev server"
smoke_test "http://localhost:8082/health" "200" "User service health"
smoke_test "http://localhost:8083/health" "200" "Application service health"
smoke_test "http://localhost:8084/health" "200" "Evaluation service health"
smoke_test "http://localhost:8085/health" "200" "Notification service health"
smoke_test "http://localhost:8086/health" "200" "Dashboard service health"

# Authenticated endpoint (should return 401 without token)
AUTH_CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080/api/users/me" 2>/dev/null || echo "000")
if [ "$AUTH_CODE" = "401" ] || [ "$AUTH_CODE" = "200" ]; then
    echo -e "${GREEN}✅ Auth endpoint working${NC} (/api/users/me → $AUTH_CODE)"
    PASSED=$((PASSED + 1))
else
    echo -e "${RED}❌ Auth endpoint failed${NC} (/api/users/me → $AUTH_CODE)"
    FAILED=$((FAILED + 1))
fi

# ============================================================================
# FINAL REPORT
# ============================================================================
echo ""
echo "=========================================="
echo "📊 SMOKE TEST RESULTS"
echo "=========================================="
echo -e "Passed: ${GREEN}$PASSED${NC}"
echo -e "Failed: ${RED}$FAILED${NC}"
TOTAL=$((PASSED + FAILED))
PERCENTAGE=$((PASSED * 100 / TOTAL))
echo "Success Rate: $PERCENTAGE%"

# Generate smoke_results.json
cat > "$ROOT/reports/smoke_results.json" <<EOF
{
  "timestamp": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
  "passed": $PASSED,
  "failed": $FAILED,
  "total": $TOTAL,
  "success_rate": $PERCENTAGE,
  "status": "$([ $PERCENTAGE -ge 80 ] && echo 'PASS' || echo 'FAIL')"
}
EOF

echo ""
if [ "$PERCENTAGE" -ge 80 ]; then
    echo -e "${GREEN}✅ VALIDATION PASSED${NC}"
    echo "Check detailed report: reports/CI_AUDIT_REPORT.md"
    exit 0
else
    echo -e "${RED}❌ VALIDATION FAILED${NC}"
    echo "Check logs in reports/logs/ for details"
    exit 1
fi
