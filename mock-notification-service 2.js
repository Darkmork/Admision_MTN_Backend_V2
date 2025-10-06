const express = require('express');
const nodemailer = require('nodemailer');
const CircuitBreaker = require('opossum');
const app = express();
const port = 8085;

app.use(express.json());

// ============= EXTERNAL SERVICE CIRCUIT BREAKER =============
// Notification Service solo necesita External breaker (SMTP, no usa DB directamente)

// External Service Calls (8s, 70% threshold, 120s reset) - SMTP email sending
const externalServiceBreakerOptions = {
  timeout: 8000,                  // Long timeout for SMTP (8s)
  errorThresholdPercentage: 70,   // Very tolerant (70% - SMTP failures shouldn't kill service)
  resetTimeout: 120000,           // Very long recovery (120s / 2min)
  rollingCountTimeout: 20000,     // Wide window (20s)
  rollingCountBuckets: 10,        // 10 buckets
  name: 'NotificationExternalBreaker'
};

// Create external service breaker for SMTP operations
const externalServiceBreaker = new CircuitBreaker(
  async (fn) => await fn(),
  externalServiceBreakerOptions
);

// Event listeners
externalServiceBreaker.on('open', () => {
  console.error('⚠️ [Circuit Breaker External] OPEN - Too many SMTP failures in notification service');
});

externalServiceBreaker.on('halfOpen', () => {
  console.warn('🔄 [Circuit Breaker External] HALF-OPEN - Testing SMTP recovery');
});

externalServiceBreaker.on('close', () => {
  console.log('✅ [Circuit Breaker External] CLOSED - Notification service recovered');
});

externalServiceBreaker.fallback(() => {
  throw new Error('Email service temporarily unavailable - circuit breaker open');
});

// Configurar transportador de email con Gmail
const transporter = nodemailer.createTransport({
  service: 'gmail',
  host: 'smtp.gmail.com',
  port: 587,
  secure: false,
  auth: {
    user: 'admision@mtn.cl',
    pass: 'elecdqywuqyuhafr' // App password sin espacios
  }
});

// Función para verificar conectividad de email
async function verifyEmailConnection() {
  try {
    await transporter.verify();
    console.log('✅ Conexión Gmail SMTP establecida correctamente');
    return true;
  } catch (error) {
    console.error('❌ Error conectando con Gmail SMTP:', error.message);
    return false;
  }
}

// Verificar conexión al iniciar
verifyEmailConnection();

// Almacén en memoria para códigos de verificación (solo para desarrollo/testing)
const verificationCodes = new Map();

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ 
    status: 'UP', 
    service: 'notification-service',
    port: port,
    timestamp: new Date().toISOString()
  });
});

// Mock notification endpoints
app.get('/api/notifications', (req, res) => {
  res.json({
    message: 'Notifications from notification-service microservice',
    notifications: [
      { id: 1, type: 'EMAIL', recipient: 'parent1@email.com', subject: 'Application Status Update' },
      { id: 2, type: 'SMS', recipient: '+56912345678', message: 'Interview scheduled' }
    ]
  });
});

