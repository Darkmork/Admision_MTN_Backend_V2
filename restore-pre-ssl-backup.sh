#!/bin/bash
# ============================================================
# RESTORE PRE-SSL WORKING STATE
# Restaurar base de datos al estado previo a configuración SSL
# Fecha del backup: October 3, 2025
# ============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_FILE="$SCRIPT_DIR/backup-system/backups/admision_mtn_full_MacBook-Pro-de-Jorge-4_20251003_203417.sql.gz.gpg"
ENCRYPTION_KEY_FILE="$SCRIPT_DIR/backup-system/config/.encryption.key"
RESTORE_DIR="/tmp/mtn_restore_$(date +%s)"

# Database credentials
DB_HOST="localhost"
DB_PORT="5432"
DB_NAME="Admisión_MTN_DB"
DB_USER="admin"
DB_PASSWORD="admin123"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

log() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[✅ OK]${NC} $1"
}

log_error() {
    echo -e "${RED}[❌ ERROR]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[⚠️ WARN]${NC} $1"
}

echo -e "${BLUE}"
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║         RESTAURACIÓN AL ESTADO PRE-SSL (Oct 3, 2025)        ║"
echo "║                   Sistema de Admisión MTN                    ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

log "Esta operación restaurará la base de datos al estado funcional"
log "del 3 de octubre de 2025 (antes de la configuración SSL)"
echo ""
log_warning "ADVERTENCIA: Esto sobrescribirá la base de datos actual"
echo ""
read -p "¿Estás seguro de continuar? (escribir 'SI'): " confirmation

if [ "$confirmation" != "SI" ]; then
    log "Restauración cancelada por el usuario"
    exit 0
fi

# Create restore directory
mkdir -p "$RESTORE_DIR"

# Step 1: Decrypt backup
log "Step 1/6: Decrypting backup from October 3..."
DECRYPT_START=$(date +%s)

if [ ! -f "$BACKUP_FILE" ]; then
    log_error "Backup file not found: $BACKUP_FILE"
    exit 1
fi

if [ ! -f "$ENCRYPTION_KEY_FILE" ]; then
    log_error "Encryption key not found: $ENCRYPTION_KEY_FILE"
    exit 1
fi

DECRYPTED_FILE="$RESTORE_DIR/admision_mtn_full.sql.gz"

if openssl enc -d -aes-256-cbc -pbkdf2 -iter 100000 \
    -in "$BACKUP_FILE" \
    -out "$DECRYPTED_FILE" \
    -pass file:"$ENCRYPTION_KEY_FILE"; then

    DECRYPT_END=$(date +%s)
    log_success "Decryption completed ($((DECRYPT_END - DECRYPT_START))s)"
else
    log_error "Decryption failed"
    rm -rf "$RESTORE_DIR"
    exit 1
fi

# Step 2: Decompress
log "Step 2/6: Decompressing backup..."
DECOMPRESS_START=$(date +%s)

SQL_FILE="$RESTORE_DIR/admision_mtn_full.sql"

if gunzip -c "$DECRYPTED_FILE" > "$SQL_FILE"; then
    DECOMPRESS_END=$(date +%s)
    DECOMPRESSED_SIZE=$(du -h "$SQL_FILE" | awk '{print $1}')
    log_success "Decompression completed (Size: $DECOMPRESSED_SIZE, Duration: $((DECOMPRESS_END - DECOMPRESS_START))s)"
else
    log_error "Decompression failed"
    rm -rf "$RESTORE_DIR"
    exit 1
fi

# Step 3: Verify SQL file
log "Step 3/6: Verifying SQL file integrity..."
if head -n 1 "$SQL_FILE" | grep -q "PostgreSQL database dump"; then
    log_success "SQL file verified successfully"
else
    log_error "SQL file verification failed - not a valid PostgreSQL dump"
    rm -rf "$RESTORE_DIR"
    exit 1
fi

# Step 4: Backup current database
log "Step 4/6: Creating backup of current database..."
CURRENT_BACKUP="$RESTORE_DIR/current_backup_$(date +%Y%m%d_%H%M%S).sql"

export PGPASSWORD="$DB_PASSWORD"
if pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" > "$CURRENT_BACKUP" 2>/dev/null; then
    log_success "Current database backed up to: $CURRENT_BACKUP"
else
    log_warning "Could not backup current database (may not exist or be empty)"
fi

# Step 5: Drop and recreate database
log "Step 5/6: Preparing database..."

# Terminate all connections
log "  Terminating active connections..."
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c \
    "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$DB_NAME' AND pid <> pg_backend_pid();" \
    2>/dev/null || true

# Drop database
log "  Dropping existing database..."
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c \
    "DROP DATABASE IF EXISTS \"$DB_NAME\";" 2>/dev/null

# Create database
log "  Creating fresh database..."
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c \
    "CREATE DATABASE \"$DB_NAME\";" 2>/dev/null

log_success "Database prepared"

# Step 6: Restore data
log "Step 6/6: Restoring data from October 3 backup..."
RESTORE_START=$(date +%s)

if psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
    -f "$SQL_FILE" 2>/dev/null; then

    RESTORE_END=$(date +%s)
    RESTORE_DURATION=$((RESTORE_END - RESTORE_START))

    log_success "Data restoration completed (Duration: ${RESTORE_DURATION}s)"
else
    log_error "Data restoration failed"
    log_error "You can restore the previous state from: $CURRENT_BACKUP"
    exit 1
fi

# Verification
log "Verifying restored database..."

TABLE_COUNT=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c \
    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" 2>/dev/null | xargs)

USERS_COUNT=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c \
    "SELECT COUNT(*) FROM users;" 2>/dev/null | xargs || echo "0")

APPLICATIONS_COUNT=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c \
    "SELECT COUNT(*) FROM applications;" 2>/dev/null | xargs || echo "0")

echo ""
log_success "Database verification completed"
log "  Tables: $TABLE_COUNT"
log "  Users: $USERS_COUNT"
log "  Applications: $APPLICATIONS_COUNT"

echo ""
echo -e "${GREEN}"
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║              ✅ RESTORATION COMPLETED SUCCESSFULLY           ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

log "Database restored to October 3, 2025 state (before SSL work)"
log "Backup of previous state saved at: $CURRENT_BACKUP"
log ""
log "Next steps:"
log "  1. Test login: jorge.gangale@mtn.cl / admin123"
log "  2. Start services: ./start-microservices-gateway.sh"
log "  3. Access frontend: http://localhost:5173"

# Keep temp directory with backups
log ""
log "Temporary files location: $RESTORE_DIR"
log "(Contains current database backup in case you need to rollback)"

exit 0
