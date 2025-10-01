# Linkerd mTLS Alternative Implementation

## Overview
This document provides Linkerd-based mTLS configuration as an alternative to Istio for the MTN Admission System. Linkerd offers simpler configuration with automatic mTLS and policy enforcement.

## Prerequisites

```bash
# Install Linkerd CLI
curl --proto '=https' --tlsv1.2 -sSfL https://run.linkerd.io/install | sh

# Add to PATH
export PATH=$PATH:$HOME/.linkerd2/bin

# Verify installation
linkerd check --pre
```

## 1. Linkerd Installation

```bash
# Install Linkerd control plane
linkerd install --crds | kubectl apply -f -
linkerd install | kubectl apply -f -

# Verify installation
linkerd check

# Install Linkerd Viz (optional, for observability)
linkerd viz install | kubectl apply -f -
```

## 2. Namespace Injection Configuration

```yaml
# platform/linkerd/namespace-config.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: admissions
  annotations:
    linkerd.io/inject: enabled
    config.linkerd.io/proxy-cpu-limit: "1"
    config.linkerd.io/proxy-cpu-request: "100m"
    config.linkerd.io/proxy-memory-limit: "512Mi"
    config.linkerd.io/proxy-memory-request: "64Mi"
    # Enable automatic mTLS for all traffic
    config.linkerd.io/default-inbound-policy: all-authenticated
  labels:
    linkerd.io/control-plane-ns: linkerd
    linkerd.io/is-control-plane: false
```

## 3. Automatic Injection for Existing Deployments

```bash
# Inject Linkerd proxy into existing deployments
kubectl get deployment -n admissions -o yaml | \
  linkerd inject - | \
  kubectl apply -f -

# Verify injection
linkerd check --proxy -n admissions
```

## 4. Server Policies (Equivalent to Istio AuthorizationPolicies)

### Default Deny Policy

```yaml
# platform/linkerd/server-policy-default-deny.yaml
apiVersion: policy.linkerd.io/v1beta1
kind: Server
metadata:
  name: default-server
  namespace: admissions
spec:
  podSelector:
    matchLabels: {}
  port: 8080
  proxyProtocol: "HTTP/2"
---
apiVersion: policy.linkerd.io/v1beta1
kind: ServerAuthorization
metadata:
  name: default-deny
  namespace: admissions
spec:
  server:
    name: default-server
  # Empty requiredRoutes means deny all by default
  requiredRoutes: []
```

### API Gateway Access Policy

```yaml
# platform/linkerd/server-policy-gateway.yaml
apiVersion: policy.linkerd.io/v1beta1
kind: Server
metadata:
  name: api-gateway-server
  namespace: admissions
spec:
  podSelector:
    matchLabels:
      app: api-gateway
  port: 8080
  proxyProtocol: "HTTP/2"
---
apiVersion: policy.linkerd.io/v1beta1
kind: ServerAuthorization
metadata:
  name: allow-ingress-to-gateway
  namespace: admissions
spec:
  server:
    name: api-gateway-server
  client:
    meshTLS:
      serviceAccounts:
        - name: linkerd-proxy
          namespace: linkerd
    # Allow external traffic through ingress
    unauthenticated: true
  requiredRoutes:
    - pathRegex: "/api/.*"
      methods: ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"]
    - pathRegex: "/actuator/(health|info)"
      methods: ["GET"]
```

### Service-to-Service Policies

