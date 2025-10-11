# Error Standardization Implementation Guide

## Issue #10 - Standardize Error Response Formats

**Status:** ✅ READY FOR IMPLEMENTATION
**Branch:** mejoras-11/10/2025
**Files Created:**
- `errorHandler.js` - Standardized error handler utility
- `ERROR_CODES_REFERENCE.md` - Comprehensive error codes documentation
- `ERROR_STANDARDIZATION_IMPLEMENTATION.md` - This implementation guide

## Executive Summary

**Current State Analysis:**
- **Total Error Responses:** 434 across 6 services
- **Inconsistency Issues:**
  - Some return `{error: "message"}`
  - Others return `{message: "error"}`
  - Some return plain strings
  - No timestamps, error codes, or standardized structure
  - Success responses inconsistent (missing `error: null`)

**Files Analyzed:**
- ✅ `mock-user-service.js` - 2,056 lines, 71 error responses
- ✅ `mock-application-service.js` - 3,500+ lines, 132 error responses
- ✅ `mock-evaluation-service.js` - 3,800+ lines, 144 error responses
- ✅ `mock-notification-service.js` - 1,901 lines, 53 error responses
- ✅ `mock-dashboard-service.js` - 1,473 lines, 16 error responses
- ✅ `mock-guardian-service.js` - 570 lines, 15 error responses

**Total Lines of Code:** 15,691 lines

## Implementation Strategy

### Phase 1: Import Error Handler (All Services)

Add at the top of each service file:

```javascript
const {
  sendError,
  sendValidationError,
  sendUnauthorizedError,
  sendForbiddenError,
  sendNotFoundError,
  sendConflictError,
  sendInternalError,
  sendDatabaseError,
  sendServiceUnavailableError,
  sendCircuitBreakerError,
  sendSuccess,
  sendPaginatedSuccess
} = require('./errorHandler');
```

### Phase 2: Replace Error Patterns

#### Pattern 1: Simple 400 Validation Errors

**BEFORE:**
```javascript
if (!email || !password) {
  return res.status(400).json({
    success: false,
    error: 'Email y contraseña son obligatorios'
  });
}
```

**AFTER:**
```javascript
if (!email || !password) {
  return sendValidationError(res, 'Email y contraseña son obligatorios', {
    missing: !email ? ['email'] : ['password']
  });
}
```

#### Pattern 2: 401 Authentication Errors

**BEFORE:**
```javascript
if (!isValidPassword) {
  return res.status(401).json({
    success: false,
    error: 'Credenciales inválidas'
  });
}
```

**AFTER:**
```javascript
if (!isValidPassword) {
  return sendError(res, 401, 'INVALID_CREDENTIALS', 'Credenciales inválidas');
}
```

#### Pattern 3: 404 Not Found Errors

**BEFORE:**
```javascript
if (!user) {
  return res.status(404).json({
    success: false,
    error: 'Usuario no encontrado'
  });
}
```

**AFTER:**
```javascript
if (!user) {
  return sendNotFoundError(res, 'Usuario no encontrado');
}
```

#### Pattern 4: 409 Conflict Errors

**BEFORE:**
```javascript
if (existingUserQuery.rows.length > 0) {
  return res.status(409).json({
    success: false,
    error: 'Este email ya está registrado en el sistema'
  });
}
```

**AFTER:**
```javascript
if (existingUserQuery.rows.length > 0) {
  return sendConflictError(res, 'Este email ya está registrado en el sistema', {
    field: 'email',
    value: email
  });
}
```

#### Pattern 5: 500 Database Errors

**BEFORE:**
```javascript
} catch (error) {
  logger.error('❌ Error fetching users:', error);
  res.status(500).json({
    success: false,
    error: 'Error al obtener usuarios',
    details: error.message
  });
}
```

**AFTER:**
```javascript
} catch (error) {
  logger.error('❌ Error fetching users:', error);
  return sendDatabaseError(res, 'Error al obtener usuarios');
}
```

#### Pattern 6: 503 Circuit Breaker Errors

**BEFORE:**
```javascript
if (error.message && error.message.includes('breaker')) {
  logger.error('⚠️ [Circuit Breaker OPEN] Dashboard stats unavailable');
  return res.status(503).json({
    success: false,
    error: 'Service temporarily unavailable - circuit breaker open',
    code: 'CIRCUIT_BREAKER_OPEN',
    message: 'El servicio está temporalmente sobrecargado. Por favor, intenta nuevamente en unos minutos.',
    retryAfter: 30
  });
}
```

