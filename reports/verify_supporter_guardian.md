# Verification Commands - Supporter & Guardian Integration

## Overview
This document provides comprehensive verification commands to test the end-to-end integration of `supporter` (sostenedor) and `guardian` (apoderado) persistence in the admissions system.

## Prerequisites
- PostgreSQL database running: `Admisión_MTN_DB`
- Backend services running (mock-application-service.js on port 8083)
- NGINX gateway running on port 8080
- Valid JWT token for authentication

---

## 1. Database Verification

### Check Tables Exist
```bash
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c "
SELECT table_name
FROM information_schema.tables
WHERE table_name IN ('supporters', 'guardians')
ORDER BY table_name;
"
```

**Expected Output:**
```
 table_name
------------
 guardians
 supporters
(2 rows)
```

### Check Foreign Keys
```bash
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c "
SELECT
  tc.table_name,
  kcu.column_name,
  ccu.table_name AS foreign_table_name,
  ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_name = 'applications'
  AND kcu.column_name IN ('supporter_id', 'guardian_id');
"
```

**Expected Output:**
```
 table_name  | column_name  | foreign_table_name | foreign_column_name
-------------+--------------+--------------------+--------------------
 applications| supporter_id | supporters         | id
 applications| guardian_id  | guardians          | id
```

---

## 2. API Testing - Happy Path

### Generate Auth Token
First, login to get a valid JWT token:

```bash
curl -i -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "jorge.gangale@gmail.com",
    "password": "admin123"
  }'
```

Extract the `token` from the response and set it as environment variable:
```bash
export AUTH_TOKEN="your_jwt_token_here"
```

### Create Application with Supporter & Guardian (Happy Path)
```bash
curl -i -X POST http://localhost:8080/api/applications \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -d '{
    "firstName": "MARIA",
    "paternalLastName": "GONZALEZ",
    "maternalLastName": "PEREZ",
    "rut": "25.123.456-7",
    "birthDate": "2010-05-15",
    "grade": "1basico",
    "currentSchool": "Escuela San Pedro",
    "studentAddress": "Av. Principal 123",
    "studentEmail": "maria.gonzalez@gmail.com",
    "parent1Name": "CARLOS GONZALEZ",
    "parent1Rut": "15.111.222-3",
    "parent1Email": "carlos.gonzalez@gmail.com",
    "parent1Phone": "+56912345678",
    "parent1Profession": "Ingeniero",
    "parent2Name": "ANA PEREZ",
    "parent2Rut": "16.333.444-5",
    "parent2Email": "ana.perez@gmail.com",
    "parent2Phone": "+56987654321",
    "parent2Profession": "Profesora",
    "supporterName": "PEDRO GONZALEZ",
    "supporterRut": "12.555.666-7",
    "supporterEmail": "pedro.gonzalez@empresa.cl",
    "supporterPhone": "+56911111111",
    "supporterRelation": "PADRE",
    "guardianName": "ISABEL PEREZ",
    "guardianRut": "13.777.888-9",
    "guardianEmail": "isabel.perez@hogar.cl",
    "guardianPhone": "+56922222222",
    "guardianRelation": "MADRE",
    "applicationYear": 2026,
    "additionalNotes": "Test de verificación de supporter y guardian"
  }'
```

**Expected Response:** `201 Created` with JSON containing application `id`.

Save the application ID from response:
```bash
export APP_ID="<id_from_response>"
```

---

## 3. Database Verification After Creation

### Verify Supporter was saved
```bash
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c "
SELECT s.id, s.full_name, s.rut, s.email, s.phone, s.relationship
FROM supporters s
JOIN applications a ON a.supporter_id = s.id
WHERE a.id = $APP_ID;
"
```

**Expected Output:**
```
 id |    full_name    |       rut      |          email           |     phone      | relationship
----+-----------------+----------------+--------------------------+----------------+-------------
  1 | PEDRO GONZALEZ  | 12.555.666-7   | pedro.gonzalez@empresa.cl| +56911111111   | PADRE
```

### Verify Guardian was saved
```bash
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c "
SELECT g.id, g.full_name, g.rut, g.email, g.phone, g.relationship
FROM guardians g
JOIN applications a ON a.guardian_id = g.id
WHERE a.id = $APP_ID;
"
```

**Expected Output:**
```
 id |   full_name   |      rut       |        email         |    phone     | relationship
----+---------------+----------------+----------------------+--------------+-------------
  1 | ISABEL PEREZ  | 13.777.888-9   | isabel.perez@hogar.cl| +56922222222 | MADRE
```

### Verify Complete Application with JOIN
```bash
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c "
SELECT
    a.id,
    s.first_name || ' ' || s.paternal_last_name AS student_name,
    sup.full_name AS supporter_name,
    sup.relationship AS supporter_rel,
    gua.full_name AS guardian_name,
    gua.relationship AS guardian_rel
FROM applications a
JOIN students s ON s.id = a.student_id
LEFT JOIN supporters sup ON sup.id = a.supporter_id
LEFT JOIN guardians gua ON gua.id = a.guardian_id
WHERE a.id = $APP_ID;
"
```

**Expected Output:**
```
 id |  student_name   | supporter_name  | supporter_rel | guardian_name | guardian_rel
----+-----------------+-----------------+---------------+---------------+-------------
 30 | MARIA GONZALEZ  | PEDRO GONZALEZ  | PADRE         | ISABEL PEREZ  | MADRE
```

