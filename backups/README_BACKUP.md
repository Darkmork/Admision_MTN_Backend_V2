# 🗄️ Backup de Base de Datos - Sistema de Admisión MTN

## 📋 Información del Backup

**Fecha de creación**: 13 de Octubre 2025, 08:28:02
**Base de datos**: Admisión_MTN_DB
**Versión**: Pre-producción (branch: pre_produccion)

### 📊 Estadísticas del Backup

| Tabla | Registros |
|-------|-----------|
| Users | 38 |
| Applications | 21 |
| Students | 51 |
| Guardians | 27 |
| Evaluations | 6 |
| Interviews | 1 |

**Total de registros**: 144

### 📦 Archivos de Backup

- **SQL sin comprimir**: `admision_mtn_backup_20251013_082802.sql` (111 KB)
- **SQL comprimido**: `admision_mtn_backup_20251013_082802.sql.gz` (20 KB)
- **Compresión**: 82% (5.5x reducción de tamaño)

## 🔄 Restaurar Base de Datos

### Opción 1: Desde archivo SQL sin comprimir

```bash
# Descartar base de datos actual (CUIDADO: esto borra todos los datos actuales)
PGPASSWORD=admin123 dropdb -h localhost -U admin "Admisión_MTN_DB"

# Crear base de datos vacía
PGPASSWORD=admin123 createdb -h localhost -U admin "Admisión_MTN_DB"

# Restaurar desde backup
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -f admision_mtn_backup_20251013_082802.sql
```

### Opción 2: Desde archivo comprimido

```bash
# Descomprimir primero
gunzip -k admision_mtn_backup_20251013_082802.sql.gz

# Luego seguir los pasos de la Opción 1
```

### Opción 3: Restaurar sin descartar DB (solo datos)

```bash
# Esto intentará insertar los datos sin eliminar la DB actual
# Puede fallar si ya existen datos conflictivos
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -f admision_mtn_backup_20251013_082802.sql
```

## 🛡️ Script de Restauración Seguro

Se incluye un script automatizado para restauración segura:

```bash
# Hacer ejecutable el script de restauración
chmod +x restore-backup.sh

# Ejecutar restauración
./restore-backup.sh admision_mtn_backup_20251013_082802.sql
```

El script incluye:
- ✅ Confirmación antes de proceder
- ✅ Backup automático de la DB actual antes de restaurar
- ✅ Validación de archivos
- ✅ Reporte de éxito/error

## 📝 Verificar Restauración

Después de restaurar, verifica que los datos estén correctos:

```bash
# Contar registros en tablas principales
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c "
SELECT 'Users' as tabla, COUNT(*) as registros FROM users
UNION ALL SELECT 'Applications', COUNT(*) FROM applications
UNION ALL SELECT 'Students', COUNT(*) FROM students
UNION ALL SELECT 'Guardians', COUNT(*) FROM guardians
UNION ALL SELECT 'Evaluations', COUNT(*) FROM evaluations
UNION ALL SELECT 'Interviews', COUNT(*) FROM interviews;
"
```

Los números deben coincidir con las estadísticas mostradas arriba.

## ⚠️ Advertencias Importantes

1. **Siempre haz un backup antes de restaurar**: La restauración sobrescribirá todos los datos actuales.

2. **Detén los servicios backend**: Antes de restaurar, detén todos los servicios Node.js que estén accediendo a la base de datos.

3. **Verifica las credenciales**: Asegúrate de usar las credenciales correctas:
   - Host: `localhost`
   - Usuario: `admin`
   - Password: `admin123`
   - Base de datos: `Admisión_MTN_DB`

4. **Espacio en disco**: Asegúrate de tener suficiente espacio (mínimo 500 MB libres).

## 🔐 Seguridad del Backup

- ✅ El backup incluye la estructura completa de todas las tablas
- ✅ Todos los datos están incluidos (usuarios, aplicaciones, estudiantes, etc.)
- ✅ Las contraseñas están hasheadas con BCrypt (seguras)
- ✅ No incluye datos sensibles de configuración del sistema
- ⚠️ **NO compartir este archivo públicamente** - contiene datos reales del sistema

## 📅 Recomendaciones de Backup

- Crear backups **antes de cada deployment** a producción
- Crear backups **después de cambios importantes** en la estructura de BD
- Mantener **al menos 3 backups** recientes
- Guardar backups en **ubicación segura** (Google Drive, Dropbox, etc.)
- Rotar backups antiguos después de 30 días

## 🆘 En Caso de Emergencia

Si perdiste datos y necesitas restaurar:

1. **STOP** - No hagas más cambios en la base de datos
2. Revisa este README
3. Ejecuta el script de restauración: `./restore-backup.sh`
4. Verifica que los datos se restauraron correctamente
5. Reinicia los servicios backend
6. Prueba el login y funcionalidades críticas

## 📞 Soporte

Si tienes problemas con la restauración:
- Revisa los logs de PostgreSQL: `/opt/homebrew/var/log/postgresql@15/`
- Verifica que PostgreSQL esté corriendo: `brew services list`
- Contacta al equipo de desarrollo

---

🤖 Backup creado automáticamente por Claude Code
📅 Fecha: 2025-10-13 08:28:02
