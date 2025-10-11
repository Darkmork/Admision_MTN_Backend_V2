# Error Handler Quick Reference Card

## Import Statement

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
  sendCircuitBreakerError,
  sendSuccess,
  sendPaginatedSuccess
} = require('./errorHandler');
```

## Quick Patterns

### Validation Error (400)
```javascript
if (!email || !password) {
  return sendValidationError(res, 'Email y contraseña son obligatorios', {
    missing: ['email', 'password']
  });
}
```

### Authentication Error (401)
```javascript
if (!isValidPassword) {
  return sendError(res, 401, 'INVALID_CREDENTIALS', 'Credenciales inválidas');
}
```

### Authorization Error (403)
```javascript
if (user.role !== 'ADMIN') {
  return sendForbiddenError(res, 'Se requiere rol de administrador');
}
```

### Not Found Error (404)
```javascript
if (!user) {
  return sendNotFoundError(res, 'Usuario no encontrado');
}
```

### Conflict Error (409)
```javascript
if (existingUser) {
  return sendConflictError(res, 'Email ya registrado', { email });
}
```

### Database Error (500)
```javascript
} catch (error) {
  logger.error('Error:', error);
  return sendDatabaseError(res, 'Error al obtener datos');
}
```

### Circuit Breaker Error (503)
```javascript
if (error.message.includes('breaker')) {
  return sendCircuitBreakerError(res);
}
```

### Success Response
```javascript
return sendSuccess(res, {
  users: users,
  count: users.length
});
```

### Paginated Success
```javascript
return sendPaginatedSuccess(res, items, {
  total: 100,
  page: 0,
  limit: 10
});
```

## Response Formats

### Error Response
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Email is required",
    "details": { "missing": ["email"] },
    "timestamp": "2025-10-11T10:30:00Z"
  },
  "data": null
}
```

### Success Response
```json
{
  "success": true,
  "data": {
    "users": [...],
    "count": 47
  },
  "error": null,
  "timestamp": "2025-10-11T10:30:00Z"
}
```

## Common Error Codes

| Code | HTTP | Use When |
|------|------|----------|
| `VALIDATION_ERROR` | 400 | Invalid/missing input |
| `INVALID_CREDENTIALS` | 401 | Wrong password |
| `UNAUTHORIZED` | 401 | Missing token |
| `FORBIDDEN` | 403 | Insufficient permissions |
| `NOT_FOUND` | 404 | Resource doesn't exist |
| `CONFLICT` | 409 | Duplicate entry |
| `EMAIL_EXISTS` | 409 | Email already registered |
| `DATABASE_ERROR` | 500 | DB query failed |
| `INTERNAL_ERROR` | 500 | Unhandled exception |
| `CIRCUIT_BREAKER_OPEN` | 503 | Service overloaded |

## Testing

```bash
# Test validation error
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "test@test.cl"}'

# Test auth error
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@test.cl", "password": "wrong"}'

# Test not found
curl -X GET http://localhost:8080/api/users/99999 \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

## Checklist

When migrating an endpoint:

- [ ] Import errorHandler functions
- [ ] Replace all `res.status().json()` with helper functions
- [ ] Add error codes to all errors
- [ ] Add details field to validation errors
- [ ] Use `sendSuccess()` for success responses
- [ ] Test with curl command
- [ ] Verify response format matches standard

## See Also

- Full documentation: `ERROR_CODES_REFERENCE.md`
- Implementation guide: `ERROR_STANDARDIZATION_IMPLEMENTATION.md`
- Summary report: `ERROR_STANDARDIZATION_SUMMARY.md`
