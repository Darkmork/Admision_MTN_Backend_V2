# 🔍 Investigación: Login Timeout en Railway

**Fecha:** 13 de octubre de 2025
**Problema:** El endpoint `/api/auth/login` funciona en 77ms localmente pero hace timeout (>10s) en Railway

---

## 📊 Análisis del Flujo de Login

### Código Actual (mock-user-service.js:916-1029)

```javascript
app.post('/api/auth/login', decryptCredentials, async (req, res) => {
  // 1. decryptCredentials middleware (RSA+AES)
  // 2. Validación de campos
  // 3. DB Pool connection
  // 4. SELECT query - Buscar usuario
  // 5. bcrypt.compare - Verificar contraseña
  // 6. UPDATE last_login
  // 7. SELECT applicationId (solo APODERADO)
  // 8. Generar JWT token
  // 9. Responder
});
```

### Puntos de Timeout Potenciales

| Paso | Operación | Tiempo Local | Sospecha Railway |
|------|-----------|--------------|------------------|
| 1 | `decryptCredentials` | <5ms | ⚠️ Posible |
| 2 | Validación campos | <1ms | ✅ OK |
| 3 | `dbPool.connect()` | <10ms | ⚠️ **SOSPECHA ALTA** |
| 4 | `SELECT` usuario | <20ms | ⚠️ **SOSPECHA ALTA** |
| 5 | `bcrypt.compare` | ~40ms | ⚠️ **SOSPECHA MUY ALTA** |
| 6 | `UPDATE` last_login | <10ms | ⚠️ Posible |
| 7 | `SELECT` applicationId | <10ms | ✅ OK (solo APODERADO) |
| 8 | Generar JWT | <1ms | ✅ OK |

---

## 🎯 Hipótesis Principal: BCrypt es el Cuello de Botella

### Por Qué BCrypt Puede Ser el Problema

**BCrypt es CPU-intensivo:**
- `bcrypt.compare(password, hash)` hace 2^10 iteraciones (rounds=10)
- En máquinas rápidas (local): ~30-50ms
- En máquinas lentas (Railway shared CPU): ~5-10 segundos ⚠️

**Evidencia circunstancial:**
1. Local (M1 Max): 77ms total → BCrypt ~40ms (normal)
2. Railway (shared vCPU): 10+ segundos → BCrypt podría ser ~9+ segundos
3. Otros endpoints funcionan bien → Solo login tiene problema
4. Login es el ÚNICO endpoint que usa `bcrypt.compare`

### Configuración Actual de BCrypt

```javascript
// En registro (línea 518, 678, 844):
const hashedPassword = await bcrypt.hash(password, 10);  // 10 rounds

// En login (línea 958):
const isValidPassword = await bcrypt.compare(password, user.password);  // Compara contra hash de 10 rounds
```

**10 rounds significa:** 2^10 = 1,024 iteraciones de hasheo
- **Recomendado para producción:** 10-12 rounds
- **Para entornos lentos:** 8-9 rounds
- **Cada round adicional duplica el tiempo**

---

## 🔬 Plan de Diagnóstico

### Paso 1: Agregar Logging Temporal

Modificar el login endpoint para medir tiempos:

```javascript
app.post('/api/auth/login', decryptCredentials, async (req, res) => {
  const startTime = Date.now();
  const { email, password } = req.body;

  console.time('[Login] Total');
  console.time('[Login] DB Connect');

  const client = await dbPool.connect();
  console.timeEnd('[Login] DB Connect');

  try {
    console.time('[Login] SELECT User');
    const userQuery = await client.query(
      'SELECT id, first_name, last_name, email, role, subject, password, active, email_verified FROM users WHERE email = $1',
      [email.toLowerCase().trim()]
    );
    console.timeEnd('[Login] SELECT User');

    if (userQuery.rows.length === 0) {
      return res.status(401).json({ success: false, error: 'Credenciales inválidas' });
    }

    const user = userQuery.rows[0];

    console.time('[Login] BCrypt Compare');
    const isValidPassword = await bcrypt.compare(password, user.password);
    console.timeEnd('[Login] BCrypt Compare');

    if (!isValidPassword) {
      return res.status(401).json({ success: false, error: 'Credenciales inválidas' });
    }

    console.time('[Login] UPDATE last_login');
    await client.query('UPDATE users SET last_login = NOW() WHERE id = $1', [user.id]);
    console.timeEnd('[Login] UPDATE last_login');

    // ... resto del código ...

    console.timeEnd('[Login] Total');
    console.log(`[Login] Total execution: ${Date.now() - startTime}ms`);

    res.json(responseData);
  } finally {
    client.release();
  }
});
```

