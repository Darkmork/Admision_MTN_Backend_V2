#!/bin/bash

# QA Comprehensive Test Script
# Tests the complete admission workflow end-to-end

echo "════════════════════════════════════════════════════════════════"
echo "   🧪 QA COMPREHENSIVE TEST - Sistema de Admisión MTN"
echo "════════════════════════════════════════════════════════════════"
echo ""

# Admin token
ADMIN_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiIxIiwiZW1haWwiOiJqb3JnZS5nYW5nYWxlQG10bi5jbCIsInJvbGUiOiJBRE1JTiIsImlhdCI6MTc1OTcwOTQwMCwiZXhwIjoxNzU5Nzk1ODAwfQ==.mock-signature"

# Test counter
TESTS_PASSED=0
TESTS_FAILED=0
ERRORS_FOUND=()
WARNINGS_FOUND=()

# Function to check HTTP status
check_http_status() {
  local url=$1
  local expected_status=$2
  local description=$3

  HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$url" -H "Authorization: Bearer $ADMIN_TOKEN")

  if [ "$HTTP_STATUS" -eq "$expected_status" ]; then
    echo "  ✅ $description (HTTP $HTTP_STATUS)"
    ((TESTS_PASSED++))
    return 0
  else
    echo "  ❌ $description (Expected $expected_status, got $HTTP_STATUS)"
    ERRORS_FOUND+=("$description: Expected HTTP $expected_status, got $HTTP_STATUS")
    ((TESTS_FAILED++))
    return 1
  fi
}

# Function to test JSON response
test_json_response() {
  local url=$1
  local description=$2
  local expected_field=$3

  RESPONSE=$(curl -s "$url" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json")

  if echo "$RESPONSE" | jq -e "$expected_field" > /dev/null 2>&1; then
    echo "  ✅ $description"
    ((TESTS_PASSED++))
    return 0
  else
    echo "  ❌ $description (Field '$expected_field' not found or invalid)"
    ERRORS_FOUND+=("$description: Field '$expected_field' missing or invalid")
    ((TESTS_FAILED++))
    return 1
  fi
}

echo "═══════════════════════════════════════════════════════════════"
echo "1️⃣  HEALTH CHECKS - Verificando todos los servicios"
echo "═══════════════════════════════════════════════════════════════"
echo ""

check_http_status "http://localhost:8082/health" 200 "User Service Health"
check_http_status "http://localhost:8083/health" 200 "Application Service Health"
check_http_status "http://localhost:8084/health" 200 "Evaluation Service Health"
check_http_status "http://localhost:8085/health" 200 "Notification Service Health"
check_http_status "http://localhost:8086/health" 200 "Dashboard Service Health"
check_http_status "http://localhost:8087/health" 200 "Guardian Service Health"
check_http_status "http://localhost:8080/gateway/status" 200 "NGINX Gateway Status"

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "2️⃣  AUTHENTICATION - Probando login y JWT"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Test admin login
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "x-test-mode: true" \
  -d '{"email":"jorge.gangale@mtn.cl","password":"admin123"}')

if echo "$LOGIN_RESPONSE" | jq -e '.token' > /dev/null 2>&1; then
  echo "  ✅ Admin login successful"
  ADMIN_TOKEN_NEW=$(echo "$LOGIN_RESPONSE" | jq -r '.token')
  ((TESTS_PASSED++))
else
  echo "  ❌ Admin login failed"
  ERRORS_FOUND+=("Admin login: Token not returned")
  ((TESTS_FAILED++))
fi

# Test guardian login (from created data)
GUARDIAN_LOGIN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "x-test-mode: true" \
  -d '{"email":"jorge.gangale@gmail.com","password":"12345678"}')

if echo "$GUARDIAN_LOGIN" | jq -e '.token' > /dev/null 2>&1; then
  echo "  ✅ Guardian login successful (jorge.gangale@gmail.com)"
  GUARDIAN_TOKEN=$(echo "$GUARDIAN_LOGIN" | jq -r '.token')
  ((TESTS_PASSED++))
else
  echo "  ❌ Guardian login failed"
  ERRORS_FOUND+=("Guardian login: Failed for jorge.gangale@gmail.com")
  ((TESTS_FAILED++))
fi

# Test invalid credentials
INVALID_LOGIN=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"invalid@test.com","password":"wrongpass"}')

