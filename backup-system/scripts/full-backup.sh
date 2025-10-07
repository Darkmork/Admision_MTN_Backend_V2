#!/bin/bash
# ============================================================
# FULL BACKUP SCRIPT
# Backup completo diario de PostgreSQL con cifrado y upload a S3
# ============================================================

set -euo pipefail  # Exit on error, undefined vars, pipe failures

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
HOSTNAME=$(hostname -s)
BACKUP_NAME="${BACKUP_PREFIX}_full_${HOSTNAME}_${TIMESTAMP}"
BACKUP_FILE="${TEMP_DIR}/${BACKUP_NAME}.sql"
COMPRESSED_FILE="${BACKUP_FILE}.gz"
ENCRYPTED_FILE="${COMPRESSED_FILE}.gpg"
LOG_FILE="${LOG_DIR}/full-backup_${TIMESTAMP}.log"
METRICS_FILE="${METRICS_DIR}/full-backup_${TIMESTAMP}.json"

# Create necessary directories
mkdir -p "$TEMP_DIR" "$LOG_DIR" "$METRICS_DIR" "$BACKUP_DIR"

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
    rm -f "$BACKUP_FILE" "$COMPRESSED_FILE"
    # Keep encrypted file for local backup
}

trap cleanup EXIT

# --- Start Metrics ---
START_TIME=$(date +%s)
cat > "$METRICS_FILE" <<EOF
{
  "backup_type": "full",
  "timestamp": "$TIMESTAMP",
  "hostname": "$HOSTNAME",
EOF

log "========================================="
log "FULL BACKUP STARTED"
log "========================================="
log "Database: $DB_NAME"
log "Backup name: $BACKUP_NAME"

# --- Step 1: Database Dump ---
log "Step 1/6: Creating database dump..."
DUMP_START=$(date +%s)

export PGPASSWORD="$DB_PASSWORD"

# Use configured pg_dump if available, otherwise use system default
PG_DUMP_CMD="${PG_DUMP:-pg_dump}"

if $PG_DUMP_CMD -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
    --format=plain \
    --no-owner \
    --no-acl \
    --verbose \
    --file="$BACKUP_FILE" 2>> "$LOG_FILE"; then

    DUMP_END=$(date +%s)
    DUMP_DURATION=$((DUMP_END - DUMP_START))
    BACKUP_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)

    log "✓ Database dump completed successfully"
    log "  Size: $BACKUP_SIZE"
    log "  Duration: ${DUMP_DURATION}s"

    log_metric "dump_size_bytes" "$(stat -f%z "$BACKUP_FILE")"
    log_metric "dump_duration_seconds" "$DUMP_DURATION"
else
    log_error "Database dump failed"
    log_metric "status" "failed"
    log_metric "error" "dump_failed"
    exit 1
fi

# --- Step 2: Verification ---
if [[ "$ENABLE_POST_BACKUP_VERIFY" == "true" ]]; then
    log "Step 2/6: Verifying backup integrity..."
    VERIFY_START=$(date +%s)

    # Check if file is valid SQL (check first 10 lines for PostgreSQL dump header)
    if head -n 10 "$BACKUP_FILE" | grep -q -E "(PostgreSQL database dump|pg_dump|Database:|Dumped from)"; then
        log "✓ Backup file verified successfully"
        log_metric "verification" "passed"
    else
        log_error "Backup verification failed - invalid SQL dump"
        log_metric "verification" "failed"
        exit 1
    fi

    VERIFY_END=$(date +%s)
    log_metric "verify_duration_seconds" "$((VERIFY_END - VERIFY_START))"
else
    log "Step 2/6: Verification skipped (disabled in config)"
fi

# --- Step 3: Compression ---
log "Step 3/6: Compressing backup..."
COMPRESS_START=$(date +%s)

if gzip -${COMPRESSION_LEVEL} -c "$BACKUP_FILE" > "$COMPRESSED_FILE"; then
    COMPRESS_END=$(date +%s)
    COMPRESSED_SIZE=$(du -h "$COMPRESSED_FILE" | cut -f1)
    ORIGINAL_SIZE=$(stat -f%z "$BACKUP_FILE")
    COMPRESSED_SIZE_BYTES=$(stat -f%z "$COMPRESSED_FILE")
    COMPRESSION_RATIO=$(echo "scale=2; $COMPRESSED_SIZE_BYTES * 100 / $ORIGINAL_SIZE" | bc)

    log "✓ Compression completed"
    log "  Original: $(du -h "$BACKUP_FILE" | cut -f1)"
    log "  Compressed: $COMPRESSED_SIZE"
    log "  Ratio: ${COMPRESSION_RATIO}%"

    log_metric "compressed_size_bytes" "$COMPRESSED_SIZE_BYTES"
    log_metric "compression_ratio_percent" "$COMPRESSION_RATIO"
    log_metric "compress_duration_seconds" "$((COMPRESS_END - COMPRESS_START))"
else
    log_error "Compression failed"
    exit 1
fi

