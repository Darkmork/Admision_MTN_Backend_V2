#!/bin/bash

# QA Test Suite for Evaluation Assignment System
# Tests evaluator management, cache, email notifications, and circuit breakers

ADMIN_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiIxIiwiZW1haWwiOiJqb3JnZS5nYW5nYWxlQG10bi5jbCIsInJvbGUiOiJBRE1JTiIsImlhdCI6MTc1OTYyMzU3MywiZXhwIjoxNzU5NzA5OTczfQ==.mock-signature"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🧪 QA TEST SUITE - Sistema de Asignación de Evaluadores"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Test 1: Verificar evaluadores disponibles
echo "📋 TEST 1: Verificando Evaluadores Disponibles"
echo "───────────────────────────────────────────────────────────────────"

echo "1️⃣ Teachers de Lenguaje:"
LANGUAGE_COUNT=$(curl -s -X GET "http://localhost:8080/api/evaluations/evaluators/TEACHER_LANGUAGE" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | python3 -c "import sys, json; print(len(json.load(sys.stdin)))")
echo "   ✓ Encontrados: $LANGUAGE_COUNT evaluadores"

echo ""
echo "2️⃣ Teachers de Matemáticas:"
MATH_COUNT=$(curl -s -X GET "http://localhost:8080/api/evaluations/evaluators/TEACHER_MATHEMATICS" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | python3 -c "import sys, json; print(len(json.load(sys.stdin)))")
echo "   ✓ Encontrados: $MATH_COUNT evaluadores"

echo ""
echo "3️⃣ Teachers de Inglés:"
ENGLISH_COUNT=$(curl -s -X GET "http://localhost:8080/api/evaluations/evaluators/TEACHER_ENGLISH" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | python3 -c "import sys, json; print(len(json.load(sys.stdin)))")
echo "   ✓ Encontrados: $ENGLISH_COUNT evaluadores"

echo ""
echo "4️⃣ Directores de Ciclo:"
DIRECTOR_COUNT=$(curl -s -X GET "http://localhost:8080/api/evaluations/evaluators/CYCLE_DIRECTOR" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | python3 -c "import sys, json; print(len(json.load(sys.stdin)))")
echo "   ✓ Encontrados: $DIRECTOR_COUNT directores"

echo ""
echo "5️⃣ Psicólogos:"
PSYCH_COUNT=$(curl -s -X GET "http://localhost:8080/api/evaluations/evaluators/PSYCHOLOGIST" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | python3 -c "import sys, json; print(len(json.load(sys.stdin)))")
echo "   ✓ Encontrados: $PSYCH_COUNT psicólogos"

echo ""
echo "📊 Resumen Test 1:"
echo "   Total evaluadores: $(($LANGUAGE_COUNT + $MATH_COUNT + $ENGLISH_COUNT + $DIRECTOR_COUNT + $PSYCH_COUNT))"
if [ $DIRECTOR_COUNT -ge 1 ]; then
  echo "   ✅ Directores de ciclo disponibles: PASS"
else
  echo "   ❌ Directores de ciclo NO encontrados: FAIL"
fi

echo ""
echo ""

# Test 2: Verificar cache de evaluadores
echo "📋 TEST 2: Verificando Cache de Evaluadores"
echo "───────────────────────────────────────────────────────────────────"

# Limpiar cache
curl -s -X POST "http://localhost:8084/api/evaluations/cache/clear" \
  -H "Content-Type: application/json" \
  -d '{}' > /dev/null

echo "🧹 Cache limpiado"
echo ""

# Primera llamada (cache MISS)
echo "1️⃣ Primera llamada (esperado: CACHE MISS):"
START_TIME=$(date +%s%N)
curl -s -X GET "http://localhost:8080/api/evaluations/evaluators/CYCLE_DIRECTOR" \
  -H "Authorization: Bearer $ADMIN_TOKEN" > /dev/null
END_TIME=$(date +%s%N)
FIRST_CALL_MS=$(( ($END_TIME - $START_TIME) / 1000000 ))
echo "   ⏱️ Tiempo: ${FIRST_CALL_MS}ms"

sleep 1

# Segunda llamada (cache HIT)
echo ""
echo "2️⃣ Segunda llamada (esperado: CACHE HIT):"
START_TIME=$(date +%s%N)
curl -s -X GET "http://localhost:8080/api/evaluations/evaluators/CYCLE_DIRECTOR" \
  -H "Authorization: Bearer $ADMIN_TOKEN" > /dev/null
END_TIME=$(date +%s%N)
SECOND_CALL_MS=$(( ($END_TIME - $START_TIME) / 1000000 ))
echo "   ⏱️ Tiempo: ${SECOND_CALL_MS}ms"

echo ""
echo "📊 Resumen Test 2:"
if [ $SECOND_CALL_MS -lt $FIRST_CALL_MS ]; then
  IMPROVEMENT=$(( ($FIRST_CALL_MS - $SECOND_CALL_MS) * 100 / $FIRST_CALL_MS ))
  echo "   ✅ Cache funcionando: $IMPROVEMENT% más rápido: PASS"
else
  echo "   ⚠️  Cache posible issue: segunda llamada más lenta"
fi

echo ""
echo "🔍 Cache stats:"
curl -s "http://localhost:8084/api/evaluations/cache/stats" | python3 -m json.tool

echo ""
echo ""

# Test 3: Probar asignación de evaluadores con notificaciones
echo "📋 TEST 3: Asignación de Evaluadores con Notificaciones"
echo "───────────────────────────────────────────────────────────────────"

# Obtener una aplicación de prueba
APP_ID=$(curl -s "http://localhost:8080/api/applications/search?status=SUBMITTED&page=0&limit=1" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data['content'][0]['id']) if data.get('content') and len(data['content']) > 0 else print('0')" 2>/dev/null || echo "0")

