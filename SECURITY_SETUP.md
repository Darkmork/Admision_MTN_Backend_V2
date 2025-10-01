# 🔒 Guía de Configuración Segura
## Sistema de Admisión - Colegio Monte Tabor y Nazaret

---

## 🚨 **IMPORTANTE - LEE ANTES DE USAR EN PRODUCCIÓN**

Este sistema maneja **datos sensibles de menores de edad**. Es CRÍTICO seguir todas las medidas de seguridad antes de cualquier deployment en producción.

---

## 📋 **Configuración Rápida (Desarrollo)**

### 1. **Variables de Entorno**
```bash
# 1. Copia el archivo .env de ejemplo
cp .env.example .env

# 2. O ejecuta el script automático
./setup-security.sh

# 3. Edita .env con tus configuraciones
nano .env
```

### 2. **Configuración Básica Necesaria**
```bash
# Base de datos
DB_HOST=localhost
DB_PORT=5432
DB_NAME=Admisión_MTN_DB
DB_USERNAME=admin
DB_PASSWORD=TU_PASSWORD_SEGURA_AQUI

# JWT (CAMBIAR ESTE SECRET)
JWT_SECRET=TU_JWT_SECRET_SUPER_SEGURO_256_BITS_MINIMO
JWT_EXPIRATION_TIME=86400000

# Email (configurar con tu SMTP)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=tu_email@mtn.cl
SMTP_PASSWORD=tu_app_password_aqui
```

### 3. **Ejecutar Aplicación**
```bash
# Con variables de entorno
source .env && mvn spring-boot:run

# O usando las variables por defecto del application.yml
mvn spring-boot:run
```

---

## 🔐 **Configuración para Producción**

### 🛡️ **Paso 1: Credenciales Seguras**

#### **Generar JWT Secret Seguro**
```bash
# Generar secret de 512 bits (mínimo para HS512)
openssl rand -base64 64 | tr -d '\n'

# Ejemplo de output:
THbH5+S5jDiB0WdjNkFLsZZY5WrEz8KXjOFDPAp0w2s7DIte81a3X1NOR413Gbz+4cmtYYiF54gxoRraSyVnIg==
```

#### **Generar Password Hash BCrypt**
```bash
# Para generar hash de passwords de usuarios
htpasswd -bnBC 10 "" "tu_password" | tr -d ':\n' | sed 's/^.*://'

# O usar el script
./setup-security.sh
```

#### **Cambiar TODAS las Credenciales por Defecto**
```bash
# ❌ NUNCA uses estas credenciales en producción:
DB_PASSWORD=admin123
SMTP_PASSWORD=your_app_password_here
JWT_SECRET=default_secret

# ✅ Siempre genera nuevas:
DB_PASSWORD=$(openssl rand -base64 32)
SMTP_PASSWORD=tu_app_password_real
JWT_SECRET=$(openssl rand -base64 64)
```

### 🌐 **Paso 2: Configuración HTTPS/SSL**

#### **Certificado SSL (Producción)**
```yaml
# En application-production.yml
server:
  port: 443
  ssl:
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: mtn-ssl
    enabled: true

# Redirect HTTP to HTTPS
security:
  require-ssl: true
```

#### **Headers de Seguridad**
```java
// En SecurityConfig.java
@Override
protected void configure(HttpSecurity http) throws Exception {
    http.headers()
        .frameOptions().deny()
        .contentTypeOptions().and()
        .xssProtection().and()
        .httpStrictTransportSecurity(hstsConfig -> hstsConfig
            .maxAgeInSeconds(31536000)
            .includeSubdomains(true));
}
```

### 📧 **Paso 3: Email Servicio Profesional**

#### **SendGrid Configuration**
```bash
# Variables .env para SendGrid
SMTP_HOST=smtp.sendgrid.net
SMTP_PORT=587
SMTP_USERNAME=apikey
SMTP_PASSWORD=TU_SENDGRID_API_KEY

# O AWS SES
SMTP_HOST=email-smtp.us-west-2.amazonaws.com
SMTP_PORT=587
SMTP_USERNAME=TU_SES_ACCESS_KEY
SMTP_PASSWORD=TU_SES_SECRET_KEY
```

#### **Rate Limiting Email**
```java
// Implementar rate limiting
@Component
public class EmailRateLimiter {
    private final RedisTemplate redisTemplate;
    
    public boolean canSendEmail(String recipientEmail) {
        String key = "email_rate:" + recipientEmail;
        String count = redisTemplate.opsForValue().get(key);
        
        if (count == null) {
            redisTemplate.opsForValue().set(key, "1", Duration.ofHours(1));
            return true;
        }
        
        int emailCount = Integer.parseInt(count);
        if (emailCount < 10) { // Máximo 10 emails por hora
            redisTemplate.opsForValue().increment(key);
            return true;
        }
        
        return false;
    }
}
```

### 💾 **Paso 4: Backup y Recovery**