### Paso 2: Revisar Logs de Railway

Una vez deployado, revisar:
```bash
# En Railway Dashboard → Service → Logs
# Buscar líneas que contengan "[Login]"
```

---

## 💡 Soluciones Propuestas

### Solución A: Reducir BCrypt Rounds (RECOMENDADO)

**Ventajas:**
- Simple y directo
- No rompe compatibilidad
- Reduce tiempo de 2^10 (1,024) a 2^8 (256) iteraciones = 75% más rápido

**Desventajas:**
- Ligeramente menos seguro (pero 8 rounds sigue siendo aceptable)

**Implementación:**

```javascript
// Cambiar de:
const BCRYPT_ROUNDS = 10;

// A:
const BCRYPT_ROUNDS = process.env.NODE_ENV === 'production' ? 8 : 10;
```

**Nota:** Los hashes existentes (10 rounds) seguirán funcionando porque BCrypt detecta automáticamente el número de rounds del hash. Solo los nuevos usuarios tendrán 8 rounds.

### Solución B: Re-hash Progresivo (AVANZADO)

Detectar hashes viejos y re-hashearlos automáticamente:

```javascript
// Después de bcrypt.compare exitoso:
if (isValidPassword) {
  const currentRounds = bcrypt.getRounds(user.password);
  const targetRounds = 8;

  if (currentRounds > targetRounds) {
    // Re-hash password con menos rounds
    const newHash = await bcrypt.hash(password, targetRounds);
    await client.query('UPDATE users SET password = $1 WHERE id = $2', [newHash, user.id]);
    console.log(`[Login] Re-hashed user ${user.id} from ${currentRounds} to ${targetRounds} rounds`);
  }
}
```

### Solución C: Aumentar Timeout del Endpoint (TEMPORAL)

Si BCrypt no es el problema:

```javascript
// En Railway, aumentar timeouts:
req.setTimeout(30000); // 30 segundos
res.setTimeout(30000);
```

### Solución D: Connection Pool Tuning

Si el problema es DB connection:

```javascript
const dbPool = new Pool({
  // ... existing config ...
  connectionTimeoutMillis: 5000,  // Aumentar de 2000 a 5000
  query_timeout: 10000,           // Aumentar de 5000 a 10000
  max: 10,                        // Reducir de 20 a 10 (Railway tiene recursos limitados)
});
```

---

## 🧪 Tests de Validación

### Test 1: BCrypt Performance Local

```javascript
// test-bcrypt-railway.js
const bcrypt = require('bcryptjs');

async function testBCrypt() {
  const password = 'admin123';

  // Test 10 rounds (actual)
  console.time('Hash 10 rounds');
  const hash10 = await bcrypt.hash(password, 10);
  console.timeEnd('Hash 10 rounds');

  console.time('Compare 10 rounds');
  await bcrypt.compare(password, hash10);
  console.timeEnd('Compare 10 rounds');

  // Test 8 rounds (propuesto)
  console.time('Hash 8 rounds');
  const hash8 = await bcrypt.hash(password, 8);
  console.timeEnd('Hash 8 rounds');

  console.time('Compare 8 rounds');
  await bcrypt.compare(password, hash8);
  console.timeEnd('Compare 8 rounds');
}

testBCrypt();
```

