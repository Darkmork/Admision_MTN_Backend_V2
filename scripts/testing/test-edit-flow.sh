#!/bin/bash

echo "=== QA Test: Edit Application Flow ==="
echo ""

# Get a valid token
echo "1. Getting authentication token..."
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/apoderado/login \
  -H "Content-Type: application/json" \
  -d '{"email":"schweikart.cr@gmail.com","password":"password123"}' | jq -r '.token')

if [ "$TOKEN" = "null" ] || [ -z "$TOKEN" ]; then
  echo "❌ Failed to get token"
  exit 1
fi
echo "✅ Token obtained"

# Get application details
echo ""
echo "2. Getting application 38 details..."
APP_DATA=$(curl -s -X GET "http://localhost:8080/api/applications/38" \
  -H "Authorization: Bearer $TOKEN")

echo "$APP_DATA" | jq -r '.student.firstName' > /dev/null 2>&1
if [ $? -eq 0 ]; then
  echo "✅ Application retrieved successfully"
  echo "   Student: $(echo "$APP_DATA" | jq -r '.student.firstName') $(echo "$APP_DATA" | jq -r '.student.paternalLastName')"
else
  echo "❌ Failed to get application"
  exit 1
fi

# Get documents
echo ""
echo "3. Getting application documents..."
DOCS=$(curl -s -X GET "http://localhost:8080/api/applications/38/documents" \
  -H "Authorization: Bearer $TOKEN")

DOC_COUNT=$(echo "$DOCS" | jq -r 'if type=="array" then length elif .documents then (.documents | length) else 0 end')
echo "✅ Documents retrieved: $DOC_COUNT documents"

# Test PUT update
echo ""
echo "4. Testing PUT update (changing student email)..."
UPDATE_RESPONSE=$(curl -s -X PUT "http://localhost:8080/api/applications/38" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "student": {
      "firstName": "RODRIGO",
      "paternalLastName": "SCHWEIKART",
      "maternalLastName": "SALAS",
      "rut": "18.246.018-4",
      "birthDate": "2009-08-25",
      "email": "rodrigo.test@example.com",
      "address": "LYON 3443 DEPTO 407, ÑUÑOA,",
      "gradeApplied": "2medio",
      "currentSchool": "COLEGIO LOS AROMOS DE QUILPUE",
      "additionalNotes": "Test update"
    },
    "father": {
      "fullName": "CRISTIAN SCHWEIKART SALAS",
      "rut": "18.879.517-K",
      "email": "schweikart.cr@gmail.com",
      "phone": "+56995441213",
      "address": "LYON 3443",
      "profession": "INGENIERO"
    },
    "mother": {
      "fullName": "VALENTINA GONZALEZ",
      "rut": "9.764.485-3",
      "email": "valentina.gonzalez@123.cl",
      "phone": "+56995441213",
      "address": "AV. VITACURA 234",
      "profession": "SICOLOGA"
    },
    "supporter": {
      "fullName": "CRISTIAN SCHWEIKART SALAS",
      "rut": "18.879.517-K",
      "email": "schweikart.cr@gmail.com",
      "phone": "+56995441213",
      "relationship": "PADRE"
    },
    "guardian": {
      "fullName": "VALENTINA GONZALEZ",
      "rut": "9.764.485-3",
      "email": "valentina.gonzalez@123.cl",
      "phone": "+56995441213",
      "relationship": "MADRE"
    },
    "schoolApplied": "MONTE_TABOR"
  }')

echo "$UPDATE_RESPONSE" | jq -r '.success' > /dev/null 2>&1
if [ $? -eq 0 ]; then
  SUCCESS=$(echo "$UPDATE_RESPONSE" | jq -r '.success')
  if [ "$SUCCESS" = "true" ]; then
    echo "✅ PUT update successful"
  else
    echo "❌ PUT failed: $(echo "$UPDATE_RESPONSE" | jq -r '.error // .message')"
    exit 1
  fi
else
  echo "❌ Invalid response format"
  echo "$UPDATE_RESPONSE"
  exit 1
fi

# Verify the update
echo ""
echo "5. Verifying update was saved..."
UPDATED_APP=$(curl -s -X GET "http://localhost:8080/api/applications/38" \
  -H "Authorization: Bearer $TOKEN")

STUDENT_EMAIL=$(echo "$UPDATED_APP" | jq -r '.student.email')
if [ "$STUDENT_EMAIL" = "rodrigo.test@example.com" ]; then
  echo "✅ Update verified - email changed successfully"
else
  echo "❌ Update not reflected in database"
  echo "   Expected: rodrigo.test@example.com"
  echo "   Got: $STUDENT_EMAIL"
  exit 1
fi

echo ""
echo "=== QA Test Summary ==="
echo "✅ All tests passed!"
echo "   - Authentication: OK"
echo "   - Get application: OK"
echo "   - Get documents: OK ($DOC_COUNT docs)"
echo "   - PUT update: OK"
echo "   - Verification: OK"
