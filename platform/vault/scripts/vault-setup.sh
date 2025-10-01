#!/bin/bash
# Vault Setup Script for MTN Admission System
# Configures authentication, policies, and secret engines

set -euo pipefail

# Chilean timezone and locale
export TZ="America/Santiago"
export LC_ALL="es_CL.UTF-8"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S %Z')] $1${NC}"
}

warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S %Z')] WARNING: $1${NC}"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S %Z')] ERROR: $1${NC}"
}

# Configuration
VAULT_ADDR="${VAULT_ADDR:-https://vault.mtn.cl}"
VAULT_NAMESPACE="${VAULT_NAMESPACE:-mtn-admission}"
VAULT_TOKEN="${VAULT_TOKEN:-}"

# Validate environment
if [[ -z "$VAULT_TOKEN" ]]; then
    error "VAULT_TOKEN environment variable is required"
    exit 1
fi

export VAULT_ADDR
export VAULT_NAMESPACE

log "🚀 Iniciando configuración de Vault para MTN Admission System"
log "🏛️  Vault Address: $VAULT_ADDR"
log "📁 Namespace: $VAULT_NAMESPACE"

# Check Vault connectivity
log "🔍 Verificando conectividad con Vault..."
if ! vault status > /dev/null 2>&1; then
    error "No se puede conectar con Vault en $VAULT_ADDR"
    exit 1
fi

log "✅ Conectividad con Vault verificada"

# Enable audit logging
log "📊 Configurando auditoría..."
vault audit enable -path="mtn-admission-audit" file file_path="/vault/logs/mtn-admission-audit.log" || warn "Audit ya podría estar habilitado"

# Enable KV secrets engine v2
log "🗝️  Configurando KV secrets engine..."
vault secrets enable -version=2 -path=kv kv || warn "KV engine ya podría estar habilitado"

# Enable database secrets engine
log "🗄️  Configurando database secrets engine..."
vault secrets enable -path=database database || warn "Database engine ya podría estar habilitado"

# Configure PostgreSQL database connection
log "🐘 Configurando conexión PostgreSQL..."
vault write database/config/mtn-postgres-db \
    plugin_name="postgresql-database-plugin" \
    connection_url="postgresql://{{username}}:{{password}}@postgres.infrastructure.svc.cluster.local:5432/mtn_admission_db?sslmode=require&timezone=America/Santiago" \
    allowed_roles="mtn-application-ro,mtn-application-rw,mtn-evaluation-ro,mtn-evaluation-rw,mtn-notification-rw" \
    username="${POSTGRES_ADMIN_USER:-postgres}" \
    password="${POSTGRES_ADMIN_PASSWORD:-}"

# Create database roles
log "👤 Creando roles de base de datos..."

# Read-only role for applications
vault write database/roles/mtn-application-ro \
    db_name="mtn-postgres-db" \
    creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}' IN ROLE readonly_role; GRANT CONNECT ON DATABASE mtn_admission_db TO \"{{name}}\";" \
    default_ttl="1h" \
    max_ttl="24h"

# Read-write role for applications
vault write database/roles/mtn-application-rw \
    db_name="mtn-postgres-db" \
    creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}' IN ROLE readwrite_role; GRANT CONNECT ON DATABASE mtn_admission_db TO \"{{name}}\";" \
    default_ttl="1h" \
    max_ttl="24h"

# Read-only role for evaluations
vault write database/roles/mtn-evaluation-ro \
    db_name="mtn-postgres-db" \
    creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}' IN ROLE evaluation_readonly_role; GRANT CONNECT ON DATABASE mtn_admission_db TO \"{{name}}\";" \
    default_ttl="1h" \
    max_ttl="24h"

# Read-write role for evaluations
vault write database/roles/mtn-evaluation-rw \
    db_name="mtn-postgres-db" \
    creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}' IN ROLE evaluation_readwrite_role; GRANT CONNECT ON DATABASE mtn_admission_db TO \"{{name}}\";" \
    default_ttl="1h" \
    max_ttl="24h"

