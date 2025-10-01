-- Crear templates iniciales de correo institucional
INSERT INTO email_templates (template_key, name, description, type, category, subject, html_content, text_content, variables, active, is_default, language) VALUES

-- Template 1: Asignación de Entrevista Individual
('INTERVIEW_ASSIGNMENT', 'Asignación de Entrevista Individual', 'Template para notificar asignación de entrevista individual', 'NOTIFICATION', 'INTERVIEW_ASSIGNMENT', 
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
    <p><strong>Ubicación:</strong> {{interviewLocation}}</p>
  </div>
  <p>Saludos cordiales,<br>{{collegeName}}</p>
</div>',
'Estimado/a {{applicantName}}, se ha programado una entrevista para {{studentName}} el {{interviewDate}} a las {{interviewTime}}. Entrevistador: {{interviewerName}}. Modalidad: {{interviewMode}}.',
'studentName,studentFirstName,studentLastName,gradeApplied,applicantName,applicantEmail,collegeName,collegePhone,collegeEmail,currentDate,currentYear,interviewDate,interviewTime,interviewerName,interviewMode,interviewLocation',
true, true, 'es'),

-- Template 2: Set Completo de Entrevistas
('INTERVIEW_COMPLETE_SET', 'Set Completo de Entrevistas', 'Template para notificar asignación de set completo de 3 entrevistas', 'NOTIFICATION', 'INTERVIEW_ASSIGNMENT',
'Set Completo de Entrevistas - {{studentName}}',
'<div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
  <h2 style="color: #2563eb;">Set Completo de Entrevistas Programado</h2>
  <p>Estimado/a <strong>{{applicantName}}</strong>,</p>
  <p>Le informamos que se ha programado el set completo de entrevistas para 
     <strong>{{studentName}}</strong> como parte del proceso de admisión.</p>
  <div style="background: #f3f4f6; padding: 15px; margin: 20px 0; border-radius: 8px;">
    <h3 style="margin-top: 0;">Entrevistas Programadas:</h3>
    <ul>
      <li><strong>Entrevista Directiva:</strong> {{directorInterviewDate}} - {{directorInterviewTime}}</li>
      <li><strong>Entrevista Psicológica:</strong> {{psychologyInterviewDate}} - {{psychologyInterviewTime}}</li>
      <li><strong>Entrevista Académica:</strong> {{academicInterviewDate}} - {{academicInterviewTime}}</li>
    </ul>
  </div>
  <p>Saludos cordiales,<br>{{collegeName}}</p>
</div>',
'Set completo de entrevistas programado para {{studentName}}. Entrevistas: Directiva, Psicológica y Académica. Detalles enviados por separado.',
'studentName,applicantName,collegeName,directorInterviewDate,directorInterviewTime,psychologyInterviewDate,psychologyInterviewTime,academicInterviewDate,academicInterviewTime',
true, false, 'es'),

-- Template 3: Selección de Estudiante
('STUDENT_SELECTION', 'Selección de Estudiante', 'Template para notificar selección positiva de estudiante', 'APPROVAL', 'STUDENT_SELECTION',
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
'¡Felicitaciones! {{studentName}} ha sido seleccionado/a para {{gradeApplied}} en {{collegeName}}. Información de matrícula próximamente.',
'studentName,gradeApplied,applicantName,collegeName,currentYear',
true, true, 'es'),

-- Template 4: Rechazo de Estudiante  
('STUDENT_REJECTION', 'Resultado de Proceso - No Seleccionado', 'Template para notificar resultado negativo del proceso', 'REJECTION', 'STUDENT_REJECTION',
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
'Resultado del proceso de admisión para {{studentName}}: No seleccionado/a para {{gradeApplied}}. Motivo: {{rejectionReason}}.',
'studentName,gradeApplied,applicantName,collegeName,rejectionReason',
true, true, 'es'),

-- Template 5: Recordatorio de Entrevista
('INTERVIEW_REMINDER', 'Recordatorio de Entrevista', 'Template para recordar entrevista programada', 'REMINDER', 'INTERVIEW_REMINDER',
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
    <p><strong>Ubicación:</strong> {{interviewLocation}}</p>
  </div>
  <p>Por favor, llegue puntualmente. En caso de inconvenientes, contáctenos al {{collegePhone}}.</p>
  <p>Saludos cordiales,<br>{{collegeName}}</p>
</div>',
'Recordatorio: Entrevista mañana para {{studentName}} el {{interviewDate}} a las {{interviewTime}}. Entrevistador: {{interviewerName}}.',
'studentName,applicantName,interviewDate,interviewTime,interviewerName,interviewMode,interviewLocation,collegeName,collegePhone',
true, true, 'es'),

-- Template 6: Confirmación de Entrevista
('INTERVIEW_CONFIRMATION', 'Confirmación de Entrevista', 'Template para confirmar asistencia a entrevista', 'CONFIRMATION', 'INTERVIEW_CONFIRMATION',
'Confirme su Asistencia - Entrevista {{studentName}}',
'<div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
  <h2 style="color: #3b82f6;">Confirmación de Entrevista</h2>
  <p>Estimado/a <strong>{{applicantName}}</strong>,</p>
  <p>Para confirmar su asistencia a la entrevista de <strong>{{studentName}}</strong>, 
     programada para el proceso de admisión, por favor responda este correo.</p>
  <div style="background: #eff6ff; padding: 15px; margin: 20px 0; border-radius: 8px;">
    <p><strong>Fecha:</strong> {{interviewDate}}</p>
    <p><strong>Hora:</strong> {{interviewTime}}</p>
    <p><strong>Entrevistador:</strong> {{interviewerName}}</p>
    <p><strong>Modalidad:</strong> {{interviewMode}}</p>
  </div>
  <p><strong>Por favor confirme su asistencia respondiendo a este correo antes de las 48 horas.</strong></p>
  <p>Saludos cordiales,<br>{{collegeName}}</p>
</div>',
'Por favor confirme asistencia a entrevista de {{studentName}} el {{interviewDate}} a las {{interviewTime}}.',
'studentName,applicantName,interviewDate,interviewTime,interviewerName,interviewMode,collegeName',
true, false, 'es');

-- Verificar inserción
SELECT template_key, name, category, type, active FROM email_templates ORDER BY id;