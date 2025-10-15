#!/bin/bash

# ===========================================
# SCRIPT DE CONFIGURACIÓN SEGURA
# Sistema de Admisión MTN
# ===========================================

echo "🔒 CONFIGURACIÓN DE SEGURIDAD - Sistema Admisión MTN"
echo "=================================================="

# Colores para output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Función para generar JWT secret seguro
generate_jwt_secret() {
    echo $(openssl rand -base64 64 | tr -d '\n')
}

# Función para generar password hash BCrypt
generate_bcrypt_hash() {
    echo "Ingresa la password para generar hash BCrypt:"
    read -s password
    echo $(htpasswd -bnBC 10 "" "$password" | tr -d ':\n' | sed 's/^.*://')
}

echo -e "${YELLOW}1. Generando JWT Secret seguro...${NC}"
JWT_SECRET=$(generate_jwt_secret)
echo -e "${GREEN}✅ JWT Secret generado${NC}"

echo -e "${YELLOW}2. Verificando archivo .env...${NC}"
if [ -f ".env" ]; then
    echo -e "${GREEN}✅ Archivo .env existe${NC}"
    
    # Actualizar JWT_SECRET en .env
    sed -i.bak "s/JWT_SECRET=.*/JWT_SECRET=$JWT_SECRET/" .env
    echo -e "${GREEN}✅ JWT_SECRET actualizado en .env${NC}"
else
    echo -e "${RED}❌ Archivo .env no encontrado${NC}"
    echo "Ejecuta este script desde el directorio del backend"
    exit 1
fi

echo -e "${YELLOW}3. Verificando .gitignore...${NC}"
if grep -q "\.env" .gitignore; then
    echo -e "${GREEN}✅ .env está en .gitignore${NC}"
else
    echo ".env" >> .gitignore
    echo -e "${GREEN}✅ .env agregado a .gitignore${NC}"
fi

echo -e "${YELLOW}4. Configuración de Base de Datos...${NC}"
echo "¿Deseas cambiar las credenciales de BD? (y/N):"
read -r change_db
if [[ $change_db =~ ^[Yy]$ ]]; then
    echo "Nueva username de BD:"
    read db_user
    echo "Nueva password de BD:"
    read -s db_pass
    
    sed -i.bak "s/DB_USERNAME=.*/DB_USERNAME=$db_user/" .env
    sed -i.bak "s/DB_PASSWORD=.*/DB_PASSWORD=$db_pass/" .env
    echo -e "${GREEN}✅ Credenciales BD actualizadas${NC}"
fi

echo -e "${YELLOW}5. Configuración de Email...${NC}"
echo "¿Deseas configurar SMTP? (y/N):"
read -r change_smtp
if [[ $change_smtp =~ ^[Yy]$ ]]; then
    echo "SMTP Host (ej: smtp.gmail.com):"
    read smtp_host
    echo "SMTP Port (ej: 587):"
    read smtp_port
    echo "SMTP Username:"
    read smtp_user
    echo "SMTP Password:"
    read -s smtp_pass
    
    sed -i.bak "s/SMTP_HOST=.*/SMTP_HOST=$smtp_host/" .env
    sed -i.bak "s/SMTP_PORT=.*/SMTP_PORT=$smtp_port/" .env
    sed -i.bak "s/SMTP_USERNAME=.*/SMTP_USERNAME=$smtp_user/" .env
    sed -i.bak "s/SMTP_PASSWORD=.*/SMTP_PASSWORD=$smtp_pass/" .env
    echo -e "${GREEN}✅ Configuración SMTP actualizada${NC}"
fi

echo -e "${YELLOW}6. Verificando dependencias...${NC}"
if command -v mvn &> /dev/null; then
    echo -e "${GREEN}✅ Maven encontrado${NC}"
else
    echo -e "${RED}❌ Maven no encontrado${NC}"
    echo "Instala Maven antes de continuar"
fi

if command -v psql &> /dev/null; then
    echo -e "${GREEN}✅ PostgreSQL cliente encontrado${NC}"
else
    echo -e "${YELLOW}⚠️ PostgreSQL cliente no encontrado${NC}"
    echo "Instala PostgreSQL para usar comandos de BD"
fi

echo ""
echo -e "${GREEN}🎉 CONFIGURACIÓN COMPLETADA${NC}"
echo "=================================================="
echo -e "${YELLOW}PRÓXIMOS PASOS:${NC}"
echo "1. Revisa el archivo .env con tus configuraciones"
echo "2. Ejecuta: mvn spring-boot:run"
echo "3. Verifica que todo funcione correctamente"
echo ""
echo -e "${RED}⚠️  IMPORTANTE PARA PRODUCCIÓN:${NC}"
echo "- Cambia todas las passwords por defecto"
echo "- Configura HTTPS/SSL"
echo "- Configura un servicio SMTP profesional"
echo "- Realiza backup de la BD regularmente"
echo "- Nunca commits el archivo .env"
echo ""
echo -e "${YELLOW}JWT Secret generado:${NC}"
echo "$JWT_SECRET"
echo ""
echo -e "${GREEN}¡Configuración de seguridad completada!${NC}"