/**
 * Mock Guardian Service
 *
 * Este servicio maneja SOLO a los APODERADOS (padres/tutores de postulantes)
 * Los apoderados son usuarios EXTERNOS al colegio que:
 * - Registran las postulaciones de sus hijos
 * - Suben documentos
 * - Revisan el estado del proceso
 * - NO participan en evaluaciones ni gestión interna
 *
 * ARQUITECTURA PROPUESTA:
 * - Tabla separada 'guardians' en la base de datos
 * - Sistema de autenticación independiente (posiblemente con menos privilegios)
 * - Vinculación con applications mediante guardian_id
 */

const express = require('express');
const compression = require('compression');
const CircuitBreaker = require('opossum');
const { Pool } = require('pg');
const bcrypt = require('bcryptjs');
const { validateRUT } = require('./utils/validateRUT');
const createLogger = require('./logger');
const logger = createLogger('guardian-service');
const app = express();
const port = 8087; // Puerto diferente al user-service y notification-service

app.use(express.json());
app.use(compression({
  filter: (req, res) => {
    if (req.headers['x-no-compression']) return false;
    return compression.filter(req, res);
  },
  threshold: 1024,
  level: 6
}));

// ============= DATABASE CONNECTION POOL =============
const dbPool = new Pool({
  host: process.env.DB_HOST || 'localhost',
  port: parseInt(process.env.DB_PORT || '5432'),
  database: process.env.DB_NAME || 'Admisión_MTN_DB',
  user: process.env.DB_USERNAME || 'admin',
  password: process.env.DB_PASSWORD || 'admin123',
  ssl: false,                 // No SSL for Railway internal network
  max: 20,                    // 20 connections per service
  idleTimeoutMillis: 30000,   // Close idle after 30s
  connectionTimeoutMillis: 2000, // 2s connection timeout
  query_timeout: 5000         // 5s query timeout
});

dbPool.on('error', (err) => {
  logger.error('⚠️ Unexpected database pool error:', err);
});

logger.info('✅ Database connection pool initialized (max: 20 connections)');

// ============= DIFFERENTIATED CIRCUIT BREAKERS =============
// 3 circuit breaker categories for Guardian Service
// (No necesita Heavy ni External - sin analytics ni llamadas SMTP/S3)

// 1. Simple Queries (2s, 60% threshold, 20s reset) - Fast lookups
const simpleQueryBreakerOptions = {
  timeout: 2000,
  errorThresholdPercentage: 60,
  resetTimeout: 20000,
  rollingCountTimeout: 10000,
  rollingCountBuckets: 10,
  name: 'GuardianSimpleQueryBreaker'
};

// 2. Medium Queries (5s, 50% threshold, 30s reset) - Standard queries
const mediumQueryBreakerOptions = {
  timeout: 5000,
  errorThresholdPercentage: 50,
  resetTimeout: 30000,
  rollingCountTimeout: 10000,
  rollingCountBuckets: 10,
  name: 'GuardianMediumQueryBreaker'
};

// 3. Write Operations (3s, 30% threshold, 45s reset) - Guardian registration/updates
const writeOperationBreakerOptions = {
  timeout: 3000,
  errorThresholdPercentage: 30,
  resetTimeout: 45000,
  rollingCountTimeout: 10000,
  rollingCountBuckets: 10,
  name: 'GuardianWriteBreaker'
};

// Create circuit breakers (for future DB integration)
const simpleQueryBreaker = new CircuitBreaker(
  async (fn) => await fn(),
  simpleQueryBreakerOptions
);

const mediumQueryBreaker = new CircuitBreaker(
  async (fn) => await fn(),
  mediumQueryBreakerOptions
);

const writeOperationBreaker = new CircuitBreaker(
  async (fn) => await fn(),
  writeOperationBreakerOptions
);

// Event listeners for all breakers
const setupBreakerEvents = (breaker, name) => {
  breaker.on('open', () => {
    logger.error(`⚠️ [Circuit Breaker ${name}] OPEN - Too many failures in guardian service`);
  });

  breaker.on('halfOpen', () => {
    logger.warn(`🔄 [Circuit Breaker ${name}] HALF-OPEN - Testing recovery`);
  });

  breaker.on('close', () => {
    logger.info(`✅ [Circuit Breaker ${name}] CLOSED - Guardian service recovered`);
  });

  breaker.fallback(() => {
    throw new Error(`Service temporarily unavailable - ${name} circuit breaker open`);
  });
};

// Setup events for all breakers
setupBreakerEvents(simpleQueryBreaker, 'Simple');
setupBreakerEvents(mediumQueryBreaker, 'Medium');
setupBreakerEvents(writeOperationBreaker, 'Write');

