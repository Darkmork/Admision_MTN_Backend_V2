# MTN Evaluation Service Vault Policy
# Provides access for evaluation operations with enhanced security

# Database secrets for PostgreSQL
path "database/creds/mtn-evaluation-ro" {
  capabilities = ["read"]
}

path "database/creds/mtn-evaluation-rw" {
  capabilities = ["read"]
}

# JWT signing keys for service-to-service communication
path "jwt/sign/evaluation-service" {
  capabilities = ["read", "update"]
}

path "jwt/verify/evaluation-service" {
  capabilities = ["read"]
}

# Encryption keys for sensitive evaluation data
path "transit/encrypt/evaluation-scores" {
  capabilities = ["update"]
}

path "transit/decrypt/evaluation-scores" {
  capabilities = ["update"]
}

# Chilean psychologist validation credentials
path "external-apis/creds/colegio-psicologos" {
  capabilities = ["read"]
}

# Teacher certification validation
path "external-apis/creds/mineduc-docentes" {
  capabilities = ["read"]
}

# Evaluation service configuration
path "kv/evaluation-service/*" {
  capabilities = ["read"]
}

# Academic templates and rubrics (encrypted)
path "kv/evaluation-templates/*" {
  capabilities = ["read", "create", "update"]
}

# Integration with notification service
path "kv/notification-service/evaluation-templates" {
  capabilities = ["read"]
}

# Health check capability
path "sys/health" {
  capabilities = ["read"]
}