# 🔐 Guía de Seguridad - Variables de Entorno

## ⚠️ Principio Fundamental

**NUNCA hardcodees credenciales en el código. SIEMPRE usa variables de entorno.**

## 📋 Variables Sensibles en este Proyecto

### 1. Base de Datos (PostgreSQL)

```bash
DB_HOST=${{Postgres.PGHOST}}
DB_PORT=${{Postgres.PGPORT}}
DB_NAME=${{Postgres.PGDATABASE}}
DB_USERNAME=${{Postgres.PGUSER}}
DB_PASSWORD=${{Postgres.PGPASSWORD}}
```

**✅ Correcto**: Railway maneja estas automáticamente
**❌ Incorrecto**: Hardcodear en el código

### 2. JWT Secret

```bash
JWT_SECRET=mtn_secure_jwt_secret_2025_production_v1
```

**Propósito**: Firmar y verificar tokens de autenticación

**Mejores prácticas**:
- ✅ Usa un string aleatorio largo (mínimo 32 caracteres)
- ✅ Diferente en desarrollo y producción
- ✅ NUNCA lo compartas públicamente
- ✅ Si se compromete, genera uno nuevo inmediatamente

**Generar un JWT_SECRET seguro**:
```bash
# Opción 1: OpenSSL
openssl rand -base64 64

# Opción 2: Node.js
node -e "console.log(require('crypto').randomBytes(64).toString('base64'))"

# Opción 3: Online (solo para desarrollo)
# https://www.grc.com/passwords.htm
```

### 3. SMTP (Email)

```bash
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=jorge.gangale@mtn.cl
SMTP_PASSWORD=yaejhysibcgifpng  # App Password de Gmail
EMAIL_MOCK_MODE=false
```

**⚠️ CRÍTICO - SMTP_PASSWORD**:

Esta NO es tu contraseña normal de Gmail. Es una "App Password" generada específicamente para aplicaciones.

**¿Por qué App Password?**
- Gmail bloquea login de apps con contraseña normal por seguridad
- Las App Passwords son específicas para cada aplicación
- Puedes revocarlas sin cambiar tu contraseña principal
- Más seguro si la app es comprometida

**Cómo generar Gmail App Password**:

1. **Habilitar verificación en 2 pasos**:
   - Ve a: https://myaccount.google.com/security
   - Busca "2-Step Verification"
   - Actívala si no está activada

2. **Generar App Password**:
   - Ve a: https://myaccount.google.com/apppasswords
   - O busca "App passwords" en tu cuenta de Google
   - Selecciona "Mail" como app
   - Selecciona "Other" como device
   - Nombre: "MTN Admission System"
   - Click "Generate"
   - Copia la password de 16 caracteres (sin espacios)

3. **Usar en Railway**:
   ```bash
   SMTP_PASSWORD=abcdabcdabcdabcd  # La password generada
   ```

**Alternativas a Gmail**:
- SendGrid (2000 emails/día gratis)
- Mailgun (5000 emails/mes gratis)
- AWS SES (muy barato, requiere configuración)
- Resend (3000 emails/mes gratis)

## 🔒 Dónde Almacenar Variables

### ✅ Lugares SEGUROS

1. **Railway (Producción)**:
   - Variables tab en cada servicio
   - Encriptadas automáticamente
   - No visibles en logs
   - Sincronizadas entre deployments

2. **Archivo .env (Desarrollo LOCAL)**:
   ```bash
   # .env (en tu computadora solamente)
   DB_HOST=localhost
   DB_PORT=5432
   DB_USERNAME=admin
   DB_PASSWORD=admin123
   JWT_SECRET=dev_secret_not_for_production
   SMTP_PASSWORD=your_app_password_here
   ```

   **IMPORTANTE**:
   - ✅ `.env` debe estar en `.gitignore`
   - ❌ NUNCA hacer commit de `.env`

3. **Gestores de Secretos (Opcional)**:
   - AWS Secrets Manager
   - HashiCorp Vault
   - 1Password Secrets Automation
   - Doppler

### ❌ Lugares INSEGUROS

1. **❌ Código fuente**:
   ```javascript
   // ❌ MALO - NUNCA hagas esto
   const password = "admin123";
   const jwtSecret = "my_secret_key";
   ```

2. **❌ Archivos de configuración commiteados**:
   ```json
   // ❌ config.json (si está en Git)
   {
     "database": {
       "password": "admin123"  // MAL
     }
   }
   ```

3. **❌ Logs o consola**:
   ```javascript
   // ❌ MALO
   console.log("DB Password:", process.env.DB_PASSWORD);
   ```

