#!/bin/bash
# Gmail configuration for institutional emails - Development
# Use this script to set environment variables before running the application

export SMTP_HOST="smtp.gmail.com"
export SMTP_PORT="587"
export SMTP_USERNAME="jorge.gangale@mtn.cl"
export SMTP_PASSWORD="brye mjax brum bgux"
export SMTP_AUTH="true"
export SMTP_STARTTLS="true"

# Institutional email settings
export INSTITUTIONAL_EMAIL_FROM_NAME="Colegio Monte Tabor y Nazaret - Sistema de Admisión"
export INSTITUTIONAL_EMAIL_REPLY_TO="jorge.gangale@mtn.cl"
export INSTITUTIONAL_EMAIL_SUPPORT="jorge.gangale@mtn.cl"

echo "✅ Gmail SMTP configuration loaded for jorge.gangale@mtn.cl"
echo "✅ Gmail app password configured successfully"
echo "📧 Email from: $SMTP_USERNAME"
echo "🏢 Institution: $INSTITUTIONAL_EMAIL_FROM_NAME"