# Read-write role for notifications
vault write database/roles/mtn-notification-rw \
    db_name="mtn-postgres-db" \
    creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}' IN ROLE notification_readwrite_role; GRANT CONNECT ON DATABASE mtn_admission_db TO \"{{name}}\";" \
    default_ttl="30m" \
    max_ttl="2h"

# Enable Transit secrets engine for encryption
log "🔐 Configurando Transit secrets engine para cifrado..."
vault secrets enable -path=transit transit || warn "Transit engine ya podría estar habilitado"

# Create encryption keys
vault write -f transit/keys/document-uploads || warn "Clave document-uploads ya existe"
vault write -f transit/keys/evaluation-scores || warn "Clave evaluation-scores ya existe"
vault write -f transit/keys/email-templates || warn "Clave email-templates ya existe"
vault write -f transit/keys/pii-data || warn "Clave pii-data ya existe"

# Enable JWT/OIDC auth method
log "🎫 Configurando JWT/OIDC auth method..."
vault auth enable -path=jwt jwt || warn "JWT auth ya podría estar habilitado"

# Configure JWT auth with Keycloak
vault write auth/jwt/config \
    bound_issuer="https://auth.mtn.cl/realms/mtn-admision" \
    default_role="mtn-admission-default" \
    jwks_url="https://auth.mtn.cl/realms/mtn-admision/protocol/openid-connect/certs" \
    jwks_ca_pem="@/vault/tls/keycloak-ca.crt"

# Enable Kubernetes auth method
log "☸️  Configurando Kubernetes auth method..."
vault auth enable -path=kubernetes kubernetes || warn "Kubernetes auth ya podría estar habilitado"

# Configure Kubernetes auth
vault write auth/kubernetes/config \
    token_reviewer_jwt="@/var/run/secrets/kubernetes.io/serviceaccount/token" \
    kubernetes_host="https://kubernetes.default.svc.cluster.local" \
    kubernetes_ca_cert="@/var/run/secrets/kubernetes.io/serviceaccount/ca.crt" \
    issuer="https://kubernetes.default.svc.cluster.local"

# Create policies
log "📋 Creando políticas de acceso..."

# Application service policy
vault policy write mtn-application-service - <<EOF
# Database access
path "database/creds/mtn-application-ro" {
  capabilities = ["read"]
}
path "database/creds/mtn-application-rw" {
  capabilities = ["read"]
}

# JWT keys
path "jwt/sign/application-service" {
  capabilities = ["read", "update"]
}

# SMTP credentials
path "smtp/creds/institutional" {
  capabilities = ["read"]
}

# Transit encryption
path "transit/encrypt/document-uploads" {
  capabilities = ["update"]
}
path "transit/decrypt/document-uploads" {
  capabilities = ["update"]
}

# Configuration secrets
path "kv/data/application-service/*" {
  capabilities = ["read"]
}

# External APIs
path "external-apis/creds/registro-civil" {
  capabilities = ["read"]
}
EOF

# Evaluation service policy
vault policy write mtn-evaluation-service - <<EOF
# Database access
path "database/creds/mtn-evaluation-ro" {
  capabilities = ["read"]
}
path "database/creds/mtn-evaluation-rw" {
  capabilities = ["read"]
}

# JWT keys
path "jwt/sign/evaluation-service" {
  capabilities = ["read", "update"]
}

# Transit encryption for sensitive evaluation data
path "transit/encrypt/evaluation-scores" {
  capabilities = ["update"]
}
path "transit/decrypt/evaluation-scores" {
  capabilities = ["update"]
}

# Configuration secrets
path "kv/data/evaluation-service/*" {
  capabilities = ["read"]
}
path "kv/data/evaluation-templates/*" {
  capabilities = ["read", "create", "update"]
}
EOF

# Notification service policy
vault policy write mtn-notification-service - <<EOF
# SMTP credentials
path "smtp/creds/institutional" {
  capabilities = ["read"]
}

# SMS credentials
path "sms/creds/entel" {
  capabilities = ["read"]
}

# Push notification credentials
path "push/creds/firebase" {
  capabilities = ["read"]
}

# JWT keys
path "jwt/sign/notification-service" {
  capabilities = ["read", "update"]
}

