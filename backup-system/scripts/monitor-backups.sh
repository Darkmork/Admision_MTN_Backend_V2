#!/bin/bash
# ============================================================
# BACKUP MONITORING SCRIPT
# Monitoreo de backups, RPO/RTO y generación de reportes
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
REPORT_FILE="${LOG_DIR}/backup-report_${TIMESTAMP}.txt"
JSON_REPORT="${METRICS_DIR}/backup-status_${TIMESTAMP}.json"

# Colors for terminal output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Create directories
mkdir -p "$LOG_DIR" "$METRICS_DIR"

# --- Functions ---
log() {
    echo -e "$*" | tee -a "$REPORT_FILE"
}

log_json() {
    echo "$*" >> "$JSON_REPORT"
}

check_backup_freshness() {
    local backup_type=$1
    local max_age_hours=$2

    log "\n${BLUE}=== Checking $backup_type Backups ===${NC}"

    if [[ "$backup_type" == "full" ]]; then
        PATTERN="${BACKUP_PREFIX}_full_*.sql.gz.gpg"
    else
        PATTERN="${BACKUP_PREFIX}_diff_*.sql.gz.gpg"
    fi

    # Find most recent backup
    LATEST_BACKUP=$(find "$BACKUP_DIR" -name "$PATTERN" -type f | sort -r | head -n 1)

    if [[ -z "$LATEST_BACKUP" ]]; then
        log "${RED}✗ No $backup_type backups found!${NC}"
        echo "\"${backup_type}_backup_status\": \"MISSING\"," >> "$JSON_REPORT"
        echo "\"${backup_type}_backup_age_hours\": null," >> "$JSON_REPORT"
        return 1
    fi

    # Calculate age
    BACKUP_TIME=$(stat -f%m "$LATEST_BACKUP")
    CURRENT_TIME=$(date +%s)
    AGE_SECONDS=$((CURRENT_TIME - BACKUP_TIME))
    AGE_HOURS=$(echo "scale=2; $AGE_SECONDS / 3600" | bc)
    AGE_MINUTES=$(echo "scale=0; $AGE_SECONDS / 60" | bc)

    log "Latest backup: $(basename "$LATEST_BACKUP")"
    log "Age: ${AGE_HOURS} hours (${AGE_MINUTES} minutes)"

    # Check freshness
    if (( $(echo "$AGE_HOURS > $max_age_hours" | bc -l) )); then
        log "${RED}✗ Backup is too old! Maximum: ${max_age_hours}h${NC}"
        echo "\"${backup_type}_backup_status\": \"STALE\"," >> "$JSON_REPORT"
        echo "\"${backup_type}_backup_age_hours\": $AGE_HOURS," >> "$JSON_REPORT"
        return 1
    else
        log "${GREEN}✓ Backup is fresh (within ${max_age_hours}h window)${NC}"
        echo "\"${backup_type}_backup_status\": \"FRESH\"," >> "$JSON_REPORT"
        echo "\"${backup_type}_backup_age_hours\": $AGE_HOURS," >> "$JSON_REPORT"
    fi

    # Size information
    BACKUP_SIZE=$(du -h "$LATEST_BACKUP" | cut -f1)
    log "Size: $BACKUP_SIZE"
    echo "\"${backup_type}_backup_size\": \"$BACKUP_SIZE\"," >> "$JSON_REPORT"

    return 0
}

