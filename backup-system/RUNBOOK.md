# RUNBOOK - Sistema de Backup y Restore
# Admisión MTN - PostgreSQL Backup System

**Versión:** 1.0
**Última actualización:** 2025-10-03
**Responsable:** Equipo DevOps
**Audiencia:** Administradores de sistemas, DevOps, SRE

---

## Tabla de Contenidos

1. [Descripción General](#1-descripción-general)
2. [Arquitectura del Sistema](#2-arquitectura-del-sistema)
3. [Configuración Inicial](#3-configuración-inicial)
4. [Operaciones Diarias](#4-operaciones-diarias)
5. [Procedimientos de Emergencia](#5-procedimientos-de-emergencia)
6. [Monitoreo y Alertas](#6-monitoreo-y-alertas)
7. [Troubleshooting](#7-troubleshooting)
8. [Mantenimiento](#8-mantenimiento)
9. [Contactos y Escalación](#9-contactos-y-escalación)

---

## 1. Descripción General

### Propósito
Sistema automatizado de backup y restauración para PostgreSQL que protege los datos del sistema de admisión del Colegio Monte Tabor y Nazaret.

### Objetivos de Servicio
- **RPO (Recovery Point Objective):** ≤ 1 hora
- **RTO (Recovery Time Objective):** ≤ 4 horas
- **Disponibilidad:** 99.9%

### Componentes
- **Base de datos:** PostgreSQL "Admisión_MTN_DB"
- **Microservicios:** 6 servicios (puertos 8082-8087)
- **Almacenamiento:** S3/MinIO con cifrado AES-256
- **Retención:** Full 30 días, Diferencial 90 días

---

## 2. Arquitectura del Sistema

### Diagrama de Flujo

```
┌─────────────────┐
│   PostgreSQL    │
│ Admisión_MTN_DB │
└────────┬────────┘
         │ pg_dump
         ▼
┌─────────────────┐
│  Full Backup    │ ──────► Diario 2:00 AM
│  (script)       │
└────────┬────────┘
         │
         ├─► Compresión (gzip -9)
         ├─► Cifrado (AES-256-CBC)
         └─► Upload S3/MinIO
                   │
                   ▼
         ┌──────────────────┐
         │ Differential      │ ──────► Cada hora
         │ Backup (script)   │
         └─────────┬─────────┘
                   │
         ┌─────────▼─────────┐
         │   Monitoring      │ ──────► Cada 6 horas
         │   (script)        │
         └───────────────────┘
```

### Ubicación de Archivos

```
backup-system/
├── config/
│   ├── backup.conf                    # Configuración principal
│   ├── .encryption.key                # Clave de cifrado (NO VERSIONAR)
│   ├── crontab.txt                    # Configuración crontab
│   ├── com.mtn.backup.full.plist      # launchd - Full backup
│   ├── com.mtn.backup.differential.plist  # launchd - Differential
│   └── com.mtn.backup.monitor.plist   # launchd - Monitoring
├── scripts/
│   ├── full-backup.sh                 # Backup completo diario
│   ├── differential-backup.sh         # Backup diferencial horario
│   ├── restore.sh                     # Restauración con verificación
│   ├── monitor-backups.sh             # Monitoreo y reportes
│   └── send-notification.sh           # Alertas por email
├── backups/                           # Backups locales
├── logs/                              # Logs de ejecución
│   └── metrics/                       # Métricas JSON
└── temp/                              # Archivos temporales
```

---

## 3. Configuración Inicial

### 3.1. Requisitos Previos

**Software requerido:**
```bash
# Verificar herramientas instaladas
which pg_dump      # PostgreSQL client
which openssl      # Cifrado
which gzip         # Compresión
which bc           # Cálculos matemáticos
which python3      # Envío de emails
```

**Para S3 (AWS):**
```bash
which aws          # AWS CLI
aws configure      # Configurar credenciales
```

**Para MinIO (local):**
```bash
brew install minio/stable/mc
mc alias set mtn http://localhost:9000 minioadmin minioadmin
```

### 3.2. Instalación del Sistema

**Paso 1: Verificar estructura de directorios**
```bash
cd /path/to/backup-system
mkdir -p backups logs logs/metrics temp
```

**Paso 2: Configurar credenciales de base de datos**
```bash
# Editar config/backup.conf
vim config/backup.conf

# Verificar:
DB_HOST="localhost"
DB_PORT="5432"
DB_NAME="Admisión_MTN_DB"
DB_USER="admin"
DB_PASSWORD="admin123"  # ⚠️ CAMBIAR EN PRODUCCIÓN
```

**Paso 3: Generar clave de cifrado**
```bash
# La clave se genera automáticamente en el primer full-backup
# O generarla manualmente:
openssl rand -base64 32 > config/.encryption.key
chmod 600 config/.encryption.key
```

**Paso 4: Configurar email (SMTP)**
```bash
# Editar config/backup.conf
SMTP_SERVER="smtp.gmail.com"
SMTP_PORT="587"
SMTP_USER="admision@mtn.cl"
SMTP_PASSWORD="app-specific-password"  # ⚠️ Usar App Password de Gmail
ALERT_EMAIL="admin@mtn.cl"
```

### 3.3. Configurar Automatización

**Opción A: Usar crontab (Linux)**
```bash
# Editar crontab
crontab -e

# Copiar contenido de config/crontab.txt
# Ajustar rutas según tu instalación

# Verificar
crontab -l
```

**Opción B: Usar launchd (macOS) - RECOMENDADO**
```bash
# Copiar archivos .plist a LaunchAgents
cp config/*.plist ~/Library/LaunchAgents/

# Cargar servicios
launchctl load ~/Library/LaunchAgents/com.mtn.backup.full.plist
launchctl load ~/Library/LaunchAgents/com.mtn.backup.differential.plist
launchctl load ~/Library/LaunchAgents/com.mtn.backup.monitor.plist

# Verificar que estén cargados
launchctl list | grep com.mtn.backup

# Ver logs
tail -f /tmp/mtn-full-backup.log
```

### 3.4. Prueba Inicial

**Test completo del sistema:**
```bash
# 1. Ejecutar backup completo manualmente
cd backup-system/scripts
./full-backup.sh

# 2. Verificar que se creó el backup
ls -lh ../backups/

# 3. Verificar logs
tail -n 50 ../logs/full-backup_*.log

# 4. Ejecutar backup diferencial
./differential-backup.sh

# 5. Ejecutar monitoreo
./monitor-backups.sh

# 6. Revisar reporte
cat ../logs/backup-report_*.txt
```

---

## 4. Operaciones Diarias

### 4.1. Verificación Matutina (Daily Health Check)

**Checklist diario (5 minutos):**

1. **Verificar último backup completo**
```bash
cd backup-system
ls -lht backups/admision_mtn_full_*.sql.gz.gpg | head -n 1
```
✓ Debe existir un backup de las últimas 26 horas

2. **Verificar backups diferenciales**
```bash
ls -lht backups/admision_mtn_diff_*.sql.gz.gpg | head -n 5
```
✓ Deben existir backups de las últimas horas

3. **Revisar logs de errores**
```bash
tail -n 100 logs/full-backup_*.log | grep -i error
tail -n 100 logs/diff-backup_*.log | grep -i error
```
✓ No deben aparecer errores críticos

4. **Ejecutar reporte de monitoreo**
```bash
./scripts/monitor-backups.sh
cat logs/backup-report_$(date +%Y%m%d)*.txt
```
✓ Overall status debe ser "OK"

5. **Verificar RPO/RTO**
```bash
# En el reporte de monitoreo:
# - RPO actual < 1 hora
# - RTO estimado < 4 horas
```

### 4.2. Tareas Semanales

**Lunes (inicio de semana):**
- Revisar métricas de la semana anterior
- Verificar espacio en disco
- Comprobar retención de backups

```bash
# Verificar espacio en disco
df -h /path/to/backup-system/backups

# Ver resumen de backups de los últimos 7 días
find backups -name "*.gpg" -mtime -7 -ls | wc -l

# Verificar tamaño total
du -sh backups/
```

**Viernes (fin de semana):**
- Generar reporte semanal de métricas
- Revisar alertas acumuladas

### 4.3. Tareas Mensuales

**Primer día del mes:**
- Verificar política de retención
- Limpiar backups antiguos manualmente (si es necesario)
- Revisar logs de monitoreo

```bash
# Backups más antiguos de 30 días (full)
find backups -name "admision_mtn_full_*.gpg" -mtime +30 -ls

# Backups más antiguos de 90 días (differential)
find backups -name "admision_mtn_diff_*.gpg" -mtime +90 -ls
```

**Test de restauración mensual:**
- Ejecutar restore en staging (ver sección 5.2)
- Documentar tiempo de recuperación
- Actualizar métricas de RTO

---

## 5. Procedimientos de Emergencia

### 5.1. Pérdida Total de Base de Datos

**Escenario:** PostgreSQL corrupto o servidor caído

**Tiempo estimado:** 2-4 horas (según tamaño)

**Procedimiento:**

**PASO 1: Evaluar la situación**
```bash
# Intentar conectar a PostgreSQL
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c "SELECT version();"

# Si falla, verificar servicio
systemctl status postgresql   # Linux
brew services list | grep postgres  # macOS
```

**PASO 2: Notificar al equipo**
```bash
# Enviar alerta
./scripts/send-notification.sh "error" "CRITICAL: Database Down" \
  "PostgreSQL no responde. Iniciando procedimiento de restauración."
```

**PASO 3: Identificar último backup válido**
```bash
# Listar backups disponibles
ls -lht backups/admision_mtn_full_*.sql.gz.gpg | head -n 5

# Seleccionar el más reciente
LATEST_BACKUP=$(ls -t backups/admision_mtn_full_*.sql.gz.gpg | head -n 1)
echo "Restaurando desde: $LATEST_BACKUP"
```

**PASO 4: Restaurar en staging (verificación)**
```bash
# IMPORTANTE: SIEMPRE restaurar primero en staging

cd backup-system/scripts
./restore.sh \
  --file "$LATEST_BACKUP" \
  --target "Admisión_MTN_DB_staging" \
  --drop

# Verificar resultado
tail -n 50 ../logs/restore_*.log
```

**PASO 5: Validar datos en staging**
```bash
# Conectar a staging
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB_staging"

# Verificar tablas principales
SELECT COUNT(*) FROM users;
SELECT COUNT(*) FROM applications;
SELECT COUNT(*) FROM interviews;

# Verificar datos recientes
SELECT MAX(created_at) FROM applications;
```

**PASO 6: Restaurar en producción**
```bash
# ⚠️ CUIDADO: Esto sobrescribirá la base de datos de producción

./restore.sh \
  --file "$LATEST_BACKUP" \
  --target "Admisión_MTN_DB" \
  --type differential \
  --drop

# El script pedirá confirmación:
# "Type 'CONFIRM DELETE PRODUCTION' to proceed:"
```

**PASO 7: Verificar servicios**
```bash
# Reiniciar microservicios
cd /path/to/Admision_MTN_backend
./start-microservices-gateway.sh

# Verificar health checks
for port in 8082 8083 8084 8085 8086 8087; do
  echo -n "Port $port: "
  curl -s http://localhost:$port/health | head -c 50
  echo ""
done
```

**PASO 8: Notificar recuperación**
```bash
./scripts/send-notification.sh "success" "Database Restored" \
  "Base de datos restaurada exitosamente desde backup de [FECHA]"
```

### 5.2. Restauración a Punto Específico en el Tiempo

**Escenario:** Necesidad de volver a un estado anterior (ej: error humano)

```bash
# 1. Identificar momento del error
# Por ejemplo: 2025-10-03 14:30

# 2. Buscar backup full anterior a esa hora
ls -lt backups/admision_mtn_full_*20251003*.gpg

# 3. Buscar backups diferenciales posteriores al full pero anteriores al error
ls -lt backups/admision_mtn_diff_*20251003*.gpg

# 4. Restaurar con tipo "differential"
./scripts/restore.sh \
  --file backups/admision_mtn_full_20251003_020000.sql.gz.gpg \
  --type differential \
  --target "Admisión_MTN_DB_recovery" \
  --drop

# Esto aplicará automáticamente los diferenciales hasta el momento del error
```

### 5.3. Corrupción de Backups

**Escenario:** Backup principal corrupto o no descifra

**Procedimiento:**

```bash
# 1. Verificar integridad del archivo
file backups/admision_mtn_full_*.gpg

# 2. Intentar descifrar manualmente
openssl enc -d -aes-256-cbc -pbkdf2 -iter 100000 \
  -in backups/admision_mtn_full_*.gpg \
  -out /tmp/test_decrypt.sql.gz \
  -pass file:config/.encryption.key

# 3. Si falla, intentar con backup anterior
ls -lht backups/admision_mtn_full_*.gpg | head -n 10

# 4. Notificar al equipo sobre backup corrupto
./scripts/send-notification.sh "error" "Corrupted Backup Detected" \
  "Backup [NOMBRE] corrupto. Usando backup anterior."
```

### 5.4. Falla de Almacenamiento S3/MinIO

**Escenario:** S3 o MinIO no accesible

```bash
# 1. Verificar conectividad
mc alias list
mc du mtn/mtn-backups

# 2. Los backups locales siguen disponibles en:
ls -lh backups/

# 3. Reconfigurar S3 si es necesario
mc alias set mtn http://localhost:9000 minioadmin minioadmin

# 4. Re-subir backups faltantes
for file in backups/*.gpg; do
  mc cp "$file" "mtn/mtn-backups/full/"
done
```

---

## 6. Monitoreo y Alertas

### 6.1. Métricas Clave

**Métricas de Backup:**
- Tamaño del backup (bytes)
- Duración del backup (segundos)
- Ratio de compresión (%)
- Éxito/Fallo de subida a S3

**Métricas de Sistema:**
- RPO actual (horas)
- RTO estimado (horas)
- Espacio en disco (GB)
- Edad del último backup (horas)

**Acceso a métricas:**
```bash
# Ver métricas en formato JSON
cat logs/metrics/full-backup_*.json | jq .

# Extraer métrica específica
cat logs/metrics/full-backup_latest.json | jq -r '.total_duration_seconds'
```

### 6.2. Tipos de Alertas

**CRITICAL (Rojo) - Acción Inmediata:**
- No hay backup full en últimas 26 horas
- RPO excedido (> 1 hora)
- Fallo en restauración de prueba
- Espacio en disco < 10%

**WARNING (Amarillo) - Revisar en 1 hora:**
- No hay backup diferencial en últimas 2 horas
- RTO estimado > 4 horas
- Backup con errores menores
- Espacio en disco < 20%

**INFO (Azul) - Informativo:**
- Backup completado exitosamente
- Monitoreo programado ejecutado

### 6.3. Configuración de Alertas

**Email:**
```bash
# Ya configurado en scripts/send-notification.sh
# Se envía automáticamente en:
# - Backups completados
# - Errores críticos
# - Reportes de monitoreo
```

**Logs del sistema (syslog):**
```bash
# Ver logs del sistema
tail -f /var/log/syslog | grep backup-system

# Búsqueda de eventos específicos
journalctl -u backup-system -f
```

---

## 7. Troubleshooting

### 7.1. Backup Falla con "Permission Denied"

**Síntoma:**
```
ERROR: could not open file "..." for reading: Permission denied
```

**Solución:**
```bash
# Verificar permisos de scripts
chmod +x scripts/*.sh

# Verificar permisos de directorios
chmod 755 backups logs temp

# Verificar ownership
ls -ld backups logs temp
```

### 7.2. "Encryption Key Not Found"

**Síntoma:**
```
ERROR: Encryption key not found: config/.encryption.key
```

**Solución:**
```bash
# Generar nueva clave (⚠️ backups anteriores no serán descifrables)
openssl rand -base64 32 > config/.encryption.key
chmod 600 config/.encryption.key

# O restaurar clave desde backup seguro
```

### 7.3. "PostgreSQL Connection Refused"

**Síntoma:**
```
psql: could not connect to server: Connection refused
```

**Solución:**
```bash
# Verificar que PostgreSQL esté corriendo
systemctl status postgresql  # Linux
brew services list | grep postgres  # macOS

# Verificar credenciales en config/backup.conf
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c "SELECT 1;"

# Verificar puerto
netstat -an | grep 5432
```

### 7.4. "Backup Size is Zero"

**Síntoma:**
Backup generado pero tamaño = 0 bytes

**Solución:**
```bash
# Verificar que la base de datos tenga datos
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" \
  -c "SELECT pg_size_pretty(pg_database_size('Admisión_MTN_DB'));"

# Ejecutar backup con más verbosidad
PGDUMP_DEBUG=1 ./scripts/full-backup.sh

# Revisar logs detallados
tail -n 200 logs/full-backup_*.log
```

### 7.5. "MinIO Client Not Installed"

**Síntoma:**
```
mc: command not found
```

**Solución:**
```bash
# Instalar MinIO client
brew install minio/stable/mc

# Configurar alias
mc alias set mtn http://localhost:9000 minioadmin minioadmin

# Verificar
mc ls mtn/
```

### 7.6. "RPO Exceeded"

**Síntoma:**
Monitoreo reporta RPO > 1 hora

**Diagnóstico:**
```bash
# Verificar último backup diferencial
ls -lt backups/admision_mtn_diff_*.gpg | head -n 1

# Revisar logs de cron/launchd
tail -f /tmp/mtn-diff-backup.log

# Verificar que el servicio esté activo
launchctl list | grep com.mtn.backup.differential
```

**Solución:**
```bash
# Ejecutar backup diferencial manualmente
./scripts/differential-backup.sh

# Si persiste, verificar cronjob/launchd configuration
crontab -l  # cron
launchctl list | grep mtn  # launchd
```

---

## 8. Mantenimiento

### 8.1. Rotación de Logs

**Automático:**
Los scripts limpian logs antiguos automáticamente (>90 días)

**Manual:**
```bash
# Limpiar logs mayores a 30 días
find logs -name "*.log" -mtime +30 -delete

# Limpiar métricas mayores a 60 días
find logs/metrics -name "*.json" -mtime +60 -delete
```

### 8.2. Verificación de Espacio en Disco

```bash
# Ver uso de disco del sistema de backups
du -sh backups logs temp

# Ver desglose por tipo
du -sh backups/*full* backups/*diff*

# Proyección de crecimiento (últimos 7 días)
find backups -name "*.gpg" -mtime -7 -exec du -ch {} + | tail -n 1
```

### 8.3. Actualización de Credenciales

**Cambiar password de base de datos:**
```bash
# 1. Actualizar en config/backup.conf
vim config/backup.conf
# DB_PASSWORD="nueva_password"

# 2. Probar conexión
PGPASSWORD=nueva_password psql -h localhost -U admin -d "Admisión_MTN_DB" -c "SELECT 1;"

# 3. Ejecutar test de backup
./scripts/full-backup.sh
```

**Cambiar credenciales de S3:**
```bash
# Actualizar en config/backup.conf
MINIO_ACCESS_KEY="nuevo_key"
MINIO_SECRET_KEY="nuevo_secret"

# Reconfigurar MinIO client
mc alias set mtn http://localhost:9000 nuevo_key nuevo_secret
```

### 8.4. Test de Restauración Mensual

**Procedimiento (primer viernes de cada mes):**

```bash
# 1. Seleccionar backup full más reciente
LATEST_FULL=$(ls -t backups/admision_mtn_full_*.gpg | head -n 1)

# 2. Restaurar en staging
./scripts/restore.sh \
  --file "$LATEST_FULL" \
  --target "Admisión_MTN_DB_test_$(date +%Y%m)" \
  --type differential \
  --drop

# 3. Documentar tiempo de recuperación
echo "$(date): RTO Test - Duration: [X minutos]" >> logs/rto-tests.log

# 4. Eliminar base de datos de prueba
PGPASSWORD=admin123 psql -h localhost -U admin -d postgres \
  -c "DROP DATABASE IF EXISTS \"Admisión_MTN_DB_test_$(date +%Y%m)\";"
```

---

## 9. Contactos y Escalación

### 9.1. Equipo de Respuesta

**Nivel 1 - Operaciones (First Responder):**
- **Nombre:** [Administrador de Sistemas]
- **Email:** admin@mtn.cl
- **Teléfono:** +56 9 XXXX XXXX
- **Disponibilidad:** Lun-Vie 8:00-18:00

**Nivel 2 - DevOps Engineer:**
- **Nombre:** [Ingeniero DevOps]
- **Email:** devops@mtn.cl
- **Teléfono:** +56 9 XXXX XXXX
- **Disponibilidad:** 24/7 (on-call)

**Nivel 3 - Database Administrator:**
- **Nombre:** [DBA]
- **Email:** dba@mtn.cl
- **Teléfono:** +56 9 XXXX XXXX
- **Disponibilidad:** Lun-Vie 9:00-17:00

**Escalación Ejecutiva:**
- **CTO:** cto@mtn.cl
- **Director TI:** ti@mtn.cl

### 9.2. Matriz de Escalación

| Severidad | Tiempo de Respuesta | Notificar |
|-----------|---------------------|-----------|
| **CRITICAL** | < 15 minutos | Nivel 1 + Nivel 2 inmediato |
| **HIGH** | < 1 hora | Nivel 1, escalar a Nivel 2 si no resuelve en 2h |
| **MEDIUM** | < 4 horas | Nivel 1 |
| **LOW** | < 1 día | Ticket en sistema |

### 9.3. Procedimiento de Escalación

**CRITICAL - Base de datos caída:**
1. Nivel 1 intenta restauración (15 min)
2. Si falla → Notificar Nivel 2 inmediatamente
3. Si no resuelve en 1 hora → Notificar DBA + CTO
4. Documentar todo en ticket de incidencia

**HIGH - Backups fallando:**
1. Nivel 1 investiga causa (30 min)
2. Si no identifica → Escalar a Nivel 2
3. Ejecutar backup manual mientras se resuelve
4. Documentar en ticket

### 9.4. Recursos Adicionales

**Documentación:**
- PostgreSQL Docs: https://www.postgresql.org/docs/
- OpenSSL Docs: https://www.openssl.org/docs/
- AWS S3 Docs: https://docs.aws.amazon.com/s3/
- MinIO Docs: https://min.io/docs/minio/linux/

**Herramientas de Diagnóstico:**
```bash
# Health check completo
./scripts/monitor-backups.sh

# Ver estado de servicios
systemctl status postgresql
launchctl list | grep com.mtn.backup

# Logs en tiempo real
tail -f logs/*.log
tail -f /tmp/mtn-*.log
```

---

## Historial de Cambios

| Versión | Fecha | Cambios | Autor |
|---------|-------|---------|-------|
| 1.0 | 2025-10-03 | Creación inicial del runbook | DevOps Team |

---

## Apéndices

### A. Comandos Rápidos de Referencia

```bash
# BACKUPS
./scripts/full-backup.sh              # Backup completo manual
./scripts/differential-backup.sh      # Backup diferencial manual

# MONITOREO
./scripts/monitor-backups.sh          # Generar reporte
cat logs/backup-report_*.txt          # Ver último reporte

# RESTAURACIÓN
./scripts/restore.sh -f ARCHIVO       # Restaurar backup

# CRON/LAUNCHD
crontab -l                            # Ver cron jobs
launchctl list | grep mtn             # Ver servicios launchd

# POSTGRESQL
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB"

# S3/MINIO
mc ls mtn/mtn-backups                 # Listar backups en MinIO
aws s3 ls s3://mtn-admision-backups   # Listar en AWS S3
```

### B. Checklist de Verificación Post-Restore

- [ ] Base de datos accesible
- [ ] Tablas principales existentes (users, applications, interviews)
- [ ] Conteo de registros razonable
- [ ] Fechas recientes correctas
- [ ] Microservicios conectan correctamente
- [ ] Health checks de servicios OK
- [ ] Login de usuarios funcional
- [ ] Prueba de creación de nueva aplicación

### C. Política de Retención Detallada

| Tipo de Backup | Retención Local | Retención S3 | Clase de Almacenamiento S3 |
|----------------|-----------------|--------------|---------------------------|
| Full Daily | 30 días | 90 días | STANDARD_IA |
| Differential Hourly | 90 días | 90 días | STANDARD_IA |
| Logs | 90 días | - | - |
| Métricas | 90 días | - | - |

---

**FIN DEL RUNBOOK**

*Para actualizaciones o preguntas sobre este documento, contactar a: devops@mtn.cl*