// GET email notifications with filtering and pagination
app.get('/api/notifications/email', (req, res) => {
  const { status, type, page = 0, limit = 10, search } = req.query;

  // Mock email notifications data
  const mockEmails = [
    {
      id: 1,
      recipient: 'padre1@example.com',
      recipientName: 'Juan Pérez',
      subject: 'Postulación Recibida - Sistema de Admisión MTN',
      template: 'APPLICATION_SUBMITTED',
      status: 'SENT',
      sentAt: '2025-10-03T10:30:00Z',
      openedAt: '2025-10-03T11:15:00Z',
      clickedAt: null,
      bounced: false,
      responseReceived: false,
      autoReplyGenerated: false,
      applicationId: 101,
      studentName: 'María Pérez González'
    },
    {
      id: 2,
      recipient: 'madre2@example.com',
      recipientName: 'Ana Martínez',
      subject: 'Actualización de Postulación - En Revisión',
      template: 'STATUS_CHANGE',
      status: 'SENT',
      sentAt: '2025-10-03T09:00:00Z',
      openedAt: '2025-10-03T09:30:00Z',
      clickedAt: '2025-10-03T09:35:00Z',
      bounced: false,
      responseReceived: true,
      autoReplyGenerated: true,
      applicationId: 102,
      studentName: 'Carlos Martínez López',
      responseSubject: 'Re: Actualización de Postulación',
      responseDate: '2025-10-03T10:00:00Z'
    },
    {
      id: 3,
      recipient: 'apoderado3@example.com',
      recipientName: 'Pedro Silva',
      subject: 'Entrevista Programada - Sistema de Admisión MTN',
      template: 'INTERVIEW_SCHEDULED',
      status: 'SENT',
      sentAt: '2025-10-02T14:20:00Z',
      openedAt: '2025-10-02T15:00:00Z',
      clickedAt: null,
      bounced: false,
      responseReceived: false,
      autoReplyGenerated: false,
      applicationId: 103,
      studentName: 'Lucía Silva Rojas'
    },
    {
      id: 4,
      recipient: 'invalid@bounced.com',
      recipientName: 'Usuario No Válido',
      subject: 'Postulación Recibida - Sistema de Admisión MTN',
      template: 'APPLICATION_SUBMITTED',
      status: 'BOUNCED',
      sentAt: '2025-10-01T08:00:00Z',
      openedAt: null,
      clickedAt: null,
      bounced: true,
      bounceReason: 'Email address does not exist',
      responseReceived: false,
      autoReplyGenerated: false,
      applicationId: 104,
      studentName: 'Estudiante Prueba'
    },
    {
      id: 5,
      recipient: 'apoderado5@example.com',
      recipientName: 'Carmen Torres',
      subject: 'Postulación Aprobada - Felicitaciones',
      template: 'APPLICATION_APPROVED',
      status: 'SENT',
      sentAt: '2025-10-03T16:00:00Z',
      openedAt: null,
      clickedAt: null,
      bounced: false,
      responseReceived: false,
      autoReplyGenerated: false,
      applicationId: 105,
      studentName: 'Diego Torres Muñoz'
    },
    {
      id: 6,
      recipient: 'apoderado6@example.com',
      recipientName: 'Roberto Fernández',
      subject: 'Documentación Pendiente - Acción Requerida',
      template: 'DOCUMENTS_REQUIRED',
      status: 'PENDING',
      sentAt: null,
      openedAt: null,
      clickedAt: null,
      bounced: false,
      responseReceived: false,
      autoReplyGenerated: false,
      applicationId: 106,
      studentName: 'Sofía Fernández Castro',
      scheduledFor: '2025-10-04T09:00:00Z'
    }
  ];

  // Apply filters
  let filteredEmails = mockEmails;

  if (status) {
    filteredEmails = filteredEmails.filter(email => email.status === status);
  }

  if (type) {
    filteredEmails = filteredEmails.filter(email => email.template === type);
  }

  if (search) {
    const searchLower = search.toLowerCase();
    filteredEmails = filteredEmails.filter(email =>
      email.recipient.toLowerCase().includes(searchLower) ||
      email.recipientName.toLowerCase().includes(searchLower) ||
      email.subject.toLowerCase().includes(searchLower) ||
      email.studentName.toLowerCase().includes(searchLower)
    );
  }

  // Pagination
  const startIndex = page * limit;
  const endIndex = startIndex + parseInt(limit);
  const paginatedEmails = filteredEmails.slice(startIndex, endIndex);

  res.json({
    success: true,
    data: paginatedEmails,
    total: filteredEmails.length,
    page: parseInt(page),
    limit: parseInt(limit),
    totalPages: Math.ceil(filteredEmails.length / limit),
    message: 'Email notifications retrieved successfully'
  });
});

