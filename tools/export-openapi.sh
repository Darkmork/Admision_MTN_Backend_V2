#!/bin/bash

# Export OpenAPI Documentation Script
# Sistema de Admisión MTN - Fase 0 Pre-flight

set -e

# Configuration
API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
DOCS_DIR="${DOCS_DIR:-docs}"
MAX_RETRIES=30
RETRY_INTERVAL=2

echo "🔧 Exportando documentación OpenAPI..."
echo "📍 Base URL: $API_BASE_URL"
echo "📁 Directorio destino: $DOCS_DIR"

# Function to check if API is running
check_api_health() {
    local url="$1/actuator/health"
    echo "🩺 Verificando health check en: $url"
    
    if curl -s -f "$url" > /dev/null 2>&1; then
        echo "✅ API está corriendo y saludable"
        return 0
    else
        echo "❌ API no responde en health check"
        return 1
    fi
}

# Function to wait for API to be ready
wait_for_api() {
    local retries=0
    echo "⏳ Esperando que la API esté lista..."
    
    while [ $retries -lt $MAX_RETRIES ]; do
        if check_api_health "$API_BASE_URL"; then
            echo "🚀 API lista para exportar documentación"
            return 0
        fi
        
        retries=$((retries + 1))
        echo "⏱️  Intento $retries/$MAX_RETRIES - esperando ${RETRY_INTERVAL}s..."
        sleep $RETRY_INTERVAL
    done
    
    echo "💥 Error: API no estuvo lista después de $MAX_RETRIES intentos"
    return 1
}

# Create docs directory if it doesn't exist
mkdir -p "$DOCS_DIR"

# Wait for API to be ready
if ! wait_for_api; then
    echo "❌ No se pudo conectar con la API. ¿Está corriendo en $API_BASE_URL?"
    echo "💡 Intenta ejecutar: mvn spring-boot:run"
    exit 1
fi

# Export OpenAPI JSON
echo "📄 Exportando OpenAPI JSON..."
JSON_URL="$API_BASE_URL/v3/api-docs"
JSON_FILE="$DOCS_DIR/openapi.json"

if curl -s -f "$JSON_URL" -o "$JSON_FILE"; then
    echo "✅ OpenAPI JSON exportado: $JSON_FILE"
    # Pretty print the JSON
    if command -v jq > /dev/null 2>&1; then
        echo "🎨 Formateando JSON con jq..."
        jq . "$JSON_FILE" > "${JSON_FILE}.tmp" && mv "${JSON_FILE}.tmp" "$JSON_FILE"
    fi
else
    echo "❌ Error exportando OpenAPI JSON desde $JSON_URL"
    exit 1
fi

# Export OpenAPI YAML
echo "📄 Exportando OpenAPI YAML..."
YAML_URL="$API_BASE_URL/v3/api-docs.yaml"
YAML_FILE="$DOCS_DIR/openapi.yaml"

if curl -s -f "$YAML_URL" -o "$YAML_FILE"; then
    echo "✅ OpenAPI YAML exportado: $YAML_FILE"
else
    echo "❌ Error exportando OpenAPI YAML desde $YAML_URL"
    # YAML export might not be available, that's OK
fi

# Generate summary
echo "📊 Generando resumen de la documentación..."
if command -v jq > /dev/null 2>&1 && [ -f "$JSON_FILE" ]; then
    TOTAL_ENDPOINTS=$(jq '.paths | to_entries | length' "$JSON_FILE")
    API_VERSION=$(jq -r '.info.version // "unknown"' "$JSON_FILE")
    API_TITLE=$(jq -r '.info.title // "unknown"' "$JSON_FILE")
    
    echo "📋 Resumen:"
    echo "   📌 Título: $API_TITLE"
    echo "   🏷️  Versión: $API_VERSION"
    echo "   🔗 Total endpoints: $TOTAL_ENDPOINTS"
    
    # Generate endpoint summary
    echo "🔍 Top 10 endpoints más complejos:"
    jq -r '.paths | to_entries[] | .key as $path | .value | to_entries[] | .key as $method | "\($method | ascii_upcase) \($path)"' "$JSON_FILE" | head -10 | while read -r endpoint; do
        echo "   • $endpoint"
    done
else
    echo "⚠️  jq no disponible - no se puede generar resumen detallado"
fi

# Generate timestamp file
TIMESTAMP_FILE="$DOCS_DIR/export_timestamp.txt"
echo "$(date '+%Y-%m-%d %H:%M:%S') - Exportado desde $API_BASE_URL" > "$TIMESTAMP_FILE"

# Show files generated
echo ""
echo "📁 Archivos generados:"
ls -la "$DOCS_DIR"/ | grep -E "(openapi\.|export_)" || echo "   (sin archivos OpenAPI encontrados)"

echo ""
echo "🎉 Exportación completada exitosamente!"
echo "📖 Puedes ver la documentación en: $API_BASE_URL/swagger-ui.html"
echo "📄 Archivos exportados en: $DOCS_DIR/"

# Optional: Open Swagger UI in browser (macOS only)
if [[ "$OSTYPE" == "darwin"* ]] && command -v open > /dev/null 2>&1; then
    read -p "🌐 ¿Abrir Swagger UI en el navegador? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        open "$API_BASE_URL/swagger-ui.html"
    fi
fi