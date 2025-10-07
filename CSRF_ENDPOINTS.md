# CSRF Protected Endpoints - Complete Reference

## User Service (mock-user-service.js)

### Public Endpoints (No CSRF Required)

| Endpoint | Method | Purpose | Auth Required |
|----------|--------|---------|---------------|
| `/health` | GET | Service health check | ❌ |
| `/api/auth/csrf-token` | GET | Generate CSRF token | ❌ |
| `/api/users/roles` | GET | Get all user roles | ❌ |
| `/api/users/public/school-staff` | GET | Get school staff list | ❌ |
| `/api/users/cache/stats` | GET | Cache statistics | ❌ |
| `/api/users` | GET | List all users | ✅ JWT |
| `/api/users/:id` | GET | Get user by ID | ✅ JWT |

### CSRF Protected Endpoints (Mutations)

| Endpoint | Method | Purpose | Auth Required | CSRF Token | Status |
|----------|--------|---------|---------------|------------|--------|
| `/api/auth/login` | POST | Admin/staff login | ❌ | ✅ | ✅ PROTECTED |
| `/api/auth/register` | POST | Guardian registration | ❌ | ✅ | ✅ PROTECTED |
| `/api/users` | POST | Create new user | ✅ JWT | ✅ | ✅ PROTECTED |
| `/api/users/:id` | PUT | Update user | ✅ JWT | ✅ | ✅ PROTECTED |
| `/api/users/:id` | DELETE | Delete user | ✅ JWT | ✅ | ✅ PROTECTED |
| `/api/users/:id/deactivate` | PUT | Deactivate user | ✅ JWT | ✅ | ⚠️ TODO |
| `/api/users/:id/activate` | PUT | Activate user | ✅ JWT | ✅ | ⚠️ TODO |
| `/api/users/:id/reset-password` | PUT | Reset password | ✅ JWT | ✅ | ⚠️ TODO |
| `/api/users/cache/clear` | POST | Clear cache | ❌ | ✅ | ⚠️ TODO |

**Legend:**
- ✅ PROTECTED = CSRF middleware applied
- ⚠️ TODO = Endpoint exists but CSRF not yet applied
- ❌ = Not required
- ✅ = Required

---

## How to Protect Additional Endpoints

### Step 1: Identify Mutation Endpoints

Find all POST/PUT/DELETE/PATCH endpoints:

```bash
grep -n "app\.\(post\|put\|delete\|patch\)" mock-user-service.js
```

### Step 2: Apply CSRF Middleware

**Before:**
```javascript
app.post('/api/users', authenticateToken, async (req, res) => {
  // ...
});
```

**After:**
```javascript
app.post('/api/users', csrfProtection, authenticateToken, async (req, res) => {
  //                     ^^^^^^^^^^^^^^^ ADD THIS
  // ...
});
```

**Important:** `csrfProtection` must come BEFORE `authenticateToken` in the middleware chain!

### Step 3: Test Protection

```bash
# 1. Get CSRF token
curl -c cookies.txt http://localhost:8082/api/auth/csrf-token

# 2. Extract token
TOKEN=$(cat cookies.txt | grep csrf_cookie | awk '{print $7}')

# 3. Test endpoint WITH token (should succeed)
curl -b cookies.txt -X POST http://localhost:8082/api/your-endpoint \
  -H "Content-Type: application/json" \
  -H "X-CSRF-Token: $TOKEN" \
  -H "Authorization: Bearer your-jwt-token" \
  -d '{"your":"data"}'

# 4. Test endpoint WITHOUT token (should fail with 403)
curl -X POST http://localhost:8082/api/your-endpoint \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-jwt-token" \
  -d '{"your":"data"}'
# Expected: 403 { "error": "CSRF token missing" }
```

### Step 4: Update Documentation

Add the endpoint to this file under "CSRF Protected Endpoints" with status ✅ PROTECTED.

---

## Exempt Endpoints (Never Need CSRF)

### Safe HTTP Methods (RFC 7231)

These methods are AUTOMATICALLY exempt by the `csrfProtection` middleware:

```javascript
if (['GET', 'HEAD', 'OPTIONS'].includes(method)) {
  return next(); // Skip CSRF validation
}
```

### Special Cases

1. **Webhook callbacks** - External services can't provide CSRF tokens
2. **Public read-only APIs** - No state change, no CSRF risk
3. **Health checks** - Must always be accessible
4. **CSRF token endpoint itself** - Can't require a token to get a token!

If you need to exempt a specific POST/PUT/DELETE endpoint, add it to an exclusion list:

```javascript
const CSRF_EXEMPT_PATHS = [
  '/api/webhooks/stripe',
  '/api/webhooks/external-service'
];

function csrfProtection(req, res, next) {
  const method = req.method.toUpperCase();

  // Skip safe methods
  if (['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    return next();
  }

  // Skip exempt paths
  if (CSRF_EXEMPT_PATHS.some(path => req.path.startsWith(path))) {
    console.log(`[CSRF] Exempt path: ${req.path}`);
    return next();
  }

  // ... rest of validation
}
```

---

## TODO: Apply CSRF to Remaining Endpoints

### Pending Endpoints (User Service)

```bash
# Find lines to update
grep -n "app.put('/api/users/:id/deactivate" mock-user-service.js
grep -n "app.put('/api/users/:id/activate" mock-user-service.js
grep -n "app.put('/api/users/:id/reset-password" mock-user-service.js
grep -n "app.post('/api/users/cache/clear" mock-user-service.js
```

Apply CSRF middleware to each:

```javascript
// Line ~1608
app.put('/api/users/:id/deactivate', csrfProtection, authenticateToken, (req, res) => {

// Line ~1629
app.put('/api/users/:id/activate', csrfProtection, authenticateToken, (req, res) => {

// Line ~1647
app.put('/api/users/:id/reset-password', csrfProtection, authenticateToken, (req, res) => {

// Line ~1768
app.post('/api/users/cache/clear', csrfProtection, (req, res) => {
```

### Other Services (Future Work)

1. **Application Service** (`mock-application-service.js`)
   - POST `/api/applications` - Create application
   - PUT `/api/applications/:id` - Update application
   - DELETE `/api/applications/:id` - Delete application
   - POST `/api/documents/:id/upload` - Upload documents

2. **Evaluation Service** (`mock-evaluation-service.js`)
   - POST `/api/interviews` - Schedule interview
   - PUT `/api/interviews/:id` - Update interview
   - DELETE `/api/interviews/:id` - Cancel interview
   - POST `/api/evaluations` - Create evaluation

3. **Guardian Service** (`mock-guardian-service.js`)
   - POST `/api/guardians/register` - Register guardian
   - PUT `/api/guardians/:id` - Update guardian
   - DELETE `/api/guardians/:id` - Delete guardian

4. **Dashboard Service** (`mock-dashboard-service.js`)
   - POST `/api/dashboard/cache/clear` - Clear cache

5. **Notification Service** (`mock-notification-service.js`)
   - POST `/api/notifications` - Send notification
   - POST `/api/email/send` - Send email

---

## Quick Command Reference

```bash
# Generate CSRF token
curl -c cookies.txt http://localhost:8082/api/auth/csrf-token

# Extract token from cookie
TOKEN=$(cat cookies.txt | grep csrf_cookie | awk '{print $7}')
echo "Token: $TOKEN"

# Use token in POST request
curl -b cookies.txt -X POST http://localhost:8082/api/endpoint \
  -H "Content-Type: application/json" \
  -H "X-CSRF-Token: $TOKEN" \
  -d '{"key":"value"}'

# Test CSRF protection (should fail with 403)
curl -X POST http://localhost:8082/api/endpoint \
  -H "Content-Type: application/json" \
  -d '{"key":"value"}'

# Run automated tests
./test-csrf.sh

# View CSRF logs
tail -f /tmp/user-service-test.log | grep CSRF
```

---

## Verification Checklist

Before deploying CSRF protection to production:

- [ ] All mutation endpoints identified
- [ ] CSRF middleware applied to all POST/PUT/DELETE/PATCH
- [ ] Safe methods (GET/HEAD/OPTIONS) exempt
- [ ] Token generation endpoint working
- [ ] Frontend axios interceptor active
- [ ] `withCredentials: true` in axios config
- [ ] NGINX CORS headers configured
- [ ] Automated tests passing (`./test-csrf.sh`)
- [ ] Manual curl tests passing
- [ ] Browser console tests passing
- [ ] Cookie attributes correct (httpOnly=false, sameSite=lax)
- [ ] Error responses clear and actionable
- [ ] Logging enabled for monitoring
- [ ] Documentation updated
- [ ] Rollback plan tested

---

## Maintenance

### Monthly Review

1. Check CSRF rejection logs for unusual patterns
2. Verify token expiration time still appropriate
3. Review exempt endpoints list
4. Update documentation with new endpoints
5. Run automated test suite

### On Adding New Endpoints

1. Identify if endpoint is a mutation (POST/PUT/DELETE/PATCH)
2. If yes, apply `csrfProtection` middleware
3. Test with and without CSRF token
4. Update this documentation
5. Update automated test suite if needed

### On Security Incidents

1. Review CSRF logs for attack patterns
2. Consider reducing token lifetime
3. Add rate limiting if needed
4. Investigate exempt endpoints
5. Review cookie attributes (consider sameSite=strict)

---

Last Updated: October 6, 2025
Version: 1.0.0