4. **❌ URLs o query strings**:
   ```javascript
   // ❌ MALO
   fetch(`/api/login?password=${password}`);
   ```

5. **❌ Comentarios en el código**:
   ```javascript
   // ❌ MALO
   // Usar password: admin123
   ```

## 🛡️ Mejores Prácticas

### 1. Usar dotenv en Desarrollo

```bash
npm install dotenv
```

```javascript
// En el inicio de tu app
require('dotenv').config();

// Ahora puedes usar:
const dbPassword = process.env.DB_PASSWORD;
const jwtSecret = process.env.JWT_SECRET;
```

### 2. Validar Variables al Inicio

```javascript
// Valida que existan al arrancar
const requiredEnvVars = [
  'DB_HOST',
  'DB_PASSWORD',
  'JWT_SECRET',
  'SMTP_PASSWORD'
];

requiredEnvVars.forEach(varName => {
  if (!process.env[varName]) {
    console.error(`❌ Error: Variable de entorno ${varName} no configurada`);
    process.exit(1);
  }
});
```

### 3. Nunca Logear Secretos

```javascript
// ✅ BUENO
console.log('Connecting to database...');

// ❌ MALO
console.log('DB Password:', process.env.DB_PASSWORD);
```

### 4. Usar Valores por Defecto Solo para Dev

```javascript
// ✅ BUENO - Solo para desarrollo
const dbHost = process.env.DB_HOST || 'localhost';

// ❌ MALO - Nunca defaults para passwords
const dbPassword = process.env.DB_PASSWORD || 'default_password';
```

### 5. Diferentes Valores por Entorno

```bash
# .env.development
DB_HOST=localhost
JWT_SECRET=dev_secret_12345

# .env.production (en Railway)
DB_HOST=${{Postgres.PGHOST}}
JWT_SECRET=prod_secret_very_long_and_secure_67890
```

## 🔍 Auditoría de Seguridad

### Verificar que NO hay secretos en Git

```bash
# Buscar posibles passwords hardcodeadas
cd Admision_MTN_backend
grep -r "password.*=" --include="*.js" | grep -v "process.env"

# Buscar API keys
grep -r "api_key\|apiKey\|API_KEY" --include="*.js" | grep -v "process.env"

# Buscar tokens
grep -r "token.*=" --include="*.js" | grep -v "process.env"
```

### Verificar .gitignore

```bash
# Debe incluir:
.env
.env.local
.env.production
.env.*.local
*.log
node_modules/
```

## 📝 Checklist de Seguridad

- [ ] Todas las passwords están en variables de entorno
- [ ] `.env` está en `.gitignore`
- [ ] No hay secretos hardcodeados en el código
- [ ] JWT_SECRET es diferente en dev y producción
- [ ] SMTP_PASSWORD es una App Password, no la normal
- [ ] Variables de entorno validadas al arrancar la app
- [ ] No hay logs de passwords o tokens
- [ ] Archivo `.env.example` creado (sin valores reales)

## 📄 Crear .env.example

```bash
# .env.example - Para que otros developers sepan qué variables necesitan
# NO incluye valores reales

# Database
DB_HOST=
DB_PORT=5432
DB_NAME=
DB_USERNAME=
DB_PASSWORD=

# JWT
JWT_SECRET=

# Email (SMTP)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=
SMTP_PASSWORD=
EMAIL_MOCK_MODE=true
```

Commitealo a Git para que otros sepan qué variables configurar.

## 🚨 Si un Secreto se Compromete

1. **Inmediatamente**:
   - Genera un nuevo secreto
   - Actualiza en Railway
   - Reinicia el servicio

2. **Para SMTP_PASSWORD**:
   - Revoca la App Password en Google
   - Genera una nueva
   - Actualiza en Railway

3. **Para JWT_SECRET**:
   - Genera uno nuevo
   - Actualiza en Railway
   - Todos los usuarios tendrán que hacer login de nuevo

4. **Para DB_PASSWORD**:
   - Cambia la password en PostgreSQL
   - Actualiza en todas las variables de Railway

## 🎓 Recursos

- [OWASP Secrets Management](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)
- [Railway Environment Variables](https://docs.railway.app/develop/variables)
- [dotenv Documentation](https://github.com/motdotla/dotenv)
- [Gmail App Passwords](https://support.google.com/accounts/answer/185833)

---

**Recuerda**: La seguridad es un proceso continuo, no un estado final. Revisa regularmente que no haya secretos expuestos.
