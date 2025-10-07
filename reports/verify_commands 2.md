# Verification Commands for Frontend-Backend Contract Fixes

## Prerequisites

```bash
# Ensure all services are running
cd /Users/jorgegangale/Library/Mobile\ Documents/com~apple~CloudDocs/Proyectos/Admision_MTN/Admision_MTN_backend

# Start services
./start-microservices-gateway.sh

# Verify services are up
curl http://localhost:8080/gateway/status
curl http://localhost:8082/health  # User service
curl http://localhost:8083/health  # Application service
curl http://localhost:8085/health  # Notification service
```

---

## 1. Apply Database Migration (Critical Fix)

```bash
# Navigate to database directory
cd /Users/jorgegangale/Library/Mobile\ Documents/com~apple~CloudDocs/Proyectos/Admision_MTN/Admision_MTN_backend/reports

# Apply migration
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -f 001_add_supporter_guardian_tables.sql

# Expected output:
# CREATE TABLE
# CREATE TABLE
# CREATE INDEX
# CREATE INDEX
# CREATE INDEX
# CREATE INDEX
# CREATE INDEX
# CREATE INDEX
# NOTICE: Table "supporters" created successfully
# NOTICE: Table "guardians" created successfully

# Verify tables exist
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c "\dt supporters"
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c "\dt guardians"

# Verify indexes
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c "\di idx_supporters_*"
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c "\di idx_guardians_*"
```

---

## 2. Test Application Submission (Complete Flow)

### 2.1 Get Authentication Token

```bash
# Login as test user
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "jorge.gangale@mtn.cl",
    "password": "admin123"
  }' | jq .

# Save token from response
export AUTH_TOKEN="YOUR_JWT_TOKEN_HERE"
```

### 2.2 Submit Complete Application

```bash
# Submit application with ALL fields including supporter and guardian
curl -X POST http://localhost:8080/api/applications \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "SOFIA VALENTINA",
    "paternalLastName": "GONZALEZ",
    "maternalLastName": "MARTINEZ",
    "rut": "25123456-7",
    "birthDate": "2018-03-15",
    "studentEmail": "sofia.gonzalez@test.com",
    "studentAddress": "AV. LAS CONDES 12345, LAS CONDES, SANTIAGO",
    "grade": "1basico",
    "schoolApplied": "MONTE_TABOR",
    "currentSchool": "JARDIN INFANTIL LOS ANGELITOS",
    "additionalNotes": "ESTUDIANTE CON EXPERIENCIA PREVIA EN ACTIVIDADES MUSICALES",
    "applicationYear": "2026",
    "parent1Name": "CARLOS ALBERTO GONZALEZ PEREZ",
    "parent1Rut": "16123456-8",
    "parent1Email": "carlos.gonzalez@test.com",
    "parent1Phone": "+56912345678",
    "parent1Address": "AV. LAS CONDES 12345, LAS CONDES, SANTIAGO",
    "parent1Profession": "INGENIERO COMERCIAL",
    "parent2Name": "MARIA ELENA MARTINEZ LOPEZ",
    "parent2Rut": "17234567-9",
    "parent2Email": "maria.martinez@test.com",
    "parent2Phone": "+56987654321",
    "parent2Address": "AV. LAS CONDES 12345, LAS CONDES, SANTIAGO",
    "parent2Profession": "PROFESORA DE EDUCACION BASICA",
    "supporterName": "CARLOS ALBERTO GONZALEZ PEREZ",
    "supporterRut": "16123456-8",
    "supporterEmail": "carlos.gonzalez@test.com",
    "supporterPhone": "+56912345678",
    "supporterRelation": "padre",
    "guardianName": "MARIA ELENA MARTINEZ LOPEZ",
    "guardianRut": "17234567-9",
    "guardianEmail": "maria.martinez@test.com",
    "guardianPhone": "+56987654321",
    "guardianRelation": "madre"
  }' | jq .

# Expected response:
# {
#   "success": true,
#   "message": "Postulación creada exitosamente",
#   "id": 123,
#   "studentName": "SOFIA VALENTINA GONZALEZ",
#   "grade": "1basico",
#   "status": "PENDIENTE",
#   "submissionDate": "2025-10-01T...",
#   "applicantEmail": "carlos.gonzalez@test.com"
# }

# Save application ID
export APP_ID=123  # Use the ID from response
```

