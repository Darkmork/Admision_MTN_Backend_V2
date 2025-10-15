# Plan de Limpieza de Archivos .MD

## Total encontrados: 105 archivos

## ✅ MANTENER (15 archivos esenciales)

### Documentación Principal (5)
- `CLAUDE.md` - Instrucciones principales del proyecto ✅
- `README.md` - Documentación principal ✅
- `ARCHITECTURE.md` - Arquitectura del sistema ✅
- `API_DOCUMENTATION.md` - Documentación de API ✅
- `README_DEVOPS.md` - DevOps y despliegue ✅

### Documentación de Agentes Claude (13 en .claude/agents/)
- Todos los agentes en `.claude/agents/*.md` ✅ (necesarios para Claude Code)

### Documentación Reciente de Reorganización (3)
- `LIMPIEZA_RESUMEN.md` - Resumen de limpieza ✅
- `REORGANIZACION_COMPLETA.md` - Resumen de reorganización ✅
- `REORGANIZATION_PLAN.md` - Plan de reorganización ✅

**Total a mantener: ~21 archivos**

---

## ❌ ELIMINAR (84 archivos obsoletos)

### 1. Documentación Obsoleta de Features Implementadas
- `CSRF_ENDPOINTS.md`
- `CSRF_IMPLEMENTATION.md`
- `CSRF_QUICK_START.md`
- `CREDENTIAL_ENCRYPTION.md`
- `LOCATION_FIELDS_IMPLEMENTATION.md`
- `LOCATION_FIELDS_TESTING_GUIDE.md`
- `guardian-validation-integration.md`
- `SECURITY_CREDENTIALS_UPDATE.md`
- `SECURITY_SETUP.md`
- `SECURITY_VARIABLES.md`
- `EMAIL_SETUP.md`

### 2. Reportes de QA Antiguos
- `ANALISIS_EXHAUSTIVO_QA.md`
- `QA_REPORT.md`
- `QA_TESTING_REPORT.md`
- `QA_TEST_FAILURES_COMPLETE_ANALYSIS.md`
- `QA_TEST_REPORT.md`
- `QA_TEST_RESOLUTION_PLAN.md`
- `PRODUCTION_IMPROVEMENTS_VERIFICATION.md`
- `reports/forms_audit.md`
- `reports/verify_commands.md`
- `reports/verify_supporter_guardian.md`

### 3. Documentación de Circuit Breakers (ya implementados)
- `CIRCUIT_BREAKERS_IMPLEMENTACION_RESUMEN.md`
- `CIRCUIT_BREAKER_CATEGORIES.md`
- `CIRCUIT_BREAKER_TEST_PLAN.md`
- `IMPLEMENTACION_COMPLETA_CIRCUIT_BREAKERS.md`
- `CACHE_INVALIDATION_TEST_PLAN.md`

### 4. Deployment Temporales
- `DEPLOYMENT_STATUS.md`
- `DEPLOYMENT_SUMMARY.md`
- `CONFIGURE_BACKEND_VARS.md`
- `LOCKFILE_FIX_SUMMARY.md`
- `LOGIN_TIMEOUT_INVESTIGATION.md`
- `MANUAL_DB_RESTORE.md`
- `MITIGATION_3_SUMMARY.md`
- `POSTGRESQL_SSL_GUIDE.md`
- `platform/DEPLOYMENT_SUMMARY.md`

### 5. Summaries y Fixes Antiguos
- `FIXES_APPLIED.md`
- `IMPLEMENTATION_SUMMARY.md`
- `PREFLIGHT_SUMMARY.md`
- `PR_BODY.md`
- `PR_SUPPORTER_GUARDIAN.md`
- `USUARIOS_PRUEBA.md`
- `PLAN_TRABAJO_BACKEND.md`

