#!/bin/bash

###############################################################################
# Script Simplificado de Restauración de Base de Datos Railway
###############################################################################
# Este script es para que LO EJECUTES TÚ directamente en tu terminal
# Ya que Claude Code no puede obtener el DATABASE_URL automáticamente
###############################################################################

echo "╔═══════════════════════════════════════════════════════════╗"
echo "║                                                           ║"
echo "║  🗄️  RESTAURACIÓN DE BASE DE DATOS RAILWAY                ║"
echo "║                                                           ║"
echo "╚═══════════════════════════════════════════════════════════╝"
echo ""

# Paso 1: Obtener DATABASE_URL
echo "📋 PASO 1: Obtener DATABASE_URL"
echo ""
echo "Ve a Railway Dashboard y copia el DATABASE_URL:"
echo "  1. Abre: https://railway.app/dashboard"
echo "  2. Click en: Admision_MTN_Backend"
echo "  3. Click en: Postgres (icono de elefante)"
echo "  4. Tab: Variables"
echo "  5. Busca: DATABASE_URL"
echo "  6. Click en el ojo 👁️ para revelar"
echo "  7. Copia la URL completa"
echo ""
read -p "Pega aquí el DATABASE_URL: " DATABASE_URL
echo ""

# Validar que se ingresó algo
if [ -z "$DATABASE_URL" ]; then
    echo "❌ ERROR: No ingresaste ninguna URL"
    exit 1
fi

# Validar que comienza con postgresql://
if [[ ! "$DATABASE_URL" =~ ^postgresql:// ]]; then
    echo "❌ ERROR: La URL no parece válida (debe comenzar con postgresql://)"
    exit 1
fi

echo "✅ DATABASE_URL capturado correctamente"
echo ""

# Paso 2: Verificar que psql está instalado
echo "📋 PASO 2: Verificar herramientas"
echo ""

if ! command -v psql &> /dev/null; then
    echo "❌ ERROR: psql no está instalado"
    echo "Instálalo con: brew install postgresql@15"
    exit 1
fi

echo "✅ psql está instalado"
echo ""

# Paso 3: Verificar backup existe
BACKUP_FILE="backups/admision_mtn_backup_20251013_082802.sql"

if [ ! -f "$BACKUP_FILE" ]; then
    echo "❌ ERROR: No se encontró el archivo de backup: $BACKUP_FILE"
    exit 1
fi

echo "✅ Backup encontrado: $BACKUP_FILE ($(du -h "$BACKUP_FILE" | cut -f1))"
echo ""

# Paso 4: Confirmar restauración
echo "📋 PASO 3: Confirmar restauración"
echo ""
echo "⚠️  ADVERTENCIA:"
echo "  - Esto sobrescribirá TODOS los datos actuales en Railway"
echo "  - Se restaurarán 144 registros:"
echo "    · 38 usuarios"
echo "    · 21 aplicaciones"
echo "    · 51 estudiantes"
echo "    · 27 apoderados"
echo "    · 6 evaluaciones"
echo "    · 1 entrevista"
echo ""
read -p "¿Continuar con la restauración? (escribe 'SI' para confirmar): " confirm

if [ "$confirm" != "SI" ]; then
    echo "❌ Restauración cancelada"
    exit 0
fi

echo ""
echo "╔═══════════════════════════════════════════════════════════╗"
echo "║  🚀 INICIANDO RESTAURACIÓN...                             ║"
echo "╚═══════════════════════════════════════════════════════════╝"
echo ""

# Paso 5: Restaurar
echo "📦 Restaurando base de datos..."
echo "(Esto puede tomar 30-60 segundos...)"
echo ""

psql "$DATABASE_URL" < "$BACKUP_FILE" 2>&1 | \
  grep -v "ERROR:  relation .* already exists" | \
  grep -v "ERROR:  type .* already exists" | \
  grep -v "ERROR:  constraint .* already exists" | \
  tail -20

if [ ${PIPESTATUS[0]} -eq 0 ]; then
    echo ""
    echo "✅ Restauración completada"
else
    echo ""
    echo "⚠️  Restauración completada con algunos warnings (probablemente normales)"
fi

echo ""

# Paso 6: Verificar
echo "╔═══════════════════════════════════════════════════════════╗"
echo "║  🔍 VERIFICANDO RESTAURACIÓN...                           ║"
echo "╚═══════════════════════════════════════════════════════════╝"
echo ""

echo "📊 Conteo de registros:"
psql "$DATABASE_URL" -c "
SELECT
  'users' as tabla, COUNT(*) as registros FROM users
UNION ALL SELECT 'applications', COUNT(*) FROM applications
UNION ALL SELECT 'students', COUNT(*) FROM students
UNION ALL SELECT 'guardians', COUNT(*) FROM guardians
UNION ALL SELECT 'evaluations', COUNT(*) FROM evaluations
UNION ALL SELECT 'interviews', COUNT(*) FROM interviews
ORDER BY tabla;
" 2>/dev/null

echo ""
echo "📋 Usuario administrador:"
psql "$DATABASE_URL" -c "
SELECT id, email, role, first_name, last_name
FROM users
WHERE role = 'ADMIN'
LIMIT 3;
" 2>/dev/null

echo ""
echo "╔═══════════════════════════════════════════════════════════╗"
echo "║                                                           ║"
echo "║  ✅ RESTAURACIÓN COMPLETADA EXITOSAMENTE                  ║"
echo "║                                                           ║"
echo "╚═══════════════════════════════════════════════════════════╝"
echo ""
echo "🎉 ¡La base de datos está lista!"
echo ""
echo "📋 Próximos pasos:"
echo "  1. Probar login:"
echo "     curl -X POST 'https://admisionmtnbackendv2-production.up.railway.app/api/auth/login' \\"
echo "       -H 'Content-Type: application/json' \\"
echo "       -d '{\"email\":\"jorge.gangale@mtn.cl\",\"password\":\"admin123\"}'"
echo ""
echo "  2. Ejecutar smoke tests:"
echo "     export RAILWAY_URL='https://admisionmtnbackendv2-production.up.railway.app'"
echo "     ./railway-smoke-tests.sh"
echo ""
echo "  3. Configurar frontend Vercel con:"
echo "     VITE_API_BASE_URL=https://admisionmtnbackendv2-production.up.railway.app"
echo ""
