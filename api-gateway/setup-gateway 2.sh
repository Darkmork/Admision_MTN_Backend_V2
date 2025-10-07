#!/bin/bash
# MTN API Gateway Setup Script
# Sets up Kong Gateway with Feature Flags for Microservices Migration

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
NAMESPACE="mtn-gateway"
KONG_NAMESPACE="kong-system"
MONITORING_NAMESPACE="monitoring"
TIMEOUT="300s"

# Logging function
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARNING:${NC} $1"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR:${NC} $1"
    exit 1
}

# Check prerequisites
check_prerequisites() {
    log "Checking prerequisites..."
    
    # Check if kubectl is installed
    if ! command -v kubectl &> /dev/null; then
        error "kubectl is not installed"
    fi
    
    # Check if helm is installed
    if ! command -v helm &> /dev/null; then
        error "helm is not installed"
    fi
    
    # Check Kubernetes connection
    if ! kubectl cluster-info &> /dev/null; then
        error "Cannot connect to Kubernetes cluster"
    fi
    
    log "Prerequisites check passed"
}

# Create namespaces
create_namespaces() {
    log "Creating namespaces..."
    
    # Create namespaces with proper labels
    kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -
    kubectl create namespace $KONG_NAMESPACE --dry-run=client -o yaml | kubectl apply -f -
    kubectl create namespace $MONITORING_NAMESPACE --dry-run=client -o yaml | kubectl apply -f -
    
    # Label namespaces
    kubectl label namespace $NAMESPACE name=$NAMESPACE --overwrite
    kubectl label namespace $KONG_NAMESPACE name=$KONG_NAMESPACE --overwrite
    kubectl label namespace $MONITORING_NAMESPACE name=$MONITORING_NAMESPACE --overwrite
    
    log "Namespaces created successfully"
}

# Install Kong Gateway
install_kong() {
    log "Installing Kong Gateway..."
    
    # Add Kong Helm repository
    helm repo add kong https://charts.konghq.com
    helm repo update
    
    # Install Kong with custom values
    cat > kong-values.yaml << EOF
# Kong Gateway Values for MTN
env:
  database: "off"  # DB-less mode
  nginx_worker_processes: "2"
  proxy_access_log: /dev/stdout
  admin_access_log: /dev/stdout
  admin_gui_access_log: /dev/stdout
  portal_api_access_log: /dev/stdout
  proxy_error_log: /dev/stderr
  admin_error_log: /dev/stderr
  admin_gui_error_log: /dev/stderr
  portal_api_error_log: /dev/stderr
  prefix: /kong_prefix/

image:
  repository: kong
  tag: "3.4"
  
admin:
  enabled: true
  type: ClusterIP
  annotations: {}
  http:
    enabled: true
    servicePort: 8001
  tls:
    enabled: false

proxy:
  enabled: true
  type: LoadBalancer
  annotations:
    service.beta.kubernetes.io/aws-load-balancer-type: nlb
    service.beta.kubernetes.io/aws-load-balancer-cross-zone-load-balancing-enabled: "true"
  http:
    enabled: true
    servicePort: 80
    containerPort: 8000
  tls:
    enabled: true
    servicePort: 443
    containerPort: 8443

ingressController:
  enabled: true
  installCRDs: false
  image:
    repository: kong/kubernetes-ingress-controller
    tag: "2.12"
  env:
    kong_admin_url: "http://localhost:8001"
    kong_admin_tls_skip_verify: "true"

resources:
  limits:
    cpu: 1000m
    memory: 1Gi
  requests:
    cpu: 200m
    memory: 512Mi

autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70

podSecurityContext:
  runAsUser: 1001
  runAsGroup: 1001
  fsGroup: 1001

securityContext:
  runAsNonRoot: true
  runAsUser: 1001
  readOnlyRootFilesystem: true
  allowPrivilegeEscalation: false
  capabilities:
    drop:
    - ALL

serviceMonitor:
  enabled: true
  labels:
    release: prometheus
EOF

    helm install kong kong/kong -n $KONG_NAMESPACE --values kong-values.yaml --wait --timeout $TIMEOUT
    
    log "Kong Gateway installed successfully"
}

