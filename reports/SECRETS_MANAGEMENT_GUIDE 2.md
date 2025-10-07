# 🔐 Guía de Gestión de Secretos - Sistema de Admisión MTN

**Fecha:** 2025-10-01  
**Versión:** 1.0  
**Propósito:** Documentar buenas prácticas para manejo seguro de credenciales

---

## 📋 TABLA DE CONTENIDOS

1. [Resumen Ejecutivo](#resumen-ejecutivo)
2. [Problemas Identificados](#problemas-identificados)
3. [Soluciones Implementadas](#soluciones-implementadas)
4. [Guía por Entorno](#guía-por-entorno)
5. [Herramientas Recomendadas](#herramientas-recomendadas)
6. [Mejores Prácticas](#mejores-prácticas)
7. [Plan de Rotación de Secretos](#plan-de-rotación-de-secretos)
8. [Checklist de Seguridad](#checklist-de-seguridad)

---

## 🎯 RESUMEN EJECUTIVO

### Antes de las Correcciones
- ❌ **DB_PASSWORD**: Hardcoded como "admin123"
- ❌ **JWT_SECRET**: Valor genérico "your_secure_jwt_secret"
- ❌ **SMTP_PASSWORD**: Password expuesto en .env
- ❌ **Total de secretos en riesgo:** 3 críticos

### Después de las Correcciones
- ✅ **JWT_SECRET**: Generado con 512 bits aleatorios (crypto.randomBytes)
- ✅ **.env**: Actualizado con warnings de seguridad
- ✅ **.env.example**: Template sin secretos reales
- ✅ **Documentación**: Guía completa de secrets management
- ✅ **Script de verificación**: Detección automática de secretos débiles

---

## 🔴 PROBLEMAS IDENTIFICADOS

### 1. Contraseña de Base de Datos Hardcoded

**Problema:**
```bash
DB_PASSWORD=admin123  # ❌ Password débil y visible en .env
```

**Riesgos:**
- Si el repositorio se hace público, la BD queda expuesta
- Password débil vulnerable a fuerza bruta
- Misma contraseña en desarrollo y producción

**Solución Aplicada:**
```bash
# 🔴 DEVELOPMENT ONLY - Use AWS Secrets Manager/Azure Key Vault in production
# NEVER commit real passwords to version control
DB_PASSWORD=admin123  # OK para desarrollo local
```

---

### 2. JWT Secret Genérico

**Problema:**
```bash
JWT_SECRET=your_secure_jwt_secret  # ❌ Valor predecible
```

**Riesgos:**
- Tokens JWT pueden ser falsificados
- Atacante puede generar tokens válidos
- Compromiso total de autenticación

**Solución Aplicada:**
```bash
# Secreto de 512 bits generado con crypto.randomBytes(64)
JWT_SECRET=509ce9cae62cc6dacd3bc45d9037bbb31cf9427b5ffb2249b291edb1df6b14d991dc8cc6a3ef65ddad4a87f8857a1072d48f7f813b23994eecefd080162f9063
```

---

### 3. SMTP Password Expuesto

**Problema:**
```bash
SMTP_PASSWORD=yaejhysibcgifpng  # ❌ Password real visible
```

**Riesgos:**
- Acceso a cuenta de correo institucional
- Posible spam o phishing desde cuenta oficial
- Violación de políticas de seguridad de Gmail

**Solución Aplicada:**
```bash
# 🔴 DEVELOPMENT ONLY - Use app-specific password or OAuth2 in production
# Gmail App Password: https://support.google.com/accounts/answer/185833
SMTP_PASSWORD=CHANGE_TO_INSTITUTIONAL_APP_PASSWORD
```

---

## ✅ SOLUCIONES IMPLEMENTADAS

### 1. Generación de Secretos Seguros

**Comando para generar JWT secrets:**
```bash
node -e "console.log(require('crypto').randomBytes(64).toString('hex'))"
```

**Secretos generados:**

```bash
# Desarrollo
DEV_JWT_SECRET=509ce9cae62cc6dacd3bc45d9037bbb31cf9427b5ffb2249b291edb1df6b14d991dc8cc6a3ef65ddad4a87f8857a1072d48f7f813b23994eecefd080162f9063

# Producción
PROD_JWT_SECRET=b94b8bb8d6842a3d2c8583bc0636f7b813f80ec4baffa1f5129c37e8b708dc0e2350b69747ea170b0953480b15325b12ade005bc256019901a04393a5c91accb

# Staging
STAGING_JWT_SECRET=0386517902f4cb8e804cef65192fcdbd26b4e5b0c0fe8233b032254e3bcadd0dc4aebdb3a68a66aefb45bb559c61655996ca9c13c040d6d5dbfe989dc7e30b0d
```

### 2. Template .env.example Seguro

**Ubicación:** `Admision_MTN_backend/.env.example.new`

**Características:**
- ✅ No contiene secretos reales
- ✅ Incluye instrucciones de generación
- ✅ Links a documentación oficial
- ✅ Warnings de seguridad claros
- ✅ Ejemplos de integración con secrets managers

### 3. Actualización de .env con Warnings

**Ubicación:** `Admision_MTN_backend/.env`

**Cambios aplicados:**
- ✅ JWT_SECRET actualizado a valor aleatorio de 512 bits
- ✅ Warnings de seguridad agregados
- ✅ Instrucciones de uso por entorno
- ✅ Links a guías de App-Specific Passwords

---

## 🌍 GUÍA POR ENTORNO

### Desarrollo Local

**Objetivo:** Seguridad básica, facilidad de uso

```bash
# .env.local (NO COMMITEAR)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=Admisión_MTN_DB
DB_USERNAME=admin
DB_PASSWORD=admin123  # OK para desarrollo local

JWT_SECRET=509ce9cae62cc6dacd3bc45d9037bbb31cf9427b5ffb2249b291edb1df6b14d991dc8cc6a3ef65ddad4a87f8857a1072d48f7f813b23994eecefd080162f9063
JWT_EXPIRATION_TIME=86400000

SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=dev@mtn.cl
SMTP_PASSWORD=your_app_specific_password_here
```

**Checklist de Desarrollo:**
- [ ] .env está en .gitignore
- [ ] Usar DB local con password temporal
- [ ] JWT_SECRET generado aleatoriamente
- [ ] SMTP con App-Specific Password

---

### Staging/QA

**Objetivo:** Simulación de producción, datos de prueba

**Opción 1: Variables de Entorno (Heroku, Render, etc.)**
```bash
# No usar archivos .env en staging
# Configurar variables en plataforma:

heroku config:set DB_HOST=staging-db.mtn.cl
heroku config:set DB_PASSWORD=$(openssl rand -base64 32)
heroku config:set JWT_SECRET=0386517902f4cb8e804cef65192fcdbd26b4e5b0c0fe8233b032254e3bcadd0dc4aebdb3a68a66aefb45bb559c61655996ca9c13c040d6d5dbfe989dc7e30b0d
```

**Opción 2: AWS Secrets Manager**
```javascript
// secrets-loader.js
const AWS = require('aws-sdk');
const secretsManager = new AWS.SecretsManager({ region: 'us-east-1' });

async function loadSecrets() {
  const secret = await secretsManager.getSecretValue({
    SecretId: 'admision-mtn-staging'
  }).promise();
  
  const secrets = JSON.parse(secret.SecretString);
  
  process.env.DB_PASSWORD = secrets.DB_PASSWORD;
  process.env.JWT_SECRET = secrets.JWT_SECRET;
  process.env.SMTP_PASSWORD = secrets.SMTP_PASSWORD;
}

module.exports = { loadSecrets };
```

**Checklist de Staging:**
- [ ] Usar secrets manager (AWS/Azure/GCP)
- [ ] Credentials diferentes a producción
- [ ] JWT_SECRET único para staging
- [ ] Logs de acceso habilitados
- [ ] Database isolada de producción

---

### Producción

**Objetivo:** Máxima seguridad, compliance

**AWS Secrets Manager (Recomendado)**

```javascript
// production-secrets.js
const AWS = require('aws-sdk');
const secretsManager = new AWS.SecretsManager({ 
  region: process.env.AWS_REGION || 'us-east-1' 
});

async function getSecret(secretName) {
  try {
    const data = await secretsManager.getSecretValue({ 
      SecretId: secretName 
    }).promise();
    
    if ('SecretString' in data) {
      return JSON.parse(data.SecretString);
    } else {
      const buff = Buffer.from(data.SecretBinary, 'base64');
      return JSON.parse(buff.toString('ascii'));
    }
  } catch (error) {
    console.error(`Error retrieving secret ${secretName}:`, error);
    throw error;
  }
}

// Cargar secretos al inicio de la aplicación
async function initSecrets() {
  const dbSecrets = await getSecret('admision-mtn/database');
  const jwtSecrets = await getSecret('admision-mtn/jwt');
  const smtpSecrets = await getSecret('admision-mtn/smtp');
  
  process.env.DB_HOST = dbSecrets.host;
  process.env.DB_PORT = dbSecrets.port;
  process.env.DB_NAME = dbSecrets.database;
  process.env.DB_USERNAME = dbSecrets.username;
  process.env.DB_PASSWORD = dbSecrets.password;
  
  process.env.JWT_SECRET = jwtSecrets.secret;
  process.env.JWT_EXPIRATION_TIME = jwtSecrets.expirationTime;
  
  process.env.SMTP_HOST = smtpSecrets.host;
  process.env.SMTP_PORT = smtpSecrets.port;
  process.env.SMTP_USERNAME = smtpSecrets.username;
  process.env.SMTP_PASSWORD = smtpSecrets.password;
}

module.exports = { initSecrets };
```

**Azure Key Vault (Alternativa)**

```javascript
// azure-secrets.js
const { SecretClient } = require("@azure/keyvault-secrets");
const { DefaultAzureCredential } = require("@azure/identity");

const vaultName = process.env.AZURE_KEY_VAULT_NAME;
const url = `https://${vaultName}.vault.azure.net`;

const credential = new DefaultAzureCredential();
const client = new SecretClient(url, credential);

async function getSecret(secretName) {
  const secret = await client.getSecret(secretName);
  return secret.value;
}

async function initSecrets() {
  process.env.DB_PASSWORD = await getSecret("db-password");
  process.env.JWT_SECRET = await getSecret("jwt-secret");
  process.env.SMTP_PASSWORD = await getSecret("smtp-password");
}

module.exports = { initSecrets };
```

**Google Cloud Secret Manager (Alternativa)**

```javascript
// gcp-secrets.js
const {SecretManagerServiceClient} = require('@google-cloud/secret-manager');
const client = new SecretManagerServiceClient();

async function getSecret(secretName) {
  const projectId = process.env.GCP_PROJECT_ID;
  const name = `projects/${projectId}/secrets/${secretName}/versions/latest`;
  
  const [version] = await client.accessSecretVersion({ name });
  const payload = version.payload.data.toString('utf8');
  
  return payload;
}

async function initSecrets() {
  process.env.DB_PASSWORD = await getSecret('db-password');
  process.env.JWT_SECRET = await getSecret('jwt-secret');
  process.env.SMTP_PASSWORD = await getSecret('smtp-password');
}

module.exports = { initSecrets };
```

**Checklist de Producción:**
- [ ] **NUNCA** usar archivos .env en producción
- [ ] Usar secrets manager (AWS/Azure/GCP)
- [ ] JWT_SECRET único de 512+ bits
- [ ] Database password rotado cada 90 días
- [ ] SMTP con OAuth2 o App-Specific Password
- [ ] Logs de acceso a secretos habilitados
- [ ] Alertas de acceso no autorizado configuradas
- [ ] Secrets encriptados en tránsito y en reposo
- [ ] Backups de secretos en vault separado

---

## 🛠️ HERRAMIENTAS RECOMENDADAS

### 1. AWS Secrets Manager

**Pros:**
- ✅ Rotación automática de secretos
- ✅ Auditoría completa con CloudTrail
- ✅ Encriptación con KMS
- ✅ Versionado de secretos

**Pricing:**
- $0.40 por secreto/mes
- $0.05 por 10,000 llamadas a API

**Setup:**
```bash
# Crear secreto
aws secretsmanager create-secret \
  --name admision-mtn/database \
  --secret-string '{"username":"admin","password":"RANDOM_PASSWORD_HERE"}'

# Leer secreto
aws secretsmanager get-secret-value \
  --secret-id admision-mtn/database
```

---

### 2. Azure Key Vault

**Pros:**
- ✅ Integración con Azure AD
- ✅ Certificados SSL incluidos
- ✅ HSM-backed secrets (alta seguridad)

**Pricing:**
- Gratis para primeros 1000 operaciones
- $0.03 por 10,000 operaciones después

**Setup:**
```bash
# Crear Key Vault
az keyvault create \
  --name admision-mtn-vault \
  --resource-group admision-rg \
  --location eastus

# Agregar secreto
az keyvault secret set \
  --vault-name admision-mtn-vault \
  --name db-password \
  --value "RANDOM_PASSWORD_HERE"
```

---

### 3. Google Cloud Secret Manager

**Pros:**
- ✅ Integración nativa con GCP
- ✅ IAM granular
- ✅ Versionado automático

**Pricing:**
- $0.06 por secreto activo/mes
- Primeros 10,000 accesos gratis

**Setup:**
```bash
# Crear secreto
echo -n "RANDOM_PASSWORD_HERE" | \
  gcloud secrets create db-password \
  --data-file=-

# Leer secreto
gcloud secrets versions access latest --secret="db-password"
```

---

### 4. Doppler (Recomendado para Startups)

**Pros:**
- ✅ UI amigable
- ✅ Sync automático con entornos
- ✅ CLI poderoso
- ✅ Integraciones con frameworks

**Pricing:**
- Free tier: 3 usuarios, 1 proyecto
- Pro: $8/usuario/mes

**Setup:**
```bash
# Instalar CLI
brew install dopplerhq/cli/doppler

# Login
doppler login

# Setup proyecto
doppler setup

# Correr app con secretos inyectados
doppler run -- npm start
```

---

### 5. Vault by HashiCorp (On-Premise)

**Pros:**
- ✅ Self-hosted (control total)
- ✅ Rotación dinámica
- ✅ Múltiples auth methods

**Contras:**
- ❌ Requiere mantenimiento
- ❌ Complejidad de setup

**Setup:**
```bash
# Iniciar Vault
vault server -dev

# Escribir secreto
vault kv put secret/admision-mtn db_password="RANDOM_PASSWORD"

# Leer secreto
vault kv get secret/admision-mtn
```

---

## 📚 MEJORES PRÁCTICAS

### 1. Principio de Menor Privilegio

```javascript
// ❌ MAL: Un solo secreto con todos los permisos
DB_USER=admin  // Tiene permisos de DROP, ALTER, etc.

// ✅ BIEN: Secretos específicos por rol
DB_USER_APP=app_user      // Solo SELECT, INSERT, UPDATE
DB_USER_MIGRATION=migrator // Solo ALTER, CREATE
DB_USER_ADMIN=admin        // Todos los permisos (solo para DBA)
```

---

### 2. Separación por Entorno

```bash
# ✅ BIEN: Secretos diferentes por entorno
Development:   JWT_SECRET=dev_secret_abc123
Staging:       JWT_SECRET=staging_secret_xyz789
Production:    JWT_SECRET=prod_secret_lmn456

# ❌ MAL: Mismo secreto en todos los entornos
JWT_SECRET=shared_secret_for_all
```

---

### 3. Rotación Regular

```javascript
// Ejemplo de rotación automática
const SECRET_MAX_AGE_DAYS = 90;

async function checkSecretAge() {
  const secretMetadata = await getSecretMetadata('jwt-secret');
  const ageInDays = (Date.now() - secretMetadata.createdAt) / (1000 * 60 * 60 * 24);
  
  if (ageInDays > SECRET_MAX_AGE_DAYS) {
    console.warn(`⚠️ Secret 'jwt-secret' is ${ageInDays} days old. Rotation recommended.`);
    // Trigger alert to DevOps team
  }
}
```

**Calendario de Rotación:**
- 🔴 **Database passwords:** Cada 90 días
- 🟡 **JWT secrets:** Cada 180 días
- 🟢 **API keys:** Cada 365 días
- 🔵 **SMTP passwords:** Al detectar uso no autorizado

---

### 4. Auditoría y Monitoreo

```javascript
// Ejemplo de logging de acceso a secretos
const winston = require('winston');

const logger = winston.createLogger({
  level: 'info',
  format: winston.format.json(),
  transports: [
    new winston.transports.File({ filename: 'secrets-access.log' })
  ]
});

async function getSecretWithLogging(secretName) {
  const startTime = Date.now();
  
  try {
    const secret = await getSecret(secretName);
    
    logger.info({
      event: 'SECRET_ACCESS',
      secret: secretName,
      user: process.env.USER,
      timestamp: new Date().toISOString(),
      duration_ms: Date.now() - startTime,
      status: 'SUCCESS'
    });
    
    return secret;
  } catch (error) {
    logger.error({
      event: 'SECRET_ACCESS_FAILED',
      secret: secretName,
      error: error.message,
      timestamp: new Date().toISOString()
    });
    throw error;
  }
}
```

---

### 5. Detección de Secretos en Código

**git-secrets (Pre-commit Hook)**

```bash
# Instalar
brew install git-secrets

# Configurar
git secrets --install
git secrets --register-aws

# Agregar reglas personalizadas
git secrets --add 'DB_PASSWORD=.*'
git secrets --add 'JWT_SECRET=.*'
git secrets --add '[0-9a-f]{32,}'  # Detectar hashes largos
```

**TruffleHog (CI/CD)**

```yaml
# .github/workflows/security.yml
name: Detect Secrets
on: [push]

jobs:
  trufflehog:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: TruffleHog
        uses: trufflesecurity/trufflehog@main
        with:
          path: ./
          base: main
          head: HEAD
```

---

## 📅 PLAN DE ROTACIÓN DE SECRETOS

### Proceso de Rotación de JWT_SECRET

**Paso 1: Generar nuevo secreto**
```bash
NEW_JWT_SECRET=$(node -e "console.log(require('crypto').randomBytes(64).toString('hex'))")
echo "New JWT Secret: $NEW_JWT_SECRET"
```

**Paso 2: Implementar soporte de múltiples secretos (zero-downtime)**
```javascript
// auth-middleware.js
const OLD_JWT_SECRET = process.env.JWT_SECRET;
const NEW_JWT_SECRET = process.env.NEW_JWT_SECRET;

function verifyToken(token) {
  try {
    // Intentar verificar con el nuevo secreto
    return jwt.verify(token, NEW_JWT_SECRET);
  } catch (error) {
    // Fallback al secreto viejo durante período de transición
    return jwt.verify(token, OLD_JWT_SECRET);
  }
}

// Solo firmar tokens nuevos con NEW_JWT_SECRET
function signToken(payload) {
  return jwt.sign(payload, NEW_JWT_SECRET, { expiresIn: '24h' });
}
```

**Paso 3: Deployment y transición (7 días)**
```bash
# Día 0: Deploy con soporte de ambos secretos
JWT_SECRET=old_secret_here
NEW_JWT_SECRET=new_secret_here

# Día 7: Después de expiración de todos los tokens viejos
# Promover nuevo secreto a principal
JWT_SECRET=new_secret_here
# Remover NEW_JWT_SECRET
```

**Paso 4: Verificación post-rotación**
```bash
# Verificar que no haya tokens viejos activos
SELECT COUNT(*) FROM sessions WHERE created_at < NOW() - INTERVAL '7 days';
```

---

### Proceso de Rotación de DB_PASSWORD

**Paso 1: Crear nuevo usuario temporal**
```sql
-- En PostgreSQL
CREATE USER app_user_new WITH PASSWORD 'NEW_SECURE_PASSWORD';
GRANT CONNECT ON DATABASE "Admisión_MTN_DB" TO app_user_new;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_user_new;
```

**Paso 2: Actualizar configuración (blue-green deployment)**
```javascript
// database-config.js
const pool = new Pool({
  host: process.env.DB_HOST,
  port: process.env.DB_PORT,
  database: process.env.DB_NAME,
  user: process.env.DB_USERNAME_NEW || process.env.DB_USERNAME,
  password: process.env.DB_PASSWORD_NEW || process.env.DB_PASSWORD,
});
```

**Paso 3: Deployment y verificación**
```bash
# Deploy con nuevo usuario
DB_USERNAME_NEW=app_user_new
DB_PASSWORD_NEW=NEW_SECURE_PASSWORD

# Verificar conexiones activas
SELECT usename, COUNT(*) 
FROM pg_stat_activity 
WHERE datname = 'Admisión_MTN_DB' 
GROUP BY usename;
```

**Paso 4: Cleanup**
```sql
-- Después de 24h sin errores
DROP USER app_user_old;
```

---

## ✅ CHECKLIST DE SEGURIDAD

### Pre-Deployment

- [ ] **Generar secretos únicos** para cada entorno
- [ ] **Verificar .gitignore** incluye .env, .env.local, .env.*.local
- [ ] **Escanear commits** con git-secrets o TruffleHog
- [ ] **Configurar secrets manager** (AWS/Azure/GCP/Doppler)
- [ ] **Documentar proceso** de rotación de secretos
- [ ] **Setup alertas** de acceso no autorizado

### Post-Deployment

- [ ] **Verificar secretos** no están en logs
- [ ] **Confirmar encriptación** en tránsito (HTTPS/TLS)
- [ ] **Revisar permisos** de IAM/RBAC
- [ ] **Probar rollback** de secretos
- [ ] **Validar monitoreo** de accesos
- [ ] **Documentar incidentes** de seguridad

### Mantenimiento Continuo

- [ ] **Rotar DB_PASSWORD** cada 90 días
- [ ] **Rotar JWT_SECRET** cada 180 días
- [ ] **Auditar logs** de acceso a secretos semanalmente
- [ ] **Revisar permisos** mensualmente
- [ ] **Actualizar dependencias** con vulnerabilidades
- [ ] **Entrenar equipo** en mejores prácticas (trimestral)

---

## 📞 CONTACTO Y SOPORTE

**Responsable de Seguridad:** DevOps Team  
**Email:** devops@mtn.cl  
**Escalación:** Tech Lead

**Reportar Incidente de Seguridad:**
1. Email inmediato a: security@mtn.cl
2. Slack: #security-incidents
3. Rotación de secretos comprometidos en <4 horas

---

## 📚 RECURSOS ADICIONALES

**Documentación Oficial:**
- [AWS Secrets Manager](https://docs.aws.amazon.com/secretsmanager/)
- [Azure Key Vault](https://docs.microsoft.com/azure/key-vault/)
- [GCP Secret Manager](https://cloud.google.com/secret-manager/docs)
- [Doppler Documentation](https://docs.doppler.com/)

**Herramientas de Seguridad:**
- [git-secrets](https://github.com/awslabs/git-secrets)
- [TruffleHog](https://github.com/trufflesecurity/trufflehog)
- [detect-secrets](https://github.com/Yelp/detect-secrets)

**Estándares de Seguridad:**
- [OWASP Secrets Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)
- [NIST SP 800-57 (Key Management)](https://csrc.nist.gov/publications/detail/sp/800-57-part-1/rev-5/final)

---

**Fin de la Guía** | Generado: 2025-10-01 | Versión 1.0
