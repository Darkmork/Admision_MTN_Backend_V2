#!/bin/bash
# ============================================================
# NOTIFICATION SCRIPT
# Send email alerts for backup events
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

# --- Parse Arguments ---
SEVERITY="${1:-info}"      # success, warning, error, info
SUBJECT="${2:-Backup Notification}"
MESSAGE="${3:-No message provided}"

# --- Email Template ---
send_email() {
    local severity=$1
    local subject=$2
    local message=$3

    # Color coding for email
    case $severity in
        success)
            COLOR="#10b981"  # Green
            ICON="✅"
            ;;
        warning)
            COLOR="#f59e0b"  # Orange
            ICON="⚠️"
            ;;
        error)
            COLOR="#ef4444"  # Red
            ICON="❌"
            ;;
        *)
            COLOR="#3b82f6"  # Blue
            ICON="ℹ️"
            ;;
    esac

    # Create HTML email
    cat > /tmp/backup_email.html <<EOF
<!DOCTYPE html>
<html>
<head>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
            line-height: 1.6;
            color: #333;
            max-width: 600px;
            margin: 0 auto;
            padding: 20px;
        }
        .header {
            background-color: ${COLOR};
            color: white;
            padding: 20px;
            border-radius: 8px 8px 0 0;
            text-align: center;
        }
        .content {
            background-color: #f9fafb;
            padding: 30px;
            border: 1px solid #e5e7eb;
            border-top: none;
            border-radius: 0 0 8px 8px;
        }
        .message {
            background-color: white;
            padding: 20px;
            border-radius: 6px;
            border-left: 4px solid ${COLOR};
            margin: 20px 0;
            white-space: pre-line;
        }
        .footer {
            margin-top: 30px;
            padding-top: 20px;
            border-top: 1px solid #e5e7eb;
            font-size: 12px;
            color: #6b7280;
            text-align: center;
        }
        .info-table {
            width: 100%;
            margin: 20px 0;
        }
        .info-table td {
            padding: 8px;
            border-bottom: 1px solid #e5e7eb;
        }
        .info-table td:first-child {
            font-weight: 600;
            color: #6b7280;
            width: 40%;
        }
    </style>
</head>
<body>
    <div class="header">
        <h1>${ICON} ${subject}</h1>
    </div>
    <div class="content">
        <table class="info-table">
            <tr>
                <td>Severidad:</td>
                <td style="color: ${COLOR}; font-weight: 600; text-transform: uppercase;">${severity}</td>
            </tr>
            <tr>
                <td>Fecha/Hora:</td>
                <td>$(date +'%Y-%m-%d %H:%M:%S %Z')</td>
            </tr>
            <tr>
                <td>Sistema:</td>
                <td>$(hostname)</td>
            </tr>
            <tr>
                <td>Base de Datos:</td>
                <td>${DB_NAME}</td>
            </tr>
        </table>

        <div class="message">
            <strong>Mensaje:</strong><br>
            ${message}
        </div>

        <p style="margin-top: 20px; font-size: 14px;">
            Este es un mensaje automático del sistema de backup de Admisión MTN.
            Para más detalles, revise los logs en: <code>${LOG_DIR}</code>
        </p>
    </div>
    <div class="footer">
        <p>
            <strong>Sistema de Backup Automático</strong><br>
            Colegio Monte Tabor y Nazaret<br>
            Proyecto: Admisión MTN
        </p>
    </div>
</body>
</html>
EOF

    # Send email using Python with SMTP
    python3 - <<PYTHON_SCRIPT
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
import sys

# Email configuration
smtp_server = "${SMTP_SERVER}"
smtp_port = ${SMTP_PORT}
smtp_user = "${SMTP_USER}"
smtp_password = "${SMTP_PASSWORD}"
recipient = "${ALERT_EMAIL}"

try:
    # Read HTML content
    with open('/tmp/backup_email.html', 'r') as f:
        html_content = f.read()

    # Create message
    msg = MIMEMultipart('alternative')
    msg['Subject'] = "[Backup System] ${subject}"
    msg['From'] = smtp_user
    msg['To'] = recipient

    # Plain text fallback
    text_content = """
${ICON} ${subject}

Severidad: ${severity}
Fecha/Hora: $(date +'%Y-%m-%d %H:%M:%S %Z')
Sistema: $(hostname)
Base de Datos: ${DB_NAME}

Mensaje:
${message}

---
Este es un mensaje automático del sistema de backup.
Para más detalles, revise los logs en: ${LOG_DIR}
"""

    part1 = MIMEText(text_content, 'plain')
    part2 = MIMEText(html_content, 'html')

    msg.attach(part1)
    msg.attach(part2)

    # Send email
    with smtplib.SMTP(smtp_server, smtp_port) as server:
        server.starttls()
        server.login(smtp_user, smtp_password)
        server.send_message(msg)

    print("✓ Email notification sent successfully")
    sys.exit(0)

except Exception as e:
    print(f"✗ Failed to send email: {str(e)}", file=sys.stderr)
    sys.exit(1)

PYTHON_SCRIPT

    # Clean up temp file
    rm -f /tmp/backup_email.html
}

# --- Log to syslog if enabled ---
if [[ "$ENABLE_SYSLOG" == "true" ]]; then
    logger -t "backup-system" -p "user.${severity}" "$subject: $message"
fi

# --- Send email notification ---
if [[ "$ENABLE_MONITORING" == "true" ]]; then
    send_email "$SEVERITY" "$SUBJECT" "$MESSAGE"
else
    echo "Monitoring disabled - notification skipped"
    echo "Subject: $SUBJECT"
    echo "Message: $MESSAGE"
fi
