# 👥 USUARIOS DE PRUEBA - Sistema de Admisión MTN

Este documento contiene las credenciales de todos los usuarios de prueba disponibles en el sistema para testing.

---

## 🔐 FORMATO DE ACCESO

**URL Base Frontend:** http://localhost:5173
**URL Base Backend:** http://localhost:8080

### Portales de Login:
- **Admin/Coordinador:** http://localhost:5173/login
- **Profesores/Entrevistadores:** http://localhost:5173/profesor/login
- **Apoderados (Familias):** http://localhost:5173/apoderado/login

---

## 👑 ADMINISTRADOR

### Usuario: Jorge Gangale (ID: 1)
- **Email:** jorge.gangale@mtn.cl
- **Contraseña:** admin123
- **Rol:** ADMIN
- **Permisos:** Acceso completo al sistema
- **Portal:** http://localhost:5173/login

**Funcionalidades:**
- ✅ Dashboard administrativo
- ✅ Gestión de postulaciones
- ✅ Gestión de usuarios
- ✅ Gestión de entrevistas
- ✅ Gestión de evaluaciones
- ✅ Reportes y estadísticas
- ✅ Configuración del sistema

---

## 🎓 COORDINADOR

### Usuario: Carlos Morales (ID: 3)
- **Email:** carlos.morales@mtn.cl
- **Contraseña:** coord123
- **Rol:** COORDINATOR
- **Permisos:** Gestión académica y coordinación
- **Portal:** http://localhost:5173/login (mismo que admin)

**Funcionalidades:**
- ✅ Dashboard de coordinación
- ✅ Análisis de postulaciones
- ✅ Tendencias temporales
- ✅ Búsqueda avanzada
- ✅ Reportes académicos

---

## 👨‍🏫 PROFESORES (Por Asignatura)

### 1. Profesora de Matemáticas: Alejandra Flores (ID: 47)
- **Email:** alejandra.flores@mtn.cl
- **Contraseña:** teacher123
- **Rol:** TEACHER
- **Asignatura:** MATHEMATICS
- **Portal:** http://localhost:5173/profesor/login

**Funcionalidades:**
- ✅ Dashboard de profesor
- ✅ Evaluaciones de matemáticas asignadas
- ✅ Formularios de examen (puntajes 0-100)
- ✅ Observaciones cualitativas
- ✅ Recomendaciones de admisión
- ✅ Historial de estudiantes
- ✅ Gestión de horarios de disponibilidad

---

### 2. Profesora de Lenguaje: Patricia Silva (ID: 30)
- **Email:** patricia.silva@mtn.cl
- **Contraseña:** teacher123
- **Rol:** TEACHER
- **Asignatura:** SPANISH (Lenguaje y Comunicación)
- **Portal:** http://localhost:5173/profesor/login

**Funcionalidades:**
- ✅ Evaluaciones de lenguaje
- ✅ Exámenes de comprensión lectora
- ✅ Evaluación de redacción
- ✅ Observaciones pedagógicas
- ✅ Gestión de horarios

---

### 3. Profesor de Inglés: Jorge Gangale Alt (ID: 12)
- **Email:** jorge.gangale@mtn.com
- **Contraseña:** teacher123
- **Rol:** TEACHER
- **Asignatura:** ENGLISH
- **Portal:** http://localhost:5173/profesor/login
- **Nota:** Usuario alternativo para probar evaluaciones de inglés

**Funcionalidades:**
- ✅ Evaluaciones de inglés
- ✅ Exámenes de idioma
- ✅ Nivel de competencia lingüística

---

## 🧠 PSICÓLOGO

### Usuario: Diego Fuentes (ID: 33)
- **Email:** diego.fuentes@mtn.cl
- **Contraseña:** psych123
- **Rol:** PSYCHOLOGIST
- **Portal:** http://localhost:5173/profesor/login

