package com.desafios.mtn.notificationservice.service;

import com.desafios.mtn.notificationservice.domain.Template;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servicio para renderizado de plantillas con soporte para Mustache y Handlebars
 */
@Service
@Slf4j
public class TemplateRenderingService {

    private final MustacheFactory mustacheFactory;
    private final Handlebars handlebars;
    
    // Patrón para detectar variables en plantillas
    private static final Pattern MUSTACHE_VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([^}\\s]+)\\s*\\}\\}");
    private static final Pattern HANDLEBARS_VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([^}\\s]+)\\s*\\}\\}");
    
    // Zona horaria de Chile
    private static final ZoneId CHILE_ZONE = ZoneId.of("America/Santiago");
    private static final DateTimeFormatter CHILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter CHILE_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public TemplateRenderingService() {
        this.mustacheFactory = new DefaultMustacheFactory();
        this.handlebars = new Handlebars();
        
        // Configurar helpers personalizados para Handlebars
        configureHandlebarsHelpers();
    }

    /**
     * Renderiza una plantilla con las variables proporcionadas
     */
    public RenderedTemplate renderTemplate(Template template, Map<String, Object> variables) {
        if (template == null) {
            throw new IllegalArgumentException("Template cannot be null");
        }

        if (!template.isReadyForUse()) {
            throw new IllegalArgumentException("Template is not ready for use: " + template.getId());
        }

        // Validar variables requeridas
        validateRequiredVariables(template, variables);

        // Enriquecer variables con datos del sistema
        Map<String, Object> enrichedVariables = enrichVariables(variables);

        try {
            RenderedTemplate.RenderedTemplateBuilder builder = RenderedTemplate.builder()
                    .templateId(template.getId())
                    .channel(template.getChannel());

            // Renderizar subject (solo para email)
            if (template.isEmailTemplate() && template.getSubject() != null) {
                String renderedSubject = renderContent(template.getSubject(), enrichedVariables);
                builder.subject(renderedSubject);
            }

            // Renderizar body text
            if (template.hasTextContent()) {
                String renderedText = renderContent(template.getBodyText(), enrichedVariables);
                builder.bodyText(renderedText);
                
                // Para SMS, validar longitud
                if (template.isSmsTemplate() && renderedText.length() > 160) {
                    log.warn("SMS template {} rendered to {} characters, truncating to 160", 
                            template.getId(), renderedText.length());
                    renderedText = truncateSmsText(renderedText);
                    builder.bodyText(renderedText);
                }
            }

            // Renderizar body HTML (solo para email)
            if (template.hasHtmlContent()) {
                String renderedHtml = renderContent(template.getBodyHtml(), enrichedVariables);
                builder.bodyHtml(renderedHtml);
            }

            RenderedTemplate result = builder.build();
            
            log.debug("Template {} rendered successfully for channel {}", 
                     template.getId(), template.getChannel());
            
            return result;

        } catch (Exception e) {
            log.error("Failed to render template {}: {}", template.getId(), e.getMessage(), e);
            throw new TemplateRenderingException("Failed to render template: " + template.getId(), e);
        }
    }

    /**
     * Renderiza contenido específico con variables
     */
    public String renderContent(String content, Map<String, Object> variables) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }

        Map<String, Object> enrichedVariables = enrichVariables(variables);

        try {
            // Intentar con Handlebars primero (más funcionalidad)
            if (containsHandlebarsFeatures(content)) {
                return renderWithHandlebars(content, enrichedVariables);
            } else {
                // Usar Mustache para casos simples
                return renderWithMustache(content, enrichedVariables);
            }
        } catch (Exception e) {
            log.error("Failed to render content: {}", e.getMessage(), e);
            throw new TemplateRenderingException("Failed to render content", e);
        }
    }

    /**
     * Extrae las variables de una plantilla
     */
    public Set<String> extractVariables(String content) {
        if (content == null) {
            return Collections.emptySet();
        }

        Set<String> variables = new HashSet<>();
        
        // Buscar variables Mustache/Handlebars
        Matcher matcher = MUSTACHE_VARIABLE_PATTERN.matcher(content);
        while (matcher.find()) {
            String variable = matcher.group(1).trim();
            // Filtrar helpers y expresiones complejas
            if (!variable.contains(" ") && !variable.startsWith("#") && !variable.startsWith("/")) {
                variables.add(variable);
            }
        }
        
        return variables;
    }

    /**
     * Extrae variables de una plantilla completa
     */
    public Set<String> extractTemplateVariables(Template template) {
        Set<String> variables = new HashSet<>();
        
        if (template.getSubject() != null) {
            variables.addAll(extractVariables(template.getSubject()));
        }
        
        if (template.getBodyText() != null) {
            variables.addAll(extractVariables(template.getBodyText()));
        }
        
        if (template.getBodyHtml() != null) {
            variables.addAll(extractVariables(template.getBodyHtml()));
        }
        
        return variables;
    }

    /**
     * Valida que todas las variables requeridas estén presentes
     */
    public void validateRequiredVariables(Template template, Map<String, Object> variables) {
        if (template.getVariables() == null || template.getVariables().isEmpty()) {
            return;
        }

        List<String> missingVariables = new ArrayList<>();
        for (String requiredVar : template.getVariables()) {
            if (!variables.containsKey(requiredVar) || variables.get(requiredVar) == null) {
                missingVariables.add(requiredVar);
            }
        }

        if (!missingVariables.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("Missing required variables for template %s: %s", 
                                 template.getId(), missingVariables));
        }
    }

    /**
     * Verifica si el contenido contiene características específicas de Handlebars
     */
    private boolean containsHandlebarsFeatures(String content) {
        return content.contains("{{#if") || 
               content.contains("{{#each") || 
               content.contains("{{#with") ||
               content.contains("{{formatDate") ||
               content.contains("{{formatChileanRut");
    }

    /**
     * Renderiza con motor Mustache
     */
    private String renderWithMustache(String content, Map<String, Object> variables) {
        try {
            Mustache mustache = mustacheFactory.compile(new StringReader(content), "template");
            StringWriter writer = new StringWriter();
            mustache.execute(writer, variables);
            return writer.toString();
        } catch (IOException e) {
            throw new TemplateRenderingException("Mustache rendering failed", e);
        }
    }

    /**
     * Renderiza con motor Handlebars
     */
    private String renderWithHandlebars(String content, Map<String, Object> variables) {
        try {
            com.github.jknack.handlebars.Template template = handlebars.compileInline(content);
            return template.apply(variables);
        } catch (IOException e) {
            throw new TemplateRenderingException("Handlebars rendering failed", e);
        }
    }

    /**
     * Enriquece las variables con datos del sistema
     */
    private Map<String, Object> enrichVariables(Map<String, Object> originalVariables) {
        Map<String, Object> enriched = new HashMap<>(originalVariables != null ? originalVariables : Collections.emptyMap());
        
        // Agregar variables del sistema
        Instant now = Instant.now();
        enriched.put("fecha_actual", CHILE_DATE_FORMATTER.format(now.atZone(CHILE_ZONE)));
        enriched.put("fecha_actual_completa", CHILE_DATETIME_FORMATTER.format(now.atZone(CHILE_ZONE)));
        enriched.put("timestamp", now.toString());
        enriched.put("sistema", "Sistema de Admisión MTN");
        enriched.put("colegio_nombre", "Colegio Monte Tabor y Nazaret");
        enriched.put("año_actual", String.valueOf(now.atZone(CHILE_ZONE).getYear()));
        
        return enriched;
    }

    /**
     * Configura helpers personalizados para Handlebars
     */
    private void configureHandlebarsHelpers() {
        // Helper para formatear fechas
        handlebars.registerHelper("formatDate", new Helper<Object>() {
            @Override
            public String apply(Object context, Options options) throws IOException {
                if (context == null) return "";
                
                try {
                    Instant instant;
                    if (context instanceof Instant) {
                        instant = (Instant) context;
                    } else {
                        instant = Instant.parse(context.toString());
                    }
                    
                    String format = options.param(0, "dd/MM/yyyy");
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                    return formatter.format(instant.atZone(CHILE_ZONE));
                } catch (Exception e) {
                    log.warn("Failed to format date {}: {}", context, e.getMessage());
                    return context.toString();
                }
            }
        });

        // Helper para formatear RUT chileno
        handlebars.registerHelper("formatChileanRut", new Helper<String>() {
            @Override
            public String apply(String rut, Options options) throws IOException {
                if (rut == null || rut.trim().isEmpty()) return "";
                
                // Limpiar RUT
                String cleaned = rut.replaceAll("[^0-9Kk]", "").toUpperCase();
                if (cleaned.length() < 2) return rut;
                
                // Formatear como 12.345.678-9
                String number = cleaned.substring(0, cleaned.length() - 1);
                String verifier = cleaned.substring(cleaned.length() - 1);
                
                StringBuilder formatted = new StringBuilder();
                for (int i = number.length() - 1, j = 0; i >= 0; i--, j++) {
                    if (j > 0 && j % 3 == 0) {
                        formatted.insert(0, ".");
                    }
                    formatted.insert(0, number.charAt(i));
                }
                formatted.append("-").append(verifier);
                
                return formatted.toString();
            }
        });

        // Helper para capitalizar texto
        handlebars.registerHelper("capitalize", new Helper<String>() {
            @Override
            public String apply(String text, Options options) throws IOException {
                if (text == null || text.isEmpty()) return "";
                return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
            }
        });

        // Helper condicional para verificar igualdad
        handlebars.registerHelper("ifEquals", new Helper<Object>() {
            @Override
            public String apply(Object value1, Options options) throws IOException {
                Object value2 = options.param(0);
                if (Objects.equals(value1, value2)) {
                    return options.fn();
                } else {
                    return options.inverse();
                }
            }
        });
    }

    /**
     * Trunca texto de SMS manteniendo palabras completas cuando es posible
     */
    private String truncateSmsText(String text) {
        if (text.length() <= 160) {
            return text;
        }
        
        // Intentar truncar en un espacio
        int lastSpace = text.lastIndexOf(' ', 157);
        if (lastSpace > 140) { // Solo si no queda muy corto
            return text.substring(0, lastSpace) + "...";
        }
        
        // Truncar directamente
        return text.substring(0, 157) + "...";
    }

    // ======================
    // CLASSES & EXCEPTIONS
    // ======================

    /**
     * Resultado del renderizado de una plantilla
     */
    public static class RenderedTemplate {
        private final String templateId;
        private final Template.NotificationChannel channel;
        private final String subject;
        private final String bodyText;
        private final String bodyHtml;

        private RenderedTemplate(RenderedTemplateBuilder builder) {
            this.templateId = builder.templateId;
            this.channel = builder.channel;
            this.subject = builder.subject;
            this.bodyText = builder.bodyText;
            this.bodyHtml = builder.bodyHtml;
        }

        public static RenderedTemplateBuilder builder() {
            return new RenderedTemplateBuilder();
        }

        // Getters
        public String getTemplateId() { return templateId; }
        public Template.NotificationChannel getChannel() { return channel; }
        public String getSubject() { return subject; }
        public String getBodyText() { return bodyText; }
        public String getBodyHtml() { return bodyHtml; }
        
        public boolean isEmailTemplate() { return channel == Template.NotificationChannel.email; }
        public boolean isSmsTemplate() { return channel == Template.NotificationChannel.sms; }
        
        public static class RenderedTemplateBuilder {
            private String templateId;
            private Template.NotificationChannel channel;
            private String subject;
            private String bodyText;
            private String bodyHtml;

            public RenderedTemplateBuilder templateId(String templateId) {
                this.templateId = templateId;
                return this;
            }

            public RenderedTemplateBuilder channel(Template.NotificationChannel channel) {
                this.channel = channel;
                return this;
            }

            public RenderedTemplateBuilder subject(String subject) {
                this.subject = subject;
                return this;
            }

            public RenderedTemplateBuilder bodyText(String bodyText) {
                this.bodyText = bodyText;
                return this;
            }

            public RenderedTemplateBuilder bodyHtml(String bodyHtml) {
                this.bodyHtml = bodyHtml;
                return this;
            }

            public RenderedTemplate build() {
                return new RenderedTemplate(this);
            }
        }
    }

    /**
     * Excepción para errores de renderizado
     */
    public static class TemplateRenderingException extends RuntimeException {
        public TemplateRenderingException(String message) {
            super(message);
        }

        public TemplateRenderingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}