#!/bin/bash

# Script para enviar notificaciones retroactivas para las 3 aplicaciones creadas
# Apps: 40, 41, 42 (María, Pedro, Ana Gangale)

echo "📧 Enviando notificaciones retroactivas para aplicaciones 40, 41, 42..."
echo ""

# Token de admin para hacer las llamadas
ADMIN_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiIxIiwiZW1haWwiOiJqb3JnZS5nYW5nYWxlQG10bi5jbCIsInJvbGUiOiJBRE1JTiIsImlhdCI6MTc1OTcwOTQwMCwiZXhwIjoxNzU5Nzk1ODAwfQ==.mock-signature"

# Función para enviar notificación de cambio de estado
send_status_notification() {
  local app_id=$1
  local student_name=$2
  local guardian_email=$3
  local old_status=$4
  local new_status=$5

  echo "📨 Enviando notificación de estado para ${student_name}..."

  curl -s -X POST http://localhost:8080/api/notifications/send \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -d "{
      \"type\": \"email\",
      \"recipient\": \"${guardian_email}\",
      \"template\": \"status_change\",
      \"templateData\": {
        \"applicationId\": ${app_id},
        \"studentName\": \"${student_name}\",
        \"oldStatus\": \"${old_status}\",
        \"newStatus\": \"${new_status}\",
        \"statusDate\": \"$(date +%Y-%m-%d)\"
      }
    }" | jq '.'

  echo ""
}

# Función para enviar notificación de entrevista
send_interview_notification() {
  local app_id=$1
  local student_name=$2
  local guardian_email=$3
  local interview_date=$4

  echo "📅 Enviando notificación de entrevista para ${student_name}..."

  curl -s -X POST http://localhost:8080/api/notifications/send \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -d "{
      \"type\": \"email\",
      \"recipient\": \"${guardian_email}\",
      \"template\": \"interview_scheduled\",
      \"templateData\": {
        \"studentName\": \"${student_name}\",
        \"interviewDate\": \"${interview_date}\",
        \"interviewTime\": \"10:00\",
        \"interviewer\": \"Carlos Morales (Coordinador)\",
        \"location\": \"Sala de Entrevistas - Piso 2\"
      }
    }" | jq '.'

  echo ""
}

# Calcular fechas de entrevista (7, 8 y 9 días después)
DATE_7=$(date -v+7d +%Y-%m-%d 2>/dev/null || date -d "+7 days" +%Y-%m-%d)
DATE_8=$(date -v+8d +%Y-%m-%d 2>/dev/null || date -d "+8 days" +%Y-%m-%d)
DATE_9=$(date -v+9d +%Y-%m-%d 2>/dev/null || date -d "+9 days" +%Y-%m-%d)

echo "═══════════════════════════════════════════════════════════"
echo "📧 APLICACIÓN #40 - María Gangale"
echo "═══════════════════════════════════════════════════════════"

# 1. Notificación de creación de aplicación
send_status_notification 40 "María Gangale Torres" "jorge.gangale@gmail.com" "PENDING" "UNDER_REVIEW"

# 2. Notificación de entrevista programada
send_interview_notification 40 "María Gangale Torres" "jorge.gangale@gmail.com" "$DATE_7"

# 3. Notificación de cambio a APPROVED
send_status_notification 40 "María Gangale Torres" "jorge.gangale@gmail.com" "UNDER_REVIEW" "APPROVED"

echo "═══════════════════════════════════════════════════════════"
echo "📧 APLICACIÓN #41 - Pedro Gangale"
echo "═══════════════════════════════════════════════════════════"

# 1. Notificación de creación de aplicación
send_status_notification 41 "Pedro Gangale Torres" "jorge.gangale@bamail.udo.cl" "PENDING" "UNDER_REVIEW"

# 2. Notificación de entrevista programada
send_interview_notification 41 "Pedro Gangale Torres" "jorge.gangale@bamail.udo.cl" "$DATE_8"

# 3. Notificación de cambio a APPROVED
send_status_notification 41 "Pedro Gangale Torres" "jorge.gangale@bamail.udo.cl" "UNDER_REVIEW" "APPROVED"

echo "═══════════════════════════════════════════════════════════"
echo "📧 APLICACIÓN #42 - Ana Gangale"
echo "═══════════════════════════════════════════════════════════"

# 1. Notificación de creación de aplicación
send_status_notification 42 "Ana Gangale Torres" "jorge.gangale@ilcoud.com" "PENDING" "UNDER_REVIEW"

# 2. Notificación de entrevista programada
send_interview_notification 42 "Ana Gangale Torres" "jorge.gangale@ilcoud.com" "$DATE_9"

# 3. Notificación de cambio a APPROVED
send_status_notification 42 "Ana Gangale Torres" "jorge.gangale@ilcoud.com" "UNDER_REVIEW" "APPROVED"

echo "═══════════════════════════════════════════════════════════"
echo "✅ NOTIFICACIONES COMPLETADAS"
echo "═══════════════════════════════════════════════════════════"
echo ""
echo "📬 Total de correos enviados:"
echo "   - 3 notificaciones de cambio de estado (PENDING → UNDER_REVIEW)"
echo "   - 3 notificaciones de entrevista programada"
echo "   - 3 notificaciones de aprobación (UNDER_REVIEW → APPROVED)"
echo ""
echo "   TOTAL: 9 correos enviados"
echo ""
echo "📧 Destinatarios:"
echo "   1. jorge.gangale@gmail.com (María Gangale)"
echo "   2. jorge.gangale@bamail.udo.cl (Pedro Gangale)"
echo "   3. jorge.gangale@ilcoud.com (Ana Gangale)"
echo ""