**Funcionalidades:**
- ✅ Dashboard de entrevistador
- ✅ Entrevistas psicológicas asignadas
- ✅ **NUEVO:** Entrevistas familiares
- ✅ Evaluación de madurez emocional
- ✅ Evaluación de habilidades sociales
- ✅ Evaluación de comportamiento
- ✅ Recomendaciones psicológicas
- ✅ Gestión de horarios de atención

**Tipos de Entrevista que Realiza:**
- 🔹 PSYCHOLOGICAL_INTERVIEW (Entrevista Psicológica Individual)
- 🔹 **FAMILY_INTERVIEW (Entrevista Familiar - NUEVO)**

---

## 📚 DIRECTOR DE CICLO

### Usuario: Por Asignar
- **Nota:** Los directores de ciclo son profesores con rol CYCLE_DIRECTOR o TEACHER con permisos especiales
- **Email:** (usar cualquier profesor con CYCLE_DIRECTOR_INTERVIEW asignado)
- **Portal:** http://localhost:5173/profesor/login

**Funcionalidades:**
- ✅ Entrevistas con director de ciclo
- ✅ **NUEVO:** Entrevistas familiares
- ✅ Informes de director de ciclo
- ✅ Evaluación integral del estudiante
- ✅ Gestión de horarios

**Tipos de Evaluación que Realiza:**
- 🔹 CYCLE_DIRECTOR_INTERVIEW (Entrevista Director/a de Ciclo)
- 🔹 CYCLE_DIRECTOR_REPORT (Informe Director de Ciclo)
- 🔹 **FAMILY_INTERVIEW (Entrevista Familiar - NUEVO)**

---

## 👪 APODERADOS (Familias)

### 1. Familia Torres (ID: 20)
- **Email:** isabella.torres@email.com
- **Contraseña:** apoderado123
- **Rol:** APODERADO
- **RUT:** 18.765.432-1
- **Portal:** http://localhost:5173/apoderado/login

**Estudiante Asociado:**
- Nombre: Mateo Torres Soto (ID: 34)
- Curso Postulado: 5° Básico
- Estado Postulación: PENDING

**Funcionalidades:**
- ✅ Dashboard familiar
- ✅ Ver estado de postulación
- ✅ Calendario de entrevistas
- ✅ Confirmación de citas
- ✅ Documentos de postulación
- ✅ Notificaciones por email

---

### 2. Familia Pérez (Usuarios Existentes en DB)
- **Buscar en base de datos con:**
```sql
SELECT id, email, first_name, last_name, rut
FROM users
WHERE role = 'APODERADO'
ORDER BY id;
```

---

## 🔧 TESTING POR ROL

### Test Flujo Administrador:
1. Login: jorge.gangale@mtn.cl / admin123
2. Ir a Dashboard Admin
3. Gestionar postulaciones
4. Asignar evaluaciones
5. Programar entrevistas
6. Ver reportes

### Test Flujo Profesor de Asignatura:
1. Login: alejandra.flores@mtn.cl / teacher123
2. Ver evaluaciones asignadas
3. Completar examen de matemáticas
4. Agregar puntaje (0-100)
5. Agregar observaciones cualitativas
6. Guardar evaluación

### Test Flujo Entrevistador (Psicólogo/Director):
1. Login: diego.fuentes@mtn.cl / psych123
2. Ver entrevistas asignadas
3. Gestionar horarios de disponibilidad
4. **NUEVO:** Completar entrevista familiar
5. Completar entrevista psicológica
6. Agregar recomendaciones

### Test Flujo Apoderado:
1. Login: isabella.torres@email.com / apoderado123
2. Ver estado de postulación
3. Confirmar citas de entrevistas
4. Descargar documentos
5. Ver calendario

---

## 📊 TIPOS DE EVALUACIONES EN EL SISTEMA

### Exámenes Académicos (Profesores):
1. **LANGUAGE_EXAM** - Examen de Lenguaje
2. **MATHEMATICS_EXAM** - Examen de Matemáticas
3. **ENGLISH_EXAM** - Examen de Inglés