// ============= STANDARDIZED RESPONSE HELPERS =============
/**
 * Standardized response wrapper for all API responses
 * Ensures consistent contract with frontend
 */
const ResponseHelper = {
  /**
   * Success response for single entity
   * @param {Object} data - The data to return
   * @returns {Object} Standardized response with success, data, timestamp
   */
  ok(data) {
    return {
      success: true,
      data: data,
      timestamp: new Date().toISOString()
    };
  },

  /**
   * Success response for paginated lists
   * @param {Array} items - Array of items
   * @param {Object} meta - Pagination metadata {total, page, limit}
   * @returns {Object} Standardized paginated response
   */
  page(items, meta) {
    const { total, page, limit } = meta;
    const totalPages = Math.ceil(total / limit);
    const hasNext = page < totalPages - 1;
    const hasPrev = page > 0;

    return {
      success: true,
      data: items,
      total: total,
      page: page,
      limit: limit,
      totalPages: totalPages,
      hasNext: hasNext,
      hasPrev: hasPrev,
      timestamp: new Date().toISOString()
    };
  },

  /**
   * Error response
   * @param {String} error - Error message
   * @param {Object} options - Optional {errorCode, details}
   * @returns {Object} Standardized error response
   */
  fail(error, options = {}) {
    const response = {
      success: false,
      error: error,
      timestamp: new Date().toISOString()
    };

    if (options.errorCode) response.errorCode = options.errorCode;
    if (options.details) response.details = options.details;

    return response;
  }
};

// Middleware to simulate JWT verification for guardians
const authenticateGuardian = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (token && token.split('.').length === 3) {
    req.guardian = {
      guardianId: "1",
      email: "carmen.garcia@email.com",
      role: "GUARDIAN"
    };
    next();
  } else {
    res.status(401).json({ error: 'Access token required for guardian' });
  }
};

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ 
    status: 'UP', 
    service: 'guardian-service',
    port: port,
    timestamp: new Date().toISOString()
  });
});

// Datos de APODERADOS/GUARDIANS
const guardians = [
  { 
    id: 1, 
    firstName: 'Carmen Lucia', 
    lastName: 'García Rodríguez', 
    fullName: 'Carmen Lucia García Rodríguez',
    email: 'carmen.garcia@email.com', 
    rut: '17181920-1',
    phone: '+56917181920',
    address: 'Av. Las Condes 123, Santiago',
    relationshipType: 'MADRE',
    active: true,
    emailVerified: true,
    createdAt: '2024-01-10T10:00:00Z',
    // Información adicional específica de apoderados
    emergencyContact: true,
    authorizedToPickup: true,
    // Referencias a sus hijos/postulantes
    applicantIds: [1, 2] // IDs de las postulaciones de sus hijos
  },
  { 
    id: 2, 
    firstName: 'Luis Fernando', 
    lastName: 'Martínez López', 
    fullName: 'Luis Fernando Martínez López',
    email: 'luis.martinez@gmail.com', 
    rut: '18192021-2',
    phone: '+56918192021',
    address: 'Providencia 456, Santiago',
    relationshipType: 'PADRE',
    active: true,
    emailVerified: false,
    createdAt: '2024-02-15T14:30:00Z',
    emergencyContact: true,
    authorizedToPickup: true,
    applicantIds: [3]
  },
  { 
    id: 3, 
    firstName: 'Rosa María', 
    lastName: 'Jiménez Valdez', 
    fullName: 'Rosa María Jiménez Valdez',
    email: 'rosa.jimenez@hotmail.com', 
    rut: '19202122-3',
    phone: '+56919202122',
    address: 'La Reina 789, Santiago',
    relationshipType: 'MADRE',
    active: true,
    emailVerified: true,
    createdAt: '2024-03-08T11:45:00Z',
    emergencyContact: true,
    authorizedToPickup: true,
    applicantIds: [4]
  },
  { 
    id: 4, 
    firstName: 'Pedro Antonio', 
    lastName: 'Sánchez Díaz', 
    fullName: 'Pedro Antonio Sánchez Díaz',
    email: 'pedro.sanchez@empresa.cl', 
    rut: '20212223-4',
    phone: '+56920212223',
    address: 'Vitacura 321, Santiago',
    relationshipType: 'PADRE',
    active: false, // Cuenta desactivada
    emailVerified: true,
    createdAt: '2024-04-20T09:15:00Z',
    emergencyContact: false,
    authorizedToPickup: true,
    applicantIds: [5]
  },
  { 
    id: 5, 
    firstName: 'Isabel Cristina', 
    lastName: 'Moreno Castillo', 
    fullName: 'Isabel Cristina Moreno Castillo',
    email: 'isabel.moreno@correo.cl', 
    rut: '21222324-5',
    phone: '+56921222324',
    address: 'Lo Barnechea 654, Santiago',
    relationshipType: 'TUTORA',
    active: true,
    emailVerified: false,
    createdAt: '2024-05-12T15:00:00Z',
    emergencyContact: true,
    authorizedToPickup: true,
    applicantIds: [6, 7]
  }
];

