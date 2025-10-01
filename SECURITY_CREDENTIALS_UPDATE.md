# 🔒 ACTUALIZACIÓN CRÍTICA DE SEGURIDAD - CREDENCIALES

## ⚠️ **ACCIÓN INMEDIATA REQUERIDA**

Se han actualizado las credenciales de seguridad del sistema para resolver vulnerabilidades críticas.

---

## 🔑 **NUEVAS CREDENCIALES GENERADAS**

### **Admin Login Actualizado:**
- **Email**: `jorge.gangale@mtn.cl`
- **Nueva contraseña**: `8TK7;R2o>@X6A7[?`
- **Hash BCrypt**: `$2b$12$6fB3ZSXJRWU7fiObW02owOY0gD50GTRdULJU8pYZHYV9yyXD/sbIW`

### **JWT Secret Actualizado:**
- **Anterior**: Expuesto y potencialmente comprometido
- **Nuevo**: `Vvh5hNnPnRX6EmnYgIKZMZ6Rxpu4064dAxxnWDeSESxaUAzPT7Xctp772otbuxC/ZrLJoOEfXynHAYloOkZbGA==`
- **Longitud**: 512 bits (recomendado para producción)

---

## 📋 **PASOS PARA ACTIVAR**

### **1. Actualizar Base de Datos:**
```bash
cd "/Users/jorgegangale/Library/Mobile Documents/com~apple~CloudDocs/Proyectos/Admision_MTN/Admision_MTN_backend"
PGPASSWORD=admin123 /opt/homebrew/Cellar/postgresql@15/15.13/bin/psql -h localhost -U admin -d "Admisión_MTN_DB" -f update_admin_credentials.sql
```

### **2. Reiniciar Backend:**
```bash
# El backend se reiniciará automáticamente al detectar cambios en .env
# Si no se reinicia automáticamente, usar Ctrl+C y volver a ejecutar:
mvn spring-boot:run
```

### **3. Verificar Login:**
```bash
# Probar login con las nuevas credenciales
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "jorge.gangale@mtn.cl", "password": "8TK7;R2o>@X6A7[?"}'
```

---

## 🛡️ **MEJORAS DE SEGURIDAD IMPLEMENTADAS**

### **✅ Credenciales Fortalecidas:**
- JWT secret de 512 bits (era 256 bits)
- Admin password de 16 caracteres con símbolos especiales
- BCrypt hash con strength 12 (era 10)

### **✅ Protección de Secrets:**
- `.env.example` creado como template seguro
- `.env` ya está en `.gitignore` (no se subirá a git)
- Advertencias de seguridad prominentes agregadas

### **✅ Documentación de Seguridad:**
- Instrucciones claras para producción
- Comandos para generar secrets seguros
- Lista de verificación de seguridad

---

## 🚨 **PARA PRODUCCIÓN**

### **Cambios Obligatorios antes de Deployment:**

1. **Base de Datos:**
   ```bash
   DB_PASSWORD=$(openssl rand -base64 32)
   # Actualizar PostgreSQL con la nueva contraseña
   ```

2. **JWT Secret:**
   ```bash
   JWT_SECRET=$(openssl rand -base64 64)
   # Ya configurado con valor seguro
   ```

3. **Email SMTP:**
   ```bash
   SMTP_PASSWORD=your_institutional_app_password_here
   # Configurar con credenciales reales de Gmail/Office365
   ```

4. **Environment:**
   ```bash
   SPRING_PROFILES_ACTIVE=production
   # Cambiar de development a production
   ```

---

## 🔍 **VERIFICACIÓN DE SEGURIDAD**

### **Antes de esta actualización:**
- ❌ Admin password: `admin123` (débil)
- ❌ JWT secret expuesto en archivo de configuración
- ❌ Credenciales hardcodeadas y visibles

### **Después de esta actualización:**
- ✅ Admin password: `8TK7;R2o>@X6A7[?` (fuerte)
- ✅ JWT secret: 512 bits criptográficamente seguro
- ✅ Credenciales protegidas y documentadas

---

## ⚠️ **IMPORTANTE**

1. **Guarda la nueva contraseña** en un lugar seguro (gestor de contraseñas)
2. **NO SUBAS el archivo .env** a git (ya protegido por .gitignore)
3. **Cambia SMTP_PASSWORD** con credenciales institucionales reales
4. **Ejecuta update_admin_credentials.sql** para activar las nuevas credenciales
5. **Considera cambiar la contraseña admin** después del primer login por seguridad adicional

---

## 📞 **Si tienes problemas:**

1. **No puedo hacer login**: Verifica que ejecutaste `update_admin_credentials.sql`
2. **Backend no arranca**: Revisa que los valores en `.env` sean correctos
3. **JWT errors**: El nuevo secret es compatible, debería funcionar automáticamente

**La nueva contraseña admin es: `8TK7;R2o>@X6A7[?`**