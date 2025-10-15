#!/bin/bash

# Script para iniciar todos los microservicios + NGINX Gateway
# Estructura modular reorganizada

echo "========================================"
echo "INICIANDO SISTEMA DE ADMISIÓN MTN"
echo "========================================"
echo ""

# Directorio base
BASE_DIR="/Users/jorgegangale/Library/Mobile Documents/com~apple~CloudDocs/Proyectos/Admision_MTN/Admision_MTN_backend"
cd "$BASE_DIR"

echo "📍 Directorio base: $BASE_DIR"
echo ""

# 1. Verificar que los servicios existen
echo "🔍 Verificando archivos de servicios..."
SERVICES=(
    "services/user-service/src/mock-user-service.js"
    "services/application-service/src/mock-application-service.js"
    "services/evaluation-service/src/mock-evaluation-service.js"
    "services/notification-service/src/mock-notification-service.js"
    "services/dashboard-service/src/mock-dashboard-service.js"
    "services/guardian-service/src/mock-guardian-service.js"
)

for service in "${SERVICES[@]}"; do
    if [ ! -f "$service" ]; then
        echo "❌ ERROR: No se encuentra $service"
        exit 1
    else
        echo "  ✅ $(basename $service)"
    fi
done

echo ""
echo "✅ Todos los servicios encontrados"
echo ""

# 2. Matar procesos anteriores
echo "🧹 Limpiando procesos anteriores..."
pkill -f "mock-user-service.js" 2>/dev/null
pkill -f "mock-application-service.js" 2>/dev/null
pkill -f "mock-evaluation-service.js" 2>/dev/null
pkill -f "mock-notification-service.js" 2>/dev/null
pkill -f "mock-dashboard-service.js" 2>/dev/null
pkill -f "mock-guardian-service.js" 2>/dev/null
sleep 2
echo "✅ Procesos anteriores limpiados"
echo ""

# 3. Iniciar NGINX Gateway
echo "🌐 Iniciando NGINX Gateway..."
NGINX_CONF="$BASE_DIR/api-gateway/conf/local-gateway.conf"

if [ ! -f "$NGINX_CONF" ]; then
    echo "❌ ERROR: No se encuentra $NGINX_CONF"
    exit 1
fi

# Verificar si NGINX está corriendo
if pgrep -x "nginx" > /dev/null; then
    echo "  ⚠️  NGINX ya está corriendo, recargando configuración..."
    sudo nginx -s reload -c "$NGINX_CONF"
else
    echo "  Iniciando NGINX con configuración: $NGINX_CONF"
    sudo nginx -c "$NGINX_CONF"
fi

if [ $? -eq 0 ]; then
    echo "  ✅ NGINX iniciado en puerto 8080"
else
    echo "  ❌ ERROR al iniciar NGINX"
    exit 1
fi

echo ""

# 4. Iniciar servicios Node.js
echo "🚀 Iniciando microservicios Node.js..."
echo ""

echo "  📦 User Service (puerto 8082)..."
cd "$BASE_DIR"
node services/user-service/src/mock-user-service.js > /tmp/user-service.log 2>&1 &
sleep 1

echo "  📦 Application Service (puerto 8083)..."
cd "$BASE_DIR"
node services/application-service/src/mock-application-service.js > /tmp/application-service.log 2>&1 &
sleep 1

echo "  📦 Evaluation Service (puerto 8084)..."
cd "$BASE_DIR"
node services/evaluation-service/src/mock-evaluation-service.js > /tmp/evaluation-service.log 2>&1 &
sleep 1

echo "  📦 Notification Service (puerto 8085)..."
cd "$BASE_DIR"
node services/notification-service/src/mock-notification-service.js > /tmp/notification-service.log 2>&1 &
sleep 1

echo "  📦 Dashboard Service (puerto 8086)..."
cd "$BASE_DIR"
node services/dashboard-service/src/mock-dashboard-service.js > /tmp/dashboard-service.log 2>&1 &
sleep 1

echo "  📦 Guardian Service (puerto 8087)..."
cd "$BASE_DIR"
node services/guardian-service/src/mock-guardian-service.js > /tmp/guardian-service.log 2>&1 &
sleep 2

echo ""
echo "✅ Todos los servicios iniciados"
echo ""

# 5. Verificar que todos los servicios están corriendo
echo "🔍 Verificando health de servicios..."
echo ""

sleep 3  # Esperar a que todos los servicios estén listos

PORTS=(8082 8083 8084 8085 8086 8087 8080)
NAMES=("User" "Application" "Evaluation" "Notification" "Dashboard" "Guardian" "Gateway")

ALL_OK=true
for i in "${!PORTS[@]}"; do
    port="${PORTS[$i]}"
    name="${NAMES[$i]}"

    response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$port/health 2>/dev/null)

    if [ "$response" = "200" ]; then
        echo "  ✅ $name Service (port $port): UP"
    else
        echo "  ❌ $name Service (port $port): DOWN (HTTP $response)"
        ALL_OK=false
    fi
done

echo ""

if [ "$ALL_OK" = true ]; then
    echo "========================================"
    echo "✅ SISTEMA INICIADO CORRECTAMENTE"
    echo "========================================"
    echo ""
    echo "🌐 API Gateway: http://localhost:8080"
    echo "📊 User Service: http://localhost:8082"
    echo "📄 Application Service: http://localhost:8083"
    echo "📝 Evaluation Service: http://localhost:8084"
    echo "📧 Notification Service: http://localhost:8085"
    echo "📈 Dashboard Service: http://localhost:8086"
    echo "👨‍👩‍👧 Guardian Service: http://localhost:8087"
    echo ""
    echo "📝 Logs en /tmp/*-service.log"
    echo ""
    echo "Para detener: pkill -f 'mock-.*-service.js' && sudo nginx -s stop"
else
    echo "========================================"
    echo "⚠️  SISTEMA INICIADO CON ERRORES"
    echo "========================================"
    echo ""
    echo "Revisa los logs en /tmp/*-service.log"
    exit 1
fi
