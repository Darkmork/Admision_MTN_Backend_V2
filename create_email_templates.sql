-- Script para crear la tabla de templates de correo y los templates iniciales
-- Ejecutar este script después de que el backend haya creado las tablas automáticamente

-- Crear tabla de templates de correo (si no existe)
CREATE TABLE IF NOT EXISTS email_templates (
    id SERIAL PRIMARY KEY,
    template_key VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    type VARCHAR(50) NOT NULL,
    category VARCHAR(50) NOT NULL,
    subject VARCHAR(300) NOT NULL,
    html_content TEXT NOT NULL,
    text_content TEXT,
    variables TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    language VARCHAR(100) DEFAULT 'es',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by BIGINT,
    updated_by BIGINT
);

-- Eliminar templates existentes si los hay
DELETE FROM email_templates;

-- Template 1: Asignación de una entrevista individual
INSERT INTO email_templates (
    template_key, name, description, type, category, subject, html_content, text_content, variables, is_default
) VALUES (
    'INTERVIEW_ASSIGNMENT',
    'Asignación de Entrevista Individual',
    'Template para notificar la asignación de una entrevista específica',
    'NOTIFICATION',
    'INTERVIEW_ASSIGNMENT',
    'Entrevista Programada - {{studentName}} - {{collegeName}}',
    '<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Entrevista Programada</title>
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
        .header { background-color: #2c5aa0; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
        .content { background-color: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px; }
        .highlight { background-color: #e8f4f8; padding: 15px; margin: 20px 0; border-left: 4px solid #2c5aa0; }
        .details { background-color: white; padding: 20px; margin: 20px 0; border-radius: 5px; border: 1px solid #ddd; }
        .footer { text-align: center; margin-top: 30px; font-size: 12px; color: #666; }
        .button { display: inline-block; padding: 12px 25px; background-color: #2c5aa0; color: white; text-decoration: none; border-radius: 5px; margin: 10px 0; }
    </style>
</head>
<body>
    <div class="header">
        <h1>{{collegeName}}</h1>
        <h2>Entrevista Programada</h2>
    </div>
    
    <div class="content">
        <p>Estimado/a {{applicantName}},</p>
        
        <p>Nos complace informarle que hemos programado una entrevista para <strong>{{studentName}}</strong> como parte del proceso de admisión para el año {{currentYear}}.</p>
        
        <div class="highlight">
            <h3>Detalles de la Entrevista:</h3>
        </div>
        
        <div class="details">
            <p><strong>Estudiante:</strong> {{studentName}}</p>
            <p><strong>Curso al que postula:</strong> {{gradeApplied}}</p>
            <p><strong>Tipo de Entrevista:</strong> {{interviewType}}</p>
            <p><strong>Modalidad:</strong> {{interviewMode}}</p>
            <p><strong>Fecha:</strong> {{interviewDate}}</p>
            <p><strong>Hora:</strong> {{interviewTime}}</p>
            <p><strong>Duración:</strong> {{interviewDuration}}</p>
            <p><strong>Lugar:</strong> {{interviewLocation}}</p>
            <p><strong>Entrevistador:</strong> {{interviewerName}}</p>
        </div>
        
        <div class="highlight">
            <h3>Información Importante:</h3>
            <ul>
                <li>Por favor, llegue 10 minutos antes de la hora programada</li>
                <li>Traiga el documento de identidad del estudiante y del apoderado</li>
                <li>En caso de no poder asistir, comuníquese con nosotros con al menos 24 horas de anticipación</li>
                <li>Para entrevistas virtuales, recibirá el enlace por separado</li>
            </ul>
        </div>
        
        <p>Si tiene alguna consulta sobre esta entrevista, no dude en contactarnos.</p>
        
        <p>Saludos cordiales,<br>
        <strong>Equipo de Admisiones</strong><br>
        {{collegeName}}</p>
    </div>
    
    <div class="footer">
        <p>{{collegeName}} - {{collegeAddress}}<br>
        Teléfono: {{collegePhone}} | Email: {{collegeEmail}}</p>
        <p>Este es un correo automático, por favor no responda a esta dirección.</p>
    </div>
</body>
</html>',
    'Estimado/a {{applicantName}}, nos complace informarle que hemos programado una entrevista para {{studentName}}...',
    'studentName,applicantName,gradeApplied,interviewType,interviewMode,interviewDate,interviewTime,interviewDuration,interviewLocation,interviewerName,collegeName,collegePhone,collegeEmail,collegeAddress,currentYear',
    true
);

-- Template 2: Set completo de 3 entrevistas
INSERT INTO email_templates (
    template_key, name, description, type, category, subject, html_content, text_content, variables, is_default
) VALUES (
    'INTERVIEW_COMPLETE_SET',
    'Set Completo de Entrevistas',
    'Template para notificar que se han programado las 3 entrevistas requeridas',
    'NOTIFICATION',
    'INTERVIEW_ASSIGNMENT',
    'Todas las Entrevistas Programadas - {{studentName}} - {{collegeName}}',
    '<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Entrevistas Programadas</title>
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
        .header { background-color: #2c5aa0; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
        .content { background-color: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px; }
        .highlight { background-color: #e8f4f8; padding: 15px; margin: 20px 0; border-left: 4px solid #2c5aa0; }
        .interview-list { background-color: white; padding: 20px; margin: 20px 0; border-radius: 5px; border: 1px solid #ddd; }
        .footer { text-align: center; margin-top: 30px; font-size: 12px; color: #666; }
        .success { background-color: #d4edda; color: #155724; padding: 10px; border-radius: 5px; margin: 15px 0; }
    </style>
</head>
<body>
    <div class="header">
        <h1>{{collegeName}}</h1>
        <h2>🎉 Todas las Entrevistas Programadas</h2>
    </div>
    
    <div class="content">
        <p>Estimado/a {{applicantName}},</p>
        
        <div class="success">
            <p><strong>¡Excelente noticia!</strong> Hemos completado la programación de todas las entrevistas requeridas para <strong>{{studentName}}</strong> en nuestro proceso de admisión {{currentYear}}.</p>
        </div>
        
        <div class="highlight">
            <h3>Entrevistas Programadas ({{totalInterviews}} en total):</h3>
        </div>
        
        <div class="interview-list">
            {{interviewList}}
        </div>
        
        <div class="highlight">
            <h3>Información Importante:</h3>
            <ul>
                <li><strong>Por favor, asista a las 3 entrevistas</strong> - todas son obligatorias para completar el proceso</li>
                <li>Llegue 10 minutos antes de cada entrevista</li>
                <li>Traiga documento de identidad en cada ocasión</li>
                <li>Para entrevistas virtuales, recibirá los enlaces por separado</li>
                <li>En caso de no poder asistir a alguna, comuníquese con mínimo 24 horas de anticipación</li>
            </ul>
        </div>
        
        <p>Una vez completadas las 3 entrevistas, procesaremos su evaluación y le comunicaremos los resultados del proceso de admisión.</p>
        
        <p>¡Esperamos conocerlos pronto!</p>
        
        <p>Saludos cordiales,<br>
        <strong>Equipo de Admisiones</strong><br>
        {{collegeName}}</p>
    </div>
    
    <div class="footer">
        <p>{{collegeName}} - {{collegeAddress}}<br>
        Teléfono: {{collegePhone}} | Email: {{collegeEmail}}</p>
    </div>
</body>
</html>',
    'Estimado/a {{applicantName}}, ¡Excelente noticia! Hemos completado la programación de todas las entrevistas requeridas para {{studentName}}...',
    'studentName,applicantName,totalInterviews,interviewList,collegeName,collegePhone,collegeEmail,collegeAddress,currentYear',
    false
);

-- Template 3: Confirmación de entrevista
INSERT INTO email_templates (
    template_key, name, description, type, category, subject, html_content, text_content, variables, is_default
) VALUES (
    'INTERVIEW_CONFIRMATION',
    'Confirmación de Entrevista',
    'Template para confirmar la asistencia a una entrevista',
    'CONFIRMATION',
    'INTERVIEW_CONFIRMATION',
    'Entrevista Confirmada - {{studentName}} - {{collegeName}}',
    '<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Entrevista Confirmada</title>
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
        .header { background-color: #28a745; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
        .content { background-color: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px; }
        .success { background-color: #d4edda; color: #155724; padding: 15px; border-radius: 5px; margin: 20px 0; text-align: center; }
        .details { background-color: white; padding: 20px; margin: 20px 0; border-radius: 5px; border: 1px solid #ddd; }
        .footer { text-align: center; margin-top: 30px; font-size: 12px; color: #666; }
    </style>
</head>
<body>
    <div class="header">
        <h1>{{collegeName}}</h1>
        <h2>✅ Entrevista Confirmada</h2>
    </div>
    
    <div class="content">
        <p>Estimado/a {{applicantName}},</p>
        
        <div class="success">
            <h3>¡Su entrevista ha sido confirmada exitosamente!</h3>
        </div>
        
        <p>Confirmamos que la entrevista para <strong>{{studentName}}</strong> está programada y confirmada con los siguientes detalles:</p>
        
        <div class="details">
            <p><strong>Fecha:</strong> {{interviewDate}}</p>
            <p><strong>Hora:</strong> {{interviewTime}}</p>
            <p><strong>Tipo:</strong> {{interviewType}}</p>
            <p><strong>Modalidad:</strong> {{interviewMode}}</p>
            <p><strong>Lugar:</strong> {{interviewLocation}}</p>
            <p><strong>Entrevistador:</strong> {{interviewerName}}</p>
        </div>
        
        <p>Nos vemos pronto. ¡Muchas gracias por su tiempo!</p>
        
        <p>Saludos cordiales,<br>
        <strong>{{interviewerName}}</strong><br>
        {{collegeName}}</p>
    </div>
    
    <div class="footer">
        <p>{{collegeName}} - {{collegePhone}} | {{collegeEmail}}</p>
    </div>
</body>
</html>',
    'Estimado/a {{applicantName}}, confirmamos que la entrevista para {{studentName}} está programada y confirmada...',
    'studentName,applicantName,interviewDate,interviewTime,interviewType,interviewMode,interviewLocation,interviewerName,collegeName,collegePhone,collegeEmail',
    true
);

-- Template 4: Selección de estudiante
INSERT INTO email_templates (
    template_key, name, description, type, category, subject, html_content, text_content, variables, is_default
) VALUES (
    'STUDENT_SELECTION',
    'Selección de Estudiante',
    'Template para notificar que el estudiante ha sido seleccionado',
    'APPROVAL',
    'STUDENT_SELECTION',
    '🎉 ¡Felicitaciones! {{studentName}} ha sido seleccionado/a - {{collegeName}}',
    '<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>¡Estudiante Seleccionado!</title>
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
        .header { background-color: #28a745; color: white; padding: 30px; text-align: center; border-radius: 8px 8px 0 0; }
        .content { background-color: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px; }
        .celebration { background-color: #d4edda; color: #155724; padding: 20px; border-radius: 5px; margin: 20px 0; text-align: center; font-size: 18px; }
        .next-steps { background-color: white; padding: 20px; margin: 20px 0; border-radius: 5px; border: 1px solid #ddd; }
        .footer { text-align: center; margin-top: 30px; font-size: 12px; color: #666; }
        .important { background-color: #fff3cd; color: #856404; padding: 15px; border-radius: 5px; margin: 15px 0; }
    </style>
</head>
<body>
    <div class="header">
        <h1>{{collegeName}}</h1>
        <h2>🎉 ¡FELICITACIONES! 🎉</h2>
    </div>
    
    <div class="content">
        <p>Estimado/a {{applicantName}},</p>
        
        <div class="celebration">
            <h3>¡Nos complace enormemente informarle que <strong>{{studentName}}</strong> ha sido SELECCIONADO/A para ingresar a {{collegeName}} el año {{currentYear}}!</h3>
        </div>
        
        <p>Después de un riguroso proceso de evaluación y entrevistas, estamos seguros de que {{studentFirstName}} será una excelente adición a nuestra comunidad educativa en <strong>{{gradeApplied}}</strong>.</p>
        
        <div class="next-steps">
            <h3>Próximos Pasos:</h3>
            <ol>
                <li><strong>Confirmación de Matrícula:</strong> Debe confirmar la aceptación del cupo dentro de los próximos 10 días hábiles</li>
                <li><strong>Documentación:</strong> Completar la documentación de matrícula</li>
                <li><strong>Reunión de Bienvenida:</strong> Participar en nuestra reunión informativa para nuevas familias</li>
                <li><strong>Preparación para el año escolar:</strong> Recibir información sobre uniformes, útiles escolares y calendario académico</li>
            </ol>
        </div>
        
        <div class="important">
            <p><strong>Importante:</strong> Para completar el proceso de matrícula y asegurar el cupo, debe contactarnos a la brevedad al {{collegePhone}} o por email a {{collegeEmail}}.</p>
        </div>
        
        <p>Estamos muy emocionados de recibir a {{studentFirstName}} en nuestra familia educativa y acompañarlo/a en esta nueva etapa de crecimiento académico y personal.</p>
        
        <p>¡Bienvenidos a {{collegeName}}!</p>
        
        <p>Con gran alegría,<br>
        <strong>Equipo de Admisiones</strong><br>
        {{collegeName}}</p>
    </div>
    
    <div class="footer">
        <p>{{collegeName}} - {{collegeAddress}}<br>
        Teléfono: {{collegePhone}} | Email: {{collegeEmail}}</p>
        <p>¡Gracias por confiar en nosotros para la educación de {{studentFirstName}}!</p>
    </div>
</body>
</html>',
    'Estimado/a {{applicantName}}, ¡Nos complace enormemente informarle que {{studentName}} ha sido SELECCIONADO/A para ingresar a {{collegeName}}!',
    'studentName,studentFirstName,applicantName,gradeApplied,collegeName,collegePhone,collegeEmail,collegeAddress,currentYear',
    true
);

-- Template 5: Recordatorio de entrevista
INSERT INTO email_templates (
    template_key, name, description, type, category, subject, html_content, text_content, variables, is_default
) VALUES (
    'INTERVIEW_REMINDER',
    'Recordatorio de Entrevista',
    'Template para recordar una entrevista próxima',
    'REMINDER',
    'INTERVIEW_REMINDER',
    'Recordatorio: Entrevista mañana - {{studentName}} - {{collegeName}}',
    '<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Recordatorio de Entrevista</title>
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
        .header { background-color: #ffc107; color: #212529; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
        .content { background-color: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px; }
        .reminder { background-color: #fff3cd; color: #856404; padding: 15px; border-radius: 5px; margin: 20px 0; text-align: center; }
        .details { background-color: white; padding: 20px; margin: 20px 0; border-radius: 5px; border: 1px solid #ddd; }
        .footer { text-align: center; margin-top: 30px; font-size: 12px; color: #666; }
    </style>
</head>
<body>
    <div class="header">
        <h1>{{collegeName}}</h1>
        <h2>⏰ Recordatorio de Entrevista</h2>
    </div>
    
    <div class="content">
        <p>Estimado/a {{applicantName}},</p>
        
        <div class="reminder">
            <h3>Le recordamos que tiene una entrevista programada para <strong>{{studentName}}</strong></h3>
        </div>
        
        <div class="details">
            <p><strong>Fecha:</strong> {{interviewDate}}</p>
            <p><strong>Hora:</strong> {{interviewTime}}</p>
            <p><strong>Tipo:</strong> {{interviewType}}</p>
            <p><strong>Lugar:</strong> {{interviewLocation}}</p>
            <p><strong>Entrevistador:</strong> {{interviewerName}}</p>
        </div>
        
        <p><strong>Por favor, no olvide:</strong></p>
        <ul>
            <li>Llegar 10 minutos antes</li>
            <li>Traer documento de identidad</li>
            <li>En caso de no poder asistir, avisar con 24 horas de anticipación</li>
        </ul>
        
        <p>¡Los esperamos!</p>
        
        <p>Saludos,<br>
        <strong>Equipo de Admisiones</strong><br>
        {{collegeName}}</p>
    </div>
    
    <div class="footer">
        <p>{{collegeName}} - {{collegePhone}} | {{collegeEmail}}</p>
    </div>
</body>
</html>',
    'Estimado/a {{applicantName}}, le recordamos que tiene una entrevista programada para {{studentName}} el {{interviewDate}} a las {{interviewTime}}...',
    'studentName,applicantName,interviewDate,interviewTime,interviewType,interviewLocation,interviewerName,collegeName,collegePhone,collegeEmail',
    true
);

-- Confirmar creación de templates
SELECT 'Templates creados exitosamente:' as mensaje;
SELECT template_key, name, category, is_default FROM email_templates ORDER BY category, name;