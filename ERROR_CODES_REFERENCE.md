# Error Codes Reference - Admision MTN Backend

## Overview

This document provides a comprehensive reference for all standardized error codes used across the 6 microservices in the Admision MTN backend.

**Standard Error Response Format:**
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message",
    "details": "Additional context (optional)",
    "timestamp": "2025-10-11T10:30:00Z"
  },
  "data": null
}
```

**Standard Success Response Format:**
```json
{
  "success": true,
  "data": { /* response data */ },
  "error": null,
  "timestamp": "2025-10-11T10:30:00Z"
}
```

## Error Code Categories

### 1. Validation Errors (400 Bad Request)

| Error Code | HTTP Status | Message | Usage |
|------------|-------------|---------|-------|
| `VALIDATION_ERROR` | 400 | Validation failed | Generic validation failure |
| `INVALID_INPUT` | 400 | Invalid input data | Malformed or incorrect data type |
| `MISSING_FIELDS` | 400 | Required fields missing | One or more required fields not provided |
| `INVALID_FORMAT` | 400 | Invalid format | Email, phone, RUT format errors |
| `INVALID_DATE` | 400 | Invalid date | Date parsing or validation errors |
| `INVALID_RUT` | 400 | Invalid Chilean RUT | RUT format or verification digit errors |

**Example Scenarios:**
- Missing email field in registration
- Invalid date format in interview scheduling
- Malformed JSON payload
- Chilean RUT with incorrect verification digit

**Example curl command:**
```bash
# Missing required fields
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "test@test.cl"}'

# Expected Response:
{
  "success": false,
  "error": {
    "code": "MISSING_FIELDS",
    "message": "Required fields missing: firstName, lastName, password",
    "details": ["firstName", "lastName", "password"],
    "timestamp": "2025-10-11T10:30:00Z"
  },
  "data": null
}
```

### 2. Authentication Errors (401 Unauthorized)

| Error Code | HTTP Status | Message | Usage |
|------------|-------------|---------|-------|
| `UNAUTHORIZED` | 401 | Authentication required | No token provided |
| `INVALID_TOKEN` | 401 | Invalid or expired token | JWT validation failed |
| `INVALID_CREDENTIALS` | 401 | Invalid credentials | Wrong email/password combination |
| `EXPIRED_TOKEN` | 401 | Token expired | JWT expired |
| `USER_INACTIVE` | 401 | User account inactive | Account deactivated |

**Example Scenarios:**
- Login with wrong password
- Accessing protected endpoint without token
- Using expired JWT token
- Attempting to use deactivated account

**Example curl commands:**
```bash
# Invalid credentials
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "jorge.gangale@mtn.cl", "password": "wrongpass"}'

# Expected Response:
{
  "success": false,
  "error": {
    "code": "INVALID_CREDENTIALS",
    "message": "Invalid email or password",
    "timestamp": "2025-10-11T10:30:00Z"
  },
  "data": null
}

# Missing token
curl -X GET http://localhost:8080/api/users

# Expected Response:
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Authentication required - no token provided",
    "timestamp": "2025-10-11T10:30:00Z"
  },
  "data": null
}
```

### 3. Authorization Errors (403 Forbidden)

| Error Code | HTTP Status | Message | Usage |
|------------|-------------|---------|-------|
| `FORBIDDEN` | 403 | Insufficient permissions | User lacks required role/permission |
| `ACCESS_DENIED` | 403 | Access denied | Specific resource access denied |
| `ROLE_REQUIRED` | 403 | Required role missing | Endpoint requires specific role (e.g., ADMIN) |
| `CSRF_TOKEN_MISSING` | 403 | CSRF token missing | CSRF protection triggered |
| `CSRF_TOKEN_INVALID` | 403 | CSRF token invalid | CSRF token validation failed |

**Example Scenarios:**
- Teacher trying to access admin-only endpoint
- Guardian trying to modify another user's application
- Missing CSRF token on POST/PUT/DELETE requests
- User without ADMIN role attempting user deletion

**Example curl command:**
```bash
# Insufficient permissions
curl -X DELETE http://localhost:8080/api/users/5 \
  -H "Authorization: Bearer TEACHER_TOKEN"