// Endpoints para guardians

// Obtener todos los apoderados (protegido - solo admin/staff)
app.get('/api/guardians', authenticateGuardian, (req, res) => {
  const activeOnly = req.query.active === 'true';
  const page = parseInt(req.query.page) || 0;
  const limit = parseInt(req.query.limit) || 10;

  let filteredGuardians = guardians;

  if (activeOnly) {
    filteredGuardians = guardians.filter(g => g.active);
  }

  const total = filteredGuardians.length;
  const startIndex = page * limit;
  const endIndex = startIndex + limit;
  const paginatedData = filteredGuardians.slice(startIndex, endIndex);

  const response = ResponseHelper.page(paginatedData, { total, page, limit });
  response.guardians = paginatedData;  // Add guardians field for frontend
  res.json(response);
});

// Estadísticas de apoderados
app.get('/api/guardians/stats', authenticateGuardian, (req, res) => {
  const stats = {
    totalGuardians: guardians.length,
    activeGuardians: guardians.filter(g => g.active).length,
    verifiedEmails: guardians.filter(g => g.emailVerified).length,
    relationshipTypes: {
      madre: guardians.filter(g => g.relationshipType === 'MADRE').length,
      padre: guardians.filter(g => g.relationshipType === 'PADRE').length,
      tutor: guardians.filter(g => g.relationshipType === 'TUTORA' || g.relationshipType === 'TUTOR').length,
      otros: guardians.filter(g => !['MADRE', 'PADRE', 'TUTORA', 'TUTOR'].includes(g.relationshipType)).length
    },
    totalApplications: guardians.reduce((acc, g) => acc + g.applicantIds.length, 0)
  };

  res.json(ResponseHelper.ok(stats));
});

// Obtener apoderado por ID
app.get('/api/guardians/:id', authenticateGuardian, (req, res) => {
  const guardianId = parseInt(req.params.id);
  const guardian = guardians.find(g => g.id === guardianId);

  if (guardian) {
    res.json(ResponseHelper.ok(guardian));
  } else {
    res.status(404).json(ResponseHelper.fail('Apoderado no encontrado', { errorCode: 'GUARDIAN_404' }));
  }
});

// Obtener apoderados por IDs de postulación
app.get('/api/guardians/by-application/:applicationId', authenticateGuardian, (req, res) => {
  const applicationId = parseInt(req.params.applicationId);
  const guardian = guardians.find(g => g.applicantIds.includes(applicationId));

  if (guardian) {
    res.json(ResponseHelper.ok(guardian));
  } else {
    res.status(404).json(ResponseHelper.fail('No se encontró apoderado para esta postulación', { errorCode: 'GUARDIAN_NOT_FOUND_FOR_APP' }));
  }
});

// Login específico para apoderados
app.post('/api/guardians/auth/login', (req, res) => {
  const { email, password } = req.body;
  
  // Mock authentication para apoderados
  const guardian = guardians.find(g => g.email === email && g.active);
  
  if (guardian) {
    res.json({
      success: true,
      token: 'mock-jwt-token-for-guardian',
      guardian: {
        id: guardian.id,
        firstName: guardian.firstName,
        lastName: guardian.lastName,
        email: guardian.email,
        role: 'GUARDIAN'
      }
    });
  } else {
    res.status(401).json({
      success: false,
      error: 'Credenciales inválidas'
    });
  }
});