calculate_rpo() {
    log "\n${BLUE}=== RPO (Recovery Point Objective) Analysis ===${NC}"

    # Find most recent differential backup
    LATEST_DIFF=$(find "$BACKUP_DIR" -name "${BACKUP_PREFIX}_diff_*.sql.gz.gpg" -type f | sort -r | head -n 1)

    if [[ -z "$LATEST_DIFF" ]]; then
        log "${YELLOW}⚠ No differential backups found - using full backup${NC}"
        LATEST_BACKUP=$(find "$BACKUP_DIR" -name "${BACKUP_PREFIX}_full_*.sql.gz.gpg" -type f | sort -r | head -n 1)
    else
        LATEST_BACKUP="$LATEST_DIFF"
    fi

    if [[ -z "$LATEST_BACKUP" ]]; then
        log "${RED}✗ No backups found - cannot calculate RPO!${NC}"
        echo "\"rpo_status\": \"CRITICAL\"," >> "$JSON_REPORT"
        echo "\"rpo_actual_hours\": null," >> "$JSON_REPORT"
        echo "\"rpo_target_hours\": $TARGET_RPO_HOURS," >> "$JSON_REPORT"
        return 1
    fi

    # Calculate actual RPO (time since last backup)
    BACKUP_TIME=$(stat -f%m "$LATEST_BACKUP")
    CURRENT_TIME=$(date +%s)
    RPO_SECONDS=$((CURRENT_TIME - BACKUP_TIME))
    RPO_HOURS=$(echo "scale=2; $RPO_SECONDS / 3600" | bc)

    log "Last backup: $(date -r "$BACKUP_TIME" +'%Y-%m-%d %H:%M:%S')"
    log "Current RPO: ${RPO_HOURS} hours"
    log "Target RPO: ${TARGET_RPO_HOURS} hours"

    echo "\"rpo_actual_hours\": $RPO_HOURS," >> "$JSON_REPORT"
    echo "\"rpo_target_hours\": $TARGET_RPO_HOURS," >> "$JSON_REPORT"

    # Compare with target
    if (( $(echo "$RPO_HOURS > $TARGET_RPO_HOURS" | bc -l) )); then
        log "${RED}✗ RPO EXCEEDED! Current: ${RPO_HOURS}h > Target: ${TARGET_RPO_HOURS}h${NC}"
        echo "\"rpo_status\": \"EXCEEDED\"," >> "$JSON_REPORT"
        return 1
    else
        log "${GREEN}✓ RPO within target (${RPO_HOURS}h < ${TARGET_RPO_HOURS}h)${NC}"
        echo "\"rpo_status\": \"OK\"," >> "$JSON_REPORT"
    fi

    return 0
}

estimate_rto() {
    log "\n${BLUE}=== RTO (Recovery Time Objective) Estimation ===${NC}"

    # Find most recent full backup for size estimation
    LATEST_FULL=$(find "$BACKUP_DIR" -name "${BACKUP_PREFIX}_full_*.sql.gz.gpg" -type f | sort -r | head -n 1)

    if [[ -z "$LATEST_FULL" ]]; then
        log "${RED}✗ No full backup found - cannot estimate RTO${NC}"
        echo "\"rto_estimated_minutes\": null," >> "$JSON_REPORT"
        echo "\"rto_target_hours\": $TARGET_RTO_HOURS," >> "$JSON_REPORT"
        return 1
    fi

    # Get backup size
    BACKUP_SIZE_BYTES=$(stat -f%z "$LATEST_FULL")
    BACKUP_SIZE_MB=$(echo "scale=2; $BACKUP_SIZE_BYTES / 1024 / 1024" | bc)

    # Estimate restore time based on size
    # Rough estimate: 50 MB/s decompression + 20 MB/s database import = ~15 MB/s combined
    RESTORE_SPEED_MBS=15
    ESTIMATED_SECONDS=$(echo "scale=0; $BACKUP_SIZE_MB / $RESTORE_SPEED_MBS" | bc)
    ESTIMATED_MINUTES=$(echo "scale=2; $ESTIMATED_SECONDS / 60" | bc)
    ESTIMATED_HOURS=$(echo "scale=2; $ESTIMATED_MINUTES / 60" | bc)

    log "Backup size: ${BACKUP_SIZE_MB} MB"
    log "Estimated RTO: ${ESTIMATED_MINUTES} minutes (${ESTIMATED_HOURS} hours)"
    log "Target RTO: ${TARGET_RTO_HOURS} hours"

    echo "\"rto_estimated_minutes\": $ESTIMATED_MINUTES," >> "$JSON_REPORT"
    echo "\"rto_estimated_hours\": $ESTIMATED_HOURS," >> "$JSON_REPORT"
    echo "\"rto_target_hours\": $TARGET_RTO_HOURS," >> "$JSON_REPORT"

    # Compare with target
    if (( $(echo "$ESTIMATED_HOURS > $TARGET_RTO_HOURS" | bc -l) )); then
        log "${YELLOW}⚠ Estimated RTO exceeds target: ${ESTIMATED_HOURS}h > ${TARGET_RTO_HOURS}h${NC}"
        log "   Consider database optimization or faster hardware"
        echo "\"rto_status\": \"WARNING\"," >> "$JSON_REPORT"
    else
        log "${GREEN}✓ Estimated RTO within target (${ESTIMATED_HOURS}h < ${TARGET_RTO_HOURS}h)${NC}"
        echo "\"rto_status\": \"OK\"," >> "$JSON_REPORT"
    fi

    return 0
}

