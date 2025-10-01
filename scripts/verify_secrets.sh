#!/usr/bin/env bash
# ============================================================================
# Script de Verificación de Secretos - Sistema de Admisión MTN
# Detecta secretos débiles, hardcoded o en riesgo
# ============================================================================
set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "🔐 VERIFICACIÓN DE SECRETOS - Sistema de Admisión MTN"
echo "======================================================"
echo ""

# Detect root directory
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
echo "📁 Root: $ROOT"
echo ""

# Counters
CRITICAL=0
WARNINGS=0
PASSED=0

# ============================================================================
# 1. CHECK .env FILE EXISTS
# ============================================================================
echo "📋 [1/8] Verificando archivos .env..."

if [ ! -f "$ROOT/.env" ]; then
    echo -e "${RED}❌ .env file not found${NC}"
    echo "   Create .env file from .env.example template"
    exit 1
fi

echo -e "${GREEN}✅ .env file exists${NC}"

# ============================================================================
# 2. CHECK .gitignore
# ============================================================================
echo ""
echo "📋 [2/8] Verificando .gitignore..."

if [ -f "$ROOT/.gitignore" ]; then
    if grep -q "^\.env$" "$ROOT/.gitignore" || grep -q "^\.env\.local$" "$ROOT/.gitignore"; then
        echo -e "${GREEN}✅ .env está en .gitignore${NC}"
        PASSED=$((PASSED + 1))
    else
        echo -e "${RED}❌ CRITICAL: .env NO está en .gitignore${NC}"
        echo "   Riesgo: Secretos pueden ser commiteados al repositorio"
        CRITICAL=$((CRITICAL + 1))
    fi
else
    echo -e "${YELLOW}⚠️  .gitignore not found${NC}"
    WARNINGS=$((WARNINGS + 1))
fi

# ============================================================================
# 3. CHECK JWT_SECRET STRENGTH
# ============================================================================
echo ""
echo "🔑 [3/8] Verificando JWT_SECRET..."