# --- Step 4: Encryption ---
if [[ "$ENABLE_ENCRYPTION" == "true" ]]; then
    log "Step 4/6: Encrypting backup..."
    ENCRYPT_START=$(date +%s)

    # Generate encryption key if it doesn't exist
    if [[ ! -f "$ENCRYPTION_KEY_FILE" ]]; then
        log "Generating new encryption key..."
        openssl rand -base64 32 > "$ENCRYPTION_KEY_FILE"
        chmod 600 "$ENCRYPTION_KEY_FILE"
    fi

    # Encrypt using OpenSSL (AES-256-CBC)
    if openssl enc -aes-256-cbc -salt -pbkdf2 -iter 100000 \
        -in "$COMPRESSED_FILE" \
        -out "$ENCRYPTED_FILE" \
        -pass file:"$ENCRYPTION_KEY_FILE"; then

        ENCRYPT_END=$(date +%s)
        ENCRYPTED_SIZE=$(du -h "$ENCRYPTED_FILE" | cut -f1)

        log "✓ Encryption completed"
        log "  Encrypted size: $ENCRYPTED_SIZE"
        log "  Algorithm: AES-256-CBC"

        log_metric "encrypted_size_bytes" "$(stat -f%z "$ENCRYPTED_FILE")"
        log_metric "encryption_algorithm" "AES-256-CBC"
        log_metric "encrypt_duration_seconds" "$((ENCRYPT_END - ENCRYPT_START))"

        FINAL_FILE="$ENCRYPTED_FILE"
    else
        log_error "Encryption failed"
        exit 1
    fi
else
    log "Step 4/6: Encryption skipped (disabled in config)"
    FINAL_FILE="$COMPRESSED_FILE"
fi

# --- Step 5: Upload to S3 ---
log "Step 5/6: Uploading to S3..."
UPLOAD_START=$(date +%s)

if [[ "$USE_MINIO" == "true" ]]; then
    # Using MinIO (local S3-compatible storage)
    log "Using MinIO endpoint: $MINIO_ENDPOINT"

    # Install mc (MinIO client) if not present
    if ! command -v mc &> /dev/null; then
        log "Installing MinIO client..."
        brew install minio/stable/mc 2>> "$LOG_FILE" || {
            log_error "Failed to install MinIO client"
            exit 1
        }
    fi

    # Configure MinIO alias
    mc alias set mtn "$MINIO_ENDPOINT" "$MINIO_ACCESS_KEY" "$MINIO_SECRET_KEY" 2>> "$LOG_FILE" || true

    # Create bucket if it doesn't exist
    mc mb "mtn/${MINIO_BUCKET}" 2>> "$LOG_FILE" || true

    # Upload file
    if mc cp "$FINAL_FILE" "mtn/${MINIO_BUCKET}/full/${BACKUP_NAME}.sql.gz.gpg" 2>> "$LOG_FILE"; then
        UPLOAD_END=$(date +%s)
        log "✓ Upload to MinIO completed"
        log_metric "upload_destination" "minio://${MINIO_BUCKET}/full/"
        log_metric "upload_duration_seconds" "$((UPLOAD_END - UPLOAD_START))"
    else
        log_error "Upload to MinIO failed"
        log_metric "upload_status" "failed"
        # Don't exit - local backup still exists
    fi
else
    # Using AWS S3
    log "Using AWS S3: s3://${S3_BUCKET}"

    if aws s3 cp "$FINAL_FILE" \
        "s3://${S3_BUCKET}/full/${BACKUP_NAME}.sql.gz.gpg" \
        --storage-class STANDARD_IA \
        --server-side-encryption AES256 \
        --profile "$AWS_PROFILE" 2>> "$LOG_FILE"; then

        UPLOAD_END=$(date +%s)
        log "✓ Upload to S3 completed"
        log_metric "upload_destination" "s3://${S3_BUCKET}/full/"
        log_metric "upload_duration_seconds" "$((UPLOAD_END - UPLOAD_START))"
    else
        log_error "Upload to S3 failed"
        log_metric "upload_status" "failed"
    fi
fi

# --- Step 6: Copy to Local Backup Dir ---
log "Step 6/6: Copying to local backup directory..."
cp "$FINAL_FILE" "${BACKUP_DIR}/${BACKUP_NAME}.sql.gz.gpg"
log "✓ Local backup saved: ${BACKUP_DIR}/${BACKUP_NAME}.sql.gz.gpg"

# --- Cleanup Old Backups ---
log "Cleaning up old backups (retention: ${FULL_BACKUP_RETENTION_DAYS} days)..."
find "$BACKUP_DIR" -name "${BACKUP_PREFIX}_full_*.sql.gz.gpg" -type f -mtime +${FULL_BACKUP_RETENTION_DAYS} -delete
log "✓ Old backups cleaned"

# --- Calculate Total Duration ---
END_TIME=$(date +%s)
TOTAL_DURATION=$((END_TIME - START_TIME))

log_metric "total_duration_seconds" "$TOTAL_DURATION"
log_metric "status" "success"
echo "}" >> "$METRICS_FILE"

log "========================================="
log "FULL BACKUP COMPLETED SUCCESSFULLY"
log "Total duration: ${TOTAL_DURATION}s"
log "Backup file: ${BACKUP_NAME}.sql.gz.gpg"
log "========================================="

# --- Send Success Notification ---
if [[ "$ENABLE_MONITORING" == "true" ]]; then
    "${SCRIPT_DIR}/send-notification.sh" "success" "Full Backup Completed" \
        "Database: $DB_NAME\nSize: $COMPRESSED_SIZE\nDuration: ${TOTAL_DURATION}s" \
        2>> "$LOG_FILE" || true
fi

exit 0