---

## 4. API Testing - Retrieve Application

### GET /api/applications/my-applications
```bash
curl -s -X GET http://localhost:8080/api/applications/my-applications \
  -H "Authorization: Bearer $AUTH_TOKEN" | jq '.[0] | {id, studentName, supporter, guardian}'
```

**Expected Output (formatted):**
```json
{
  "id": "30",
  "studentName": "MARIA GONZALEZ",
  "supporter": {
    "fullName": "PEDRO GONZALEZ",
    "rut": "12.555.666-7",
    "email": "pedro.gonzalez@empresa.cl",
    "phone": "+56911111111",
    "relationship": "PADRE"
  },
  "guardian": {
    "fullName": "ISABEL PEREZ",
    "rut": "13.777.888-9",
    "email": "isabel.perez@hogar.cl",
    "phone": "+56922222222",
    "relationship": "MADRE"
  }
}
```

---

## 5. Validation Testing - Missing Required Fields

### Test Missing Supporter Name
```bash
curl -i -X POST http://localhost:8080/api/applications \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -d '{
    "firstName": "JUAN",
    "paternalLastName": "PEREZ",
    "rut": "26.123.456-8",
    "birthDate": "2011-03-20",
    "grade": "1basico",
    "supporterName": "",
    "supporterRut": "14.111.222-3",
    "supporterEmail": "supporter@test.cl",
    "supporterPhone": "+56911111111",
    "supporterRelation": "PADRE",
    "guardianName": "MARIA LOPEZ",
    "guardianRut": "15.333.444-5",
    "guardianEmail": "maria@test.cl",
    "guardianPhone": "+56922222222",
    "guardianRelation": "MADRE",
    "applicationYear": 2026
  }'
```

**Expected Response:** `400 Bad Request` or `422 Unprocessable Entity` with validation error message.

### Test Missing Guardian Email
```bash
curl -i -X POST http://localhost:8080/api/applications \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -d '{
    "firstName": "PEDRO",
    "paternalLastName": "MARTINEZ",
    "rut": "27.123.456-9",
    "birthDate": "2012-07-10",
    "grade": "1basico",
    "supporterName": "LUIS MARTINEZ",
    "supporterRut": "16.111.222-4",
    "supporterEmail": "luis@test.cl",
    "supporterPhone": "+56933333333",
    "supporterRelation": "PADRE",
    "guardianName": "CARMEN SILVA",
    "guardianRut": "17.333.444-6",
    "guardianEmail": "",
    "guardianPhone": "+56944444444",
    "guardianRelation": "MADRE",
    "applicationYear": 2026
  }'
```

**Expected Response:** `400 Bad Request` with validation error for missing guardian email.

---

## 6. Frontend Testing (Manual)

### Steps:
1. Open browser: http://localhost:5173
2. Login with: jorge.gangale@gmail.com / admin123
3. Navigate to "Nueva Postulación"
4. Fill all required fields including:
   - Student information
   - Parent information
   - **Supporter (Sostenedor):** Name, RUT, Email, Phone, Relation
   - **Guardian (Apoderado):** Name, RUT, Email, Phone, Relation
5. Submit application
6. Navigate to "Mis Postulaciones"
7. Verify supporter and guardian information is displayed correctly

---

## 7. Data Cleanup (Optional)

### Delete Test Applications
```bash
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c "
DELETE FROM applications WHERE additional_notes LIKE '%Test de verificación%';
"
```

**Note:** This will CASCADE delete associated supporters and guardians due to FK constraints.

---

## 8. Checklist

Use this checklist to ensure all verification steps pass:

- [ ] Database tables `supporters` and `guardians` exist
- [ ] Foreign keys from `applications` to `supporters` and `guardians` are configured
- [ ] POST `/api/applications` successfully creates supporter and guardian records
- [ ] Supporter data is correctly saved to database with all 5 fields
- [ ] Guardian data is correctly saved to database with all 5 fields
- [ ] GET `/api/applications/my-applications` returns supporter and guardian in response
- [ ] Validation errors are returned when required fields are missing
- [ ] Frontend form allows entry of supporter and guardian information
- [ ] Frontend displays supporter and guardian correctly in "Mis Postulaciones"
- [ ] CASCADE delete works (deleting application removes supporter and guardian)

---

## 9. Troubleshooting

### Issue: 500 Error when creating application
**Check:** Service logs in `/tmp/application-service.log`
```bash
tail -f /tmp/application-service.log
```

### Issue: Supporter/Guardian not showing in response
**Check:** Database query to verify they were saved
```bash
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c "
SELECT a.id, a.supporter_id, a.guardian_id
FROM applications a
ORDER BY a.id DESC
LIMIT 5;
"
```

### Issue: Validation not working
**Check:** mock-application-service.js has been restarted with latest changes
```bash
pkill -f mock-application-service.js
cd "/Users/jorgegangale/Library/Mobile Documents/com~apple~CloudDocs/Proyectos/Admision_MTN/Admision_MTN_backend"
node mock-application-service.js > /tmp/application-service.log 2>&1 &
```

---

## Success Criteria

✅ **All tests pass** when:
1. Applications can be created with supporter and guardian
2. Data persists correctly in database
3. API responses include complete supporter/guardian information
4. Frontend correctly displays supporter/guardian data
5. Validation prevents incomplete submissions

---

**Document Version:** 1.0
**Last Updated:** 2025-10-01
**Author:** System Integration Team