#### **Script de Backup Automático**
```bash
#!/bin/bash
# backup-db.sh

DATE=$(date +%Y%m%d_%H%M%S)
DB_NAME="Admisión_MTN_DB"
BACKUP_DIR="/opt/backups/mtn"

# Crear directorio si no existe
mkdir -p $BACKUP_DIR

# Backup base de datos
PGPASSWORD=$DB_PASSWORD pg_dump -h $DB_HOST -U $DB_USERNAME $DB_NAME > "$BACKUP_DIR/mtn_db_$DATE.sql"

# Backup archivos subidos
tar -czf "$BACKUP_DIR/mtn_uploads_$DATE.tar.gz" uploads/

# Limpiar backups antiguos (mantener últimos 30 días)
find $BACKUP_DIR -name "mtn_*" -mtime +30 -delete

echo "Backup completado: $DATE"
```

#### **Crontab para Backup Automático**
```bash
# Ejecutar backup cada 6 horas
0 */6 * * * /path/to/backup-db.sh >> /var/log/mtn-backup.log 2>&1

# Backup semanal a cloud storage
0 2 * * 0 rsync -av /opt/backups/mtn/ user@backup-server:/backups/mtn/
```

---

## 🔍 **Verificación de Seguridad**

### ✅ **Checklist Pre-Producción**

#### **Credenciales y Secretos**
- [ ] JWT secret cambiado (mínimo 64 caracteres)
- [ ] Password BD cambiada 
- [ ] Credenciales SMTP configuradas correctamente
- [ ] .env agregado a .gitignore
- [ ] No hay credenciales hardcodeadas en código

#### **Configuración Servidor**
- [ ] HTTPS habilitado con certificado válido
- [ ] Headers de seguridad configurados
- [ ] CORS configurado restrictivamente
- [ ] Rate limiting implementado
- [ ] Logs de seguridad habilitados

#### **Base de Datos**
- [ ] Usuario BD con permisos mínimos necesarios
- [ ] BD no accesible desde internet
- [ ] Backup automático funcionando
- [ ] Encriptación en reposo habilitada (si disponible)

#### **Email y Archivos**
- [ ] Servicio SMTP profesional configurado
- [ ] Límites de archivos apropiados
- [ ] Validación de tipos de archivo
- [ ] Archivos no accesibles directamente vía web

### 🧪 **Tests de Seguridad**

#### **Test JWT**
```bash
# Test JWT secret funciona
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@mtn.cl", "password": "admin123"}'

# Debería retornar JWT válido
```

#### **Test CORS**
```bash
# Test CORS desde origen no permitido
curl -H "Origin: http://malicious-site.com" \
  -H "Access-Control-Request-Method: POST" \
  -X OPTIONS http://localhost:8080/api/auth/login

# Debería bloquear el origen
```

#### **Test Rate Limiting**
```bash
# Enviar múltiples emails rápidamente
for i in {1..15}; do
  curl -X POST http://localhost:8080/api/email/test
done

# Debería limitar después de cierto número
```

---

## 🚨 **Incident Response**

### **En caso de Breach de Seguridad:**

1. **Inmediato (0-1 hora)**
   - [ ] Desconectar sistema de internet
   - [ ] Cambiar todas las credenciales
   - [ ] Revisar logs de acceso
   - [ ] Notificar al equipo técnico

2. **Corto Plazo (1-24 horas)**  
   - [ ] Evaluar datos comprometidos
   - [ ] Notificar a autoridades (si es necesario)
   - [ ] Informar a familias afectadas
   - [ ] Implementar parches de seguridad

3. **Largo Plazo (1+ días)**
   - [ ] Auditoría completa de seguridad
   - [ ] Mejoras arquitecturales
   - [ ] Training adicional del equipo
   - [ ] Documentar lecciones aprendidas

### **Contactos de Emergencia**
```
Administrador Sistema: admin@mtn.cl
Soporte Técnico: soporte@mtn.cl  
Director TI: director.ti@mtn.cl
Legal/Compliance: legal@mtn.cl
```

---

## 📚 **Recursos Adicionales**

### **Herramientas Recomendadas**
- **Monitoring**: New Relic, DataDog, Prometheus
- **Security Scanning**: OWASP ZAP, SonarQube
- **Secrets Management**: HashiCorp Vault, AWS Secrets Manager
- **WAF**: Cloudflare, AWS WAF

### **Normativas Aplicables**
- **Ley de Protección de Datos Chile** (Ley 19.628)
- **GDPR** (si hay estudiantes europeos)
- **Normativas educacionales MINEDUC**

### **Best Practices**
- Principio de menor privilegio
- Defense in depth (múltiples capas de seguridad)
- Monitoring y alerting proactivo
- Regular security assessments
- Incident response plan actualizado

---

## ⚡ **Quick Commands Reference**

```bash
# Generar JWT secret
openssl rand -base64 64

# Generar password hash
htpasswd -bnBC 10 "" "password"

# Ejecutar con .env
set -a && source .env && set +a && mvn spring-boot:run

# Backup BD
pg_dump -U admin -h localhost Admisión_MTN_DB > backup.sql

# Verificar configuración
curl -k https://localhost:8080/actuator/health
```

---

**🔒 RECUERDA: La seguridad es un proceso continuo, no una configuración única. Revisa y actualiza regularmente estas configuraciones.**

*Última actualización: Agosto 2025*