#!/bin/bash

# Script de ejecución directa para limpiar la base de datos Railway
# Este script solicita las credenciales y ejecuta el cleanup

echo "========================================"
echo "🗑️  LIMPIEZA DE BASE DE DATOS RAILWAY"
echo "========================================"
echo ""
echo "⚠️  ADVERTENCIA: Esta operación es DESTRUCTIVA e IRREVERSIBLE"
echo "    Se eliminarán TODOS los datos excepto el usuario admin"
echo ""
echo "Necesitas las credenciales de Railway PostgreSQL:"
echo ""
echo "1. Ve a https://railway.app/"
echo "2. Selecciona: Admision_MTN_Backend"
echo "3. Click en servicio PostgreSQL"
echo "4. Tab 'Connect'"
echo "5. Copia 'Postgres Connection URL'"
echo ""
echo "El URL debe verse así:"
echo "postgresql://postgres:XXXXX@containers-us-west-XX.railway.app:YYYY/railway"
echo ""
echo "========================================"
echo ""

# Solicitar DATABASE_URL
read -p "Pega el DATABASE_URL aquí: " DATABASE_URL

if [ -z "$DATABASE_URL" ]; then
  echo ""
  echo "❌ ERROR: DATABASE_URL vacío"
  exit 1
fi

# Validar que sea una URL de PostgreSQL
if [[ ! "$DATABASE_URL" =~ ^postgresql:// ]]; then
  echo ""
  echo "❌ ERROR: El URL debe comenzar con 'postgresql://'"
  echo "   Recibido: ${DATABASE_URL:0:50}..."
  exit 1
fi

echo ""
echo "✅ DATABASE_URL recibido"
echo "   ${DATABASE_URL:0:60}... (truncado)"
echo ""

# Preguntar si ejecutar dry-run primero
echo "========================================"
echo "MODO DE EJECUCIÓN"
echo "========================================"
echo ""
echo "1. DRY-RUN (recomendado): Prueba sin hacer cambios"
echo "2. EXECUTE: Ejecutar limpieza real (DESTRUCTIVO)"
echo ""
read -p "Selecciona (1 o 2): " MODE_CHOICE

if [ "$MODE_CHOICE" = "1" ]; then
  MODE="--dry-run"
  echo ""
  echo "🔍 Ejecutando en modo DRY-RUN (sin cambios)..."
elif [ "$MODE_CHOICE" = "2" ]; then
  MODE="--execute"
  echo ""
  echo "⚠️  ⚠️  ⚠️  CONFIRMACIÓN FINAL ⚠️  ⚠️  ⚠️"
  echo ""
  echo "Vas a ELIMINAR PERMANENTEMENTE todos los datos de Railway"
  echo "excepto el usuario admin (jorge.gangale@mtn.cl)."
  echo ""
  echo "Esta acción NO SE PUEDE DESHACER."
  echo ""
  read -p "Escribe 'ELIMINAR TODO' para confirmar: " CONFIRMATION

  if [ "$CONFIRMATION" != "ELIMINAR TODO" ]; then
    echo ""
    echo "❌ Limpieza cancelada (confirmación incorrecta)"
    exit 1
  fi

  echo ""
  echo "✅ Confirmación recibida. Ejecutando limpieza..."
else
  echo ""
  echo "❌ Opción inválida: $MODE_CHOICE"
  exit 1
fi

echo ""
echo "========================================"
echo "EJECUTANDO SCRIPT..."
echo "========================================"
echo ""

# Exportar DATABASE_URL y ejecutar script Node.js
export DATABASE_URL="$DATABASE_URL"
node scripts/cleanup-railway-db.js $MODE

EXIT_CODE=$?

echo ""
if [ $EXIT_CODE -eq 0 ]; then
  echo "========================================"
  echo "✅ SCRIPT COMPLETADO EXITOSAMENTE"
  echo "========================================"

  if [ "$MODE" = "--dry-run" ]; then
    echo ""
    echo "Revisa el output arriba para ver qué se eliminaría."
    echo ""
    echo "Para ejecutar la limpieza real, vuelve a ejecutar:"
    echo "  ./scripts/execute-cleanup-now.sh"
    echo "Y selecciona opción 2 (EXECUTE)"
  else
    echo ""
    echo "⚠️  Base de datos limpiada exitosamente"
    echo "   Solo queda el usuario admin: jorge.gangale@mtn.cl"
    echo ""
    echo "Verifica el deployment de Railway:"
    echo "  curl https://admisionmtnbackendv2-production.up.railway.app/health"
  fi
else
  echo "========================================"
  echo "❌ ERROR EN LA EJECUCIÓN"
  echo "========================================"
  echo ""
  echo "Código de salida: $EXIT_CODE"
  echo "Revisa los logs arriba para más detalles."
fi

echo ""
