# MTN Application Service Vault Policy
# Provides minimal required access for application service operations

# Database secrets for PostgreSQL
path "database/creds/mtn-application-ro" {
  capabilities = ["read"]
}

path "database/creds/mtn-application-rw" {
  capabilities = ["read"]
}

# JWT signing keys rotation
path "jwt/sign/application-service" {
  capabilities = ["read", "update"]
}

path "jwt/verify/application-service" {
  capabilities = ["read"]
}

# Email/SMTP credentials
path "smtp/creds/institutional" {
  capabilities = ["read"]
}

# File encryption keys for document uploads
path "transit/encrypt/document-uploads" {
  capabilities = ["update"]
}

path "transit/decrypt/document-uploads" {
  capabilities = ["update"]
}

# Chilean RUT validation service credentials
path "external-apis/creds/registro-civil" {
  capabilities = ["read"]
}

# Application-specific configuration secrets
path "kv/application-service/*" {
  capabilities = ["read"]
}

# Audit logging access (read-only)
path "sys/audit" {
  capabilities = ["read", "sudo"]
}

# Health check capability
path "sys/health" {
  capabilities = ["read"]
}