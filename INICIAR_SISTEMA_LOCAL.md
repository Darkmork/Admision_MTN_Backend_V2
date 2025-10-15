# Cómo Iniciar el Sistema Local

## ✅ Estado Actual (Completado Automáticamente)

Todos los servicios Node.js ya están corriendo:

- ✅ User Service (puerto 8082): UP
- ✅ Application Service (puerto 8083): UP
- ✅ Evaluation Service (puerto 8084): UP
- ✅ Notification Service (puerto 8085): UP
- ✅ Dashboard Service (puerto 8086): UP
- ✅ Guardian Service (puerto 8087): UP

## 🚀 Próximo Paso: Iniciar NGINX Gateway

Para que el frontend pueda acceder al sistema, necesitas iniciar NGINX manualmente (requiere sudo):

```bash
cd "/Users/jorgegangale/Library/Mobile Documents/com~apple~CloudDocs/Proyectos/Admision_MTN/Admision_MTN_backend"

sudo nginx -c "$(pwd)/api-gateway/conf/local-gateway.conf"
```

## 🔍 Verificar que NGINX está Funcionando

```bash
curl http://localhost:8080/health
```

Deberías ver una respuesta como:
```json
{
  "status": "UP",
  "gateway": "NGINX API Gateway",
  "services": [...]
}
```

## ✅ Sistema Completamente Iniciado

Una vez NGINX esté corriendo, el sistema estará 100% funcional:

- 🌐 **API Gateway (NGINX)**: http://localhost:8080
- 📊 **User Service**: http://localhost:8082
- 📄 **Application Service**: http://localhost:8083
- 📝 **Evaluation Service**: http://localhost:8084
- 📧 **Notification Service**: http://localhost:8085
- 📈 **Dashboard Service**: http://localhost:8086
- 👨‍👩‍👧 **Guardian Service**: http://localhost:8087

## 📝 Logs de Servicios

Los logs están disponibles en:
```bash
tail -f /tmp/user-service.log
tail -f /tmp/application-service.log
tail -f /tmp/evaluation-service.log
tail -f /tmp/notification-service.log
tail -f /tmp/dashboard-service.log
tail -f /tmp/guardian-service.log
```

## 🛑 Detener Todo el Sistema

```bash
# Detener servicios Node.js
lsof -ti:8082,8083,8084,8085,8086,8087 | xargs kill -9

# Detener NGINX
sudo nginx -s stop
```

## 🔄 Reiniciar Todo el Sistema

Si necesitas reiniciar todo de cero:

```bash
# 1. Detener todo
lsof -ti:8080,8082,8083,8084,8085,8086,8087 | xargs kill -9
sudo nginx -s stop 2>/dev/null

# 2. Usar el script automático
./scripts/deployment/start-microservices-gateway.sh
```

**NOTA:** El script automático requiere que ejecutes `sudo` manualmente cuando te lo pida para iniciar NGINX.

## ⚡ Inicio Rápido (Sin NGINX automático)

Si prefieres iniciar servicios sin el script:

```bash
cd "/Users/jorgegangale/Library/Mobile Documents/com~apple~CloudDocs/Proyectos/Admision_MTN/Admision_MTN_backend"

# Iniciar servicios Node.js
node services/user-service/src/mock-user-service.js > /tmp/user-service.log 2>&1 &
node services/application-service/src/mock-application-service.js > /tmp/application-service.log 2>&1 &
node services/evaluation-service/src/mock-evaluation-service.js > /tmp/evaluation-service.log 2>&1 &
node services/notification-service/src/mock-notification-service.js > /tmp/notification-service.log 2>&1 &
node services/dashboard-service/src/mock-dashboard-service.js > /tmp/dashboard-service.log 2>&1 &
node services/guardian-service/src/mock-guardian-service.js > /tmp/guardian-service.log 2>&1 &

# Esperar 3 segundos
sleep 3

# Iniciar NGINX
sudo nginx -c "$(pwd)/api-gateway/conf/local-gateway.conf"
```

## 🎯 Frontend

Una vez el backend esté corriendo, puedes iniciar el frontend:

```bash
cd "/Users/jorgegangale/Library/Mobile Documents/com~apple~CloudDocs/Proyectos/Admision_MTN/Admision_MTN_front"
npm run dev
```

El frontend estará disponible en: http://localhost:5173
