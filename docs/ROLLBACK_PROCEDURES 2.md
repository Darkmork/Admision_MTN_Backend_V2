# Rollback Procedures - MTN Admission System

## Overview

This document provides comprehensive rollback procedures for the MTN Admission System microservices. Our rollback strategy ensures rapid recovery from deployment issues while maintaining data integrity and user experience.

## Rollback Philosophy

### Core Principles
1. **Speed over Perfection:** Fast rollback is better than perfect rollback
2. **Automated First:** Prefer automated rollback triggers over manual intervention
3. **Data Preservation:** Never compromise data integrity during rollback
4. **User Experience:** Minimize user impact during rollback procedures
5. **Learn and Improve:** Every rollback is a learning opportunity

### Rollback Types
- **Automatic Rollback:** Triggered by metrics and health checks
- **Manual Emergency Rollback:** Human-initiated for critical issues
- **Scheduled Rollback:** Planned rollback during maintenance windows
- **Partial Rollback:** Rollback specific components while keeping others

## Automatic Rollback System

### Trigger Conditions

#### Health Check Failures
```yaml
health_check_triggers:
  readiness_failure:
    threshold: 50%  # 50% of pods failing
    duration: 2m
    action: immediate_rollback
    
  liveness_failure:
    threshold: 25%  # 25% of pods failing  
    duration: 1m
    action: immediate_rollback
    
  startup_failure:
    threshold: 75%  # 75% of pods failing to start
    duration: 5m
    action: immediate_rollback
```

#### Performance Metrics
```yaml
performance_triggers:
  error_rate:
    threshold: 0.05  # 5% error rate
    duration: 2m
    severity: critical
    action: immediate_rollback
    
  latency_p99:
    threshold: 2000  # 2 seconds
    duration: 3m  
    severity: critical
    action: immediate_rollback
    
  availability:
    threshold: 0.995  # 99.5% availability
    duration: 1m
    severity: critical
    action: immediate_rollback
```

#### Business Logic Failures
```yaml
business_triggers:
  authentication_failure:
    metric: login_success_rate
    threshold: 0.99  # 99% success rate
    duration: 1m
    action: immediate_rollback
    
  application_submission_failure:
    metric: application_submit_success_rate
    threshold: 0.98  # 98% success rate
    duration: 2m
    action: immediate_rollback
    
  document_upload_failure:
    metric: document_upload_failure_rate
    threshold: 0.02  # 2% failure rate
    duration: 1m
    action: immediate_rollback
```

### Automatic Rollback Implementation

#### Argo Rollouts Configuration
```yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: user-service
spec:
  strategy:
    canary:
      analysis:
        templates:
        - templateName: success-rate
        - templateName: latency-analysis  
        - templateName: error-rate
        args:
        - name: service-name
          value: user-service
      steps:
      - setWeight: 10
      - pause: {duration: 2m}
      - analysis:
          templates:
          - templateName: success-rate
          args:
          - name: service-name
            value: user-service
      abortScaleDownDelaySeconds: 30
      scaleDownDelaySeconds: 30
      autoRollbackOnFailure: true
```

#### Analysis Template Example
```yaml
apiVersion: argoproj.io/v1alpha1
kind: AnalysisTemplate
metadata:
  name: success-rate
spec:
  args:
  - name: service-name
  metrics:
  - name: success-rate
    interval: 30s
    count: 5
    successCondition: result[0] >= 0.99
    failureLimit: 2
    inconclusiveLimit: 1
    provider:
      prometheus:
        address: http://prometheus:9090
        query: |
          sum(rate(http_requests_total{
            job="{{args.service-name}}",
            status!~"5.."
          }[2m])) /
          sum(rate(http_requests_total{
            job="{{args.service-name}}"
          }[2m]))
```

## Manual Rollback Procedures

### Emergency Rollback Scripts