### 6. Documentación de Spring Boot (no usada)
- `MICROSERVICES_GUIDE.md` (Spring Boot obsoleto)
- `platform/api-gateway/README.md`
- `platform/eureka-server/README.md`
- `platform/linkerd/mtls-alternative.md`
- Todo `platform/runbooks/` (Spring Boot)

### 7. Docs Antiguos
- `docs/CI_CD_STRATEGY.md`
- `docs/REDIS_MIGRATION_PLAN.md`
- `docs/RELEASE_STRATEGY.md`
- `docs/ROLLBACK_PROCEDURES.md`
- `docs/admin_secretary_implementation_checklist.md`
- `docs/admin_secretary_user_stories_analysis.md`
- `docs/data_access_rules.md`
- `docs/data_ownership.md`
- `docs/data_strategy.md`
- `docs/deployment_guide.md`
- `docs/devops/post-deploy-report.md`
- `docs/diagrams/flows/apoderado-workflow/README.md`
- `docs/domain_events.md`
- `docs/domain_map.md`
- `docs/endpoints_inventory.md`
- `docs/implementation_summary.md`
- `docs/observability_strategy.md`
- `docs/pm-decision.md`
- `docs/urls.md`

### 8. Database Optimization (ya aplicado)
- `database_optimization/OPTIMIZATION_SUMMARY.md`
- `database_optimization/README.md`
- `database_optimization/optimization_validation_report.md`

### 9. Reports CI/CD Antiguos
- `reports/CICD_IMPROVEMENTS_SUMMARY.md`
- `reports/CI_AUDIT_REPORT.md`
- `reports/SECRETS_MANAGEMENT_GUIDE.md`
- `reports/SECURITY_FIXES_SUMMARY.md`

### 10. Backend Common y Backups
- `backend-common/README-otel.md`
- `backup-system/README.md`
- `backup-system/RUNBOOK.md`
- `backups/README_BACKUP.md`
- `configs/archive/README.md`

### 11. AGENTS.md duplicado
- `AGENTS.md` (duplicado, ya tenemos .claude/agents/)

---

## 🚀 Comando de Eliminación

```bash
# Eliminar todos los archivos obsoletos (84 archivos)
cd "/Users/jorgegangale/Library/Mobile Documents/com~apple~CloudDocs/Proyectos/Admision_MTN/Admision_MTN_backend"

# Eliminar en bloques
rm -f CSRF_*.md CREDENTIAL_ENCRYPTION.md LOCATION_FIELDS_*.md guardian-validation-integration.md
rm -f SECURITY_*.md EMAIL_SETUP.md
rm -f ANALISIS_EXHAUSTIVO_QA.md QA_*.md PRODUCTION_IMPROVEMENTS_VERIFICATION.md
rm -f CIRCUIT_*.md IMPLEMENTACION_COMPLETA_CIRCUIT_BREAKERS.md CACHE_INVALIDATION_TEST_PLAN.md
rm -f DEPLOYMENT_*.md CONFIGURE_BACKEND_VARS.md LOCKFILE_FIX_SUMMARY.md LOGIN_TIMEOUT_INVESTIGATION.md
rm -f MANUAL_DB_RESTORE.md MITIGATION_3_SUMMARY.md POSTGRESQL_SSL_GUIDE.md
rm -f FIXES_APPLIED.md IMPLEMENTATION_SUMMARY.md PREFLIGHT_SUMMARY.md PR_*.md USUARIOS_PRUEBA.md
rm -f PLAN_TRABAJO_BACKEND.md MICROSERVICES_GUIDE.md AGENTS.md
rm -rf platform/
rm -rf docs/
rm -rf reports/
rm -rf database_optimization/
rm -f backend-common/README-otel.md
rm -f backup-system/README.md backup-system/RUNBOOK.md
rm -f backups/README_BACKUP.md
rm -f configs/archive/README.md
```

---

## ✅ Resultado Final

**Antes:** 105 archivos .md
**Después:** ~21 archivos .md
**Eliminados:** 84 archivos obsoletos
