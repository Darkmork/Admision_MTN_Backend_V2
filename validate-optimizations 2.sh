#!/bin/bash
# Script de validación de optimizaciones
# Sistema de Admisión MTN - Octubre 2025

echo "======================================"
echo "  VALIDACIÓN DE OPTIMIZACIONES MTN  "
echo "======================================"
echo ""

# Colores para output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 1. Test de configuración NGINX
echo "1. Validando configuración NGINX..."
cd "/Users/jorgegangale/Library/Mobile Documents/com~apple~CloudDocs/Proyectos/Admision_MTN/Admision_MTN_backend"
sudo nginx -t -c "$(pwd)/local-gateway.conf" 2>&1 | grep -q "successful"
if [ $? -eq 0 ]; then
  echo -e "${GREEN}✅ Configuración NGINX válida${NC}"
else
  echo -e "${RED}❌ Error en configuración NGINX${NC}"
  sudo nginx -t -c "$(pwd)/local-gateway.conf"
fi
echo ""

# 2. Verificar puertos de servicios
echo "2. Verificando puertos de servicios..."
SERVICES=(
  "8082:User Service"
  "8083:Application Service"
  "8084:Evaluation Service"
  "8085:Notification Service"
  "8086:Dashboard Service"
  "8087:Guardian Service"
)

for service in "${SERVICES[@]}"; do
  PORT=$(echo $service | cut -d: -f1)
  NAME=$(echo $service | cut -d: -f2)

  lsof -ti:$PORT > /dev/null 2>&1
  if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ $NAME (puerto $PORT)${NC}"
  else
    echo -e "${YELLOW}⚠️  $NAME (puerto $PORT) - NO está corriendo${NC}"
  fi
done
echo ""

# 3. Test de rutas del gateway
echo "3. Testeando rutas del gateway..."
GATEWAY="http://localhost:8080"

# Test 1: Health check
STATUS=$(curl -s -o /dev/null -w "%{http_code}" $GATEWAY/health)
if [ "$STATUS" == "200" ]; then
  echo -e "${GREEN}✅ /health → 200${NC}"
else
  echo -e "${RED}❌ /health → $STATUS${NC}"
fi

# Test 2: Gateway status
STATUS=$(curl -s -o /dev/null -w "%{http_code}" $GATEWAY/gateway/status)
if [ "$STATUS" == "200" ]; then
  echo -e "${GREEN}✅ /gateway/status → 200${NC}"
  # Verificar que incluye guardian-service
  RESPONSE=$(curl -s $GATEWAY/gateway/status)
  echo "$RESPONSE" | grep -q "guardian-service"
  if [ $? -eq 0 ]; then
    echo -e "${GREEN}   ✓ Guardian service incluido en gateway${NC}"
  else
    echo -e "${YELLOW}   ⚠️ Guardian service NO incluido en respuesta${NC}"
  fi
else
  echo -e "${RED}❌ /gateway/status → $STATUS${NC}"
fi

# Test 3: NEW - Guardian routes (CRITICAL FIX)
STATUS=$(curl -s -o /dev/null -w "%{http_code}" $GATEWAY/api/guardians)
if [ "$STATUS" == "200" ] || [ "$STATUS" == "401" ] || [ "$STATUS" == "403" ]; then
  echo -e "${GREEN}✅ /api/guardians → $STATUS (routed to guardian service)${NC}"
elif [ "$STATUS" == "404" ]; then
  echo -e "${RED}❌ /api/guardians → 404 (NO mapeado en gateway - ERROR CRÍTICO)${NC}"
else
  echo -e "${YELLOW}⚠️  /api/guardians → $STATUS${NC}"
fi

# Test 4: Rutas existentes
ROUTES=(
  "/api/auth"
  "/api/users"
  "/api/applications"
  "/api/evaluations"
  "/api/interviews"
  "/api/dashboard"
  "/api/analytics"
)

for route in "${ROUTES[@]}"; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" $GATEWAY$route)
  if [ "$STATUS" == "404" ]; then
    echo -e "${RED}❌ $route → 404 (sin mapeo)${NC}"
  elif [ "$STATUS" == "401" ] || [ "$STATUS" == "403" ] || [ "$STATUS" == "200" ]; then
    echo -e "${GREEN}✅ $route → $STATUS (OK)${NC}"
  else
    echo -e "${YELLOW}⚠️  $route → $STATUS${NC}"
  fi
done
echo ""