### Entrevistas y Reportes (Entrevistadores):
4. **PSYCHOLOGICAL_INTERVIEW** - Entrevista Psicológica
5. **CYCLE_DIRECTOR_INTERVIEW** - Entrevista Director de Ciclo
6. **CYCLE_DIRECTOR_REPORT** - Informe Director de Ciclo
7. **FAMILY_INTERVIEW** - Entrevista Familiar ⭐ **NUEVO**

---

## 🆕 ENTREVISTA FAMILIAR - NUEVA FUNCIONALIDAD

### ¿Quién puede realizarla?
- ✅ Psicólogos (PSYCHOLOGIST)
- ✅ Directores de Ciclo (CYCLE_DIRECTOR)
- ✅ Cualquier entrevistador autorizado (NO profesores de asignatura)

### Estructura del Formulario:
**Total: 100 puntos**

1. **Sección I: Familia y Educación (26 pts = 90%)**
   - Motivación para el ingreso (1-3 pts)
   - Valores familiares (1-3 pts)
   - Hábitos y límites (1-3 pts)
   - Fortalezas del niño (1-3 pts)
   - Manejo de frustración (1-2 pts)
   - Espiritualidad (1-3 pts)
   - Responsabilidad social (1-3 pts)

2. **Sección II: Observaciones (6 pts = 5%)**
   - Pertenencia a Schoenstatt (Sí/No)
   - Respeto entre la pareja (Sí/No)
   - Sencillez y honestidad (Sí/No)
   - Deseo de pertenencia (Sí/No)

3. **Sección III: Opinión Final (5 pts = 5%)**
   - Puntaje de recomendación (1-5 pts)

4. **Justificación**
   - Campo de texto (máx 500 caracteres)

### Campos del Formulario:
- Nombres de entrevistadores
- Nombre de la familia
- Estudiantes postulantes
- Colegio actual
- Nombres de padre y madre
- Link al cuestionario
- Scores detallados por sección
- Cálculo automático de totales y porcentajes

---

## 🧪 CÓMO PROBAR LA ENTREVISTA FAMILIAR

### Paso 1: Login como Administrador
```
Email: jorge.gangale@mtn.cl
Password: admin123
URL: http://localhost:5173/login
```

### Paso 2: Asignar Entrevista Familiar
1. Ir a "Gestión de Evaluaciones"
2. Seleccionar una postulación
3. Asignar evaluación tipo: **FAMILY_INTERVIEW**
4. Asignar evaluador: Diego Fuentes (Psicólogo)
5. Guardar

### Paso 3: Login como Entrevistador
```
Email: diego.fuentes@mtn.cl
Password: psych123
URL: http://localhost:5173/profesor/login
```

### Paso 4: Completar Entrevista Familiar
1. En el dashboard, ver "Evaluaciones Pendientes"
2. Encontrar la evaluación tipo "Entrevista Familiar"
3. Click en "Crear Entrevista"
4. Completar todas las secciones del formulario
5. Los puntajes se calculan automáticamente
6. Agregar justificación
7. Click en "Completar Evaluación"

### Paso 5: Verificar Guardado
1. La evaluación cambia a estado "COMPLETED"
2. El puntaje total se guarda en el campo `score`
3. Los datos del formulario se guardan como JSON en `observations`
4. Puedes hacer click en "Ver Entrevista" para ver los datos

---

## 🔍 VERIFICACIÓN EN BASE DE DATOS

### Ver Usuarios:
```sql
SELECT id, email, role, first_name, last_name, subject
FROM users
WHERE role IN ('ADMIN', 'TEACHER', 'PSYCHOLOGIST', 'COORDINATOR', 'APODERADO')
ORDER BY role, id;
```

### Ver Evaluaciones Asignadas:
```sql
SELECT
  e.id,
  e.evaluation_type,
  e.status,
  e.score,
  u.first_name || ' ' || u.last_name as evaluator,
  s.first_name || ' ' || s.paternal_last_name as student
FROM evaluations e
JOIN users u ON e.evaluator_id = u.id
JOIN applications a ON e.application_id = a.id
JOIN students s ON a.student_id = s.id
ORDER BY e.created_at DESC
LIMIT 10;
```

