# Guardian Validation Integration Guide

## Overview
This document shows how to integrate guardian validation into `mock-application-service.js` to prevent orphaned applications.

## Option 1: Add Middleware (Recommended)

### Step 1: Import the validation middleware
Add at the top of `mock-application-service.js` (after other requires):

```javascript
const { validateGuardianExists } = require('./guardian-validation-middleware');
```

### Step 2: Add middleware to POST /api/applications route
Modify line 1735 to include the middleware:

```javascript
// BEFORE:
app.post('/api/applications', authenticateToken, validateApplicationInput, async (req, res) => {

// AFTER:
app.post('/api/applications', authenticateToken, validateApplicationInput, validateGuardianExists, async (req, res) => {
```

### Step 3: Update guardian creation logic
Replace lines 1840-1859 with:

```javascript
// Guardian validation middleware has already verified/created guardian
// Use guardian_id from request body (set by middleware)
const guardianId = req.body.guardian_id || null;

// If guardianId is still null, this is an error (should be caught by middleware)
if (!guardianId) {
  throw new Error('guardian_id es requerido pero no fue proporcionado por el middleware');
}

console.log(`[Application Service] Using guardian_id=${guardianId} for application`);
```

## Option 2: Inline Validation (Alternative)

If you prefer not to use middleware, add this validation before line 1840:

```javascript
// ===== GUARDIAN VALIDATION (INLINE) =====
// Validate guardian exists or create one
let guardianId = null;

// Check if guardian_id provided
if (body.guardian_id) {
  // Verify guardian exists
  const guardianCheckQuery = `SELECT id FROM guardians WHERE id = $1`;
  const guardianCheck = await client.query(guardianCheckQuery, [body.guardian_id]);

  if (guardianCheck.rows.length === 0) {
    throw new Error(`Guardian con ID ${body.guardian_id} no existe`);
  }

  guardianId = body.guardian_id;
  console.log(`[Application] Using existing guardian_id=${guardianId}`);

} else if (body.guardianName && body.guardianRut && body.guardianEmail) {
  // Create new guardian
  const guardianQuery = `
    INSERT INTO guardians (
      full_name, rut, email, phone, relationship, created_at, updated_at
    ) VALUES ($1, $2, $3, $4, $5, NOW(), NOW())
    RETURNING id
  `;

  const guardianResult = await queryWithCircuitBreaker.fire(client, guardianQuery, [
    body.guardianName,
    body.guardianRut,
    body.guardianEmail,
    body.guardianPhone || '',
    body.guardianRelation || 'OTRO'
  ]);

  guardianId = guardianResult.rows[0].id;
  console.log(`[Application] Created new guardian_id=${guardianId}`);

} else {
  // No guardian data provided - ERROR
  await client.query('ROLLBACK');
  return res.status(400).json({
    success: false,
    error: 'Apoderado requerido',
    details: 'Debe proporcionar guardian_id o los datos completos del apoderado',
    errorCode: 'GUARDIAN_REQUIRED'
  });
}

// Ensure guardianId is not null before proceeding
if (!guardianId) {
  throw new Error('guardian_id no pudo ser establecido');
}
// ===== END GUARDIAN VALIDATION =====
```

Then replace line 1883 to use the validated `guardianId`:

```javascript
// BEFORE:
guardianId,

// AFTER (no change needed - variable is already set above):
guardianId,
```

## Frontend Contract Updates

### Current Request Body
```typescript
interface ApplicationCreateRequest {
  // Student data
  firstName: string;
  paternalLastName: string;
  maternalLastName: string;
  rut: string;
  birthDate: string;
  grade: string;
  currentSchool?: string;
  studentAddress?: string;
  studentEmail?: string;

  // Parent data (optional)
  parent1Name?: string;
  parent1Rut?: string;
  parent1Email?: string;
  parent1Phone?: string;

  parent2Name?: string;
  parent2Rut?: string;
  parent2Email?: string;
  parent2Phone?: string;

  // Supporter data (optional)
  supporterName?: string;
  supporterRut?: string;
  supporterEmail?: string;
  supporterPhone?: string;

  // Guardian data (REQUIRED - one of these options)
  guardian_id?: number;  // Option 1: Existing guardian ID

  // Option 2: Create new guardian
  guardianName?: string;       // Required if no guardian_id
  guardianRut?: string;        // Required if no guardian_id
  guardianEmail?: string;      // Required if no guardian_id
  guardianPhone?: string;      // Required if no guardian_id
  guardianRelation?: string;   // Required if no guardian_id (PADRE/MADRE/ABUELO/TIO/HERMANO/TUTOR/OTRO)

  // Application metadata
  applicationYear: number;
  additionalNotes?: string;
}
```

### New Error Responses

