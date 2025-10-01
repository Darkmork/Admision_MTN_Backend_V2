# MTN Admission System - Runbooks de Incidentes

## Índice de Runbooks

Este directorio contiene los runbooks para la respuesta a incidentes del Sistema de Admisión MTN, organizados por tipo de alerta y servicio.

### 📚 Runbooks por Categoría

#### 🔴 Críticos (PagerDuty)
- [SLO Latencia P99 Alto](./slo-latency-p99.md) - P99 > 2 segundos
- [SLO Error Rate Alto](./slo-error-rate.md) - Error rate > 5%
- [RabbitMQ DLQ No Vacía](./rabbitmq-dlq.md) - Mensajes en Dead Letter Queue
- [Servicio Caído](./service-down.md) - Servicio no responde

#### 🟡 Advertencias (Slack)
- [CPU Alto](./high-cpu.md) - CPU > 80%
- [Memoria Alta](./high-memory.md) - Memoria > 85%
- [Disco Lleno](./disk-full.md) - Disco > 90%
- [Cola Larga](./queue-backlog.md) - Backlog > umbral

#### 🔵 Informativos (Jira)
- [Autenticación Fallida](./auth-failures.md) - Fallos de autenticación aumentan
- [Reinicio de Pod](./pod-restarts.md) - Pods reiniciando frecuentemente

### 🏥 Procedimientos de Emergencia

#### Escalación de Incidentes
1. **Nivel 1 (0-15 min)**: SRE/DevOps on-call
2. **Nivel 2 (15-30 min)**: Lead Engineer + Product Manager
3. **Nivel 3 (30+ min)**: Director TI + Comunicaciones

#### Contactos de Emergencia
- **SRE On-call**: +56 9 XXXX XXXX
- **Lead Engineer**: nombre@mtn.cl
- **Director TI**: director@mtn.cl
- **Comunicaciones**: comunicaciones@mtn.cl

### 🛠 Herramientas y Accesos

#### Dashboards Principales
- **Golden Signals**: http://grafana.mtn.cl/d/mtn-golden-signals
- **RabbitMQ**: http://grafana.mtn.cl/d/mtn-rabbitmq
- **Traces**: http://jaeger.mtn.cl:16686

#### Comandos Útiles
```bash
# Verificar estado de servicios
kubectl get pods -n mtn-admission

# Ver logs recientes
kubectl logs -f deployment/mtn-admission-backend -n mtn-admission

# Consultar métricas
curl http://prometheus.mtn.cl:9090/api/v1/query?query=up{job="mtn-admission"}

# Verificar RabbitMQ
curl -u admin:password http://rabbitmq.mtn.cl:15672/api/overview
```

### 📋 Plantilla de Respuesta

#### Para cada incidente:

1. **Reconocimiento** (< 5 min)
   - [ ] Confirmar alerta en Grafana
   - [ ] Verificar scope del impacto
   - [ ] Notificar inicio de investigación

2. **Investigación** (< 15 min)
   - [ ] Revisar dashboards relevantes
   - [ ] Consultar logs correlacionados
   - [ ] Identificar traces afectados
   - [ ] Determinar causa raíz

3. **Mitigación** (< 30 min)
   - [ ] Aplicar solución temporal
   - [ ] Verificar restauración de servicio
   - [ ] Monitorear estabilidad

4. **Resolución**
   - [ ] Implementar fix definitivo
   - [ ] Actualizar documentación
   - [ ] Post-mortem si es crítico

### 📊 Métricas de Respuesta

#### SLOs de Respuesta
- **Reconocimiento**: < 5 minutos para alertas críticas
- **Mitigación**: < 30 minutos para alertas críticas  
- **Resolución**: < 2 horas para alertas críticas

#### Seguimiento
- Tiempo medio de respuesta por tipo de alerta
- % de incidentes resueltos dentro de SLO
- Número de escalaciones por mes

### 🇨🇱 Consideraciones Chilenas

#### Horarios de Operación
- **Horario Escolar**: 08:00 - 18:00 CLT (prioridad máxima)
- **Proceso de Admisión**: 24/7 durante período crítico (Nov-Ene)
- **Mantenimiento**: Domingos 02:00 - 06:00 CLT

#### Cumplimiento Normativo
- Logs de auditoría: 6 meses mínimo
- Datos PII: enmascaramiento obligatorio
- Notificación de incidentes: CPLT si aplica

### 🔄 Actualización de Runbooks

- **Responsable**: Equipo SRE
- **Frecuencia**: Mensual o post-incidente mayor
- **Proceso**: PR + revisión técnica + aprobación lead

---

## Contacto y Soporte

**Equipo SRE MTN**  
Email: sre@mtn.cl  
Slack: #sre-mtn-admission  
On-call: +56 9 XXXX XXXX  

*Última actualización: $(date "+%Y-%m-%d")*