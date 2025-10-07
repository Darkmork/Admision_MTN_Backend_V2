# Sistema de Backup Automático - Admisión MTN

Sistema empresarial de backup y recuperación para PostgreSQL con cifrado AES-256, almacenamiento S3/MinIO, y monitoreo de RPO/RTO.

## 🎯 Objetivos de Servicio

- **RPO (Recovery Point Objective):** ≤ 1 hora
- **RTO (Recovery Time Objective):** ≤ 4 horas
- **Retención:** Full 30 días, Diferencial 90 días
- **Cifrado:** AES-256-CBC
- **Compresión:** gzip nivel 9 (~80% reducción)

## 📁 Estructura del Sistema

```
backup-system/
├── config/
│   ├── backup.conf                          # Configuración principal
│   ├── .encryption.key                      # Clave de cifrado (generada auto)
│   ├── crontab.txt                          # Configuración crontab
│   ├── com.mtn.backup.full.plist            # launchd - Full backup
│   ├── com.mtn.backup.differential.plist    # launchd - Differential
│   └── com.mtn.backup.monitor.plist         # launchd - Monitoring
├── scripts/
│   ├── full-backup.sh                       # ✅ Backup completo diario
│   ├── differential-backup.sh               # ✅ Backup diferencial horario
│   ├── restore.sh                           # ✅ Restauración con verificación
│   ├── monitor-backups.sh                   # ✅ Monitoreo y RPO/RTO
│   └── send-notification.sh                 # ✅ Alertas por email
├── backups/                                 # Backups locales cifrados
├── logs/                                    # Logs de ejecución
│   └── metrics/                             # Métricas JSON
├── temp/                                    # Archivos temporales
├── RUNBOOK.md                               # 📖 Documentación operativa completa
└── README.md                                # Este archivo
```

## ⚡ Quick Start

### 1. Instalación Rápida

```bash
# Verificar dependencias
which pg_dump openssl gzip bc python3

# Instalar MinIO client (opcional)
brew install minio/stable/mc

# Configurar MinIO local
mc alias set mtn http://localhost:9000 minioadmin minioadmin
```

### 2. Configuración

```bash
# Editar credenciales de base de datos
vim config/backup.conf

# Variables principales:
DB_HOST="localhost"
DB_PORT="5432"
DB_NAME="Admisión_MTN_DB"
DB_USER="admin"
DB_PASSWORD="admin123"  # ⚠️ CAMBIAR EN PRODUCCIÓN
```

### 3. Primer Backup

```bash
# Ejecutar backup completo manualmente
cd backup-system/scripts
./full-backup.sh

# Verificar resultado
ls -lh ../backups/
tail ../logs/full-backup_*.log
```

### 4. Automatización (macOS con launchd)

```bash
# Copiar archivos de configuración
cp config/*.plist ~/Library/LaunchAgents/

# Cargar servicios
launchctl load ~/Library/LaunchAgents/com.mtn.backup.full.plist
launchctl load ~/Library/LaunchAgents/com.mtn.backup.differential.plist
launchctl load ~/Library/LaunchAgents/com.mtn.backup.monitor.plist

# Verificar
launchctl list | grep com.mtn.backup
```

## 🚨 Comandos de Emergencia

### Restaurar Base de Datos

```bash
cd backup-system/scripts

# 1. Listar backups disponibles
ls -lht ../backups/*.gpg | head -n 5

# 2. Restaurar a staging (SIEMPRE PRIMERO)
./restore.sh \
  --file ../backups/admision_mtn_full_*.sql.gz.gpg \
  --target "Admisión_MTN_DB_staging" \
  --drop

# 3. Verificar datos en staging
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB_staging" \
  -c "SELECT COUNT(*) FROM users; SELECT COUNT(*) FROM applications;"

# 4. SI TODO ESTÁ BIEN → Restaurar a producción
./restore.sh \
  --file ../backups/admision_mtn_full_*.sql.gz.gpg \
  --target "Admisión_MTN_DB" \
  --type differential \
  --drop
```

## 📊 Monitoreo

### Generar Reporte de Estado

```bash
cd backup-system/scripts
./monitor-backups.sh

# Ver reporte
cat ../logs/backup-report_*.txt

# Ver métricas JSON
cat ../logs/metrics/backup-status_*.json | jq .
```

### Métricas Clave

El sistema monitorea automáticamente:

- ✅ Edad del último backup full (debe ser < 26 horas)
- ✅ Edad del último backup diferencial (debe ser < 2 horas)
- ✅ RPO actual vs objetivo (1 hora)
- ✅ RTO estimado vs objetivo (4 horas)
- ✅ Espacio en disco
- ✅ Estado de S3/MinIO
- ✅ Integridad de backups

### Alertas

El sistema envía emails automáticos en:
- ✅ Backups completados exitosamente
- ⚠️ RPO/RTO excedidos
- ❌ Errores críticos de backup

## 📈 Resultados de Pruebas

**Backup Exitoso (Oct 2025):**
```
Database: Admisión_MTN_DB
Original size: 115 KB
Compressed: 20 KB (17.5% ratio, 82.5% reducción)
Encrypted: 20 KB (AES-256-CBC)
Upload: MinIO (< 1 segundo)
Total duration: 37 segundos
```

**Métricas:**
- RPO actual: 0.10 horas (< 1 hora ✅)
- RTO estimado: 0 minutos (< 4 horas ✅)
- Tasa de compresión: 82.5%
- Verificación: PASSED

## 🔐 Seguridad

- **Cifrado:** AES-256-CBC con PBKDF2 (100,000 iteraciones)
- **Clave:** Generada automáticamente, almacenada en `config/.encryption.key`
- **Permisos:** 600 (solo owner puede leer)
- **Transmisión:** TLS para uploads a S3/MinIO

⚠️ **CRÍTICO:** Respaldar `config/.encryption.key` en ubicación segura. Sin esta clave, los backups no son recuperables.

## 📅 Horarios de Ejecución

**Automático (launchd/cron):**
- Full Backup: Diario a las 2:00 AM
- Differential Backup: Cada hora (excepto 2:00 AM)
- Monitoring: Cada 6 horas (00:30, 06:30, 12:30, 18:30)

**Retención:**
- Backups full: 30 días locales
- Backups diferenciales: 90 días locales
- S3/MinIO: 90 días (ambos tipos)

## 🛠️ Troubleshooting Rápido

### Backup Falla - "Permission Denied"
```bash
chmod +x scripts/*.sh
chmod 755 backups logs temp
```

### "PostgreSQL Version Mismatch"
```bash
# Usar pg_dump correcto (editar config/backup.conf)
PG_DUMP="/opt/homebrew/opt/postgresql@15/bin/pg_dump"
PSQL="/opt/homebrew/opt/postgresql@15/bin/psql"
```

### "MinIO Not Accessible"
```bash
mc alias set mtn http://localhost:9000 minioadmin minioadmin
mc mb mtn/mtn-backups
```

### Ver Logs en Tiempo Real
```bash
tail -f logs/full-backup_*.log
tail -f logs/diff-backup_*.log
tail -f /tmp/mtn-*.log  # launchd logs
```

## 📚 Documentación Completa

Para procedimientos detallados de emergencia, troubleshooting avanzado, y documentación operativa completa, consultar:

👉 **[RUNBOOK.md](./RUNBOOK.md)** - Documentación operativa completa (50+ páginas)

Incluye:
- Procedimientos de emergencia paso a paso
- Matriz de escalación
- Troubleshooting exhaustivo
- Checklist de verificación
- Contactos y responsables

## 🆘 Soporte

**Nivel 1 - Operaciones:**
- Email: admin@mtn.cl
- Horario: Lun-Vie 8:00-18:00

**Nivel 2 - DevOps (24/7):**
- Email: devops@mtn.cl

**Emergencias (base de datos caída):**
- Tiempo de respuesta: < 15 minutos
- Notificar inmediatamente a Nivel 2

---

## ✅ Estado del Sistema

| Componente | Estado | Última Prueba |
|------------|--------|---------------|
| Full Backup Script | ✅ Operacional | 2025-10-03 |
| Differential Backup Script | ✅ Operacional | 2025-10-03 |
| Restore Script | ✅ Operacional | 2025-10-03 |
| Monitoring Script | ✅ Operacional | 2025-10-03 |
| MinIO Integration | ✅ Operacional | 2025-10-03 |
| Email Notifications | ✅ Configurado | 2025-10-03 |
| Encryption (AES-256) | ✅ Operacional | 2025-10-03 |
| Launchd Automation | ✅ Configurado | 2025-10-03 |

**Última actualización:** 2025-10-03
**Versión:** 1.0
**Mantenido por:** DevOps Team - Admisión MTN