### 2.3 Verify Supporter Data Saved

```bash
# Query supporter table
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c \
  "SELECT
     s.id,
     s.application_id,
     s.full_name,
     s.rut,
     s.email,
     s.phone,
     s.relationship,
     s.created_at
   FROM supporters s
   WHERE s.application_id = $APP_ID;"

# Expected output:
# id | application_id | full_name | rut | email | phone | relationship | created_at
# ---+----------------+-----------+-----+-------+-------+--------------+------------
# 1  | 123            | CARLOS... | ... | ...   | ...   | padre        | 2025-10-01...
```

### 2.4 Verify Guardian Data Saved

```bash
# Query guardian table
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c \
  "SELECT
     g.id,
     g.application_id,
     g.full_name,
     g.rut,
     g.email,
     g.phone,
     g.relationship,
     g.created_at
   FROM guardians g
   WHERE g.application_id = $APP_ID;"

# Expected output:
# id | application_id | full_name | rut | email | phone | relationship | created_at
# ---+----------------+-----------+-----+-------+-------+--------------+------------
# 1  | 123            | MARIA...  | ... | ...   | ...   | madre        | 2025-10-01...
```

### 2.5 Verify Complete Application Data

```bash
# Get complete application with all relations
curl -X GET "http://localhost:8080/api/applications/$APP_ID" \
  -H "Authorization: Bearer $AUTH_TOKEN" | jq .

# Expected response should include:
# {
#   "success": true,
#   "data": {
#     "id": 123,
#     "status": "PENDING",
#     "student": { ... },
#     "father": { ... },
#     "mother": { ... },
#     "supporter": {  # NEW - should now be present
#       "id": 1,
#       "fullName": "CARLOS ALBERTO GONZALEZ PEREZ",
#       "rut": "16123456-8",
#       "email": "carlos.gonzalez@test.com",
#       "phone": "+56912345678",
#       "relationship": "padre"
#     },
#     "guardian": {  # NEW - should now be present
#       "id": 1,
#       "fullName": "MARIA ELENA MARTINEZ LOPEZ",
#       "rut": "17234567-9",
#       "email": "maria.martinez@test.com",
#       "phone": "+56987654321",
#       "relationship": "madre"
#     }
#   }
# }
```

---

## 3. Test Validation (Should Fail)

### 3.1 Test Missing Required Fields

```bash
# Attempt submission with incomplete data (missing parent info)
curl -X POST http://localhost:8080/api/applications \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "INCOMPLETE",
    "paternalLastName": "TEST",
    "rut": "12345678-9",
    "birthDate": "2018-01-01",
    "grade": "1basico",
    "schoolApplied": "MONTE_TABOR",
    "studentAddress": "TEST ADDRESS 123"
  }' | jq .

# Expected response: 400 Bad Request
# {
#   "success": false,
#   "error": "Errores de validación",
#   "details": [
#     "Nombre del padre es requerido",
#     "RUT del padre es requerido y debe ser válido",
#     "Email del padre es requerido y debe ser válido",
#     "Teléfono del padre es requerido y debe ser válido",
#     "Dirección del padre es requerida",
#     "Profesión del padre es requerida",
#     "Nombre de la madre es requerido",
#     "RUT de la madre es requerido y debe ser válido",
#     "Email de la madre es requerido y debe ser válido",
#     "Teléfono de la madre es requerido y debe ser válido",
#     "Dirección de la madre es requerida",
#     "Profesión de la madre es requerida"
#   ]
# }
```

