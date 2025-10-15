#!/bin/bash

# Script de análisis para limpieza del proyecto
# Identifica archivos innecesarios, duplicados y obsoletos

echo "======================================"
echo "ANÁLISIS DE LIMPIEZA DEL PROYECTO"
echo "======================================"
echo ""

cd "/Users/jorgegangale/Library/Mobile Documents/com~apple~CloudDocs/Proyectos/Admision_MTN/Admision_MTN_backend"

echo "📊 1. ARCHIVOS DUPLICADOS (con ' 2' en el nombre)"
echo "---------------------------------------------------"
find . -name "* 2.*" -type f | grep -v node_modules | sort
echo ""

echo "📊 2. ARCHIVOS DE BACKUP (.backup, .bak, .old)"
echo "---------------------------------------------------"
find . \( -name "*.backup" -o -name "*.bak" -o -name "*.old" \) -type f | grep -v node_modules | sort
echo ""

echo "📊 3. DIRECTORIOS SPRING BOOT (NO USADOS)"
echo "---------------------------------------------------"
echo "user-service/"
echo "application-service/"
echo "evaluation-service/"
echo "notification-service/"
echo "eureka-server/"
echo "shared-libs/"
echo "api-gateway/"
echo ""

echo "📊 4. ARCHIVOS DE CONFIGURACIÓN DOCKER (NO USADOS)"
echo "---------------------------------------------------"
find . -name "docker-compose*.yml" -type f | sort
find . -name "Dockerfile*" -type f | grep -v node_modules | sort
echo ""

echo "📊 5. ARCHIVOS SQL DE TESTING/MIGRACIÓN"
echo "---------------------------------------------------"
find . -name "*.sql" -type f | wc -l
echo "Total archivos SQL encontrados (muchos pueden ser obsoletos)"
echo ""

echo "📊 6. ARCHIVOS DE LOG"
echo "---------------------------------------------------"
find . -name "*.log" -type f | grep -v node_modules | sort
echo ""

echo "📊 7. DIRECTORIOS DE BUILD (Maven/Spring Boot)"
echo "---------------------------------------------------"
find . -name "target" -type d | grep -v node_modules
echo ""

echo "📊 8. ARCHIVOS MAVEN (pom.xml NO USADOS)"
echo "---------------------------------------------------"
find . -name "pom.xml" -o -name "pom-*.xml" | sort
echo ""

echo "📊 9. ARCHIVOS WORKERS CLOUDFLARE (FALLIDO)"
echo "---------------------------------------------------"
ls -la workers/ 2>/dev/null || echo "No existe directorio workers/"
echo ""

echo "📊 10. ARCHIVOS RAILWAY HTML/TICKETS (TEMPORALES)"
echo "---------------------------------------------------"
ls -la railway-*.html railway-*.sh RAILWAY_*.md RAILWAY_*.txt 2>/dev/null | head -20
echo ""

echo "======================================"
echo "RESUMEN DE TAMAÑO"
echo "======================================"
echo ""

echo "Tamaño total del proyecto:"
du -sh .

echo ""
echo "Tamaño de directorios grandes:"
du -sh */ 2>/dev/null | sort -hr | head -10

echo ""
echo "======================================"
echo "FIN DEL ANÁLISIS"
echo "======================================"