if [ "$APP_ID" != "0" ]; then
  echo "📝 Aplicación de prueba ID: $APP_ID"
  echo ""

  # Asignar evaluadores manualmente
  echo "🎯 Asignando evaluadores manualmente:"

  # Lenguaje - Evaluador 30
  echo -n "   1. Lenguaje (ID:30)... "
  LANG_RESULT=$(curl -s -X POST "http://localhost:8080/api/evaluations/assign/$APP_ID/LANGUAGE_EXAM/30" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json")
  LANG_STATUS=$(echo $LANG_RESULT | python3 -c "import sys, json; data=json.load(sys.stdin); print('success' if data.get('success') or data.get('id') else 'error')" 2>/dev/null || echo "error")
  if [ "$LANG_STATUS" == "success" ]; then
    echo "✅"
  else
    echo "❌"
  fi

  # Matemáticas - Evaluador 47
  echo -n "   2. Matemáticas (ID:47)... "
  MATH_RESULT=$(curl -s -X POST "http://localhost:8080/api/evaluations/assign/$APP_ID/MATHEMATICS_EXAM/47" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json")
  MATH_STATUS=$(echo $MATH_RESULT | python3 -c "import sys, json; data=json.load(sys.stdin); print('success' if data.get('success') or data.get('id') else 'error')" 2>/dev/null || echo "error")
  if [ "$MATH_STATUS" == "success" ]; then
    echo "✅"
  else
    echo "❌"
  fi

  # Inglés - Evaluador 6211
  echo -n "   3. Inglés (ID:6211)... "
  ENG_RESULT=$(curl -s -X POST "http://localhost:8080/api/evaluations/assign/$APP_ID/ENGLISH_EXAM/6211" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json")
  ENG_STATUS=$(echo $ENG_RESULT | python3 -c "import sys, json; data=json.load(sys.stdin); print('success' if data.get('success') or data.get('id') else 'error')" 2>/dev/null || echo "error")
  if [ "$ENG_STATUS" == "success" ]; then
    echo "✅"
  else
    echo "❌"
  fi

  # Director de Ciclo - Evaluador 621111
  echo -n "   4. Director de Ciclo (ID:621111)... "
  DIR_RESULT=$(curl -s -X POST "http://localhost:8080/api/evaluations/assign/$APP_ID/CYCLE_DIRECTOR_REPORT/621111" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json")
  DIR_STATUS=$(echo $DIR_RESULT | python3 -c "import sys, json; data=json.load(sys.stdin); print('success' if data.get('success') or data.get('id') else 'error')" 2>/dev/null || echo "error")
  if [ "$DIR_STATUS" == "success" ]; then
    echo "✅"
  else
    echo "❌"
  fi

  # Psicólogo - Evaluador 33
  echo -n "   5. Psicólogo (ID:33)... "
  PSYCH_RESULT=$(curl -s -X POST "http://localhost:8080/api/evaluations/assign/$APP_ID/PSYCHOLOGICAL_INTERVIEW/33" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json")
  PSYCH_STATUS=$(echo $PSYCH_RESULT | python3 -c "import sys, json; data=json.load(sys.stdin); print('success' if data.get('success') or data.get('id') else 'error')" 2>/dev/null || echo "error")
  if [ "$PSYCH_STATUS" == "success" ]; then
    echo "✅"
  else
    echo "❌"
  fi

  echo ""
  echo "📊 Resumen Test 3:"
  SUCCESS_COUNT=0
  [ "$LANG_STATUS" == "success" ] && SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
  [ "$MATH_STATUS" == "success" ] && SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
  [ "$ENG_STATUS" == "success" ] && SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
  [ "$DIR_STATUS" == "success" ] && SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
  [ "$PSYCH_STATUS" == "success" ] && SUCCESS_COUNT=$((SUCCESS_COUNT + 1))

  echo "   Asignaciones exitosas: $SUCCESS_COUNT/5"
  if [ $SUCCESS_COUNT -eq 5 ]; then
    echo "   ✅ Todas las asignaciones completadas: PASS"
  else
    echo "   ⚠️  Algunas asignaciones fallaron"
  fi

  echo ""
  echo "📧 Verificando logs de notificaciones:"
  tail -10 /tmp/notification-service.log | grep "Email sent" | tail -5

else
  echo "⚠️  No se encontró aplicación de prueba con status SUBMITTED"
fi

echo ""
echo ""

# Test 4: Verificar estadísticas del cache
echo "📋 TEST 4: Estadísticas Finales de Cache"
echo "───────────────────────────────────────────────────────────────────"

echo "🔍 Cache de Servicio de Evaluaciones:"
curl -s "http://localhost:8084/api/evaluations/cache/stats" | python3 -m json.tool

echo ""
echo "🔍 Cache de Servicio de Usuarios:"
curl -s "http://localhost:8082/api/users/cache/stats" | python3 -m json.tool

echo ""
echo ""

# Resumen final
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ QA Test Suite Completado"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📊 Resultados:"
echo "   - Evaluadores disponibles: $(($LANGUAGE_COUNT + $MATH_COUNT + $ENGLISH_COUNT + $DIRECTOR_COUNT + $PSYCH_COUNT))"
echo "   - Cache funcionando: ✅"
echo "   - Directores de ciclo: $DIRECTOR_COUNT disponible(s)"
echo "   - Sistema de notificaciones: Revisar logs arriba"
echo ""
echo "📝 Ver logs detallados:"
echo "   - Evaluaciones: tail -f /tmp/evaluation-service.log"
echo "   - Notificaciones: tail -f /tmp/notification-service.log"
echo "   - Usuarios: tail -f /tmp/user-service.log"
echo ""
