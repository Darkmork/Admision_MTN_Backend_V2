# 🚂 Guía Completa de Configuración en Railway

## Paso 1: Agregar PostgreSQL al Proyecto

1. **Accede a Railway Dashboard**
   - Ve a: https://railway.app/dashboard
   - Busca el proyecto: **mtn-admission-backend**
   - Click para abrirlo

2. **Agregar servicio PostgreSQL**
   - Click en el botón **"+ New"** (esquina superior derecha)
   - Selecciona **"Database"**
   - Click en **"Add PostgreSQL"**
   - Railway creará automáticamente una instancia de PostgreSQL

3. **Espera a que PostgreSQL esté listo**
   - El servicio mostrará "Deploying..." y luego "Active"
   - Esto toma aproximadamente 1-2 minutos

## Paso 2: Configurar Variables de Entorno

1. **Encuentra tu servicio Backend**
   - En el dashboard del proyecto, verás varios servicios
   - Click en el servicio que está corriendo (el que tiene tu código)
   - **NO** hagas click en el servicio "Postgres"

2. **Ir a la pestaña Variables**
   - Click en la pestaña **"Variables"**
   - Verás variables existentes (si las hay)

3. **Agregar las siguientes variables**

   Click en **"New Variable"** y agrega cada una:

   | Variable | Valor | Descripción |
   |----------|-------|-------------|
   | `DB_HOST` | `${{Postgres.PGHOST}}` | Host de PostgreSQL (referencia) |
   | `DB_PORT` | `${{Postgres.PGPORT}}` | Puerto de PostgreSQL (referencia) |
   | `DB_NAME` | `${{Postgres.PGDATABASE}}` | Nombre de la base de datos |
   | `DB_USERNAME` | `${{Postgres.PGUSER}}` | Usuario de PostgreSQL |
   | `DB_PASSWORD` | `${{Postgres.PGPASSWORD}}` | Contraseña de PostgreSQL |
   | `JWT_SECRET` | `mtn_secure_jwt_secret_2025_prod` | Secreto para JWT (cambia por uno más seguro) |
   | `NODE_ENV` | `production` | Entorno de producción |

   **Importante**: Las variables que empiezan con `${{Postgres.` son referencias automáticas de Railway que toman los valores del servicio PostgreSQL.

4. **Guardar cambios**
   - Railway guardará automáticamente cada variable
   - El servicio se reiniciará automáticamente

## Paso 3: Conectarse a PostgreSQL para Crear las Tablas

### Opción A: Usar Railway CLI (Recomendado)

1. **Abrir terminal** y navegar al directorio del backend:
   ```bash
   cd "/Users/jorgegangale/Library/Mobile Documents/com~apple~CloudDocs/Proyectos/Admision_MTN/Admision_MTN_backend"
   ```

2. **Conectarse a PostgreSQL de Railway**:
   ```bash
   railway link
   # Selecciona: mtn-admission-backend

   railway service
   # Selecciona el servicio: Postgres

   railway run psql
   ```

3. **Ejecutar el script SQL**:
   Una vez conectado a PostgreSQL, ejecuta:
   ```sql
   \i railway-db-setup.sql
   ```

### Opción B: Usar Railway Dashboard

1. **Ir al servicio PostgreSQL**
   - En el dashboard, click en el servicio **"Postgres"**

2. **Ir a la pestaña "Data"**
   - Click en **"Data"**
   - Verás un botón **"Query"**

3. **Copiar y pegar el contenido de `railway-db-setup.sql`**
   - Abre el archivo `railway-db-setup.sql` en un editor
   - Copia todo el contenido
   - Pégalo en el editor de consultas de Railway
   - Click en **"Run"** o **"Execute"**

### Opción C: Conectarse manualmente con psql

1. **Obtener credenciales de conexión**
   - En Railway, ve al servicio Postgres
   - Pestaña **"Connect"**
   - Copia la **"Postgres Connection URL"**

2. **Conectarse desde terminal**:
   ```bash
   psql "postgresql://usuario:password@host:puerto/database"
   ```

