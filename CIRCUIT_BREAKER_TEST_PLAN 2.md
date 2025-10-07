# Plan de Pruebas - Circuit Breakers Diferenciados

**Fecha:** Octubre 2025
**Objetivo:** Validar la implementación de 5 categorías de circuit breakers

---

## 1. Preparación del Entorno

### Paso 1: Verificar Servicios Activos
```bash
cd "/Users/jorgegangale/Library/Mobile Documents/com~apple~CloudDocs/Proyectos/Admision_MTN/Admision_MTN_backend"

# Verificar que todos los servicios estén corriendo
lsof -ti:8082,8083,8084,8085,8086,8087

# Si no están corriendo, iniciar
./start-microservices-gateway.sh
```

### Paso 2: Verificar NGINX Gateway
```bash
# Test que NGINX esté corriendo
curl -s http://localhost:8080/health

# Verificar configuración
sudo nginx -t -c "$(pwd)/local-gateway.conf"
```

### Paso 3: Preparar Tokens de Autenticación
```bash
# Token de ADMIN (ya disponible en tu setup)
export ADMIN_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiIxIiwiZW1haWwiOiJqb3JnZS5nYW5nYWxlQG10bi5jbCIsInJvbGUiOiJBRE1JTiIsImlhdCI6MTc1OTI3OTg1MCwiZXhwIjoxNzU5MzY2MjUwfQ==.mock-signature"

# Token de COORDINATOR
export COORD_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiIzIiwiZW1haWwiOiJjYXJsb3MubW9yYWxlc0BtdG4uY2wiLCJyb2xlIjoiQ09PUkRJTkFUT1IiLCJpYXQiOjE3NTkyNzk4NTAsImV4cCI6MTc1OTM2NjI1MH0=.mock-signature"
```

---

## 2. Test Suite: Simple Query Breaker (2s timeout)

### Test 2.1: Verificar Timeout Corto
```bash
# Endpoint: GET /api/users/roles
echo "Test 2.1: Simple Query - Users Roles"
time curl -X GET "http://localhost:8080/api/users/roles" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json"

# ✅ Esperado:
# - Response en < 2.5s (incluye latencia red)
# - Status 200
# - Lista de roles: ["ADMIN", "TEACHER", "COORDINATOR", ...]
```

### Test 2.2: Test de Resilencia (con DB caída)
```bash
# 1. Detener PostgreSQL
brew services stop postgresql@14

# 2. Hacer múltiples requests (debe abrir breaker después de 60% de errores = 6/10)
echo "Enviando 10 requests con DB caída..."
for i in {1..10}; do
  echo "Request $i:"
  curl -w "\nTime: %{time_total}s\n" -s -X GET "http://localhost:8080/api/users/roles" \
    -H "Authorization: Bearer $ADMIN_TOKEN" | head -n 3
  sleep 0.5
done

# ✅ Esperado:
# - Primeros 1-4 requests fallan con timeout de 2s
# - Después del request 6 (60% threshold), breaker se abre
# - Siguientes requests fallan inmediatamente (sin esperar 2s)
# - Log debe mostrar: "⚠️ [Circuit Breaker Simple] OPEN"

# 3. Restaurar PostgreSQL
brew services start postgresql@14

# 4. Esperar 20s (resetTimeout) y verificar recuperación
sleep 25
echo "Verificando recuperación..."
curl -X GET "http://localhost:8080/api/users/roles" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# ✅ Esperado:
# - Log muestra: "🔄 [Circuit Breaker Simple] HALF-OPEN"
# - Request exitoso
# - Log muestra: "✅ [Circuit Breaker Simple] CLOSED"
```

---

## 3. Test Suite: Medium Query Breaker (5s timeout)

### Test 3.1: Verificar Timeout Estándar
```bash
# Endpoint: GET /api/dashboard/stats
echo "Test 3.1: Medium Query - Dashboard Stats"
time curl -X GET "http://localhost:8080/api/dashboard/stats" \
  -H "Content-Type: application/json"

# ✅ Esperado:
# - Response en < 5.5s
# - Status 200
# - Datos de estadísticas del dashboard
```

### Test 3.2: Test de Threshold (50%)
```bash
# Detener DB
brew services stop postgresql@14

# Enviar 10 requests (debe abrir en request 5)
for i in {1..10}; do
  echo "Request $i:"
  curl -w "Time: %{time_total}s\n" -s -X GET "http://localhost:8080/api/dashboard/stats" | head -n 2
  sleep 0.5
done

# ✅ Esperado:
# - Breaker abre después del request 5 (50% threshold)
# - Log: "⚠️ [Circuit Breaker Medium] OPEN"

# Restaurar DB y esperar recovery
brew services start postgresql@14
sleep 35  # resetTimeout 30s + margen

curl -X GET "http://localhost:8080/api/dashboard/stats"

# ✅ Esperado: Recuperación exitosa
```

---

## 4. Test Suite: Heavy Query Breaker (10s timeout)