#### 1. Immediate Traffic Cutoff (< 30 seconds)
```bash
#!/bin/bash
# immediate-rollback.sh - Emergency traffic cutoff
set -euo pipefail

SERVICE_NAME=${1:?"Service name required"}
NAMESPACE=${2:-production}

echo "🚨 EMERGENCY ROLLBACK: $SERVICE_NAME"
echo "Timestamp: $(date -Iseconds)"

# Log the emergency
curl -X POST http://logging-service:8080/api/emergency-log \
  -H "Content-Type: application/json" \
  -d "{
    \"event\": \"emergency_rollback\",
    \"service\": \"$SERVICE_NAME\",
    \"namespace\": \"$NAMESPACE\",
    \"timestamp\": \"$(date -Iseconds)\",
    \"triggered_by\": \"${USER:-system}\"
  }"

# Immediate traffic cutoff via feature flags
echo "🔄 Cutting traffic via feature flags..."
curl -X PATCH http://feature-flag-service:8080/api/flags/${SERVICE_NAME}_routing \
  -H "Authorization: Bearer $EMERGENCY_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "enabled": false,
    "rollout_percentage": 0,
    "canary_percentage": 0,
    "reason": "emergency_rollback"
  }' || echo "⚠️  Feature flag cutoff failed, continuing with Argo rollback"

# Verify traffic cutoff
sleep 5
CURRENT_TRAFFIC=$(curl -s http://feature-flag-service:8080/api/flags/${SERVICE_NAME}_routing | jq -r '.rollout_percentage')
if [ "$CURRENT_TRAFFIC" = "0" ]; then
  echo "✅ Traffic successfully cut to 0%"
else
  echo "❌ Traffic cutoff failed, current: ${CURRENT_TRAFFIC}%"
fi

echo "🎯 Emergency rollback completed in $SECONDS seconds"
```

#### 2. Argo Rollouts Rollback (< 2 minutes)
```bash
#!/bin/bash
# argo-rollback.sh - Argo Rollouts based rollback
set -euo pipefail

SERVICE_NAME=${1:?"Service name required"}
NAMESPACE=${2:-production}
TARGET_REVISION=${3:-previous}

echo "🔄 ARGO ROLLOUTS ROLLBACK: $SERVICE_NAME"

# Abort current rollout
echo "Aborting current rollout..."
kubectl argo rollouts abort $SERVICE_NAME -n $NAMESPACE || echo "No active rollout to abort"

# Get current and target revisions
CURRENT_REV=$(kubectl argo rollouts get rollout $SERVICE_NAME -n $NAMESPACE --no-color | grep "Revision:" | head -1 | awk '{print $2}')
echo "Current revision: $CURRENT_REV"

# Rollback to target revision
if [ "$TARGET_REVISION" = "previous" ]; then
  echo "Rolling back to previous revision..."
  kubectl argo rollouts undo $SERVICE_NAME -n $NAMESPACE
else
  echo "Rolling back to revision: $TARGET_REVISION"
  kubectl argo rollouts undo $SERVICE_NAME -n $NAMESPACE --to-revision=$TARGET_REVISION
fi

# Monitor rollback progress
echo "Monitoring rollback progress..."
timeout 300 kubectl argo rollouts wait $SERVICE_NAME -n $NAMESPACE --for=condition=Progressing=false

# Verify rollback
NEW_REV=$(kubectl argo rollouts get rollout $SERVICE_NAME -n $NAMESPACE --no-color | grep "Revision:" | head -1 | awk '{print $2}')
echo "Rolled back from revision $CURRENT_REV to $NEW_REV"

echo "✅ Argo rollback completed"
```

#### 3. Helm-based Rollback (< 5 minutes)
```bash
#!/bin/bash
# helm-rollback.sh - Helm release rollback
set -euo pipefail

SERVICE_NAME=${1:?"Service name required"}
NAMESPACE=${2:-production}
REVISION=${3:-0}  # 0 means previous revision

echo "🎡 HELM ROLLBACK: $SERVICE_NAME"

# Get current release info
echo "Getting current release information..."
helm status $SERVICE_NAME -n $NAMESPACE

# List recent revisions
echo "Recent revisions:"
helm history $SERVICE_NAME -n $NAMESPACE --max 5

# Perform rollback
if [ "$REVISION" = "0" ]; then
  echo "Rolling back to previous revision..."
  helm rollback $SERVICE_NAME -n $NAMESPACE
else
  echo "Rolling back to revision: $REVISION"
  helm rollback $SERVICE_NAME $REVISION -n $NAMESPACE
fi

# Wait for rollback completion
echo "Waiting for rollback completion..."
kubectl rollout status deployment/$SERVICE_NAME -n $NAMESPACE --timeout=300s

# Verify rollback
helm status $SERVICE_NAME -n $NAMESPACE

echo "✅ Helm rollback completed"
```