3. **Ejecutar el script**:
   ```sql
   \i railway-db-setup.sql
   ```

## Paso 4: Verificar que Todo Funciona

1. **Espera 1-2 minutos** para que el backend se reinicie con las nuevas variables

2. **Prueba el health check**:
   ```bash
   curl https://admisionmtnbackendv2-production.up.railway.app/health
   ```

   Debería responder:
   ```json
   {
     "status": "UP",
     "timestamp": "2025-10-07T...",
     "service": "MTN Admission Backend",
     "environment": "production",
     "port": "8080"
   }
   ```

3. **Prueba el login**:
   ```bash
   curl -X POST https://admisionmtnbackendv2-production.up.railway.app/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"jorge.gangale@mtn.cl","password":"admin123"}'
   ```

   Debería responder con un token JWT:
   ```json
   {
     "token": "eyJhbGc...",
     "user": {
       "id": 1,
       "email": "jorge.gangale@mtn.cl",
       "role": "ADMIN",
       "firstName": "Jorge",
       "lastName": "Gangale"
     }
   }
   ```

4. **Prueba el frontend**:
   - Ve a: https://admision-mtn-frontend.vercel.app
   - Intenta hacer login con:
     - Email: `jorge.gangale@mtn.cl`
     - Password: `admin123`

## Paso 5: Cargar Datos de Prueba (Opcional)

Si quieres cargar datos de prueba de tu base de datos local:

1. **Exportar datos locales**:
   ```bash
   PGPASSWORD=admin123 pg_dump -h localhost -U admin -d "Admisión_MTN_DB" \
     --data-only \
     --exclude-table=users \
     > local-data.sql
   ```

2. **Importar a Railway**:
   ```bash
   railway run psql < local-data.sql
   ```

## Troubleshooting

### Error: "Internal server error" al hacer login

**Causa**: La base de datos no está conectada o las tablas no existen.

**Solución**:
1. Verifica que PostgreSQL esté corriendo en Railway
2. Verifica que las variables de entorno estén configuradas
3. Ejecuta el script `railway-db-setup.sql`

### Error: "Invalid credentials"

**Causa**: El usuario admin no existe o el password es incorrecto.

**Solución**:
1. Verifica que ejecutaste el script SQL completamente
2. El script incluye el INSERT del usuario admin
3. El password es: `admin123`

### El backend no se conecta a PostgreSQL

**Causa**: Variables de entorno mal configuradas.

**Solución**:
1. Ve a Railway → tu servicio Backend → Variables
2. Verifica que todas las variables `DB_*` estén configuradas
3. Verifica que las referencias `${{Postgres.*}}` sean correctas
4. El servicio debe reiniciarse automáticamente al cambiar variables

### No puedo conectarme con railway CLI

**Solución**:
1. Verifica que railway CLI esté instalado: `railway --version`
2. Si no está instalado: `npm install -g @railway/cli`
3. Login: `railway login`
4. Link: `railway link` y selecciona el proyecto

## URLs Importantes

- **Backend**: https://admisionmtnbackendv2-production.up.railway.app
- **Frontend**: https://admision-mtn-frontend.vercel.app
- **Railway Dashboard**: https://railway.app/dashboard
- **Vercel Dashboard**: https://vercel.com/dashboard

## Credenciales de Prueba

- **Email**: jorge.gangale@mtn.cl
- **Password**: admin123
- **Role**: ADMIN

## Próximos Pasos

Una vez que todo funcione:

1. ✅ Cambiar el `JWT_SECRET` por uno más seguro
2. ✅ Agregar más endpoints al backend según se necesiten
3. ✅ Configurar dominio personalizado (opcional)
4. ✅ Configurar monitoreo y logs
5. ✅ Agregar más usuarios de prueba

## Soporte

Si tienes problemas, revisa los logs en Railway:
1. Ve al servicio Backend
2. Pestaña **"Deployments"**
3. Click en el deployment más reciente
4. Click en **"View Logs"**