### Ver Entrevistas Familiares:
```sql
SELECT
  e.id,
  e.evaluation_type,
  e.status,
  e.score,
  e.observations,
  u.email as evaluator_email,
  s.first_name || ' ' || s.paternal_last_name as student_name
FROM evaluations e
JOIN users u ON e.evaluator_id = u.id
JOIN applications a ON e.application_id = a.id
JOIN students s ON a.student_id = s.id
WHERE e.evaluation_type = 'FAMILY_INTERVIEW';
```

---

## 📝 NOTAS IMPORTANTES

### Seguridad:
- ⚠️ **ESTAS SON CREDENCIALES DE PRUEBA** - NO USAR EN PRODUCCIÓN
- Todas las contraseñas están hasheadas con BCrypt en la base de datos
- Los tokens JWT expiran después de 24 horas

### Contraseñas por Defecto:
- Administradores: `admin123`
- Coordinadores: `coord123`
- Profesores: `teacher123`
- Psicólogos: `psych123`
- Apoderados: `apoderado123`

### Obtener Token JWT Manualmente:
```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"jorge.gangale@mtn.cl","password":"admin123"}'

# Response incluirá el token JWT
```

### Verificar Servicios Activos:
```bash
# Gateway
curl http://localhost:8080/gateway/status

# Servicios individuales
curl http://localhost:8082/health  # User service
curl http://localhost:8083/health  # Application service
curl http://localhost:8084/health  # Evaluation service
curl http://localhost:8085/health  # Notification service
curl http://localhost:8086/health  # Dashboard service
curl http://localhost:8087/health  # Guardian service
```

---

## 🎯 CASOS DE USO PARA TESTING

### Caso 1: Flujo Completo de Admisión
1. Apoderado crea postulación
2. Admin asigna evaluaciones
3. Profesores completan exámenes
4. Entrevistador programa entrevista familiar
5. Entrevistador completa entrevista familiar
6. Admin revisa todas las evaluaciones
7. Admin toma decisión de admisión

### Caso 2: Gestión de Horarios
1. Login como entrevistador
2. Ir a "Mis Horarios"
3. Agregar horarios de disponibilidad
4. Marcar horarios como activos/inactivos
5. Admin programa entrevistas en horarios disponibles

### Caso 3: Reportes y Estadísticas
1. Login como admin o coordinador
2. Ver dashboard con métricas
3. Filtrar por año académico
4. Ver tendencias temporales
5. Exportar reportes

---

## 🚀 QUICK START

### 1. Iniciar Backend:
```bash
cd Admision_MTN_backend
./start-microservices-gateway.sh
```

### 2. Iniciar Frontend:
```bash
cd Admision_MTN_front
npm run dev
```

### 3. Acceder al Sistema:
- **Admin:** http://localhost:5173/login
  - jorge.gangale@mtn.cl / admin123

- **Profesor:** http://localhost:5173/profesor/login
  - alejandra.flores@mtn.cl / teacher123

- **Psicólogo:** http://localhost:5173/profesor/login
  - diego.fuentes@mtn.cl / psych123

- **Apoderado:** http://localhost:5173/apoderado/login
  - isabella.torres@email.com / apoderado123

---

## 📞 SOPORTE

Si encuentras problemas:
1. Verificar que todos los servicios estén corriendo
2. Revisar logs del backend en `/tmp/*-service.log`
3. Limpiar caché del frontend: `rm -rf node_modules/.vite dist .vite`
4. Verificar conexión a base de datos PostgreSQL

**Base de Datos:**
```
Host: localhost
Port: 5432
Database: Admisión_MTN_DB
User: admin
Password: admin123
```

---

**Última Actualización:** Octubre 2025
**Versión del Sistema:** 2.0.0 (con Entrevistas Familiares)
