#!/bin/bash

# ===========================================
# VERIFICACIÓN Y RESTAURACIÓN DE BACKUPS
# Sistema de Admisión MTN
# ===========================================

# Colores para output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_DIR="/tmp/mtn_backups"

# Función de logging
log() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[✅ OK]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[⚠️ WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[❌ ERROR]${NC} $1"
}

show_menu() {
    echo -e "${BLUE}"
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║            VERIFICACIÓN Y RESTAURACIÓN DE BACKUPS            ║"
    echo "║                   Sistema de Admisión MTN                    ║"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    echo "Selecciona una opción:"
    echo
    echo "1) 📊 Verificar estado de backups"
    echo "2) 📋 Listar backups disponibles"
    echo "3) ⬇️  Descargar backup desde Google Drive"
    echo "4) 🔄 Restaurar base de datos desde backup"
    echo "5) 📁 Restaurar archivos desde backup"
    echo "6) 🧪 Verificar integridad de backup"
    echo "7) 🚨 Restauración completa (CUIDADO)"
    echo "8) ❌ Salir"
    echo
    read -p "Ingresa tu opción (1-8): " choice
}

verify_backup_status() {
    log "🔍 Verificando estado del sistema de backups..."
    
    # Verificar si rclone está configurado
    if command -v rclone &> /dev/null; then
        log_success "rclone instalado"
        
        if rclone listremotes | grep -q "gdrive:"; then
            log_success "Google Drive configurado"
        else
            log_warning "Google Drive NO configurado"
        fi
    else
        log_warning "rclone NO instalado"
    fi
    
    # Verificar cron jobs
    if crontab -l 2>/dev/null | grep -q "backup-to-gdrive.sh"; then
        log_success "Backup automático programado"
        echo "Horario: $(crontab -l | grep backup-to-gdrive.sh | cut -d' ' -f1-5)"
    else
        log_warning "Backup automático NO programado"
    fi
    
    # Verificar backups locales
    if [ -d "$BACKUP_DIR" ]; then
        local_count=$(find "$BACKUP_DIR" -name "MTN_BACKUP_COMPLETO_*.zip" 2>/dev/null | wc -l)
        log_success "Directorio de backups existe"
        log "Backups locales encontrados: $local_count"
        
        if [ $local_count -gt 0 ]; then
            echo "Últimos backups locales:"
            ls -la "$BACKUP_DIR"/MTN_BACKUP_COMPLETO_*.zip 2>/dev/null | tail -5
        fi
    else
        log_warning "Directorio de backups no existe: $BACKUP_DIR"
    fi
    
    # Verificar backups remotos
    if rclone listremotes | grep -q "gdrive:"; then
        log "Verificando backups en Google Drive..."
        remote_count=$(rclone ls "gdrive:Backups_MTN_Sistema_Admision" 2>/dev/null | grep "MTN_BACKUP_COMPLETO" | wc -l)
        
        if [ $remote_count -gt 0 ]; then
            log_success "Backups remotos encontrados: $remote_count"
            echo "Últimos backups remotos:"
            rclone ls "gdrive:Backups_MTN_Sistema_Admision" 2>/dev/null | grep "MTN_BACKUP_COMPLETO" | tail -5
        else
            log_warning "No se encontraron backups remotos"
        fi
    fi
    
    # Verificar logs
    if [ -f "$BACKUP_DIR/logs/cron.log" ]; then
        log_success "Log de cron existe"
        echo "Últimas líneas del log:"
        tail -5 "$BACKUP_DIR/logs/cron.log" 2>/dev/null
    else
        log_warning "Log de cron no encontrado"
    fi
}