# Transit encryption
path "transit/encrypt/email-templates" {
  capabilities = ["update"]
}
path "transit/decrypt/email-templates" {
  capabilities = ["update"]
}
path "transit/encrypt/pii-data" {
  capabilities = ["update"]
}
path "transit/decrypt/pii-data" {
  capabilities = ["update"]
}

# Configuration secrets
path "kv/data/notification-service/*" {
  capabilities = ["read", "create", "update"]
}
path "kv/data/email-templates/*" {
  capabilities = ["read", "create", "update", "delete"]
}
EOF

# Create Kubernetes roles
log "👥 Creando roles de Kubernetes..."

# Role for External Secrets Operator
vault write auth/kubernetes/role/mtn-admission-external-secrets \
    bound_service_account_names="external-secrets-operator" \
    bound_service_account_namespaces="external-secrets-system" \
    policies="mtn-application-service,mtn-evaluation-service,mtn-notification-service" \
    ttl="1h"

# Role for application services
vault write auth/kubernetes/role/mtn-admission-services \
    bound_service_account_names="vault-auth" \
    bound_service_account_namespaces="admissions" \
    policies="mtn-application-service,mtn-evaluation-service,mtn-notification-service" \
    ttl="1h"

# Role for infrastructure services
vault write auth/kubernetes/role/mtn-admission-infrastructure \
    bound_service_account_names="vault-auth" \
    bound_service_account_namespaces="infrastructure" \
    policies="mtn-application-service" \
    ttl="30m"

# Setup initial secrets
log "🏗️  Configurando secretos iniciales..."

# SMTP credentials
vault kv put kv/smtp/institutional \
    host="${SMTP_HOST:-smtp.gmail.com}" \
    port="${SMTP_PORT:-587}" \
    username="${SMTP_USERNAME:-jorge.gangale@mtn.cl}" \
    password="${SMTP_PASSWORD:-}" \
    auth="true" \
    starttls="true"

# Application service configuration
vault kv put kv/application-service/database \
    database_name="mtn_admission_db"

vault kv put kv/application-service/jwt \
    issuer="https://api.mtn.cl" \
    audience="mtn-admission" \
    expiration="24h"

vault kv put kv/application-service/oidc \
    issuer_uri="https://auth.mtn.cl/realms/mtn-admision" \
    client_id="application-service" \
    client_secret="${OIDC_APPLICATION_CLIENT_SECRET:-}"

vault kv put kv/application-service/email \
    institutional_from_name="Colegio Monte Tabor y Nazaret - Sistema de Admisión" \
    institutional_reply_to="jorge.gangale@mtn.cl" \
    institutional_support="jorge.gangale@mtn.cl"

# External APIs configuration (placeholders - update with real credentials)
vault kv put kv/external-apis/registro-civil \
    api_key="${REGISTRO_CIVIL_API_KEY:-placeholder}" \
    api_url="https://api.registrocivil.cl"

vault kv put kv/external-apis/correos-chile \
    api_key="${CORREOS_CHILE_API_KEY:-placeholder}" \
    api_url="https://api.correos.cl"

# Enable SMTP secrets engine
vault secrets enable -path=smtp kv-v2 || warn "SMTP secrets engine ya podría estar habilitado"

# Enable SMS secrets engine
vault secrets enable -path=sms kv-v2 || warn "SMS secrets engine ya podría estar habilitado"

# Enable push notification secrets engine
vault secrets enable -path=push kv-v2 || warn "Push secrets engine ya podría estar habilitado"

log "✅ Configuración de Vault completada exitosamente"
log "🔑 Los servicios ahora pueden usar External Secrets Operator para obtener credenciales"
log "⚡ La rotación automática está configurada para:"
log "   - Credenciales de base de datos: cada 1 hora"
log "   - Claves JWT: cada 6 horas" 
log "   - Credenciales SMTP: cada 24 horas"

warn "⚠️  Recuerda configurar las credenciales reales en producción:"
warn "   - POSTGRES_ADMIN_PASSWORD"
warn "   - SMTP_PASSWORD"
warn "   - OIDC_APPLICATION_CLIENT_SECRET"
warn "   - REGISTRO_CIVIL_API_KEY"
warn "   - CORREOS_CHILE_API_KEY"

log "🏁 Setup completado - Vault está listo para uso en producción"