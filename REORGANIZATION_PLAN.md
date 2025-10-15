# Plan de Reorganización - Estructura Modular

## 🎯 Objetivo
Reorganizar el proyecto con estructura de carpetas profesional tipo microservicios, manteniendo 100% funcionalidad.

## 📁 Estructura Propuesta

```
Admision_MTN_backend/
├── services/                          # Todos los microservicios
│   ├── user-service/
│   │   ├── src/
│   │   │   └── mock-user-service.js
│   │   ├── Dockerfile
│   │   ├── railway.toml
│   │   └── package.json
│   ├── application-service/
│   │   ├── src/
│   │   │   └── mock-application-service.js
│   │   ├── Dockerfile
│   │   ├── railway.toml
│   │   └── package.json
│   ├── evaluation-service/
│   │   ├── src/
│   │   │   └── mock-evaluation-service.js
│   │   ├── Dockerfile
│   │   ├── railway.toml
│   │   └── package.json
│   ├── notification-service/
│   │   ├── src/
│   │   │   └── mock-notification-service.js
│   │   ├── Dockerfile
│   │   ├── railway.toml
│   │   └── package.json
│   ├── dashboard-service/
│   │   ├── src/
│   │   │   └── mock-dashboard-service.js
│   │   ├── Dockerfile
│   │   ├── railway.toml
│   │   └── package.json
│   └── guardian-service/
│       ├── src/
│       │   └── mock-guardian-service.js
│       ├── Dockerfile
│       ├── railway.toml
│       └── package.json
│
├── api-gateway/                       # Configuración NGINX
│   ├── conf/
│   │   ├── local-gateway.conf
│   │   ├── gateway-microservices.conf
│   │   └── nginx-proxy-headers.conf
│   └── scripts/
│       └── start-gateway.sh
│
├── database/                          # Scripts de base de datos
│   ├── schema/
│   │   └── railway-db-setup.sql
│   ├── migrations/
│   └── seeds/
│
├── shared/                            # Código compartido
│   ├── config/
│   │   ├── db-config.js
│   │   └── jwt-config.js
│   ├── utils/
│   │   ├── logger.js
│   │   └── validators.js
│   └── middleware/
│       └── auth-middleware.js
│
├── scripts/                           # Scripts de utilidad
│   ├── deployment/
│   │   ├── railway-restore-db.sh
│   │   ├── railway-smoke-tests.sh
│   │   └── start-microservices-gateway.sh
│   ├── backup/
│   │   └── full-backup.sh
│   └── testing/
│       ├── qa-tests.sh
│       └── integration-tests.sh
│
├── docs/                              # Documentación
│   ├── architecture/
│   │   ├── ARCHITECTURE.md
│   │   └── ADRs/
│   ├── api/
│   │   └── API_DOCUMENTATION.md
│   └── deployment/
│       ├── RAILWAY_DEPLOYMENT.md
│       └── TROUBLESHOOTING.md
│
├── uploads/                           # Archivos de usuarios
│
├── node_modules/                      # Dependencias (raíz)
│
├── package.json                       # Dependencies principales
├── package-lock.json
├── railway.toml                       # Railway config principal
├── .gitignore
├── CLAUDE.md
├── README.md
└── LIMPIEZA_RESUMEN.md
```

## ✅ Ventajas de esta Estructura

1. **Modularidad:** Cada servicio es independiente
2. **Escalabilidad:** Fácil agregar nuevos servicios
3. **Mantenibilidad:** Código organizado por dominio
4. **Claridad:** Estructura profesional tipo enterprise
5. **Railway-ready:** Cada servicio puede tener su railway.toml
6. **Testing:** Fácil ejecutar tests por servicio

## 🔧 Cambios Necesarios

### 1. Mover archivos de servicios
```bash
mkdir -p services/{user,application,evaluation,notification,dashboard,guardian}-service/src
mv mock-user-service.js services/user-service/src/
mv mock-application-service.js services/application-service/src/
# ... etc
```

### 2. Actualizar imports en cada servicio
```javascript
// Antes:
const dbPool = require('./db-config');

// Después:
const dbPool = require('../../shared/config/db-config');
```

### 3. Actualizar package.json scripts
```json
{
  "scripts": {
    "start:user": "node services/user-service/src/mock-user-service.js",
    "start:application": "node services/application-service/src/mock-application-service.js",
    "start:all": "npm run start:user & npm run start:application & ..."
  }
}
```

### 4. Actualizar scripts de inicio
```bash
# start-microservices-gateway.sh
node services/user-service/src/mock-user-service.js &
node services/application-service/src/mock-application-service.js &
# ...
```

## 🚀 Plan de Ejecución

### Fase 1: Crear estructura de carpetas
- Crear directorios services/, api-gateway/, shared/, etc.
- NO mover archivos aún

### Fase 2: Mover servicios uno por uno
- Mover mock-user-service.js → services/user-service/src/
- Actualizar imports
- Probar que funciona
- Repetir para cada servicio

### Fase 3: Mover configuraciones
- NGINX → api-gateway/conf/
- DB scripts → database/schema/
- Scripts → scripts/deployment/

### Fase 4: Actualizar scripts de inicio
- start-microservices-gateway.sh
- railway.toml
- package.json

### Fase 5: Testing completo
- Ejecutar ./start-microservices-gateway.sh
- Verificar todos los endpoints
- Confirmar 100% funcionalidad

## ⚠️ Precauciones

1. **No mover node_modules/** - Déjalo en raíz
2. **No mover uploads/** - Datos de usuarios
3. **Actualizar todos los paths en imports**
4. **Probar después de cada movimiento**
5. **Commit después de cada fase exitosa**

## 📊 Impacto Esperado

- **Funcionalidad:** 100% mantenida ✅
- **Organización:** +200% mejora
- **Profesionalismo:** Estructura enterprise-grade
- **Escalabilidad:** Preparado para crecimiento

---

**¿Proceder con la reorganización?**