// Enhanced notification sending endpoint
app.post('/api/notifications/send', async (req, res) => {
  const { type, recipient, subject, templateData, template } = req.body;
  const correlationId = req.headers['x-correlation-id'] || `notif-${Date.now()}`;
  
  if (!type || !recipient) {
    return res.status(400).json({
      success: false,
      error: 'Type and recipient are required',
      correlationId
    });
  }
  
  try {
    const timestamp = new Date().toISOString();
    console.log(`[${timestamp}] [${correlationId}] Processing notification: ${template || type} to ${recipient}`);
    
    let emailBody = '';
    let emailSubject = subject || 'Notificación - Sistema de Admisión MTN';
    
    // Dynamic year calculations
    const currentYear = new Date().getFullYear();
    const applicationYear = currentYear + 1;
    
    // Generate email content based on template
    switch (template) {
      case 'application_created':
        emailSubject = 'Postulación Recibida - Sistema de Admisión MTN';
        emailBody = `
          <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
            <div style="background-color: #1e40af; color: white; padding: 20px; text-align: center;">
              <h1>Sistema de Admisión MTN</h1>
            </div>
            <div style="padding: 30px; background-color: #f9f9f9;">
              <h2>¡Postulación Recibida!</h2>
              <p>Estimados padres/apoderados,</p>
              <p>Hemos recibido exitosamente la postulación de <strong>${templateData.studentName}</strong> para el año académico ${applicationYear}.</p>
              <div style="background-color: #e5e5e5; padding: 15px; margin: 20px 0; border-radius: 5px;">
                <p><strong>ID de Postulación:</strong> #${templateData.applicationId}</p>
                <p><strong>Año académico:</strong> ${applicationYear}</p>
                <p><strong>Fecha de Recepción:</strong> ${templateData.submissionDate}</p>
              </div>
              <p>${templateData.nextSteps}</p>
              <p>Nos pondremos en contacto con ustedes pronto para coordinar los siguientes pasos del proceso de admisión para el año ${applicationYear}.</p>
              <p>Saludos cordiales,<br>Equipo de Admisión<br>Colegio Monte Tabor y Nazaret</p>
            </div>
          </div>
        `;
        break;
        
      case 'status_change':
        emailSubject = `Actualización de Postulación - ${templateData.newStatus}`;
        emailBody = `
          <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
            <div style="background-color: #1e40af; color: white; padding: 20px; text-align: center;">
              <h1>Sistema de Admisión MTN</h1>
            </div>
            <div style="padding: 30px; background-color: #f9f9f9;">
              <h2>Actualización de Estado de Postulación</h2>
              <p>Estimados padres/apoderados,</p>
              <p>Le informamos que el estado de la postulación de <strong>${templateData.studentName}</strong> para el año académico ${applicationYear} ha sido actualizado.</p>
              <div style="background-color: #e5e5e5; padding: 15px; margin: 20px 0; border-radius: 5px;">
                <p><strong>ID de Postulación:</strong> #${templateData.applicationId}</p>
                <p><strong>Año académico:</strong> ${applicationYear}</p>
                <p><strong>Estado Anterior:</strong> ${templateData.oldStatus}</p>
                <p><strong>Nuevo Estado:</strong> <span style="color: #1e40af; font-weight: bold;">${templateData.newStatus}</span></p>
                <p><strong>Fecha de Actualización:</strong> ${templateData.statusDate}</p>
              </div>
              <p>${templateData.message}</p>
              <p>Si tiene alguna pregunta sobre el proceso de admisión ${applicationYear}, no dude en contactarnos.</p>
              <p>Saludos cordiales,<br>Equipo de Admisión<br>Colegio Monte Tabor y Nazaret</p>
            </div>
          </div>
        `;
        break;
        
      case 'interview_scheduled':
        emailSubject = 'Entrevista Programada - Sistema de Admisión MTN';
        emailBody = `
          <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
            <div style="background-color: #1e40af; color: white; padding: 20px; text-align: center;">
              <h1>Sistema de Admisión MTN</h1>
            </div>
            <div style="padding: 30px; background-color: #f9f9f9;">
              <h2>Entrevista Programada</h2>
              <p>Estimados padres/apoderados,</p>
              <p>Se ha programado una entrevista para <strong>${templateData.studentName}</strong> como parte del proceso de admisión para el año académico ${applicationYear}.</p>
              <div style="background-color: #e8f4f8; padding: 20px; margin: 20px 0; border-radius: 5px; border-left: 4px solid #1e40af;">
                <h3 style="margin-top: 0; color: #1e40af;">Detalles de la Entrevista</h3>
                <p><strong>Año académico:</strong> ${applicationYear}</p>
                <p><strong>Fecha:</strong> ${templateData.interviewDate}</p>
                <p><strong>Hora:</strong> ${templateData.interviewTime}</p>
                <p><strong>Entrevistador:</strong> ${templateData.interviewer}</p>
                <p><strong>Ubicación:</strong> ${templateData.location}</p>
              </div>
              <p><strong>Importante:</strong> Por favor llegue 15 minutos antes de la hora programada.</p>
              <p>Si necesita reprogramar la entrevista, contacte a nuestro equipo de admisión lo antes posible.</p>
              <p>Saludos cordiales,<br>Equipo de Admisión<br>Colegio Monte Tabor y Nazaret</p>
            </div>
          </div>
        `;
        break;
        
      default:
        emailBody = `
          <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
            <div style="background-color: #1e40af; color: white; padding: 20px; text-align: center;">
              <h1>Sistema de Admisión MTN</h1>
            </div>
            <div style="padding: 30px; background-color: #f9f9f9;">
              <h2>Notificación</h2>
              <p>Estimados padres/apoderados,</p>
              <p>Tenemos una actualización sobre el proceso de admisión.</p>
              <p>Saludos cordiales,<br>Equipo de Admisión<br>Colegio Monte Tabor y Nazaret</p>
            </div>
          </div>
        `;
    }
    
    if (type === 'email') {
      // Configure email options
      const mailOptions = {
        from: '"Sistema de Admisión MTN" <admision@mtn.cl>',
        to: recipient,
        subject: emailSubject,
        html: emailBody
      };
      
      try {
        // Send email
        const info = await transporter.sendMail(mailOptions);
        
        console.log(`[${timestamp}] [${correlationId}] Email sent successfully to ${recipient}`);
        console.log(`[${timestamp}] [${correlationId}] Message ID: ${info.messageId}`);
        
        res.json({
          success: true,
          message: 'Notification sent successfully',
          notificationId: Date.now(),
          messageId: info.messageId,
          recipient: recipient,
          subject: emailSubject,
          template: template,
          correlationId
        });
        
      } catch (emailError) {
        console.error(`[${timestamp}] [${correlationId}] Email sending failed:`, emailError.message);
        
        // Still return success for the API call (notification system shouldn't fail the main operation)
        res.json({
          success: true,
          message: 'Notification queued (email delivery failed)',
          notificationId: Date.now(),
          recipient: recipient,
          subject: emailSubject,
          template: template,
          correlationId,
          deliveryStatus: 'failed',
          error: emailError.message
        });
      }
    } else {
      // For other notification types (SMS, push, etc.)
      console.log(`[${timestamp}] [${correlationId}] ${type.toUpperCase()} notification to ${recipient}`);
      
      res.json({
        success: true,
        message: `${type.toUpperCase()} notification processed`,
        notificationId: Date.now(),
        recipient: recipient,
        template: template,
        correlationId
      });
    }
    
  } catch (error) {
    const timestamp = new Date().toISOString();
    console.error(`[${timestamp}] [${correlationId}] Notification processing error:`, error.message);
    
    res.status(500).json({
      success: false,
      error: 'Failed to process notification',
      correlationId,
      details: error.message
    });
  }
});

