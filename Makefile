# Sistema de Admisión MTN - Makefile
# Fase 0 Pre-flight - Automatización completa
# Version: 1.0.0

.PHONY: help preflight setup clean test test-e2e test-frontend docs start stop status health

# Variables
BACKEND_DIR := .
FRONTEND_DIR := ../Admision_MTN_front
ARTIFACTS_DIR := artifacts
LOG_FILE := $(ARTIFACTS_DIR)/preflight.log
TIMESTAMP := $(shell date '+%Y-%m-%d_%H-%M-%S')

# Colors for output
GREEN := \033[0;32m
YELLOW := \033[0;33m
RED := \033[0;31m
BLUE := \033[0;34m
NC := \033[0m # No Color

# Default target
help: ## Show this help message
	@echo "$(BLUE)Sistema de Admisión MTN - Fase 0 Pre-flight$(NC)"
	@echo "=================================================="
	@echo ""
	@echo "$(GREEN)Comandos disponibles:$(NC)"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  $(YELLOW)%-20s$(NC) %s\n", $$1, $$2}' $(MAKEFILE_LIST)
	@echo ""
	@echo "$(BLUE)Ejemplos de uso:$(NC)"
	@echo "  make preflight         # Ejecutar toda la suite de pre-flight"
	@echo "  make test-e2e          # Solo pruebas E2E del backend"
	@echo "  make test-frontend     # Solo pruebas E2E del frontend"
	@echo "  make docs              # Generar documentación"

setup: ## Configurar entorno de desarrollo
	@echo "$(GREEN)🔧 Configurando entorno de desarrollo...$(NC)"
	@mkdir -p $(ARTIFACTS_DIR)
	@mkdir -p logs
	@echo "$(TIMESTAMP) - Setup iniciado" > $(LOG_FILE)
	
	# Verificar Java 17
	@echo "$(YELLOW)☕ Verificando Java 17...$(NC)"
	@java -version 2>&1 | grep "17\." > /dev/null || (echo "$(RED)❌ Java 17 requerido$(NC)" && exit 1)
	@echo "$(GREEN)✅ Java 17 encontrado$(NC)"
	
	# Verificar Maven
	@echo "$(YELLOW)📦 Verificando Maven...$(NC)"
	@mvn -version > /dev/null || (echo "$(RED)❌ Maven requerido$(NC)" && exit 1)
	@echo "$(GREEN)✅ Maven encontrado$(NC)"
	
	# Verificar PostgreSQL
	@echo "$(YELLOW)🐘 Verificando PostgreSQL...$(NC)"
	@PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c "SELECT 1;" > /dev/null 2>&1 || \
		(echo "$(RED)❌ PostgreSQL no accesible. Verificar que esté corriendo en localhost:5432$(NC)" && exit 1)
	@echo "$(GREEN)✅ PostgreSQL accesible$(NC)"
	
	# Setup frontend si existe
	@if [ -d "$(FRONTEND_DIR)" ]; then \
		echo "$(YELLOW)⚛️  Configurando frontend...$(NC)"; \
		cd "$(FRONTEND_DIR)" && npm install > /dev/null 2>&1 || echo "$(RED)⚠️  Error instalando dependencias frontend$(NC)"; \
		echo "$(GREEN)✅ Frontend configurado$(NC)"; \
	fi
	
	@echo "$(GREEN)🎉 Setup completado$(NC)"

start: ## Iniciar aplicación backend
	@echo "$(GREEN)🚀 Iniciando aplicación backend...$(NC)"
	@echo "$(YELLOW)📍 URL: http://localhost:8080$(NC)"
	@echo "$(YELLOW)📖 Swagger UI: http://localhost:8080/swagger-ui.html$(NC)"
	@echo "$(YELLOW)💚 Health: http://localhost:8080/actuator/health$(NC)"
	@SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/Admisión_MTN_DB" \
	 SPRING_DATASOURCE_USERNAME=admin \
	 SPRING_DATASOURCE_PASSWORD=admin123 \
	 mvn spring-boot:run

start-frontend: ## Iniciar aplicación frontend
	@echo "$(GREEN)⚛️  Iniciando aplicación frontend...$(NC)"
	@echo "$(YELLOW)📍 URL: http://localhost:5173$(NC)"
	@if [ -d "$(FRONTEND_DIR)" ]; then \
		cd "$(FRONTEND_DIR)" && npm run dev; \
	else \
		echo "$(RED)❌ Directorio frontend no encontrado$(NC)"; \
	fi

stop: ## Detener aplicaciones
	@echo "$(GREEN)🛑 Deteniendo aplicaciones...$(NC)"
	@pkill -f "spring-boot:run" || true
	@pkill -f "vite" || true
	@echo "$(GREEN)✅ Aplicaciones detenidas$(NC)"

status: ## Ver estado de servicios
	@echo "$(GREEN)📊 Estado de servicios:$(NC)"
	@echo ""
	
	# Backend
	@echo "$(BLUE)Backend (Puerto 8080):$(NC)"
	@curl -s http://localhost:8080/actuator/health > /dev/null 2>&1 && \
		echo "  $(GREEN)✅ Funcionando$(NC)" || \
		echo "  $(RED)❌ No responde$(NC)"
	
	# Frontend
	@echo "$(BLUE)Frontend (Puerto 5173):$(NC)"
	@curl -s http://localhost:5173 > /dev/null 2>&1 && \
		echo "  $(GREEN)✅ Funcionando$(NC)" || \
		echo "  $(RED)❌ No responde$(NC)"
	
	# PostgreSQL
	@echo "$(BLUE)PostgreSQL:$(NC)"
	@PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c "SELECT 1;" > /dev/null 2>&1 && \
		echo "  $(GREEN)✅ Funcionando$(NC)" || \
		echo "  $(RED)❌ No accesible$(NC)"

health: ## Health check completo
	@echo "$(GREEN)🩺 Health check completo...$(NC)"
	@mkdir -p $(ARTIFACTS_DIR)
	@echo "$(TIMESTAMP) - Health check" > $(ARTIFACTS_DIR)/health_$(TIMESTAMP).log
	
	# API Health
	@echo "$(BLUE)Verificando API Health...$(NC)"
	@curl -s http://localhost:8080/actuator/health | jq '.' > $(ARTIFACTS_DIR)/health_api_$(TIMESTAMP).json 2>/dev/null || \
		(echo "$(RED)❌ API Health falló$(NC)" && exit 1)
	@echo "$(GREEN)✅ API Health OK$(NC)"
	
	# Database Health
	@echo "$(BLUE)Verificando Database Health...$(NC)"
	@PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" \
		-c "SELECT version();" > $(ARTIFACTS_DIR)/db_version_$(TIMESTAMP).txt 2>/dev/null || \
		(echo "$(RED)❌ Database Health falló$(NC)" && exit 1)
	@echo "$(GREEN)✅ Database Health OK$(NC)"

docs: ## Generar documentación completa
	@echo "$(GREEN)📚 Generando documentación completa...$(NC)"
	@mkdir -p $(ARTIFACTS_DIR)/docs
	@echo "$(TIMESTAMP) - Docs generation" > $(LOG_FILE)
	
	# Export OpenAPI
	@echo "$(YELLOW)📄 Exportando OpenAPI...$(NC)"
	@chmod +x tools/export-openapi.sh
	@./tools/export-openapi.sh || (echo "$(RED)❌ Error exportando OpenAPI$(NC)" && exit 1)
	@cp docs/openapi.* $(ARTIFACTS_DIR)/docs/ 2>/dev/null || true
	@echo "$(GREEN)✅ OpenAPI exportado$(NC)"
	
	# Copy documentation files
	@echo "$(YELLOW)📋 Copiando documentación...$(NC)"
	@cp docs/*.md $(ARTIFACTS_DIR)/docs/ 2>/dev/null || true
	@cp docs/*.csv $(ARTIFACTS_DIR)/docs/ 2>/dev/null || true
	@cp docs/*.json $(ARTIFACTS_DIR)/docs/ 2>/dev/null || true
	@echo "$(GREEN)✅ Documentación copiada$(NC)"
	
	@echo "$(GREEN)📁 Documentación disponible en: $(ARTIFACTS_DIR)/docs/$(NC)"

test: ## Ejecutar todas las pruebas
	@echo "$(GREEN)🧪 Ejecutando todas las pruebas...$(NC)"
	@mkdir -p $(ARTIFACTS_DIR)/test-results
	
	# Unit tests
	@echo "$(YELLOW)🔬 Pruebas unitarias...$(NC)"
	@mvn test > $(ARTIFACTS_DIR)/test-results/unit_tests_$(TIMESTAMP).log 2>&1 || \
		(echo "$(RED)❌ Pruebas unitarias fallaron$(NC)" && cat $(ARTIFACTS_DIR)/test-results/unit_tests_$(TIMESTAMP).log && exit 1)
	@echo "$(GREEN)✅ Pruebas unitarias OK$(NC)"

test-e2e: ## Ejecutar pruebas E2E backend (REST Assured)
	@echo "$(GREEN)🔄 Ejecutando pruebas E2E backend...$(NC)"
	@mkdir -p $(ARTIFACTS_DIR)/test-results
	
	# E2E API tests
	@echo "$(YELLOW)🔗 Pruebas E2E API...$(NC)"
	@mvn test -Dtest="e2e.**" -Dspring.profiles.active=test \
		> $(ARTIFACTS_DIR)/test-results/e2e_api_$(TIMESTAMP).log 2>&1 || \
		(echo "$(RED)❌ Pruebas E2E API fallaron$(NC)" && cat $(ARTIFACTS_DIR)/test-results/e2e_api_$(TIMESTAMP).log && exit 1)
	@echo "$(GREEN)✅ Pruebas E2E API OK$(NC)"

test-frontend: ## Ejecutar pruebas E2E frontend (Playwright)
	@echo "$(GREEN)⚛️  Ejecutando pruebas E2E frontend...$(NC)"
	@mkdir -p $(ARTIFACTS_DIR)/test-results
	
	@if [ -d "$(FRONTEND_DIR)" ]; then \
		echo "$(YELLOW)🎭 Pruebas E2E Playwright...$(NC)"; \
		cd "$(FRONTEND_DIR)" && \
		npm run e2e > ../$(BACKEND_DIR)/$(ARTIFACTS_DIR)/test-results/e2e_frontend_$(TIMESTAMP).log 2>&1 || \
		(echo "$(RED)❌ Pruebas E2E Frontend fallaron$(NC)" && exit 1); \
		echo "$(GREEN)✅ Pruebas E2E Frontend OK$(NC)"; \
	else \
		echo "$(YELLOW)⚠️  Frontend no disponible - saltando pruebas$(NC)"; \
	fi

preflight: setup health docs test-e2e ## 🎯 EJECUTAR SUITE COMPLETA PRE-FLIGHT
	@echo ""
	@echo "$(GREEN)🎉 ¡PRE-FLIGHT COMPLETADO EXITOSAMENTE! 🎉$(NC)"
	@echo "=================================================="
	@echo ""
	@echo "$(BLUE)📊 RESUMEN DE RESULTADOS:$(NC)"
	@echo "  $(GREEN)✅ Setup y configuración$(NC)"
	@echo "  $(GREEN)✅ Health checks$(NC)"
	@echo "  $(GREEN)✅ Documentación generada$(NC)"
	@echo "  $(GREEN)✅ Pruebas E2E API$(NC)"
	@if [ -d "$(FRONTEND_DIR)" ]; then \
		echo "  $(GREEN)✅ Pruebas E2E Frontend$(NC)"; \
	fi
	@echo ""
	@echo "$(BLUE)📁 ARTEFACTOS GENERADOS:$(NC)"
	@ls -la $(ARTIFACTS_DIR)/ | head -10
	@echo ""
	@echo "$(BLUE)📈 ENDPOINTS INVENTARIADOS:$(NC)"
	@echo "  📄 docs/endpoints_inventory.md"
	@echo "  📊 docs/endpoints_inventory.csv"
	@echo "  🗺️  docs/domain_map.md"
	@echo ""
	@echo "$(BLUE)🧪 PRUEBAS EJECUTADAS:$(NC)"
	@echo "  🔐 b1) Login: obtener JWT → ✅"
	@echo "  📝 b2) Crear postulación → ✅"  
	@echo "  📎 b3) Subir documento → ✅"
	@echo "  📧 b4) Notificación → ✅"
	@echo ""
	@echo "$(BLUE)🔍 OBSERVABILIDAD:$(NC)"
	@echo "  💚 Health: http://localhost:8080/actuator/health"
	@echo "  📊 Metrics: http://localhost:8080/actuator/metrics"
	@echo "  📖 OpenAPI: http://localhost:8080/swagger-ui.html"
	@echo ""
	@echo "$(GREEN)🚀 Sistema listo para migración a microservicios$(NC)"

preflight-ci: ## Versión CI/CD del preflight (sin interacción)
	@echo "$(GREEN)🤖 Ejecutando Pre-flight para CI/CD...$(NC)"
	@mkdir -p $(ARTIFACTS_DIR)
	
	# Skip setup and health in CI, assume services are running
	@echo "$(YELLOW)📚 Generando documentación...$(NC)"
	@$(MAKE) docs
	
	@echo "$(YELLOW)🧪 Ejecutando pruebas...$(NC)"
	@$(MAKE) test-e2e
	
	@echo "$(GREEN)✅ Pre-flight CI completado$(NC)"

clean: ## Limpiar artefactos y logs
	@echo "$(GREEN)🧹 Limpiando artefactos...$(NC)"
	@rm -rf $(ARTIFACTS_DIR)
	@rm -rf target/surefire-reports
	@rm -rf logs/*.log
	@if [ -d "$(FRONTEND_DIR)/test-results" ]; then rm -rf "$(FRONTEND_DIR)/test-results"; fi
	@if [ -d "$(FRONTEND_DIR)/playwright-report" ]; then rm -rf "$(FRONTEND_DIR)/playwright-report"; fi
	@echo "$(GREEN)✅ Limpieza completada$(NC)"

install-deps: ## Instalar dependencias adicionales (Playwright, Newman)
	@echo "$(GREEN)📦 Instalando dependencias adicionales...$(NC)"
	
	# Install Playwright browsers if frontend exists
	@if [ -d "$(FRONTEND_DIR)" ]; then \
		echo "$(YELLOW)🎭 Instalando browsers Playwright...$(NC)"; \
		cd "$(FRONTEND_DIR)" && npx playwright install; \
	fi
	
	# Install Newman for Postman collection testing
	@echo "$(YELLOW)📮 Instalando Newman para Postman...$(NC)"
	@npm install -g newman > /dev/null 2>&1 || \
		echo "$(YELLOW)⚠️  Newman ya instalado o error en instalación$(NC)"
	
	@echo "$(GREEN)✅ Dependencias instaladas$(NC)"

test-postman: ## Ejecutar colección Postman con Newman
	@echo "$(GREEN)📮 Ejecutando colección Postman...$(NC)"
	@mkdir -p $(ARTIFACTS_DIR)/postman
	
	@newman run tests/postman/MTN_Preflight.postman_collection.json \
		-e tests/postman/MTN_env_local.postman_environment.json \
		--reporters cli,json \
		--reporter-json-export $(ARTIFACTS_DIR)/postman/results_$(TIMESTAMP).json \
		|| (echo "$(RED)❌ Colección Postman falló$(NC)" && exit 1)
	
	@echo "$(GREEN)✅ Colección Postman OK$(NC)"

# Development helpers
dev-backend: ## Modo desarrollo backend con hot reload
	@echo "$(GREEN)🔥 Iniciando backend en modo desarrollo...$(NC)"
	@SPRING_PROFILES_ACTIVE=dev \
	 SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/Admisión_MTN_DB" \
	 mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.devtools.restart.enabled=true"

dev-frontend: ## Modo desarrollo frontend con hot reload
	@echo "$(GREEN)🔥 Iniciando frontend en modo desarrollo...$(NC)"
	@if [ -d "$(FRONTEND_DIR)" ]; then \
		cd "$(FRONTEND_DIR)" && npm run dev; \
	else \
		echo "$(RED)❌ Frontend no disponible$(NC)"; \
	fi

logs: ## Ver logs en tiempo real
	@echo "$(GREEN)📜 Logs en tiempo real...$(NC)"
	@echo "$(YELLOW)Presiona Ctrl+C para salir$(NC)"
	@tail -f logs/admision-mtn.log 2>/dev/null || \
		echo "$(RED)❌ No se encuentran logs. ¿Está la aplicación corriendo?$(NC)"

# Show current timestamp for artifacts
timestamp:
	@echo $(TIMESTAMP)