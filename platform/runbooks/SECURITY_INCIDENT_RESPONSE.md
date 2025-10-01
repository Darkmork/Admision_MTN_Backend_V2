# 🚨 Manual de Respuesta a Incidentes de Seguridad
# Sistema de Admisión MTN - Colegio Monte Tabor y Nazaret

> **IMPORTANTE**: Este documento contiene procedimientos críticos para la respuesta a incidentes de seguridad. Debe estar accesible 24/7 y actualizado según normativas chilenas.

---

## 📋 Información de Contacto de Emergencia

### Equipo de Seguridad MTN
- **Coordinador de Seguridad**: +56 9 XXXX XXXX
- **Administrador de Sistemas**: +56 9 XXXX XXXX  
- **Director de TI**: +56 9 XXXX XXXX
- **Email Grupal**: security-emergency@mtn.cl

### Autoridades Chilenas
- **Policía de Investigaciones (PDI) - Cibercrimen**: +56 2 2708 0000
- **CSIRT Chile**: incidentes@csirt.gob.cl
- **Superintendencia de Educación**: +56 2 2406 3600

### Proveedores Críticos
- **AWS Support**: +1 206 266 4064
- **HashiCorp Vault Support**: vault-support@hashicorp.com
- **Keycloak Support**: keycloak-support@redhat.com

---

## 🔴 Procedimientos de Respuesta Inmediata

### 1. Clasificación de Severidad

#### CRÍTICO (Respuesta: 0-15 minutos)
- Acceso no autorizado a datos de estudiantes/familias
- Compromiso de credenciales administrativas
- Ataque DDoS activo
- Filtración de datos PII chilenos
- Compromiso de certificados mTLS
- Vault comprometido o sellado

#### ALTO (Respuesta: 15-30 minutos)  
- Múltiples fallos de autenticación
- Intentos de escalación de privilegios
- Certificados próximos a expirar
- Anomalías en tráfico de red
- External Secrets Operator fallando

#### MEDIO (Respuesta: 30-60 minutos)
- Rate limiting excedido frecuentemente
- Headers de seguridad faltantes
- Rotación de claves retrasada
- Actividad sospechosa de IP

#### BAJO (Respuesta: 1-4 horas)
- Logs de auditoría incompletos  
- Configuración de seguridad subóptima
- Métricas de seguridad no disponibles

---

## ⚡ Respuesta a Incidentes Críticos

### 🚨 Acceso No Autorizado Detectado

#### Acciones Inmediatas (0-5 minutos)
1. **Confirmar el incidente**:
   ```bash
   # Revisar logs de autenticación
   kubectl logs -n admissions -l app=api-gateway --since=10m | grep "SECURITY_AUDIT"
   
   # Verificar actividad en Vault
   vault audit list
   tail -f /vault/logs/mtn-admission-audit.log
   ```

2. **Aislar el sistema**:
   ```bash
   # Bloquear IP sospechosa inmediatamente
   kubectl patch networkpolicy deny-suspicious-ip --type='merge' -p='{"spec":{"podSelector":{},"ingress":[{"from":[{"ipBlock":{"cidr":"SUSPICIOUS_IP/32"}}],"action":"Deny"}]}}'
   
   # Revocar tokens JWT comprometidos
   kubectl delete secret application-service-jwt-keys -n admissions
   ```

3. **Notificar autoridades**:
   - Llamar al Coordinador de Seguridad
   - Enviar email a security-emergency@mtn.cl
   - Si involucra datos PII: notificar CSIRT Chile

#### Investigación (5-30 minutos)
1. **Recopilar evidencia**:
   ```bash
   # Exportar logs críticos
   kubectl logs -n admissions --all-containers=true --since=1h > incident-logs-$(date +%Y%m%d-%H%M%S).log
   
   # Snapshot de configuración actual
   kubectl get all,secrets,configmaps -n admissions -o yaml > incident-snapshot-$(date +%Y%m%d-%H%M%S).yaml
   
   # Verificar integridad de Vault
   vault status
   vault audit list
   ```

2. **Análisis de impacto**:
   ```bash
   # Verificar qué datos fueron accedidos
   vault audit -format=json | jq '.request.path' | sort | uniq
   
   # Revisar accesos a base de datos
   psql -h postgres.infrastructure.svc.cluster.local -U admin -d mtn_admission_db -c "SELECT * FROM audit_log WHERE timestamp > NOW() - INTERVAL '1 hour';"
   ```

#### Contención (30-60 minutos)
1. **Regenerar credenciales comprometidas**:
   ```bash
   # Forzar rotación de credenciales de BD
   vault write -force database/rotate-credentials/mtn-application-rw
   
   # Regenerar claves JWT
   vault write -force jwt/rotate/application-service
   
   # Reiniciar pods para usar nuevas credenciales
   kubectl rollout restart deployment/application-service -n admissions
   ```

2. **Validar integridad del sistema**:
   ```bash
   # Verificar mTLS está funcionando
   istioctl authn tls-check api-gateway.admissions.svc.cluster.local
   
   # Validar políticas de autorización
   istioctl authz check api-gateway.admissions.svc.cluster.local
   ```

---

### 🔐 Compromiso de Vault

#### Acciones Inmediatas (0-2 minutos)
```bash
# Verificar estado de Vault
vault status

# Si está comprometido, sellar inmediatamente
vault operator seal
```

