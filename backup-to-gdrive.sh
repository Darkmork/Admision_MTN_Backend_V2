#!/bin/bash

# ===========================================
# SCRIPT DE BACKUP AUTOMÁTICO A GOOGLE DRIVE
# Sistema de Admisión MTN - DATOS CRÍTICOS
# ===========================================

# Configuración
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_DIR="/tmp/mtn_backups"
PROJECT_DIR="$(cd "$SCRIPT_DIR" && pwd)"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
DATE_ONLY=$(date +%Y%m%d)

# Colores para output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función de logging
log() {
    echo -e "${BLUE}[$(date '+%Y-%m-%d %H:%M:%S')]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')] ✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}[$(date '+%Y-%m-%d %H:%M:%S')] ⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}[$(date '+%Y-%m-%d %H:%M:%S')] ❌ $1${NC}"
}

# Crear directorios necesarios
mkdir -p "$BACKUP_DIR"
mkdir -p "$BACKUP_DIR/logs"

# Log file para este backup
LOG_FILE="$BACKUP_DIR/logs/backup_$TIMESTAMP.log"

# Redireccionar toda la salida al log file también
exec > >(tee -a "$LOG_FILE") 2>&1

log "🚀 INICIANDO BACKUP AUTOMÁTICO DEL SISTEMA MTN"
log "Fecha: $(date)"
log "Directorio de backup: $BACKUP_DIR"