// Email endpoints
// Enhanced email sending endpoint with templates - US-9, US-10
app.post('/api/email/send', async (req, res) => {
  const { to, templateType, data } = req.body;

  if (!to || !templateType) {
    return res.status(400).json({
      success: false,
      message: 'To and templateType are required'
    });
  }

  console.log(`📧 Sending email to: ${to}, template: ${templateType}`);

  // Get email template based on templateType
  const emailContent = getEmailTemplate(templateType, data || {});

  if (!emailContent) {
    return res.status(400).json({
      success: false,
      message: `Unknown template type: ${templateType}`
    });
  }

  const mailOptions = {
    from: '"Sistema de Admisión MTN" <admision@mtn.cl>',
    to: to,
    subject: emailContent.subject,
    html: emailContent.html
  };

  try {
    const info = await transporter.sendMail(mailOptions);

    console.log(`✅ Email sent successfully to ${to}`);
    console.log(`📬 Message ID: ${info.messageId}`);

    res.json({
      success: true,
      message: 'Email sent successfully',
      emailId: info.messageId,
      to: to,
      templateType: templateType,
      timestamp: new Date().toISOString()
    });

  } catch (error) {
    console.error(`❌ Error sending email to ${to}:`, error.message);

    // Fallback for development
    console.log(`⚠️ FALLBACK - Email not sent, showing in console`);
    console.log(`📧 To: ${to}`);
    console.log(`📋 Template: ${templateType}`);
    console.log(`📄 Subject: ${emailContent.subject}`);

    res.json({
      success: true, // Keep as true for development
      message: 'Email fallback - shown in console',
      emailId: `fallback-${Date.now()}`,
      to: to,
      templateType: templateType,
      fallback: true,
      error: error.message,
      timestamp: new Date().toISOString()
    });
  }
});

// Email template generator
function getEmailTemplate(templateType, data) {
  const currentYear = new Date().getFullYear();

  switch (templateType) {
    case 'STATUS_CHANGE':
      return {
        subject: `Actualización del Estado de su Postulación - ${data.studentName}`,
        html: `
          <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; background-color: #f9fafb;">
            <!-- Header -->
            <div style="background-color: #1e40af; color: white; padding: 30px 20px; text-align: center;">
              <h1 style="margin: 0; font-size: 24px;">Sistema de Admisión</h1>
              <p style="margin: 10px 0 0 0; font-size: 14px;">Colegio Monte Tabor y Nazaret</p>
            </div>

            <!-- Content -->
            <div style="padding: 40px 20px; background-color: white;">
              <h2 style="color: #1e40af; margin-top: 0;">Actualización del Estado de Postulación</h2>

              <p>Estimado(a) ${data.guardianName || 'Apoderado(a)'},</p>

              <p>Le informamos que el estado de la postulación de <strong>${data.studentName}</strong> ha sido actualizado:</p>

              <div style="background-color: #eff6ff; border-left: 4px solid #1e40af; padding: 15px; margin: 20px 0;">
                <p style="margin: 0;"><strong>Estado anterior:</strong> ${getStatusLabel(data.previousStatus)}</p>
                <p style="margin: 10px 0 0 0;"><strong>Estado actual:</strong> <span style="color: #1e40af; font-weight: bold;">${getStatusLabel(data.newStatus)}</span></p>
              </div>

              ${data.changeNote ? `
              <div style="background-color: #fef3c7; border-left: 4px solid #f59e0b; padding: 15px; margin: 20px 0;">
                <p style="margin: 0;"><strong>Observación:</strong></p>
                <p style="margin: 10px 0 0 0;">${data.changeNote}</p>
              </div>
              ` : ''}

              <p style="margin-top: 30px;">Para más información sobre el proceso de admisión, puede contactarnos a través de:</p>
              <ul style="line-height: 1.8;">
                <li>📧 Email: admision@mtn.cl</li>
                <li>📞 Teléfono: +56 2 1234 5678</li>
                <li>🌐 Web: www.mtn.cl</li>
              </ul>

              <p style="color: #6b7280; font-size: 14px; margin-top: 30px;">
                Este es un correo automático, por favor no responder directamente.
              </p>
            </div>

            <!-- Footer -->
            <div style="background-color: #1e40af; color: white; padding: 20px; text-align: center; font-size: 12px;">
              <p style="margin: 0;">© ${currentYear} Colegio Monte Tabor y Nazaret</p>
              <p style="margin: 10px 0 0 0;">Sistema de Admisión - Todos los derechos reservados</p>
            </div>
          </div>
        `
      };

    default:
      return null;
  }
}