# Install Redis for caching
install_redis() {
    log "Installing Redis for feature flag caching..."
    
    helm repo add bitnami https://charts.bitnami.com/bitnami
    helm repo update
    
    cat > redis-values.yaml << EOF
auth:
  enabled: false
architecture: standalone
master:
  persistence:
    enabled: true
    size: 1Gi
  resources:
    limits:
      memory: 256Mi
      cpu: 200m
    requests:
      memory: 128Mi
      cpu: 100m
metrics:
  enabled: true
  serviceMonitor:
    enabled: true
    namespace: $MONITORING_NAMESPACE
EOF

    helm install redis bitnami/redis -n $NAMESPACE --values redis-values.yaml --wait --timeout $TIMEOUT
    
    log "Redis installed successfully"
}

# Install PostgreSQL for feature flags
install_postgresql() {
    log "Installing PostgreSQL for feature flags..."
    
    cat > postgres-values.yaml << EOF
auth:
  postgresPassword: "change-in-production"
  username: "feature_flag_user"
  password: "change-in-production"
  database: "feature_flags"
primary:
  persistence:
    enabled: true
    size: 5Gi
  resources:
    limits:
      memory: 512Mi
      cpu: 500m
    requests:
      memory: 256Mi
      cpu: 200m
  initdb:
    scripts:
      01-init-feature-flags.sql: |
        CREATE DATABASE feature_flags;
        CREATE USER feature_flag_user WITH ENCRYPTED PASSWORD 'change-in-production';
        GRANT ALL PRIVILEGES ON DATABASE feature_flags TO feature_flag_user;
metrics:
  enabled: true
  serviceMonitor:
    enabled: true
    namespace: $MONITORING_NAMESPACE
EOF

    helm install postgres bitnami/postgresql -n $NAMESPACE --values postgres-values.yaml --wait --timeout $TIMEOUT
    
    log "PostgreSQL installed successfully"
}

# Deploy feature flag components
deploy_feature_flags() {
    log "Deploying feature flag components..."
    
    # Apply all YAML files
    kubectl apply -f kong-gateway.yaml
    kubectl apply -f feature-flag-service.yaml
    kubectl apply -f traffic-routing-controller.yaml
    
    # Wait for deployments to be ready
    kubectl wait --for=condition=available --timeout=$TIMEOUT deployment/feature-flag-service -n $NAMESPACE
    kubectl wait --for=condition=available --timeout=$TIMEOUT deployment/traffic-routing-controller -n $NAMESPACE
    
    log "Feature flag components deployed successfully"
}

