# Plan de Organización de Archivos Sueltos - Rama `limpieza`

## 📊 Análisis Completo

**Total archivos sueltos en raíz:** ~90 archivos
**Objetivo:** Organizar en estructura modular sin perder funcionalidad

---

## 📁 Clasificación de Archivos

### ✅ MANTENER EN RAÍZ (15 archivos esenciales)

#### Documentación Principal (7)
- `CLAUDE.md` ✅
- `README.md` ✅
- `README_DEVOPS.md` ✅
- `ARCHITECTURE.md` ✅
- `API_DOCUMENTATION.md` ✅
- `LIMPIEZA_RESUMEN.md` ✅
- `REORGANIZACION_COMPLETA.md` ✅
- `REORGANIZATION_PLAN.md` ✅
- `MD_FILES_CLEANUP_PLAN.md` ✅

#### Configuración Node.js (3)
- `package.json` ✅
- `package-lock.json` ✅
- `eslint.config.js` ✅

#### Configuración Railway/Deployment (5)
- `nixpacks.toml` ✅ (Railway build config)
- `railway.json` ✅ (Railway service config)
- `Procfile` ✅ (Railway start command)
- `Makefile` ✅ (Build automation)

#### Directorios Actuales (5)
- `node_modules/` ✅
- `uploads/` ✅
- `services/` ✅ (ya organizado)
- `api-gateway/` ✅ (ya organizado)
- `database/` ✅ (ya organizado)
- `scripts/` ✅ (ya organizado)
- `shared/` ✅ (existe pero vacío)

---

## 🔄 MOVER A `scripts/` (62 archivos)

### scripts/deployment/ (18 archivos)
Scripts de despliegue y configuración de Railway:
- `start-backend.sh`
- `start-microservices.sh`
- `start-microservices-only.sh`
- `start-railway.js`
- `server-railway.js`
- `server-railway-complete.js`
- `server-railway-template.js`
- `setup-railway-db.sh`
- `verify-railway-db.sh`
- `submit-railway-ticket.sh`
- `build-railway-server.js`
- `get-railway-db-url.js`
- `print-db-url.sh`
- `simple-gateway.js`
- `create-admission-flows.js`
- `create-admission-via-api.sh`
- `setup-security.sh`
- `enable-postgresql-ssl.sh`

### scripts/backup/ (7 archivos)
Scripts de backup y restauración:
- `setup-backup-system.sh`
- `backup-to-gdrive.sh`
- `backup-config.txt`
- `verify-and-restore-backup.sh`
- `restore-pre-ssl-backup.sh`
- `RESTORE_DB_SIMPLE.sh`

### scripts/testing/ (24 archivos)
Scripts de testing y QA:
- `qa-tests.sh`
- `qa-comprehensive-test.sh`
- `qa-test-suite.js`
- `integration-tests.sh`
- `comprehensive-health-check.sh`
- `check-users.sh`
- `apply-remaining-qa-fixes.sh`
- `fix-qa-test-failures.sh`
- `validate-optimizations.sh`
- `test-api-response-format.sh`
- `test-bcrypt-performance.js`
- `test-credential-encryption.sh`
- `test-csrf.sh`
- `test-edit-flow.sh`
- `test-email.js`
- `test-interviews-endpoint.js`
- `test-login.js`
- `test_location_fields.json`
- `test-address-parsing.js`

### scripts/analysis/ (2 archivos)
Scripts de análisis del proyecto:
- `analyze-cleanup.sh` (ya usado, histórico)
- `execute-cleanup.sh` (ya usado, histórico)

### scripts/utility/ (11 archivos)
Utilidades varias:
- `generate-admin-password.js`
- `migrate-to-winston.js`
- `gmail-config.sh`
- `translations.js`
- `guardian-validation-middleware.js`
- `logger.js`

---

## 🗑️ ELIMINAR ARCHIVOS OBSOLETOS (13 archivos)

### Archivos Maven (obsoletos, Spring Boot no se usa)
- `mvnw`
- `mvnw.cmd`
- `pom.xml` (si existe en raíz)

### Archivos de backup redundantes
- `local-gateway.conf.bak2` (ya hay backup)

### Servicios duplicados o no usados
- `mock-teacher-service.js` (duplicado con user-service)

### Directorios vacíos o innecesarios
- `backend-common/` (si está vacío)
- `init-scripts/` (si está obsoleto)
- `migrations/` (si no se usa flyway/liquibase)
- `patches/` (parches temporales ya aplicados)
- `template-repo/` (templates obsoletos)
- `slo/` (SLO docs si no se usan)
- `tools/` (herramientas obsoletas)
- `tests/` (si está vacío o duplicado con scripts/testing/)

---

## 🚀 Comandos de Reorganización

### Fase 1: Crear subdirectorios en scripts/
```bash
cd "/Users/jorgegangale/Library/Mobile Documents/com~apple~CloudDocs/Proyectos/Admision_MTN/Admision_MTN_backend"

mkdir -p scripts/backup
mkdir -p scripts/testing
mkdir -p scripts/analysis
mkdir -p scripts/utility
```

### Fase 2: Mover scripts de deployment
```bash
mv start-backend.sh scripts/deployment/
mv start-microservices.sh scripts/deployment/
mv start-microservices-only.sh scripts/deployment/
mv start-railway.js scripts/deployment/
mv server-railway*.js scripts/deployment/
mv setup-railway-db.sh scripts/deployment/
mv verify-railway-db.sh scripts/deployment/
mv submit-railway-ticket.sh scripts/deployment/
mv build-railway-server.js scripts/deployment/
mv get-railway-db-url.js scripts/deployment/
mv print-db-url.sh scripts/deployment/
mv simple-gateway.js scripts/deployment/
mv create-admission-*.* scripts/deployment/
mv setup-security.sh scripts/deployment/
mv enable-postgresql-ssl.sh scripts/deployment/
```