// Helper function to get human-readable status labels
function getStatusLabel(status) {
  const statusMap = {
    'SUBMITTED': 'Enviada',
    'ENVIADA': 'Enviada',
    'UNDER_REVIEW': 'En Revisión',
    'EN_REVISION': 'En Revisión',
    'INTERVIEW_SCHEDULED': 'Entrevista Programada',
    'ENTREVISTA_PROGRAMADA': 'Entrevista Programada',
    'APPROVED': 'Aceptada',
    'ACEPTADA': 'Aceptada',
    'REJECTED': 'Rechazada',
    'RECHAZADA': 'Rechazada',
    'WAITLIST': 'Lista de Espera',
    'LISTA_ESPERA': 'Lista de Espera',
    'ARCHIVED': 'Archivada',
    'ARCHIVADA': 'Archivada'
  };

  return statusMap[status?.toUpperCase()] || status;
}

// Email verification endpoint (for frontend usage)
app.post('/api/email/send-verification', async (req, res) => {
  const { email } = req.body;
  
  if (!email) {
    return res.status(400).json({
      success: false,
      message: 'Email is required'
    });
  }
  
  // Generate a random 6-digit verification code
  const verificationCode = Math.floor(100000 + Math.random() * 900000);
  
  console.log('📧 Enviando email de verificación a:', email);
  console.log('🔑 CÓDIGO DE VERIFICACIÓN:', verificationCode);
  
  // Configuración del correo
  const mailOptions = {
    from: '"Sistema de Admisión MTN" <admision@mtn.cl>',
    to: email,
    subject: 'Código de Verificación - Sistema de Admisión MTN',
    html: `
      <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
        <div style="background-color: #1e40af; color: white; padding: 20px; text-align: center;">
          <h1>Sistema de Admisión MTN</h1>
        </div>
        <div style="padding: 30px; background-color: #f9f9f9;">
          <h2>Código de Verificación</h2>
          <p>Hola,</p>
          <p>Tu código de verificación es:</p>
          <div style="background-color: #e5e5e5; padding: 20px; text-align: center; font-size: 32px; font-weight: bold; letter-spacing: 5px; margin: 20px 0;">
            ${verificationCode}
          </div>
          <p>Este código expira en 10 minutos.</p>
          <p>Si no solicitaste este código, puedes ignorar este correo.</p>
          <hr style="border: none; border-top: 1px solid #ddd; margin: 30px 0;">
          <p style="color: #666; font-size: 12px;">
            Este es un correo automático, por favor no responder.
          </p>
        </div>
        <div style="background-color: #1e40af; color: white; padding: 10px; text-align: center; font-size: 12px;">
          © ${new Date().getFullYear()} Colegio Monte Tabor y Nazaret
        </div>
      </div>
    `
  };
  
  try {
    // Almacenar el código de verificación con expiración de 10 minutos
    const expirationTime = Date.now() + 10 * 60 * 1000; // 10 minutos
    verificationCodes.set(email, {
      code: verificationCode,
      expiration: expirationTime,
      attempts: 0
    });
    
    // Enviar el correo usando Gmail SMTP
    const info = await transporter.sendMail(mailOptions);
    
    console.log('✅ Email enviado exitosamente!');
    console.log('📬 Message ID:', info.messageId);
    console.log('───────────────────────────────────────');
    
    // Mostrar el código de verificación prominentemente en consola
    console.log('\n' + '='.repeat(60));
    console.log('🎯 CÓDIGO DE VERIFICACIÓN GENERADO');
    console.log('='.repeat(60));
    console.log(`📧 Email: ${email}`);
    console.log(`🔑 CÓDIGO: ${verificationCode}`);
    console.log('='.repeat(60) + '\n');
    
    res.json({
      success: true,
      message: 'Email de verificación enviado exitosamente',
      email: email,
      messageId: info.messageId,
      verificationCode: verificationCode, // Include in response for development
      timestamp: new Date().toISOString()
    });
    
  } catch (error) {
    console.error('❌ Error enviando email:', error);
    
    // Almacenar el código de verificación incluso si falla el email (para desarrollo)
    const expirationTime = Date.now() + 10 * 60 * 1000; // 10 minutos
    verificationCodes.set(email, {
      code: verificationCode,
      expiration: expirationTime,
      attempts: 0
    });
    
    // Fallback: mostrar prominentemente en consola si falla el envío
    console.log('\n' + '='.repeat(60));
    console.log('🔄 FALLBACK - CÓDIGO DE VERIFICACIÓN GENERADO');
    console.log('='.repeat(60));
    console.log(`📧 Email: ${email}`);
    console.log(`🔑 CÓDIGO: ${verificationCode}`);
    console.log('⚠️ Nota: Email no enviado, mostrado en consola');
    console.log('='.repeat(60) + '\n');
    
    res.json({
      success: true, // Mantener como true para que la aplicación continúe
      message: 'Email fallback - código mostrado en consola del servidor',
      email: email,
      verificationCode: verificationCode,
      fallback: true,
      error: error.message,
      timestamp: new Date().toISOString()
    });
  }
});