### Multi-Service Coordinated Rollback

```bash
#!/bin/bash
# coordinated-rollback.sh - Multi-service rollback
set -euo pipefail

SERVICES=${1:?"Comma-separated service names required"}
NAMESPACE=${2:-production}
METHOD=${3:-feature-flags}  # feature-flags, argo, helm

echo "🔄 COORDINATED ROLLBACK"
echo "Services: $SERVICES"
echo "Method: $METHOD"

IFS=',' read -ra SERVICE_ARRAY <<< "$SERVICES"

# Phase 1: Traffic cutoff (if using feature flags)
if [ "$METHOD" = "feature-flags" ]; then
  echo "Phase 1: Cutting traffic for all services..."
  for service in "${SERVICE_ARRAY[@]}"; do
    echo "  Cutting traffic for $service"
    curl -X PATCH http://feature-flag-service:8080/api/flags/${service}_routing \
      -H "Authorization: Bearer $EMERGENCY_API_KEY" \
      -H "Content-Type: application/json" \
      -d '{"enabled": false, "rollout_percentage": 0}' &
  done
  wait
  echo "  All traffic cut"
fi

# Phase 2: Service rollbacks
echo "Phase 2: Rolling back services..."
for service in "${SERVICE_ARRAY[@]}"; do
  echo "  Rolling back $service"
  case $METHOD in
    "argo")
      kubectl argo rollouts undo $service -n $NAMESPACE &
      ;;
    "helm")
      helm rollback $service -n $NAMESPACE &
      ;;
    "feature-flags")
      echo "  Traffic already cut for $service"
      ;;
  esac
done

if [ "$METHOD" != "feature-flags" ]; then
  wait
fi

# Phase 3: Validation
echo "Phase 3: Validating rollbacks..."
./scripts/validate-rollback.sh "$SERVICES" $NAMESPACE

echo "✅ Coordinated rollback completed"
```

## Rollback Validation

### Automated Validation Scripts

#### Health Check Validation
```bash
#!/bin/bash
# validate-health.sh - Validate service health after rollback
set -euo pipefail

SERVICE_NAME=${1:?"Service name required"}
NAMESPACE=${2:-production}
TIMEOUT=${3:-300}

echo "🏥 HEALTH VALIDATION: $SERVICE_NAME"

# Get service endpoints
SERVICE_IP=$(kubectl get svc $SERVICE_NAME -n $NAMESPACE -o jsonpath='{.spec.clusterIP}')
SERVICE_PORT=$(kubectl get svc $SERVICE_NAME -n $NAMESPACE -o jsonpath='{.spec.ports[0].port}')

echo "Service endpoint: http://$SERVICE_IP:$SERVICE_PORT"

# Health check endpoints to test
HEALTH_ENDPOINTS=(
  "/actuator/health"
  "/actuator/health/liveness" 
  "/actuator/health/readiness"
  "/actuator/info"
)

# Test each endpoint
for endpoint in "${HEALTH_ENDPOINTS[@]}"; do
  echo -n "Testing $endpoint... "
  
  for i in {1..10}; do
    if curl -sf "http://$SERVICE_IP:$SERVICE_PORT$endpoint" >/dev/null 2>&1; then
      echo "✅ OK"
      break
    fi
    
    if [ $i -eq 10 ]; then
      echo "❌ FAILED after 10 attempts"
      exit 1
    fi
    
    sleep 5
  done
done

echo "✅ All health checks passed"
```

