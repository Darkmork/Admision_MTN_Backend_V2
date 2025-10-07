#!/bin/bash
# ============================================================
# RESTORE SCRIPT
# Restauración de PostgreSQL con verificación automática
# ============================================================

set -euo pipefail

# --- Load Configuration ---
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/../config/backup.conf"

if [[ ! -f "$CONFIG_FILE" ]]; then
    echo "ERROR: Configuration file not found: $CONFIG_FILE"
    exit 1
fi

source "$CONFIG_FILE"

# --- Variables ---
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="${LOG_DIR}/restore_${TIMESTAMP}.log"
METRICS_FILE="${METRICS_DIR}/restore_${TIMESTAMP}.json"
RESTORE_TEMP_DIR="${TEMP_DIR}/restore_${TIMESTAMP}"

# --- Parse Arguments ---
BACKUP_FILE=""
TARGET_DB="$STAGING_DB_NAME"
RESTORE_TYPE="full"  # full, differential, point-in-time
SKIP_VERIFICATION=false
DROP_EXISTING=false

usage() {
    cat <<EOF
Usage: $0 [OPTIONS]

OPTIONS:
    -f, --file PATH         Path to encrypted backup file (.gpg)
    -t, --target DB         Target database name (default: $STAGING_DB_NAME)
    -r, --type TYPE         Restore type: full|differential|point-in-time (default: full)
    -s, --skip-verify       Skip post-restore verification
    -d, --drop              Drop existing database before restore
    -h, --help              Show this help message

EXAMPLES:
    # Restore latest full backup to staging
    $0 -f backups/admision_mtn_full_*.sql.gz.gpg

    # Restore to production (dangerous!)
    $0 -f backups/admision_mtn_full_*.sql.gz.gpg -t "Admisión_MTN_DB" -d

    # Restore with differential backups
    $0 -f backups/admision_mtn_full_*.sql.gz.gpg -r differential

EOF
    exit 1
}

# Parse command-line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -f|--file)
            BACKUP_FILE="$2"
            shift 2
            ;;
        -t|--target)
            TARGET_DB="$2"
            shift 2
            ;;
        -r|--type)
            RESTORE_TYPE="$2"
            shift 2
            ;;
        -s|--skip-verify)
            SKIP_VERIFICATION=true
            shift
            ;;
        -d|--drop)
            DROP_EXISTING=true
            shift
            ;;
        -h|--help)
            usage
            ;;
        *)
            echo "ERROR: Unknown option: $1"
            usage
            ;;
    esac
done

# Validate arguments
if [[ -z "$BACKUP_FILE" ]]; then
    echo "ERROR: Backup file is required. Use -f option."
    usage
fi

if [[ ! -f "$BACKUP_FILE" ]]; then
    echo "ERROR: Backup file not found: $BACKUP_FILE"
    exit 1
fi

# Create necessary directories
mkdir -p "$RESTORE_TEMP_DIR" "$LOG_DIR" "$METRICS_DIR"

# --- Logging Functions ---
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG_FILE"
}

log_error() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] ERROR: $*" | tee -a "$LOG_FILE" >&2
}

log_metric() {
    local metric_name=$1
    local metric_value=$2
    echo "  \"$metric_name\": \"$metric_value\"," >> "$METRICS_FILE"
}

# --- Cleanup Function ---
cleanup() {
    log "Cleaning up temporary files..."
    rm -rf "$RESTORE_TEMP_DIR"
}

trap cleanup EXIT