#### Recuperación (2-15 minutos)
```bash
# Reiniciar Vault con nueva configuración
kubectl delete pod -l app=vault -n infrastructure

# Esperar a que el pod esté ready
kubectl wait --for=condition=ready pod -l app=vault -n infrastructure --timeout=300s

# Dessellar usando claves maestras
vault operator unseal [KEY_1]
vault operator unseal [KEY_2]  
vault operator unseal [KEY_3]

# Verificar auditoría
vault audit enable file file_path=/vault/logs/emergency-audit.log
```

#### Validación Post-Recuperación
```bash
# Verificar secretos críticos
vault kv get kv/application-service/database
vault read database/creds/mtn-application-rw

# Reiniciar External Secrets Operator
kubectl rollout restart deployment/external-secrets -n external-secrets-system

# Validar que los servicios pueden acceder a secretos
kubectl logs -n admissions -l app=application-service --since=5m | grep -i "secret"
```

---

### 🌐 Ataque DDoS Detectado

#### Identificación (0-2 minutos)
```bash
# Verificar métricas de tráfico
kubectl top pods -n admissions
kubectl top nodes

# Revisar logs de rate limiting
kubectl logs -n admissions -l app=api-gateway | grep "rate_limit_exceeded"
```

#### Mitigación Inmediata (2-5 minutos)
```bash
# Activar rate limiting estricto
kubectl patch configmap api-gateway-config -n admissions --type='merge' -p='{"data":{"RATE_LIMIT_EMERGENCY":"true","EMERGENCY_RATE_LIMIT":"10"}}'

# Reiniciar gateway para aplicar cambios
kubectl rollout restart deployment/api-gateway -n admissions

# Bloquear IPs atacantes (identificadas en logs)
for ip in $(kubectl logs -n admissions -l app=api-gateway | grep rate_limit_exceeded | awk '{print $NF}' | sort | uniq -c | sort -nr | head -5 | awk '{print $2}'); do
  kubectl patch networkpolicy block-ddos-ips --type='merge' -p="{\"spec\":{\"ingress\":[{\"from\":[{\"ipBlock\":{\"cidr\":\"$ip/32\",\"except\":[]}}]}]}}"
done
```

#### Escalación (5-15 minutos)
```bash
# Activar protección DDoS en AWS ALB
aws elbv2 modify-load-balancer-attributes --load-balancer-arn [ALB_ARN] --attributes Key=ddos_protection.enabled,Value=true

# Notificar a AWS Support para protección avanzada
# Activar CloudFlare DDoS protection si está disponible
```

---

## 📊 Dashboards y Monitoreo de Incidentes

### Grafana Dashboards Críticos
- **Security Overview**: https://grafana.mtn.cl/d/security-overview
- **Authentication Metrics**: https://grafana.mtn.cl/d/auth-metrics  
- **Rate Limiting**: https://grafana.mtn.cl/d/rate-limiting
- **Vault Health**: https://grafana.mtn.cl/d/vault-health
- **mTLS Status**: https://grafana.mtn.cl/d/istio-security

### Queries de Prometheus Críticas
```promql
# Fallos de autenticación por IP
rate(gateway_authentication_failures_total[5m]) by (source_ip)

# Actividad sospechosa
gateway_suspicious_activity_total

# Estado de Vault
vault_core_unsealed

# Certificados próximos a expirar  
(istio_certificate_expiration_timestamp - time()) < 86400 * 7

# Rate limits excedidos
rate(gateway_rate_limit_exceeded_total[5m]) by (source_ip, endpoint)
```

---

## 📋 Checklist Post-Incidente

### ✅ Acciones Inmediatas Completadas
- [ ] Incidente contenido y sistemas estabilizados
- [ ] Evidencia recopilada y preservada
- [ ] Autoridades notificadas (si aplica)
- [ ] Credenciales comprometidas rotadas
- [ ] Accesos no autorizados revocados

### ✅ Investigación y Documentación  
- [ ] Causa raíz identificada
- [ ] Cronología del incidente documentada
- [ ] Impacto en datos de estudiantes/familias evaluado
- [ ] Informe de incidente completado
- [ ] Lecciones aprendidas documentadas

### ✅ Recuperación y Mejoras
- [ ] Todos los sistemas funcionando normalmente
- [ ] Monitoreo reforzado implementado
- [ ] Políticas de seguridad actualizadas
- [ ] Equipo notificado de cambios
- [ ] Entrenamiento adicional programado

### ✅ Cumplimiento Normativo
- [ ] Autoridades chilenas notificadas (si aplica)
- [ ] Reporte a Superintendencia de Educación (si afecta datos de estudiantes)
- [ ] Documentación de cumplimiento actualizada
- [ ] Padres/tutores notificados (si datos comprometidos)

---

## 🔗 Enlaces de Referencia

### Documentación Técnica
- [Configuración de Seguridad](./SECURITY_CONFIGURATION.md)
- [Procedimientos de Vault](./VAULT_PROCEDURES.md)
- [Configuración de Istio mTLS](./ISTIO_SECURITY.md)

### Normativas Chilenas
- [Ley 19.628 - Protección de Datos Personales](https://www.bcn.cl/leychile/navegar?idNorma=141599)
- [Circular Superintendencia de Educación sobre Datos](https://www.supereduc.cl)

### Contactos de Soporte 24/7
- **AWS**: +1 206 266 4064
- **HashiCorp**: vault-support@hashicorp.com  
- **Red Hat (Keycloak)**: support@redhat.com

---

> **📅 Última Actualización**: Enero 2024  
> **👤 Responsable**: Coordinador de Seguridad MTN  
> **🔄 Próxima Revisión**: Abril 2024  
> **📍 Ubicación**: https://runbooks.mtn.cl/security/incident-response