# MTN Notification Service Vault Policy
# Enhanced security for email/SMS/push notification operations

# SMTP credentials with rotation
path "smtp/creds/institutional" {
  capabilities = ["read"]
}

path "smtp/creds/personal" {
  capabilities = ["read"]
}

# SMS gateway credentials (Chilean providers)
path "sms/creds/entel" {
  capabilities = ["read"]
}

path "sms/creds/movistar" {
  capabilities = ["read"]
}

# Push notification service credentials
path "push/creds/firebase" {
  capabilities = ["read"]
}

path "push/creds/apns" {
  capabilities = ["read"]
}

# JWT signing for notification authentication
path "jwt/sign/notification-service" {
  capabilities = ["read", "update"]
}

# Email template encryption
path "transit/encrypt/email-templates" {
  capabilities = ["update"]
}

path "transit/decrypt/email-templates" {
  capabilities = ["update"]
}

# Chilean compliance - PII encryption
path "transit/encrypt/pii-data" {
  capabilities = ["update"]
}

path "transit/decrypt/pii-data" {
  capabilities = ["update"]
}

# Notification service configuration
path "kv/notification-service/*" {
  capabilities = ["read", "create", "update"]
}

# Email templates and branding
path "kv/email-templates/*" {
  capabilities = ["read", "create", "update", "delete"]
}

# SMS templates for Chilean format
path "kv/sms-templates/*" {
  capabilities = ["read", "create", "update", "delete"]
}

# Webhook verification secrets
path "webhooks/creds/*" {
  capabilities = ["read"]
}

# Chilean email service provider credentials
path "external-apis/creds/correos-chile" {
  capabilities = ["read"]
}

# Health check capability
path "sys/health" {
  capabilities = ["read"]
}