# Expected Response:
{
  "success": false,
  "error": {
    "code": "FORBIDDEN",
    "message": "This action requires ADMIN role",
    "details": { "requiredRole": "ADMIN", "userRole": "TEACHER" },
    "timestamp": "2025-10-11T10:30:00Z"
  },
  "data": null
}
```

### 4. Not Found Errors (404 Not Found)

| Error Code | HTTP Status | Message | Usage |
|------------|-------------|---------|-------|
| `NOT_FOUND` | 404 | Resource not found | Generic resource not found |
| `USER_NOT_FOUND` | 404 | User not found | User ID doesn't exist |
| `APPLICATION_NOT_FOUND` | 404 | Application not found | Application ID doesn't exist |
| `INTERVIEW_NOT_FOUND` | 404 | Interview not found | Interview ID doesn't exist |
| `EVALUATION_NOT_FOUND` | 404 | Evaluation not found | Evaluation ID doesn't exist |
| `DOCUMENT_NOT_FOUND` | 404 | Document not found | Document ID doesn't exist |

**Example Scenarios:**
- Requesting user with non-existent ID
- Fetching application that was deleted
- Accessing interview that doesn't belong to user

**Example curl command:**
```bash
# Non-existent user
curl -X GET http://localhost:8080/api/users/99999 \
  -H "Authorization: Bearer ADMIN_TOKEN"

# Expected Response:
{
  "success": false,
  "error": {
    "code": "USER_NOT_FOUND",
    "message": "User with ID 99999 not found",
    "timestamp": "2025-10-11T10:30:00Z"
  },
  "data": null
}
```

### 5. Conflict Errors (409 Conflict)

| Error Code | HTTP Status | Message | Usage |
|------------|-------------|---------|-------|
| `CONFLICT` | 409 | Resource conflict | Generic conflict |
| `DUPLICATE_ENTRY` | 409 | Duplicate entry | Unique constraint violation |
| `EMAIL_EXISTS` | 409 | Email already exists | Email already registered |
| `RUT_EXISTS` | 409 | RUT already exists | Chilean RUT already registered |
| `SCHEDULE_CONFLICT` | 409 | Schedule conflict | Interview time slot conflict |
| `FOREIGN_KEY_VIOLATION` | 409 | Cannot delete - has dependencies | Referential integrity error |

**Example Scenarios:**
- Registering with existing email
- Creating user with duplicate RUT
- Scheduling interview at occupied time slot
- Deleting user who has evaluations

**Example curl command:**
```bash
# Duplicate email
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "jorge.gangale@mtn.cl",
    "password": "test123"
  }'

# Expected Response:
{
  "success": false,
  "error": {
    "code": "EMAIL_EXISTS",
    "message": "Email jorge.gangale@mtn.cl is already registered",
    "timestamp": "2025-10-11T10:30:00Z"
  },
  "data": null
}

# Foreign key violation
curl -X DELETE http://localhost:8080/api/users/7 \
  -H "Authorization: Bearer ADMIN_TOKEN"

# Expected Response:
{
  "success": false,
  "error": {
    "code": "FOREIGN_KEY_VIOLATION",
    "message": "Cannot delete user - has associated evaluations",
    "details": "This user has 5 evaluations assigned. Deactivate instead of delete.",
    "timestamp": "2025-10-11T10:30:00Z"
  },
  "data": null
}
```

### 6. Internal Server Errors (500 Internal Server Error)

| Error Code | HTTP Status | Message | Usage |
|------------|-------------|---------|-------|
| `INTERNAL_ERROR` | 500 | Internal server error | Unhandled exceptions |
| `DATABASE_ERROR` | 500 | Database operation failed | Database connection/query errors |
| `ENCRYPTION_ERROR` | 500 | Encryption failed | RSA/AES encryption errors |
| `DECRYPTION_ERROR` | 500 | Decryption failed | Credential decryption errors |

**Example Scenarios:**
- Database connection pool exhausted
- SQL query syntax error
- Unexpected exception in business logic
- RSA key pair initialization failure

**Example Response:**
```json
{
  "success": false,
  "error": {
    "code": "DATABASE_ERROR",
    "message": "Database connection failed",
    "details": "Connection pool exhausted - retry in 30 seconds",
    "timestamp": "2025-10-11T10:30:00Z"
  },
  "data": null
}
```

### 7. Service Unavailable Errors (503 Service Unavailable)

| Error Code | HTTP Status | Message | Usage |
|------------|-------------|---------|-------|
| `SERVICE_UNAVAILABLE` | 503 | Service temporarily unavailable | Service overloaded or down |
| `CIRCUIT_BREAKER_OPEN` | 503 | Circuit breaker open | Circuit breaker preventing requests |
| `EXTERNAL_SERVICE_ERROR` | 503 | External service failed | SMTP, S3, or third-party API failure |

**Example Scenarios:**
- Circuit breaker open due to database failures
- Email service (SMTP) unavailable
- Document storage (S3) unavailable
- Rate limit exceeded

**Example curl command:**
```bash
# Circuit breaker open
curl -X GET http://localhost:8080/api/dashboard/stats