#### Performance Validation
```bash
#!/bin/bash
# validate-performance.sh - Validate performance after rollback
set -euo pipefail

SERVICE_NAME=${1:?"Service name required"}
NAMESPACE=${2:-production}

echo "📊 PERFORMANCE VALIDATION: $SERVICE_NAME"

# Wait for metrics to stabilize
echo "Waiting for metrics to stabilize..."
sleep 60

# Check error rate
echo -n "Checking error rate... "
ERROR_RATE=$(curl -s "http://prometheus:9090/api/v1/query?query=sum(rate(http_requests_total{job=\"$SERVICE_NAME\",status=~\"5..\"}[5m]))/sum(rate(http_requests_total{job=\"$SERVICE_NAME\"}[5m]))" | jq -r '.data.result[0].value[1]')

if (( $(echo "$ERROR_RATE < 0.01" | bc -l) )); then
  echo "✅ OK ($ERROR_RATE)"
else
  echo "❌ HIGH ($ERROR_RATE)"
  exit 1
fi

# Check latency
echo -n "Checking P99 latency... "
P99_LATENCY=$(curl -s "http://prometheus:9090/api/v1/query?query=histogram_quantile(0.99,sum(rate(http_request_duration_seconds_bucket{job=\"$SERVICE_NAME\"}[5m])) by (le))" | jq -r '.data.result[0].value[1]')

if (( $(echo "$P99_LATENCY < 1.0" | bc -l) )); then
  echo "✅ OK (${P99_LATENCY}s)"
else
  echo "❌ HIGH (${P99_LATENCY}s)"
  exit 1
fi

# Check throughput
echo -n "Checking throughput... "
THROUGHPUT=$(curl -s "http://prometheus:9090/api/v1/query?query=sum(rate(http_requests_total{job=\"$SERVICE_NAME\"}[5m]))" | jq -r '.data.result[0].value[1]')
echo "✅ Current throughput: $THROUGHPUT req/s"

echo "✅ Performance validation passed"
```

#### Functional Testing
```bash
#!/bin/bash
# validate-functional.sh - Validate core functionality after rollback
set -euo pipefail

SERVICE_NAME=${1:?"Service name required"}
NAMESPACE=${2:-production}

echo "🔧 FUNCTIONAL VALIDATION: $SERVICE_NAME"

SERVICE_URL="http://$SERVICE_NAME.$NAMESPACE.svc.cluster.local:8080"

case $SERVICE_NAME in
  "user-service")
    echo "Testing user authentication..."
    curl -sf "$SERVICE_URL/api/auth/health" || exit 1
    
    echo "Testing user lookup..."
    curl -sf "$SERVICE_URL/api/users/health" || exit 1
    ;;
    
  "application-service")
    echo "Testing application endpoints..."
    curl -sf "$SERVICE_URL/api/applications/health" || exit 1
    
    echo "Testing document upload..."
    curl -sf "$SERVICE_URL/api/documents/health" || exit 1
    ;;
    
  "evaluation-service")
    echo "Testing evaluation endpoints..."
    curl -sf "$SERVICE_URL/api/evaluations/health" || exit 1
    
    echo "Testing interview scheduling..."
    curl -sf "$SERVICE_URL/api/interviews/health" || exit 1
    ;;
    
  "notification-service")
    echo "Testing notification endpoints..."
    curl -sf "$SERVICE_URL/api/notifications/health" || exit 1
    
    echo "Testing email service..."
    curl -sf "$SERVICE_URL/api/email/health" || exit 1
    ;;
esac

echo "✅ Functional validation passed"
```

## Database Rollback Procedures

### Database Change Categories

#### 1. Schema-Only Changes (Safe)
- Adding new columns with defaults
- Adding new indexes
- Adding new tables
- **Rollback:** Usually no action needed

#### 2. Data Migration Changes (Caution)
- Data transformations
- Column renames
- Data type changes
- **Rollback:** Requires data restoration

#### 3. Breaking Changes (High Risk)
- Column deletions
- Table deletions
- Constraint additions
- **Rollback:** Requires full backup restoration

### Database Rollback Scripts