```yaml
# platform/linkerd/server-policy-services.yaml
apiVersion: policy.linkerd.io/v1beta1
kind: Server
metadata:
  name: application-service-server
  namespace: admissions
spec:
  podSelector:
    matchLabels:
      app: application-service
  port: 8080
---
apiVersion: policy.linkerd.io/v1beta1
kind: ServerAuthorization
metadata:
  name: allow-gateway-to-application
  namespace: admissions
spec:
  server:
    name: application-service-server
  client:
    meshTLS:
      serviceAccounts:
        - name: api-gateway
          namespace: admissions
  requiredRoutes:
    - pathRegex: "/applications.*"
      methods: ["GET", "POST", "PUT", "PATCH", "DELETE"]
---
apiVersion: policy.linkerd.io/v1beta1
kind: Server
metadata:
  name: evaluation-service-server
  namespace: admissions
spec:
  podSelector:
    matchLabels:
      app: evaluation-service
  port: 8080
---
apiVersion: policy.linkerd.io/v1beta1
kind: ServerAuthorization
metadata:
  name: allow-app-to-evaluation
  namespace: admissions
spec:
  server:
    name: evaluation-service-server
  client:
    meshTLS:
      serviceAccounts:
        - name: application-service
          namespace: admissions
        - name: api-gateway
          namespace: admissions
  requiredRoutes:
    - pathRegex: "/(evaluations|interviews|scores)/.*"
      methods: ["GET", "POST", "PATCH"]
---
apiVersion: policy.linkerd.io/v1beta1
kind: Server
metadata:
  name: notification-service-server
  namespace: admissions
spec:
  podSelector:
    matchLabels:
      app: notification-service
  port: 8080
---
apiVersion: policy.linkerd.io/v1beta1
kind: ServerAuthorization
metadata:
  name: allow-services-to-notification
  namespace: admissions
spec:
  server:
    name: notification-service-server
  client:
    meshTLS:
      serviceAccounts:
        - name: application-service
          namespace: admissions
        - name: evaluation-service
          namespace: admissions
        - name: api-gateway
          namespace: admissions
  requiredRoutes:
    - pathRegex: "/(notifications|email|templates)/.*"
      methods: ["POST", "PUT"]
```

### Database and External Service Policies

```yaml
# platform/linkerd/server-policy-database.yaml
apiVersion: policy.linkerd.io/v1beta1
kind: Server
metadata:
  name: postgresql-server
  namespace: postgres
spec:
  podSelector:
    matchLabels:
      app: postgresql
  port: 5432
  proxyProtocol: "TCP"
---
apiVersion: policy.linkerd.io/v1beta1
kind: ServerAuthorization
metadata:
  name: allow-services-to-postgres
  namespace: postgres
spec:
  server:
    name: postgresql-server
  client:
    meshTLS:
      serviceAccounts:
        - name: application-service
          namespace: admissions
        - name: user-service
          namespace: admissions
        - name: evaluation-service
          namespace: admissions
        - name: notification-service
          namespace: admissions
```

## 5. Traffic Policies for Load Balancing

```yaml
# platform/linkerd/traffic-policy.yaml
apiVersion: policy.linkerd.io/v1alpha1
kind: HTTPRoute
metadata:
  name: application-service-route
  namespace: admissions
spec:
  parentRefs:
    - name: application-service
      kind: Service
      group: core
      port: 8080
  rules:
    - matches:
        - path:
            type: PathPrefix
            value: "/applications"
      backendRefs:
        - name: application-service
          port: 8080
          weight: 100
      # Circuit breaker equivalent
      timeouts:
        request: 30s
      retry:
        numRetries: 3
        backoff: 1s
---
apiVersion: policy.linkerd.io/v1alpha1
kind: HTTPRoute
metadata:
  name: evaluation-service-route
  namespace: admissions
spec:
  parentRefs:
    - name: evaluation-service
      kind: Service
      group: core
      port: 8080
  rules:
    - matches:
        - path:
            type: PathPrefix
            value: "/evaluations"
      backendRefs:
        - name: evaluation-service
          port: 8080
          weight: 100
      timeouts:
        request: 45s
      retry:
        numRetries: 2
        backoff: 2s
```

## 6. Linkerd vs Istio Feature Comparison