# --- Start Metrics ---
START_TIME=$(date +%s)
cat > "$METRICS_FILE" <<EOF
{
  "restore_type": "$RESTORE_TYPE",
  "timestamp": "$TIMESTAMP",
  "target_database": "$TARGET_DB",
  "source_backup": "$(basename "$BACKUP_FILE")",
EOF

log "========================================="
log "RESTORE STARTED"
log "========================================="
log "Source: $BACKUP_FILE"
log "Target database: $TARGET_DB"
log "Restore type: $RESTORE_TYPE"

# --- Safety Check ---
if [[ "$TARGET_DB" == "$DB_NAME" ]]; then
    log "⚠️  WARNING: You are restoring to PRODUCTION database!"
    log "   Target: $TARGET_DB"

    if [[ "$DROP_EXISTING" == "true" ]]; then
        log "   DROP_EXISTING is enabled - this will DELETE all production data!"
        read -p "   Type 'CONFIRM DELETE PRODUCTION' to proceed: " confirm

        if [[ "$confirm" != "CONFIRM DELETE PRODUCTION" ]]; then
            log_error "Restore cancelled by user"
            exit 1
        fi
    fi
fi

# --- Step 1: Decrypt Backup ---
log "Step 1/7: Decrypting backup..."
DECRYPT_START=$(date +%s)

DECRYPTED_FILE="${RESTORE_TEMP_DIR}/$(basename "$BACKUP_FILE" .gpg)"

if [[ ! -f "$ENCRYPTION_KEY_FILE" ]]; then
    log_error "Encryption key not found: $ENCRYPTION_KEY_FILE"
    exit 1
fi

if openssl enc -d -aes-256-cbc -pbkdf2 -iter 100000 \
    -in "$BACKUP_FILE" \
    -out "$DECRYPTED_FILE" \
    -pass file:"$ENCRYPTION_KEY_FILE"; then

    DECRYPT_END=$(date +%s)
    log "✓ Decryption completed"
    log "  Duration: $((DECRYPT_END - DECRYPT_START))s"
    log_metric "decrypt_duration_seconds" "$((DECRYPT_END - DECRYPT_START))"
else
    log_error "Decryption failed"
    exit 1
fi

# --- Step 2: Decompress ---
log "Step 2/7: Decompressing backup..."
DECOMPRESS_START=$(date +%s)

SQL_FILE="${RESTORE_TEMP_DIR}/$(basename "$DECRYPTED_FILE" .gz)"

if gunzip -c "$DECRYPTED_FILE" > "$SQL_FILE"; then
    DECOMPRESS_END=$(date +%s)
    DECOMPRESSED_SIZE=$(du -h "$SQL_FILE" | cut -f1)

    log "✓ Decompression completed"
    log "  Size: $DECOMPRESSED_SIZE"
    log "  Duration: $((DECOMPRESS_END - DECOMPRESS_START))s"

    log_metric "decompressed_size_bytes" "$(stat -f%z "$SQL_FILE")"
    log_metric "decompress_duration_seconds" "$((DECOMPRESS_END - DECOMPRESS_START))"
else
    log_error "Decompression failed"
    exit 1
fi

# --- Step 3: Verify SQL File ---
log "Step 3/7: Verifying SQL file integrity..."
if head -n 1 "$SQL_FILE" | grep -q "PostgreSQL database dump"; then
    log "✓ SQL file verified successfully"
    log_metric "sql_verification" "passed"
else
    log_error "SQL file verification failed - not a valid PostgreSQL dump"
    log_metric "sql_verification" "failed"
    exit 1
fi

# --- Step 4: Database Preparation ---
log "Step 4/7: Preparing target database..."
PREP_START=$(date +%s)

export PGPASSWORD="$DB_PASSWORD"

# Check if target database exists
DB_EXISTS=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -lqt | cut -d \| -f 1 | grep -w "$TARGET_DB" | wc -l)

if [[ $DB_EXISTS -gt 0 ]]; then
    log "  Target database exists: $TARGET_DB"

    if [[ "$DROP_EXISTING" == "true" ]]; then
        log "  Dropping existing database..."

        # Terminate all connections
        PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c \
            "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$TARGET_DB' AND pid <> pg_backend_pid();" \
            2>> "$LOG_FILE" || true

        # Drop database
        PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c \
            "DROP DATABASE IF EXISTS \"$TARGET_DB\";" 2>> "$LOG_FILE"

        log "  ✓ Database dropped"
    else
        log_error "Target database exists. Use --drop to overwrite."
        exit 1
    fi
fi

# Create database
log "  Creating database: $TARGET_DB"
PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c \
    "CREATE DATABASE \"$TARGET_DB\";" 2>> "$LOG_FILE"

PREP_END=$(date +%s)
log "✓ Database preparation completed"
log "  Duration: $((PREP_END - PREP_START))s"
log_metric "preparation_duration_seconds" "$((PREP_END - PREP_START))"

# --- Step 5: Restore Data ---
log "Step 5/7: Restoring data to database..."
RESTORE_START=$(date +%s)

if PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$TARGET_DB" \
    -f "$SQL_FILE" 2>> "$LOG_FILE"; then

    RESTORE_END=$(date +%s)
    RESTORE_DURATION=$((RESTORE_END - RESTORE_START))

    log "✓ Data restoration completed"
    log "  Duration: ${RESTORE_DURATION}s"
    log_metric "restore_duration_seconds" "$RESTORE_DURATION"
else
    log_error "Data restoration failed - check log for details"
    log_metric "status" "failed"
    log_metric "error" "restore_failed"
    exit 1
fi