// Registro de nuevo apoderado (público - para postulaciones)
app.post('/api/guardians/auth/register', async (req, res) => {
  const { firstName, lastName, email, rut, phone, address, relationshipType, password } = req.body;

  try {
    // Validate required fields
    if (!firstName || !lastName || !email || !rut) {
      return res.status(400).json(ResponseHelper.fail(
        'Campos requeridos faltantes: firstName, lastName, email, rut',
        { errorCode: 'VAL_001', details: { missing: ['firstName', 'lastName', 'email', 'rut'].filter(f => !req.body[f]) } }
      ));
    }

    // Validate Chilean RUT
    if (!validateRUT(rut)) {
      return res.status(400).json(ResponseHelper.fail(
        'RUT inválido. Verifique el formato y dígito verificador.',
        { errorCode: 'APP_002', details: { field: 'rut', value: rut } }
      ));
    }

    // Verificar si ya existe en la base de datos
    const existingCheck = await dbPool.query(
      'SELECT id FROM guardians WHERE email = $1 OR rut = $2',
      [email, rut]
    );

    if (existingCheck.rows.length > 0) {
      return res.status(400).json({
        success: false,
        error: 'Ya existe un apoderado con este email o RUT',
        errorCode: 'RES_409'
      });
    }

    // Crear fullName
    const fullName = `${firstName} ${lastName}`;
    const relationship = relationshipType || 'PADRE';

    // Insertar guardian en la base de datos
    const guardianQuery = `
      INSERT INTO guardians (full_name, rut, email, phone, relationship, created_at, updated_at)
      VALUES ($1, $2, $3, $4, $5, NOW(), NOW())
      RETURNING id, full_name as "fullName", rut, email, phone, relationship, created_at as "createdAt"
    `;

    const guardianResult = await writeOperationBreaker.fire(async () => {
      return await dbPool.query(guardianQuery, [fullName, rut, email, phone, relationship]);
    });

    const newGuardian = guardianResult.rows[0];

    // También crear usuario en la tabla users para autenticación
    const hashedPassword = await bcrypt.hash(password || 'defaultPassword123', 10);

    const userQuery = `
      INSERT INTO users (first_name, last_name, email, password, role, rut, phone, active, email_verified, created_at, updated_at)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, NOW(), NOW())
      RETURNING id
    `;

    const userResult = await writeOperationBreaker.fire(async () => {
      return await dbPool.query(userQuery, [
        firstName,
        lastName,
        email,
        hashedPassword,
        'APODERADO',
        rut,
        phone,
        true,
        false
      ]);
    });

    const userId = userResult.rows[0].id;

    logger.info(`✅ Guardian registered successfully: ID=${newGuardian.id}, UserID=${userId}, Email=${email}`);

    // Agregar al array en memoria para compatibilidad con endpoints existentes
    guardians.push({
      id: newGuardian.id,
      firstName,
      lastName,
      fullName: newGuardian.fullName,
      email: newGuardian.email,
      rut: newGuardian.rut,
      phone: newGuardian.phone,
      address: address || '',
      relationshipType: newGuardian.relationship,
      active: true,
      emailVerified: false,
      createdAt: newGuardian.createdAt,
      emergencyContact: true,
      authorizedToPickup: true,
      applicantIds: []
    });

    res.status(201).json({
      success: true,
      data: {
        id: newGuardian.id,
        userId: userId,
        firstName,
        lastName,
        fullName: newGuardian.fullName,
        email: newGuardian.email,
        rut: newGuardian.rut,
        phone: newGuardian.phone,
        relationshipType: newGuardian.relationship
      },
      token: 'mock-jwt-token-for-new-guardian'
    });
  } catch (error) {
    logger.error('❌ Error registering guardian:', error);
    res.status(500).json({
      success: false,
      error: 'Error al registrar apoderado',
      details: error.message
    });
  }
});

// Actualizar información del apoderado
app.put('/api/guardians/:id', authenticateGuardian, (req, res) => {
  const guardianId = parseInt(req.params.id);
  const guardianIndex = guardians.findIndex(g => g.id === guardianId);
  
  if (guardianIndex === -1) {
    return res.status(404).json({
      success: false,
      error: 'Apoderado no encontrado'
    });
  }
  
  // Actualizar datos
  const updatedData = {
    ...guardians[guardianIndex],
    ...req.body,
    updatedAt: new Date().toISOString()
  };
  
  guardians[guardianIndex] = updatedData;
  
  res.json({
    success: true,
    data: updatedData
  });
});

app.listen(port, () => {
  logger.info(`Guardian Service running on port ${port}`);
  logger.info(`Endpoints disponibles:`);
  logger.info(`  - GET    /health`);
  logger.info(`  - GET    /api/guardians`);
  logger.info(`  - GET    /api/guardians/:id`);
  logger.info(`  - GET    /api/guardians/by-application/:applicationId`);
  logger.info(`  - GET    /api/guardians/stats`);
  logger.info(`  - POST   /api/guardians/auth/login`);
  logger.info(`  - POST   /api/guardians/auth/register`);
  logger.info(`  - PUT    /api/guardians/:id`);
  logger.info(`\nNOTA: Este servicio maneja SOLO apoderados (usuarios externos).`);
  logger.info(`El personal del colegio está en el user-service (puerto 8082).`);
});