check_backup_history() {
    log "\n${BLUE}=== Backup History (Last 7 Days) ===${NC}"

    # Count backups by type in last 7 days
    SEVEN_DAYS_AGO=$(date -v-7d +%s)

    FULL_COUNT=$(find "$BACKUP_DIR" -name "${BACKUP_PREFIX}_full_*.sql.gz.gpg" -type f -newermt "@$SEVEN_DAYS_AGO" | wc -l | xargs)
    DIFF_COUNT=$(find "$BACKUP_DIR" -name "${BACKUP_PREFIX}_diff_*.sql.gz.gpg" -type f -newermt "@$SEVEN_DAYS_AGO" | wc -l | xargs)

    log "Full backups (last 7 days): $FULL_COUNT"
    log "Differential backups (last 7 days): $DIFF_COUNT"
    log "Total backups: $((FULL_COUNT + DIFF_COUNT))"

    echo "\"full_backups_7days\": $FULL_COUNT," >> "$JSON_REPORT"
    echo "\"diff_backups_7days\": $DIFF_COUNT," >> "$JSON_REPORT"

    # Expected counts (1 full per day, 24 diff per day for hourly)
    EXPECTED_FULL=7
    EXPECTED_DIFF=$((7 * 24))

    if [[ $FULL_COUNT -lt $EXPECTED_FULL ]]; then
        log "${YELLOW}⚠ Missing full backups (found: $FULL_COUNT, expected: ~$EXPECTED_FULL)${NC}"
    fi

    if [[ $DIFF_COUNT -lt $((EXPECTED_DIFF / 2)) ]]; then
        log "${YELLOW}⚠ Low differential backup count (found: $DIFF_COUNT, expected: ~$EXPECTED_DIFF)${NC}"
    fi
}

check_storage_usage() {
    log "\n${BLUE}=== Storage Usage ===${NC}"

    # Calculate total backup size
    TOTAL_SIZE=$(du -sh "$BACKUP_DIR" | cut -f1)
    log "Total backup storage: $TOTAL_SIZE"
    echo "\"total_storage_used\": \"$TOTAL_SIZE\"," >> "$JSON_REPORT"

    # Check S3/MinIO status
    if [[ "$USE_MINIO" == "true" ]]; then
        log "\nMinIO Status:"
        if command -v mc &> /dev/null; then
            mc alias set mtn "$MINIO_ENDPOINT" "$MINIO_ACCESS_KEY" "$MINIO_SECRET_KEY" 2>/dev/null || true

            if mc du "mtn/${MINIO_BUCKET}" 2>/dev/null; then
                log "${GREEN}✓ MinIO accessible${NC}"
                echo "\"s3_status\": \"OK\"," >> "$JSON_REPORT"
            else
                log "${RED}✗ MinIO not accessible${NC}"
                echo "\"s3_status\": \"ERROR\"," >> "$JSON_REPORT"
            fi
        else
            log "${YELLOW}⚠ MinIO client not installed${NC}"
            echo "\"s3_status\": \"CLIENT_NOT_INSTALLED\"," >> "$JSON_REPORT"
        fi
    fi
}