if [ -f "$ROOT/.env" ]; then
    JWT_SECRET=$(grep "^JWT_SECRET=" "$ROOT/.env" | cut -d'=' -f2)
    
    if [ -z "$JWT_SECRET" ]; then
        echo -e "${RED}❌ CRITICAL: JWT_SECRET está vacío${NC}"
        CRITICAL=$((CRITICAL + 1))
    elif [ ${#JWT_SECRET} -lt 64 ]; then
        echo -e "${RED}❌ CRITICAL: JWT_SECRET demasiado corto (${#JWT_SECRET} chars)${NC}"
        echo "   Recomendado: Al menos 128 caracteres (512 bits)"
        CRITICAL=$((CRITICAL + 1))
    elif [[ "$JWT_SECRET" == "your_secure_jwt_secret"* ]]; then
        echo -e "${RED}❌ CRITICAL: JWT_SECRET es el valor por defecto${NC}"
        echo "   Generar nuevo: node -e \"console.log(require('crypto').randomBytes(64).toString('hex'))\""
        CRITICAL=$((CRITICAL + 1))
    elif [[ "$JWT_SECRET" =~ ^[a-zA-Z0-9+/]+==?$ ]]; then
        echo -e "${YELLOW}⚠️  JWT_SECRET parece ser Base64 (menos seguro que hex)${NC}"
        echo "   Longitud: ${#JWT_SECRET} chars"
        WARNINGS=$((WARNINGS + 1))
    else
        echo -e "${GREEN}✅ JWT_SECRET es fuerte (${#JWT_SECRET} chars)${NC}"
        PASSED=$((PASSED + 1))
    fi
fi

# ============================================================================
# 4. CHECK DB_PASSWORD STRENGTH
# ============================================================================
echo ""
echo "🗄️  [4/8] Verificando DB_PASSWORD..."

if [ -f "$ROOT/.env" ]; then
    DB_PASSWORD=$(grep "^DB_PASSWORD=" "$ROOT/.env" | cut -d'=' -f2)
    
    WEAK_PASSWORDS=("password" "admin" "admin123" "12345" "qwerty" "test" "demo")
    IS_WEAK=false
    
    for weak in "${WEAK_PASSWORDS[@]}"; do
        if [[ "$DB_PASSWORD" == "$weak"* ]] || [[ "$DB_PASSWORD" == *"$weak" ]]; then
            IS_WEAK=true
            break
        fi
    done
    
    if [ -z "$DB_PASSWORD" ]; then
        echo -e "${RED}❌ CRITICAL: DB_PASSWORD está vacío${NC}"
        CRITICAL=$((CRITICAL + 1))
    elif [ "$IS_WEAK" = true ]; then
        echo -e "${YELLOW}⚠️  DB_PASSWORD parece débil: $DB_PASSWORD${NC}"
        echo "   Recomendación: Usar secretos aleatorios de 32+ caracteres"
        echo "   OK para desarrollo local, CAMBIAR en producción"
        WARNINGS=$((WARNINGS + 1))
    elif [ ${#DB_PASSWORD} -lt 16 ]; then
        echo -e "${YELLOW}⚠️  DB_PASSWORD corto (${#DB_PASSWORD} chars)${NC}"
        echo "   Recomendado: 32+ caracteres en producción"
        WARNINGS=$((WARNINGS + 1))
    else
        echo -e "${GREEN}✅ DB_PASSWORD es aceptable (${#DB_PASSWORD} chars)${NC}"
        PASSED=$((PASSED + 1))
    fi
fi

# ============================================================================
# 5. CHECK SMTP_PASSWORD
# ============================================================================
echo ""
echo "📧 [5/8] Verificando SMTP_PASSWORD..."

if [ -f "$ROOT/.env" ]; then
    SMTP_PASSWORD=$(grep "^SMTP_PASSWORD=" "$ROOT/.env" | cut -d'=' -f2)
    
    if [ -z "$SMTP_PASSWORD" ]; then
        echo -e "${YELLOW}⚠️  SMTP_PASSWORD está vacío (email mock mode?)${NC}"
        WARNINGS=$((WARNINGS + 1))
    elif [[ "$SMTP_PASSWORD" == "CHANGE"* ]] || [[ "$SMTP_PASSWORD" == "your_"* ]]; then
        echo -e "${YELLOW}⚠️  SMTP_PASSWORD es placeholder${NC}"
        echo "   Configurar App-Specific Password: https://support.google.com/accounts/answer/185833"
        WARNINGS=$((WARNINGS + 1))
    else
        echo -e "${GREEN}✅ SMTP_PASSWORD configurado${NC}"
        PASSED=$((PASSED + 1))
    fi
fi

# ============================================================================
# 6. SCAN FOR HARDCODED SECRETS IN CODE
# ============================================================================
echo ""
echo "🔍 [6/8] Escaneando secretos hardcoded en código..."

HARDCODED_FOUND=0

# Check for common patterns
if grep -r "password.*=.*['\"].*['\"]" "$ROOT" \
    --exclude-dir=node_modules \
    --exclude-dir=.git \
    --exclude-dir=dist \
    --exclude-dir=build \
    --exclude="*.log" \
    --exclude="verify_secrets.sh" \
    --exclude=".env*" \
    > /dev/null 2>&1; then
    
    echo -e "${YELLOW}⚠️  Posibles passwords hardcoded encontrados:${NC}"
    grep -rn "password.*=.*['\"].*['\"]" "$ROOT" \
        --exclude-dir=node_modules \
        --exclude-dir=.git \
        --exclude-dir=dist \
        --exclude-dir=build \
        --exclude="*.log" \
        --exclude="verify_secrets.sh" \
        --exclude=".env*" \
        | head -n 5
    HARDCODED_FOUND=$((HARDCODED_FOUND + 1))
    WARNINGS=$((WARNINGS + 1))
fi

# Check for API keys patterns
if grep -rE "['\"]?[A-Z0-9_]+_API_KEY['\"]?\s*=\s*['\"][a-zA-Z0-9]{20,}['\"]" "$ROOT" \
    --exclude-dir=node_modules \
    --exclude-dir=.git \
    > /dev/null 2>&1; then
    
    echo -e "${YELLOW}⚠️  Posibles API keys hardcoded:${NC}"
    grep -rnE "['\"]?[A-Z0-9_]+_API_KEY['\"]?\s*=\s*['\"][a-zA-Z0-9]{20,}['\"]" "$ROOT" \
        --exclude-dir=node_modules \
        --exclude-dir=.git \
        | head -n 3
    HARDCODED_FOUND=$((HARDCODED_FOUND + 1))
    WARNINGS=$((WARNINGS + 1))
fi

if [ $HARDCODED_FOUND -eq 0 ]; then
    echo -e "${GREEN}✅ No se encontraron secretos hardcoded obvios${NC}"
    PASSED=$((PASSED + 1))
fi

# ============================================================================
# 7. CHECK FOR SECRETS IN GIT HISTORY
# ============================================================================
echo ""
echo "📜 [7/8] Verificando secretos en Git history..."

if [ -d "$ROOT/.git" ]; then
    # Check if .env was ever committed
    if git -C "$ROOT" log --all --full-history -- ".env" > /dev/null 2>&1; then
        ENV_COMMITS=$(git -C "$ROOT" log --all --full-history --oneline -- ".env" 2>/dev/null | wc -l | tr -d ' ')
        
        if [ "$ENV_COMMITS" -gt 0 ]; then
            echo -e "${RED}❌ CRITICAL: .env fue commiteado $ENV_COMMITS veces en el pasado${NC}"
            echo "   Secretos pueden estar expuestos en historial de Git"
            echo "   Acción requerida: git filter-branch o BFG Repo-Cleaner"
            echo ""
            echo "   Últimos commits con .env:"
            git -C "$ROOT" log --all --full-history --oneline -- ".env" | head -n 3
            CRITICAL=$((CRITICAL + 1))
        else
            echo -e "${GREEN}✅ .env nunca fue commiteado${NC}"
            PASSED=$((PASSED + 1))
        fi
    else
        echo -e "${GREEN}✅ .env nunca fue commiteado${NC}"
        PASSED=$((PASSED + 1))
    fi
else
    echo -e "${YELLOW}⚠️  Not a git repository, skipping Git history check${NC}"
fi

# ============================================================================
# 8. ENVIRONMENT-SPECIFIC CHECKS
# ============================================================================
echo ""
echo "🌍 [8/8] Verificando configuración por entorno..."

if [ -f "$ROOT/.env" ]; then
    NODE_ENV=$(grep "^NODE_ENV=" "$ROOT/.env" | cut -d'=' -f2 || echo "development")
    SPRING_PROFILES=$(grep "^SPRING_PROFILES_ACTIVE=" "$ROOT/.env" | cut -d'=' -f2 || echo "development")
    
    if [[ "$NODE_ENV" == "production" ]] || [[ "$SPRING_PROFILES" == "production" ]]; then
        echo -e "${BLUE}ℹ️  Environment: PRODUCTION${NC}"
        echo ""
        
        # Production-specific checks
        PROD_ISSUES=0
        
        # Check if using development password
        if [[ "$DB_PASSWORD" == "admin123" ]]; then
            echo -e "${RED}❌ CRITICAL: Usando DB_PASSWORD de desarrollo en producción${NC}"
            CRITICAL=$((CRITICAL + 1))
            PROD_ISSUES=$((PROD_ISSUES + 1))
        fi
        
        # Check if JWT_SECRET looks like dev secret
        if [ ${#JWT_SECRET} -lt 128 ]; then
            echo -e "${RED}❌ CRITICAL: JWT_SECRET muy corto para producción${NC}"
            CRITICAL=$((CRITICAL + 1))
            PROD_ISSUES=$((PROD_ISSUES + 1))
        fi
        
        # Check if using localhost
        DB_HOST=$(grep "^DB_HOST=" "$ROOT/.env" | cut -d'=' -f2)
        if [[ "$DB_HOST" == "localhost" ]]; then
            echo -e "${YELLOW}⚠️  DB_HOST=localhost en producción (¿correcto?)${NC}"
            WARNINGS=$((WARNINGS + 1))
        fi
        
        if [ $PROD_ISSUES -eq 0 ]; then
            echo -e "${GREEN}✅ Configuración de producción parece segura${NC}"
            PASSED=$((PASSED + 1))
        fi
    else
        echo -e "${BLUE}ℹ️  Environment: DEVELOPMENT${NC}"
        echo -e "${GREEN}✅ OK usar secretos simples en desarrollo${NC}"
        PASSED=$((PASSED + 1))
    fi
fi

# ============================================================================
# FINAL REPORT
# ============================================================================
echo ""
echo "======================================================"
echo "📊 REPORTE FINAL DE SEGURIDAD"
echo "======================================================"
echo -e "Críticos:   ${RED}$CRITICAL${NC}"
echo -e "Warnings:   ${YELLOW}$WARNINGS${NC}"
echo -e "Passed:     ${GREEN}$PASSED${NC}"
echo ""

TOTAL=$((CRITICAL + WARNINGS + PASSED))
if [ $TOTAL -gt 0 ]; then
    PERCENTAGE=$((PASSED * 100 / TOTAL))
    echo "Score: $PERCENTAGE%"
else
    PERCENTAGE=0
fi

# Recommendations
if [ $CRITICAL -gt 0 ]; then
    echo ""
    echo -e "${RED}⚠️  ACCIONES CRÍTICAS REQUERIDAS:${NC}"
    echo "1. Generar JWT_SECRET seguro:"
    echo "   node -e \"console.log(require('crypto').randomBytes(64).toString('hex'))\""
    echo ""
    echo "2. Agregar .env a .gitignore:"
    echo "   echo '.env' >> .gitignore"
    echo ""
    echo "3. Si .env fue commiteado, limpiar historial:"
    echo "   git filter-branch --force --index-filter \\"
    echo "     'git rm --cached --ignore-unmatch .env' \\"
    echo "     --prune-empty --tag-name-filter cat -- --all"
    echo ""
fi

if [ $WARNINGS -gt 0 ]; then
    echo ""
    echo -e "${YELLOW}⚠️  RECOMENDACIONES:${NC}"
    echo "• Usar secrets manager en producción (AWS/Azure/GCP)"
    echo "• Rotar secretos cada 90 días"
    echo "• Configurar App-Specific Password para SMTP"
    echo "• Revisar código para secretos hardcoded"
fi

echo ""
echo "======================================================"

# Exit code
if [ $CRITICAL -gt 0 ]; then
    echo -e "${RED}❌ VERIFICACIÓN FALLIDA - Issues críticos encontrados${NC}"
    exit 1
elif [ $WARNINGS -gt 5 ]; then
    echo -e "${YELLOW}⚠️  VERIFICACIÓN CON WARNINGS${NC}"
    exit 0
else
    echo -e "${GREEN}✅ VERIFICACIÓN PASSED${NC}"
    exit 0
fi
