{{/*
Expand the name of the chart.
*/}}
{{- define "<service>.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "<service>.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "<service>.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "<service>.labels" -}}
helm.sh/chart: {{ include "<service>.chart" . }}
{{ include "<service>.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- with .Values.commonLabels }}
{{ toYaml . }}
{{- end }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "<service>.selectorLabels" -}}
app.kubernetes.io/name: {{ include "<service>.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "<service>.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "<service>.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Create the name of the priority class to use
*/}}
{{- define "<service>.priorityClassName" -}}
{{- if .Values.priorityClassName }}
{{- .Values.priorityClassName }}
{{- else }}
{{- printf "%s-priority" (include "<service>.fullname" .) }}
{{- end }}
{{- end }}

{{/*
Create image pull secret names
*/}}
{{- define "<service>.imagePullSecrets" -}}
{{- if .Values.image.pullSecrets }}
{{- range $secretName := .Values.image.pullSecrets }}
- name: {{ $secretName }}
{{- end }}
{{- else if .Values.global.imagePullSecrets }}
{{- range $secretName := .Values.global.imagePullSecrets }}
- name: {{ $secretName }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Generate the full image name
*/}}
{{- define "<service>.image" -}}
{{- $registry := .Values.global.imageRegistry | default .Values.image.registry -}}
{{- $repository := .Values.image.repository -}}
{{- $tag := .Values.image.tag | default .Chart.AppVersion -}}
{{- printf "%s/%s:%s" $registry $repository $tag }}
{{- end }}

{{/*
Create environment-specific configuration
*/}}
{{- define "<service>.environment" -}}
{{- .Values.commonLabels.environment | default "development" }}
{{- end }}

{{/*
Create resource limits with intelligent defaults
*/}}
{{- define "<service>.resources" -}}
{{- if .Values.resources }}
{{- toYaml .Values.resources }}
{{- else }}
limits:
  cpu: 500m
  memory: 512Mi
requests:
  cpu: 100m
  memory: 256Mi
{{- end }}
{{- end }}

{{/*
Create Pod Anti-Affinity rules
*/}}
{{- define "<service>.podAntiAffinity" -}}
{{- if eq .Values.podAntiAffinity "hard" }}
requiredDuringSchedulingIgnoredDuringExecution:
- labelSelector:
    matchExpressions:
    - key: app.kubernetes.io/name
      operator: In
      values:
      - {{ include "<service>.name" . }}
  topologyKey: kubernetes.io/hostname
{{- else if eq .Values.podAntiAffinity "soft" }}
preferredDuringSchedulingIgnoredDuringExecution:
- weight: 100
  podAffinityTerm:
    labelSelector:
      matchExpressions:
      - key: app.kubernetes.io/name
        operator: In
        values:
        - {{ include "<service>.name" . }}
    topologyKey: kubernetes.io/hostname
{{- end }}
{{- end }}

{{/*
Generate TLS configuration for ingress
*/}}
{{- define "<service>.ingress.tls" -}}
{{- if .Values.ingress.tls }}
{{- range .Values.ingress.tls }}
- hosts:
  {{- range .hosts }}
  - {{ . | quote }}
  {{- end }}
  secretName: {{ .secretName }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create canary service name for Argo Rollouts
*/}}
{{- define "<service>.canaryServiceName" -}}
{{- printf "%s-canary" (include "<service>.fullname" .) }}
{{- end }}

{{/*
Create stable service name for Argo Rollouts
*/}}
{{- define "<service>.stableServiceName" -}}
{{- printf "%s-stable" (include "<service>.fullname" .) }}
{{- end }}

{{/*
Create monitoring labels
*/}}
{{- define "<service>.monitoringLabels" -}}
app.kubernetes.io/component: microservice
app.kubernetes.io/part-of: mtn-admission-system
monitoring.mtn.cl/scrape: "true"
monitoring.mtn.cl/path: "/actuator/prometheus"
monitoring.mtn.cl/port: "8080"
{{- end }}

{{/*
Create security context with sensible defaults
*/}}
{{- define "<service>.securityContext" -}}
{{- if .Values.securityContext }}
{{- toYaml .Values.securityContext }}
{{- else }}
allowPrivilegeEscalation: false
capabilities:
  drop:
  - ALL
readOnlyRootFilesystem: false
runAsNonRoot: true
runAsUser: 1001
{{- end }}
{{- end }}

{{/*
Create pod security context with sensible defaults
*/}}
{{- define "<service>.podSecurityContext" -}}
{{- if .Values.podSecurityContext }}
{{- toYaml .Values.podSecurityContext }}
{{- else }}
fsGroup: 1001
runAsNonRoot: true
runAsUser: 1001
runAsGroup: 1001
seccompProfile:
  type: RuntimeDefault
{{- end }}
{{- end }}

{{/*
Generate feature flag environment variables
*/}}
{{- define "<service>.featureFlags" -}}
{{- if .Values.featureFlags.enabled }}
- name: FEATURE_FLAGS_ENABLED
  value: "true"
- name: FEATURE_CANARY_TRAFFIC
  value: {{ .Values.featureFlags.canaryTraffic | quote }}
- name: FEATURE_NEW_FEATURES
  value: {{ .Values.featureFlags.newFeatures | quote }}
- name: FEATURE_DEBUG_MODE
  value: {{ .Values.featureFlags.debugMode | quote }}
- name: FEATURE_RATE_LIMITING
  value: {{ .Values.featureFlags.rateLimiting | quote }}
- name: FEATURE_SECURITY_HEADERS
  value: {{ .Values.featureFlags.securityHeaders | quote }}
{{- end }}
{{- end }}