#### Error: Guardian Required (400)
```json
{
  "success": false,
  "error": "Apoderado requerido",
  "details": "Debe proporcionar guardian_id o los datos completos del apoderado",
  "errorCode": "GUARDIAN_REQUIRED",
  "requiredOptions": [
    {
      "option": "guardian_id",
      "description": "ID de apoderado existente"
    },
    {
      "option": "guardianData",
      "description": "Datos completos de nuevo apoderado",
      "fields": ["guardianName", "guardianRut", "guardianEmail", "guardianPhone", "guardianRelation"]
    }
  ]
}
```

#### Error: Guardian Not Found (400)
```json
{
  "success": false,
  "error": "Guardian inválido",
  "details": "El apoderado con ID 999 no existe en el sistema",
  "errorCode": "GUARDIAN_NOT_FOUND"
}
```

#### Error: Guardian Incomplete Data (400)
```json
{
  "success": false,
  "error": "Datos de apoderado incompletos",
  "details": "Para crear un apoderado se requiere: nombre, RUT, email, teléfono y relación",
  "errorCode": "GUARDIAN_INCOMPLETE_DATA",
  "requiredFields": ["guardianName", "guardianRut", "guardianEmail", "guardianPhone", "guardianRelation"]
}
```

#### Error: Invalid Relationship (400)
```json
{
  "success": false,
  "error": "Relación de apoderado inválida",
  "details": "La relación debe ser una de: PADRE, MADRE, ABUELO, TIO, HERMANO, TUTOR, OTRO",
  "errorCode": "GUARDIAN_INVALID_RELATIONSHIP",
  "validOptions": ["PADRE", "MADRE", "ABUELO", "TIO", "HERMANO", "TUTOR", "OTRO"]
}
```

### Success Response
```json
{
  "success": true,
  "data": {
    "id": 43,
    "student_id": 41,
    "guardian_id": 37,
    "status": "PENDING",
    "application_year": 2026,
    "created_at": "2025-10-06T12:00:00.000Z"
  },
  "guardian": {
    "id": 37,
    "created": true
  }
}
```

## Testing Examples

### Test 1: Create application with existing guardian
```bash
curl -X POST http://localhost:8080/api/applications \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "firstName": "Juan",
    "paternalLastName": "Pérez",
    "maternalLastName": "González",
    "rut": "12345678-9",
    "birthDate": "2015-05-10",
    "grade": "1° Básico",
    "guardian_id": 17,
    "applicationYear": 2026
  }'
```

### Test 2: Create application with new guardian
```bash
curl -X POST http://localhost:8080/api/applications \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "firstName": "María",
    "paternalLastName": "López",
    "maternalLastName": "Silva",
    "rut": "98765432-1",
    "birthDate": "2016-03-20",
    "grade": "Kinder",
    "guardianName": "Carmen López Vega",
    "guardianRut": "13579246-8",
    "guardianEmail": "carmen.lopez@email.com",
    "guardianPhone": "+56912345678",
    "guardianRelation": "MADRE",
    "applicationYear": 2026
  }'
```

### Test 3: Invalid - No guardian data (should return 400)
```bash
curl -X POST http://localhost:8080/api/applications \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "firstName": "Pedro",
    "paternalLastName": "Rojas",
    "maternalLastName": "Castro",
    "rut": "11111111-1",
    "birthDate": "2017-08-15",
    "grade": "Pre-Kinder",
    "applicationYear": 2026
  }'
```

### Test 4: Invalid - Guardian not found (should return 400)
```bash
curl -X POST http://localhost:8080/api/applications \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "firstName": "Ana",
    "paternalLastName": "Muñoz",
    "maternalLastName": "Vega",
    "rut": "22222222-2",
    "birthDate": "2018-01-10",
    "grade": "Pre-Kinder",
    "guardian_id": 99999,
    "applicationYear": 2026
  }'
```

## Integration Checklist

- [ ] Install validation middleware file (`guardian-validation-middleware.js`)
- [ ] Import middleware in `mock-application-service.js`
- [ ] Add middleware to POST /api/applications route
- [ ] Update guardian creation logic to use validated guardian_id
- [ ] Test with existing guardian_id
- [ ] Test with new guardian data
- [ ] Test error cases (no guardian, invalid guardian_id, incomplete data)
- [ ] Update frontend to handle new error codes
- [ ] Update API documentation with new contract
- [ ] Verify no orphaned applications can be created

## Rollback Plan

If integration causes issues:

1. Remove middleware from route:
```javascript
// Revert to:
app.post('/api/applications', authenticateToken, validateApplicationInput, async (req, res) => {
```

2. Keep original guardian creation logic (lines 1840-1859)

3. Delete or comment out middleware import

4. Service will return to previous behavior (allowing NULL guardian_id)
