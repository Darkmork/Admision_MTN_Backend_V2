# Dockerfile para NGINX API Gateway en Railway
FROM nginx:alpine

# Metadata
LABEL maintainer="MTN Admission System"
LABEL description="NGINX API Gateway for Railway microservices"

# Copiar configuración de NGINX
COPY conf/railway-gateway.conf /etc/nginx/nginx.conf

# Crear directorio para logs
RUN mkdir -p /var/log/nginx

# Exponer puerto (Railway usa la variable PORT, por defecto 8080)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

# Iniciar NGINX en foreground
CMD ["nginx", "-g", "daemon off;"]

