#!/bin/bash

# Script de Restauración Segura de Base de Datos
# Sistema de Admisión MTN
# Uso: ./restore-backup.sh <archivo_backup.sql>

set -e  # Exit on error

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuración de base de datos
DB_HOST="localhost"
DB_USER="admin"
DB_PASSWORD="admin123"
DB_NAME="Admisión_MTN_DB"

# Función para imprimir mensajes
print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Banner
echo "╔════════════════════════════════════════════════════════════╗"
echo "║                                                            ║"
echo "║   🗄️  RESTAURACIÓN DE BASE DE DATOS - SISTEMA MTN         ║"
echo "║                                                            ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Validar argumentos
if [ $# -eq 0 ]; then
    print_error "Uso: ./restore-backup.sh <archivo_backup.sql>"
    echo ""
    echo "Backups disponibles:"
    ls -lh *.sql 2>/dev/null || echo "  No hay archivos .sql en este directorio"
    exit 1
fi

BACKUP_FILE="$1"

# Validar que el archivo existe
if [ ! -f "$BACKUP_FILE" ]; then
    print_error "El archivo '$BACKUP_FILE' no existe"
    exit 1
fi

# Mostrar información del backup
print_info "Archivo de backup: $BACKUP_FILE"
print_info "Tamaño: $(du -h "$BACKUP_FILE" | cut -f1)"
print_info "Base de datos objetivo: $DB_NAME"
echo ""

# Verificar conexión a PostgreSQL
print_info "Verificando conexión a PostgreSQL..."
if ! PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -U $DB_USER -d postgres -c '\q' 2>/dev/null; then
    print_error "No se puede conectar a PostgreSQL"
    print_info "Asegúrate de que PostgreSQL esté corriendo: brew services list"
    exit 1
fi
print_success "Conexión establecida"
echo ""

# Advertencia y confirmación
print_warning "⚠️  ADVERTENCIA ⚠️"
echo ""
echo "Esta operación hará lo siguiente:"
echo "  1. Creará un backup de seguridad de la base de datos actual"
echo "  2. Eliminará TODOS los datos actuales de '$DB_NAME'"
echo "  3. Restaurará los datos desde '$BACKUP_FILE'"
echo ""
print_warning "Esta acción NO se puede deshacer (excepto con el backup de seguridad)"
echo ""
read -p "¿Estás seguro de continuar? (escribe 'SI' para confirmar): " confirm

if [ "$confirm" != "SI" ]; then
    print_info "Operación cancelada por el usuario"
    exit 0
fi

echo ""
print_info "Iniciando proceso de restauración..."
echo ""

# Paso 1: Crear backup de seguridad de la base de datos actual
SAFETY_BACKUP="safety_backup_$(date +%Y%m%d_%H%M%S).sql"
print_info "Paso 1/5: Creando backup de seguridad de la BD actual..."
if PGPASSWORD=$DB_PASSWORD pg_dump -h $DB_HOST -U $DB_USER -d "$DB_NAME" -F p -f "$SAFETY_BACKUP" 2>/dev/null; then
    print_success "Backup de seguridad creado: $SAFETY_BACKUP"
else
    print_warning "No se pudo crear backup de seguridad (la BD podría no existir)"
fi
echo ""

# Paso 2: Descartar base de datos actual
print_info "Paso 2/5: Descartando base de datos actual..."
if PGPASSWORD=$DB_PASSWORD dropdb -h $DB_HOST -U $DB_USER "$DB_NAME" 2>/dev/null; then
    print_success "Base de datos descartada"
else
    print_warning "Base de datos no existía (se creará una nueva)"
fi
echo ""

# Paso 3: Crear base de datos vacía
print_info "Paso 3/5: Creando base de datos vacía..."
if PGPASSWORD=$DB_PASSWORD createdb -h $DB_HOST -U $DB_USER "$DB_NAME"; then
    print_success "Base de datos creada"
else
    print_error "Error al crear base de datos"
    print_info "Intentando restaurar desde backup de seguridad..."
    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -U $DB_USER -d "$DB_NAME" -f "$SAFETY_BACKUP" > /dev/null 2>&1
    exit 1
fi
echo ""

# Paso 4: Restaurar desde backup
print_info "Paso 4/5: Restaurando datos desde backup..."
print_info "Esto puede tomar unos minutos..."
if PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -U $DB_USER -d "$DB_NAME" -f "$BACKUP_FILE" > /dev/null 2>&1; then
    print_success "Datos restaurados exitosamente"
else
    print_error "Error durante la restauración"
    print_info "Intentando restaurar desde backup de seguridad..."
    PGPASSWORD=$DB_PASSWORD dropdb -h $DB_HOST -U $DB_USER "$DB_NAME" 2>/dev/null
    PGPASSWORD=$DB_PASSWORD createdb -h $DB_HOST -U $DB_USER "$DB_NAME"
    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -U $DB_USER -d "$DB_NAME" -f "$SAFETY_BACKUP" > /dev/null 2>&1
    exit 1
fi
echo ""

# Paso 5: Verificar restauración
print_info "Paso 5/5: Verificando restauración..."
RECORD_COUNT=$(PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -U $DB_USER -d "$DB_NAME" -t -c "
SELECT COUNT(*) FROM users +
       (SELECT COUNT(*) FROM applications) +
       (SELECT COUNT(*) FROM students) +
       (SELECT COUNT(*) FROM guardians);
" 2>/dev/null | tr -d ' \n')

if [ -n "$RECORD_COUNT" ] && [ "$RECORD_COUNT" -gt 0 ]; then
    print_success "Verificación exitosa: $RECORD_COUNT registros restaurados"
else
    print_warning "No se pudieron contar los registros (podría ser normal)"
fi
echo ""

# Resumen final
echo "╔════════════════════════════════════════════════════════════╗"
echo "║                                                            ║"
echo "║   ✅ RESTAURACIÓN COMPLETADA EXITOSAMENTE                  ║"
echo "║                                                            ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""
print_success "Base de datos restaurada desde: $BACKUP_FILE"
print_info "Backup de seguridad guardado en: $SAFETY_BACKUP"
echo ""
print_info "Próximos pasos:"
echo "  1. Reinicia los servicios backend si estaban corriendo"
echo "  2. Verifica que puedes hacer login en http://localhost:5173"
echo "  3. Comprueba que los datos se ven correctos"
echo ""
print_info "Para ver detalles de los datos restaurados:"
echo "  PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -U $DB_USER -d \"$DB_NAME\" -c \\"
echo "  \"SELECT 'Users' as tabla, COUNT(*) FROM users"
echo "   UNION ALL SELECT 'Applications', COUNT(*) FROM applications"
echo "   UNION ALL SELECT 'Students', COUNT(*) FROM students;\""
echo ""