### 3.2 Test Invalid RUT Format

```bash
# Attempt submission with invalid RUT
curl -X POST http://localhost:8080/api/applications \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "TEST",
    "paternalLastName": "INVALID",
    "maternalLastName": "RUT",
    "rut": "12345678-0",
    "birthDate": "2018-01-01",
    "grade": "1basico",
    "schoolApplied": "MONTE_TABOR",
    "studentAddress": "TEST ADDRESS",
    "parent1Name": "PARENT ONE",
    "parent1Rut": "INVALID-RUT",
    "parent1Email": "parent1@test.com",
    "parent1Phone": "+56912345678",
    "parent1Address": "ADDRESS 123",
    "parent1Profession": "ENGINEER",
    "parent2Name": "PARENT TWO",
    "parent2Rut": "22222222-2",
    "parent2Email": "parent2@test.com",
    "parent2Phone": "+56987654321",
    "parent2Address": "ADDRESS 456",
    "parent2Profession": "TEACHER"
  }' | jq .

# Expected response: 400 Bad Request
# {
#   "success": false,
#   "error": "Errores de validación",
#   "details": [
#     "RUT del estudiante inválido",
#     "RUT del padre inválido"
#   ]
# }
```

### 3.3 Test Short Parent Name (Length Validation)

```bash
# Attempt submission with 2-character parent name (should fail with backend validation)
curl -X POST http://localhost:8080/api/applications \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "TEST",
    "paternalLastName": "STUDENT",
    "maternalLastName": "NAME",
    "rut": "12345678-9",
    "birthDate": "2018-01-01",
    "grade": "1basico",
    "schoolApplied": "MONTE_TABOR",
    "studentAddress": "ADDRESS 123",
    "parent1Name": "AB",
    "parent1Rut": "11111111-1",
    "parent1Email": "parent1@test.com",
    "parent1Phone": "+56912345678",
    "parent1Address": "ADDRESS 123",
    "parent1Profession": "ENG",
    "parent2Name": "COMPLETE MOTHER NAME",
    "parent2Rut": "22222222-2",
    "parent2Email": "parent2@test.com",
    "parent2Phone": "+56987654321",
    "parent2Address": "ADDRESS 456",
    "parent2Profession": "TEACHER"
  }' | jq .

# Expected response: 400 Bad Request
# {
#   "success": false,
#   "error": "Errores de validación",
#   "details": [
#     "Nombre completo del padre debe tener al menos 3 caracteres"
#   ]
# }
```

---

## 4. Test Email Verification Flow

### 4.1 Send Verification Code

```bash
# Send verification code to test email
curl -X POST http://localhost:8080/api/email/send-verification \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "type": "REGISTRATION"
  }' | jq .

# Expected response:
# {
#   "success": true,
#   "message": "Email de verificación enviado exitosamente",
#   "email": "test@example.com",
#   "messageId": "...",
#   "verificationCode": "123456",  # For development only
#   "timestamp": "2025-10-01T..."
# }

# Save the verification code from console output or response
export VERIFICATION_CODE="123456"
```

### 4.2 Verify Code (Valid)

```bash
# Verify the code
curl -X POST http://localhost:8080/api/email/verify-code \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "code": "'"$VERIFICATION_CODE"'"
  }' | jq .

# Expected response:
# {
#   "success": true,
#   "message": "Código verificado exitosamente",
#   "isValid": true
# }
```

### 4.3 Verify Code (Invalid)

```bash
# Attempt verification with wrong code
curl -X POST http://localhost:8080/api/email/verify-code \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "code": "000000"
  }' | jq .

# Expected response:
# {
#   "success": false,
#   "message": "Código de verificación incorrecto",
#   "isValid": false,
#   "error": "INVALID_CODE",
#   "attemptsRemaining": 2
# }
```

---

## 5. Test Authentication Flow

### 5.1 Register New User

