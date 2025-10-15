# Restauración Manual de Base de Datos Railway

## 🎯 Objetivo
Restaurar los 144 registros del backup a la base de datos PostgreSQL de Railway.

## 📋 Paso a Paso (5 minutos)

### **PASO 1: Obtener DATABASE_URL desde Railway Dashboard** (1 min)

1. Ve a: https://railway.app/dashboard
2. Click en proyecto: **Admision_MTN_Backend**
3. Click en el plugin: **Postgres** (icono de elefante)
4. Click en pestaña: **Variables**
5. Busca: `DATABASE_URL`
6. Click en el ícono del **ojo** 👁️ para revelar el valor
7. Copia la URL completa (se ve así):
   ```
   postgresql://postgres:PASSWORD@HOST:PORT/railway
   ```

### **PASO 2: Ejecutar Restauración** (2 min)

Abre una terminal y ejecuta:

```bash
# Navegar al directorio del backend
cd "/Users/jorgegangale/Library/Mobile Documents/com~apple~CloudDocs/Proyectos/Admision_MTN/Admision_MTN_backend"

# Definir DATABASE_URL (pega la URL que copiaste)
export DATABASE_URL="postgresql://postgres:PASSWORD@HOST:PORT/railway"

# Restaurar la base de datos
psql "$DATABASE_URL" < backups/admision_mtn_backup_20251013_082802.sql
```

**Nota:** Si ves algunos errores de "already exists", es normal. Ignóralos y continúa.

### **PASO 3: Verificar Restauración** (2 min)

```bash
# Contar registros
psql "$DATABASE_URL" -c "
SELECT
  'users' as tabla, COUNT(*) as registros FROM users
UNION ALL SELECT 'applications', COUNT(*) FROM applications
UNION ALL SELECT 'students', COUNT(*) FROM students
UNION ALL SELECT 'guardians', COUNT(*) FROM guardians
UNION ALL SELECT 'evaluations', COUNT(*) FROM evaluations
UNION ALL SELECT 'interviews', COUNT(*) FROM interviews
ORDER BY tabla;
"
```

**Resultado Esperado:**
```
    tabla     | registros
--------------+-----------
 applications |        21
 evaluations  |         6
 guardians    |        27
 interviews   |         1
 students     |        51
 users        |        38
```

**Total: 144 registros** ✅

### **PASO 4: Probar Login** (30 segundos)

```bash
# Test de autenticación
curl -X POST "https://admisionmtnbackendv2-production.up.railway.app/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"jorge.gangale@mtn.cl","password":"admin123"}'
```

**Resultado Esperado:**
```json
{
  "success": true,
  "data": {
    "token": "eyJ...",
    "user": {
      "id": 25,
      "email": "jorge.gangale@mtn.cl",
      "role": "ADMIN",
      "firstName": "Jorge",
      "lastName": "Gangale"
    }
  }
}
```

---

## ⚠️ Troubleshooting

### Error: "psql: command not found"
```bash
# Instalar PostgreSQL client
brew install postgresql@15
```

### Error: "connection refused"
- Verifica que el DATABASE_URL sea correcto
- Asegúrate de tener conexión a internet
- Verifica que el plugin PostgreSQL esté activo en Railway

### Error: "password authentication failed"
- Vuelve a copiar el DATABASE_URL desde Railway Dashboard
- Asegúrate de copiar la URL completa con la contraseña

---

## 🚀 Después de Restaurar

Una vez restaurada la base de datos:

1. ✅ El sistema estará **100% funcional**
2. ✅ Podrás hacer login con: `jorge.gangale@mtn.cl` / `admin123`
3. ✅ Todos los 144 registros estarán disponibles
4. ✅ El frontend podrá conectarse al backend

**Siguiente paso:** Ejecutar smoke tests con `./railway-smoke-tests.sh`

---

## 📞 Soporte

Si tienes algún problema, avísame y continuamos juntos.