check_retention_policy() {
    log "\n${BLUE}=== Retention Policy Compliance ===${NC}"

    # Find old backups that should have been deleted
    OLD_FULL=$(find "$BACKUP_DIR" -name "${BACKUP_PREFIX}_full_*.sql.gz.gpg" -type f -mtime +${FULL_BACKUP_RETENTION_DAYS} | wc -l | xargs)
    OLD_DIFF=$(find "$BACKUP_DIR" -name "${BACKUP_PREFIX}_diff_*.sql.gz.gpg" -type f -mtime +${DIFF_BACKUP_RETENTION_DAYS} | wc -l | xargs)

    log "Full backups older than ${FULL_BACKUP_RETENTION_DAYS} days: $OLD_FULL"
    log "Differential backups older than ${DIFF_BACKUP_RETENTION_DAYS} days: $OLD_DIFF"

    if [[ $OLD_FULL -gt 0 || $OLD_DIFF -gt 0 ]]; then
        log "${YELLOW}⚠ Found old backups that should be cleaned up${NC}"
        echo "\"retention_compliance\": \"CLEANUP_NEEDED\"," >> "$JSON_REPORT"
    else
        log "${GREEN}✓ Retention policy compliant${NC}"
        echo "\"retention_compliance\": \"OK\"," >> "$JSON_REPORT"
    fi
}

generate_summary() {
    log "\n${BLUE}=====================================${NC}"
    log "${BLUE}=== BACKUP MONITORING SUMMARY ===${NC}"
    log "${BLUE}=====================================${NC}"

    # Overall status
    if [[ $FULL_STATUS -eq 0 && $DIFF_STATUS -eq 0 && $RPO_STATUS -eq 0 ]]; then
        log "${GREEN}✓ All systems operational${NC}"
        OVERALL_STATUS="OK"
    elif [[ $FULL_STATUS -ne 0 || $RPO_STATUS -ne 0 ]]; then
        log "${RED}✗ Critical issues detected${NC}"
        OVERALL_STATUS="CRITICAL"
    else
        log "${YELLOW}⚠ Warnings detected${NC}"
        OVERALL_STATUS="WARNING"
    fi

    log "\nReport generated: $REPORT_FILE"
    log "JSON metrics: $JSON_REPORT"

    echo "\"overall_status\": \"$OVERALL_STATUS\"," >> "$JSON_REPORT"
    echo "\"timestamp\": \"$(date -Iseconds)\"" >> "$JSON_REPORT"
    echo "}" >> "$JSON_REPORT"
}

send_alert() {
    local status=$1

    if [[ "$ENABLE_MONITORING" != "true" ]]; then
        return
    fi

    if [[ "$status" == "CRITICAL" ]]; then
        "${SCRIPT_DIR}/send-notification.sh" "error" "CRITICAL: Backup System Alert" \
            "Backup monitoring detected critical issues. Check: $REPORT_FILE" \
            2>/dev/null || true
    elif [[ "$status" == "WARNING" ]]; then
        "${SCRIPT_DIR}/send-notification.sh" "warning" "WARNING: Backup System Alert" \
            "Backup monitoring detected warnings. Check: $REPORT_FILE" \
            2>/dev/null || true
    fi
}

# --- Main Execution ---
log "${BLUE}=============================================${NC}"
log "${BLUE}BACKUP MONITORING REPORT${NC}"
log "${BLUE}Generated: $(date +'%Y-%m-%d %H:%M:%S')${NC}"
log "${BLUE}=============================================${NC}"

# Initialize JSON report
cat > "$JSON_REPORT" <<EOF
{
  "report_timestamp": "$(date -Iseconds)",
  "hostname": "$(hostname -s)",
EOF

# Run checks
check_backup_freshness "full" 26 && FULL_STATUS=0 || FULL_STATUS=1
check_backup_freshness "differential" 2 && DIFF_STATUS=0 || DIFF_STATUS=1
calculate_rpo && RPO_STATUS=0 || RPO_STATUS=1
estimate_rto
check_backup_history
check_storage_usage
check_retention_policy

# Generate summary and send alerts
generate_summary
send_alert "$OVERALL_STATUS"

# Exit with appropriate code
if [[ "$OVERALL_STATUS" == "CRITICAL" ]]; then
    exit 2
elif [[ "$OVERALL_STATUS" == "WARNING" ]]; then
    exit 1
else
    exit 0
fi