# Initialize feature flags
initialize_feature_flags() {
    log "Initializing feature flags..."
    
    # Wait for feature flag service to be ready
    kubectl wait --for=condition=ready pod -l app=feature-flag-service -n $NAMESPACE --timeout=$TIMEOUT
    
    # Port-forward to access the service
    kubectl port-forward svc/feature-flag-service 8080:8080 -n $NAMESPACE &
    PORT_FORWARD_PID=$!
    
    sleep 10
    
    # Initialize default feature flags via API
    cat > feature-flags-init.json << EOF
{
  "flags": [
    {
      "name": "microservices_migration",
      "description": "Enable microservices architecture migration",
      "enabled": true,
      "rollout_percentage": 100,
      "targeting_rules": {
        "user_roles": ["ADMIN", "TEACHER", "COORDINATOR"],
        "beta_users": true
      }
    },
    {
      "name": "user_service_routing",
      "description": "Route user management to user-service",
      "enabled": true,
      "rollout_percentage": 100,
      "canary_percentage": 0
    },
    {
      "name": "application_service_routing", 
      "description": "Route application management to application-service",
      "enabled": true,
      "rollout_percentage": 100,
      "canary_percentage": 0
    },
    {
      "name": "evaluation_service_routing",
      "description": "Route evaluations to evaluation-service",
      "enabled": true,
      "rollout_percentage": 100,
      "canary_percentage": 0
    },
    {
      "name": "notification_service_routing",
      "description": "Route notifications to notification-service",
      "enabled": true,
      "rollout_percentage": 100,
      "canary_percentage": 0
    }
  ]
}
EOF

    # Use curl to initialize (retry logic)
    for i in {1..5}; do
        if curl -X POST http://localhost:8080/api/admin/feature-flags/bulk \
               -H "Content-Type: application/json" \
               -H "Authorization: Bearer admin-key-change-in-production" \
               -d @feature-flags-init.json; then
            log "Feature flags initialized successfully"
            break
        else
            warn "Attempt $i failed, retrying in 10 seconds..."
            sleep 10
        fi
    done
    
    # Clean up port-forward
    kill $PORT_FORWARD_PID 2>/dev/null || true
    
    # Clean up temporary files
    rm -f kong-values.yaml redis-values.yaml postgres-values.yaml feature-flags-init.json
}

# Verify installation
verify_installation() {
    log "Verifying installation..."
    
    # Check all pods are running
    kubectl get pods -n $NAMESPACE
    kubectl get pods -n $KONG_NAMESPACE
    
    # Check services
    kubectl get services -n $NAMESPACE
    kubectl get services -n $KONG_NAMESPACE
    
    # Check ingress configuration
    kubectl get ingress -n $NAMESPACE
    
    # Test Kong admin API
    if kubectl port-forward svc/kong-admin 8001:8001 -n $KONG_NAMESPACE --timeout=30s &>/dev/null &
    then
        sleep 5
        if curl -s http://localhost:8001/ | grep -q "kong"; then
            log "Kong admin API is accessible"
        else
            warn "Kong admin API test failed"
        fi
        pkill -f "port-forward.*8001" || true
    fi
    
    log "Installation verification complete"
}

# Display access information
display_access_info() {
    log "=== MTN API Gateway Installation Complete ==="
    echo
    echo "Access Information:"
    echo "=================="
    
    # Get external IP for Kong proxy
    EXTERNAL_IP=$(kubectl get svc kong-proxy -n $KONG_NAMESPACE -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "pending")
    if [ "$EXTERNAL_IP" = "pending" ]; then
        EXTERNAL_IP=$(kubectl get svc kong-proxy -n $KONG_NAMESPACE -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "pending")
    fi
    
    echo "🌐 API Gateway URL: http://$EXTERNAL_IP"
    echo "🔧 Kong Admin API: kubectl port-forward svc/kong-admin 8001:8001 -n $KONG_NAMESPACE"
    echo "🏁 Feature Flag Service: kubectl port-forward svc/feature-flag-service 8080:8080 -n $NAMESPACE"
    echo
    echo "Feature Flag Management:"
    echo "======================"
    echo "• All microservice routing is enabled (100% traffic)"
    echo "• Monolith routing is disabled (0% traffic)"
    echo "• Canary deployments ready for gradual rollouts"
    echo
    echo "Next Steps:"
    echo "=========="
    echo "1. Deploy your microservices with the provided Helm charts"
    echo "2. Configure DNS to point api.mtn.cl to $EXTERNAL_IP"
    echo "3. Set up TLS certificates"
    echo "4. Monitor traffic routing via feature flag service"
    echo
    log "Setup complete! 🎉"
}

# Main execution
main() {
    log "Starting MTN API Gateway setup..."
    
    check_prerequisites
    create_namespaces
    install_kong
    install_redis
    install_postgresql
    deploy_feature_flags
    initialize_feature_flags
    verify_installation
    display_access_info
}

# Run with error handling
trap 'error "Setup failed at line $LINENO"' ERR
main "$@"