```bash
# Register new apoderado user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "NUEVO",
    "lastName": "APODERADO",
    "email": "nuevo.apoderado@test.com",
    "password": "password123",
    "rut": "19876543-2",
    "phone": "+56999888777",
    "address": "NUEVA DIRECCION 789",
    "profession": "MEDICO"
  }' | jq .

# Expected response:
# {
#   "success": true,
#   "message": "Usuario registrado exitosamente",
#   "token": "eyJhbGciOiJIUzUxMiJ9...",
#   "email": "nuevo.apoderado@test.com",
#   "firstName": "NUEVO",
#   "lastName": "APODERADO",
#   "role": "APODERADO"
# }
```

### 5.2 Login with New User

```bash
# Login with newly registered user
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "nuevo.apoderado@test.com",
    "password": "password123"
  }' | jq .

# Expected response:
# {
#   "success": true,
#   "message": "Login exitoso",
#   "token": "eyJhbGciOiJIUzUxMiJ9...",
#   "email": "nuevo.apoderado@test.com",
#   "firstName": "NUEVO",
#   "lastName": "APODERADO",
#   "role": "APODERADO"
# }
```

### 5.3 Test Duplicate Email (Should Fail)

```bash
# Attempt to register with existing email
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "DUPLICATE",
    "lastName": "USER",
    "email": "nuevo.apoderado@test.com",
    "password": "password123",
    "rut": "18765432-1",
    "phone": "+56988888888"
  }' | jq .

# Expected response: 409 Conflict
# {
#   "success": false,
#   "message": "Ya existe un usuario con este email o RUT",
#   "error": "DUPLICATE_USER"
# }
```

---

## 6. Test Document Upload

### 6.1 Upload Document to Application

```bash
# Create a test PDF file
echo "This is a test document" > test_document.txt

# Upload document
curl -X POST http://localhost:8080/api/applications/documents \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -F "file=@test_document.txt" \
  -F "applicationId=$APP_ID" \
  -F "documentType=BIRTH_CERTIFICATE" \
  -F "isRequired=true" | jq .

# Expected response:
# {
#   "success": true,
#   "message": "Documento subido exitosamente",
#   "document": {
#     "id": 1,
#     "applicationId": 123,
#     "documentType": "BIRTH_CERTIFICATE",
#     "fileName": "123_1727884800000_test_document.txt",
#     "originalName": "test_document.txt",
#     "filePath": "/path/to/uploads/...",
#     "fileSize": 24,
#     "contentType": "text/plain",
#     "isRequired": true,
#     "uploadDate": "2025-10-01T..."
#   }
# }
```

### 6.2 Get Application Documents

```bash
# Retrieve all documents for application
curl -X GET "http://localhost:8080/api/applications/$APP_ID/documents" \
  -H "Authorization: Bearer $AUTH_TOKEN" | jq .

# Expected response:
# {
#   "success": true,
#   "documents": [
#     {
#       "id": 1,
#       "applicationId": 123,
#       "documentType": "BIRTH_CERTIFICATE",
#       "fileName": "123_1727884800000_test_document.txt",
#       "originalName": "test_document.txt",
#       "filePath": "/path/to/uploads/...",
#       "fileSize": 24,
#       "contentType": "text/plain",
#       "isRequired": true,
#       "uploadDate": "2025-10-01T..."
#     }
#   ],
#   "count": 1
# }
```

---

## 7. Frontend Testing (Manual)

### 7.1 Test Application Form Flow

```bash
# 1. Start frontend (in separate terminal)
cd /Users/jorgegangale/Library/Mobile\ Documents/com~apple~CloudDocs/Proyectos/Admision_MTN/Admision_MTN_front
npm run dev

# 2. Open browser
open http://localhost:5173/postular

# 3. Manual test steps:
# - Click "Crear Cuenta y Postular"
# - Fill in registration form with all fields
# - Verify email with code (check backend console for code)
# - Complete Step 1: Student Information
# - Complete Step 2: Parent Information
# - Complete Step 3: Supporter Information
# - Complete Step 4: Guardian Information
# - Complete Step 5: Document Upload (optional)
# - Submit application
# - Verify success message appears
# - Check browser console for no errors
# - Verify application appears in dashboard
```

