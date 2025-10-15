#!/bin/bash

# Script de limpieza automática del proyecto
# Elimina archivos duplicados, obsoletos y no utilizados

echo "======================================"
echo "LIMPIEZA AUTOMÁTICA DEL PROYECTO"
echo "======================================"
echo ""

cd "/Users/jorgegangale/Library/Mobile Documents/com~apple~CloudDocs/Proyectos/Admision_MTN/Admision_MTN_backend"

# Contador de archivos eliminados
TOTAL_DELETED=0

echo "📦 1. ELIMINANDO ARCHIVOS DUPLICADOS (con ' 2' en el nombre)"
echo "-----------------------------------------------------------"
COUNT=0
find . -name "* 2.*" -type f | grep -v node_modules | while read -r file; do
    echo "  Eliminando: $file"
    rm -f "$file"
    ((COUNT++))
done
echo "✅ Archivos duplicados eliminados"
echo ""

echo "📦 2. ELIMINANDO ARCHIVOS DE BACKUP (.backup, .bak, .old)"
echo "-----------------------------------------------------------"
find . \( -name "*.backup" -o -name "*.bak" -o -name "*.old" \) -type f | grep -v node_modules | while read -r file; do
    echo "  Eliminando: $file"
    rm -f "$file"
    ((TOTAL_DELETED++))
done
echo "✅ Archivos de backup eliminados"
echo ""

echo "📦 3. ELIMINANDO DIRECTORIOS SPRING BOOT (NO USADOS)"
echo "-----------------------------------------------------------"
SPRING_DIRS=(
    "user-service"
    "application-service"
    "evaluation-service"
    "notification-service"
    "eureka-server"
    "shared-libs"
    "api-gateway"
)

for dir in "${SPRING_DIRS[@]}"; do
    if [ -d "$dir" ]; then
        echo "  Eliminando directorio: $dir/"
        rm -rf "$dir"
        ((TOTAL_DELETED++))
    fi
done
echo "✅ Directorios Spring Boot eliminados"
echo ""

echo "📦 4. ELIMINANDO ARCHIVOS DOCKER (NO USADOS)"
echo "-----------------------------------------------------------"
find . -name "docker-compose*.yml" -type f | while read -r file; do
    echo "  Eliminando: $file"
    rm -f "$file"
    ((TOTAL_DELETED++))
done

find . -name "Dockerfile*" -type f | grep -v node_modules | while read -r file; do
    echo "  Eliminando: $file"
    rm -f "$file"
    ((TOTAL_DELETED++))
done
echo "✅ Archivos Docker eliminados"
echo ""

echo "📦 5. ELIMINANDO ARCHIVOS SQL OBSOLETOS (MANTENIENDO ESENCIALES)"
echo "-----------------------------------------------------------"
# Mantener solo los archivos SQL esenciales
KEEP_SQL=(
    "railway-db-setup.sql"
    "db-setup.sql"
    "schema.sql"
)

find . -name "*.sql" -type f | while read -r file; do
    BASENAME=$(basename "$file")
    SHOULD_KEEP=false

    for keep_file in "${KEEP_SQL[@]}"; do
        if [[ "$BASENAME" == "$keep_file" ]]; then
            SHOULD_KEEP=true
            break
        fi
    done

    if [[ "$SHOULD_KEEP" == false ]]; then
        echo "  Eliminando: $file"
        rm -f "$file"
        ((TOTAL_DELETED++))
    else
        echo "  Manteniendo: $file (esencial)"
    fi
done
echo "✅ Archivos SQL obsoletos eliminados"
echo ""

echo "📦 6. ELIMINANDO ARCHIVOS RAILWAY TEMPORALES"
echo "-----------------------------------------------------------"
find . -maxdepth 1 \( -name "railway-*.html" -o -name "RAILWAY_*.md" -o -name "RAILWAY_*.txt" \) -type f | while read -r file; do
    echo "  Eliminando: $file"
    rm -f "$file"
    ((TOTAL_DELETED++))
done
echo "✅ Archivos Railway temporales eliminados"
echo ""

echo "📦 7. ELIMINANDO DIRECTORIOS TARGET (MAVEN)"
echo "-----------------------------------------------------------"
find . -name "target" -type d | grep -v node_modules | while read -r dir; do
    echo "  Eliminando directorio: $dir"
    rm -rf "$dir"
    ((TOTAL_DELETED++))
done
echo "✅ Directorios target eliminados"
echo ""

echo "📦 8. ELIMINANDO ARCHIVOS DE LOG"
echo "-----------------------------------------------------------"
find . -name "*.log" -type f | grep -v node_modules | while read -r file; do
    echo "  Eliminando: $file"
    rm -f "$file"
    ((TOTAL_DELETED++))
done
echo "✅ Archivos de log eliminados"
echo ""

echo "📦 9. ELIMINANDO DIRECTORIO WORKERS (CLOUDFLARE FALLIDO)"
echo "-----------------------------------------------------------"
if [ -d "workers" ]; then
    echo "  Eliminando directorio: workers/"
    rm -rf workers
    ((TOTAL_DELETED++))
    echo "✅ Directorio workers eliminado"
else
    echo "  ℹ️  Directorio workers no existe (ya eliminado)"
fi
echo ""

echo "======================================"
echo "RESUMEN DE LIMPIEZA"
echo "======================================"
echo ""
echo "Tamaño ANTES de la limpieza:"
du -sh . 2>/dev/null | awk '{print $1}'

echo ""
echo "Estructura de directorios después de limpieza:"
echo ""
ls -la | grep ^d | awk '{print $9}' | grep -v "^\." | while read -r dir; do
    if [[ -n "$dir" ]]; then
        SIZE=$(du -sh "$dir" 2>/dev/null | awk '{print $1}')
        echo "  📁 $dir/ - $SIZE"
    fi
done

echo ""
echo "======================================"
echo "✅ LIMPIEZA COMPLETADA"
echo "======================================"
echo ""
echo "Archivos importantes MANTENIDOS:"
echo "  ✅ node_modules/ (dependencias necesarias)"
echo "  ✅ uploads/ (archivos de usuarios)"
echo "  ✅ mock-*.js (servicios activos)"
echo "  ✅ package.json (configuración Node.js)"
echo "  ✅ local-gateway.conf (configuración NGINX)"
echo "  ✅ railway-db-setup.sql (schema esencial)"
echo ""
echo "Próximo paso: Revisar cambios con 'git status' y hacer commit"
echo ""