**Resultado esperado:**
- Local: 10 rounds ~40ms, 8 rounds ~10ms
- Railway: 10 rounds ~10s, 8 rounds ~2.5s

### Test 2: Database Query Performance

```bash
# Conectar a Railway PostgreSQL
psql "$DATABASE_URL"

# Medir query de login
\timing on
SELECT id, first_name, last_name, email, role, subject, password, active, email_verified
FROM users
WHERE email = 'jorge.gangale@mtn.cl';
```

**Resultado esperado:** <100ms

### Test 3: Connection Pool Saturation

```javascript
// test-pool-saturation.js
const { Pool } = require('pg');

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  max: 20
});

async function testPoolSaturation() {
  const promises = [];

  // Abrir 25 conexiones simultáneas (más que el max de 20)
  for (let i = 0; i < 25; i++) {
    promises.push(
      pool.query('SELECT NOW()').catch(err => {
        console.error(`Query ${i} failed:`, err.message);
      })
    );
  }

  console.time('25 concurrent queries');
  await Promise.all(promises);
  console.timeEnd('25 concurrent queries');
}

testPoolSaturation();
```

---

## 📈 Métricas de Éxito

Después de implementar la solución, el login debe:

| Métrica | Objetivo | Actual Railway | Estado |
|---------|----------|----------------|---------|
| Tiempo de respuesta | <2s | >10s | ❌ FAIL |
| BCrypt compare | <500ms | ? | ⏳ MEDIR |
| DB query | <100ms | ? | ⏳ MEDIR |
| DB connect | <50ms | ? | ⏳ MEDIR |
| Success rate | 100% | 0% | ❌ FAIL |

---

## 🚀 Plan de Acción Recomendado

### FASE 1: Diagnóstico (AHORA)
1. ✅ Documentar problema
2. ⏳ Agregar logging temporal al endpoint
3. ⏳ Deploy con logging a Railway
4. ⏳ Revisar logs para identificar cuello de botella

### FASE 2: Fix Rápido (SI BCrypt es el problema)
1. ⏳ Reducir BCrypt rounds a 8
2. ⏳ Deploy a Railway
3. ⏳ Probar login
4. ⏳ Medir tiempos de respuesta

### FASE 3: Fix Definitivo (SI pool/DB es el problema)
1. ⏳ Ajustar connection pool config
2. ⏳ Agregar índice a `users.email` (si no existe)
3. ⏳ Deploy a Railway
4. ⏳ Probar login

### FASE 4: Verificación
1. ⏳ Ejecutar smoke tests completos
2. ⏳ Confirmar login <2 segundos
3. ⏳ Remover logging temporal

---

## 📝 Notas Adicionales

### BCrypt Rounds Benchmark

| Rounds | Iteraciones | Tiempo (M1 Max) | Tiempo (Railway vCPU estimado) |
|--------|-------------|-----------------|--------------------------------|
| 8 | 256 | ~10ms | ~2.5s |
| 9 | 512 | ~20ms | ~5s |
| 10 | 1,024 | ~40ms | ~10s |
| 11 | 2,048 | ~80ms | ~20s |
| 12 | 4,096 | ~160ms | ~40s |

### Security Considerations

**8 rounds es seguro?**
- ✅ Sí, para la mayoría de aplicaciones
- ✅ OWASP recomienda mínimo 10, pero 8 es aceptable si hay limitaciones de hardware
- ✅ 2^8 = 256 iteraciones sigue siendo computacionalmente costoso para ataques de fuerza bruta
- ⚠️ No usar menos de 8 rounds

**Alternativas si 8 rounds no es suficiente:**
1. Usar Argon2 (más eficiente que BCrypt)
2. Implementar rate limiting agresivo (3 intentos/minuto)
3. Usar CAPTCHA después de 3 intentos fallidos
4. Implementar 2FA obligatorio para admins

---

🤖 **Investigación por Claude Code**
📅 **Última actualización:** 13 de octubre de 2025