// Check if email exists endpoint (for frontend usage)
app.post('/api/email/check-exists', (req, res) => {
  const { email } = req.body;
  console.log('🔍 Checking email existence:', email);
  
  // Mock response - normally would check against database
  const exists = email && email.includes('existing');
  
  res.json({
    success: true,
    exists: exists,
    email: email,
    message: exists ? 'Email exists in system' : 'Email not found in system'
  });
});

// GET version for query param style
app.get('/api/email/check-exists', (req, res) => {
  const { email } = req.query;
  console.log('🔍 Checking email existence (GET):', email);
  
  const exists = email && email.includes('existing');
  
  res.json({
    success: true,
    exists: exists,
    email: email,
    message: exists ? 'Email exists in system' : 'Email not found in system'
  });
});

// Email verification code endpoint
app.post('/api/email/verify-code', (req, res) => {
  const { email, code } = req.body;
  
  console.log(`🔍 Verificando código para: ${email}, código: ${code}`);
  
  // Validar parámetros requeridos
  if (!email || !code) {
    return res.status(400).json({
      success: false,
      message: 'Email y código son requeridos',
      error: 'MISSING_PARAMETERS'
    });
  }
  
  // Obtener el código almacenado
  const storedCodeData = verificationCodes.get(email);
  
  if (!storedCodeData) {
    console.log(`❌ No se encontró código para: ${email}`);
    return res.status(400).json({
      success: false,
      message: 'No se ha enviado un código de verificación para este email',
      error: 'CODE_NOT_FOUND'
    });
  }
  
  // Verificar expiración
  if (Date.now() > storedCodeData.expiration) {
    console.log(`⏰ Código expirado para: ${email}`);
    verificationCodes.delete(email);
    return res.status(400).json({
      success: false,
      message: 'El código de verificación ha expirado',
      error: 'CODE_EXPIRED'
    });
  }
  
  // Verificar número de intentos (máximo 3)
  if (storedCodeData.attempts >= 3) {
    console.log(`🔒 Demasiados intentos para: ${email}`);
    verificationCodes.delete(email);
    return res.status(400).json({
      success: false,
      message: 'Demasiados intentos. Solicite un nuevo código',
      error: 'TOO_MANY_ATTEMPTS'
    });
  }
  
  // Verificar el código
  const providedCode = code.toString().trim();
  const storedCode = storedCodeData.code.toString();
  
  if (providedCode !== storedCode) {
    console.log(`❌ Código incorrecto para: ${email}. Esperado: ${storedCode}, Recibido: ${providedCode}`);
    
    // Incrementar intentos
    storedCodeData.attempts += 1;
    verificationCodes.set(email, storedCodeData);
    
    return res.status(400).json({
      success: false,
      message: 'Código de verificación incorrecto',
      error: 'INVALID_CODE',
      attemptsRemaining: 3 - storedCodeData.attempts
    });
  }
  
  // Código correcto - limpiar el código almacenado
  console.log(`✅ Código verificado exitosamente para: ${email}`);
  verificationCodes.delete(email);
  
  res.json({
    success: true,
    isValid: true,  // Agregar campo isValid que el frontend espera
    message: 'Código de verificación correcto',
    email: email,
    timestamp: new Date().toISOString()
  });
});