### 7.2 Verify Data in Dashboard

```bash
# Navigate to apoderado dashboard
open http://localhost:5173/dashboard-apoderado

# Manual verification:
# - Application appears in list
# - Student name is correct
# - Status shows "PENDING"
# - Click on application to view details
# - Verify ALL information is displayed including supporter and guardian
```

---

## 8. Database Verification Queries

### 8.1 Count Records

```bash
# Count total applications
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c \
  "SELECT COUNT(*) as total_applications FROM applications;"

# Count supporters
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c \
  "SELECT COUNT(*) as total_supporters FROM supporters;"

# Count guardians
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c \
  "SELECT COUNT(*) as total_guardians FROM guardians;"
```

### 8.2 Verify Data Integrity

```bash
# Check for applications without supporters (should be 0 after fix)
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c \
  "SELECT
     a.id,
     a.status,
     s.id as student_id,
     sup.id as supporter_id,
     g.id as guardian_id
   FROM applications a
   LEFT JOIN students s ON s.id = a.student_id
   LEFT JOIN supporters sup ON sup.application_id = a.id
   LEFT JOIN guardians g ON g.application_id = a.id
   WHERE sup.id IS NULL OR g.id IS NULL;"

# If any rows returned, supporter/guardian data is missing
```

### 8.3 Complete Application Report

```bash
# Generate complete report of latest application
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c \
  "SELECT
     a.id as app_id,
     a.status,
     a.submission_date,
     s.first_name || ' ' || s.paternal_last_name as student,
     s.grade_applied as grade,
     f.full_name as father,
     m.full_name as mother,
     sup.full_name as supporter,
     sup.relationship as supporter_relation,
     g.full_name as guardian,
     g.relationship as guardian_relation
   FROM applications a
   LEFT JOIN students s ON s.id = a.student_id
   LEFT JOIN parents f ON f.id = a.father_id
   LEFT JOIN parents m ON m.id = a.mother_id
   LEFT JOIN supporters sup ON sup.application_id = a.id
   LEFT JOIN guardians g ON g.application_id = a.id
   ORDER BY a.id DESC
   LIMIT 1;"
```

---

## 9. Cleanup Test Data

```bash
# Delete test application and related data
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c \
  "DELETE FROM applications WHERE id = $APP_ID;"

# Verify cascade delete worked
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c \
  "SELECT COUNT(*) FROM supporters WHERE application_id = $APP_ID;"
# Expected: 0

PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c \
  "SELECT COUNT(*) FROM guardians WHERE application_id = $APP_ID;"
# Expected: 0

# Delete test user
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c \
  "DELETE FROM users WHERE email = 'nuevo.apoderado@test.com';"

# Clean up test files
rm -f test_document.txt
```

---

## Summary Checklist

- [ ] Database migration applied successfully
- [ ] Supporters table created with indexes
- [ ] Guardians table created with indexes
- [ ] Application submission saves supporter data
- [ ] Application submission saves guardian data
- [ ] GET application endpoint returns supporter data
- [ ] GET application endpoint returns guardian data
- [ ] Validation errors returned for missing required fields
- [ ] Validation errors returned for invalid RUT format
- [ ] Validation errors returned for short names (min 3 chars)
- [ ] Email verification code sent successfully
- [ ] Email verification code validated correctly
- [ ] User registration works with all fields
- [ ] User login returns correct JWT token
- [ ] Document upload saves to database
- [ ] Document retrieval returns correct data
- [ ] Frontend form submission works end-to-end
- [ ] Dashboard displays complete application data
- [ ] No console errors in browser or backend
- [ ] All cascade deletes work correctly

**All tests passed**: System ready for production
**Any test failed**: Review error messages and fix before deploying