| Feature | Istio | Linkerd | MTN Implementation |
|---------|--------|---------|-------------------|
| **Automatic mTLS** | ✅ PeerAuthentication | ✅ Automatic | Both support |
| **Zero-config encryption** | ⚠️ Requires config | ✅ Out of the box | Linkerd simpler |
| **Authorization Policies** | ✅ AuthorizationPolicy | ✅ ServerAuthorization | Both equivalent |
| **Traffic Management** | ✅ Full featured | ⚠️ Basic | Istio more advanced |
| **Observability** | ✅ Comprehensive | ✅ Good | Both adequate |
| **Resource Usage** | ⚠️ Higher | ✅ Lower | Linkerd lighter |
| **Learning Curve** | ⚠️ Steep | ✅ Gentle | Linkerd easier |
| **Enterprise Features** | ✅ Full | ⚠️ Limited | Istio more complete |

## 7. Deployment Commands

```bash
# Apply namespace configuration
kubectl apply -f platform/linkerd/namespace-config.yaml

# Inject and deploy services
kubectl get deployment -n admissions -o yaml | \
  linkerd inject - | \
  kubectl apply -f -

# Apply server policies
kubectl apply -f platform/linkerd/server-policy-*.yaml

# Apply traffic policies  
kubectl apply -f platform/linkerd/traffic-policy.yaml

# Verify mTLS is working
linkerd edges -n admissions
linkerd stat -n admissions
```

## 8. Verification and Testing

```bash
# Check mTLS status
linkerd edges -n admissions

# Verify policies are working
linkerd authz -n admissions

# Test unauthorized access (should fail)
kubectl exec -n admissions deployment/api-gateway -- \
  curl -v http://evaluation-service.admissions.svc.cluster.local:8080/evaluations

# Check traffic encryption
linkerd tap -n admissions deployment/application-service
```

## 9. Monitoring and Observability

```bash
# Install Linkerd Viz dashboard
linkerd viz install | kubectl apply -f -

# Access dashboard
linkerd viz dashboard &

# Check service mesh metrics
linkerd viz stat -n admissions
linkerd viz top -n admissions
linkerd viz routes -n admissions
```

## 10. Key Differences from Istio

### Advantages of Linkerd:
1. **Simpler Configuration**: Less YAML, more conventions
2. **Automatic mTLS**: No manual PeerAuthentication needed
3. **Lower Resource Usage**: Lighter proxy footprint
4. **Easier Debugging**: Better observability out of the box
5. **Faster Startup**: Quicker proxy injection and startup

### Advantages of Istio:
1. **More Advanced Traffic Management**: VirtualServices, etc.
2. **Better Enterprise Integrations**: More mature ecosystem
3. **Advanced Security Features**: More granular policies
4. **Vendor Support**: Wider industry support

### Recommendation for MTN:
- **Use Linkerd if**: Simplicity and ease of operation are priorities
- **Use Istio if**: Advanced traffic management features are needed

## 11. Migration Strategy

If switching from Istio to Linkerd:

```bash
# 1. Remove Istio injection
kubectl label namespace admissions istio-injection-

# 2. Remove Istio policies
kubectl delete authorizationpolicy -n admissions --all
kubectl delete peerauthentication -n admissions --all
kubectl delete destinationrule -n admissions --all

# 3. Install Linkerd
linkerd install | kubectl apply -f -

# 4. Inject Linkerd
kubectl label namespace admissions linkerd.io/inject=enabled
kubectl rollout restart deployment -n admissions

# 5. Apply Linkerd policies
kubectl apply -f platform/linkerd/server-policy-*.yaml
```

## 12. Troubleshooting

```bash
# Check Linkerd health
linkerd check

# Debug proxy issues
linkerd proxy-log -n admissions deployment/application-service

# Check policy enforcement
linkerd policy -n admissions

# Analyze traffic
linkerd tap -n admissions deployment/api-gateway | head -20
```

---

**Note**: This Linkerd configuration provides equivalent security to the Istio setup but with simpler configuration and lower resource usage. Choose based on your operational preferences and requirements.