// Email templates endpoint (for InterviewManagement component)
app.get('/api/email-templates/all', (req, res) => {
  console.log('📧 Fetching all email templates');
  
  const mockTemplates = [
    {
      id: 1,
      name: 'Confirmación de Entrevista',
      subject: 'Confirmación de su entrevista - Sistema de Admisión MTN',
      body: `
        Estimado(a) [GUARDIAN_NAME],
        
        Le confirmamos que su entrevista ha sido programada para:
        
        Fecha: [INTERVIEW_DATE]
        Hora: [INTERVIEW_TIME]
        Evaluador: [INTERVIEWER_NAME]
        
        Por favor confirme su asistencia.
        
        Saludos cordiales,
        Sistema de Admisión MTN
      `,
      type: 'interview_confirmation',
      active: true
    },
    {
      id: 2,
      name: 'Recordatorio de Entrevista',
      subject: 'Recordatorio: Su entrevista es mañana - Sistema de Admisión MTN',
      body: `
        Estimado(a) [GUARDIAN_NAME],
        
        Le recordamos que su entrevista está programada para mañana:
        
        Fecha: [INTERVIEW_DATE]
        Hora: [INTERVIEW_TIME]
        Evaluador: [INTERVIEWER_NAME]
        
        Por favor llegue 15 minutos antes.
        
        Saludos cordiales,
        Sistema de Admisión MTN
      `,
      type: 'interview_reminder',
      active: true
    },
    {
      id: 3,
      name: 'Reprogramación de Entrevista',
      subject: 'Cambio de horario - Su entrevista ha sido reprogramada',
      body: `
        Estimado(a) [GUARDIAN_NAME],
        
        Por motivos operativos, su entrevista ha sido reprogramada:
        
        Nueva fecha: [INTERVIEW_DATE]
        Nueva hora: [INTERVIEW_TIME]
        Evaluador: [INTERVIEWER_NAME]
        
        Disculpe las molestias ocasionadas.
        
        Saludos cordiales,
        Sistema de Admisión MTN
      `,
      type: 'interview_reschedule',
      active: true
    },
    {
      id: 4,
      name: 'Entrevista Completada',
      subject: 'Entrevista completada - Próximos pasos',
      body: `
        Estimado(a) [GUARDIAN_NAME],
        
        Su entrevista del [INTERVIEW_DATE] ha sido completada exitosamente.
        
        Le informaremos sobre los próximos pasos en el proceso de admisión en los próximos días.
        
        Gracias por su tiempo.
        
        Saludos cordiales,
        Sistema de Admisión MTN
      `,
      type: 'interview_completed',
      active: true
    }
  ];
  
  res.json({
    success: true,
    data: mockTemplates,
    total: mockTemplates.length,
    message: 'Email templates retrieved successfully'
  });
});

// ============= NOTIFICATION CONFIGURATION ENDPOINTS =============
// Admin endpoints to manage notification configurations

const { Pool } = require('pg');
const dbPool = new Pool({
  host: 'localhost',
  port: 5432,
  database: 'Admisión_MTN_DB',
  user: 'admin',
  password: 'admin123',
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000
});

// GET all notification configurations
app.get('/api/notifications/config', async (req, res) => {
  const client = await dbPool.connect();
  try {
    const query = `
      SELECT
        id, event_type, event_name, description, enabled,
        send_email, send_sms, send_push,
        notify_applicant, notify_admin, notify_coordinator, notify_evaluator,
        custom_recipients, email_template_key,
        send_immediately, delay_minutes,
        created_at, updated_at
      FROM notification_configs
      ORDER BY event_type
    `;

    const result = await client.query(query);

    res.json({
      success: true,
      data: result.rows,
      total: result.rows.length,
      message: 'Notification configurations retrieved successfully'
    });
  } catch (error) {
    console.error('❌ Error fetching notification configs:', error);
    res.status(500).json({
      success: false,
      error: 'Error fetching notification configurations',
      details: error.message
    });
  } finally {
    client.release();
  }
});

// GET single notification configuration by event_type
app.get('/api/notifications/config/:eventType', async (req, res) => {
  const { eventType } = req.params;
  const client = await dbPool.connect();

  try {
    const query = `
      SELECT
        id, event_type, event_name, description, enabled,
        send_email, send_sms, send_push,
        notify_applicant, notify_admin, notify_coordinator, notify_evaluator,
        custom_recipients, email_template_key,
        send_immediately, delay_minutes,
        created_at, updated_at
      FROM notification_configs
      WHERE event_type = $1
    `;

    const result = await client.query(query, [eventType]);

    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        error: 'Notification configuration not found'
      });
    }

    res.json({
      success: true,
      data: result.rows[0],
      message: 'Notification configuration retrieved successfully'
    });
  } catch (error) {
    console.error('❌ Error fetching notification config:', error);
    res.status(500).json({
      success: false,
      error: 'Error fetching notification configuration',
      details: error.message
    });
  } finally {
    client.release();
  }
});

