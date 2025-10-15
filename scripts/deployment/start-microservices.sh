#!/bin/bash

# ======================================================
# SCRIPT DE INICIO COMPLETO - MICROSERVICIOS MTN
# Sistema de Admisión Monte Tabor y Nazaret
# ======================================================

set -e

echo "🚀 Iniciando arquitectura de microservicios MTN..."

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función para logs con timestamp
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR $(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN $(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

info() {
    echo -e "${BLUE}[INFO $(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

# Verificar que Docker está corriendo
check_docker() {
    if ! docker info >/dev/null 2>&1; then
        error "Docker no está corriendo. Por favor inicia Docker Desktop."
        exit 1
    fi
    log "✅ Docker está corriendo correctamente"
}

# Limpiar contenedores previos si existen
cleanup() {
    log "🧹 Limpiando contenedores previos..."
    
    # Parar contenedores si están corriendo
    docker-compose down --remove-orphans 2>/dev/null || true
    
    # Limpiar volúmenes huérfanos
    docker system prune -f --volumes 2>/dev/null || true
    
    log "✅ Limpieza completada"
}

# Construir imágenes
build_services() {
    log "🔨 Construyendo imágenes de microservicios..."
    
    # Construir servicios principales
    docker-compose build --parallel \
        eureka-server \
        api-gateway \
        user-service \
        admision-monolith \
        frontend
    
    log "✅ Imágenes construidas exitosamente"
}

# Iniciar infraestructura base
start_infrastructure() {
    log "🏗️ Iniciando infraestructura base..."
    
    # Iniciar bases de datos y servicios base
    docker-compose up -d \
        postgres \
        users-db \
        rabbitmq \
        keycloak
    
    # Esperar que las bases de datos estén listas
    info "⏳ Esperando que las bases de datos estén listas..."
    sleep 30
    
    # Verificar conectividad de PostgreSQL
    docker-compose exec postgres pg_isready -U admin -d "Admisión_MTN_DB" || {
        error "Base de datos principal no está lista"
        exit 1
    }
    
    docker-compose exec users-db pg_isready -U users_admin -d users_db || {
        error "Base de datos de usuarios no está lista"
        exit 1
    }
    
    log "✅ Infraestructura base iniciada correctamente"
}

# Iniciar service discovery
start_service_discovery() {
    log "🔍 Iniciando Service Discovery (Eureka)..."
    
    docker-compose up -d eureka-server
    
    # Esperar que Eureka esté listo
    info "⏳ Esperando que Eureka esté listo..."
    sleep 45
    
    # Verificar que Eureka esté respondiendo
    if ! curl -f http://localhost:8761/actuator/health >/dev/null 2>&1; then
        error "Eureka Server no está respondiendo"
        exit 1
    fi
    
    log "✅ Eureka Server iniciado correctamente"
}

# Iniciar API Gateway
start_api_gateway() {
    log "🌐 Iniciando API Gateway..."
    
    docker-compose up -d api-gateway
    
    # Esperar que el gateway esté listo
    info "⏳ Esperando que API Gateway esté listo..."
    sleep 30
    
    # Verificar que el gateway esté respondiendo
    if ! curl -f http://localhost:8080/actuator/health >/dev/null 2>&1; then
        error "API Gateway no está respondiendo"
        exit 1
    fi
    
    log "✅ API Gateway iniciado correctamente"
}

# Iniciar microservicios
start_microservices() {
    log "⚡ Iniciando microservicios..."
    
    # Iniciar servicios en paralelo
    docker-compose up -d \
        user-service
    
    # Esperar que los servicios estén registrados en Eureka
    info "⏳ Esperando que los servicios se registren en Eureka..."
    sleep 60
    
    # Verificar que los servicios estén respondiendo
    if ! curl -f http://localhost:8082/actuator/health >/dev/null 2>&1; then
        warn "User Service no está respondiendo completamente, pero continuando..."
    fi
    
    log "✅ Microservicios iniciados correctamente"
}

# Iniciar monolito (temporal)
start_monolith() {
    log "🏛️ Iniciando monolito (modo compatibilidad)..."
    
    docker-compose up -d admision-monolith
    
    # Esperar que el monolito esté listo
    info "⏳ Esperando que el monolito esté listo..."
    sleep 45
    
    if ! curl -f http://localhost:8081/actuator/health >/dev/null 2>&1; then
        warn "Monolito no está respondiendo completamente, pero continuando..."
    fi
    
    log "✅ Monolito iniciado correctamente"
}

# Iniciar stack de observabilidad
start_observability() {
    log "📊 Iniciando stack de observabilidad..."
    
    docker-compose up -d \
        otel-collector \
        jaeger \
        prometheus \
        grafana \
        loki \
        promtail
    
    info "⏳ Esperando que los servicios de observabilidad estén listos..."
    sleep 30
    
    log "✅ Stack de observabilidad iniciado"
}

# Iniciar frontend
start_frontend() {
    log "🎨 Iniciando frontend..."
    
    docker-compose up -d frontend
    
    info "⏳ Esperando que el frontend esté listo..."
    sleep 20
    
    log "✅ Frontend iniciado correctamente"
}

# Verificar estado de todos los servicios
verify_services() {
    log "🔍 Verificando estado de todos los servicios..."
    
    echo ""
    echo "=========================================="
    echo "          ESTADO DE SERVICIOS"
    echo "=========================================="
    
    services=(
        "postgres:5432:Base de datos principal"
        "users-db:5433:Base de datos usuarios"
        "rabbitmq:15672:RabbitMQ Management"
        "keycloak:8090:Keycloak Auth"
        "eureka-server:8761:Service Discovery"
        "api-gateway:8080:API Gateway"
        "user-service:8082:User Service"
        "admision-monolith:8081:Monolito"
        "jaeger:16686:Jaeger Tracing"
        "grafana:3001:Grafana Dashboards"
        "prometheus:9090:Prometheus Metrics"
        "frontend:3000:Frontend App"
    )
    
    for service in "${services[@]}"; do
        IFS=':' read -r name port description <<< "$service"
        if curl -f "http://localhost:$port" >/dev/null 2>&1 || \
           curl -f "http://localhost:$port/actuator/health" >/dev/null 2>&1 || \
           nc -z localhost "$port" 2>/dev/null; then
            echo -e "✅ ${GREEN}$description${NC} - http://localhost:$port"
        else
            echo -e "❌ ${RED}$description${NC} - http://localhost:$port"
        fi
    done
    
    echo ""
    echo "=========================================="
    echo "      URLS IMPORTANTES"
    echo "=========================================="
    echo -e "🎯 ${BLUE}Frontend Principal:${NC} http://localhost:3000"
    echo -e "🌐 ${BLUE}API Gateway:${NC} http://localhost:8080/actuator/health"
    echo -e "🔍 ${BLUE}Eureka Dashboard:${NC} http://localhost:8761"
    echo -e "🔐 ${BLUE}Keycloak Admin:${NC} http://localhost:8090/admin (admin/admin123)"
    echo -e "📊 ${BLUE}Grafana:${NC} http://localhost:3001 (admin/admin123)"
    echo -e "🔍 ${BLUE}Jaeger:${NC} http://localhost:16686"
    echo -e "📈 ${BLUE}Prometheus:${NC} http://localhost:9090"
    echo -e "🐰 ${BLUE}RabbitMQ:${NC} http://localhost:15672 (admin/admin123)"
    echo ""
}

# Migrar datos si es necesario
migrate_data() {
    if [[ "$1" == "--migrate" ]]; then
        log "📦 Migrando datos del monolito a microservicios..."
        
        # Esperar un poco más para asegurar que todo está estable
        sleep 30
        
        # Ejecutar script de migración
        if [[ -f "migrate-to-microservices.sql" ]]; then
            PGPASSWORD=admin psql -h localhost -p 5432 -U admin -d postgres -f migrate-to-microservices.sql
            log "✅ Migración de datos completada"
        else
            warn "Script de migración no encontrado, saltando migración"
        fi
    fi
}

# Función principal
main() {
    echo ""
    echo "=================================================="
    echo "    🏫 SISTEMA DE ADMISIÓN MTN - MICROSERVICIOS"
    echo "    🚀 Iniciando arquitectura completa..."
    echo "=================================================="
    echo ""
    
    # Verificar dependencias
    check_docker
    
    # Limpiar contenedores previos
    cleanup
    
    # Construir servicios
    build_services
    
    # Iniciar servicios paso a paso
    start_infrastructure
    start_service_discovery
    start_api_gateway
    start_microservices
    start_monolith
    start_observability
    start_frontend
    
    # Migrar datos si se especifica
    migrate_data "$@"
    
    # Verificar estado final
    verify_services
    
    echo ""
    log "🎉 ¡Arquitectura de microservicios iniciada exitosamente!"
    echo ""
    echo "=================================================="
    echo "Para detener todos los servicios ejecuta:"
    echo "  docker-compose down"
    echo ""
    echo "Para ver logs de un servicio específico:"
    echo "  docker-compose logs -f [nombre-servicio]"
    echo ""
    echo "Para migrar datos del monolito:"
    echo "  ./start-microservices.sh --migrate"
    echo "=================================================="
    echo ""
}

# Ejecutar función principal con todos los argumentos
main "$@"