### Test 4.1: Verificar Timeout Largo
```bash
# Endpoint: GET /api/analytics/dashboard-metrics
echo "Test 4.1: Heavy Query - Dashboard Metrics"
time curl -X GET "http://localhost:8080/api/analytics/dashboard-metrics" \
  -H "Content-Type: application/json"

# ✅ Esperado:
# - Response en < 10.5s
# - Status 200
# - Métricas completas del dashboard
```

### Test 4.2: Test de Threshold Bajo (40%)
```bash
# Detener DB
brew services stop postgresql@14

# Enviar 10 requests (debe abrir en request 4)
for i in {1..10}; do
  echo "Request $i:"
  curl -w "Time: %{time_total}s\n" -s -X GET "http://localhost:8080/api/analytics/temporal-trends" | head -n 2
  sleep 0.5
done

# ✅ Esperado:
# - Breaker abre después del request 4 (40% threshold)
# - Timeout más largo (10s) permite queries complejas
# - Log: "⚠️ [Circuit Breaker Heavy] OPEN"

# Restaurar y esperar recovery largo
brew services start postgresql@14
sleep 65  # resetTimeout 60s + margen

curl -X GET "http://localhost:8080/api/analytics/temporal-trends"

# ✅ Esperado: Recuperación después de 60s
```

---

## 5. Test Suite: Write Operation Breaker (3s timeout)

### Test 5.1: Verificar Timeout en Escritura
```bash
# Endpoint: POST /api/users
echo "Test 5.1: Write Operation - Create User"
time curl -X POST "http://localhost:8080/api/users" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Test",
    "lastName": "User CB",
    "email": "test.cb@mtn.cl",
    "password": "admin123",
    "role": "TEACHER",
    "subject": "MATHEMATICS",
    "educationalLevel": "HIGH_SCHOOL"
  }'

# ✅ Esperado:
# - Response en < 3.5s
# - Status 201
# - Usuario creado con horarios
```

### Test 5.2: Test de Threshold Muy Bajo (30%)
```bash
# Detener DB
brew services stop postgresql@14

# Enviar 10 requests (debe abrir en request 3)
for i in {1..10}; do
  echo "Request $i:"
  curl -w "Time: %{time_total}s\n" -s -X POST "http://localhost:8080/api/users" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"firstName\":\"Test$i\",\"lastName\":\"User\",\"email\":\"test$i@mtn.cl\",\"password\":\"admin123\",\"role\":\"TEACHER\"}" | head -n 2
  sleep 0.5
done

# ✅ Esperado:
# - Breaker abre después del request 3 (30% threshold)
# - Protege integridad de datos cerrando rápido
# - Log: "⚠️ [Circuit Breaker Write] OPEN"

# Restaurar y esperar recovery
brew services start postgresql@14
sleep 50  # resetTimeout 45s + margen

# ✅ Esperado: Recuperación después de 45s
```

---

## 6. Test Suite: External Service Breaker (8s timeout)

### Test 6.1: Verificar Timeout en Email
```bash
# Endpoint: POST /api/email/send-verification
echo "Test 6.1: External Service - Send Email"
time curl -X POST "http://localhost:8080/api/email/send-verification" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com"
  }'

# ✅ Esperado:
# - Response en < 8.5s (SMTP puede ser lento)
# - Status 200
# - Código de verificación en response
```

### Test 6.2: Test de Threshold Alto (70%)
```bash
# Simular fallos de SMTP (apagar WiFi o cambiar credenciales SMTP inválidas)
# Editar mock-notification-service.js temporalmente para forzar errores

# Enviar 10 requests (debe abrir en request 7)
for i in {1..10}; do
  echo "Request $i:"
  curl -w "Time: %{time_total}s\n" -s -X POST "http://localhost:8080/api/email/send-verification" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"test$i@example.com\"}" | head -n 2
  sleep 1
done

# ✅ Esperado:
# - Breaker muy tolerante, abre después del request 7 (70% threshold)
# - Timeout largo (8s) da tiempo a servicios externos lentos
# - Log: "⚠️ [Circuit Breaker External] OPEN"

# Esperar recovery muy largo
sleep 125  # resetTimeout 120s + margen

# ✅ Esperado: Recuperación después de 2 minutos
```

---

## 7. Test de Monitoreo de Logs

### Ver Logs de Dashboard Service
```bash
tail -f /tmp/dashboard-service.log | grep "Circuit Breaker"
```

### Ver Logs de Todos los Servicios
```bash
# En una terminal separada
tail -f /tmp/{user,application,evaluation,notification,dashboard,guardian}-service.log | grep "Circuit Breaker"
```

**Ejemplo de output esperado:**
```
⚠️ [Circuit Breaker Medium] OPEN - Too many failures in dashboard service
🔄 [Circuit Breaker Medium] HALF-OPEN - Testing recovery
✅ [Circuit Breaker Medium] CLOSED - Dashboard service recovered
⚠️ [Circuit Breaker Simple] OPEN - Too many failures in user service
```

