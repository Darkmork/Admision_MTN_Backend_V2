#!/bin/bash

echo "╔═══════════════════════════════════════════════════════════╗"
echo "║  🔍 VERIFICAR BASE DE DATOS RAILWAY                       ║"
echo "╚═══════════════════════════════════════════════════════════╝"
echo ""

read -p "Pega aquí el DATABASE_URL de Railway: " DATABASE_URL

if [ -z "$DATABASE_URL" ]; then
    echo "❌ No ingresaste DATABASE_URL"
    exit 1
fi

echo ""
echo "📊 Contando registros en Railway..."
echo ""

psql "$DATABASE_URL" -c "
SELECT
  'users' as tabla, COUNT(*) as registros FROM users
UNION ALL SELECT 'applications', COUNT(*) FROM applications
UNION ALL SELECT 'students', COUNT(*) FROM students
UNION ALL SELECT 'guardians', COUNT(*) FROM guardians
UNION ALL SELECT 'evaluations', COUNT(*) FROM evaluations
UNION ALL SELECT 'interviews', COUNT(*) FROM interviews
ORDER BY tabla;
"

echo ""
echo "Si todos los contadores están en 0, ejecuta:"
echo "  psql \"\$DATABASE_URL\" < backups/admision_mtn_backup_20251013_082802.sql"
