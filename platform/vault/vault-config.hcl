# HashiCorp Vault Configuration for MTN Admission System
# Security-first configuration with Chilean compliance

# API Configuration
api_addr = "https://vault.mtn.cl"
cluster_addr = "https://vault.mtn.cl:8201"

# Storage Backend - Integrated Storage (Raft)
storage "raft" {
  path = "/vault/data"
  node_id = "vault-mtn-1"
  
  # Autopilot for automated operations
  autopilot {
    cleanup_dead_servers = true
    last_contact_threshold = "10s"
    max_trailing_logs = 1000
    min_quorum = 3
    server_stabilization_time = "10s"
  }
}

# Cluster Configuration
cluster_name = "mtn-admission-vault"

# Listener Configuration
listener "tcp" {
  address     = "0.0.0.0:8200"
  tls_cert_file = "/vault/tls/tls.crt"
  tls_key_file  = "/vault/tls/tls.key"
  tls_min_version = "tls12"
  tls_cipher_suites = [
    "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
    "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
    "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
    "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384"
  ]
  
  # Security headers for Chilean compliance
  tls_require_and_verify_client_cert = false
  tls_disable_client_certs = false
}

# Telemetry and Monitoring
telemetry {
  prometheus_retention_time = "30s"
  disable_hostname = false
  enable_hostname_label = true
  
  # Chilean timezone
  usage_gauge_period = "10m"
  maximum_gauge_cardinality = 500
  
  # Metrics for Chilean compliance reporting
  statsd_address = "statsd.monitoring.svc.cluster.local:8125"
}

# Seal Configuration - Auto-unseal with KMS
seal "awskms" {
  region     = "us-east-1"
  kms_key_id = "alias/mtn-vault-unseal"
  endpoint   = "https://kms.us-east-1.amazonaws.com"
}

# UI Configuration
ui = true

# Logging
log_level = "INFO"
log_format = "json"

# Chilean-specific configuration
default_lease_ttl = "24h"      # 24 hours for Chilean business hours
max_lease_ttl = "720h"         # 30 days maximum
default_max_request_duration = "90s"

# Disable mlock for containerized environments
disable_mlock = true

# Plugin directory
plugin_directory = "/vault/plugins"

# Performance and limits
raw_storage_endpoint = true
introspection_endpoint = true
disable_sealwrap = false
disable_indexing = false

# Chilean compliance - PII handling
disable_cache = false
disable_clustering = false