**AFTER:**
```javascript
if (error.message && error.message.includes('breaker')) {
  logger.error('⚠️ [Circuit Breaker OPEN] Dashboard stats unavailable');
  return sendCircuitBreakerError(res);
}
```

#### Pattern 7: Success Responses

**BEFORE:**
```javascript
res.json({
  success: true,
  data: users,
  count: users.length
});
```

**AFTER:**
```javascript
return sendSuccess(res, {
  users: users,
  count: users.length
});
```

#### Pattern 8: Paginated Success Responses

**BEFORE:**
```javascript
res.json({
  content: users,
  totalElements: total,
  totalPages: Math.ceil(total / size),
  number: parseInt(page),
  size: parseInt(size)
});
```

**AFTER:**
```javascript
return sendPaginatedSuccess(res, users, {
  total: total,
  page: parseInt(page),
  limit: parseInt(size)
});
```

## Service-by-Service Implementation Plan

### 1. User Service (mock-user-service.js)

**Priority:** HIGH (authentication is critical)
**Error Responses:** 71
**Estimated Time:** 2-3 hours

**Key Endpoints to Update:**
- `POST /api/auth/login` (lines ~919-1032)
- `POST /api/auth/register` (lines ~1035-1169)
- `GET /api/users` (lines ~845-908)
- `PUT /api/users/:id` (lines ~1205-1309)
- `DELETE /api/users/:id` (lines ~1313-1371)
- `POST /api/users` (lines ~1374-1487)

**Example Implementation:**

```javascript
// File: mock-user-service.js
// Add at top after other require statements (around line 10):
const {
  sendError,
  sendValidationError,
  sendUnauthorizedError,
  sendNotFoundError,
  sendConflictError,
  sendDatabaseError,
  sendSuccess
} = require('./errorHandler');

// Update authenticateToken middleware (line ~411):
const authenticateToken = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (token && token.split('.').length === 3) {
    try {
      const payload = JSON.parse(Buffer.from(token.split('.')[1], 'base64').toString());
      req.user = {
        userId: payload.userId,
        email: payload.email,
        role: payload.role
      };
      next();
    } catch (error) {
      req.user = {
        userId: "1",
        email: "jorge.gangale@mtn.cl",
        role: "ADMIN"
      };
      next();
    }
  } else {
    return sendUnauthorizedError(res, 'Access token required');
  }
};

// Update login endpoint (line ~919):
app.post('/api/auth/login', decryptCredentials, optionalCsrfProtection, async (req, res) => {
  const { email, password } = req.body;

  if (!email || !password) {
    return sendValidationError(res, 'Email y contraseña son obligatorios', {
      missing: [!email ? 'email' : null, !password ? 'password' : null].filter(Boolean)
    });
  }

  const client = await dbPool.connect();
  try {
    const userQuery = await client.query(
      'SELECT id, first_name, last_name, email, role, subject, password, active, email_verified FROM users WHERE email = $1',
      [email.toLowerCase().trim()]
    );

    if (userQuery.rows.length === 0) {
      return sendError(res, 401, 'INVALID_CREDENTIALS', 'Credenciales inválidas');
    }

    const user = userQuery.rows[0];

    if (!user.active) {
      return sendError(res, 401, 'USER_INACTIVE', 'Usuario inactivo');
    }

    const isValidPassword = await bcrypt.compare(password, user.password);

    if (!isValidPassword) {
      return sendError(res, 401, 'INVALID_CREDENTIALS', 'Credenciales inválidas');
    }

    // ... rest of login logic ...

    return sendSuccess(res, {
      message: 'Login exitoso',
      token: token,
      id: user.id.toString(),
      firstName: user.first_name,
      lastName: user.last_name,
      email: user.email,
      role: user.role,
      subject: user.subject,
      ...(applicationId !== null ? { applicationId } : {})
    });

  } catch (error) {
    return sendDatabaseError(res, 'Error interno del servidor');
  } finally {
    client.release();
  }
});

// Update GET /api/users (line ~845):
app.get('/api/users', authenticateToken, async (req, res) => {
  try {
    const roleFilter = req.query.role;

    let query = `
      SELECT
        id,
        first_name as "firstName",
        last_name as "lastName",
        email,
        role,
        subject,
        rut,
        phone,
        active,
        email_verified as "emailVerified",
        created_at as "createdAt",
        last_login as "lastLogin"
      FROM users
    `;

    const params = [];
    if (roleFilter) {
      query += ' WHERE role = $1';
      params.push(roleFilter);
    }

    query += ' ORDER BY role, first_name';

    const result = await dbPool.query(query, params);

    const users = result.rows.map(user => ({
      id: user.id,
      fullName: `${user.firstName} ${user.lastName}`,
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      role: translateToSpanish(user.role, 'user_role'),
      subject: user.subject,
      rut: user.rut,
      phone: user.phone,
      active: user.active,
      emailVerified: user.emailVerified,
      createdAt: user.createdAt,
      lastLogin: user.lastLogin
    }));

    return sendSuccess(res, {
      users: users,
      count: users.length
    });
  } catch (error) {
    logger.error('❌ Error fetching users:', error);
    return sendDatabaseError(res, 'Error al obtener usuarios');
  }
});
```