if [ "$INVALID_LOGIN" -eq 401 ] || [ "$INVALID_LOGIN" -eq 400 ]; then
  echo "  ✅ Invalid credentials properly rejected"
  ((TESTS_PASSED++))
else
  echo "  ⚠️  Warning: Invalid credentials returned HTTP $INVALID_LOGIN (expected 401/400)"
  WARNINGS_FOUND+=("Auth: Invalid credentials should return 401/400, got $INVALID_LOGIN")
fi

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "3️⃣  APPLICATIONS - Probando endpoints de postulaciones"
echo "═══════════════════════════════════════════════════════════════"
echo ""

test_json_response "http://localhost:8080/api/applications?page=0&limit=10" \
  "Get applications list" ".data"

test_json_response "http://localhost:8080/api/applications/40" \
  "Get specific application (ID 40)" ".id"

test_json_response "http://localhost:8080/api/applications/search?status=APPROVED" \
  "Search applications by status" ".data"

# Test application statistics
test_json_response "http://localhost:8080/api/applications/stats" \
  "Application statistics" ".total"

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "4️⃣  EVALUATIONS - Probando evaluaciones y entrevistas"
echo "═══════════════════════════════════════════════════════════════"
echo ""

test_json_response "http://localhost:8080/api/evaluations/application/40" \
  "Get evaluations for application 40" ".[0].id"

test_json_response "http://localhost:8080/api/interviews?applicationId=40" \
  "Get interviews for application 40" ".[0].id"

test_json_response "http://localhost:8080/api/evaluations/evaluators/TEACHER" \
  "Get evaluators by role (TEACHER)" ".[0].id"

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "5️⃣  DASHBOARD - Probando estadísticas y analytics"
echo "═══════════════════════════════════════════════════════════════"
echo ""

test_json_response "http://localhost:8080/api/dashboard/stats" \
  "General dashboard stats" ".totalApplications"

test_json_response "http://localhost:8080/api/dashboard/admin/detailed-stats?academicYear=2026" \
  "Admin detailed stats" ".academicYear"

test_json_response "http://localhost:8080/api/analytics/temporal-trends" \
  "Temporal trends analytics" ".trends"

test_json_response "http://localhost:8080/api/analytics/insights" \
  "Analytics insights" ".insights"

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "6️⃣  USERS & GUARDIANS - Probando gestión de usuarios"
echo "═══════════════════════════════════════════════════════════════"
echo ""

test_json_response "http://localhost:8080/api/users?page=0&limit=10" \
  "Get users list" ".users"

test_json_response "http://localhost:8080/api/users/roles" \
  "Get available roles" ".roles"

test_json_response "http://localhost:8080/api/guardians?page=0&limit=10" \
  "Get guardians list" ".guardians"

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "7️⃣  DATA INTEGRITY - Verificando datos en base de datos"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Check for created applications
APP_COUNT=$(PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -t -c "SELECT COUNT(*) FROM applications WHERE id IN (40, 41, 42);" 2>/dev/null | tr -d ' ')

if [ "$APP_COUNT" -eq 3 ]; then
  echo "  ✅ 3 test applications exist (40, 41, 42)"
  ((TESTS_PASSED++))
else
  echo "  ❌ Expected 3 test applications, found $APP_COUNT"
  ERRORS_FOUND+=("Database: Expected 3 applications, found $APP_COUNT")
  ((TESTS_FAILED++))
fi

# Check for evaluations
EVAL_COUNT=$(PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -t -c "SELECT COUNT(*) FROM evaluations WHERE application_id IN (40, 41, 42);" 2>/dev/null | tr -d ' ')

if [ "$EVAL_COUNT" -eq 9 ]; then
  echo "  ✅ 9 evaluations exist (3 per application)"
  ((TESTS_PASSED++))
else
  echo "  ⚠️  Warning: Expected 9 evaluations, found $EVAL_COUNT"
  WARNINGS_FOUND+=("Database: Expected 9 evaluations, found $EVAL_COUNT")
fi

# Check for interviews
INTERVIEW_COUNT=$(PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -t -c "SELECT COUNT(*) FROM interviews WHERE application_id IN (40, 41, 42);" 2>/dev/null | tr -d ' ')

if [ "$INTERVIEW_COUNT" -eq 3 ]; then
  echo "  ✅ 3 interviews exist (1 per application)"
  ((TESTS_PASSED++))
