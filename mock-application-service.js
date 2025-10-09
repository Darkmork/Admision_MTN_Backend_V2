const express = require('express');
const compression = require('compression');
const { Pool } = require('pg');
const CircuitBreaker = require('opossum');
const axios = require('axios');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const PDFDocument = require('pdfkit');
const { translateToSpanish } = require('./translations');
const { validateRUT } = require('./utils/validateRUT');
const { logAudit, getClientIp, getUserAgent, AuditActions, EntityTypes } = require('./utils/auditLogger');
const app = express();
const port = 8083;

// Set timezone to America/Santiago for consistent date handling
process.env.TZ = 'America/Santiago';

// Notification service configuration
const NOTIFICATION_SERVICE_URL = 'http://localhost:8085';

// Database configuration with connection pooling
const dbPool = new Pool({
  host: process.env.DB_HOST || 'localhost',
  port: parseInt(process.env.DB_PORT || '5432'),
  database: process.env.DB_NAME || 'Admisión_MTN_DB',
  user: process.env.DB_USERNAME || 'admin',
  password: process.env.DB_PASSWORD || 'admin123',
  ssl: false, // No SSL for Railway internal network
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000,
  query_timeout: 5000
});

// ============= DIFFERENTIATED CIRCUIT BREAKERS =============
// 4 circuit breaker categories for Application Service
// (No necesita Heavy - sin analytics complejos, pero sí External para uploads)

// 1. Simple Queries (2s, 60% threshold, 20s reset) - Fast lookups
const simpleQueryBreakerOptions = {
  timeout: 2000,
  errorThresholdPercentage: 60,
  resetTimeout: 20000,
  rollingCountTimeout: 10000,
  rollingCountBuckets: 10,
  name: 'ApplicationSimpleQueryBreaker'
};

// 2. Medium Queries (5s, 50% threshold, 30s reset) - Standard queries with joins
const mediumQueryBreakerOptions = {
  timeout: 5000,
  errorThresholdPercentage: 50,
  resetTimeout: 30000,
  rollingCountTimeout: 10000,
  rollingCountBuckets: 10,
  name: 'ApplicationMediumQueryBreaker'
};

// 3. Write Operations (3s, 30% threshold, 45s reset) - Critical application mutations
const writeOperationBreakerOptions = {
  timeout: 3000,
  errorThresholdPercentage: 30,
  resetTimeout: 45000,
  rollingCountTimeout: 10000,
  rollingCountBuckets: 10,
  name: 'ApplicationWriteBreaker'
};

// 4. External Service Calls (8s, 70% threshold, 120s reset) - S3 uploads, external APIs
const externalServiceBreakerOptions = {
  timeout: 8000,
  errorThresholdPercentage: 70,
  resetTimeout: 120000,
  rollingCountTimeout: 20000,
  rollingCountBuckets: 10,
  name: 'ApplicationExternalBreaker'
};

// Create circuit breakers
const simpleQueryBreaker = new CircuitBreaker(
  async (client, query, params) => await client.query(query, params),
  simpleQueryBreakerOptions
);

const mediumQueryBreaker = new CircuitBreaker(
  async (client, query, params) => await client.query(query, params),
  mediumQueryBreakerOptions
);

const writeOperationBreaker = new CircuitBreaker(
  async (client, query, params) => await client.query(query, params),
  writeOperationBreakerOptions
);

const externalServiceBreaker = new CircuitBreaker(
  async (fn) => await fn(),
  externalServiceBreakerOptions
);

// Event listeners for all breakers
const setupBreakerEvents = (breaker, name) => {
  breaker.on('open', () => {
    console.error(`⚠️ [Circuit Breaker ${name}] OPEN - Too many failures in application service`);
  });

  breaker.on('halfOpen', () => {
    console.warn(`🔄 [Circuit Breaker ${name}] HALF-OPEN - Testing recovery`);
  });

  breaker.on('close', () => {
    console.log(`✅ [Circuit Breaker ${name}] CLOSED - Application service recovered`);
  });

  breaker.on('failure', (error) => {
    console.error(`❌ [Circuit Breaker ${name}] FAILURE:`, {
      message: error.message,
      stack: error.stack?.substring(0, 300)
    });
  });

  breaker.on('timeout', () => {
    console.error(`⏱️ [Circuit Breaker ${name}] TIMEOUT exceeded`);
  });

  breaker.fallback(() => {
    console.warn(`🔄 [Circuit Breaker ${name}] Fallback triggered - returning empty result`);
    // Return null instead of throwing to allow circuit breaker to eventually close
    // The endpoint will check for null and handle it appropriately
    return { rows: [] };  // Return empty result set for database queries
  });
};

// Setup events for all breakers
setupBreakerEvents(simpleQueryBreaker, 'Simple');
setupBreakerEvents(mediumQueryBreaker, 'Medium');
setupBreakerEvents(writeOperationBreaker, 'Write');
setupBreakerEvents(externalServiceBreaker, 'External');

// Legacy breaker for backward compatibility (maps to medium query breaker)
const queryWithCircuitBreaker = mediumQueryBreaker;

// ============= RESPONSE UTILITY FUNCTIONS =============
// Standard response format helpers to ensure API contract consistency

const now = () => new Date().toISOString();

/**
 * Success response for single entity
 * @param {Object} data - The data object to return
 * @param {Object} meta - Optional metadata (merged into response)
 * @returns {Object} Standardized success response
 */
const ok = (data, meta = {}) => ({
  success: true,
  data,
  timestamp: now(),
  ...meta
});

/**
 * Success response for paginated lists
 * @param {Array} items - Array of items to return
 * @param {Object} pagination - Pagination info: { total, page, limit }
 * @returns {Object} Standardized paginated response
 */
const page = (items, { total, page = 0, limit = items?.length ?? 10 } = {}) => {
  const totalPages = limit > 0 ? Math.ceil(total / limit) : 1;
  const hasNext = page < totalPages - 1;
  const hasPrev = page > 0;

  return {
    success: true,
    data: items,
    total,
    page,
    limit,
    totalPages,
    hasNext,
    hasPrev,
    timestamp: now()
  };
};

/**
 * Error response
 * @param {string} error - Human-readable error message
 * @param {Object} options - Error details: { errorCode, details, status }
 * @returns {Object} Standardized error response
 */
const fail = (error, { errorCode = 'GEN_000', details = {}, status = 400 } = {}) => ({
  success: false,
  error,
  errorCode,
  details,
  timestamp: now()
});

// ============= FILE UPLOAD CONFIGURATION =============

// Ensure uploads directory exists
const uploadsDir = path.join(__dirname, 'uploads', 'documents');
if (!fs.existsSync(uploadsDir)) {
  fs.mkdirSync(uploadsDir, { recursive: true });
}

// Configure multer storage
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, uploadsDir);
  },
  filename: (req, file, cb) => {
    // Generate unique filename: applicationId_documentType_timestamp_originalname
    const applicationId = req.params.id || 'temp';
    const timestamp = Date.now();
    const sanitizedOriginalName = file.originalname.replace(/[^a-zA-Z0-9._-]/g, '_');
    const uniqueFilename = `${applicationId}_${timestamp}_${sanitizedOriginalName}`;
    cb(null, uniqueFilename);
  }
});

// File filter for document validation
const fileFilter = (req, file, cb) => {
  // Allowed MIME types
  const allowedMimeTypes = [
    'application/pdf',
    'image/jpeg',
    'image/jpg',
    'image/png',
    'image/gif',
    'application/msword', // .doc
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document' // .docx
  ];

  // Allowed file extensions
  const allowedExtensions = ['.pdf', '.jpg', '.jpeg', '.png', '.gif', '.doc', '.docx'];
  const fileExtension = path.extname(file.originalname).toLowerCase();

  if (allowedMimeTypes.includes(file.mimetype) && allowedExtensions.includes(fileExtension)) {
    cb(null, true);
  } else {
    cb(new Error(`Tipo de archivo no permitido: ${file.mimetype}. Solo se aceptan: PDF, JPG, PNG, GIF, DOC, DOCX`), false);
  }
};

// Multer configuration with limits
const upload = multer({
  storage: storage,
  fileFilter: fileFilter,
  limits: {
    fileSize: 10 * 1024 * 1024, // 10 MB limit
    files: 5 // Max 5 files per request
  }
});

// Document type validation
const VALID_DOCUMENT_TYPES = [
  'BIRTH_CERTIFICATE',
  'GRADES_2023',
  'GRADES_2024',
  'GRADES_2025_SEMESTER_1',
  'PERSONALITY_REPORT_2024',
  'PERSONALITY_REPORT_2025_SEMESTER_1',
  'STUDENT_PHOTO',
  'BAPTISM_CERTIFICATE',
  'PREVIOUS_SCHOOL_REPORT',
  'MEDICAL_CERTIFICATE',
  'PSYCHOLOGICAL_REPORT'
];

// ============= END FILE UPLOAD CONFIGURATION =============

// Middleware to simulate JWT verification
const authenticateToken = (req, res, next) => {
  // Try to get token from Authorization header first, then from query parameter
  const authHeader = req.headers['authorization'];
  let token = authHeader && authHeader.split(' ')[1];

  // If no token in header, try query parameter (for file downloads from browser)
  if (!token && req.query.token) {
    token = req.query.token;
  }

  // For mock purposes, we'll accept any token that looks like a JWT
  if (token && token.split('.').length === 3) {
    try {
      // Decode the JWT payload (base64 decode the middle part)
      const payload = JSON.parse(Buffer.from(token.split('.')[1], 'base64').toString());
      req.user = {
        userId: payload.userId,
        email: payload.email,
        firstName: payload.firstName,
        lastName: payload.lastName,
        role: payload.role || 'APODERADO'
      };
      console.log('🔐 Authenticated user:', req.user.email);
      next();
    } catch (error) {
      console.log('❌ Invalid token format:', error.message);
      return res.status(401).json({ error: 'Token inválido' });
    }
  } else {
    console.log('❌ No token provided or invalid format');
    return res.status(401).json({ error: 'Token de acceso requerido' });
  }
};

