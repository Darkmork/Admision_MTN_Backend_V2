#!/bin/bash

# start-microservices-only.sh
# Script para iniciar la arquitectura 100% microservicios (sin monolito)

set -e

echo "🚀 Iniciando Sistema 100% Microservicios MTN"
echo "==========================================="
echo ""

# Colores para output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Variables de configuración
PROJECT_DIR="/Users/jorgegangale/Library/Mobile Documents/com~apple~CloudDocs/Proyectos/Admision_MTN/Admision_MTN_backend"
COMPOSE_FILE="docker-compose.microservices-only.yml"

# Función de logging
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Verificar que estamos en el directorio correcto
if [ ! -f "$COMPOSE_FILE" ]; then
    error "No se encuentra el archivo $COMPOSE_FILE"
    error "Asegúrate de estar en el directorio del backend"
    exit 1
fi

# Función para verificar si Docker está ejecutándose
check_docker() {
    log "Verificando Docker..."
    if ! docker info > /dev/null 2>&1; then
        error "Docker no está ejecutándose. Por favor, inicia Docker Desktop."
        exit 1
    fi
    log "✅ Docker está ejecutándose"
}

# Función para limpiar contenedores y volúmenes previos
cleanup() {
    log "Limpiando contenedores y volúmenes previos..."
    
    # Detener todos los contenedores relacionados
    echo "Deteniendo contenedores existentes..."
    docker-compose -f "$COMPOSE_FILE" down --remove-orphans --volumes || true
    
    # Limpiar imágenes dangling
    echo "Limpiando imágenes no utilizadas..."
    docker image prune -f || true
    
    log "✅ Limpieza completada"
}

# Función para construir las imágenes
build_images() {
    log "Construyendo imágenes de microservicios..."
    
    # Construir todas las imágenes desde cero
    docker-compose -f "$COMPOSE_FILE" build --no-cache --parallel
    
    if [ $? -eq 0 ]; then
        log "✅ Todas las imágenes construidas exitosamente"
    else
        error "Falló la construcción de imágenes"
        exit 1
    fi
}

# Función para iniciar los servicios
start_services() {
    log "Iniciando servicios de microservicios..."
    
    # Iniciar servicios en orden específico
    echo "1. Iniciando bases de datos..."
    docker-compose -f "$COMPOSE_FILE" up -d users-db applications-db evaluations-db notifications-db
    sleep 15
    
    echo "2. Iniciando RabbitMQ..."
    docker-compose -f "$COMPOSE_FILE" up -d rabbitmq
    sleep 10
    
    echo "3. Iniciando microservicios..."
    docker-compose -f "$COMPOSE_FILE" up -d user-service application-service evaluation-service notification-service
    sleep 20
    
    echo "4. Iniciando API Gateway..."
    docker-compose -f "$COMPOSE_FILE" up -d api-gateway
    sleep 5
    
    echo "5. Iniciando servicios de observabilidad..."
    docker-compose -f "$COMPOSE_FILE" up -d prometheus grafana
    
    log "✅ Todos los servicios iniciados"
}

# Función para verificar el estado de los servicios
check_health() {
    log "Verificando estado de servicios..."
    
    echo ""
    echo "🏥 Estado de Salud de Servicios:"
    echo "=================================="
    
    # Verificar bases de datos
    echo ""
    echo "📊 Bases de Datos:"
    for db in users-db applications-db evaluations-db notifications-db; do
        if docker-compose -f "$COMPOSE_FILE" ps "$db" | grep -q "Up"; then
            echo -e "  ${GREEN}✅${NC} $db - Ejecutándose"
        else
            echo -e "  ${RED}❌${NC} $db - No disponible"
        fi
    done
    
    # Verificar RabbitMQ
    echo ""
    echo "🐰 Message Broker:"
    if docker-compose -f "$COMPOSE_FILE" ps rabbitmq | grep -q "Up"; then
        echo -e "  ${GREEN}✅${NC} RabbitMQ - Ejecutándose"
    else
        echo -e "  ${RED}❌${NC} RabbitMQ - No disponible"
    fi
    
    # Verificar microservicios
    echo ""
    echo "🏗️ Microservicios:"
    for service in user-service application-service evaluation-service notification-service; do
        if docker-compose -f "$COMPOSE_FILE" ps "$service" | grep -q "Up"; then
            echo -e "  ${GREEN}✅${NC} $service - Ejecutándose"
        else
            echo -e "  ${RED}❌${NC} $service - No disponible"
        fi
    done
    
    # Verificar API Gateway
    echo ""
    echo "🌐 API Gateway:"
    if docker-compose -f "$COMPOSE_FILE" ps api-gateway | grep -q "Up"; then
        echo -e "  ${GREEN}✅${NC} NGINX API Gateway - Ejecutándose"
    else
        echo -e "  ${RED}❌${NC} NGINX API Gateway - No disponible"
    fi
    
    # Verificar observabilidad
    echo ""
    echo "📈 Observabilidad:"
    for service in prometheus grafana; do
        if docker-compose -f "$COMPOSE_FILE" ps "$service" | grep -q "Up"; then
            echo -e "  ${GREEN}✅${NC} $service - Ejecutándose"
        else
            echo -e "  ${RED}❌${NC} $service - No disponible"
        fi
    done
}