else
  echo "  ⚠️  Warning: Expected 3 interviews, found $INTERVIEW_COUNT"
  WARNINGS_FOUND+=("Database: Expected 3 interviews, found $INTERVIEW_COUNT")
fi

# Check for documents
DOC_COUNT=$(PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -t -c "SELECT COUNT(*) FROM documents WHERE application_id IN (40, 41, 42);" 2>/dev/null | tr -d ' ')

if [ "$DOC_COUNT" -eq 9 ]; then
  echo "  ✅ 9 documents exist (3 per application)"
  ((TESTS_PASSED++))
else
  echo "  ⚠️  Warning: Expected 9 documents, found $DOC_COUNT"
  WARNINGS_FOUND+=("Database: Expected 9 documents, found $DOC_COUNT")
fi

# Check for orphaned records
ORPHANED=$(PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -t -c "
  SELECT COUNT(*) FROM applications a
  WHERE NOT EXISTS (SELECT 1 FROM students s WHERE s.id = a.student_id)
     OR NOT EXISTS (SELECT 1 FROM guardians g WHERE g.id = a.guardian_id);
" 2>/dev/null | tr -d ' ')

if [ "$ORPHANED" -eq 0 ]; then
  echo "  ✅ No orphaned applications (all FKs valid)"
  ((TESTS_PASSED++))
else
  echo "  ❌ Found $ORPHANED orphaned applications"
  ERRORS_FOUND+=("Database: $ORPHANED applications with invalid foreign keys")
  ((TESTS_FAILED++))
fi

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "8️⃣  NOTIFICATIONS - Probando sistema de notificaciones"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Test notification config
test_json_response "http://localhost:8080/api/notifications/config" \
  "Get notification configurations" ".data"

# Test email templates
test_json_response "http://localhost:8080/api/email-templates/all" \
  "Get email templates" ".data"

echo ""
echo "════════════════════════════════════════════════════════════════"
echo "   📊 REPORTE FINAL DE QA"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "✅ Tests Pasados: $TESTS_PASSED"
echo "❌ Tests Fallidos: $TESTS_FAILED"
echo "⚠️  Warnings: ${#WARNINGS_FOUND[@]}"
echo ""

TOTAL_TESTS=$((TESTS_PASSED + TESTS_FAILED))
if [ $TOTAL_TESTS -gt 0 ]; then
  SUCCESS_RATE=$((TESTS_PASSED * 100 / TOTAL_TESTS))
  echo "📈 Tasa de Éxito: $SUCCESS_RATE%"
  echo ""
fi

if [ ${#ERRORS_FOUND[@]} -gt 0 ]; then
  echo "════════════════════════════════════════════════════════════════"
  echo "❌ ERRORES ENCONTRADOS:"
  echo "════════════════════════════════════════════════════════════════"
  for error in "${ERRORS_FOUND[@]}"; do
    echo "  • $error"
  done
  echo ""
fi

if [ ${#WARNINGS_FOUND[@]} -gt 0 ]; then
  echo "════════════════════════════════════════════════════════════════"
  echo "⚠️  WARNINGS ENCONTRADOS:"
  echo "════════════════════════════════════════════════════════════════"
  for warning in "${WARNINGS_FOUND[@]}"; do
    echo "  • $warning"
  done
  echo ""
fi

echo "════════════════════════════════════════════════════════════════"
echo "🔍 MEJORAS SUGERIDAS:"
echo "════════════════════════════════════════════════════════════════"
echo "  1. Implementar paginación consistente en todos los endpoints"
echo "  2. Agregar validación de RUT chileno en formularios"
echo "  3. Implementar rate limiting en endpoints públicos"
echo "  4. Agregar auditoría de cambios (audit log)"
echo "  5. Mejorar mensajes de error con códigos únicos"
echo "  6. Implementar soft delete para datos críticos"
echo "  7. Agregar índices en columnas más consultadas (email, rut)"
echo "  8. Implementar caché distribuido (Redis) para alta concurrencia"
echo "  9. Agregar validación de archivos (tamaño, tipo MIME)"
echo " 10. Implementar backup automático de base de datos"
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
  echo "🎉 ¡TODOS LOS TESTS PASARON! Sistema listo para producción."
  exit 0
else
  echo "⚠️  TESTS FALLIDOS: Revisar errores antes de deployment."
  exit 1
fi
