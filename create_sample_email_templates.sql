-- Insertar templates de email de ejemplo
INSERT INTO email_templates 
(template_key, name, category, type, subject, html_content, description, language, active, is_default, created_at) 
VALUES 
-- Template de asignación de entrevista individual
(
    'INTERVIEW_ASSIGNMENT',
    'Asignación de Entrevista Individual',
    'INTERVIEW_ASSIGNMENT',
    'NOTIFICATION',
    'Entrevista Programada - {{studentName}} - {{gradeApplied}}',
    '<div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
        <h2 style="color: #2563eb;">Entrevista Programada</h2>
        <p>Estimado/a <strong>{{applicantName}}</strong>,</p>
        <p>Le informamos que se ha programado una entrevista para su hijo/a <strong>{{studentName}}</strong> 
           para el proceso de admisión al grado <strong>{{gradeApplied}}</strong>.</p>
        <div style="background: #f3f4f6; padding: 15px; margin: 20px 0; border-radius: 8px;">
            <p><strong>Fecha:</strong> {{interviewDate}}</p>
            <p><strong>Hora:</strong> {{interviewTime}}</p>
            <p><strong>Entrevistador:</strong> {{interviewerName}}</p>
            <p><strong>Modalidad:</strong> {{interviewMode}}</p>
        </div>
        <p>Saludos cordiales,<br>{{collegeName}}</p>
    </div>',
    'Template para notificar asignación de entrevista individual',
    'es',
    true,
    true,
    NOW()
),

-- Template de selección de estudiante
(
    'STUDENT_SELECTION',
    'Selección de Estudiante',
    'STUDENT_SELECTION',
    'APPROVAL',
    '¡Felicitaciones! {{studentName}} ha sido seleccionado/a',
    '<div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
        <h2 style="color: #16a34a;">¡Felicitaciones!</h2>
        <p>Estimado/a <strong>{{applicantName}}</strong>,</p>
        <p>Nos complace informarle que <strong>{{studentName}}</strong> ha sido 
           <strong>seleccionado/a</strong> para formar parte de nuestra institución 
           en el grado <strong>{{gradeApplied}}</strong>.</p>
        <div style="background: #dcfce7; padding: 15px; margin: 20px 0; border-radius: 8px; border-left: 4px solid #16a34a;">
            <p><strong>Resultado:</strong> SELECCIONADO/A</p>
            <p><strong>Grado:</strong> {{gradeApplied}}</p>
            <p><strong>Año Académico:</strong> {{currentYear}}</p>
        </div>
        <p>En los próximos días recibirá información sobre el proceso de matrícula.</p>
        <p>Saludos cordiales,<br>{{collegeName}}</p>
    </div>',
    'Template para notificar selección positiva de estudiante',
    'es',
    true,
    true,
    NOW()
),

-- Template de rechazo de estudiante
(
    'STUDENT_REJECTION',
    'Resultado de Proceso - No Seleccionado',
    'STUDENT_REJECTION',
    'REJECTION',
    'Resultado del Proceso de Admisión - {{studentName}}',
    '<div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
        <h2 style="color: #dc2626;">Resultado del Proceso de Admisión</h2>
        <p>Estimado/a <strong>{{applicantName}}</strong>,</p>
        <p>Le informamos que después de un cuidadoso proceso de evaluación, 
           lamentablemente <strong>{{studentName}}</strong> no ha sido seleccionado/a 
           para el grado <strong>{{gradeApplied}}</strong> en nuestro establecimiento.</p>
        <div style="background: #fef2f2; padding: 15px; margin: 20px 0; border-radius: 8px; border-left: 4px solid #dc2626;">
            <p><strong>Resultado:</strong> NO SELECCIONADO/A</p>
            <p><strong>Motivo:</strong> {{rejectionReason}}</p>
        </div>
        <p>Agradecemos su interés en nuestra institución y le deseamos el mejor de los éxitos.</p>
        <p>Saludos cordiales,<br>{{collegeName}}</p>
    </div>',
    'Template para notificar resultado negativo del proceso',
    'es',
    true,
    true,
    NOW()
),

-- Template de recordatorio de entrevista
(
    'INTERVIEW_REMINDER',
    'Recordatorio de Entrevista',
    'INTERVIEW_REMINDER',
    'REMINDER',
    'Recordatorio: Entrevista Mañana - {{studentName}}',
    '<div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
        <h2 style="color: #f59e0b;">Recordatorio de Entrevista</h2>
        <p>Estimado/a <strong>{{applicantName}}</strong>,</p>
        <p>Le recordamos que tiene programada una entrevista <strong>mañana</strong> 
           para <strong>{{studentName}}</strong> como parte del proceso de admisión.</p>
        <div style="background: #fef3c7; padding: 15px; margin: 20px 0; border-radius: 8px;">
            <p><strong>Fecha:</strong> {{interviewDate}}</p>
            <p><strong>Hora:</strong> {{interviewTime}}</p>
            <p><strong>Entrevistador:</strong> {{interviewerName}}</p>
            <p><strong>Modalidad:</strong> {{interviewMode}}</p>
        </div>
        <p>Por favor, llegue puntualmente. En caso de inconvenientes, contáctenos al {{collegePhone}}.</p>
        <p>Saludos cordiales,<br>{{collegeName}}</p>
    </div>',
    'Template para recordar entrevista programada',
    'es',
    true,
    true,
    NOW()
),

-- Template de bienvenida
(
    'WELCOME_MESSAGE',
    'Mensaje de Bienvenida',
    'WELCOME_MESSAGE',
    'WELCOME',
    'Bienvenido/a a {{collegeName}} - {{studentName}}',
    '<div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
        <h2 style="color: #2563eb;">¡Bienvenido/a a nuestra comunidad!</h2>
        <p>Estimado/a <strong>{{applicantName}}</strong>,</p>
        <p>Es un gran placer dar la bienvenida a <strong>{{studentName}}</strong> 
           a la familia de <strong>{{collegeName}}</strong>.</p>
        <div style="background: #dbeafe; padding: 15px; margin: 20px 0; border-radius: 8px;">
            <p><strong>Estudiante:</strong> {{studentName}}</p>
            <p><strong>Grado:</strong> {{gradeApplied}}</p>
            <p><strong>Año Académico:</strong> {{currentYear}}</p>
        </div>
        <p>En los próximos días recibirá información detallada sobre el inicio del año escolar.</p>
        <p>¡Esperamos trabajar juntos en el crecimiento y desarrollo de {{studentFirstName}}!</p>
        <p>Saludos cordiales,<br>{{collegeName}}</p>
    </div>',
    'Template de bienvenida para nuevos estudiantes',
    'es',
    true,
    false,
    NOW()
);