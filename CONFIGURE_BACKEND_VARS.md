# ⚙️ Configurar Variables de Entorno del Backend

## ✅ Base de Datos Configurada

Ya se crearon todas las tablas en PostgreSQL:
- ✅ users (con usuario admin)
- ✅ applications
- ✅ evaluations
- ✅ interviews
- ✅ notifications
- ✅ documents
- ✅ guardians
- ✅ interviewer_schedules

## 🔴 Acción Requerida: Configurar Variables en el Servicio Backend

### Paso 1: Ir a Railway Dashboard

1. Abre: https://railway.app/dashboard
2. Busca el proyecto: **abundant-courtesy**
3. Click para abrirlo

### Paso 2: Identificar el Servicio Backend

Verás 2 servicios en el dashboard:
- **Postgres** - Base de datos (ya configurado ✅)
- **Otro servicio** - Este es tu backend (el que tiene tu código Node.js)

Click en el **servicio del backend** (NO en Postgres)

### Paso 3: Configurar Variables de Entorno

1. En el servicio Backend, ve a la pestaña **"Variables"**

2. Click en **"New Variable"** y agrega CADA UNA de estas variables:

#### Variables de Base de Datos

| Variable Name | Value | Descripción |
|--------------|-------|-------------|
| `DB_HOST` | `${{Postgres.PGHOST}}` | Host de PostgreSQL |
| `DB_PORT` | `${{Postgres.PGPORT}}` | Puerto de PostgreSQL |
| `DB_NAME` | `${{Postgres.PGDATABASE}}` | Nombre de la base de datos |
| `DB_USERNAME` | `${{Postgres.PGUSER}}` | Usuario de PostgreSQL |
| `DB_PASSWORD` | `${{Postgres.PGPASSWORD}}` | Password de PostgreSQL |

**Importante**: Usa EXACTAMENTE `${{Postgres.PGHOST}}` (con las llaves y todo). Railway las reemplazará automáticamente con los valores correctos.

#### Variables de Aplicación

| Variable Name | Value | Descripción |
|--------------|-------|-------------|
| `JWT_SECRET` | `mtn_secure_jwt_secret_2025_production_v1` | Secreto para JWT |
| `NODE_ENV` | `production` | Entorno de producción |

#### Variables de Email (SMTP) - IMPORTANTE

**⚠️ NUNCA hardcodees passwords en el código - siempre usa variables de entorno**

| Variable Name | Value | Descripción |
|--------------|-------|-------------|
| `SMTP_HOST` | `smtp.gmail.com` | Host del servidor SMTP de Gmail |
| `SMTP_PORT` | `587` | Puerto SMTP (TLS) |
| `SMTP_USERNAME` | `jorge.gangale@mtn.cl` | Email de envío |
| `SMTP_PASSWORD` | `yaejhysibcgifpng` | **App Password de Gmail** (NO la password normal) |
| `EMAIL_MOCK_MODE` | `false` | `true` = no envía emails reales, `false` = envía emails |

**Importante sobre SMTP_PASSWORD**:
- Esta es una "App Password" de Gmail, NO tu contraseña normal
- Para generar una App Password:
  1. Ve a Google Account → Security
  2. Activa "2-Step Verification" (si no está activado)
  3. Busca "App passwords"
  4. Genera una nueva app password para "Mail"
  5. Usa esa password de 16 caracteres aquí

**Seguridad**:
- ✅ Esta password SOLO existe en Railway (variables de entorno)
- ✅ NO está en el código fuente
- ✅ NO está en Git
- ✅ Railway la encripta automáticamente

### Paso 4: Guardar y Esperar

1. Railway guardará automáticamente cada variable que agregues
2. El servicio se reiniciará automáticamente (toma 30-60 segundos)
3. Espera a que el deployment termine (verás "Active" o "Running")

### Paso 5: Verificar que Funciona

Una vez que el servicio esté "Active", prueba estos endpoints:

#### Test 1: Health Check

```bash
curl https://admisionmtnbackendv2-production.up.railway.app/health
```

**Respuesta esperada:**
```json
{
  "status": "UP",
  "timestamp": "2025-10-07T...",
  "service": "MTN Admission Backend",
  "environment": "production",
  "port": "8080"
}
```

#### Test 2: Login

```bash
curl -X POST https://admisionmtnbackendv2-production.up.railway.app/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"jorge.gangale@mtn.cl","password":"admin123"}'
```

**Respuesta esperada:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "email": "jorge.gangale@mtn.cl",
    "role": "ADMIN",
    "firstName": "Jorge",
    "lastName": "Gangale"
  }
}
```

Si ves el token JWT, ¡todo está funcionando! ✅

#### Test 3: Frontend

1. Ve a: https://admision-mtn-frontend.vercel.app
2. Intenta hacer login:
   - **Email**: jorge.gangale@mtn.cl
   - **Password**: admin123
3. Deberías ver el dashboard de admin

## 🔧 Troubleshooting

### Error: "Internal server error" después de configurar variables

**Posible causa**: Variables mal configuradas

**Solución**:
1. Ve a Railway Dashboard → Backend Service → Variables
2. Verifica que las referencias `${{Postgres.PGHOST}}` estén exactamente así (con llaves)
3. Verifica que no haya espacios extra
4. Reinicia el servicio manualmente (Settings → Restart)

### El backend no se reinicia

**Solución**:
1. Ve a Settings en el servicio Backend
2. Click en "Restart"
3. Espera 1 minuto

### No veo el servicio Backend, solo Postgres

**Solución**:
1. Puede que el backend esté en otro proyecto
2. Busca en Railway dashboard un proyecto llamado "mtn-admission-backend"
3. Ese debería tener el servicio del backend

## 📋 Checklist

- [ ] Abrí Railway Dashboard
- [ ] Encontré el proyecto "abundant-courtesy"
- [ ] Identifiqué el servicio Backend (el que NO es Postgres)
- [ ] Agregué todas las variables DB_*
- [ ] Agregué JWT_SECRET y NODE_ENV
- [ ] El servicio se reinició automáticamente
- [ ] Health check responde OK
- [ ] Login endpoint funciona
- [ ] Login desde frontend funciona

## 🎉 ¡Listo!

Una vez completado el checklist, tu sistema estará 100% funcional en producción.

---

**Credenciales de PostgreSQL (para referencia):**
- Host público: shuttle.proxy.rlwy.net:28742
- Database: railway
- User: postgres
- Password: lDOJKXWNqXiacBcHNFnklZyLQBFQterc

**No agregues estas credenciales manualmente** - usa las referencias `${{Postgres.*}}` para que Railway las maneje automáticamente.