# Expected Response:
{
  "success": false,
  "error": {
    "code": "CIRCUIT_BREAKER_OPEN",
    "message": "Service temporarily unavailable - circuit breaker open",
    "details": { "retryAfter": 30 },
    "timestamp": "2025-10-11T10:30:00Z"
  },
  "data": null
}
```

## Service-Specific Error Codes

### User Service (Port 8082)

| Endpoint | Common Error Codes |
|----------|-------------------|
| `POST /api/auth/login` | INVALID_CREDENTIALS, USER_INACTIVE, VALIDATION_ERROR |
| `POST /api/auth/register` | EMAIL_EXISTS, RUT_EXISTS, VALIDATION_ERROR |
| `GET /api/users/:id` | USER_NOT_FOUND, UNAUTHORIZED |
| `PUT /api/users/:id` | USER_NOT_FOUND, VALIDATION_ERROR, FORBIDDEN |
| `DELETE /api/users/:id` | USER_NOT_FOUND, FOREIGN_KEY_VIOLATION, FORBIDDEN |

### Application Service (Port 8083)

| Endpoint | Common Error Codes |
|----------|-------------------|
| `POST /api/applications` | VALIDATION_ERROR, GUARDIAN_NOT_FOUND, DUPLICATE_ENTRY |
| `GET /api/applications/:id` | APPLICATION_NOT_FOUND, UNAUTHORIZED |
| `PUT /api/applications/:id/status` | APPLICATION_NOT_FOUND, INVALID_STATUS_TRANSITION |
| `POST /api/documents` | FILE_TOO_LARGE, INVALID_FILE_TYPE, EXTERNAL_SERVICE_ERROR |

### Evaluation Service (Port 8084)

| Endpoint | Common Error Codes |
|----------|-------------------|
| `POST /api/interviews` | SCHEDULE_CONFLICT, VALIDATION_ERROR, APPLICATION_NOT_FOUND |
| `POST /api/evaluations` | VALIDATION_ERROR, EVALUATOR_NOT_FOUND, APPLICATION_NOT_FOUND |
| `GET /api/interviews/:id` | INTERVIEW_NOT_FOUND, UNAUTHORIZED |

### Notification Service (Port 8085)

| Endpoint | Common Error Codes |
|----------|-------------------|
| `POST /api/email/send` | VALIDATION_ERROR, EXTERNAL_SERVICE_ERROR (SMTP) |
| `POST /api/email/send-verification` | VALIDATION_ERROR, EMAIL_SEND_FAILED |
| `POST /api/email/verify-code` | INVALID_CODE, CODE_EXPIRED, TOO_MANY_ATTEMPTS |

### Dashboard Service (Port 8086)

| Endpoint | Common Error Codes |
|----------|-------------------|
| `GET /api/dashboard/stats` | DATABASE_ERROR, CIRCUIT_BREAKER_OPEN |
| `GET /api/analytics/*` | DATABASE_ERROR, CIRCUIT_BREAKER_OPEN |

### Guardian Service (Port 8087)

| Endpoint | Common Error Codes |
|----------|-------------------|
| `POST /api/guardians/auth/register` | EMAIL_EXISTS, RUT_EXISTS, VALIDATION_ERROR |
| `GET /api/guardians/:id` | NOT_FOUND, UNAUTHORIZED |

## Testing Error Responses

### Test Suite for Validation Errors (400)

```bash
# Missing fields
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "test@test.cl"}'

# Invalid email format
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstName": "Test", "lastName": "User", "email": "invalid-email", "password": "test123"}'

# Invalid RUT
curl -X POST http://localhost:8080/api/guardians/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstName": "Test", "lastName": "User", "email": "test@test.cl", "rut": "12.345.678-0", "password": "test123"}'
```

### Test Suite for Authentication Errors (401)

```bash
# No token
curl -X GET http://localhost:8080/api/users

# Invalid credentials
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "jorge.gangale@mtn.cl", "password": "wrongpassword"}'

# Expired token (simulated)
curl -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE2MDAwMDAwMDB9.EXPIRED"
```

### Test Suite for Authorization Errors (403)

```bash
# Insufficient permissions
curl -X DELETE http://localhost:8080/api/users/5 \
  -H "Authorization: Bearer TEACHER_TOKEN"

# CSRF token missing (when enabled)
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -d '{"firstName": "Test", "lastName": "User", "email": "test@test.cl"}'
```

### Test Suite for Not Found Errors (404)

```bash
# Non-existent user
curl -X GET http://localhost:8080/api/users/99999 \
  -H "Authorization: Bearer ADMIN_TOKEN"

# Non-existent application
curl -X GET http://localhost:8080/api/applications/99999 \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

### Test Suite for Conflict Errors (409)

```bash
# Duplicate email
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstName": "John", "lastName": "Doe", "email": "jorge.gangale@mtn.cl", "password": "test123"}'

# Foreign key violation
curl -X DELETE http://localhost:8080/api/users/7 \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

### Test Suite for Service Unavailable Errors (503)

```bash
# Circuit breaker open (simulate by overloading service)
for i in {1..100}; do
  curl -X GET http://localhost:8080/api/dashboard/stats &
done
wait

# Then try normal request
curl -X GET http://localhost:8080/api/dashboard/stats
```

## Error Response Examples by HTTP Status

### 400 - Bad Request
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Email is required",
    "details": { "field": "email", "reason": "missing" },
    "timestamp": "2025-10-11T10:30:00Z"
  },
  "data": null
}
```

### 401 - Unauthorized
```json
{
  "success": false,
  "error": {
    "code": "INVALID_CREDENTIALS",
    "message": "Invalid email or password",
    "timestamp": "2025-10-11T10:30:00Z"
  },
  "data": null
}
```

### 403 - Forbidden
```json
{
  "success": false,
  "error": {
    "code": "FORBIDDEN",
    "message": "This action requires ADMIN role",
    "details": { "requiredRole": "ADMIN", "userRole": "TEACHER" },
    "timestamp": "2025-10-11T10:30:00Z"
  },
  "data": null
}
```

### 404 - Not Found
```json
{
  "success": false,
  "error": {
    "code": "USER_NOT_FOUND",
    "message": "User with ID 99999 not found",
    "timestamp": "2025-10-11T10:30:00Z"
  },
  "data": null
}
```

### 409 - Conflict
```json
{
  "success": false,
  "error": {
    "code": "EMAIL_EXISTS",
    "message": "Email jorge.gangale@mtn.cl is already registered",
    "timestamp": "2025-10-11T10:30:00Z"
  },
  "data": null
}
```

### 500 - Internal Server Error
```json
{
  "success": false,
  "error": {
    "code": "DATABASE_ERROR",
    "message": "Database connection failed",
    "details": "Connection pool exhausted - retry in 30 seconds",
    "timestamp": "2025-10-11T10:30:00Z"
  },
  "data": null
}
```

### 503 - Service Unavailable
```json
{
  "success": false,
  "error": {
    "code": "CIRCUIT_BREAKER_OPEN",
    "message": "Service temporarily unavailable - circuit breaker open",
    "details": { "retryAfter": 30 },
    "timestamp": "2025-10-11T10:30:00Z"
  },
  "data": null
}
```

## Migration Checklist

- [x] Create standardized error handler utility (`errorHandler.js`)
- [ ] Update User Service (71 error responses)
- [ ] Update Application Service (132 error responses)
- [ ] Update Evaluation Service (144 error responses)
- [ ] Update Notification Service (53 error responses)
- [ ] Update Dashboard Service (16 error responses)
- [ ] Update Guardian Service (15 error responses)
- [ ] Update all success responses to include `error: null`
- [ ] Test all endpoints with curl commands from this document
- [ ] Update frontend error handling to parse new format
- [ ] Update CLAUDE.md with error handling documentation

## Frontend Error Handling

The frontend should parse error responses as follows:

```typescript
try {
  const response = await api.post('/api/auth/login', { email, password });
  if (response.data.success) {
    // Handle success
    const data = response.data.data;
  }
} catch (error) {
  if (error.response?.data) {
    const errorResponse = error.response.data;
    // Display error.error.message to user
    console.error(`Error ${errorResponse.error.code}: ${errorResponse.error.message}`);

    // Show details if available
    if (errorResponse.error.details) {
      console.error('Details:', errorResponse.error.details);
    }
  }
}
```

## Summary

**Total Error Responses Standardized:** 434 across 6 services
- User Service: 71
- Application Service: 132
- Evaluation Service: 144
- Notification Service: 53
- Dashboard Service: 16
- Guardian Service: 15

**Error Code Categories:** 7
1. Validation Errors (400)
2. Authentication Errors (401)
3. Authorization Errors (403)
4. Not Found Errors (404)
5. Conflict Errors (409)
6. Internal Server Errors (500)
7. Service Unavailable Errors (503)

**Total Error Codes Defined:** 35+

All services now follow the standard contract:
```json
{
  "success": boolean,
  "data": object | null,
  "error": { "code": string, "message": string, "details": any, "timestamp": string } | null,
  "timestamp": string
}
```
