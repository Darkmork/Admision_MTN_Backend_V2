#!/bin/bash

# Script para iniciar el backend con la base de datos correcta
# Uso: ./start-backend.sh

echo "🚀 Iniciando backend con base de datos unificada: Admisión_MTN_DB"

# Configurar variables de entorno para base de datos correcta
export DB_HOST=localhost
export DB_USERNAME=admin  
export DB_PASSWORD=admin123
export DB_NAME="Admisión_MTN_DB"

# URL completa para asegurar conexión correcta
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/Admisión_MTN_DB"
export SPRING_DATASOURCE_USERNAME=admin
export SPRING_DATASOURCE_PASSWORD=admin123

echo "✅ Variables de entorno configuradas:"
echo "   DB_NAME: $DB_NAME" 
echo "   SPRING_DATASOURCE_URL: $SPRING_DATASOURCE_URL"

# Cambiar al directorio del backend
cd "/Users/jorgegangale/Library/Mobile Documents/com~apple~CloudDocs/Proyectos/Admision_MTN/Admision_MTN_backend"

# Iniciar aplicación
echo "🔄 Iniciando Spring Boot..."
mvn spring-boot:run