### Fase 3: Mover scripts de backup
```bash
mv setup-backup-system.sh scripts/backup/
mv backup-to-gdrive.sh scripts/backup/
mv backup-config.txt scripts/backup/
mv verify-and-restore-backup.sh scripts/backup/
mv restore-pre-ssl-backup.sh scripts/backup/
mv RESTORE_DB_SIMPLE.sh scripts/backup/
```

### Fase 4: Mover scripts de testing
```bash
mv qa-*.* scripts/testing/
mv integration-tests.sh scripts/testing/
mv comprehensive-health-check.sh scripts/testing/
mv check-users.sh scripts/testing/
mv apply-remaining-qa-fixes.sh scripts/testing/
mv fix-qa-test-failures.sh scripts/testing/
mv validate-optimizations.sh scripts/testing/
mv test-*.* scripts/testing/
```

### Fase 5: Mover scripts de análisis
```bash
mv analyze-cleanup.sh scripts/analysis/
mv execute-cleanup.sh scripts/analysis/
```

### Fase 6: Mover utilidades
```bash
mv generate-admin-password.js scripts/utility/
mv migrate-to-winston.js scripts/utility/
mv gmail-config.sh scripts/utility/
mv translations.js scripts/utility/
mv guardian-validation-middleware.js shared/middleware/
mv logger.js shared/utils/
```

### Fase 7: Eliminar archivos obsoletos
```bash
rm -f mvnw mvnw.cmd
rm -f local-gateway.conf.bak2
rm -f mock-teacher-service.js
rm -rf backend-common/ init-scripts/ migrations/ patches/ template-repo/ slo/ tools/
rm -rf tests/ # Solo si está vacío o duplicado
```

---

## ✅ Estructura Final Esperada

```
Admision_MTN_backend/
├── CLAUDE.md                          ✅ Documentación
├── README.md
├── README_DEVOPS.md
├── ARCHITECTURE.md
├── API_DOCUMENTATION.md
├── LIMPIEZA_RESUMEN.md
├── REORGANIZACION_COMPLETA.md
├── REORGANIZATION_PLAN.md
├── MD_FILES_CLEANUP_PLAN.md
│
├── package.json                       ✅ Config Node.js
├── package-lock.json
├── eslint.config.js
├── nixpacks.toml                      ✅ Config Railway
├── railway.json
├── Procfile
├── Makefile
│
├── services/                          ✅ Microservicios
│   ├── user-service/src/
│   ├── application-service/src/
│   ├── evaluation-service/src/
│   ├── notification-service/src/
│   ├── dashboard-service/src/
│   └── guardian-service/src/
│
├── api-gateway/                       ✅ Gateway NGINX
│   └── conf/
│
├── database/                          ✅ Base de datos
│   └── schema/
│
├── scripts/                           ✅ Scripts organizados
│   ├── deployment/
│   │   ├── start-microservices-gateway.sh
│   │   ├── railway-smoke-tests.sh
│   │   ├── start-backend.sh
│   │   ├── start-railway.js
│   │   ├── setup-railway-db.sh
│   │   └── ... (18 archivos)
│   ├── backup/
│   │   ├── setup-backup-system.sh
│   │   ├── restore-pre-ssl-backup.sh
│   │   └── ... (7 archivos)
│   ├── testing/
│   │   ├── qa-tests.sh
│   │   ├── integration-tests.sh
│   │   ├── test-*.js
│   │   └── ... (24 archivos)
│   ├── analysis/
│   │   ├── analyze-cleanup.sh
│   │   └── execute-cleanup.sh
│   └── utility/
│       ├── generate-admin-password.js
│       ├── migrate-to-winston.js
│       └── ... (11 archivos)
│
├── shared/                            ✅ Código compartido
│   ├── config/
│   ├── utils/
│   │   └── logger.js
│   └── middleware/
│       └── guardian-validation-middleware.js
│
├── uploads/                           ✅ Archivos usuarios
├── node_modules/                      ✅ Dependencias
└── configs/                           ✅ Configs varias
```

---

## 📊 Resumen de Cambios

| Categoría | Antes | Después | Eliminados |
|-----------|-------|---------|------------|
| **Archivos en raíz** | ~90 | 15 | - |
| **Scripts organizados** | 0 | 62 | - |
| **Archivos obsoletos** | 13 | 0 | 13 |
| **Directorios en raíz** | 15+ | 10 | 5+ |

---

## ⚠️ Notas Importantes

1. **Scripts de deployment actualizados**: Después de mover, actualizar rutas en:
   - `start-microservices-gateway.sh` (ya actualizado)
   - Otros scripts que referencien paths

2. **Shared directory**: Poblar con código compartido:
   - `logger.js` → `shared/utils/`
   - `guardian-validation-middleware.js` → `shared/middleware/`

3. **Testing**: Verificar que todos los scripts funcionen después de mover

4. **Commit incremental**: Commit después de cada fase exitosa

---

## 🎯 Beneficios Esperados

1. **Raíz limpia**: Solo 15 archivos esenciales vs 90 actuales
2. **Scripts organizados**: 62 scripts categorizados en 5 subdirectorios
3. **Profesionalismo**: Estructura enterprise-grade
4. **Mantenibilidad**: Fácil encontrar scripts por categoría
5. **Escalabilidad**: Agregar nuevos scripts es trivial

---

**Generado:** 2025-10-15
**Rama:** limpieza
**Siguiente commit:** Organización de archivos sueltos en estructura modular