---

## 8. Test de Performance Comparativo

### Antes (Circuit Breaker Único)
```bash
# Simular 100 requests con mezcla de endpoints
echo "Benchmark ANTES (CB único 5s):"
for i in {1..100}; do
  curl -s "http://localhost:8080/api/users/roles" &
  curl -s "http://localhost:8080/api/dashboard/stats" &
  curl -s "http://localhost:8080/api/analytics/temporal-trends" &
done
wait

# Medir tiempo promedio
```

### Después (Circuit Breakers Diferenciados)
```bash
# Mismo test
echo "Benchmark DESPUÉS (5 CB diferenciados):"
for i in {1..100}; do
  curl -s "http://localhost:8080/api/users/roles" &  # Simple: 2s
  curl -s "http://localhost:8080/api/dashboard/stats" &  # Medium: 5s
  curl -s "http://localhost:8080/api/analytics/temporal-trends" &  # Heavy: 10s
done
wait

# ✅ Esperado:
# - Queries simples más rápidas (2s vs 5s)
# - Queries complejas con más tiempo (10s vs 5s)
# - Menor tasa de fallos en queries complejas
```

---

## 9. Verificación de Implementación

### Check 1: Verificar que Código Tiene los 5 Breakers
```bash
# Dashboard Service
grep -c "QueryBreaker\|OperationBreaker\|ServiceBreaker" mock-dashboard-service.js

# ✅ Esperado: 5 (uno por cada categoría)
```

### Check 2: Verificar Event Listeners
```bash
# Verificar que cada breaker tiene eventos
grep -A 10 "setupBreakerEvents" mock-dashboard-service.js

# ✅ Esperado: Ver setup para Simple, Medium, Heavy, Write, External
```

### Check 3: Verificar Uso Correcto en Endpoints
```bash
# Verificar que analytics usa heavy breaker
grep -B 5 "heavyQueryBreaker.fire" mock-dashboard-service.js

# ✅ Esperado: Ver uso en /api/analytics/*
```

---

## 10. Criterios de Éxito

### ✅ Test Pasado Si:

1. **Simple Query Breaker:**
   - Timeout < 2.5s
   - Abre al 60% de errores (6/10 requests)
   - Recupera en ~20s

2. **Medium Query Breaker:**
   - Timeout < 5.5s
   - Abre al 50% de errores (5/10 requests)
   - Recupera en ~30s

3. **Heavy Query Breaker:**
   - Timeout < 10.5s
   - Abre al 40% de errores (4/10 requests)
   - Recupera en ~60s

4. **Write Operation Breaker:**
   - Timeout < 3.5s
   - Abre al 30% de errores (3/10 requests)
   - Recupera en ~45s

5. **External Service Breaker:**
   - Timeout < 8.5s
   - Abre al 70% de errores (7/10 requests)
   - Recupera en ~120s

6. **Logs:**
   - Cada breaker loguea open/halfOpen/close con nombre correcto
   - No hay errores de configuración

---

## 11. Script de Validación Automatizado

```bash
#!/bin/bash
# validate-circuit-breakers.sh

echo "🧪 Iniciando validación de circuit breakers..."

# Test 1: Simple Query
echo "\n1. Test Simple Query Breaker..."
SIMPLE_TIME=$(curl -w "%{time_total}" -s -o /dev/null "http://localhost:8080/api/users/roles" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
if (( $(echo "$SIMPLE_TIME < 2.5" | bc -l) )); then
  echo "✅ Simple query timeout correcto: ${SIMPLE_TIME}s"
else
  echo "❌ Simple query demasiado lento: ${SIMPLE_TIME}s (esperado <2.5s)"
fi

# Test 2: Medium Query
echo "\n2. Test Medium Query Breaker..."
MEDIUM_TIME=$(curl -w "%{time_total}" -s -o /dev/null "http://localhost:8080/api/dashboard/stats")
if (( $(echo "$MEDIUM_TIME < 5.5" | bc -l) )); then
  echo "✅ Medium query timeout correcto: ${MEDIUM_TIME}s"
else
  echo "❌ Medium query demasiado lento: ${MEDIUM_TIME}s (esperado <5.5s)"
fi

# Test 3: Heavy Query
echo "\n3. Test Heavy Query Breaker..."
HEAVY_TIME=$(curl -w "%{time_total}" -s -o /dev/null "http://localhost:8080/api/analytics/dashboard-metrics")
if (( $(echo "$HEAVY_TIME < 10.5" | bc -l) )); then
  echo "✅ Heavy query timeout correcto: ${HEAVY_TIME}s"
else
  echo "❌ Heavy query demasiado lento: ${HEAVY_TIME}s (esperado <10.5s)"
fi

echo "\n✅ Validación completada!"
```

---

## Contacto

**Fecha:** Octubre 2025
**Responsable:** Claude Code
**Archivo:** CIRCUIT_BREAKER_TEST_PLAN.md