# Función para mostrar información de acceso
show_access_info() {
    log "Información de Acceso"
    
    echo ""
    echo "🌐 URLs de Acceso:"
    echo "=================="
    echo ""
    echo "📍 API Gateway (Punto de entrada único):"
    echo "   http://localhost:8080"
    echo ""
    echo "🔍 Health Check del Gateway:"
    echo "   http://localhost:8080/health"
    echo ""
    echo "📊 Monitoreo y Observabilidad:"
    echo "   • RabbitMQ Management: http://localhost:15672 (admin/admin123)"
    echo "   • Prometheus: http://localhost:9090"
    echo "   • Grafana: http://localhost:3001 (admin/admin123)"
    echo ""
    echo "🏗️ Microservicios Directos (Solo para desarrollo):"
    echo "   • User Service: http://localhost:8082/api/users"
    echo "   • Application Service: http://localhost:8083/api/applications"
    echo "   • Evaluation Service: http://localhost:8084/api/evaluations"
    echo "   • Notification Service: http://localhost:8085/api/notifications"
    echo ""
    echo "📊 Bases de Datos (Solo para administración):"
    echo "   • Users DB: postgresql://users_admin:users123@localhost:5433/users_db"
    echo "   • Applications DB: postgresql://app_admin:app123@localhost:5434/applications_db"
    echo "   • Evaluations DB: postgresql://eval_admin:eval123@localhost:5435/evaluations_db"
    echo "   • Notifications DB: postgresql://notif_admin:notif123@localhost:5436/notifications_db"
    echo ""
    echo "⚠️  IMPORTANTE: El frontend debe apuntar a http://localhost:8080 (API Gateway)"
    echo "   No usar puertos individuales de microservicios en producción."
    echo ""
}

# Función para mostrar logs útiles
show_useful_commands() {
    log "Comandos Útiles"
    
    echo ""
    echo "🛠️ Comandos para Administración:"
    echo "================================="
    echo ""
    echo "Ver logs de todos los servicios:"
    echo "  docker-compose -f $COMPOSE_FILE logs -f"
    echo ""
    echo "Ver logs de un servicio específico:"
    echo "  docker-compose -f $COMPOSE_FILE logs -f user-service"
    echo ""
    echo "Detener todos los servicios:"
    echo "  docker-compose -f $COMPOSE_FILE down"
    echo ""
    echo "Reiniciar un servicio específico:"
    echo "  docker-compose -f $COMPOSE_FILE restart user-service"
    echo ""
    echo "Ver estado de servicios:"
    echo "  docker-compose -f $COMPOSE_FILE ps"
    echo ""
    echo "Acceder a una base de datos:"
    echo "  docker-compose -f $COMPOSE_FILE exec users-db psql -U users_admin -d users_db"
    echo ""
}

# Función principal
main() {
    log "Iniciando proceso de despliegue 100% microservicios"
    
    # Cambiar al directorio del proyecto
    cd "$PROJECT_DIR" || exit 1
    
    # Ejecutar pasos del despliegue
    check_docker
    cleanup
    build_images
    start_services
    
    # Esperar un poco para que los servicios se estabilicen
    echo ""
    log "Esperando que los servicios se estabilicen..."
    sleep 30
    
    # Verificar estado
    check_health
    
    # Mostrar información de acceso
    show_access_info
    
    # Mostrar comandos útiles
    show_useful_commands
    
    echo ""
    log "🎉 Sistema 100% Microservicios iniciado exitosamente!"
    echo ""
    echo "El frontend puede conectarse a: http://localhost:8080"
    echo ""
    warn "Recuerda configurar el frontend para usar el API Gateway en puerto 8080"
    echo ""
}

# Manejar señales para limpieza
trap 'echo -e "\n${RED}Proceso interrumpido${NC}"; exit 1' INT TERM

# Ejecutar función principal
main "$@"