// UPDATE notification configuration
app.put('/api/notifications/config/:id', async (req, res) => {
  const { id } = req.params;
  const {
    event_name,
    description,
    enabled,
    send_email,
    send_sms,
    send_push,
    notify_applicant,
    notify_admin,
    notify_coordinator,
    notify_evaluator,
    custom_recipients,
    email_template_key,
    send_immediately,
    delay_minutes
  } = req.body;

  const client = await dbPool.connect();

  try {
    const query = `
      UPDATE notification_configs SET
        event_name = COALESCE($1, event_name),
        description = COALESCE($2, description),
        enabled = COALESCE($3, enabled),
        send_email = COALESCE($4, send_email),
        send_sms = COALESCE($5, send_sms),
        send_push = COALESCE($6, send_push),
        notify_applicant = COALESCE($7, notify_applicant),
        notify_admin = COALESCE($8, notify_admin),
        notify_coordinator = COALESCE($9, notify_coordinator),
        notify_evaluator = COALESCE($10, notify_evaluator),
        custom_recipients = COALESCE($11, custom_recipients),
        email_template_key = COALESCE($12, email_template_key),
        send_immediately = COALESCE($13, send_immediately),
        delay_minutes = COALESCE($14, delay_minutes),
        updated_at = NOW()
      WHERE id = $15
      RETURNING *
    `;

    const result = await client.query(query, [
      event_name,
      description,
      enabled,
      send_email,
      send_sms,
      send_push,
      notify_applicant,
      notify_admin,
      notify_coordinator,
      notify_evaluator,
      custom_recipients,
      email_template_key,
      send_immediately,
      delay_minutes,
      id
    ]);

    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        error: 'Notification configuration not found'
      });
    }

    console.log(`✅ Notification config updated: ${result.rows[0].event_type}`);

    res.json({
      success: true,
      data: result.rows[0],
      message: 'Notification configuration updated successfully'
    });
  } catch (error) {
    console.error('❌ Error updating notification config:', error);
    res.status(500).json({
      success: false,
      error: 'Error updating notification configuration',
      details: error.message
    });
  } finally {
    client.release();
  }
});

// CREATE new notification configuration
app.post('/api/notifications/config', async (req, res) => {
  const {
    event_type,
    event_name,
    description,
    enabled = true,
    send_email = true,
    send_sms = false,
    send_push = false,
    notify_applicant = false,
    notify_admin = false,
    notify_coordinator = false,
    notify_evaluator = false,
    custom_recipients = null,
    email_template_key = null,
    send_immediately = true,
    delay_minutes = 0
  } = req.body;

  if (!event_type || !event_name) {
    return res.status(400).json({
      success: false,
      error: 'event_type and event_name are required'
    });
  }

  const client = await dbPool.connect();

  try {
    const query = `
      INSERT INTO notification_configs (
        event_type, event_name, description, enabled,
        send_email, send_sms, send_push,
        notify_applicant, notify_admin, notify_coordinator, notify_evaluator,
        custom_recipients, email_template_key,
        send_immediately, delay_minutes
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15)
      RETURNING *
    `;

    const result = await client.query(query, [
      event_type,
      event_name,
      description,
      enabled,
      send_email,
      send_sms,
      send_push,
      notify_applicant,
      notify_admin,
      notify_coordinator,
      notify_evaluator,
      custom_recipients,
      email_template_key,
      send_immediately,
      delay_minutes
    ]);

    console.log(`✅ Notification config created: ${event_type}`);

    res.status(201).json({
      success: true,
      data: result.rows[0],
      message: 'Notification configuration created successfully'
    });
  } catch (error) {
    console.error('❌ Error creating notification config:', error);

    if (error.code === '23505') { // Unique violation
      return res.status(409).json({
        success: false,
        error: 'Notification configuration with this event_type already exists'
      });
    }

    res.status(500).json({
      success: false,
      error: 'Error creating notification configuration',
      details: error.message
    });
  } finally {
    client.release();
  }
});

// DELETE notification configuration
app.delete('/api/notifications/config/:id', async (req, res) => {
  const { id } = req.params;
  const client = await dbPool.connect();

  try {
    const query = 'DELETE FROM notification_configs WHERE id = $1 RETURNING event_type';
    const result = await client.query(query, [id]);

    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        error: 'Notification configuration not found'
      });
    }

    console.log(`✅ Notification config deleted: ${result.rows[0].event_type}`);

    res.json({
      success: true,
      message: 'Notification configuration deleted successfully'
    });
  } catch (error) {
    console.error('❌ Error deleting notification config:', error);
    res.status(500).json({
      success: false,
      error: 'Error deleting notification configuration',
      details: error.message
    });
  } finally {
    client.release();
  }
});

// TOGGLE notification configuration enabled/disabled
app.patch('/api/notifications/config/:id/toggle', async (req, res) => {
  const { id } = req.params;
  const client = await dbPool.connect();

  try {
    const query = `
      UPDATE notification_configs
      SET enabled = NOT enabled, updated_at = NOW()
      WHERE id = $1
      RETURNING *
    `;

    const result = await client.query(query, [id]);

    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        error: 'Notification configuration not found'
      });
    }

    const status = result.rows[0].enabled ? 'enabled' : 'disabled';
    console.log(`✅ Notification config ${status}: ${result.rows[0].event_type}`);

    res.json({
      success: true,
      data: result.rows[0],
      message: `Notification configuration ${status} successfully`
    });
  } catch (error) {
    console.error('❌ Error toggling notification config:', error);
    res.status(500).json({
      success: false,
      error: 'Error toggling notification configuration',
      details: error.message
    });
  } finally {
    client.release();
  }
});

app.listen(port, () => {
  console.log(`Notification Service running on port ${port}`);
});