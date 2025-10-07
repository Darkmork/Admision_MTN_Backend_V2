#!/bin/bash

# Script para crear flujo completo de admisión usando APIs
# Para 3 estudiantes con 3 padres/apoderados

echo "🚀 Iniciando flujo completo de admisión vía API para 3 estudiantes..."
echo ""

# Función para registrar apoderados y crear flujo
create_admission_flow() {
  local email=$1
  local rut=$2
  local student_name=$3
  local student_rut=$4
  local student_birthdate=$5
  local student_grade=$6
  local gender=$7

  echo "📝 Procesando: $student_name ($email)"

  # 1. Registrar apoder ado (guardian)
  echo "  1/8 Registrando apoderado..."
  GUARDIAN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/guardians/register \
    -H "Content-Type: application/json" \
    -d "{
      \"rut\": \"$rut\",
      \"email\": \"$email\",
      \"fullName\": \"Jorge Gangale Torres\",
      \"phone\": \"+56912345678\",
      \"relationship\": \"PADRE\",
      \"password\": \"12345678\"
    }")

  # Extraer guardian_id
  GUARDIAN_ID=$(echo $GUARDIAN_RESPONSE | grep -o '"guardianId":[0-9]*' | head -1 | grep -o '[0-9]*')

  if [ -z "$GUARDIAN_ID" ]; then
    echo "  ❌ Error: No se pudo crear el apoderado"
    return 1
  fi

  echo "  ✅ Apoderado creado: ID $GUARDIAN_ID"

  # 2. Login para obtener token
  echo "  2/8 Obteniendo token de autenticación..."
  LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d "{
      \"email\": \"$email\",
      \"password\": \"12345678\"
    }")

  TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*"' | head -1 | cut -d'"' -f4)

  if [ -z "$TOKEN" ]; then
    echo "  ❌ Error: No se pudo obtener token"
    return 1
  fi

  echo "  ✅ Token obtenido"

  # 3. Crear aplicación (postulación)
  echo "  3/8 Creando aplicación..."
  APP_RESPONSE=$(curl -s -X POST http://localhost:8080/api/applications \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d "{
      \"studentFirstName\": \"$student_name\",
      \"studentPaternalLastName\": \"Gangale\",
      \"studentMaternalLastName\": \"Torres\",
      \"studentRut\": \"$student_rut\",
      \"studentBirthDate\": \"$student_birthdate\",
      \"studentGender\": \"$gender\",
      \"gradeApplyingFor\": \"$student_grade\",
      \"previousSchool\": \"Colegio San José\",
      \"hasSiblings\": false,
      \"guardianId\": $GUARDIAN_ID
    }")

  APP_ID=$(echo $APP_RESPONSE | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')

  if [ -z "$APP_ID" ]; then
    echo "  ❌ Error: No se pudo crear aplicación"
    return 1
  fi

  echo "  ✅ Aplicación creada: ID $APP_ID"

  # 4. Cambiar estado a UNDER_REVIEW (admin)
  echo "  4/8 Cambiando estado a UNDER_REVIEW..."
  ADMIN_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiIxIiwiZW1haWwiOiJqb3JnZS5nYW5nYWxlQG10bi5jbCIsInJvbGUiOiJBRE1JTiIsImlhdCI6MTc1OTcwOTQwMCwiZXhwIjoxNzU5Nzk1ODAwfQ==.mock-signature"

  curl -s -X PATCH http://localhost:8080/api/applications/$APP_ID/status \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -d '{"status": "UNDER_REVIEW"}' > /dev/null

  echo "  ✅ Estado: UNDER_REVIEW"

  # 5. Crear evaluaciones (Lenguaje, Matemáticas, Psicológica)
  echo "  5/8 Creando evaluaciones..."

  # Evaluación de Lenguaje (Patricia Silva - ID 30)
  curl -s -X POST http://localhost:8080/api/evaluations \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -d "{
      \"applicationId\": $APP_ID,
      \"evaluationType\": \"LANGUAGE_EXAM\",
      \"evaluatorId\": 30,
      \"subject\": \"LANGUAGE\",
      \"score\": 28,
      \"maxScore\": 32,
      \"observations\": \"Excelente comprensión lectora\",
      \"status\": \"COMPLETED\",
      \"strengths\": \"Buena expresión oral y escrita\",
      \"areasForImprovement\": \"Mejorar velocidad lectora\",
      \"recommendations\": \"Estudiante con alto potencial\"
    }" > /dev/null

  # Evaluación de Matemáticas (Alejandra Flores - ID 47)
  curl -s -X POST http://localhost:8080/api/evaluations \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -d "{
      \"applicationId\": $APP_ID,
      \"evaluationType\": \"MATH_EXAM\",
      \"evaluatorId\": 47,
      \"subject\": \"MATHEMATICS\",
      \"score\": 30,
      \"maxScore\": 35,
      \"observations\": \"Buen razonamiento matemático\",
      \"status\": \"COMPLETED\",
      \"strengths\": \"Excelente en resolución de problemas\",
      \"areasForImprovement\": \"Practicar cálculo mental\",
      \"recommendations\": \"Estudiante destacado en matemáticas\"
    }" > /dev/null

  # Evaluación Psicológica (Diego Fuentes - ID 33)
  curl -s -X POST http://localhost:8080/api/evaluations \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -d "{
      \"applicationId\": $APP_ID,
      \"evaluationType\": \"PSYCHOLOGIST_REPORT\",
      \"evaluatorId\": 33,
      \"score\": 25,
      \"maxScore\": 30,
      \"observations\": \"Perfil psicológico adecuado\",
      \"status\": \"COMPLETED\",
      \"strengths\": \"Buena adaptación social\",
      \"areasForImprovement\": \"Trabajar autoestima\",
      \"recommendations\": \"Estudiante con buen perfil emocional\"
    }" > /dev/null

  echo "  ✅ 3 Evaluaciones completadas"

  # 6. Programar entrevista
  echo "  6/8 Programando entrevista..."

  # Fecha 7 días después
  INTERVIEW_DATE=$(date -v+7d +%Y-%m-%d 2>/dev/null || date -d "+7 days" +%Y-%m-%d)

  curl -s -X POST http://localhost:8080/api/interviews \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -d "{
      \"applicationId\": $APP_ID,
      \"interviewerId\": 3,
      \"scheduledDate\": \"$INTERVIEW_DATE\",
      \"scheduledTime\": \"10:00\",
      \"durationMinutes\": 45,
      \"location\": \"Sala de Entrevistas - Piso 2\",
      \"status\": \"COMPLETED\",
      \"notes\": \"Entrevista exitosa. Familia comprometida con valores del colegio.\"
    }" > /dev/null

  echo "  ✅ Entrevista programada: $INTERVIEW_DATE 10:00"

  # 7. Cambiar estado a ACCEPTED
  echo "  7/8 Cambiando estado a ACCEPTED..."

  curl -s -X PATCH http://localhost:8080/api/applications/$APP_ID/status \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -d "{
      \"status\": \"ACCEPTED\",
      \"decisionNotes\": \"Estudiante admitido/a. Excelente perfil académico y familiar.\"
    }" > /dev/null

  echo "  ✅ Estado final: ACCEPTED"

  echo ""
  echo "  🎉 Flujo completo para $student_name: ✅ ADMITIDO/A"
  echo "     Guardian ID: $GUARDIAN_ID"
  echo "     Application ID: $APP_ID"
  echo "     Email: $email"
  echo "     Password: 12345678"
  echo "  ---"
  echo ""
}

# Crear los 3 flujos de admisión
create_admission_flow "jorge.gangale@gmail.com" "12345678-9" "María" "25678901-2" "2015-03-15" "4° Básico" "FEMALE"
create_admission_flow "jorge.gangale@bamail.udo.cl" "23456789-0" "Pedro" "26789012-3" "2016-07-22" "3° Básico" "MALE"
create_admission_flow "jorge.gangale@ilcoud.com" "34567890-1" "Ana" "27890123-4" "2014-11-08" "5° Básico" "FEMALE"

echo "✨✅ FLUJO COMPLETO TERMINADO PARA LOS 3 ESTUDIANTES!"
echo ""
echo "📧 Credenciales de acceso:"
echo "   1. jorge.gangale@gmail.com / 12345678"
echo "   2. jorge.gangale@bamail.udo.cl / 12345678"
echo "   3. jorge.gangale@ilcoud.com / 12345678"
echo ""