// Enhanced logging middleware
app.use((req, res, next) => {
  const timestamp = new Date().toISOString();
  const correlationId = req.headers['x-correlation-id'] || `app-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  
  req.correlationId = correlationId;
  
  console.log(`[${timestamp}] [${correlationId}] ${req.method} ${req.url} - Started`);
  
  const originalSend = res.send;
  res.send = function(data) {
    const endTimestamp = new Date().toISOString();
    console.log(`[${endTimestamp}] [${correlationId}] ${req.method} ${req.url} - Completed ${res.statusCode}`);
    return originalSend.call(this, data);
  };
  
  next();
});

// Notification functions
const sendNotification = async (type, data, correlationId) => {
  try {
    const timestamp = new Date().toISOString();
    console.log(`[${timestamp}] [${correlationId}] Sending ${type} notification to ${data.recipient}`);
    
    let notificationData;
    
    switch (type) {
      case 'application_created':
        notificationData = {
          type: 'email',
          recipient: data.recipient,
          subject: 'Postulación Recibida - Sistema de Admisión MTN',
          templateData: {
            studentName: data.studentName,
            applicationId: data.applicationId,
            submissionDate: data.submissionDate,
            nextSteps: 'Revisaremos su postulación y nos contactaremos pronto.'
          },
          template: 'application_created'
        };
        break;
        
      case 'application_status_change':
        notificationData = {
          type: 'email',
          recipient: data.recipient,
          subject: `Actualización de Postulación - ${data.newStatus}`,
          templateData: {
            studentName: data.studentName,
            applicationId: data.applicationId,
            oldStatus: data.oldStatus,
            newStatus: data.newStatus,
            statusDate: data.statusDate,
            message: getStatusMessage(data.newStatus)
          },
          template: 'status_change'
        };
        break;
        
      case 'interview_scheduled':
        notificationData = {
          type: 'email',
          recipient: data.recipient,
          subject: 'Entrevista Programada - Sistema de Admisión MTN',
          templateData: {
            studentName: data.studentName,
            interviewDate: data.interviewDate,
            interviewTime: data.interviewTime,
            interviewer: data.interviewer,
            location: data.location || 'Colegio Monte Tabor y Nazaret'
          },
          template: 'interview_scheduled'
        };
        break;
        
      default:
        console.log(`[${timestamp}] [${correlationId}] Unknown notification type: ${type}`);
        return { success: false, error: 'Unknown notification type' };
    }
    
    // Send to notification service
    const response = await axios.post(`${NOTIFICATION_SERVICE_URL}/api/notifications/send`, notificationData, {
      headers: {
        'Content-Type': 'application/json',
        'x-correlation-id': correlationId
      },
      timeout: 5000
    });
    
    console.log(`[${timestamp}] [${correlationId}] Notification sent successfully: ${type}`);
    return { success: true, data: response.data };
    
  } catch (error) {
    const timestamp = new Date().toISOString();
    console.error(`[${timestamp}] [${correlationId}] Failed to send ${type} notification:`, error.message);
    
    // Don't fail the main operation if notification fails
    return { success: false, error: error.message };
  }
};

const getStatusMessage = (status) => {
  const statusMessages = {
    'PENDING': 'Su postulación está siendo revisada por nuestro equipo.',
    'UNDER_REVIEW': 'Su postulación está en proceso de evaluación.',
    'DOCUMENTS_REQUESTED': 'Necesitamos documentos adicionales para continuar.',
    'INTERVIEW_SCHEDULED': 'Se ha programado su entrevista.',
    'EXAM_SCHEDULED': 'Se ha programado el examen de admisión.',
    'APPROVED': '¡Felicitaciones! Su postulación ha sido aprobada.',
    'REJECTED': 'Lamentablemente su postulación no fue aprobada en esta ocasión.',
    'WAITLIST': 'Su postulación está en lista de espera.',
    'ARCHIVED': 'Su postulación ha sido archivada.'
  };
  
  return statusMessages[status] || 'Estado de postulación actualizado.';
};

// Queue for batch notifications (processed every 30 seconds)
let notificationQueue = [];

const processNotificationQueue = async () => {
  if (notificationQueue.length === 0) return;
  
  const timestamp = new Date().toISOString();
  const batchId = `batch-${Date.now()}`;
  console.log(`[${timestamp}] [${batchId}] Processing ${notificationQueue.length} queued notifications`);
  
  const currentQueue = [...notificationQueue];
  notificationQueue = []; // Clear queue
  
  const results = await Promise.allSettled(
    currentQueue.map(notification => 
      sendNotification(notification.type, notification.data, notification.correlationId)
    )
  );
  
  const successful = results.filter(r => r.status === 'fulfilled' && r.value.success).length;
  const failed = results.length - successful;
  
  console.log(`[${timestamp}] [${batchId}] Batch complete: ${successful} sent, ${failed} failed`);
};

// Process notification queue every 30 seconds
setInterval(processNotificationQueue, 30000);

// Helper to queue notifications
const queueNotification = (type, data, correlationId) => {
  notificationQueue.push({ type, data, correlationId });
  
  // If queue is getting large, process immediately
  if (notificationQueue.length >= 10) {
    processNotificationQueue();
  }
};

// Global error handler
const handleDatabaseError = (error, correlationId) => {
  const timestamp = new Date().toISOString();
  console.error(`[${timestamp}] [${correlationId}] Database Error:`, {
    message: error.message,
    code: error.code,
    detail: error.detail,
    stack: error.stack?.split('\n').slice(0, 5).join('\n')
  });
  
  // Map common PostgreSQL errors
  switch (error.code) {
    case '23505': // Unique constraint violation
      return { status: 409, message: 'Ya existe un registro con estos datos' };
    case '23503': // Foreign key violation
      return { status: 400, message: 'Error de integridad de datos' };
    case '23502': // Not null violation
      return { status: 400, message: 'Faltan datos requeridos' };
    case '08003': // Connection does not exist
      return { status: 503, message: 'Servicio temporalmente no disponible' };
    default:
      return { status: 500, message: 'Error interno del servidor' };
  }
};

app.use(express.json({ limit: '10mb' }));

// Response compression middleware
app.use(compression({
  filter: (req, res) => {
    if (req.headers['x-no-compression']) {
      return false;
    }
    return compression.filter(req, res);
  },
  threshold: 1024,
  level: 6
}));

// Input validation helpers (validateRUT imported from utils/validateRUT.js)
const validateEmail = (email) => {
  if (!email || typeof email !== 'string') return false;
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email) && email.length <= 100;
};

const validatePhone = (phone) => {
  if (!phone || typeof phone !== 'string') return false;
  // Clean phone - remove all spaces
  const cleanPhone = phone.replace(/\s/g, '');
  // Chilean phone format: +569xxxxxxxx or 9xxxxxxxx or +56 9 xxxx xxxx variations
  const phoneRegex = /^(\+?56\s?9\s?\d{4}\s?\d{4}|\+?569\d{8}|9\d{8})$/;
  return phoneRegex.test(cleanPhone);
};

const validateGrade = (grade) => {
  if (!grade || typeof grade !== 'string') return false;
  const validGrades = [
    'Pre Kinder', 'Kinder', 
    '1° Básico', '2° Básico', '3° Básico', '4° Básico', 
    '5° Básico', '6° Básico', '7° Básico', '8° Básico',
    '1° Medio', '2° Medio', '3° Medio', '4° Medio',
    // Also accept variations without special characters
    '1basico', '2basico', '3basico', '4basico',
    '5basico', '6basico', '7basico', '8basico',
    '1medio', '2medio', '3medio', '4medio',
    'prekinder', 'kinder'
  ];
  // Check both exact match and lowercase version
  return validGrades.includes(grade) || validGrades.includes(grade.toLowerCase());
};

const sanitizeString = (str, maxLength = 255) => {
  if (!str || typeof str !== 'string') return null;
  return str.trim().substring(0, maxLength);
};

const validateDate = (date) => {
  if (!date) return true; // Allow null dates
  const dateObj = new Date(date);
  return !isNaN(dateObj.getTime()) && dateObj < new Date();
};

// Validation middleware for flat structure from frontend
const validateApplicationInput = (req, res, next) => {
  const body = req.body;
  const errors = [];
  
  // 🔍 DEBUG: Log incoming data
  console.log('📥 VALIDACIÓN INICIADA - Datos recibidos:');
  console.log('- firstName:', body.firstName);
  console.log('- lastName:', body.lastName);
  console.log('- paternalLastName:', body.paternalLastName);
  console.log('- rut:', body.rut);
  console.log('- grade:', body.grade);
  console.log('- parent1Name:', body.parent1Name);
  console.log('- parent1Phone:', body.parent1Phone);
  console.log('- Todos los campos:', JSON.stringify(body, null, 2));
  
  // Student validation (using flat structure from frontend)
  if (!body.firstName || body.firstName.trim().length < 2) {
    errors.push('Nombre del estudiante debe tener al menos 2 caracteres');
  }
  
  if (!body.lastName && !body.paternalLastName) {
    errors.push('Apellido del estudiante es requerido');
  }
  
  if (!body.rut || !validateRUT(body.rut)) {
    errors.push('RUT del estudiante inválido');
  }
  
  if (body.studentEmail && !validateEmail(body.studentEmail)) {
    errors.push('Email del estudiante inválido');
  }
  
  if (!body.grade || !validateGrade(body.grade)) {
    errors.push('Curso postulado inválido');
  }
  
  if (body.birthDate && !validateDate(body.birthDate)) {
    errors.push('Fecha de nacimiento inválida');
  }
  
  // Father validation (parent1 in frontend = father)
  if (body.parent1Name && body.parent1Name.trim().length < 3) {
    errors.push('Nombre completo del padre debe tener al menos 3 caracteres');
  }
  
  if (body.parent1Rut && !validateRUT(body.parent1Rut)) {
    errors.push('RUT del padre inválido');
  }
  
  if (body.parent1Email && !validateEmail(body.parent1Email)) {
    errors.push('Email del padre inválido');
  }
  
  if (body.parent1Phone && !validatePhone(body.parent1Phone)) {
    errors.push('Teléfono del padre inválido');
  }
  
  // Mother validation (parent2 in frontend = mother)
  if (body.parent2Name && body.parent2Name.trim().length < 3) {
    errors.push('Nombre completo de la madre debe tener al menos 3 caracteres');
  }
  
  if (body.parent2Rut && !validateRUT(body.parent2Rut)) {
    errors.push('RUT de la madre inválido');
  }
  
  if (body.parent2Email && !validateEmail(body.parent2Email)) {
    errors.push('Email de la madre inválido');
  }
  
  if (body.parent2Phone && !validatePhone(body.parent2Phone)) {
    errors.push('Teléfono de la madre inválido');
  }
  
  if (errors.length > 0) {
    console.log('❌ ERRORES DE VALIDACIÓN ENCONTRADOS:');
    errors.forEach((error, index) => {
      console.log(`   ${index + 1}. ${error}`);
    });
    console.log('📤 Enviando respuesta 400 con errores:', errors);
    
    return res.status(400).json({
      success: false,
      error: 'Errores de validación',
      details: errors
    });
  }
  
  console.log('✅ VALIDACIÓN EXITOSA - Continuando con el procesamiento...');
  
  // Sanitize inputs (using frontend field names)
  req.body.firstName = sanitizeString(req.body.firstName, 100);
  req.body.lastName = sanitizeString(req.body.lastName, 100);
  req.body.paternalLastName = sanitizeString(req.body.paternalLastName, 100);
  req.body.maternalLastName = sanitizeString(req.body.maternalLastName, 100);
  req.body.currentSchool = sanitizeString(req.body.currentSchool, 200);
  req.body.studentAddress = sanitizeString(req.body.studentAddress, 300);
  req.body.additionalNotes = sanitizeString(req.body.additionalNotes, 1000);
  
  req.body.parent1Name = sanitizeString(req.body.parent1Name, 200);
  req.body.parent1Profession = sanitizeString(req.body.parent1Profession, 100);
  req.body.parent1Address = sanitizeString(req.body.parent1Address, 300);
  
  req.body.parent2Name = sanitizeString(req.body.parent2Name, 200);
  req.body.parent2Profession = sanitizeString(req.body.parent2Profession, 100);
  req.body.parent2Address = sanitizeString(req.body.parent2Address, 300);
  
  next();
};

// CORS middleware - commented out because NGINX handles it
// app.use((req, res, next) => {
//   res.header('Access-Control-Allow-Origin', '*');
//   res.header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
//   res.header('Access-Control-Allow-Headers', 'Origin, X-Requested-With, Content-Type, Accept, Authorization');
//
//   if (req.method === 'OPTIONS') {
//     res.sendStatus(200);
//   } else {
//     next();
//   }
// });

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ 
    status: 'UP', 
    service: 'application-service',
    port: port,
    timestamp: new Date().toISOString()
  });
});

// Get all applications with real database data
app.get('/api/applications', async (req, res) => {
  const client = await dbPool.connect();
  try {
    // Extract pagination and filter parameters
    const pageNum = parseInt(req.query.page) || 0;
    const limit = parseInt(req.query.limit) || 20;
    const offset = pageNum * limit;
    const statusFilter = req.query.status; // Extract status filter

    // Build WHERE conditions
    const whereConditions = ['a.deleted_at IS NULL'];
    const queryParams = [];
    let paramIndex = 1;

    if (statusFilter) {
      whereConditions.push(`a.status = $${paramIndex}`);
      queryParams.push(statusFilter);
      paramIndex++;
    }

    const whereClause = whereConditions.join(' AND ');

    // Query to count total applications with filters
    const countQuery = `SELECT COUNT(*) as total FROM applications a WHERE ${whereClause}`;
    const countResult = await queryWithCircuitBreaker.fire(client, countQuery, queryParams);
    const total = parseInt(countResult.rows[0].total);

    // Query to get applications with student, parent, guardian and evaluation details
    const query = `
      SELECT
        a.id,
        a.status,
        a.submission_date,
        a.created_at,
        a.updated_at,
        a.additional_notes,

        -- Student information
        s.id as student_id,
        s.first_name as student_first_name,
        s.paternal_last_name as student_paternal_last_name,
        s.maternal_last_name as student_maternal_last_name,
        s.rut as student_rut,
        s.birth_date as student_birth_date,
        s.grade_applied as student_grade,
        s.current_school as student_current_school,
        s.address as student_address,

        -- Father information
        f.id as father_id,
        f.full_name as father_name,
        f.email as father_email,
        f.phone as father_phone,
        f.profession as father_profession,

        -- Mother information
        m.id as mother_id,
        m.full_name as mother_name,
        m.email as mother_email,
        m.phone as mother_phone,
        m.profession as mother_profession,

        -- Guardian information (Contacto Principal / Apoderado)
        g.id as guardian_id,
        g.full_name as guardian_name,
        g.email as guardian_email,
        g.phone as guardian_phone,
        g.relationship as guardian_relationship,
        g.rut as guardian_rut,

        -- Applicant User information (Usuario que creó la postulación)
        u.id as applicant_user_id,
        u.first_name as applicant_first_name,
        u.last_name as applicant_last_name,
        u.email as applicant_email,

        -- Count evaluations
        (SELECT COUNT(*) FROM evaluations e WHERE e.application_id = a.id) as total_evaluations,
        (SELECT COUNT(*) FROM evaluations e WHERE e.application_id = a.id AND e.status = 'COMPLETED') as completed_evaluations,

        -- Count interviews
        (SELECT COUNT(*) FROM interviews i WHERE i.application_id = a.id) as total_interviews,
        (SELECT COUNT(*) FROM interviews i WHERE i.application_id = a.id AND i.status = 'COMPLETED') as completed_interviews

      FROM applications a
      LEFT JOIN students s ON s.id = a.student_id
      LEFT JOIN parents f ON f.id = a.father_id AND f.parent_type = 'FATHER'
      LEFT JOIN parents m ON m.id = a.mother_id AND m.parent_type = 'MOTHER'
      LEFT JOIN guardians g ON g.id = a.guardian_id
      LEFT JOIN users u ON u.id = a.applicant_user_id
      WHERE ${whereClause}
      ORDER BY a.created_at DESC
      LIMIT $${paramIndex} OFFSET $${paramIndex + 1}
    `;

    // Add pagination parameters to the query params array
    const fullQueryParams = [...queryParams, limit, offset];
    const result = await queryWithCircuitBreaker.fire(client, query, fullQueryParams);

    // Fetch evaluations and interviews for all applications
    const applicationIds = result.rows.map(row => row.id);

    let evaluationsMap = {};
    let interviewsMap = {};

    if (applicationIds.length > 0) {
      // Fetch evaluations
      const evaluationsQuery = `
        SELECT
          e.id,
          e.application_id,
          e.evaluation_type,
          e.status,
          e.score,
          e.max_score,
          e.observations,
          e.evaluation_date,
          e.created_at,
          u.id as evaluator_id,
          u.first_name as evaluator_first_name,
          u.last_name as evaluator_last_name
        FROM evaluations e
        LEFT JOIN users u ON u.id = e.evaluator_id
        WHERE e.application_id = ANY($1)
        ORDER BY e.created_at DESC
      `;

      const evaluationsResult = await client.query(evaluationsQuery, [applicationIds]);

      evaluationsResult.rows.forEach(evalRow => {
        if (!evaluationsMap[evalRow.application_id]) {
          evaluationsMap[evalRow.application_id] = [];
        }
        evaluationsMap[evalRow.application_id].push({
          id: evalRow.id,
          evaluationType: evalRow.evaluation_type,
          status: evalRow.status,
          score: evalRow.score,
          maxScore: evalRow.max_score,
          observations: evalRow.observations,
          evaluationDate: evalRow.evaluation_date,
          createdAt: evalRow.created_at,
          evaluator: evalRow.evaluator_id ? {
            id: evalRow.evaluator_id,
            firstName: evalRow.evaluator_first_name,
            lastName: evalRow.evaluator_last_name
          } : null
        });
      });

      // Fetch interviews
      const interviewsQuery = `
        SELECT
          i.id,
          i.application_id,
          i.interview_type,
          i.status,
          i.scheduled_date,
          i.duration_minutes,
          i.location,
          i.notes,
          i.created_at,
          u.id as interviewer_id,
          u.first_name as interviewer_first_name,
          u.last_name as interviewer_last_name
        FROM interviews i
        LEFT JOIN users u ON u.id = i.interviewer_id
        WHERE i.application_id = ANY($1)
        ORDER BY i.scheduled_date DESC
      `;

      const interviewsResult = await client.query(interviewsQuery, [applicationIds]);

      interviewsResult.rows.forEach(intRow => {
        if (!interviewsMap[intRow.application_id]) {
          interviewsMap[intRow.application_id] = [];
        }
        interviewsMap[intRow.application_id].push({
          id: intRow.id,
          interviewType: intRow.interview_type,
          status: intRow.status,
          scheduledDate: intRow.scheduled_date,
          durationMinutes: intRow.duration_minutes,
          location: intRow.location,
          notes: intRow.notes,
          createdAt: intRow.created_at,
          interviewer: intRow.interviewer_id ? {
            id: intRow.interviewer_id,
            firstName: intRow.interviewer_first_name,
            lastName: intRow.interviewer_last_name
          } : null
        });
      });
    }

    // Transform the data to the expected format
    const applications = result.rows.map(row => ({
      id: row.id,
      status: translateToSpanish(row.status, 'application_status'),
      submissionDate: row.submission_date,
      createdAt: row.created_at,
      updatedAt: row.updated_at,
      additionalNotes: row.additional_notes,

      // Progress information
      progress: {
        totalEvaluations: parseInt(row.total_evaluations),
        completedEvaluations: parseInt(row.completed_evaluations),
        totalInterviews: parseInt(row.total_interviews),
        completedInterviews: parseInt(row.completed_interviews),
        completionPercentage: calculateCompletionPercentage(row)
      },
      
      // Student object
      student: {
        id: row.student_id,
        fullName: `${row.student_first_name} ${row.student_paternal_last_name} ${row.student_maternal_last_name || ''}`.trim(),
        firstName: row.student_first_name,
        lastName: `${row.student_paternal_last_name} ${row.student_maternal_last_name || ''}`.trim(), // Apellido completo para compatibilidad
        paternalLastName: row.student_paternal_last_name,
        maternalLastName: row.student_maternal_last_name || '',
        rut: row.student_rut,
        birthDate: row.student_birth_date,
        gradeApplied: row.student_grade,
        currentSchool: row.student_current_school,
        address: row.student_address
      },
      
      // Parents
      father: row.father_id ? {
        id: row.father_id,
        fullName: row.father_name,
        email: row.father_email,
        phone: row.father_phone,
        profession: row.father_profession
      } : null,
      
      mother: row.mother_id ? {
        id: row.mother_id,
        fullName: row.mother_name,
        email: row.mother_email,
        phone: row.mother_phone,
        profession: row.mother_profession
      } : null,

      // Guardian (Contacto Principal / Apoderado)
      guardian: row.guardian_id ? {
        id: row.guardian_id,
        fullName: row.guardian_name,
        email: row.guardian_email,
        phone: row.guardian_phone,
        relationship: row.guardian_relationship,
        rut: row.guardian_rut
      } : null,

      // Applicant User (Usuario que creó la postulación)
      applicantUser: row.applicant_user_id ? {
        id: row.applicant_user_id,
        firstName: row.applicant_first_name,
        lastName: row.applicant_last_name,
        email: row.applicant_email
      } : null,

      // Evaluations for this application
      evaluations: evaluationsMap[row.id] || [],

      // Interviews for this application
      interviews: interviewsMap[row.id] || []
    }));

    // Traducir estados de applications al español
    const translatedApplications = applications.map(app => ({
      ...app,
      status: translateToSpanish(app.status, 'application_status')
    }));

    // Return standardized paginated response
    res.json(page(translatedApplications, { total, page: pageNum, limit }));

  } catch (error) {
    const errorInfo = handleDatabaseError(error, req.correlationId);
    res.status(errorInfo.status).json(fail(errorInfo.message, {
      errorCode: 'APP_LIST_001',
      details: { correlationId: req.correlationId }
    }));
  } finally {
    client.release();
  }
});

// Helper function to calculate completion percentage
function calculateCompletionPercentage(row) {
  const totalTasks = (parseInt(row.total_evaluations) + parseInt(row.total_interviews)) || 1;
  const completedTasks = parseInt(row.completed_evaluations) + parseInt(row.completed_interviews);
  return Math.round((completedTasks / totalTasks) * 100);
}

// ============= HU-ADMIN-2: ENHANCED APPLICATIONS SEARCH =============
// Advanced search endpoint with filtering, sorting, and pagination
app.get('/api/applications/search', async (req, res) => {
  const client = await dbPool.connect();
  try {
    // Extract query parameters
    const {
      // Search parameters
      search,           // General search (student name or RUT)
      studentName,      // Specific student name search
      studentRut,       // Specific RUT search

      // Filter parameters
      status,           // Application status (can be comma-separated)
      gradeApplied,     // Grade/course
      academicYear,     // Academic year
      schoolApplied,    // School (MONTE_TABOR, NAZARET)

      // Document filters
      documentsComplete, // true/false
      hasSpecialNeeds,   // true/false

      // Parent filters
      parentName,       // Parent/guardian name
      parentEmail,      // Parent email

      // Evaluation filters
      evaluationStatus, // PENDING, IN_PROGRESS, COMPLETED
      minScore,         // Minimum evaluation score
      maxScore,         // Maximum evaluation score

      // Date filters
      submissionDateFrom,
      submissionDateTo,
      interviewDateFrom,
      interviewDateTo,

      // Sorting
      sortBy = 'created_at', // Field to sort by
      sortOrder = 'DESC',    // ASC or DESC

      // Pagination
      page: pageNum = 0,  // 0-based pagination (0 = first page)
      limit = 20
    } = req.query;

    // Build WHERE conditions
    const conditions = [];
    const params = [];
    let paramCount = 0;

    // General search (student name or RUT)
    if (search) {
      paramCount++;
      conditions.push(`(
        LOWER(s.first_name || ' ' || s.paternal_last_name || ' ' || COALESCE(s.maternal_last_name, '')) LIKE $${paramCount}
        OR LOWER(s.rut) LIKE $${paramCount}
      )`);
      params.push(`%${search.toLowerCase()}%`);
    }

    // Specific student name search
    if (studentName) {
      paramCount++;
      conditions.push(`LOWER(s.first_name || ' ' || s.paternal_last_name || ' ' || COALESCE(s.maternal_last_name, '')) LIKE $${paramCount}`);
      params.push(`%${studentName.toLowerCase()}%`);
    }

    // Specific RUT search
    if (studentRut) {
      paramCount++;
      conditions.push(`LOWER(s.rut) LIKE $${paramCount}`);
      params.push(`%${studentRut.toLowerCase()}%`);
    }

    // Status filter (can be multiple, comma-separated)
    if (status) {
      const statusArray = status.split(',').map(s => s.trim());
      paramCount++;
      conditions.push(`a.status = ANY($${paramCount})`);
      params.push(statusArray);
    }

    // Grade filter
    if (gradeApplied) {
      paramCount++;
      conditions.push(`s.grade_applied = $${paramCount}`);
      params.push(gradeApplied);
    }

    // Academic year filter
    if (academicYear) {
      paramCount++;
      conditions.push(`EXTRACT(YEAR FROM a.submission_date) = $${paramCount}`);
      params.push(parseInt(academicYear));
    }

    // Documents complete filter
    if (documentsComplete !== undefined) {
      const docsComplete = documentsComplete === 'true';
      conditions.push(`(SELECT COUNT(*) FROM documents d WHERE d.application_id = a.id) ${docsComplete ? '> 0' : '= 0'}`);
    }

    // Special needs filter - DISABLED: column doesn't exist in students table
    // if (hasSpecialNeeds !== undefined) {
    //   const hasNeeds = hasSpecialNeeds === 'true';
    //   paramCount++;
    //   conditions.push(`s.has_special_needs = $${paramCount}`);
    //   params.push(hasNeeds);
    // }

    // Parent name filter
    if (parentName) {
      paramCount++;
      conditions.push(`(
        LOWER(f.full_name) LIKE $${paramCount}
        OR LOWER(m.full_name) LIKE $${paramCount}
      )`);
      params.push(`%${parentName.toLowerCase()}%`);
    }

    // Parent email filter
    if (parentEmail) {
      paramCount++;
      conditions.push(`(
        LOWER(f.email) LIKE $${paramCount}
        OR LOWER(m.email) LIKE $${paramCount}
      )`);
      params.push(`%${parentEmail.toLowerCase()}%`);
    }

    // Date range filters
    if (submissionDateFrom) {
      paramCount++;
      conditions.push(`a.submission_date >= $${paramCount}`);
      params.push(submissionDateFrom);
    }

    if (submissionDateTo) {
      paramCount++;
      conditions.push(`a.submission_date <= $${paramCount}`);
      params.push(submissionDateTo);
    }

    // Build WHERE clause
    const whereClause = conditions.length > 0 ? `WHERE ${conditions.join(' AND ')}` : '';

    // Validate and sanitize sort field
    const allowedSortFields = [
      'created_at', 'submission_date', 'student_first_name',
      'student_rut', 'status', 'grade_applied'
    ];
    const sortField = allowedSortFields.includes(sortBy) ? sortBy : 'created_at';
    const sortDirection = sortOrder.toUpperCase() === 'ASC' ? 'ASC' : 'DESC';

    // Calculate pagination (0-based: page 0 = first page)
    // pageNum already declared from destructuring above
    const limitNum = parseInt(limit);
    const offset = pageNum * limitNum;  // page 0 → offset 0, page 1 → offset 20, etc.

    // Count total results
    const countQuery = `
      SELECT COUNT(*) as total
      FROM applications a
      LEFT JOIN students s ON s.id = a.student_id
      LEFT JOIN parents f ON f.id = a.father_id AND f.parent_type = 'FATHER'
      LEFT JOIN parents m ON m.id = a.mother_id AND m.parent_type = 'MOTHER'
      ${whereClause}
    `;

    const countResult = await simpleQueryBreaker.fire(client, countQuery, params);
    const totalResults = parseInt(countResult.rows[0].total);

    // Main query with pagination
    const query = `
      SELECT
        a.id,
        a.status,
        a.submission_date,
        a.created_at,
        a.updated_at,
        a.additional_notes,
        a.application_year,

        -- Student information
        s.id as student_id,
        s.first_name as student_first_name,
        s.paternal_last_name as student_paternal_last_name,
        s.maternal_last_name as student_maternal_last_name,
        s.rut as student_rut,
        s.birth_date as student_birth_date,
        s.grade_applied as student_grade,
        s.current_school as student_current_school,
        s.address as student_address,

        -- Father information
        f.id as father_id,
        f.full_name as father_name,
        f.email as father_email,
        f.phone as father_phone,
        f.profession as father_profession,

        -- Mother information
        m.id as mother_id,
        m.full_name as mother_name,
        m.email as mother_email,
        m.phone as mother_phone,
        m.profession as mother_profession,

        -- Document count
        (SELECT COUNT(*) FROM documents d WHERE d.application_id = a.id) as documents_count,

        -- Evaluation stats
        (SELECT COUNT(*) FROM evaluations e WHERE e.application_id = a.id) as total_evaluations,
        (SELECT COUNT(*) FROM evaluations e WHERE e.application_id = a.id AND e.status = 'COMPLETED') as completed_evaluations,
        (SELECT AVG(e.score) FROM evaluations e WHERE e.application_id = a.id AND e.score IS NOT NULL) as avg_evaluation_score,

        -- Interview info
        (SELECT COUNT(*) FROM interviews i WHERE i.application_id = a.id) as total_interviews,
        (SELECT COUNT(*) FROM interviews i WHERE i.application_id = a.id AND i.status = 'COMPLETED') as completed_interviews,
        (SELECT MIN(i.scheduled_date) FROM interviews i WHERE i.application_id = a.id) as next_interview_date

      FROM applications a
      LEFT JOIN students s ON s.id = a.student_id
      LEFT JOIN parents f ON f.id = a.father_id AND f.parent_type = 'FATHER'
      LEFT JOIN parents m ON m.id = a.mother_id AND m.parent_type = 'MOTHER'
      ${whereClause}
      ORDER BY ${sortField === 'student_first_name' ? 's.first_name' : 'a.' + sortField} ${sortDirection}
      LIMIT $${paramCount + 1} OFFSET $${paramCount + 2}
    `;

    params.push(limitNum, offset);

    let result;
    try {
      result = await mediumQueryBreaker.fire(client, query, params);
    } catch (queryError) {
      console.error('❌ Main query error details:', {
        error: queryError.message,
        stack: queryError.stack,
        query: query.substring(0, 200) + '...',
        paramCount: params.length
      });
      throw queryError;
    }

    // Transform the data
    const applications = result.rows.map(row => ({
      id: row.id,
      status: row.status,
      submissionDate: row.submission_date,
      createdAt: row.created_at,
      updatedAt: row.updated_at,
      additionalNotes: row.additional_notes,
      applicationYear: row.application_year,

      // Student object
      student: {
        id: row.student_id,
        fullName: `${row.student_first_name} ${row.student_paternal_last_name} ${row.student_maternal_last_name || ''}`.trim(),
        firstName: row.student_first_name,
        lastName: `${row.student_paternal_last_name} ${row.student_maternal_last_name || ''}`.trim(), // Apellido completo para compatibilidad
        paternalLastName: row.student_paternal_last_name,
        maternalLastName: row.student_maternal_last_name || '',
        rut: row.student_rut,
        birthDate: row.student_birth_date,
        gradeApplied: row.student_grade,
        currentSchool: row.student_current_school,
        address: row.student_address
      },

      // Parents
      father: row.father_id ? {
        id: row.father_id,
        fullName: row.father_name,
        email: row.father_email,
        phone: row.father_phone,
        profession: row.father_profession
      } : null,

      mother: row.mother_id ? {
        id: row.mother_id,
        fullName: row.mother_name,
        email: row.mother_email,
        phone: row.mother_phone,
        profession: row.mother_profession
      } : null,

      // Progress and metadata
      documentsCount: parseInt(row.documents_count),
      documentsComplete: parseInt(row.documents_count) > 0,
      totalEvaluations: parseInt(row.total_evaluations),
      completedEvaluations: parseInt(row.completed_evaluations),
      avgEvaluationScore: row.avg_evaluation_score ? parseFloat(row.avg_evaluation_score).toFixed(1) : null,
      totalInterviews: parseInt(row.total_interviews),
      completedInterviews: parseInt(row.completed_interviews),
      nextInterviewDate: row.next_interview_date
    }));

    // Build response with standardized pagination
    res.json(page(applications, {
      total: totalResults,
      page: pageNum,
      limit: limitNum
    }));

  } catch (error) {
    console.error('❌ Error in enhanced application search:', error);

    if (error.message && error.message.includes('breaker')) {
      return res.status(503).json(fail('Service temporarily unavailable - circuit breaker open', {
        errorCode: 'CIRCUIT_BREAKER_OPEN',
        details: { retryAfter: 30 }
      }));
    }

    res.status(500).json(fail('Error en búsqueda avanzada de postulaciones', {
      errorCode: 'APP_SEARCH_001',
      details: { correlationId: req.correlationId, message: error.message }
    }));
  } finally {
    client.release();
  }
});

app.get('/api/students', (req, res) => {
  res.json({
    message: 'Students from application-service',
    students: [
      { id: 1, name: 'Pedro López', grade: '5th' },
      { id: 2, name: 'María García', grade: '6th' }
    ]
  });
});

// Complete application data template
const createCompleteApplication = (id, studentData, status = 'IN_PROGRESS', currentStep = 3) => ({
  id: id,
  studentName: `${studentData.firstName} ${studentData.lastName}`,
  studentAge: studentData.age,
  grade: studentData.gradeApplied,
  status: status,
  currentStep: currentStep,
  totalSteps: 5,
  createdAt: studentData.createdAt,
  updatedAt: studentData.updatedAt,
  student: {
    firstName: studentData.firstName,
    lastName: studentData.lastName,
    rut: studentData.rut,
    birthDate: studentData.birthDate,
    gradeApplied: studentData.gradeApplied,
    address: 'Av. Los Leones 1234, Providencia',
    currentSchool: studentData.currentSchool
  },
  father: {
    fullName: 'Carlos Eduardo Fernández Vargas',
    rut: '12.345.678-9',
    email: 'familia.fernandez@test.cl',
    phone: '+56912345678',
    occupation: 'Ingeniero',
    workPlace: 'Empresa ABC'
  },
  mother: {
    fullName: 'María Cecilia García López',
    rut: '13.456.789-0',
    email: 'maria.garcia@email.com',
    phone: '+56912345679',
    occupation: 'Profesora',
    workPlace: 'Colegio XYZ'
  },
  documents: studentData.documents,
  interview: studentData.interview
});

// Student data
const sofiaData = {
  firstName: 'Sofía',
  lastName: 'Fernández García',
  age: 8,
  rut: '22.345.678-9',
  birthDate: '2017-03-15',
  gradeApplied: '3° Básico',
  currentSchool: 'Colegio San José',
  createdAt: '2025-08-15T10:00:00Z',
  updatedAt: '2025-09-05T14:30:00Z',
  documents: [
    { id: 1, name: 'Certificado de nacimiento', status: 'uploaded' },
    { id: 2, name: 'Informe de notas', status: 'uploaded' },
    { id: 3, name: 'Certificado médico', status: 'pending' }
  ],
  interview: {
    date: '2025-09-20',
    time: '10:00',
    status: 'SCHEDULED'
  }
};

const diegoData = {
  firstName: 'Diego',
  lastName: 'Fernández García',
  age: 12,
  rut: '23.456.789-0',
  birthDate: '2013-07-22',
  gradeApplied: '7° Básico',
  currentSchool: 'Colegio San José',
  createdAt: '2025-08-20T11:00:00Z',
  updatedAt: '2025-09-01T09:15:00Z',
  documents: [
    { id: 1, name: 'Certificado de nacimiento', status: 'uploaded' },
    { id: 2, name: 'Informe de notas', status: 'pending' },
    { id: 3, name: 'Certificado médico', status: 'pending' }
  ],
  interview: null
};

const isabellaData = {
  firstName: 'Isabella',
  lastName: 'Fernández García',
  age: 5,
  rut: '24.567.890-1',
  birthDate: '2020-01-10',
  gradeApplied: 'Kinder',
  currentSchool: 'Jardín Infantil Pequeños Pasos',
  createdAt: '2025-07-10T09:00:00Z',
  updatedAt: '2025-08-30T16:00:00Z',
  documents: [
    { id: 1, name: 'Certificado de nacimiento', status: 'uploaded' },
    { id: 2, name: 'Informe jardín infantil', status: 'uploaded' },
    { id: 3, name: 'Certificado médico', status: 'uploaded' }
  ],
  interview: {
    date: '2025-08-25',
    time: '11:00',
    status: 'COMPLETED',
    result: 'APPROVED'
  }
};

// Mock applications for public access
app.get('/api/applications/public/mock-applications', (req, res) => {
  const mockApplications = [
    createCompleteApplication(1, sofiaData, 'IN_PROGRESS', 3),
    createCompleteApplication(2, diegoData, 'PENDING_DOCUMENTS', 2),
    createCompleteApplication(3, isabellaData, 'COMPLETED', 5)
  ];

  res.json(mockApplications);
});

// Get all public applications
app.get('/api/applications/public/all', async (req, res) => {
  const client = await dbPool.connect();
  try {
    // Get real applications from database
    const applicationsQuery = await client.query(`
      SELECT
        a.id,
        a.status,
        a.created_at,
        a.updated_at,
        s.first_name,
        s.paternal_last_name,
        s.maternal_last_name,
        s.grade_applied,
        s.current_school
      FROM applications a
      JOIN students s ON s.id = a.student_id
      WHERE a.deleted_at IS NULL
      ORDER BY a.created_at DESC
      LIMIT 50
    `);

    const applications = applicationsQuery.rows.map(row => ({
      id: row.id,
      studentName: `${row.first_name} ${row.paternal_last_name}`,
      grade: row.grade_applied,
      status: translateToSpanish(row.status, 'application_status'),
      currentSchool: row.current_school || 'No especificado',
      createdAt: row.created_at,
      updatedAt: row.updated_at
    }));

    res.json({
      success: true,
      data: applications,
      total: applications.length
    });
  } catch (error) {
    const timestamp = new Date().toISOString();
    console.error(`[${timestamp}] Database error in /api/applications/public/all:`, error.message);
    // Fallback to mock data
    const mockApplications = [
      createCompleteApplication(1, sofiaData, 'APPROVED', 5),
      createCompleteApplication(2, diegoData, 'UNDER_REVIEW', 2),
      createCompleteApplication(3, isabellaData, 'COMPLETED', 5)
    ].map(app => ({
      id: app.id,
      studentName: app.studentName,
      grade: app.grade,
      status: app.status,
      currentSchool: app.student.currentSchool,
      createdAt: app.createdAt,
      updatedAt: app.updatedAt
    }));

    res.json({
      success: true,
      data: mockApplications,
      total: mockApplications.length
    });
  } finally {
    client.release();
  }
});

// Get application statistics - QA Test requirement
app.get('/api/applications/stats', authenticateToken, async (req, res) => {
  const client = await dbPool.connect();
  try {
    // Get counts by status
    const statusQuery = await client.query(`
      SELECT status, COUNT(*) as count
      FROM applications
      GROUP BY status
    `);

    // Get total count
    const totalQuery = await client.query('SELECT COUNT(*) as total FROM applications');
    const total = parseInt(totalQuery.rows[0].total);

    // Build flat stats object
    const stats = {
      total: total,
      pending: 0,
      under_review: 0,
      exam_scheduled: 0,
      approved: 0,
      rejected: 0,
      waitlisted: 0
    };

    // Populate stats from query
    statusQuery.rows.forEach(row => {
      const status = row.status.toLowerCase();
      stats[status] = parseInt(row.count);
    });

    console.log('📊 Application stats:', stats);
    res.json(stats);
  } catch (error) {
    console.error('❌ Error getting application stats:', error);
    res.status(500).json({
      success: false,
      error: 'Error al obtener estadísticas de postulaciones'
    });
  } finally {
    client.release();
  }
});

// Get my applications (for authenticated users)
app.get('/api/applications/my-applications', authenticateToken, async (req, res) => {
  const client = await dbPool.connect();
  try {
    console.log(`🔍 Getting applications for user: ${req.user.email}`);

    // Get real applications from database filtered by current user's email
    const applicationsQuery = await client.query(`
      SELECT
        a.id,
        a.status,
        a.created_at,
        a.updated_at,
        a.applicant_user_id,
        s.first_name,
        s.paternal_last_name,
        s.maternal_last_name,
        s.rut,
        s.birth_date,
        s.grade_applied,
        s.current_school,
        s.address,
        s.email as student_email,
        p_father.full_name as father_name,
        p_father.rut as father_rut,
        p_father.email as father_email,
        p_father.phone as father_phone,
        p_father.profession as father_profession,
        p_father.address as father_address,
        p_mother.full_name as mother_name,
        p_mother.rut as mother_rut,
        p_mother.email as mother_email,
        p_mother.phone as mother_phone,
        p_mother.profession as mother_profession,
        p_mother.address as mother_address,
        sup.full_name as supporter_name,
        sup.rut as supporter_rut,
        sup.email as supporter_email,
        sup.phone as supporter_phone,
        sup.relationship as supporter_relationship,
        gua.full_name as guardian_name,
        gua.rut as guardian_rut,
        gua.email as guardian_email,
        gua.phone as guardian_phone,
        gua.relationship as guardian_relationship,
        u.email as applicant_email,
        u.first_name as applicant_first_name,
        u.last_name as applicant_last_name
      FROM applications a
      JOIN students s ON s.id = a.student_id
      LEFT JOIN parents p_father ON p_father.id = a.father_id
      LEFT JOIN parents p_mother ON p_mother.id = a.mother_id
      LEFT JOIN supporters sup ON sup.id = a.supporter_id
      LEFT JOIN guardians gua ON gua.id = a.guardian_id
      LEFT JOIN users u ON u.id = a.applicant_user_id
      WHERE u.email = $1
      ORDER BY a.created_at DESC
      LIMIT 20
    `, [req.user.email]);

    const applications = applicationsQuery.rows.map(row => ({
      id: row.id,
      studentName: `${row.first_name} ${row.paternal_last_name}`,
      studentAge: new Date().getFullYear() - new Date(row.birth_date).getFullYear(),
      grade: row.grade_applied,
      status: row.status,
      currentStep: getStepFromStatus(row.status),
      totalSteps: 5,
      createdAt: row.created_at,
      updatedAt: row.updated_at,
      student: {
        firstName: row.first_name,
        lastName: `${row.paternal_last_name} ${row.maternal_last_name}`,
        rut: row.rut,
        birthDate: row.birth_date,
        gradeApplied: row.grade_applied,
        address: row.address || 'Dirección no especificada',
        currentSchool: row.current_school || 'Escuela no especificada'
      },
      father: {
        fullName: row.father_name || 'Nombre no especificado',
        rut: row.father_rut || 'RUT no especificado',
        email: row.father_email || 'Email no especificado',
        phone: row.father_phone || 'Teléfono no especificado',
        profession: row.father_profession || 'Profesión no especificada',
        address: row.father_address || 'Dirección no especificada'
      },
      mother: {
        fullName: row.mother_name || 'Nombre no especificado',
        rut: row.mother_rut || 'RUT no especificado',
        email: row.mother_email || 'Email no especificado',
        phone: row.mother_phone || 'Teléfono no especificado',
        profession: row.mother_profession || 'Profesión no especificada',
        address: row.mother_address || 'Dirección no especificada'
      },
      supporter: {
        fullName: row.supporter_name || 'Nombre no especificado',
        rut: row.supporter_rut || 'RUT no especificado',
        email: row.supporter_email || 'Email no especificado',
        phone: row.supporter_phone || 'Teléfono no especificado',
        relationship: row.supporter_relationship || 'No especificada'
      },
      guardian: {
        fullName: row.guardian_name || 'Nombre no especificado',
        rut: row.guardian_rut || 'RUT no especificado',
        email: row.guardian_email || 'Email no especificado',
        phone: row.guardian_phone || 'Teléfono no especificado',
        relationship: row.guardian_relationship || 'No especificada'
      },
      applicantUser: {
        email: row.applicant_email,
        firstName: row.applicant_first_name,
        lastName: row.applicant_last_name
      },
      submissionDate: row.created_at,
      documents: [
        { id: 1, name: 'Certificado de nacimiento', status: 'uploaded' },
        { id: 2, name: 'Informe de notas', status: 'uploaded' },
        { id: 3, name: 'Certificado médico', status: 'pending' }
      ],
      interviewDate: '2025-09-20'
    }));

    res.json(applications);
  } catch (error) {
    const timestamp = new Date().toISOString();
    console.error(`[${timestamp}] Database error in /api/applications/my-applications:`, error.message);
    // Fallback to mock data
    const myApplications = [
      createCompleteApplication(1, sofiaData, 'IN_PROGRESS', 3),
      createCompleteApplication(2, diegoData, 'PENDING_DOCUMENTS', 2),
      createCompleteApplication(3, isabellaData, 'COMPLETED', 5)
    ];
    res.json(myApplications);
  } finally {
    client.release();
  }
});

// Helper function to map status to step number
function getStepFromStatus(status) {
  const statusStepMap = {
    'PENDING': 1,
    'UNDER_REVIEW': 2,
    'DOCUMENTS_REQUESTED': 2,
    'INTERVIEW_SCHEDULED': 3,
    'EXAM_SCHEDULED': 4,
    'APPROVED': 5,
    'REJECTED': 5,
    'WAITLIST': 4,
    'ARCHIVED': 5
  };
  return statusStepMap[status] || 1;
}

// Get application by ID with real database data
app.get('/api/applications/:id', async (req, res) => {
  const applicationId = parseInt(req.params.id);

  if (!applicationId || isNaN(applicationId)) {
    return res.status(400).json({
      success: false,
      error: 'ID de aplicación inválido'
    });
  }

  const client = await dbPool.connect();
  try {
    // Main application query with complete information
    const applicationQuery = `
      SELECT
        a.id,
        a.status,
        a.submission_date,
        a.created_at,
        a.updated_at,
        a.additional_notes,

        -- Student information
        s.id as student_id,
        s.first_name as student_first_name,
        s.paternal_last_name as student_paternal_last_name,
        s.maternal_last_name as student_maternal_last_name,
        s.rut as student_rut,
        s.birth_date as student_birth_date,
        s.grade_applied as student_grade,
        s.current_school as student_current_school,
        s.address as student_address,
        s.email as student_email,
        s.additional_notes as student_notes,

        -- Father information
        f.id as father_id,
        f.full_name as father_name,
        f.rut as father_rut,
        f.email as father_email,
        f.phone as father_phone,
        f.profession as father_profession,
        f.address as father_address,

        -- Mother information
        m.id as mother_id,
        m.full_name as mother_name,
        m.rut as mother_rut,
        m.email as mother_email,
        m.phone as mother_phone,
        m.profession as mother_profession,
        m.address as mother_address,

        -- Applicant user information (guardian who created the application)
        au.email as applicant_email,
        au.first_name as applicant_first_name,
        au.last_name as applicant_last_name

      FROM applications a
      LEFT JOIN students s ON s.id = a.student_id
      LEFT JOIN parents f ON f.id = a.father_id AND f.parent_type = 'FATHER'
      LEFT JOIN parents m ON m.id = a.mother_id AND m.parent_type = 'MOTHER'
      LEFT JOIN users au ON au.id = a.applicant_user_id
      WHERE a.id = $1
    `;

    const applicationResult = await client.query(applicationQuery, [applicationId]);
    
    if (applicationResult.rows.length === 0) {
      return res.status(404).json({
        success: false,
        error: 'Aplicación no encontrada'
      });
    }
    
    const row = applicationResult.rows[0];
    
    // Get interviews for this application
    const interviewsQuery = `
      SELECT
        i.id,
        i.scheduled_date,
        i.duration_minutes,
        i.status,
        i.notes,
        i.interview_type,
        u.first_name || ' ' || u.last_name as interviewer_name
      FROM interviews i
      LEFT JOIN users u ON u.id = i.interviewer_id
      WHERE i.application_id = $1
      ORDER BY i.scheduled_date
    `;

    const interviewsResult = await client.query(interviewsQuery, [applicationId]);

    // Get evaluations for this application
    const evaluationsQuery = `
      SELECT
        e.id,
        e.evaluation_type,
        e.score,
        e.status,
        e.observations,
        e.evaluation_date,
        e.created_at,
        u.first_name || ' ' || u.last_name as evaluator_name
      FROM evaluations e
      LEFT JOIN users u ON u.id = e.evaluator_id
      WHERE e.application_id = $1
      ORDER BY e.evaluation_date DESC
    `;

    const evaluationsResult = await client.query(evaluationsQuery, [applicationId]);

    // Get documents for this application
    const documentsQuery = `
      SELECT
        d.id,
        d.document_type as name,
        d.created_at as upload_date,
        d.file_path,
        d.file_name,
        d.original_name,
        d.file_size,
        d.is_required
      FROM documents d
      WHERE d.application_id = $1
      ORDER BY d.created_at DESC
    `;

    let documentsResult;
    try {
      documentsResult = await client.query(documentsQuery, [applicationId]);
    } catch (error) {
      console.log('Documents table might not exist, using mock data');
      documentsResult = { rows: [] };
    }
    
    // Build the complete application object
    const application = {
      id: row.id,
      status: row.status,
      submissionDate: row.submission_date,
      createdAt: row.created_at,
      updatedAt: row.updated_at,
      additionalNotes: row.additional_notes,
      
      // Student information
      student: {
        id: row.student_id,
        firstName: row.student_first_name,
        lastName: `${row.student_paternal_last_name} ${row.student_maternal_last_name || ''}`.trim(),
        fullName: `${row.student_first_name} ${row.student_paternal_last_name} ${row.student_maternal_last_name || ''}`.trim(),
        rut: row.student_rut,
        birthDate: row.student_birth_date,
        gradeApplied: row.student_grade,
        currentSchool: row.student_current_school || 'No especificado',
        address: row.student_address || 'Dirección no especificada',
        email: row.student_email,
        notes: row.student_notes,
        age: row.student_birth_date ? new Date().getFullYear() - new Date(row.student_birth_date).getFullYear() : null
      },
      
      // Parents information
      father: row.father_id ? {
        id: row.father_id,
        fullName: row.father_name,
        rut: row.father_rut,
        email: row.father_email,
        phone: row.father_phone,
        profession: row.father_profession,
        address: row.father_address
      } : null,

      mother: row.mother_id ? {
        id: row.mother_id,
        fullName: row.mother_name,
        rut: row.mother_rut,
        email: row.mother_email,
        phone: row.mother_phone,
        profession: row.mother_profession,
        address: row.mother_address
      } : null,

      // Applicant user (guardian who created the application)
      applicantUser: row.applicant_email ? {
        email: row.applicant_email,
        firstName: row.applicant_first_name,
        lastName: row.applicant_last_name
      } : null,

      // Interviews
      interviews: interviewsResult.rows.map(interview => ({
        id: interview.id,
        date: interview.scheduled_date ? interview.scheduled_date.toISOString().split('T')[0] : null,
        time: interview.scheduled_date ? interview.scheduled_date.toTimeString().split(' ')[0].substring(0, 5) : null,
        duration: interview.duration_minutes,
        status: interview.status,
        type: interview.interview_type,
        interviewer: interview.interviewer_name,
        notes: interview.notes
      })),
      
      // Evaluations
      evaluations: evaluationsResult.rows.map(evaluation => ({
        id: evaluation.id,
        type: evaluation.evaluation_type,
        score: evaluation.score,
        status: evaluation.status,
        observations: evaluation.observations,
        evaluationDate: evaluation.evaluation_date,
        evaluator: evaluation.evaluator_name,
        createdAt: evaluation.created_at
      })),
      
      // Documents
      documents: documentsResult.rows.length > 0
        ? documentsResult.rows.map(doc => ({
            id: doc.id,
            name: doc.name,
            fileName: doc.file_name,
            originalName: doc.original_name,
            uploadDate: doc.upload_date,
            filePath: doc.file_path,
            fileSize: doc.file_size,
            isRequired: doc.is_required
          }))
        : [],
      
      // Progress summary
      progress: {
        totalInterviews: interviewsResult.rows.length,
        completedInterviews: interviewsResult.rows.filter(i => i.status === 'COMPLETED').length,
        totalEvaluations: evaluationsResult.rows.length,
        completedEvaluations: evaluationsResult.rows.filter(e => e.status === 'COMPLETED').length,
        currentStep: getStepFromStatus(row.status),
        totalSteps: 5
      }
    };

    // Return unwrapped application object (no success/data/timestamp wrapper)
    res.json(application);

  } catch (error) {
    const errorInfo = handleDatabaseError(error, req.correlationId);
    res.status(errorInfo.status).json(fail(errorInfo.message, {
      errorCode: 'APP_GET_001',
      details: { correlationId: req.correlationId }
    }));
  } finally {
    client.release();
  }
});

// Create new application with real database integration - accepting flat structure from frontend
app.post('/api/applications', authenticateToken, validateApplicationInput, async (req, res) => {
  const client = await dbPool.connect();
  try {
    const body = req.body;

    // Get authenticated user ID from JWT token
    const applicantUserId = req.user ? parseInt(req.user.userId) : null;

    // Start transaction
    await client.query('BEGIN');

    try {
      // Validate student RUT
      if (body.rut && !validateRUT(body.rut)) {
        await client.query('ROLLBACK');
        return res.status(400).json(fail(
          'RUT del estudiante inválido. Verifique el formato y dígito verificador.',
          { errorCode: 'APP_002', details: { field: 'rut', value: body.rut } }
        ));
      }

      // Insert student using flat structure from frontend
      const studentQuery = `
        INSERT INTO students (
          first_name, paternal_last_name, maternal_last_name, rut,
          birth_date, grade_applied, current_school, address,
          email, school_applied, created_at, updated_at
        ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, NOW(), NOW())
        RETURNING id
      `;

      const studentResult = await queryWithCircuitBreaker.fire(client, studentQuery, [
        body.firstName,
        body.paternalLastName || body.lastName?.split(' ')[0] || '',
        body.maternalLastName || body.lastName?.split(' ')[1] || '',
        body.rut,
        body.birthDate || '2010-01-01', // default birth date for students
        body.grade, // frontend uses 'grade' not 'gradeApplied'
        body.currentSchool || null,
        body.studentAddress || null,
        body.studentEmail || null,
        body.schoolApplied || 'MONTE_TABOR' // default school
      ]);

      const studentId = studentResult.rows[0].id;
      
      let fatherId = null;
      let motherId = null;
      
      // Insert father if provided (parent1 = father)
      if (body.parent1Name) {
        const fatherQuery = `
          INSERT INTO parents (
            full_name, rut, email, phone, profession,
            address, parent_type, created_at, updated_at
          ) VALUES ($1, $2, $3, $4, $5, $6, 'FATHER', NOW(), NOW())
          RETURNING id
        `;

        const fatherResult = await queryWithCircuitBreaker.fire(client, fatherQuery, [
          body.parent1Name,
          body.parent1Rut || null,
          body.parent1Email || null,
          body.parent1Phone || null,
          body.parent1Profession || null,
          body.parent1Address || null
        ]);
        
        fatherId = fatherResult.rows[0].id;
      }
      
      // Insert mother if provided (parent2 = mother)
      if (body.parent2Name) {
        const motherQuery = `
          INSERT INTO parents (
            full_name, rut, email, phone, profession,
            address, parent_type, created_at, updated_at
          ) VALUES ($1, $2, $3, $4, $5, $6, 'MOTHER', NOW(), NOW())
          RETURNING id
        `;

        const motherResult = await queryWithCircuitBreaker.fire(client, motherQuery, [
          body.parent2Name,
          body.parent2Rut || null,
          body.parent2Email || null,
          body.parent2Phone || null,
          body.parent2Profession || null,
          body.parent2Address || null
        ]);

        motherId = motherResult.rows[0].id;
      }

      // Insert supporter (sostenedor) - REQUIRED
      let supporterId = null;
      if (body.supporterName) {
        const supporterQuery = `
          INSERT INTO supporters (
            full_name, rut, email, phone, relationship, created_at, updated_at
          ) VALUES ($1, $2, $3, $4, $5, NOW(), NOW())
          RETURNING id
        `;

        const supporterResult = await queryWithCircuitBreaker.fire(client, supporterQuery, [
          body.supporterName,
          body.supporterRut || null,
          body.supporterEmail || null,
          body.supporterPhone || null,
          body.supporterRelation || 'OTRO'
        ]);

        supporterId = supporterResult.rows[0].id;
      }

      // Insert guardian (apoderado) - REQUIRED
      let guardianId = null;
      if (body.guardianName) {
        const guardianQuery = `
          INSERT INTO guardians (
            full_name, rut, email, phone, relationship, created_at, updated_at
          ) VALUES ($1, $2, $3, $4, $5, NOW(), NOW())
          RETURNING id
        `;

        const guardianResult = await queryWithCircuitBreaker.fire(client, guardianQuery, [
          body.guardianName,
          body.guardianRut || null,
          body.guardianEmail || null,
          body.guardianPhone || null,
          body.guardianRelation || 'OTRO'
        ]);

        guardianId = guardianResult.rows[0].id;
      }

      // Insert application with application year validation
      const currentYear = new Date().getFullYear();
      const applicationYear = parseInt(body.applicationYear) || currentYear + 1;
      
      // Validate that applicationYear is always current year + 1
      if (applicationYear !== currentYear + 1) {
        throw new Error(`El año de postulación debe ser ${currentYear + 1}`);
      }
      
      const applicationQuery = `
        INSERT INTO applications (
          student_id, father_id, mother_id, supporter_id, guardian_id, applicant_user_id, status,
          submission_date, additional_notes, application_year, created_at, updated_at
        ) VALUES ($1, $2, $3, $4, $5, $6, 'PENDING', NOW(), $7, $8, NOW(), NOW())
        RETURNING id, status, submission_date, application_year, created_at, updated_at
      `;

      const applicationResult = await queryWithCircuitBreaker.fire(client, applicationQuery, [
        studentId,
        fatherId,
        motherId,
        supporterId,
        guardianId,
        applicantUserId,
        body.additionalNotes || null,
        applicationYear
      ]);
      
      const application = applicationResult.rows[0];

      // Commit transaction
      await client.query('COMMIT');

      // Audit log: Application created
      await logAudit(dbPool, {
        userId: applicantUserId || null,
        action: AuditActions.CREATE,
        entityType: EntityTypes.APPLICATION,
        entityId: application.id,
        oldValues: null,
        newValues: {
          status: 'PENDING',
          studentId: studentId,
          applicationYear: applicationYear,
          submissionDate: application.submission_date
        },
        ipAddress: getClientIp(req),
        userAgent: getUserAgent(req)
      });

      // Send notification for new application
      const primaryEmail = body.parent1Email || body.parent2Email || body.studentEmail;
      if (primaryEmail) {
        queueNotification('application_created', {
          recipient: primaryEmail,
          studentName: `${body.firstName} ${body.paternalLastName || body.lastName?.split(' ')[0] || ''}`,
          applicationId: application.id,
          submissionDate: application.submission_date.toISOString().split('T')[0]
        }, req.correlationId);
      }
      
      // Return created application
      const newApplication = {
        id: application.id,
        status: application.status,
        submissionDate: application.submission_date,
        createdAt: application.created_at,
        updatedAt: application.updated_at,
        additionalNotes: body.additionalNotes,
        student: {
          id: studentId,
          firstName: body.firstName,
          lastName: `${body.paternalLastName || body.lastName?.split(' ')[0] || ''} ${body.maternalLastName || body.lastName?.split(' ')[1] || ''}`.trim(),
          fullName: `${body.firstName} ${body.paternalLastName || body.lastName?.split(' ')[0] || ''} ${body.maternalLastName || body.lastName?.split(' ')[1] || ''}`.trim(),
          rut: body.rut,
          birthDate: body.birthDate,
          gradeApplied: body.grade,
          currentSchool: body.currentSchool,
          address: body.studentAddress,
          email: body.studentEmail
        },
        father: body.parent1Name ? {
          id: fatherId,
          fullName: body.parent1Name,
          rut: body.parent1Rut,
          email: body.parent1Email,
          phone: body.parent1Phone,
          profession: body.parent1Profession,
          address: body.parent1Address
        } : null,
        mother: body.parent2Name ? {
          id: motherId,
          fullName: body.parent2Name,
          rut: body.parent2Rut,
          email: body.parent2Email,
          phone: body.parent2Phone,
          profession: body.parent2Profession,
          address: body.parent2Address
        } : null,
        progress: {
          currentStep: 1,
          totalSteps: 5,
          completionPercentage: 20
        }
      };
      
      res.status(201).json({
        success: true,
        message: 'Postulación creada exitosamente',
        id: newApplication.id,
        studentName: `${body.firstName} ${body.lastName}`,
        grade: body.grade,
        status: 'PENDIENTE',
        submissionDate: new Date().toISOString(),
        applicantEmail: body.parent1Email || body.studentEmail
      });

    } catch (error) {
      // Rollback transaction
      await client.query('ROLLBACK');
      throw error;
    }

  } catch (error) {
    const errorInfo = handleDatabaseError(error, req.correlationId);
    res.status(errorInfo.status).json({
      success: false,
      error: errorInfo.message,
      correlationId: req.correlationId,
      details: process.env.NODE_ENV === 'development' ? error.message : undefined
    });
  } finally {
    client.release();
  }
});

// Update application with real database integration (no validation middleware - uses nested structure)
app.put('/api/applications/:id', authenticateToken, async (req, res) => {
  const applicationId = parseInt(req.params.id);

  if (!applicationId || isNaN(applicationId)) {
    return res.status(400).json({
      success: false,
      error: 'ID de aplicación inválido'
    });
  }

  const client = await dbPool.connect();
  try {
    const { status, additionalNotes, student, father, mother, supporter, guardian } = req.body;

    // Start transaction
    await client.query('BEGIN');

    try {
      // First check if application exists, get current status, and verify ownership
      const checkQuery = `
        SELECT
          a.id, a.student_id, a.father_id, a.mother_id, a.applicant_user_id,
          a.status as current_status, a.supporter_id, a.guardian_id,
          s.first_name, s.paternal_last_name,
          f.email as father_email, m.email as mother_email, s.email as student_email
        FROM applications a
        LEFT JOIN students s ON s.id = a.student_id
        LEFT JOIN parents f ON f.id = a.father_id
        LEFT JOIN parents m ON m.id = a.mother_id
        WHERE a.id = $1
      `;
      const checkResult = await client.query(checkQuery, [applicationId]);

      if (checkResult.rows.length === 0) {
        await client.query('ROLLBACK');
        return res.status(404).json({
          success: false,
          error: 'Aplicación no encontrada'
        });
      }

      const existingApp = checkResult.rows[0];

      // Authorization: Only allow owner or admin to edit
      const isOwner = existingApp.applicant_user_id && existingApp.applicant_user_id.toString() === req.user.userId;
      const isAdmin = req.user.role === 'ADMIN' || req.user.role === 'COORDINATOR' || req.user.role === 'CYCLE_DIRECTOR';

      if (!isOwner && !isAdmin) {
        await client.query('ROLLBACK');
        return res.status(403).json({
          success: false,
          error: 'No tienes permisos para editar esta postulación'
        });
      }

      // Status validation: Only allow editing for certain statuses (unless admin)
      const editableStatuses = ['PENDING', 'IN_REVIEW', 'INCOMPLETE'];
      if (!isAdmin && !editableStatuses.includes(existingApp.current_status)) {
        await client.query('ROLLBACK');
        return res.status(400).json({
          success: false,
          error: `No se puede editar una postulación con estado ${existingApp.current_status}. Solo se permiten ediciones para estados: ${editableStatuses.join(', ')}`
        });
      }
      
      // Update student if provided
      if (student) {
        const studentUpdateQuery = `
          UPDATE students SET 
            first_name = COALESCE($1, first_name),
            paternal_last_name = COALESCE($2, paternal_last_name),
            maternal_last_name = COALESCE($3, maternal_last_name),
            rut = COALESCE($4, rut),
            birth_date = COALESCE($5, birth_date),
            grade_applied = COALESCE($6, grade_applied),
            current_school = COALESCE($7, current_school),
            address = COALESCE($8, address),
            email = COALESCE($9, email),
            additional_notes = COALESCE($10, additional_notes),
            updated_at = NOW()
          WHERE id = $11
        `;

        await client.query(studentUpdateQuery, [
          student.firstName,
          student.paternalLastName || student.lastName?.split(' ')[0],
          student.maternalLastName || student.lastName?.split(' ')[1],
          student.rut,
          student.birthDate,
          student.gradeApplied,
          student.currentSchool,
          student.address,
          student.email,
          student.notes || student.additionalNotes,
          existingApp.student_id
        ]);
      }
      
      // Update father if provided
      if (father && existingApp.father_id) {
        const fatherUpdateQuery = `
          UPDATE parents SET
            full_name = COALESCE($1, full_name),
            rut = COALESCE($2, rut),
            email = COALESCE($3, email),
            phone = COALESCE($4, phone),
            profession = COALESCE($5, profession),
            address = COALESCE($6, address),
            updated_at = NOW()
          WHERE id = $7
        `;

        await client.query(fatherUpdateQuery, [
          father.fullName,
          father.rut,
          father.email,
          father.phone,
          father.profession || father.occupation,
          father.address,
          existingApp.father_id
        ]);
      }
      
      // Update mother if provided
      if (mother && existingApp.mother_id) {
        const motherUpdateQuery = `
          UPDATE parents SET
            full_name = COALESCE($1, full_name),
            rut = COALESCE($2, rut),
            email = COALESCE($3, email),
            phone = COALESCE($4, phone),
            profession = COALESCE($5, profession),
            address = COALESCE($6, address),
            updated_at = NOW()
          WHERE id = $7
        `;

        await client.query(motherUpdateQuery, [
          mother.fullName,
          mother.rut,
          mother.email,
          mother.phone,
          mother.profession || mother.occupation,
          mother.address,
          existingApp.mother_id
        ]);
      }

      // Update supporter if provided
      if (supporter && existingApp.supporter_id) {
        const supporterUpdateQuery = `
          UPDATE supporters SET
            full_name = COALESCE($1, full_name),
            rut = COALESCE($2, rut),
            email = COALESCE($3, email),
            phone = COALESCE($4, phone),
            relationship = COALESCE($5, relationship),
            updated_at = NOW()
          WHERE id = $6
        `;

        await client.query(supporterUpdateQuery, [
          supporter.supporterName || supporter.fullName,
          supporter.supporterRut || supporter.rut,
          supporter.supporterEmail || supporter.email,
          supporter.supporterPhone || supporter.phone,
          supporter.supporterRelation || supporter.relationship || 'OTRO',
          existingApp.supporter_id
        ]);
      }

      // Update guardian if provided
      if (guardian && existingApp.guardian_id) {
        const guardianUpdateQuery = `
          UPDATE guardians SET
            full_name = COALESCE($1, full_name),
            rut = COALESCE($2, rut),
            email = COALESCE($3, email),
            phone = COALESCE($4, phone),
            relationship = COALESCE($5, relationship),
            updated_at = NOW()
          WHERE id = $6
        `;

        await client.query(guardianUpdateQuery, [
          guardian.guardianName || guardian.fullName,
          guardian.guardianRut || guardian.rut,
          guardian.guardianEmail || guardian.email,
          guardian.guardianPhone || guardian.phone,
          guardian.guardianRelation || guardian.relationship || 'OTRO',
          existingApp.guardian_id
        ]);
      }

      // Update application
      const applicationUpdateQuery = `
        UPDATE applications SET 
          status = COALESCE($1, status),
          additional_notes = COALESCE($2, additional_notes),
          updated_at = NOW()
        WHERE id = $3
        RETURNING id, status, submission_date, created_at, updated_at, additional_notes
      `;
      
      const applicationResult = await client.query(applicationUpdateQuery, [
        status,
        additionalNotes,
        applicationId
      ]);

      // Commit transaction
      await client.query('COMMIT');
      
      const updatedApplication = applicationResult.rows[0];
      
      // Send status change notification if status changed
      if (status && status !== existingApp.current_status) {
        const primaryEmail = existingApp.father_email || existingApp.mother_email || existingApp.student_email;
        const studentName = `${existingApp.first_name} ${existingApp.paternal_last_name}`;
        
        if (primaryEmail) {
          queueNotification('application_status_change', {
            recipient: primaryEmail,
            studentName: studentName,
            applicationId: applicationId,
            oldStatus: existingApp.current_status,
            newStatus: status,
            statusDate: new Date().toISOString().split('T')[0]
          }, req.correlationId);
        }
      }
      
      res.json({
        success: true,
        data: {
          id: updatedApplication.id,
          status: updatedApplication.status,
          submissionDate: updatedApplication.submission_date,
          createdAt: updatedApplication.created_at,
          updatedAt: updatedApplication.updated_at,
          additionalNotes: updatedApplication.additional_notes
        },
        message: 'Postulación actualizada exitosamente'
      });

    } catch (error) {
      // Rollback transaction
      await client.query('ROLLBACK');
      throw error;
    }

  } catch (error) {
    const errorInfo = handleDatabaseError(error, req.correlationId);
    res.status(errorInfo.status).json({
      success: false,
      error: errorInfo.message,
      correlationId: req.correlationId,
      details: process.env.NODE_ENV === 'development' ? error.message : undefined
    });
  } finally {
    client.release();
  }
});

// ============= MISSING ENDPOINTS REQUIRED BY FRONTEND =============

// Final admission decision endpoint - Approve or Reject application with email notification
app.post('/api/applications/:id/final-decision', authenticateToken, async (req, res) => {
  const client = await dbPool.connect();
  try {
    const applicationId = parseInt(req.params.id);
    const { decision, note } = req.body; // decision: 'APPROVED' or 'REJECTED', note: optional message

    // Validar decisión
    if (!decision || !['APPROVED', 'REJECTED'].includes(decision)) {
      return res.status(400).json({
        success: false,
        message: 'Decisión inválida. Debe ser APPROVED o REJECTED'
      });
    }

    // Obtener información completa de la aplicación
    const appResult = await client.query(`
      SELECT
        a.id,
        a.status as current_status,
        s.first_name,
        s.paternal_last_name,
        s.maternal_last_name,
        s.grade_applied,
        u.email as guardian_email,
        u.first_name as guardian_name
      FROM applications a
      JOIN students s ON s.id = a.student_id
      LEFT JOIN users u ON a.applicant_user_id = u.id
      WHERE a.id = $1
    `, [applicationId]);

    if (appResult.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'Aplicación no encontrada'
      });
    }

    const application = appResult.rows[0];
    const oldStatus = application.status;

    // Actualizar estado de la aplicación
    const newStatus = decision === 'APPROVED' ? 'APPROVED' : 'REJECTED';
    await client.query(
      'UPDATE applications SET status = $1, updated_at = NOW() WHERE id = $2',
      [newStatus, applicationId]
    );

    // Audit log: Status change
    await logAudit(dbPool, {
      userId: req.user ? parseInt(req.user.userId) : null,
      action: AuditActions.STATUS_CHANGE,
      entityType: EntityTypes.APPLICATION,
      entityId: applicationId,
      oldValues: { status: oldStatus },
      newValues: { status: newStatus, decision: decision, note: note },
      ipAddress: getClientIp(req),
      userAgent: getUserAgent(req)
    });

    // Enviar notificación por email
    const templateType = decision === 'APPROVED' ? 'ACCEPTANCE' : 'REJECTION';
    const emailData = {
      studentName: application.student_name,
      guardianName: application.guardian_name,
      gradeApplied: application.grade_applied,
      admissionNote: decision === 'APPROVED' ? note : undefined,
      rejectionReason: decision === 'REJECTED' ? note : undefined
    };

    try {
      await axios.post('http://localhost:8085/api/notifications/send', {
        to: application.guardian_email,
        templateType: templateType,
        data: emailData
      });
      console.log(`📧 Email de ${decision === 'APPROVED' ? 'aceptación' : 'rechazo'} enviado a:`, application.guardian_email);
    } catch (emailError) {
      console.error('❌ Error enviando email:', emailError.message);
      // No fallar la operación si el email falla
    }

    res.json({
      success: true,
      message: `Aplicación ${decision === 'APPROVED' ? 'aprobada' : 'rechazada'} exitosamente`,
      data: {
        id: applicationId,
        status: translateToSpanish(newStatus, 'application_status'),
        emailSent: true
      }
    });

  } catch (error) {
    console.error('❌ Error en decisión final:', error);
    res.status(500).json({
      success: false,
      message: 'Error al procesar la decisión final'
    });
  } finally {
    client.release();
  }
});

// Archive application endpoint - required by applicationService.ts:347
app.put('/api/applications/:id/archive', authenticateToken, async (req, res) => {
  const client = await dbPool.connect();
  try {
    const applicationId = parseInt(req.params.id);

    if (!applicationId || isNaN(applicationId)) {
      return res.status(400).json({
        success: false,
        error: 'ID de aplicación inválido'
      });
    }

    // Update application status to ARCHIVED
    const query = `
      UPDATE applications
      SET status = 'ARCHIVADA', updated_at = NOW()
      WHERE id = $1
      RETURNING id, status
    `;

    const result = await client.query(query, [applicationId]);
    
    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        error: 'Postulación no encontrada'
      });
    }
    
    res.json({
      success: true,
      message: 'Postulación archivada exitosamente',
      id: result.rows[0].id,
      status: result.rows[0].status
    });

  } catch (error) {
    console.error('❌ Error archivando postulación:', error);
    res.status(500).json({
      success: false,
      error: 'Error interno del servidor al archivar la postulación',
      details: error.message
    });
  } finally {
    client.release();
  }
});

// Status change endpoint with audit trail - US-9
app.patch('/api/applications/:id/status', authenticateToken, async (req, res) => {
  const client = await dbPool.connect();

  try {
    const applicationId = parseInt(req.params.id);
    const { newStatus, changeNote } = req.body;
    const userId = req.user?.userId || req.user?.id;

    // Validation
    if (!applicationId || isNaN(applicationId)) {
      return res.status(400).json({
        success: false,
        error: 'ID de aplicación inválido'
      });
    }

    if (!newStatus) {
      return res.status(400).json({
        success: false,
        error: 'El nuevo estado es requerido'
      });
    }

    // Valid status transitions
    const validStatuses = [
      'SUBMITTED', 'ENVIADA',
      'UNDER_REVIEW', 'EN_REVISION',
      'INTERVIEW_SCHEDULED', 'ENTREVISTA_PROGRAMADA',
      'APPROVED', 'ACEPTADA',
      'REJECTED', 'RECHAZADA',
      'WAITLIST', 'LISTA_ESPERA',
      'ARCHIVED', 'ARCHIVADA'
    ];

    if (!validStatuses.includes(newStatus.toUpperCase())) {
      return res.status(400).json({
        success: false,
        error: 'Estado inválido',
        validStatuses: validStatuses
      });
    }

    await client.query('BEGIN');

    // Get current application with student info
    const currentAppQuery = `
      SELECT a.*,
             s.first_name, s.paternal_last_name, s.maternal_last_name, s.email as student_email,
             u.email as guardian_email, u.first_name as guardian_first_name
      FROM applications a
      LEFT JOIN students s ON a.student_id = s.id
      LEFT JOIN users u ON a.applicant_user_id = u.id
      WHERE a.id = $1
    `;
    const currentApp = await mediumQueryBreaker.fire(client, currentAppQuery, [applicationId]);

    if (currentApp.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({
        success: false,
        error: 'Postulación no encontrada'
      });
    }

    const previousStatus = currentApp.rows[0].status;

    // Prevent redundant status changes
    if (previousStatus === newStatus) {
      await client.query('ROLLBACK');
      return res.status(400).json({
        success: false,
        error: 'El estado es el mismo que el actual'
      });
    }

    // Record status change in audit trail
    const historyQuery = `
      INSERT INTO application_status_history
        (application_id, previous_status, new_status, changed_by, change_note)
      VALUES ($1, $2, $3, $4, $5)
      RETURNING id, changed_at
    `;
    const historyResult = await writeOperationBreaker.fire(
      client,
      historyQuery,
      [applicationId, previousStatus, newStatus, userId, changeNote || null]
    );

    // Update application status
    const updateQuery = `
      UPDATE applications
      SET status = $1, updated_at = NOW()
      WHERE id = $2
      RETURNING id, status, updated_at
    `;
    const updateResult = await writeOperationBreaker.fire(
      client,
      updateQuery,
      [newStatus, applicationId]
    );

    await client.query('COMMIT');

    // Send notification to guardian about status change
    const guardianEmail = currentApp.rows[0].guardian_email;
    const studentName = `${currentApp.rows[0].first_name} ${currentApp.rows[0].paternal_last_name} ${currentApp.rows[0].maternal_last_name || ''}`.trim();

    if (guardianEmail) {
      try {
        await axios.post(`${NOTIFICATION_SERVICE_URL}/api/email/send`, {
          to: guardianEmail,
          templateType: 'STATUS_CHANGE',
          data: {
            guardianName: currentApp.rows[0].guardian_first_name || 'Apoderado',
            studentName: studentName,
            previousStatus: previousStatus,
            newStatus: newStatus,
            changeNote: changeNote || ''
          }
        });
        console.log(`✅ Notificación de cambio de estado enviada a ${guardianEmail}`);
      } catch (notifError) {
        console.error('⚠️ Error enviando notificación de cambio de estado:', notifError.message);
        // Don't fail the request if notification fails
      }
    }

    res.json({
      success: true,
      message: 'Estado actualizado exitosamente',
      data: {
        id: updateResult.rows[0].id,
        previousStatus: previousStatus,
        newStatus: updateResult.rows[0].status,
        updatedAt: updateResult.rows[0].updated_at,
        historyId: historyResult.rows[0].id,
        historyCreatedAt: historyResult.rows[0].changed_at
      }
    });

  } catch (error) {
    await client.query('ROLLBACK');
    console.error('❌ Error cambiando estado de postulación:', error);
    res.status(500).json({
      success: false,
      error: 'Error interno del servidor al cambiar el estado',
      details: error.message
    });
  } finally {
    client.release();
  }
});

// Get status history for an application - US-9
app.get('/api/applications/:id/status-history', authenticateToken, async (req, res) => {
  const client = await dbPool.connect();

  try {
    const applicationId = parseInt(req.params.id);

    if (!applicationId || isNaN(applicationId)) {
      return res.status(400).json({
        success: false,
        error: 'ID de aplicación inválido'
      });
    }

    const query = `
      SELECT
        ash.id,
        ash.application_id,
        ash.previous_status,
        ash.new_status,
        ash.change_note,
        ash.changed_at,
        u.first_name as changed_by_first_name,
        u.last_name as changed_by_last_name,
        u.email as changed_by_email
      FROM application_status_history ash
      LEFT JOIN users u ON ash.changed_by = u.id
      WHERE ash.application_id = $1
      ORDER BY ash.changed_at DESC
    `;

    const result = await mediumQueryBreaker.fire(client, query, [applicationId]);

    res.json({
      success: true,
      data: result.rows
    });

  } catch (error) {
    console.error('❌ Error obteniendo historial de estados:', error);
    res.status(500).json({
      success: false,
      error: 'Error interno del servidor al obtener el historial',
      details: error.message
    });
  } finally {
    client.release();
  }
});

// Document upload endpoint - simulated for testing
// ============= DOCUMENT UPLOAD ENDPOINT (REAL IMPLEMENTATION) =============
// POST /api/applications/documents - Upload documents for an application
app.post('/api/applications/documents',
  authenticateToken,
  upload.single('file'), // Single file upload with field name 'file'
  async (req, res) => {
    const client = await dbPool.connect();

    try {
      console.log('📎 Document upload request received');
      console.log('👤 User:', req.user?.email);
      console.log('📋 Body:', req.body);
      console.log('📎 File:', req.file);

      // Validate file was uploaded
      if (!req.file) {
        return res.status(400).json({
          success: false,
          error: 'No se ha enviado ningún archivo. El campo debe llamarse "file"'
        });
      }

      // Extract form data from body
      const { applicationId, documentType, isRequired } = req.body;

      // Validate required fields
      if (!applicationId || !documentType) {
        // Delete uploaded file if validation fails
        fs.unlinkSync(req.file.path);
        return res.status(400).json({
          success: false,
          error: 'applicationId y documentType son requeridos'
        });
      }

      // Validate document type
      if (!VALID_DOCUMENT_TYPES.includes(documentType)) {
        fs.unlinkSync(req.file.path);
        return res.status(400).json({
          success: false,
          error: `Tipo de documento inválido. Tipos permitidos: ${VALID_DOCUMENT_TYPES.join(', ')}`
        });
      }

      // Validate that application exists
      const checkQuery = 'SELECT id FROM applications WHERE id = $1';
      const checkResult = await simpleQueryBreaker.fire(client, checkQuery, [applicationId]);

      if (checkResult.rows.length === 0) {
        fs.unlinkSync(req.file.path);
        return res.status(404).json({
          success: false,
          error: 'Aplicación no encontrada'
        });
      }

      // Insert document metadata into database
      const insertQuery = `
        INSERT INTO documents (
          application_id,
          document_type,
          file_name,
          original_name,
          file_path,
          file_size,
          content_type,
          is_required,
          created_at
        ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, NOW())
        RETURNING id, application_id, document_type, file_name, original_name,
                  file_path, file_size, content_type, is_required, created_at
      `;

      const insertValues = [
        parseInt(applicationId),
        documentType,
        req.file.filename, // Unique generated filename
        req.file.originalname, // Original filename from user
        req.file.path, // Full path to file
        req.file.size, // File size in bytes
        req.file.mimetype, // MIME type
        isRequired === 'true' || isRequired === true // Convert to boolean
      ];

      const insertResult = await writeOperationBreaker.fire(client, insertQuery, insertValues);

      const savedDocument = insertResult.rows[0];

      console.log('✅ Document successfully uploaded and saved to database');
      console.log('📄 Document ID:', savedDocument.id);
      console.log('📁 File path:', savedDocument.file_path);
      console.log('💾 File size:', savedDocument.file_size, 'bytes');

      res.status(201).json({
        success: true,
        message: 'Documento subido exitosamente',
        document: {
          id: savedDocument.id,
          applicationId: savedDocument.application_id,
          documentType: savedDocument.document_type,
          fileName: savedDocument.file_name,
          originalName: savedDocument.original_name,
          filePath: savedDocument.file_path,
          fileSize: savedDocument.file_size,
          contentType: savedDocument.content_type,
          isRequired: savedDocument.is_required,
          uploadDate: savedDocument.created_at
        }
      });

    } catch (error) {
      console.error('❌ Error uploading document:', error);

      // Clean up uploaded file if database operation failed
      if (req.file && fs.existsSync(req.file.path)) {
        try {
          fs.unlinkSync(req.file.path);
          console.log('🗑️ Cleaned up uploaded file after error');
        } catch (cleanupError) {
          console.error('❌ Error cleaning up file:', cleanupError);
        }
      }

      // Handle specific multer errors
      if (error.code === 'LIMIT_FILE_SIZE') {
        return res.status(400).json({
          success: false,
          error: 'El archivo excede el tamaño máximo permitido de 10 MB'
        });
      }

      if (error.code === 'LIMIT_UNEXPECTED_FILE') {
        return res.status(400).json({
          success: false,
          error: 'Campo de archivo inesperado. Use "file" como nombre del campo'
        });
      }

      res.status(500).json({
        success: false,
        error: 'Error interno del servidor al subir documento',
        details: process.env.NODE_ENV === 'development' ? error.message : undefined
      });
    } finally {
      client.release();
    }
  }
);

// GET /api/applications/:id/documents - Get all documents for an application
app.get('/api/applications/:id/documents', authenticateToken, async (req, res) => {
  const client = await dbPool.connect();

  try {
    const applicationId = parseInt(req.params.id);

    if (!applicationId || isNaN(applicationId)) {
      return res.status(400).json({
        success: false,
        error: 'ID de aplicación inválido'
      });
    }

    // Validate application exists
    const checkQuery = 'SELECT id FROM applications WHERE id = $1';
    const checkResult = await simpleQueryBreaker.fire(client, checkQuery, [applicationId]);

    if (checkResult.rows.length === 0) {
      return res.status(404).json({
        success: false,
        error: 'Aplicación no encontrada'
      });
    }

    // Get all documents for this application
    const documentsQuery = `
      SELECT
        id,
        application_id,
        document_type,
        file_name,
        original_name,
        file_path,
        file_size,
        content_type,
        is_required,
        created_at,
        updated_at
      FROM documents
      WHERE application_id = $1
      ORDER BY created_at DESC
    `;

    const documentsResult = await mediumQueryBreaker.fire(client, documentsQuery, [applicationId]);

    res.json({
      success: true,
      documents: documentsResult.rows,
      count: documentsResult.rows.length
    });

  } catch (error) {
    console.error('❌ Error fetching documents:', error);
    res.status(500).json({
      success: false,
      error: 'Error interno del servidor al obtener documentos',
      details: process.env.NODE_ENV === 'development' ? error.message : undefined
    });
  } finally {
    client.release();
  }
});

// DELETE /api/applications/documents/:documentId - Delete a document
app.delete('/api/applications/documents/:documentId', authenticateToken, async (req, res) => {
  const client = await dbPool.connect();

  try {
    const documentId = parseInt(req.params.documentId);

    if (!documentId || isNaN(documentId)) {
      return res.status(400).json({
        success: false,
        error: 'ID de documento inválido'
      });
    }

    // Get document info before deleting
    const selectQuery = 'SELECT id, file_path FROM documents WHERE id = $1';
    const selectResult = await simpleQueryBreaker.fire(client, selectQuery, [documentId]);

    if (selectResult.rows.length === 0) {
      return res.status(404).json({
        success: false,
        error: 'Documento no encontrado'
      });
    }

    const document = selectResult.rows[0];

    // Delete from database
    const deleteQuery = 'DELETE FROM documents WHERE id = $1';
    await writeOperationBreaker.fire(client, deleteQuery, [documentId]);

    // Delete physical file
    if (fs.existsSync(document.file_path)) {
      fs.unlinkSync(document.file_path);
      console.log('🗑️ File deleted:', document.file_path);
    }

    res.json({
      success: true,
      message: 'Documento eliminado exitosamente',
      documentId: documentId
    });

  } catch (error) {
    console.error('❌ Error deleting document:', error);
    res.status(500).json({
      success: false,
      error: 'Error interno del servidor al eliminar documento',
      details: process.env.NODE_ENV === 'development' ? error.message : undefined
    });
  } finally {
    client.release();
  }
});

// GET /api/documents/:documentId/download - Download a document
app.get('/api/documents/:documentId/download', authenticateToken, async (req, res) => {
  const client = await dbPool.connect();

  try {
    const documentId = parseInt(req.params.documentId);

    if (!documentId || isNaN(documentId)) {
      return res.status(400).json({
        success: false,
        error: 'ID de documento inválido'
      });
    }

    // Get document info
    const documentQuery = 'SELECT * FROM documents WHERE id = $1';
    const documentResult = await client.query(documentQuery, [documentId]);

    if (documentResult.rows.length === 0) {
      return res.status(404).json({
        success: false,
        error: 'Documento no encontrado'
      });
    }

    const document = documentResult.rows[0];

    // Authorization: User must own the application or be admin
    const appQuery = 'SELECT applicant_user_id FROM applications WHERE id = $1';
    const appResult = await client.query(appQuery, [document.application_id]);

    if (appResult.rows.length === 0) {
      return res.status(404).json({
        success: false,
        error: 'Aplicación no encontrada'
      });
    }

    const isOwner = appResult.rows[0].applicant_user_id &&
                    appResult.rows[0].applicant_user_id.toString() === req.user.userId;
    const isAdmin = req.user.role === 'ADMIN' ||
                    req.user.role === 'COORDINATOR' ||
                    req.user.role === 'CYCLE_DIRECTOR';

    if (!isOwner && !isAdmin) {
      return res.status(403).json({
        success: false,
        error: 'No tienes permisos para descargar este documento'
      });
    }

    // Check if file exists
    const fs = require('fs');
    if (!fs.existsSync(document.file_path)) {
      return res.status(404).json({
        success: false,
        error: 'Archivo no encontrado en el servidor'
      });
    }

    // Send file for inline viewing (not download)
    res.setHeader('Content-Type', document.content_type || 'application/octet-stream');
    res.setHeader('Content-Disposition', `inline; filename="${document.original_name}"`);
    res.sendFile(document.file_path);

  } catch (error) {
    console.error('❌ Error downloading document:', error);
    res.status(500).json({
      success: false,
      error: 'Error al descargar el documento'
    });
  } finally {
    client.release();
  }
});

// ============================================================================
// PDF RECEIPT GENERATION
// ============================================================================
app.get('/api/applications/:id/receipt', authenticateToken, async (req, res) => {
  const applicationId = parseInt(req.params.id);

  if (!applicationId || isNaN(applicationId)) {
    return res.status(400).json({
      success: false,
      error: 'ID de aplicación inválido'
    });
  }

  const client = await dbPool.connect();
  try {
    // Fetch complete application data with all relationships
    const query = `
      SELECT
        a.id, a.status, a.submission_date, a.created_at, a.additional_notes, a.application_year,
        a.applicant_user_id,
        s.first_name, s.paternal_last_name, s.maternal_last_name, s.rut as student_rut,
        s.birth_date, s.grade_applied, s.current_school, s.address as student_address,
        s.email as student_email,
        f.full_name as father_name, f.rut as father_rut, f.email as father_email,
        f.phone as father_phone, f.profession as father_profession,
        m.full_name as mother_name, m.rut as mother_rut, m.email as mother_email,
        m.phone as mother_phone, m.profession as mother_profession,
        sup.full_name as supporter_name, sup.rut as supporter_rut, sup.email as supporter_email,
        sup.phone as supporter_phone, sup.relationship as supporter_relationship,
        g.full_name as guardian_name, g.rut as guardian_rut, g.email as guardian_email,
        g.phone as guardian_phone, g.relationship as guardian_relationship
      FROM applications a
      LEFT JOIN students s ON s.id = a.student_id
      LEFT JOIN parents f ON f.id = a.father_id
      LEFT JOIN parents m ON m.id = a.mother_id
      LEFT JOIN supporters sup ON sup.id = a.supporter_id
      LEFT JOIN guardians g ON g.id = a.guardian_id
      WHERE a.id = $1
    `;

    const result = await mediumQueryBreaker.fire(client, query, [applicationId]);

    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        error: 'Aplicación no encontrada'
      });
    }

    const app_data = result.rows[0];

    // Authorization: Only allow owner or admin to download receipt
    const isOwner = app_data.applicant_user_id && app_data.applicant_user_id.toString() === req.user.userId;
    const isAdmin = req.user.role === 'ADMIN' || req.user.role === 'COORDINATOR' || req.user.role === 'CYCLE_DIRECTOR';

    if (!isOwner && !isAdmin) {
      return res.status(403).json({
        success: false,
        error: 'No tienes permisos para descargar este comprobante'
      });
    }

    // Create PDF document
    const doc = new PDFDocument({ margin: 50, size: 'LETTER' });

    // Set response headers
    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader('Content-Disposition', `attachment; filename=comprobante-postulacion-${applicationId}.pdf`);

    // Pipe PDF to response
    doc.pipe(res);

    // Header with school logo (text-based for now)
    doc.fontSize(20).font('Helvetica-Bold').text('Colegio Monte Tabor y Nazaret', { align: 'center' });
    doc.fontSize(12).font('Helvetica').text('Sistema de Admisión', { align: 'center' });
    doc.moveDown(0.5);
    doc.fontSize(10).text('www.mtn.cl', { align: 'center' });
    doc.moveDown(2);

    // Title
    doc.fontSize(18).font('Helvetica-Bold').text('COMPROBANTE DE POSTULACIÓN', { align: 'center' });
    doc.moveDown(1);

    // Application info box
    const currentY = doc.y;
    doc.rect(50, currentY, 500, 80).stroke();
    doc.fontSize(11).font('Helvetica');
    doc.text(`Número de Postulación: ${String(applicationId).padStart(6, '0')}`, 60, currentY + 10);
    doc.text(`Estado: ${app_data.status}`, 60, currentY + 30);
    doc.text(`Fecha de Envío: ${app_data.submission_date ? new Date(app_data.submission_date).toLocaleDateString('es-CL') : 'Pendiente'}`, 60, currentY + 50);
    doc.text(`Año de Postulación: ${app_data.application_year || new Date().getFullYear() + 1}`, 350, currentY + 10);
    doc.moveDown(5);

    // Student information
    doc.fontSize(14).font('Helvetica-Bold').text('Datos del Estudiante', { underline: true });
    doc.moveDown(0.5);
    doc.fontSize(11).font('Helvetica');
    doc.text(`Nombre Completo: ${app_data.first_name} ${app_data.paternal_last_name} ${app_data.maternal_last_name || ''}`);
    doc.text(`RUT: ${app_data.student_rut || 'No proporcionado'}`);
    doc.text(`Fecha de Nacimiento: ${app_data.birth_date ? new Date(app_data.birth_date).toLocaleDateString('es-CL') : 'No proporcionada'}`);
    doc.text(`Curso al que Postula: ${app_data.grade_applied || 'No especificado'}`);
    doc.text(`Colegio Actual: ${app_data.current_school || 'No especificado'}`);
    doc.text(`Email: ${app_data.student_email || 'No proporcionado'}`);
    doc.moveDown(1);

    // Father information
    if (app_data.father_name) {
      doc.fontSize(14).font('Helvetica-Bold').text('Datos del Padre', { underline: true });
      doc.moveDown(0.5);
      doc.fontSize(11).font('Helvetica');
      doc.text(`Nombre: ${app_data.father_name}`);
      doc.text(`RUT: ${app_data.father_rut || 'No proporcionado'}`);
      doc.text(`Email: ${app_data.father_email || 'No proporcionado'}`);
      doc.text(`Teléfono: ${app_data.father_phone || 'No proporcionado'}`);
      doc.text(`Profesión: ${app_data.father_profession || 'No proporcionada'}`);
      doc.moveDown(1);
    }

    // Mother information
    if (app_data.mother_name) {
      doc.fontSize(14).font('Helvetica-Bold').text('Datos de la Madre', { underline: true });
      doc.moveDown(0.5);
      doc.fontSize(11).font('Helvetica');
      doc.text(`Nombre: ${app_data.mother_name}`);
      doc.text(`RUT: ${app_data.mother_rut || 'No proporcionado'}`);
      doc.text(`Email: ${app_data.mother_email || 'No proporcionado'}`);
      doc.text(`Teléfono: ${app_data.mother_phone || 'No proporcionado'}`);
      doc.text(`Profesión: ${app_data.mother_profession || 'No proporcionada'}`);
      doc.moveDown(1);
    }

    // Supporter information
    if (app_data.supporter_name) {
      doc.fontSize(14).font('Helvetica-Bold').text('Datos del Apoderado Suplente', { underline: true });
      doc.moveDown(0.5);
      doc.fontSize(11).font('Helvetica');
      doc.text(`Nombre: ${app_data.supporter_name}`);
      doc.text(`RUT: ${app_data.supporter_rut || 'No proporcionado'}`);
      doc.text(`Email: ${app_data.supporter_email || 'No proporcionado'}`);
      doc.text(`Teléfono: ${app_data.supporter_phone || 'No proporcionado'}`);
      doc.text(`Relación: ${app_data.supporter_relationship || 'No especificada'}`);
      doc.moveDown(1);
    }

    // Guardian information
    if (app_data.guardian_name) {
      doc.fontSize(14).font('Helvetica-Bold').text('Datos del Tutor/Apoderado', { underline: true });
      doc.moveDown(0.5);
      doc.fontSize(11).font('Helvetica');
      doc.text(`Nombre: ${app_data.guardian_name}`);
      doc.text(`RUT: ${app_data.guardian_rut || 'No proporcionado'}`);
      doc.text(`Email: ${app_data.guardian_email || 'No proporcionado'}`);
      doc.text(`Teléfono: ${app_data.guardian_phone || 'No proporcionado'}`);
      doc.text(`Relación: ${app_data.guardian_relationship || 'No especificada'}`);
      doc.moveDown(1);
    }

    // Additional notes
    if (app_data.additional_notes) {
      doc.fontSize(14).font('Helvetica-Bold').text('Notas Adicionales', { underline: true });
      doc.moveDown(0.5);
      doc.fontSize(11).font('Helvetica');
      doc.text(app_data.additional_notes, { align: 'justify' });
      doc.moveDown(1);
    }

    // Footer
    doc.moveDown(2);
    doc.fontSize(9).font('Helvetica-Oblique');
    doc.text('Este comprobante es un documento oficial de postulación.', { align: 'center' });
    doc.text(`Generado el: ${new Date().toLocaleDateString('es-CL')} a las ${new Date().toLocaleTimeString('es-CL')}`, { align: 'center' });
    doc.moveDown(0.5);
    doc.text('Para más información, contacte al Colegio Monte Tabor y Nazaret', { align: 'center' });

    // Finalize PDF
    doc.end();

    console.log(`✅ PDF receipt generated for application ${applicationId} by user ${req.user.email}`);

  } catch (error) {
    console.error('❌ Error generating PDF receipt:', error);
    const errorInfo = handleDatabaseError(error, req.correlationId);

    // If response hasn't been sent yet
    if (!res.headersSent) {
      res.status(errorInfo.status).json({
        success: false,
        error: 'Error generando el comprobante PDF',
        correlationId: req.correlationId,
        details: process.env.NODE_ENV === 'development' ? error.message : undefined
      });
    }
  } finally {
    client.release();
  }
});

app.listen(port, () => {
  console.log(`Application Service running on port ${port}`);
});