#### Safe Schema Rollback
```sql
-- rollback-schema.sql
-- Safe rollback for additive schema changes

-- Remove newly added indexes
DROP INDEX IF EXISTS idx_applications_enhanced_search;
DROP INDEX IF EXISTS idx_users_performance_opt;

-- Remove newly added columns (if safe)
-- Note: Only if no data has been written to these columns
ALTER TABLE applications DROP COLUMN IF EXISTS enhanced_metadata;
ALTER TABLE users DROP COLUMN IF EXISTS last_activity_timestamp;

-- Remove newly added tables (if safe)
DROP TABLE IF EXISTS user_activity_log;
DROP TABLE IF EXISTS performance_metrics;

-- Commit changes
COMMIT;
```

#### Data Restoration Rollback
```bash
#!/bin/bash
# restore-database.sh - Restore database from backup
set -euo pipefail

DB_NAME=${1:?"Database name required"}
BACKUP_TIMESTAMP=${2:?"Backup timestamp required"}
NAMESPACE=${3:-production}

echo "🗄️  DATABASE RESTORE: $DB_NAME"
echo "Backup timestamp: $BACKUP_TIMESTAMP"

# Find the backup file
BACKUP_FILE="/backups/postgres/${DB_NAME}_${BACKUP_TIMESTAMP}.sql.gz"

if [ ! -f "$BACKUP_FILE" ]; then
  echo "❌ Backup file not found: $BACKUP_FILE"
  exit 1
fi

# Confirm restore
echo "⚠️  This will restore the database to $BACKUP_TIMESTAMP"
read -p "Continue? (yes/no): " confirm
if [ "$confirm" != "yes" ]; then
  echo "Restore cancelled"
  exit 1
fi

# Stop application services
echo "Stopping application services..."
kubectl scale deployment user-service application-service evaluation-service notification-service --replicas=0 -n $NAMESPACE

# Wait for pods to terminate
kubectl wait --for=delete pod -l app.kubernetes.io/name=user-service -n $NAMESPACE --timeout=300s
kubectl wait --for=delete pod -l app.kubernetes.io/name=application-service -n $NAMESPACE --timeout=300s
kubectl wait --for=delete pod -l app.kubernetes.io/name=evaluation-service -n $NAMESPACE --timeout=300s
kubectl wait --for=delete pod -l app.kubernetes.io/name=notification-service -n $NAMESPACE --timeout=300s

# Restore database
echo "Restoring database..."
gunzip -c "$BACKUP_FILE" | kubectl exec -i postgresql-0 -n $NAMESPACE -- psql -U postgres -d $DB_NAME

# Verify restore
echo "Verifying restore..."
RESTORED_COUNT=$(kubectl exec postgresql-0 -n $NAMESPACE -- psql -U postgres -d $DB_NAME -t -c "SELECT COUNT(*) FROM applications;")
echo "Restored record count: $RESTORED_COUNT"

# Restart application services
echo "Restarting application services..."
kubectl scale deployment user-service application-service evaluation-service notification-service --replicas=2 -n $NAMESPACE

# Wait for services to be ready
kubectl wait --for=condition=available deployment/user-service -n $NAMESPACE --timeout=300s
kubectl wait --for=condition=available deployment/application-service -n $NAMESPACE --timeout=300s
kubectl wait --for=condition=available deployment/evaluation-service -n $NAMESPACE --timeout=300s
kubectl wait --for=condition=available deployment/notification-service -n $NAMESPACE --timeout=300s

echo "✅ Database restore completed"
```

## Rollback Testing

### Chaos Engineering for Rollback Validation

#### Automated Rollback Testing
```yaml
# chaos-rollback-test.yaml - Litmus Chaos experiment
apiVersion: litmuschaos.io/v1alpha1
kind: ChaosExperiment
metadata:
  name: rollback-validation-test
spec:
  definition:
    scope: Cluster
    permissions:
      - apiGroups: [""]
        resources: ["pods"]
        verbs: ["create", "delete", "get", "list"]
    image: "litmuschaos/go-runner:latest"
    args:
      - -c
      - ./experiments -name rollback-validation
    command:
      - /bin/bash
    env:
      - name: TOTAL_CHAOS_DURATION
        value: '300'
      - name: RAMP_TIME
        value: '10'
      - name: TARGET_SERVICE
        value: 'user-service'
      - name: ROLLBACK_METHOD
        value: 'feature-flags'
```