# --- Step 6: Apply Differential Backups (if applicable) ---
if [[ "$RESTORE_TYPE" == "differential" ]]; then
    log "Step 6/7: Applying differential backups..."

    # Find all differential backups newer than the full backup
    BACKUP_TIMESTAMP=$(basename "$BACKUP_FILE" | grep -oE '[0-9]{8}_[0-9]{6}')

    DIFF_BACKUPS=$(find "$BACKUP_DIR" -name "${BACKUP_PREFIX}_diff_*.sql.gz.gpg" -type f -newer "$BACKUP_FILE" | sort)

    if [[ -n "$DIFF_BACKUPS" ]]; then
        DIFF_COUNT=$(echo "$DIFF_BACKUPS" | wc -l)
        log "  Found $DIFF_COUNT differential backups to apply"

        DIFF_NUM=0
        while IFS= read -r DIFF_FILE; do
            DIFF_NUM=$((DIFF_NUM + 1))
            log "  Applying differential backup $DIFF_NUM/$DIFF_COUNT: $(basename "$DIFF_FILE")"

            # Decrypt, decompress, and apply
            DIFF_DECRYPTED="${RESTORE_TEMP_DIR}/diff_${DIFF_NUM}.sql.gz"
            DIFF_SQL="${RESTORE_TEMP_DIR}/diff_${DIFF_NUM}.sql"

            openssl enc -d -aes-256-cbc -pbkdf2 -iter 100000 \
                -in "$DIFF_FILE" -out "$DIFF_DECRYPTED" \
                -pass file:"$ENCRYPTION_KEY_FILE" 2>> "$LOG_FILE"

            gunzip -c "$DIFF_DECRYPTED" > "$DIFF_SQL"

            PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$TARGET_DB" \
                -f "$DIFF_SQL" 2>> "$LOG_FILE"

            log "  ✓ Applied differential backup $DIFF_NUM"
        done <<< "$DIFF_BACKUPS"

        log "✓ All differential backups applied"
        log_metric "differential_backups_applied" "$DIFF_COUNT"
    else
        log "  No differential backups found to apply"
        log_metric "differential_backups_applied" "0"
    fi
else
    log "Step 6/7: Skipping differential backups (restore type: $RESTORE_TYPE)"
fi

# --- Step 7: Verification ---
if [[ "$SKIP_VERIFICATION" == "false" && "$ENABLE_POST_RESTORE_VERIFY" == "true" ]]; then
    log "Step 7/7: Verifying restored database..."
    VERIFY_START=$(date +%s)

    # Count tables
    TABLE_COUNT=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$TARGET_DB" -t -c \
        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" | xargs)

    # Count rows in key tables
    USERS_COUNT=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$TARGET_DB" -t -c \
        "SELECT COUNT(*) FROM users;" 2>/dev/null | xargs || echo "0")

    APPLICATIONS_COUNT=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$TARGET_DB" -t -c \
        "SELECT COUNT(*) FROM applications;" 2>/dev/null | xargs || echo "0")

    log "✓ Database verification completed"
    log "  Tables: $TABLE_COUNT"
    log "  Users: $USERS_COUNT"
    log "  Applications: $APPLICATIONS_COUNT"

    log_metric "verification_tables_count" "$TABLE_COUNT"
    log_metric "verification_users_count" "$USERS_COUNT"
    log_metric "verification_applications_count" "$APPLICATIONS_COUNT"

    VERIFY_END=$(date +%s)
    log_metric "verify_duration_seconds" "$((VERIFY_END - VERIFY_START))"

    # Check if verification looks reasonable
    if [[ $TABLE_COUNT -lt 10 ]]; then
        log "⚠️  WARNING: Low table count ($TABLE_COUNT) - verify restore manually"
    fi
else
    log "Step 7/7: Verification skipped"
fi

# --- Calculate Total Duration and RTO ---
END_TIME=$(date +%s)
TOTAL_DURATION=$((END_TIME - START_TIME))
RTO_MINUTES=$(echo "scale=2; $TOTAL_DURATION / 60" | bc)

log_metric "total_duration_seconds" "$TOTAL_DURATION"
log_metric "rto_minutes" "$RTO_MINUTES"
log_metric "status" "success"
echo "}" >> "$METRICS_FILE"

log "========================================="
log "RESTORE COMPLETED SUCCESSFULLY"
log "========================================="
log "Target database: $TARGET_DB"
log "Total duration: ${TOTAL_DURATION}s (${RTO_MINUTES} minutes)"
log "RTO Target: ${TARGET_RTO_HOURS} hours"

if [[ $(echo "$RTO_MINUTES < ($TARGET_RTO_HOURS * 60)" | bc) -eq 1 ]]; then
    log "✓ RTO target met (${RTO_MINUTES}min < ${TARGET_RTO_HOURS}h)"
else
    log "⚠️  RTO target exceeded (${RTO_MINUTES}min > ${TARGET_RTO_HOURS}h)"
fi

log "========================================="

# --- Send Success Notification ---
if [[ "$ENABLE_MONITORING" == "true" ]]; then
    "${SCRIPT_DIR}/send-notification.sh" "success" "Restore Completed" \
        "Database: $TARGET_DB\nDuration: ${TOTAL_DURATION}s (${RTO_MINUTES}min)\nRTO Target: ${TARGET_RTO_HOURS}h" \
        2>> "$LOG_FILE" || true
fi

exit 0