list_available_backups() {
    log "📋 Listando backups disponibles..."
    
    echo
    echo -e "${YELLOW}BACKUPS LOCALES:${NC}"
    if [ -d "$BACKUP_DIR" ]; then
        find "$BACKUP_DIR" -name "MTN_BACKUP_COMPLETO_*.zip" -printf "%T+ %p\n" 2>/dev/null | sort -r
        
        local_count=$(find "$BACKUP_DIR" -name "MTN_BACKUP_COMPLETO_*.zip" 2>/dev/null | wc -l)
        echo "Total local: $local_count backups"
    else
        echo "No hay backups locales"
    fi
    
    echo
    echo -e "${YELLOW}BACKUPS EN GOOGLE DRIVE:${NC}"
    if rclone listremotes | grep -q "gdrive:"; then
        rclone lsl "gdrive:Backups_MTN_Sistema_Admision" 2>/dev/null | grep "MTN_BACKUP_COMPLETO" | sort -r
        
        remote_count=$(rclone ls "gdrive:Backups_MTN_Sistema_Admision" 2>/dev/null | grep "MTN_BACKUP_COMPLETO" | wc -l)
        echo "Total remoto: $remote_count backups"
    else
        echo "Google Drive no configurado"
    fi
}

download_from_gdrive() {
    log "⬇️ Descargando backup desde Google Drive..."
    
    if ! rclone listremotes | grep -q "gdrive:"; then
        log_error "Google Drive no está configurado"
        return 1
    fi
    
    echo "Backups disponibles en Google Drive:"
    rclone ls "gdrive:Backups_MTN_Sistema_Admision" 2>/dev/null | grep "MTN_BACKUP_COMPLETO" | nl
    
    echo
    read -p "Ingresa el número del backup a descargar (o 0 para cancelar): " backup_num
    
    if [ "$backup_num" -eq 0 ]; then
        log "Descarga cancelada"
        return 0
    fi
    
    backup_file=$(rclone ls "gdrive:Backups_MTN_Sistema_Admision" 2>/dev/null | grep "MTN_BACKUP_COMPLETO" | sed -n "${backup_num}p" | awk '{$1=""; print $0}' | sed 's/^ *//')
    
    if [ -z "$backup_file" ]; then
        log_error "Backup seleccionado no válido"
        return 1
    fi
    
    log "Descargando: $backup_file"
    mkdir -p "$BACKUP_DIR"
    
    rclone copy "gdrive:Backups_MTN_Sistema_Admision/$backup_file" "$BACKUP_DIR" --progress
    
    if [ $? -eq 0 ]; then
        log_success "Backup descargado: $BACKUP_DIR/$backup_file"
    else
        log_error "Error descargando backup"
        return 1
    fi
}

verify_backup_integrity() {
    log "🧪 Verificando integridad de backup..."
    
    echo "Backups locales disponibles:"
    find "$BACKUP_DIR" -name "MTN_BACKUP_COMPLETO_*.zip" 2>/dev/null | nl
    
    echo
    read -p "Ingresa el número del backup a verificar: " backup_num
    
    backup_file=$(find "$BACKUP_DIR" -name "MTN_BACKUP_COMPLETO_*.zip" 2>/dev/null | sed -n "${backup_num}p")
    
    if [ -z "$backup_file" ]; then
        log_error "Backup seleccionado no válido"
        return 1
    fi
    
    log "Verificando: $(basename "$backup_file")"
    
    # Verificar integridad del ZIP
    if unzip -t "$backup_file" > /dev/null 2>&1; then
        log_success "Archivo ZIP íntegro"
    else
        log_error "Archivo ZIP corrupto"
        return 1
    fi
    
    # Extraer temporalmente y verificar contenido
    TEMP_DIR="/tmp/mtn_verify_$$"
    mkdir -p "$TEMP_DIR"
    
    cd "$TEMP_DIR"
    unzip -q "$backup_file"
    
    # Verificar archivos esperados
    expected_files=("mtn_database_*.sql" "mtn_uploads_*.tar.gz" "mtn_config_*.tar.gz" "mtn_backup_metadata_*.txt")
    
    for pattern in "${expected_files[@]}"; do
        if ls $pattern 1> /dev/null 2>&1; then
            log_success "Encontrado: $pattern"
        else
            log_warning "No encontrado: $pattern"
        fi
    done
    
    # Verificar metadatos
    metadata_file=$(ls mtn_backup_metadata_*.txt 2>/dev/null | head -1)
    if [ -n "$metadata_file" ]; then
        log "Información del backup:"
        echo "────────────────────────────────────────"
        head -20 "$metadata_file"
        echo "────────────────────────────────────────"
    fi
    
    # Limpiar archivos temporales
    cd - > /dev/null
    rm -rf "$TEMP_DIR"
    
    log_success "Verificación completada"
}