#### Monthly Rollback Drills
```bash
#!/bin/bash
# rollback-drill.sh - Monthly rollback drill
set -euo pipefail

echo "🎯 MONTHLY ROLLBACK DRILL"
echo "Date: $(date)"

DRILL_SERVICE="user-service"
DRILL_NAMESPACE="staging"

# 1. Deploy a problematic version
echo "Phase 1: Deploying problematic version..."
kubectl set image deployment/$DRILL_SERVICE $DRILL_SERVICE=mtn-org/$DRILL_SERVICE:problematic -n $DRILL_NAMESPACE

# 2. Wait for automatic rollback to trigger
echo "Phase 2: Waiting for automatic rollback..."
sleep 300

# 3. Validate rollback occurred
echo "Phase 3: Validating automatic rollback..."
CURRENT_IMAGE=$(kubectl get deployment $DRILL_SERVICE -n $DRILL_NAMESPACE -o jsonpath='{.spec.template.spec.containers[0].image}')
if [[ "$CURRENT_IMAGE" == *"problematic"* ]]; then
  echo "❌ Automatic rollback failed"
  
  # 4. Test manual rollback
  echo "Phase 4: Testing manual rollback..."
  ./scripts/emergency-rollback.sh $DRILL_SERVICE $DRILL_NAMESPACE
else
  echo "✅ Automatic rollback successful"
fi

# 5. Validate system health
echo "Phase 5: Validating system health..."
./scripts/validate-rollback.sh $DRILL_SERVICE $DRILL_NAMESPACE

# 6. Generate drill report
echo "Phase 6: Generating drill report..."
cat > "rollback-drill-$(date +%Y%m%d).md" << EOF
# Rollback Drill Report - $(date)

## Summary
- **Service:** $DRILL_SERVICE
- **Environment:** $DRILL_NAMESPACE
- **Drill Duration:** $SECONDS seconds

## Results
- Automatic rollback: $([ "$CURRENT_IMAGE" != *"problematic"* ] && echo "✅ PASS" || echo "❌ FAIL")
- System health validation: ✅ PASS
- Recovery time: $SECONDS seconds

## Action Items
- [ ] Review automatic rollback triggers
- [ ] Update rollback documentation
- [ ] Schedule next drill
EOF

echo "✅ Rollback drill completed"
```

## Incident Response Integration

### Rollback Decision Matrix

| Severity | Impact | Response Time | Rollback Method | Approval Required |
|----------|---------|---------------|-----------------|-------------------|
| P0 | Production Down | < 5 minutes | Feature Flags | None |
| P1 | Major Degradation | < 15 minutes | Argo Rollouts | On-call Manager |
| P2 | Minor Issues | < 1 hour | Scheduled | Tech Lead |
| P3 | Non-critical | < 4 hours | Next Release | Product Owner |

### Incident Communication Template

```markdown
# Incident: Production Rollback Initiated

**Incident ID:** INC-2024-0123
**Severity:** P1
**Status:** In Progress
**Start Time:** 2024-01-15 14:30:00 UTC
**Duration:** 15 minutes

## Impact
- Service: user-service
- Users Affected: ~500 users
- Functionality: Authentication failures

## Actions Taken
1. 14:30 - Automatic rollback triggered due to high error rate
2. 14:32 - Traffic cut via feature flags (0% to new version)
3. 14:35 - Argo Rollouts rollback initiated
4. 14:40 - Service health validated
5. 14:45 - Incident resolved

## Root Cause
Database connection pool misconfiguration in v2.4.1

## Next Steps
- [ ] Post-incident review scheduled
- [ ] Fix deployed in v2.4.2
- [ ] Monitoring alerts tuned
```

This comprehensive rollback procedure ensures rapid recovery from deployment issues while maintaining system integrity and minimizing user impact.