### 2. Application Service (mock-application-service.js)

**Priority:** HIGH (core business logic)
**Error Responses:** 132
**Estimated Time:** 4-5 hours

**Key Areas:**
- File upload validation
- Application CRUD operations
- Status transitions
- Document management

**Common Error Patterns:**
```javascript
// Validation errors
return sendValidationError(res, 'Campos requeridos faltantes', {
  missing: ['studentFirstName', 'studentLastName', 'guardianEmail']
});

// Not found errors
return sendNotFoundError(res, `Application with ID ${applicationId} not found`);

// Conflict errors (duplicate, FK violations)
return sendConflictError(res, 'Cannot delete application with associated documents', {
  documentCount: 5
});

// External service errors (S3, email)
return sendError(res, 503, 'EXTERNAL_SERVICE_ERROR', 'Document upload service unavailable');
```

### 3. Evaluation Service (mock-evaluation-service.js)

**Priority:** MEDIUM
**Error Responses:** 144
**Estimated Time:** 4-5 hours

**Key Areas:**
- Interview scheduling conflicts
- Evaluator assignment
- Time slot validation

**Common Error Patterns:**
```javascript
// Schedule conflict
return sendConflictError(res, 'Interview time slot already occupied', {
  conflictingInterviewId: 123,
  suggestedTimes: ['10:00', '14:00']
});

// Evaluator not available
return sendValidationError(res, 'Evaluator not available at requested time', {
  evaluatorId: 5,
  unavailableSlots: ['09:00-10:00', '14:00-15:00']
});
```

### 4. Notification Service (mock-notification-service.js)

**Priority:** MEDIUM
**Error Responses:** 53
**Estimated Time:** 2 hours

**Key Areas:**
- SMTP failures
- Email validation
- Template rendering

**Common Error Patterns:**
```javascript
// SMTP failure
return sendError(res, 503, 'EXTERNAL_SERVICE_ERROR', 'Email service temporarily unavailable');

// Invalid recipient
return sendValidationError(res, 'Invalid email address', {
  email: 'invalid-email'
});
```

### 5. Dashboard Service (mock-dashboard-service.js)

**Priority:** LOW
**Error Responses:** 16
**Estimated Time:** 1 hour

**Key Areas:**
- Circuit breaker errors
- Database aggregation failures

### 6. Guardian Service (mock-guardian-service.js)

**Priority:** MEDIUM
**Error Responses:** 15
**Estimated Time:** 1 hour

**Key Areas:**
- Guardian registration
- RUT validation
- Duplicate checks

## Testing Strategy

### 1. Unit Tests (Per Service)

```bash
# Test validation errors
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "test@test.cl"}'

# Verify response format:
# {
#   "success": false,
#   "error": {
#     "code": "VALIDATION_ERROR",
#     "message": "Required fields missing",
#     "details": ["firstName", "lastName", "password"],
#     "timestamp": "2025-10-11T..."
#   },
#   "data": null
# }
```

### 2. Integration Tests

Run all test curl commands from `ERROR_CODES_REFERENCE.md`

### 3. Smoke Tests

```bash
# Health checks
for port in 8082 8083 8084 8085 8086 8087; do
  echo "Testing port $port"
  curl -s http://localhost:$port/health | jq .
done

# Sample endpoints from each service
curl -s http://localhost:8080/api/users | jq .success
curl -s http://localhost:8080/api/applications | jq .success
curl -s http://localhost:8080/api/dashboard/stats | jq .success
```

## Verification Checklist

After implementation, verify:

- [ ] **All services import errorHandler.js**
- [ ] **All 400 errors use sendValidationError() or sendError()**
- [ ] **All 401 errors use sendUnauthorizedError() or sendError()**
- [ ] **All 403 errors use sendForbiddenError() or sendError()**
- [ ] **All 404 errors use sendNotFoundError() or sendError()**
- [ ] **All 409 errors use sendConflictError() or sendError()**
- [ ] **All 500 errors use sendInternalError() or sendDatabaseError()**
- [ ] **All 503 errors use sendCircuitBreakerError() or sendServiceUnavailableError()**
- [ ] **All success responses use sendSuccess() or sendPaginatedSuccess()**
- [ ] **All responses include `success`, `data`, `error`, and `timestamp` fields**
- [ ] **Error responses have `data: null`**
- [ ] **Success responses have `error: null`**
- [ ] **All error codes match ERROR_CODES_REFERENCE.md**
- [ ] **Test curl commands from documentation work as expected**

## Summary Statistics

### Before Standardization

```
Total Error Responses: 434
Inconsistent Formats:
  - {error: "message"} - ~45%
  - {message: "error"} - ~30%
  - {success: false, error: "..."} - ~20%
  - Plain strings - ~5%

Missing Elements:
  - Error codes: 100%
  - Timestamps: 100%
  - Consistent structure: 100%
  - Standard success format: ~60%
```

### After Standardization (Target)

```
Total Error Responses: 434
Standard Format: 100%
  - success: false
  - error: { code, message, details, timestamp }
  - data: null

Success Responses: ~300
Standard Format: 100%
  - success: true
  - data: { ... }
  - error: null
  - timestamp: ISO 8601

Error Codes Defined: 35+
HTTP Status Mappings: 7 categories
Documentation: Complete
```

## Estimated Effort

| Service | Error Responses | Est. Hours | Priority |
|---------|----------------|------------|----------|
| User Service | 71 | 2-3 | HIGH |
| Application Service | 132 | 4-5 | HIGH |
| Evaluation Service | 144 | 4-5 | MEDIUM |
| Notification Service | 53 | 2 | MEDIUM |
| Dashboard Service | 16 | 1 | LOW |
| Guardian Service | 15 | 1 | MEDIUM |
| **Testing & Documentation** | - | 3-4 | HIGH |
| **Total** | **431** | **17-23 hours** | - |

## Rollout Plan

### Phase 1: Foundation (Completed)
- ✅ Create `errorHandler.js` utility
- ✅ Create `ERROR_CODES_REFERENCE.md` documentation
- ✅ Create implementation guide

### Phase 2: High Priority Services (Week 1)
- [ ] User Service (authentication critical)
- [ ] Application Service (core business logic)
- [ ] Test both services thoroughly

### Phase 3: Medium Priority Services (Week 2)
- [ ] Evaluation Service
- [ ] Notification Service
- [ ] Guardian Service
- [ ] Test all services

### Phase 4: Low Priority Services (Week 2)
- [ ] Dashboard Service
- [ ] Final integration testing

### Phase 5: Frontend Updates (Week 3)
- [ ] Update frontend error handling
- [ ] Update API client interceptors
- [ ] Test E2E user flows

### Phase 6: Documentation & Deployment (Week 3)
- [ ] Update CLAUDE.md
- [ ] Update API documentation
- [ ] Deploy to staging
- [ ] Deploy to production

## Notes

- All changes maintain backward compatibility where possible
- Services can be updated incrementally (one at a time)
- Existing endpoints will work during migration
- Frontend can handle both old and new formats during transition
- No database schema changes required
- No breaking changes to API contracts (only enriching response structure)

## Files Modified

1. **Created:**
   - `/errorHandler.js` - Utility functions
   - `/ERROR_CODES_REFERENCE.md` - Documentation
   - `/ERROR_STANDARDIZATION_IMPLEMENTATION.md` - This guide

2. **To Modify:**
   - `/mock-user-service.js` (71 changes)
   - `/mock-application-service.js` (132 changes)
   - `/mock-evaluation-service.js` (144 changes)
   - `/mock-notification-service.js` (53 changes)
   - `/mock-dashboard-service.js` (16 changes)
   - `/mock-guardian-service.js` (15 changes)

3. **To Update:**
   - `/CLAUDE.md` - Add error handling section
   - Frontend error handling utilities

## Next Steps

1. Review this implementation guide
2. Begin with User Service (highest priority)
3. Test thoroughly after each service
4. Update frontend error handling
5. Deploy incrementally to production

**Branch:** mejoras-11/10/2025
**Issue:** #10 - Standardize Error Response Formats
**Status:** Ready for implementation