restore_database() {
    log "🔄 Iniciando restauración de base de datos..."
    
    echo -e "${RED}⚠️  ADVERTENCIA: Esto sobrescribirá la base de datos actual${NC}"
    echo "Asegúrate de hacer un backup actual antes de continuar"
    echo
    read -p "¿Estás seguro de continuar? (escribir 'SI ESTOY SEGURO'): " confirmation
    
    if [ "$confirmation" != "SI ESTOY SEGURO" ]; then
        log "Restauración cancelada por el usuario"
        return 0
    fi
    
    # Listar backups disponibles
    echo "Backups locales disponibles:"
    find "$BACKUP_DIR" -name "MTN_BACKUP_COMPLETO_*.zip" 2>/dev/null | nl
    
    echo
    read -p "Ingresa el número del backup a restaurar: " backup_num
    
    backup_file=$(find "$BACKUP_DIR" -name "MTN_BACKUP_COMPLETO_*.zip" 2>/dev/null | sed -n "${backup_num}p")
    
    if [ -z "$backup_file" ]; then
        log_error "Backup seleccionado no válido"
        return 1
    fi
    
    # Extraer backup
    TEMP_DIR="/tmp/mtn_restore_$$"
    mkdir -p "$TEMP_DIR"
    cd "$TEMP_DIR"
    unzip -q "$backup_file"
    
    # Buscar archivo SQL
    sql_file=$(ls mtn_database_*.sql 2>/dev/null | head -1)
    
    if [ -z "$sql_file" ]; then
        log_error "Archivo de base de datos no encontrado en backup"
        rm -rf "$TEMP_DIR"
        return 1
    fi
    
    log "Archivo de BD encontrado: $sql_file"
    
    # Cargar variables de entorno
    if [ -f "$SCRIPT_DIR/.env" ]; then
        set -a
        source "$SCRIPT_DIR/.env"
        set +a
    fi
    
    # Hacer backup actual antes de restaurar
    current_backup="$TEMP_DIR/current_backup_$(date +%Y%m%d_%H%M%S).sql"
    log "Haciendo backup de la BD actual..."
    PGPASSWORD="$DB_PASSWORD" pg_dump -h "$DB_HOST" -U "$DB_USERNAME" -d "$DB_NAME" > "$current_backup"
    
    if [ $? -eq 0 ]; then
        log_success "Backup actual guardado en: $current_backup"
    else
        log_error "Error haciendo backup actual"
        rm -rf "$TEMP_DIR"
        return 1
    fi
    
    # Restaurar base de datos
    log "Restaurando base de datos..."
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -U "$DB_USERNAME" -d "$DB_NAME" < "$sql_file"
    
    if [ $? -eq 0 ]; then
        log_success "✅ Base de datos restaurada exitosamente"
        log "Backup actual guardado en: $current_backup"
    else
        log_error "Error restaurando base de datos"
        log "Puedes restaurar el backup actual desde: $current_backup"
    fi
    
    cd - > /dev/null
    # No eliminar TEMP_DIR para mantener backups
    
    log "Archivos temporales en: $TEMP_DIR"
}

# Menú principal
while true; do
    show_menu
    
    case $choice in
        1)
            verify_backup_status
            echo
            read -p "Presiona Enter para continuar..."
            ;;
        2)
            list_available_backups
            echo
            read -p "Presiona Enter para continuar..."
            ;;
        3)
            download_from_gdrive
            echo
            read -p "Presiona Enter para continuar..."
            ;;
        4)
            restore_database
            echo
            read -p "Presiona Enter para continuar..."
            ;;
        5)
            log "🚧 Funcionalidad de restauración de archivos en desarrollo"
            echo
            read -p "Presiona Enter para continuar..."
            ;;
        6)
            verify_backup_integrity
            echo
            read -p "Presiona Enter para continuar..."
            ;;
        7)
            log "🚧 Funcionalidad de restauración completa en desarrollo"
            echo
            read -p "Presiona Enter para continuar..."
            ;;
        8)
            log "👋 Saliendo..."
            exit 0
            ;;
        *)
            log_error "Opción no válida"
            echo
            read -p "Presiona Enter para continuar..."
            ;;
    esac
    
    clear
done