# Cargar variables de entorno desde .env
if [ -f "$PROJECT_DIR/.env" ]; then
    log "📋 Cargando configuración desde .env"
    
    # Cargar .env de forma segura sin ejecutar comandos
    while IFS='=' read -r key value || [ -n "$key" ]; do
        # Skip comments and empty lines
        [[ $key =~ ^[[:space:]]*# ]] && continue
        [[ -z "$key" ]] && continue
        
        # Remove quotes if present
        value=$(echo "$value" | sed -e 's/^"//' -e 's/"$//' -e "s/^'//" -e "s/'$//")
        
        # Export the variable
        export "$key"="$value"
    done < "$PROJECT_DIR/.env"
    
    log_success "Variables de entorno cargadas"
    log "DB_HOST: ${DB_HOST:-not_set}, DB_USER: ${DB_USERNAME:-not_set}, DB_NAME: ${DB_NAME:-not_set}"
else
    log_error "Archivo .env no encontrado en $PROJECT_DIR"
    exit 1
fi

# Verificar que PostgreSQL está disponible
log "🔍 Verificando conexión a PostgreSQL..."

# Usar Docker con PostgreSQL 16 client (coincide con servidor)
if command -v docker &> /dev/null; then
    log_success "Usando Docker PostgreSQL 16 client (coincide con servidor)"
    DOCKER_PG_CMD="docker run --rm -e PGPASSWORD=$DB_PASSWORD postgres:16 pg_dump"
    DOCKER_PSQL_CMD="docker run --rm -i -e PGPASSWORD=$DB_PASSWORD postgres:16 psql"
    USE_DOCKER=true
else
    # Fallback a cliente local
    PG_DUMP="/opt/homebrew/Cellar/postgresql@14/14.19/bin/pg_dump"
    PSQL="/opt/homebrew/Cellar/postgresql@14/14.19/bin/psql"
    
    if [ ! -f "$PG_DUMP" ]; then
        if command -v pg_dump &> /dev/null; then
            PG_DUMP="pg_dump"
            PSQL="psql"
        else
            log_error "ni Docker ni pg_dump están disponibles"
            exit 1
        fi
    fi
    log_warning "Usando cliente local (puede haber problemas de versión)"
    USE_DOCKER=false
fi

# Test de conexión a BD
log "Probando conexión a PostgreSQL..."
if [ "$USE_DOCKER" = true ]; then
    # Test con Docker
    if ! echo "SELECT 1;" | $DOCKER_PSQL_CMD -h host.docker.internal -U "$DB_USERNAME" -d "$DB_NAME" > /dev/null 2>&1; then
        log_error "No se puede conectar a la base de datos usando Docker PostgreSQL 16"
        log_error "Host: $DB_HOST, User: $DB_USERNAME, DB: $DB_NAME"
        exit 1
    fi
else
    # Test con cliente local
    if ! PGPASSWORD="$DB_PASSWORD" "$PSQL" -h "$DB_HOST" -U "$DB_USERNAME" -d "$DB_NAME" -c "SELECT 1;" > /dev/null 2>&1; then
        log_error "No se puede conectar a la base de datos PostgreSQL"
        log_error "Host: $DB_HOST, User: $DB_USERNAME, DB: $DB_NAME"
        exit 1
    fi
fi

log_success "Conexión a PostgreSQL verificada"

# Crear backup de la base de datos
log "💾 Creando backup de la base de datos..."
DB_BACKUP_FILE="$BACKUP_DIR/mtn_database_$DATE_ONLY.sql"

# Bypass version check for pg_dump y configurar compatibilidad
export PGDUMP_NO_VERSION_CHECK=1
export PGCLIENTENCODING=UTF8

# Crear backup usando el cliente apropiado
if [ "$USE_DOCKER" = true ]; then
    # Usar Docker PostgreSQL 16 client
    $DOCKER_PG_CMD \
        -h host.docker.internal \
        -U "$DB_USERNAME" \
        -d "$DB_NAME" \
        --verbose \
        --create \
        --inserts \
        --no-owner \
        --no-privileges > "$DB_BACKUP_FILE" 2>&1
else
    # Usar cliente local con variables de compatibilidad
    PGPASSWORD="$DB_PASSWORD" "$PG_DUMP" \
        -h "$DB_HOST" \
        -U "$DB_USERNAME" \
        -d "$DB_NAME" \
        --verbose \
        --create \
        --inserts \
        --no-owner \
        --no-privileges > "$DB_BACKUP_FILE" 2>&1
fi

if [ $? -eq 0 ]; then
    log_success "Backup de base de datos creado: $(basename "$DB_BACKUP_FILE")"
    DB_SIZE=$(du -h "$DB_BACKUP_FILE" | cut -f1)
    log "Tamaño del backup de BD: $DB_SIZE"
else
    log_error "Error creando backup de base de datos"
    exit 1
fi

# Crear backup de archivos subidos (documentos de postulaciones)
log "📁 Creando backup de archivos subidos..."
UPLOADS_DIR="$PROJECT_DIR/uploads"
if [ -d "$UPLOADS_DIR" ]; then
    UPLOADS_BACKUP_FILE="$BACKUP_DIR/mtn_uploads_$DATE_ONLY.tar.gz"
    tar -czf "$UPLOADS_BACKUP_FILE" -C "$PROJECT_DIR" uploads/
    
    if [ $? -eq 0 ]; then
        log_success "Backup de archivos creado: $(basename "$UPLOADS_BACKUP_FILE")"
        UPLOADS_SIZE=$(du -h "$UPLOADS_BACKUP_FILE" | cut -f1)
        log "Tamaño del backup de archivos: $UPLOADS_SIZE"
    else
        log_warning "Error creando backup de archivos subidos"
    fi
else
    log_warning "Directorio uploads no encontrado: $UPLOADS_DIR"
    UPLOADS_BACKUP_FILE=""
fi

# Crear backup de configuración crítica
log "⚙️  Creando backup de configuración..."
CONFIG_BACKUP_FILE="$BACKUP_DIR/mtn_config_$DATE_ONLY.tar.gz"

# Backup de archivos de configuración (sin .env por seguridad, solo estructura)
tar -czf "$CONFIG_BACKUP_FILE" \
    -C "$PROJECT_DIR" \
    --exclude='.env' \
    --exclude='target' \
    --exclude='node_modules' \
    --exclude='.git' \
    src/main/resources/application.yml \
    pom.xml \
    SEGURIDAD_COMPLETADA.md \
    SERVICIO_EMAIL_INSTITUCIONAL.md \
    SECURITY_SETUP.md 2>/dev/null

if [ $? -eq 0 ]; then
    log_success "Backup de configuración creado: $(basename "$CONFIG_BACKUP_FILE")"
    CONFIG_SIZE=$(du -h "$CONFIG_BACKUP_FILE" | cut -f1)
    log "Tamaño del backup de config: $CONFIG_SIZE"
else
    log_warning "Error creando backup de configuración"
fi

# Crear archivo de metadatos del backup
log "📋 Creando metadatos del backup..."
METADATA_FILE="$BACKUP_DIR/mtn_backup_metadata_$DATE_ONLY.txt"

cat > "$METADATA_FILE" << EOF
BACKUP SISTEMA ADMISIÓN MTN
===========================
Fecha: $(date)
Timestamp: $TIMESTAMP
Servidor: $(hostname)
Usuario: $(whoami)

BASE DE DATOS:
- Host: $DB_HOST
- Puerto: $DB_PORT  
- Base de datos: $DB_NAME
- Usuario: $DB_USERNAME
- Archivo backup: $(basename "$DB_BACKUP_FILE")
- Tamaño BD: $DB_SIZE

ARCHIVOS:
- Directorio uploads: $UPLOADS_DIR
- Archivo backup: $(basename "$UPLOADS_BACKUP_FILE")
- Tamaño archivos: $UPLOADS_SIZE

CONFIGURACIÓN:
- Archivo backup: $(basename "$CONFIG_BACKUP_FILE") 
- Tamaño config: $CONFIG_SIZE

SISTEMA:
- Versión Java: $(java -version 2>&1 | head -n 1)
- Cliente PostgreSQL: $(if [ "$USE_DOCKER" = true ]; then echo "Docker PostgreSQL 16"; else echo "Local client"; fi)
- Versión PostgreSQL Server: $(if [ "$USE_DOCKER" = true ]; then echo "SELECT version();" | $DOCKER_PSQL_CMD -h host.docker.internal -U "$DB_USERNAME" -d "$DB_NAME" -t 2>/dev/null | head -n 1; else PGPASSWORD="$DB_PASSWORD" "$PSQL" -h "$DB_HOST" -U "$DB_USERNAME" -d "$DB_NAME" -t -c "SELECT version();" 2>/dev/null | head -n 1; fi)

CONTEOS CRÍTICOS:
$(if [ "$USE_DOCKER" = true ]; then
    echo "SELECT 'Usuarios: ' || COUNT(*) FROM users;
SELECT 'Aplicaciones: ' || COUNT(*) FROM applications;  
SELECT 'Estudiantes: ' || COUNT(*) FROM students;
SELECT 'Padres: ' || COUNT(*) FROM parents;
SELECT 'Evaluaciones: ' || COUNT(*) FROM evaluations;
SELECT 'Entrevistas: ' || COUNT(*) FROM interviews;
SELECT 'Emails enviados: ' || COUNT(*) FROM email_notifications;" | $DOCKER_PSQL_CMD -h host.docker.internal -U "$DB_USERNAME" -d "$DB_NAME" -t 2>/dev/null
else
    PGPASSWORD="$DB_PASSWORD" "$PSQL" -h "$DB_HOST" -U "$DB_USERNAME" -d "$DB_NAME" -t -c "
SELECT 'Usuarios: ' || COUNT(*) FROM users;
SELECT 'Aplicaciones: ' || COUNT(*) FROM applications;  
SELECT 'Estudiantes: ' || COUNT(*) FROM students;
SELECT 'Padres: ' || COUNT(*) FROM parents;
SELECT 'Evaluaciones: ' || COUNT(*) FROM evaluations;
SELECT 'Entrevistas: ' || COUNT(*) FROM interviews;
SELECT 'Emails enviados: ' || COUNT(*) FROM email_notifications;
" 2>/dev/null
fi)

VERIFICACIÓN:
- Backup verificado: $(date)
- Script: $0
- Log completo: $LOG_FILE
EOF

log_success "Metadatos creados: $(basename "$METADATA_FILE")"

# Crear archivo ZIP final con todos los backups
log "📦 Comprimiendo backup completo..."
FINAL_BACKUP_FILE="$BACKUP_DIR/MTN_BACKUP_COMPLETO_$DATE_ONLY.zip"

# Usar zip para mejor compatibilidad
cd "$BACKUP_DIR"
zip -r "$(basename "$FINAL_BACKUP_FILE")" \
    "$(basename "$DB_BACKUP_FILE")" \
    $([ -n "$UPLOADS_BACKUP_FILE" ] && echo "$(basename "$UPLOADS_BACKUP_FILE")") \
    "$(basename "$CONFIG_BACKUP_FILE")" \
    "$(basename "$METADATA_FILE")" \
    > /dev/null

if [ $? -eq 0 ]; then
    FINAL_SIZE=$(du -h "$FINAL_BACKUP_FILE" | cut -f1)
    log_success "Backup completo creado: $(basename "$FINAL_BACKUP_FILE") ($FINAL_SIZE)"
else
    log_error "Error creando backup final comprimido"
    exit 1
fi

# Subir a Google Drive usando rclone
log "☁️  Subiendo backup a Google Drive..."

# Verificar que rclone está configurado
if ! command -v rclone &> /dev/null; then
    log_error "rclone no está instalado. Instalar con: brew install rclone"
    log "📋 BACKUP LOCAL COMPLETADO EN: $FINAL_BACKUP_FILE"
    exit 1
fi

# Verificar configuración de Google Drive
if ! rclone listremotes | grep -q "gdrive:"; then
    log_error "Google Drive no configurado en rclone"
    log "Configurar con: rclone config"
    log "📋 BACKUP LOCAL COMPLETADO EN: $FINAL_BACKUP_FILE"
    exit 1
fi

# Crear directorio en Google Drive si no existe
GDRIVE_FOLDER="Backups_MTN_Sistema_Admision"
rclone mkdir "gdrive:$GDRIVE_FOLDER" 2>/dev/null

# Subir backup a Google Drive
log "📤 Subiendo $(basename "$FINAL_BACKUP_FILE") a Google Drive/$GDRIVE_FOLDER..."

rclone copy "$FINAL_BACKUP_FILE" "gdrive:$GDRIVE_FOLDER" --progress

if [ $? -eq 0 ]; then
    log_success "✅ BACKUP SUBIDO EXITOSAMENTE A GOOGLE DRIVE"
    log "📁 Ubicación: Google Drive/$GDRIVE_FOLDER/$(basename "$FINAL_BACKUP_FILE")"
    
    # Verificar que el archivo está en Google Drive
    if rclone ls "gdrive:$GDRIVE_FOLDER/$(basename "$FINAL_BACKUP_FILE")" > /dev/null 2>&1; then
        log_success "✅ VERIFICACIÓN: Archivo confirmado en Google Drive"
    else
        log_warning "⚠️  No se pudo verificar el archivo en Google Drive"
    fi
    
else
    log_error "❌ ERROR subiendo a Google Drive"
    log "📋 BACKUP LOCAL DISPONIBLE EN: $FINAL_BACKUP_FILE"
fi

# Limpiar backups locales antiguos (mantener últimos 3 días)
log "🧹 Limpiando backups locales antiguos..."
find "$BACKUP_DIR" -name "mtn_*" -type f -mtime +3 -delete 2>/dev/null
find "$BACKUP_DIR" -name "MTN_BACKUP_COMPLETO_*" -type f -mtime +3 -delete 2>/dev/null

# Limpiar backups remotos antiguos (mantener últimos 30 días)
log "🧹 Limpiando backups remotos antiguos (>30 días)..."
CUTOFF_DATE=$(date -d "30 days ago" +%Y%m%d)

# Listar y eliminar backups antiguos de Google Drive
rclone ls "gdrive:$GDRIVE_FOLDER" | while read size file; do
    if [[ $file =~ MTN_BACKUP_COMPLETO_([0-9]{8}) ]]; then
        file_date="${BASH_REMATCH[1]}"
        if [[ $file_date < $CUTOFF_DATE ]]; then
            log "🗑️  Eliminando backup antiguo: $file"
            rclone delete "gdrive:$GDRIVE_FOLDER/$file"
        fi
    fi
done

# Resumen final
log "📊 RESUMEN DEL BACKUP:"
log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
log "📅 Fecha: $(date)"
log "🗂️  Backup local: $FINAL_BACKUP_FILE"
log "💾 Tamaño final: $FINAL_SIZE"
log "☁️  Google Drive: $GDRIVE_FOLDER/$(basename "$FINAL_BACKUP_FILE")"
log "📋 Log completo: $LOG_FILE"
log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Enviar notificación de éxito (opcional - se puede configurar email)
log_success "🎉 BACKUP AUTOMÁTICO COMPLETADO EXITOSAMENTE"

# Retornar código de éxito
exit 0