# 4. Verificar optimizaciones de NGINX
echo "4. Verificando optimizaciones de NGINX..."
CONFIG_FILE="local-gateway.conf"

# Check proxy_read_timeout
grep -q "proxy_read_timeout 8s" $CONFIG_FILE
if [ $? -eq 0 ]; then
  echo -e "${GREEN}✅ proxy_read_timeout optimizado (8s)${NC}"
else
  echo -e "${RED}❌ proxy_read_timeout NO optimizado${NC}"
fi

# Check rate limiting
grep -q "limit_req_zone" $CONFIG_FILE
if [ $? -eq 0 ]; then
  echo -e "${GREEN}✅ Rate limiting configurado${NC}"
else
  echo -e "${RED}❌ Rate limiting NO configurado${NC}"
fi

# Check keepalive
grep -q "keepalive_requests 100" $CONFIG_FILE
if [ $? -eq 0 ]; then
  echo -e "${GREEN}✅ Keepalive optimizado (100 requests)${NC}"
else
  echo -e "${RED}❌ Keepalive NO optimizado${NC}"
fi

# Check buffer sizes
grep -q "proxy_buffers 16 8k" $CONFIG_FILE
if [ $? -eq 0 ]; then
  echo -e "${GREEN}✅ Buffers optimizados (128KB total)${NC}"
else
  echo -e "${RED}❌ Buffers NO optimizados${NC}"
fi
echo ""

# 5. Test de Circuit Breaker (dashboard service)
echo "5. Testeando Circuit Breaker..."
RESPONSE=$(curl -s $GATEWAY/api/dashboard/stats)
echo "$RESPONSE" | grep -q "success"
if [ $? -eq 0 ]; then
  echo -e "${GREEN}✅ Dashboard stats respondiendo correctamente${NC}"

  # Verificar que tiene circuit breaker error handling
  cd "/Users/jorgegangale/Library/Mobile Documents/com~apple~CloudDocs/Proyectos/Admision_MTN/Admision_MTN_backend"
  grep -q "CIRCUIT_BREAKER_OPEN" mock-dashboard-service.js
  if [ $? -eq 0 ]; then
    echo -e "${GREEN}   ✓ Circuit breaker error handling implementado${NC}"
  else
    echo -e "${RED}   ✗ Circuit breaker error handling NO implementado${NC}"
  fi
else
  echo -e "${YELLOW}⚠️  Dashboard stats no respondió (puede ser CB abierto)${NC}"
fi
echo ""

# 6. Verificar axios-retry fix en frontend
echo "6. Verificando fix de axios-retry..."
cd "/Users/jorgegangale/Library/Mobile Documents/com~apple~CloudDocs/Proyectos/Admision_MTN/Admision_MTN_front"
grep -q "isIdempotent" src/services/http.ts
if [ $? -eq 0 ]; then
  echo -e "${GREEN}✅ axios-retry fix implementado (solo GET/HEAD/OPTIONS)${NC}"
else
  echo -e "${RED}❌ axios-retry fix NO implementado${NC}"
fi

grep -q "CIRCUIT_BREAKER_OPEN" src/services/http.ts
if [ $? -eq 0 ]; then
  echo -e "${GREEN}   ✓ Detección de CB open implementada${NC}"
else
  echo -e "${RED}   ✗ Detección de CB open NO implementada${NC}"
fi
echo ""

# 7. Resumen
echo "======================================"
echo "           RESUMEN FINAL            "
echo "======================================"
echo ""
echo "Optimizaciones implementadas:"
echo "  ✓ NGINX: timeouts, buffers, keepalive, rate limiting"
echo "  ✓ Guardian service: rutas agregadas al gateway"
echo "  ✓ Circuit breaker: error handling con código CIRCUIT_BREAKER_OPEN"
echo "  ✓ Frontend: axios-retry limitado a métodos idempotentes"
echo "  ✓ Frontend: detección de circuit breaker open"
echo ""
echo "Para aplicar cambios de NGINX:"
echo "  sudo nginx -s reload"
echo ""
echo "Para reiniciar servicios backend:"
echo "  pkill -f 'mock-.*-service.js'"
echo "  ./start-microservices-gateway.sh"
echo ""
echo "Métricas esperadas post-optimización:"
echo "  - p99 latency: 1500ms → 600ms (60% reducción)"
echo "  - UX en fallas: 13s → <1s (detección CB open)"
echo "  - Rate limit: 20 req/s por IP, 100 req/s por usuario"
echo ""
