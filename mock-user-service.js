const express = require('express');
const cookieParser = require('cookie-parser');
const compression = require('compression');
const { Pool } = require('pg');
const bcrypt = require('bcryptjs');
const CircuitBreaker = require('opossum');
const crypto = require('crypto');
const { translateToSpanish } = require('./translations');
const createLogger = require('./logger');
const logger = createLogger('user-service');
const app = express();
const port = 8082;

// ============= CUSTOM CSRF PROTECTION - DOUBLE-SUBMIT COOKIE PATTERN =============
// Simple, reliable implementation that won't crash on startup
const CSRF_COOKIE_NAME = 'csrf_cookie';
const CSRF_HEADER_NAME = 'x-csrf-token';
const CSRF_TOKEN_LENGTH = 64;

// Generate cryptographically secure random token
function generateCsrfToken() {
  return crypto.randomBytes(CSRF_TOKEN_LENGTH).toString('hex');
}

// CSRF validation middleware
function csrfProtection(req, res, next) {
  const method = req.method.toUpperCase();

  // Skip validation for safe HTTP methods
  if (['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    return next();
  }

  // Get token from cookie and header
  const cookieToken = req.cookies[CSRF_COOKIE_NAME];
  const headerToken = req.headers[CSRF_HEADER_NAME] || req.headers['csrf-token'];

  logger.info(`[CSRF] Validation - Method: ${method}, Cookie: ${cookieToken ? 'present' : 'missing'}, Header: ${headerToken ? 'present' : 'missing'}`);

  // Validate token match
  if (!cookieToken || !headerToken) {
    logger.info('[CSRF] ❌ CSRF token missing');
    return res.status(403).json({
      error: 'CSRF token missing',
      code: 'CSRF_TOKEN_MISSING',
      message: 'CSRF token is required for this request. Call GET /api/auth/csrf-token first.'
    });
  }

  if (cookieToken !== headerToken) {
    logger.info('[CSRF] ❌ CSRF token mismatch');
    return res.status(403).json({
      error: 'CSRF token invalid',
      code: 'CSRF_TOKEN_INVALID',
      message: 'CSRF token validation failed. Token mismatch between cookie and header.'
    });
  }

  logger.info('[CSRF] ✅ CSRF validation passed');
  next();
}

// ============= CREDENTIAL ENCRYPTION - RSA + AES HYBRID =============
// RSA-2048 key pair for encrypting credentials in transit
// Keys rotate every 24 hours for enhanced security
let rsaKeyPair = null;
let keyRotationTime = null;
const KEY_ROTATION_INTERVAL = 24 * 60 * 60 * 1000; // 24 hours

// Generate RSA key pair
function generateRSAKeyPair() {
  logger.info('[Encryption] Generating new RSA-2048 key pair...');
  const { publicKey, privateKey } = crypto.generateKeyPairSync('rsa', {
    modulusLength: 2048,
    publicKeyEncoding: {
      type: 'spki',
      format: 'pem'
    },
    privateKeyEncoding: {
      type: 'pkcs8',
      format: 'pem'
    }
  });

  keyRotationTime = Date.now();
  logger.info('[Encryption] RSA key pair generated successfully');

  return { publicKey, privateKey };
}

// Initialize RSA keys on startup
rsaKeyPair = generateRSAKeyPair();

// Rotate keys every 24 hours
setInterval(() => {
  const oldKeyCount = rsaKeyPair ? 1 : 0;
  rsaKeyPair = generateRSAKeyPair();
  logger.info(`[Encryption] Keys rotated. Previous keys: ${oldKeyCount}`);
}, KEY_ROTATION_INTERVAL);

// Decryption middleware - decrypts RSA + AES encrypted credentials
function decryptCredentials(req, res, next) {
  // Skip decryption if request is not encrypted
  if (!req.body.encryptedData || !req.body.encryptedKey) {
    // Allow plain text for backward compatibility during migration
    logger.info('[Encryption] Plain text credentials detected (backward compatibility mode)');
    return next();
  }

  try {
    const { encryptedData, encryptedKey, iv, authTag } = req.body;

    // Validate all required fields
    if (!encryptedData || !encryptedKey || !iv || !authTag) {
      return res.status(400).json({
        success: false,
        error: 'Invalid encrypted payload',
        code: 'ENCRYPTION_INVALID_PAYLOAD'
      });
    }

    // Step 1: Decrypt AES key with RSA private key
    const aesKey = crypto.privateDecrypt(
      {
        key: rsaKeyPair.privateKey,
        padding: crypto.constants.RSA_PKCS1_OAEP_PADDING,
        oaepHash: 'sha256'
      },
      Buffer.from(encryptedKey, 'base64')
    );

    // Step 2: Decrypt credentials with AES key
    const decipher = crypto.createDecipheriv(
      'aes-256-gcm',
      aesKey,
      Buffer.from(iv, 'base64')
    );

    decipher.setAuthTag(Buffer.from(authTag, 'base64'));

    let decrypted = Buffer.concat([
      decipher.update(Buffer.from(encryptedData, 'base64')),
      decipher.final()
    ]);

    // Parse decrypted credentials
    const credentials = JSON.parse(decrypted.toString('utf8'));

    // Replace request body with decrypted credentials
    req.body = credentials;

    logger.info('[Encryption] Credentials decrypted successfully');
    next();

  } catch (error) {
    logger.error('[Encryption] Decryption failed:', error.message);

    // Rate limiting: Log suspicious decryption attempts
    // In production, implement IP-based rate limiting here

    return res.status(400).json({
      success: false,
      error: 'Credential decryption failed',
      code: 'ENCRYPTION_DECRYPTION_FAILED'
    });
  }
}

// Database configuration with connection pooling
// Uses Railway environment variables with fallback to local dev values
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
// 3 circuit breaker categories for User Service
// (No necesita Heavy ni External - sin analytics ni llamadas externas)

// 1. Simple Queries (2s, 60% threshold, 20s reset) - Fast lookups
const simpleQueryBreakerOptions = {
  timeout: 2000,
  errorThresholdPercentage: 60,
  resetTimeout: 20000,
  rollingCountTimeout: 10000,
  rollingCountBuckets: 10,
  name: 'UserSimpleQueryBreaker'
};

// 2. Medium Queries (5s, 50% threshold, 30s reset) - Standard queries with joins + bcrypt
const mediumQueryBreakerOptions = {
  timeout: 5000,
  errorThresholdPercentage: 50,
  resetTimeout: 30000,
  rollingCountTimeout: 10000,
  rollingCountBuckets: 10,
  name: 'UserMediumQueryBreaker'
};

// 3. Write Operations (3s, 30% threshold, 45s reset) - Critical user data mutations
const writeOperationBreakerOptions = {
  timeout: 3000,
  errorThresholdPercentage: 30,
  resetTimeout: 45000,
  rollingCountTimeout: 10000,
  rollingCountBuckets: 10,
  name: 'UserWriteBreaker'
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

// Event listeners for all breakers
const setupBreakerEvents = (breaker, name) => {
  breaker.on('open', () => {
    logger.error(`⚠️ [Circuit Breaker ${name}] OPEN - Too many failures in user service`);
  });

  breaker.on('halfOpen', () => {
    logger.warn(`🔄 [Circuit Breaker ${name}] HALF-OPEN - Testing recovery`);
  });

  breaker.on('close', () => {
    logger.info(`✅ [Circuit Breaker ${name}] CLOSED - User service recovered`);
  });

  breaker.fallback(() => {
    throw new Error(`Service temporarily unavailable - ${name} circuit breaker open`);
  });
};

// Setup events for all breakers
setupBreakerEvents(simpleQueryBreaker, 'Simple');
setupBreakerEvents(mediumQueryBreaker, 'Medium');
setupBreakerEvents(writeOperationBreaker, 'Write');

// Default breaker for general queries
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
  return {
    success: true,
    data: items,
    total,
    page,
    limit,
    totalPages,
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

// Simple in-memory cache with TTL
class SimpleCache {
  constructor() {
    this.cache = new Map();
    this.stats = { hits: 0, misses: 0 };
  }

  set(key, value, ttlMs = 300000) {
    const expiresAt = Date.now() + ttlMs;
    this.cache.set(key, { value, expiresAt, createdAt: Date.now() });
  }

  get(key) {
    const item = this.cache.get(key);
    if (!item) {
      this.stats.misses++;
      return null;
    }

    if (Date.now() > item.expiresAt) {
      this.cache.delete(key);
      this.stats.misses++;
      return null;
    }

    this.stats.hits++;
    return item.value;
  }

  clear(keyPattern) {
    if (keyPattern) {
      let cleared = 0;
      for (const key of this.cache.keys()) {
        if (key.includes(keyPattern)) {
          this.cache.delete(key);
          cleared++;
        }
      }
      return cleared;
    } else {
      const size = this.cache.size;
      this.cache.clear();
      return size;
    }
  }

  size() {
    return this.cache.size;
  }

  getStats() {
    const total = this.stats.hits + this.stats.misses;
    return {
      ...this.stats,
      total,
      hitRate: total > 0 ? ((this.stats.hits / total) * 100).toFixed(2) + '%' : '0%'
    };
  }
}

// Initialize cache
const userCache = new SimpleCache();

// Periodic cleanup of expired entries (every 5 minutes)
setInterval(() => {
  const now = Date.now();
  let cleaned = 0;
  for (const [key, item] of userCache.cache.entries()) {
    if (now > item.expiresAt) {
      userCache.cache.delete(key);
      cleaned++;
    }
  }
  if (cleaned > 0) {
    logger.info(`[Cache] Cleaned ${cleaned} expired entries`);
  }
}, 300000);

// Add cookie parser and JSON middleware
app.use(cookieParser());
app.use(express.json());

// Response compression middleware
app.use(compression({
  filter: (req, res) => {
    if (req.headers['x-no-compression']) {
      return false;
    }
    return compression.filter(req, res);
  },
  threshold: 1024, // Only compress responses > 1KB
  level: 6
}));

// ============= CSRF PROTECTION MIDDLEWARE =============
// NOTE: Do NOT apply globally - apply selectively to mutation routes
// GET/HEAD/OPTIONS are automatically ignored by csrf-csrf library

// Middleware to simulate JWT verification
const authenticateToken = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  // For mock purposes, we'll accept any token that looks like a JWT
  if (token && token.split('.').length === 3) {
    try {
      // Decode the JWT payload (base64 decode the middle part)
      const payload = JSON.parse(Buffer.from(token.split('.')[1], 'base64').toString());
      req.user = {
        userId: payload.userId,
        email: payload.email,
        role: payload.role
      };
      next();
    } catch (error) {
      // Fallback for hardcoded tokens
      req.user = {
        userId: "1",
        email: "jorge.gangale@mtn.cl", 
        role: "ADMIN"
      };
      next();
    }
  } else {
    res.status(401).json({ error: 'Access token required' });
  }
};

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({
    status: 'UP',
    service: 'user-service',
    port: port,
    timestamp: new Date().toISOString()
  });
});

// ============= CSRF TOKEN GENERATION ENDPOINT =============
// Public endpoint - generates CSRF token and sets csrf_cookie
app.get('/api/auth/csrf-token', (req, res) => {
  try {
    logger.info('[CSRF] Token generation request received');

    // Generate CSRF token
    const csrfToken = generateCsrfToken();

    // Set cookie with token
    res.cookie(CSRF_COOKIE_NAME, csrfToken, {
      httpOnly: false,        // MUST be false so JavaScript can read it for Double-Submit
      sameSite: 'lax',        // Allows cookies on same-site navigation
      path: '/',              // Available on all paths
      secure: false,          // Set to true in production with HTTPS
      maxAge: 3600000         // 1 hour in milliseconds
    });

    logger.info('[CSRF] Token generated successfully:', csrfToken.substring(0, 20) + '...');

    res.json({
      success: true,
      csrfToken: csrfToken,
      message: 'CSRF token generated successfully',
      usage: {
        cookieName: CSRF_COOKIE_NAME,
        headerName: 'X-CSRF-Token',
        value: csrfToken,
        example: 'Include this token in X-CSRF-Token header for POST/PUT/DELETE requests',
        expiresIn: '1 hour'
      }
    });
  } catch (error) {
    logger.error('[CSRF] ❌ Error generating CSRF token:', error);
    res.status(500).json({
      success: false,
      error: 'Error generating CSRF token',
      details: error.message
    });
  }
});

// ============= RSA PUBLIC KEY ENDPOINT =============
// Public endpoint - returns RSA public key for client-side encryption
app.get('/api/auth/public-key', (req, res) => {
  try {
    logger.info('[Encryption] Public key request received');

    if (!rsaKeyPair || !rsaKeyPair.publicKey) {
      return res.status(503).json({
        success: false,
        error: 'Encryption keys not initialized',
        code: 'ENCRYPTION_KEYS_NOT_READY'
      });
    }

    const keyAge = Date.now() - keyRotationTime;
    const timeUntilRotation = KEY_ROTATION_INTERVAL - keyAge;

    res.json({
      success: true,
      publicKey: rsaKeyPair.publicKey,
      keyId: keyRotationTime.toString(), // Key identifier for rotation tracking
      algorithm: 'RSA-OAEP',
      keySize: 2048,
      hash: 'SHA-256',
      usage: 'Encrypt credentials with this key. Use RSA-OAEP with SHA-256 hash.',
      expiresIn: Math.floor(timeUntilRotation / 1000) + ' seconds',
      message: 'Use this public key to encrypt login credentials on the client side'
    });

    logger.info('[Encryption] Public key sent successfully');
  } catch (error) {
    logger.error('[Encryption] ❌ Error sending public key:', error);
    res.status(500).json({
      success: false,
      error: 'Error retrieving public key',
      details: error.message
    });
  }
});

// Global users array - SOLO PERSONAL DEL COLEGIO
// Los APODERADOS están en un servicio separado (mock-guardian-service.js)
// porque son usuarios externos (padres/tutores) y no personal del colegio
const users = [
    // ADMIN
    { 
      id: 1, 
      firstName: 'Jorge', 
      lastName: 'Gangale', 
      fullName: 'Jorge Gangale',
      email: 'jorge.gangale@mtn.cl', 
      rut: '12345678-9',
      phone: '+56912345678',
      role: 'ADMIN',
      subject: null,
      educationalLevel: null,
      active: true,
      emailVerified: true,
      createdAt: '2023-01-15T10:30:00Z'
    },
    
    // DIRECTORES DE CICLO (2)
    { 
      id: 2, 
      firstName: 'Ana María', 
      lastName: 'Fernández Contreras', 
      fullName: 'Ana María Fernández Contreras',
      email: 'ana.fernandez@mtn.cl', 
      rut: '11223344-5',
      phone: '+56911223344',
      role: 'CYCLE_DIRECTOR',
      subject: 'ALL_SUBJECTS',
      educationalLevel: 'ELEMENTARY',
      active: true,
      emailVerified: true,
      createdAt: '2023-02-01T09:15:00Z'
    },
    { 
      id: 3, 
      firstName: 'Roberto Carlos', 
      lastName: 'Silva Mendoza', 
      fullName: 'Roberto Carlos Silva Mendoza',
      email: 'roberto.silva@mtn.cl', 
      rut: '22334455-6',
      phone: '+56922334455',
      role: 'CYCLE_DIRECTOR',
      subject: 'ALL_SUBJECTS',
      educationalLevel: 'HIGH_SCHOOL',
      active: true,
      emailVerified: true,
      createdAt: '2023-02-05T11:20:00Z'
    },

    // PSICÓLOGOS (3)
    { 
      id: 4, 
      firstName: 'María José', 
      lastName: 'González Pérez', 
      fullName: 'María José González Pérez',
      email: 'mariajose.gonzalez@mtn.cl', 
      rut: '33445566-7',
      phone: '+56933445566',
      role: 'PSYCHOLOGIST',
      subject: null,
      educationalLevel: null,
      active: true,
      emailVerified: true,
      createdAt: '2023-02-10T14:30:00Z'
    },
    { 
      id: 5, 
      firstName: 'Patricia Elena', 
      lastName: 'López Herrera', 
      fullName: 'Patricia Elena López Herrera',
      email: 'patricia.lopez@mtn.cl', 
      rut: '44556677-8',
      phone: '+56944556677',
      role: 'PSYCHOLOGIST',
      subject: null,
      educationalLevel: null,
      active: true,
      emailVerified: true,
      createdAt: '2023-02-15T16:45:00Z'
    },
    { 
      id: 6, 
      firstName: 'Fernando José', 
      lastName: 'Morales Díaz', 
      fullName: 'Fernando José Morales Díaz',
      email: 'fernando.morales@mtn.cl', 
      rut: '55667788-9',
      phone: '+56955667788',
      role: 'PSYCHOLOGIST',
      subject: null,
      educationalLevel: null,
      active: true,
      emailVerified: true,
      createdAt: '2023-02-20T10:15:00Z'
    },

    // PROFESORES DE MATEMÁTICAS (2)
    { 
      id: 7, 
      firstName: 'Carlos Alberto', 
      lastName: 'Mendoza Fuentes', 
      fullName: 'Carlos Alberto Mendoza Fuentes',
      email: 'carlos.mendoza@mtn.cl', 
      rut: '66778899-K',
      phone: '+56966778899',
      role: 'TEACHER',
      subject: 'MATHEMATICS',
      educationalLevel: 'HIGH_SCHOOL',
      active: true,
      emailVerified: true,
      createdAt: '2023-03-01T08:30:00Z'
    },
    { 
      id: 8, 
      firstName: 'Andrea Paulina', 
      lastName: 'Ruiz Valdés', 
      fullName: 'Andrea Paulina Ruiz Valdés',
      email: 'andrea.ruiz@mtn.cl', 
      rut: '77889900-1',
      phone: '+56977889900',
      role: 'TEACHER',
      subject: 'MATHEMATICS',
      educationalLevel: 'ELEMENTARY',
      active: true,
      emailVerified: true,
      createdAt: '2023-03-05T12:15:00Z'
    },

    // PROFESORES DE LENGUAJE (2)
    { 
      id: 9, 
      firstName: 'Elena Beatriz', 
      lastName: 'Vargas Soto', 
      fullName: 'Elena Beatriz Vargas Soto',
      email: 'elena.vargas@mtn.cl', 
      rut: '88990011-2',
      phone: '+56988990011',
      role: 'TEACHER',
      subject: 'LANGUAGE',
      educationalLevel: 'HIGH_SCHOOL',
      active: true,
      emailVerified: true,
      createdAt: '2023-03-10T09:45:00Z'
    },
    { 
      id: 10, 
      firstName: 'Marcela Andrea', 
      lastName: 'Castro Rojas', 
      fullName: 'Marcela Andrea Castro Rojas',
      email: 'marcela.castro@mtn.cl', 
      rut: '99001122-3',
      phone: '+56999001122',
      role: 'TEACHER',
      subject: 'LANGUAGE',
      educationalLevel: 'ELEMENTARY',
      active: true,
      emailVerified: true,
      createdAt: '2023-03-12T15:20:00Z'
    },

    // PROFESORES DE CIENCIAS (2)
    { 
      id: 11, 
      firstName: 'Diego Ignacio', 
      lastName: 'Herrera Tapia', 
      fullName: 'Diego Ignacio Herrera Tapia',
      email: 'diego.herrera@mtn.cl', 
      rut: '10111213-4',
      phone: '+56910111213',
      role: 'TEACHER',
      subject: 'SCIENCE',
      educationalLevel: 'HIGH_SCHOOL',
      active: true,
      emailVerified: true,
      createdAt: '2023-03-15T11:30:00Z'
    },
    { 
      id: 12, 
      firstName: 'Valentina Paz', 
      lastName: 'Torres Miranda', 
      fullName: 'Valentina Paz Torres Miranda',
      email: 'valentina.torres@mtn.cl', 
      rut: '11121314-5',
      phone: '+56911121314',
      role: 'TEACHER',
      subject: 'SCIENCE',
      educationalLevel: 'ELEMENTARY',
      active: true,
      emailVerified: true,
      createdAt: '2023-03-18T14:00:00Z'
    },

    // PROFESORES DE HISTORIA (2)
    { 
      id: 13, 
      firstName: 'Gonzalo Andrés', 
      lastName: 'Pinto Sepúlveda', 
      fullName: 'Gonzalo Andrés Pinto Sepúlveda',
      email: 'gonzalo.pinto@mtn.cl', 
      rut: '12131415-6',
      phone: '+56912131415',
      role: 'TEACHER',
      subject: 'HISTORY',
      educationalLevel: 'HIGH_SCHOOL',
      active: true,
      emailVerified: true,
      createdAt: '2023-03-20T10:45:00Z'
    },
    { 
      id: 14, 
      firstName: 'Sofía Antonia', 
      lastName: 'Ramírez Molina', 
      fullName: 'Sofía Antonia Ramírez Molina',
      email: 'sofia.ramirez@mtn.cl', 
      rut: '13141516-7',
      phone: '+56913141516',
      role: 'TEACHER',
      subject: 'HISTORY',
      educationalLevel: 'ELEMENTARY',
      active: true,
      emailVerified: true,
      createdAt: '2023-03-22T13:15:00Z'
    },

    // PROFESORES DE EDUCACIÓN FÍSICA (2)
    { 
      id: 15, 
      firstName: 'Matías Eduardo', 
      lastName: 'Espinoza Aguirre', 
      fullName: 'Matías Eduardo Espinoza Aguirre',
      email: 'matias.espinoza@mtn.cl', 
      rut: '14151617-8',
      phone: '+56914151617',
      role: 'TEACHER',
      subject: 'PHYSICAL_EDUCATION',
      educationalLevel: 'HIGH_SCHOOL',
      active: true,
      emailVerified: true,
      createdAt: '2023-03-25T08:00:00Z'
    },
    { 
      id: 16, 
      firstName: 'Carla Victoria', 
      lastName: 'Muñoz Navarro', 
      fullName: 'Carla Victoria Muñoz Navarro',
      email: 'carla.munoz@mtn.cl', 
      rut: '15161718-9',
      phone: '+56915161718',
      role: 'TEACHER',
      subject: 'PHYSICAL_EDUCATION',
      educationalLevel: 'ELEMENTARY',
      active: true,
      emailVerified: true,
      createdAt: '2023-03-28T16:30:00Z'
    },

    // COORDINADORES (para completar el staff)
    { 
      id: 17, 
      firstName: 'Jaime Antonio', 
      lastName: 'Gutiérrez Soto', 
      fullName: 'Jaime Antonio Gutiérrez Soto',
      email: 'jaime.coordinator@mtn.cl', 
      rut: '16171819-K',
      phone: '+56916171819',
      role: 'COORDINATOR',
      subject: 'ALL_SUBJECTS',
      educationalLevel: 'ALL_LEVELS',
      active: true,
      emailVerified: true,
      createdAt: '2023-04-01T09:00:00Z'
    },

    // PROFESORA Alejandra Flores
    {
      id: 47,
      firstName: 'Alejandra',
      lastName: 'Flores',
      fullName: 'Alejandra Flores',
      email: 'alejandra.flores@mtn.cl',
      rut: '18.765.432-1',
      phone: '+56918765432',
      role: 'TEACHER',
      subject: 'MATHEMATICS',
      educationalLevel: 'HIGH_SCHOOL',
      active: true,
      emailVerified: true,
      createdAt: '2023-04-05T10:00:00Z'
    },

    // APODERADO Carlos Eduardo
    {
      id: 19,
      firstName: 'Carlos Eduardo',
      lastName: 'Fernández Vargas',
      fullName: 'Carlos Eduardo Fernández Vargas',
      email: 'familia.fernandez@test.cl',
      rut: '12.345.678-9',
      phone: '+56912345678',
      role: 'APODERADO',
      subject: null,
      educationalLevel: null,
      active: true,
      emailVerified: true,
      createdAt: '2023-08-01T10:00:00Z'
    }
];

// Mock user endpoints (protected)
app.get('/api/users', authenticateToken, async (req, res) => {
  try {
    // Filter by role if requested
    const roleFilter = req.query.role;

    let query = `
      SELECT
        id,
        first_name as "firstName",
        last_name as "lastName",
        email,
        role,
        subject,
        rut,
        phone,
        active,
        email_verified as "emailVerified",
        created_at as "createdAt",
        last_login as "lastLogin"
      FROM users
    `;

    const params = [];
    if (roleFilter) {
      query += ' WHERE role = $1';
      params.push(roleFilter);
    }

    query += ' ORDER BY role, first_name';

    const result = await dbPool.query(query, params);

    // Transform to match frontend format and translate roles
    const users = result.rows.map(user => ({
      id: user.id,
      fullName: `${user.firstName} ${user.lastName}`,
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      role: translateToSpanish(user.role, 'user_role'),
      subject: user.subject,
      rut: user.rut,
      phone: user.phone,
      active: user.active,
      emailVerified: user.emailVerified,
      createdAt: user.createdAt,
      lastLogin: user.lastLogin
    }));

    res.json({
      success: true,
      data: users,
      users: users,  // Add users field for frontend compatibility
      count: users.length
    });
  } catch (error) {
    logger.error('❌ Error fetching users:', error);
    res.status(500).json({
      success: false,
      error: 'Error al obtener usuarios',
      details: error.message
    });
  }
});

// Public login endpoint - consulta la base de datos PostgreSQL
// ============= LOGIN ENDPOINT - CSRF PROTECTED =============
// Optional CSRF protection for login (allows test mode)
const optionalCsrfProtection = (req, res, next) => {
  if (req.headers["x-test-mode"] === "true") {
    return next();
  }
  return csrfProtection(req, res, next);
};
app.post('/api/auth/login', decryptCredentials, optionalCsrfProtection, async (req, res) => {
  const { email, password } = req.body;

  // Logging removed for security - logger.info('🔐 LOGIN ATTEMPT:', { email, password: password ? '[PROTECTED]' : '[EMPTY]' });

  if (!email || !password) {
    // Logging removed for security - logger.info('❌ Missing email or password');
    return res.status(400).json({
      success: false,
      error: 'Email y contraseña son obligatorios'
    });
  }
  
  const client = await dbPool.connect();
  try {
    // Consultar base de datos para el usuario
    const userQuery = await client.query(
      'SELECT id, first_name, last_name, email, role, subject, password, active, email_verified FROM users WHERE email = $1',
      [email.toLowerCase().trim()]
    );

    if (userQuery.rows.length === 0) {
      // Logging removed for security - logger.info('❌ User not found:', email);
      return res.status(401).json({
        success: false,
        error: 'Credenciales inválidas'
      });
    }

    const user = userQuery.rows[0];
    // Logging removed for security - logger.info('👤 User found:', { id: user.id, email: user.email, role: user.role, active: user.active });

    // Verificar si el usuario está activo
    if (!user.active) {
      // Logging removed for security - logger.info('❌ User is inactive:', email);
      return res.status(401).json({
        success: false,
        error: 'Usuario inactivo'
      });
    }

    // Verificar contraseña usando bcrypt
    const isValidPassword = await bcrypt.compare(password, user.password);
    // Logging removed for security - logger.info('🔒 Password verification:', isValidPassword ? 'SUCCESS' : 'FAILED');

    if (!isValidPassword) {
      // Logging removed for security - logger.info('❌ Invalid password for user:', email);
      return res.status(401).json({
        success: false,
        error: 'Credenciales inválidas'
      });
    }

    // Actualizar last_login
    await client.query(
      'UPDATE users SET last_login = NOW() WHERE id = $1',
      [user.id]
    );

    // Para usuarios APODERADO, buscar su applicationId
    let applicationId = null;
    if (user.role === 'APODERADO') {
      const applicationQuery = await client.query(
        'SELECT id FROM applications WHERE applicant_user_id = $1 ORDER BY created_at DESC LIMIT 1',
        [user.id]
      );
      if (applicationQuery.rows.length > 0) {
        applicationId = applicationQuery.rows[0].id;
      }
    }

    // Create a proper JWT token with user data
    const header = Buffer.from(JSON.stringify({alg: "HS256", typ: "JWT"})).toString('base64');
    const payload = Buffer.from(JSON.stringify({
      userId: user.id.toString(),
      email: user.email,
      role: user.role,
      iat: Math.floor(Date.now() / 1000),
      exp: Math.floor(Date.now() / 1000) + (24 * 60 * 60) // 24 hours
    })).toString('base64');
    const signature = "mock-signature"; // In real app would be HMAC
    const token = `${header}.${payload}.${signature}`;

    // Logging removed for security - logger.info('✅ Login successful for user:', { id: user.id, email: user.email, role: user.role });

    const responseData = {
      success: true,
      message: 'Login exitoso',
      token: token,
      id: user.id.toString(),
      firstName: user.first_name,
      lastName: user.last_name,
      email: user.email,
      role: user.role, // Fixed: Return role as-is for frontend validation
      subject: user.subject
    };

    // Add applicationId if it exists (for APODERADO users)
    if (applicationId !== null) {
      responseData.applicationId = applicationId;
    }

    res.json(responseData);

  } catch (error) {
    // Error logging removed for security('💥 Database error during login:', error);
    res.status(500).json({
      success: false,
      error: 'Error interno del servidor'
    });
  } finally {
    client.release();
  }
});

// Public register endpoint for new APODERADO users - CSRF PROTECTED
app.post('/api/auth/register', decryptCredentials, csrfProtection, async (req, res) => {
  const { firstName, lastName, email, password, rut, phone } = req.body;

  // Validate required fields
  if (!firstName || !lastName || !email || !password) {
    return res.status(400).json({
      success: false,
      error: 'Todos los campos obligatorios deben ser completados'
    });
  }
  
  const client = await dbPool.connect();
  try {
    // Check if email already exists in database
    const existingUserQuery = await client.query(
      'SELECT id FROM users WHERE email = $1',
      [email.toLowerCase().trim()]
    );

    if (existingUserQuery.rows.length > 0) {
      // Special case: if it's a test email from our testing, allow re-registration
      if (email === 'test@example.com' || email === 'jorge.gangale@gmail.com') {
        // Delete the old user from database
        await client.query('DELETE FROM users WHERE email = $1', [email.toLowerCase().trim()]);
        // Logging removed for security - logger.info(`🗑️ Removed existing test user: ${email}`);
      } else {
        return res.status(409).json({
          success: false,
          error: 'Este email ya está registrado en el sistema'
        });
      }
    }

    // Hash password
    const hashedPassword = await bcrypt.hash(password, 10);

    // Insert new APODERADO user into database - let database auto-generate the ID
    const insertQuery = `
      INSERT INTO users (
        first_name, last_name, email, password, rut, phone, role,
        active, email_verified, created_at, updated_at
      ) VALUES (
        $1, $2, $3, $4, $5, $6, $7, $8, $9, NOW(), NOW()
      ) RETURNING id, first_name, last_name, email, role, active, email_verified, created_at
    `;

    const insertResult = await client.query(insertQuery, [
      firstName.trim(),
      lastName.trim(),
      email.toLowerCase().trim(),
      hashedPassword,
      rut ? rut.trim() : null,
      phone ? phone.trim() : null,
      'APODERADO',
      true, // active
      true, // email_verified (since email verification was done before registration)
    ]);

    const newUser = insertResult.rows[0];
    // Logging removed for security - logger.info('✅ New user saved to database:', newUser.email);
    
    // Also add to in-memory users array for immediate compatibility with existing endpoints
    const memoryUser = {
      id: newUser.id,
      firstName: newUser.first_name,
      lastName: newUser.last_name,
      fullName: `${newUser.first_name} ${newUser.last_name}`,
      email: newUser.email,
      rut: rut ? rut.trim() : null,
      phone: phone ? phone.trim() : null,
      role: newUser.role,
      subject: null,
      educationalLevel: null,
      active: newUser.active,
      emailVerified: newUser.email_verified,
      createdAt: newUser.created_at.toISOString(),
      updatedAt: newUser.created_at.toISOString()
    };
    users.push(memoryUser);
    
    // Create JWT token for immediate login
    const header = Buffer.from(JSON.stringify({alg: "HS256", typ: "JWT"})).toString('base64');
    const payload = Buffer.from(JSON.stringify({
      userId: newUser.id.toString(),
      email: newUser.email,
      role: newUser.role,
      firstName: newUser.first_name,
      lastName: newUser.last_name,
      iat: Math.floor(Date.now() / 1000),
      exp: Math.floor(Date.now() / 1000) + (24 * 60 * 60) // 24 hours
    })).toString('base64');
    const signature = "mock-signature";
    const token = `${header}.${payload}.${signature}`;
    
    // Return success response with user data and token (at root level for frontend compatibility)
    res.status(201).json({
      success: true,
      message: 'Usuario registrado exitosamente',
      token: token,
      // User data at root level for AuthContext compatibility
      id: newUser.id.toString(),
      firstName: newUser.first_name,
      lastName: newUser.last_name,
      fullName: `${newUser.first_name} ${newUser.last_name}`,
      email: newUser.email,
      role: translateToSpanish(newUser.role, 'user_role'),
      rut: rut ? rut.trim() : null,
      phone: phone ? phone.trim() : null,
      emailVerified: newUser.email_verified,
      createdAt: newUser.created_at.toISOString(),
      // Also include user object for backward compatibility
      user: {
        id: newUser.id.toString(),
        firstName: newUser.first_name,
        lastName: newUser.last_name,
        fullName: `${newUser.first_name} ${newUser.last_name}`,
        email: newUser.email,
        role: translateToSpanish(newUser.role, 'user_role'),
        rut: rut ? rut.trim() : null,
        phone: phone ? phone.trim() : null,
        emailVerified: newUser.email_verified,
        createdAt: newUser.created_at.toISOString()
      }
    });
    
  } catch (error) {
    // Error logging removed for security('❌ Error registering user:', error);
    res.status(500).json({
      success: false,
      error: 'Error interno del servidor al registrar usuario'
    });
  } finally {
    client.release();
  }
});

app.get('/api/auth/check', authenticateToken, (req, res) => {
  res.json({
    success: true,
    authenticated: true,
    user: req.user
  });
});

// Mock user stats endpoint
app.get('/api/users/stats', authenticateToken, (req, res) => {
  res.json({
    success: true,
    data: {
      total: 22,
      byRole: {
        ADMIN: 1,
        TEACHER: 10,
        PSYCHOLOGIST: 3,
        CYCLE_DIRECTOR: 2,
        COORDINATOR: 1,
        APODERADO: 5
      },
      active: 20,
      inactive: 2,
      emailVerified: 16,
      emailNotVerified: 6,
      recentlyAdded: 5,
      growthRate: 15.2
    }
  });
});

// Update user by ID
// Update user - CSRF PROTECTED
app.put('/api/users/:id', csrfProtection, authenticateToken, async (req, res) => {
  const userId = parseInt(req.params.id);
  const client = await dbPool.connect();

  try {
    logger.info('📝 Datos recibidos para actualizar usuario:', JSON.stringify(req.body, null, 2));

    // Check if user exists in DATABASE first
    const currentUserQuery = await client.query(
      'SELECT * FROM users WHERE id = $1',
      [userId]
    );

    if (currentUserQuery.rows.length === 0) {
      return res.status(404).json({
        success: false,
        error: 'User not found'
      });
    }

    let hashedPassword = currentUserQuery.rows[0].password; // Keep current password by default

    // Only hash and update password if a new one is provided
    if (req.body.password && req.body.password.trim() !== '') {
      hashedPassword = await bcrypt.hash(req.body.password, 10);
    }

    // Build updated user object from database data + request body
    const currentUser = currentUserQuery.rows[0];
    const updatedUser = {
      id: userId,
      firstName: req.body.firstName || currentUser.first_name,
      lastName: req.body.lastName || currentUser.last_name,
      fullName: req.body.firstName && req.body.lastName ?
                `${req.body.firstName} ${req.body.lastName}`.trim() :
                `${currentUser.first_name} ${currentUser.last_name}`.trim(),
      email: req.body.email || currentUser.email,
      password: hashedPassword,
      role: req.body.role || currentUser.role,
      active: req.body.active !== undefined ? req.body.active : currentUser.active,
      emailVerified: req.body.emailVerified !== undefined ? req.body.emailVerified : currentUser.email_verified,
      rut: req.body.rut || currentUser.rut,
      phone: req.body.phone || currentUser.phone,
      subject: req.body.subject || currentUser.subject,
      educationalLevel: req.body.educationalLevel || currentUser.educational_level,
      updatedAt: new Date().toISOString()
    };

    // ALSO update in database for persistent storage
    const updateUserQuery = `
      UPDATE users SET
        first_name = $2,
        last_name = $3,
        email = $4,
        password = $5,
        role = $6,
        active = $7,
        email_verified = $8,
        rut = $9,
        phone = $10,
        subject = $11,
        educational_level = $12,
        updated_at = NOW()
      WHERE id = $1
    `;

    await client.query(updateUserQuery, [
      updatedUser.id,
      updatedUser.firstName,
      updatedUser.lastName,
      updatedUser.email,
      hashedPassword,
      updatedUser.role,
      updatedUser.active,
      updatedUser.emailVerified,
      updatedUser.rut,
      updatedUser.phone,
      updatedUser.subject || null,
      updatedUser.educationalLevel || null
    ]);

    logger.info(`✅ Usuario actualizado en base de datos: ${updatedUser.firstName} ${updatedUser.lastName}`);

    // Update in-memory array for consistency (if user exists there)
    const userIndex = users.findIndex(u => u.id === userId);
    if (userIndex !== -1) {
      users[userIndex] = updatedUser;
    }

    // ============= CACHE INVALIDATION =============
    // Invalidate all user-related caches after successful update
    const cleared = userCache.clear('users:');
    logger.info(`[Cache Invalidation] Cleared ${cleared} user cache entries after user update (ID: ${userId})`);

    res.json({
      success: true,
      message: 'User updated successfully',
      data: updatedUser
    });

  } catch (error) {
    logger.error('❌ Error actualizando usuario:', error);
    res.status(500).json({
      success: false,
      error: 'Error updating user in database'
    });
  } finally {
    client.release();
  }
});

// Delete user by ID
// Delete user - CSRF PROTECTED
app.delete('/api/users/:id', csrfProtection, authenticateToken, async (req, res) => {
  const userId = parseInt(req.params.id);
  const client = await dbPool.connect();

  try {
    // Check if user exists
    const checkQuery = 'SELECT id, email, role FROM users WHERE id = $1';
    const checkResult = await client.query(checkQuery, [userId]);

    if (checkResult.rows.length === 0) {
      return res.status(404).json({
        success: false,
        error: 'Usuario no encontrado'
      });
    }

    // Attempt to delete from database
    await client.query('BEGIN');

    try {
      const deleteQuery = 'DELETE FROM users WHERE id = $1 RETURNING id';
      const result = await client.query(deleteQuery, [userId]);

      await client.query('COMMIT');

      // Also remove from in-memory array for consistency
      const userIndex = users.findIndex(u => u.id === userId);
      if (userIndex !== -1) {
        users.splice(userIndex, 1);
      }

      // ============= CACHE INVALIDATION =============
      // Invalidate all user-related caches after successful deletion
      const cleared = userCache.clear('users:');
      logger.info(`[Cache Invalidation] Cleared ${cleared} user cache entries after user deletion (ID: ${userId})`);

      res.json({
        success: true,
        message: 'Usuario eliminado correctamente'
      });
    } catch (dbError) {
      await client.query('ROLLBACK');

      // Check if it's a foreign key constraint error
      if (dbError.code === '23503') {
        return res.status(409).json({
          success: false,
          error: 'No se puede eliminar este usuario porque tiene datos asociados en el sistema (evaluaciones, entrevistas, etc.). Por favor, desactiva el usuario en lugar de eliminarlo.'
        });
      }

      throw dbError;
    }
  } catch (error) {
    logger.error('Error deleting user:', error);
    res.status(500).json({
      success: false,
      error: 'Error al eliminar el usuario',
      details: error.message
    });
  } finally {
    client.release();
  }
});

// Create new user - CSRF PROTECTED
app.post('/api/users', csrfProtection, authenticateToken, async (req, res) => {
  const client = await dbPool.connect();

  try {
    // Get the highest ID from the DATABASE, not in-memory array
    const maxIdQuery = await client.query('SELECT COALESCE(MAX(id), 0) as max_id FROM users');
    const newId = maxIdQuery.rows[0].max_id + 1;

    // Hash password if provided
    let hashedPassword = null;
    if (req.body.password) {
      hashedPassword = await bcrypt.hash(req.body.password, 10);
    }

    // Create new user with required fields
    const newUser = {
      id: newId,
      firstName: req.body.firstName || '',
      lastName: req.body.lastName || '',
      fullName: `${req.body.firstName || ''} ${req.body.lastName || ''}`.trim(),
      email: req.body.email || '',
      password: hashedPassword,
      role: req.body.role || 'APODERADO',
      active: req.body.active !== undefined ? req.body.active : true,
      emailVerified: req.body.emailVerified !== undefined ? req.body.emailVerified : false,
      rut: req.body.rut || null,
      phone: req.body.phone || null,
      subject: req.body.subject || null,
      educationalLevel: req.body.educationalLevel || null,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };

    // Save to database FIRST (primary source of truth)
    let userSavedToDB = false;
    const insertUserQuery = `
      INSERT INTO users (id, first_name, last_name, email, password, role, active, email_verified, rut, phone, subject, created_at, updated_at)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, NOW(), NOW())
      RETURNING id
    `;

    const result = await client.query(insertUserQuery, [
      newUser.id,
      newUser.firstName,
      newUser.lastName,
      newUser.email,
      newUser.password,
      newUser.role,
      newUser.active,
      newUser.emailVerified,
      newUser.rut,
      newUser.phone,
      newUser.subject
    ]);

    logger.info(`✅ Usuario ${result.rows[0].id} guardado en base de datos`);
    userSavedToDB = true;

    // ONLY add to in-memory array if DB save succeeded
    users.push(newUser);

    // ============= CACHE INVALIDATION =============
    // Invalidate all user-related caches after successful creation
    const cleared = userCache.clear('users:');
    logger.info(`[Cache Invalidation] Cleared ${cleared} user cache entries after user creation (ID: ${newUser.id})`);

    // Create schedules for evaluator roles ONLY if user was successfully saved to database
    const evaluatorRoles = ['TEACHER_LANGUAGE', 'TEACHER_MATHEMATICS', 'TEACHER_ENGLISH', 'TEACHER', 'COORDINATOR', 'CYCLE_DIRECTOR', 'PSYCHOLOGIST', 'ADMIN'];
    if (userSavedToDB && evaluatorRoles.includes(newUser.role)) {
      try {
        if (req.body.customSchedules && Array.isArray(req.body.customSchedules) && req.body.customSchedules.length > 0) {
          logger.info(`🗓️ Creando horarios personalizados para ${newUser.role}: ${newUser.fullName}`);
          await createCustomSchedulesForUser(newUser.id, req.body.customSchedules);
        } else {
          logger.info(`🗓️ Creando horarios por defecto para ${newUser.role}: ${newUser.fullName}`);
          await createDefaultScheduleForUser(newUser.id);
        }
      } catch (scheduleError) {
        logger.error('❌ Error creando horarios para usuario:', scheduleError);
        // Don't fail user creation if schedules fail
      }
    } else if (!userSavedToDB && evaluatorRoles.includes(newUser.role)) {
      logger.info(`⚠️ No se crearon horarios para ${newUser.role}: ${newUser.fullName} - usuario no guardado en base de datos`);
    }

    res.status(201).json({
      success: true,
      message: `User created successfully${evaluatorRoles.includes(newUser.role) ? ' with default schedule' : ''}`,
      data: newUser
    });

  } catch (error) {
    logger.error('❌ Error creating user:', error);

    // Handle duplicate key errors with user-friendly messages
    if (error.code === '23505') { // PostgreSQL unique violation
      let errorMessage = 'Ya existe un usuario con esos datos';

      if (error.message.includes('users_email_key') || error.message.includes('email')) {
        errorMessage = 'Ya existe un usuario con ese correo electrónico';
      } else if (error.message.includes('users_rut_key') || error.message.includes('rut')) {
        errorMessage = 'Ya existe un usuario con ese RUT';
      }

      return res.status(409).json({
        success: false,
        error: errorMessage
      });
    }

    res.status(500).json({
      success: false,
      error: 'Error interno del servidor al crear usuario',
      details: error.message
    });
  } finally {
    client.release();
  }
});

// Function to create default schedule for new evaluator users
async function createDefaultScheduleForUser(userId) {
  const client = await dbPool.connect();
  try {
    const currentYear = new Date().getFullYear();
    const defaultSchedules = [
      // Lunes a viernes, 10:00-12:00 y 14:00-16:00
      { day: 'MONDAY', start: '10:00:00', end: '12:00:00' },
      { day: 'MONDAY', start: '14:00:00', end: '16:00:00' },
      { day: 'TUESDAY', start: '10:00:00', end: '12:00:00' },
      { day: 'TUESDAY', start: '14:00:00', end: '16:00:00' },
      { day: 'WEDNESDAY', start: '10:00:00', end: '12:00:00' },
      { day: 'WEDNESDAY', start: '14:00:00', end: '16:00:00' },
      { day: 'THURSDAY', start: '10:00:00', end: '12:00:00' },
      { day: 'THURSDAY', start: '14:00:00', end: '16:00:00' },
      { day: 'FRIDAY', start: '10:00:00', end: '12:00:00' },
      { day: 'FRIDAY', start: '14:00:00', end: '16:00:00' }
    ];

    for (const schedule of defaultSchedules) {
      const insertQuery = `
        INSERT INTO interviewer_schedules
        (interviewer_id, day_of_week, start_time, end_time, schedule_type, year, is_active, created_at, notes)
        VALUES ($1, $2, $3, $4, 'RECURRING', $5, true, NOW(), 'Horario por defecto creado automáticamente')
      `;

      await client.query(insertQuery, [
        userId,
        schedule.day,
        schedule.start,
        schedule.end,
        currentYear
      ]);
    }

    logger.info(`✅ Horarios por defecto creados para usuario ${userId}`);

  } catch (error) {
    logger.error('❌ Error creando horarios por defecto:', error);
    // No lanzamos el error para no fallar la creación del usuario
  } finally {
    client.release();
  }
}

// Create custom schedules for a user
async function createCustomSchedulesForUser(userId, customSchedules) {
  const client = await dbPool.connect();
  try {
    const currentYear = new Date().getFullYear();

    logger.info(`🗓️ Creando ${customSchedules.length} horarios personalizados para usuario ${userId}`);

    for (const schedule of customSchedules) {
      // Validate required fields
      if (!schedule.dayOfWeek || !schedule.startTime || !schedule.endTime) {
        logger.warn('⚠️ Horario inválido (faltan campos requeridos):', schedule);
        continue;
      }

      // Convert frontend time format (HH:MM) to database format (HH:MM:SS)
      const startTime = schedule.startTime.includes(':') ? `${schedule.startTime}:00` : '10:00:00';
      const endTime = schedule.endTime.includes(':') ? `${schedule.endTime}:00` : '12:00:00';

      const insertQuery = `
        INSERT INTO interviewer_schedules
        (interviewer_id, day_of_week, start_time, end_time, schedule_type, year, is_active, created_at, notes)
        VALUES ($1, $2, $3, $4, 'RECURRING', $5, true, NOW(), $6)
      `;

      const notes = schedule.notes || 'Horario personalizado creado durante registro de usuario';

      await client.query(insertQuery, [
        userId,
        schedule.dayOfWeek,
        startTime,
        endTime,
        currentYear,
        notes
      ]);

      logger.info(`✅ Horario creado: ${schedule.dayOfWeek} ${startTime}-${endTime}`);
    }

    logger.info(`✅ Todos los horarios personalizados creados para usuario ${userId}`);

  } catch (error) {
    logger.error('❌ Error creando horarios personalizados:', error);
    // No lanzamos el error para no fallar la creación del usuario
  } finally {
    client.release();
  }
}

// Get current user profile
app.get('/api/users/me', authenticateToken, async (req, res) => {
  const client = await dbPool.connect();
  try {
    // Query database for user based on token data
    const userQuery = await client.query(
      'SELECT id, first_name, last_name, email, role, phone, rut, subject, active, email_verified, created_at FROM users WHERE id = $1',
      [parseInt(req.user.userId)]
    );

    if (userQuery.rows.length === 0) {
      return res.status(404).json({
        success: false,
        error: 'Usuario no encontrado'
      });
    }

    const user = userQuery.rows[0];

    // Para usuarios APODERADO, buscar su applicationId
    let applicationId = null;
    if (user.role === 'APODERADO') {
      const applicationQuery = await client.query(
        'SELECT id FROM applications WHERE applicant_user_id = $1 ORDER BY created_at DESC LIMIT 1',
        [user.id]
      );
      if (applicationQuery.rows.length > 0) {
        applicationId = applicationQuery.rows[0].id;
      }
    }

    const userProfile = {
      id: user.id,
      firstName: user.first_name,
      lastName: user.last_name,
      fullName: `${user.first_name} ${user.last_name}`,
      email: user.email,
      role: translateToSpanish(user.role, 'user_role'),
      phone: user.phone,
      rut: user.rut,
      subject: user.subject,
      educationalLevel: null,
      active: user.active,
      emailVerified: user.email_verified,
      createdAt: user.created_at
    };

    // Add applicationId if it exists (for APODERADO users)
    if (applicationId !== null) {
      userProfile.applicationId = applicationId;
    }

    // Return user profile without sensitive data
    res.json({
      success: true,
      user: userProfile
    });
  } catch (error) {
    logger.error('Error fetching user profile:', error);
    res.status(500).json({
      success: false,
      error: 'Error al obtener perfil de usuario'
    });
  } finally {
    client.release();
  }
});

// ============= MISSING ENDPOINTS REQUIRED BY FRONTEND =============

// Get staff users for administration (excludes APODERADO)
// IMPORTANT: This must be registered BEFORE /api/users/:id to prevent route conflicts
app.get('/api/users/staff', authenticateToken, async (req, res) => {
  const client = await dbPool.connect();
  try {
    const { page = 0, size = 20, search, role, active } = req.query;
    const offset = page * size;

    let whereConditions = ["role != 'APODERADO'"]; // Exclude guardians
    const params = [];
    let paramIndex = 1;

    if (search) {
      whereConditions.push(`(first_name ILIKE $${paramIndex} OR last_name ILIKE $${paramIndex} OR email ILIKE $${paramIndex})`);
      params.push(`%${search}%`);
      paramIndex++;
    }

    if (role) {
      whereConditions.push(`role = $${paramIndex}`);
      params.push(role);
      paramIndex++;
    }

    if (active !== undefined) {
      whereConditions.push(`active = $${paramIndex}`);
      params.push(active === 'true');
      paramIndex++;
    }

    const whereClause = whereConditions.length > 0 ? `WHERE ${whereConditions.join(' AND ')}` : '';

    // Get total count
    const countQuery = `SELECT COUNT(*) as total FROM users ${whereClause}`;
    const countResult = await client.query(countQuery, params);
    const total = parseInt(countResult.rows[0].total);

    // Get paginated data
    const dataQuery = `
      SELECT
        id,
        first_name as "firstName",
        last_name as "lastName",
        email,
        role,
        subject,
        rut,
        phone,
        active,
        email_verified as "emailVerified",
        created_at as "createdAt",
        last_login as "lastLogin"
      FROM users
      ${whereClause}
      ORDER BY role, last_name, first_name
      LIMIT $${paramIndex} OFFSET $${paramIndex + 1}
    `;

    params.push(parseInt(size), offset);
    const dataResult = await client.query(dataQuery, params);

    const users = dataResult.rows.map(user => ({
      id: user.id,
      fullName: `${user.firstName} ${user.lastName}`,
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      role: user.role, // Return raw role for frontend validation
      subject: user.subject,
      rut: user.rut,
      phone: user.phone,
      active: user.active,
      emailVerified: user.emailVerified,
      createdAt: user.createdAt,
      lastLogin: user.lastLogin
    }));

    res.json({
      content: users,
      totalElements: total,
      totalPages: Math.ceil(total / size),
      number: parseInt(page),
      size: parseInt(size)
    });

  } catch (error) {
    logger.error('❌ Error fetching staff users:', error);
    res.status(500).json({
      success: false,
      error: 'Error al obtener personal del colegio',
      details: error.message
    });
  } finally {
    client.release();
  }
});

// Get guardian users for administration (only APODERADO)
// IMPORTANT: This must be registered BEFORE /api/users/:id to prevent route conflicts
app.get('/api/users/guardians', authenticateToken, async (req, res) => {
  const client = await dbPool.connect();
  try {
    const { page = 0, size = 20, search, active } = req.query;
    const offset = page * size;

    let whereConditions = ["role = 'APODERADO'"]; // Only guardians
    const params = [];
    let paramIndex = 1;

    if (search) {
      whereConditions.push(`(first_name ILIKE $${paramIndex} OR last_name ILIKE $${paramIndex} OR email ILIKE $${paramIndex})`);
      params.push(`%${search}%`);
      paramIndex++;
    }

    if (active !== undefined) {
      whereConditions.push(`active = $${paramIndex}`);
      params.push(active === 'true');
      paramIndex++;
    }

    const whereClause = whereConditions.length > 0 ? `WHERE ${whereConditions.join(' AND ')}` : '';

    // Get total count
    const countQuery = `SELECT COUNT(*) as total FROM users ${whereClause}`;
    const countResult = await client.query(countQuery, params);
    const total = parseInt(countResult.rows[0].total);

    // Get paginated data
    const dataQuery = `
      SELECT
        id,
        first_name as "firstName",
        last_name as "lastName",
        email,
        role,
        subject,
        rut,
        phone,
        active,
        email_verified as "emailVerified",
        created_at as "createdAt",
        last_login as "lastLogin"
      FROM users
      ${whereClause}
      ORDER BY last_name, first_name
      LIMIT $${paramIndex} OFFSET $${paramIndex + 1}
    `;

    params.push(parseInt(size), offset);
    const dataResult = await client.query(dataQuery, params);

    const users = dataResult.rows.map(user => ({
      id: user.id,
      fullName: `${user.firstName} ${user.lastName}`,
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      role: user.role,
      subject: user.subject,
      rut: user.rut,
      phone: user.phone,
      active: user.active,
      emailVerified: user.emailVerified,
      createdAt: user.createdAt,
      lastLogin: user.lastLogin
    }));

    res.json({
      content: users,
      totalElements: total,
      totalPages: Math.ceil(total / size),
      number: parseInt(page),
      size: parseInt(size)
    });

  } catch (error) {
    logger.error('❌ Error fetching guardian users:', error);
    res.status(500).json({
      success: false,
      error: 'Error al obtener apoderados',
      details: error.message
    });
  } finally {
    client.release();
  }
});

// Get all roles endpoint - required by userService.ts:180
// IMPORTANT: Must be registered BEFORE /api/users/:id to prevent route conflicts
// PUBLIC ENDPOINT - No authentication required (for frontend caching)
app.get('/api/users/roles', (req, res) => {
  // Check cache first
  const cacheKey = 'users:roles';
  const cached = userCache.get(cacheKey);
  if (cached) {
    logger.info('[Cache HIT] users:roles');
    return res.json(cached);
  }
  logger.info('[Cache MISS] users:roles');

  const roles = [
    'ADMIN',
    'TEACHER',
    'COORDINATOR',
    'CYCLE_DIRECTOR',
    'PSYCHOLOGIST',
    'APODERADO'
  ];

  const response = { roles: roles };
  // Cache for 30 minutes
  userCache.set(cacheKey, response, 1800000);
  res.json(response);
});

// Get user by ID endpoint - required by userService.ts:57
app.get('/api/users/:id', authenticateToken, (req, res) => {
  const userId = parseInt(req.params.id);
  const user = users.find(u => u.id === userId);

  if (!user) {
    return res.status(404).json({
      success: false,
      error: 'Usuario no encontrado'
    });
  }

  res.json(user);
});

// Deactivate user endpoint - required by userService.ts:111
app.put('/api/users/:id/deactivate', authenticateToken, (req, res) => {
  const userId = parseInt(req.params.id);
  const userIndex = users.findIndex(u => u.id === userId);

  if (userIndex === -1) {
    return res.status(404).json({
      success: false,
      error: 'Usuario no encontrado'
    });
  }

  users[userIndex].active = false;
  users[userIndex].updatedAt = new Date().toISOString();

  // ============= CACHE INVALIDATION =============
  // Invalidate all user-related caches after deactivation
  const cleared = userCache.clear('users:');
  logger.info(`[Cache Invalidation] Cleared ${cleared} user cache entries after user deactivation (ID: ${userId})`);

  res.json({
    success: true,
    message: 'Usuario desactivado exitosamente'
  });
});

// Activate user endpoint - required by userService.ts:145
app.put('/api/users/:id/activate', authenticateToken, (req, res) => {
  const userId = parseInt(req.params.id);
  const userIndex = users.findIndex(u => u.id === userId);

  if (userIndex === -1) {
    return res.status(404).json({
      success: false,
      error: 'Usuario no encontrado'
    });
  }

  users[userIndex].active = true;
  users[userIndex].updatedAt = new Date().toISOString();

  // ============= CACHE INVALIDATION =============
  // Invalidate all user-related caches after activation
  const cleared = userCache.clear('users:');
  logger.info(`[Cache Invalidation] Cleared ${cleared} user cache entries after user activation (ID: ${userId})`);

  res.json(users[userIndex]);
});

// Reset user password endpoint - required by userService.ts:163
app.put('/api/users/:id/reset-password', authenticateToken, (req, res) => {
  const userId = parseInt(req.params.id);
  const userIndex = users.findIndex(u => u.id === userId);

  if (userIndex === -1) {
    return res.status(404).json({
      success: false,
      error: 'Usuario no encontrado'
    });
  }

  // Simulate password reset - in real implementation, would generate and send new password
  users[userIndex].passwordResetRequired = true;
  users[userIndex].updatedAt = new Date().toISOString();

  res.json({
    success: true,
    message: 'Contraseña restablecida exitosamente'
  });
});

// Check email exists endpoint - required by authService.ts:68
app.get('/api/auth/check-email', (req, res) => {
  const { email } = req.query;

  if (!email) {
    return res.status(400).json({
      success: false,
      error: 'Email es requerido'
    });
  }

  const emailExists = users.some(user => user.email.toLowerCase() === email.toLowerCase());

  res.json(emailExists);
});

// Public endpoint to get real school staff from database - for evaluator management
app.get('/api/users/public/school-staff', async (req, res) => {
  // Check cache first
  const cacheKey = 'users:school-staff';
  const cached = userCache.get(cacheKey);
  if (cached) {
    logger.info('[Cache HIT] users:school-staff');
    return res.json(cached);
  }
  logger.info('[Cache MISS] users:school-staff');

  const client = await dbPool.connect();
  try {
    logger.info('🔍 Public: Fetching real school staff from database...');

    const query = `
      SELECT
        id,
        first_name as "firstName",
        last_name as "lastName",
        email,
        role,
        subject,
        educational_level as "educationalLevel",
        active,
        email_verified as "emailVerified",
        created_at as "createdAt"
      FROM users
      WHERE role IN ('TEACHER', 'COORDINATOR', 'CYCLE_DIRECTOR', 'PSYCHOLOGIST')
        AND active = true
        AND email_verified = true
      ORDER BY role, subject, last_name
    `;

    const result = await client.query(query);
    const schoolStaff = result.rows;

    logger.info(`✅ Found ${schoolStaff.length} real school staff members`);

    // Return in the format expected by userService
    const staffData = {
      content: schoolStaff,
      totalElements: schoolStaff.length,
      totalPages: 1,
      currentPage: 0,
      pageSize: schoolStaff.length
    };

    // Cache before sending response
    userCache.set(cacheKey, staffData, 600000); // 10 minutes
    res.json(staffData);

  } catch (error) {
    logger.error('❌ Error fetching real school staff:', error);
    res.status(500).json({
      success: false,
      error: 'Error interno del servidor',
      details: error.message
    });
  } finally {
    client.release();
  }
});

// Cache management endpoints
app.post('/api/users/cache/clear', (req, res) => {
  try {
    const { pattern } = req.body;
    const cleared = userCache.clear(pattern);
    res.json({
      success: true,
      cleared,
      message: pattern ? `Cleared ${cleared} cache entries matching: ${pattern}` : `Cleared ${cleared} total cache entries`
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

app.get('/api/users/cache/stats', (req, res) => {
  res.json({
    cacheStats: userCache.getStats(),
    cacheSize: userCache.size(),
    serviceUptime: process.uptime()
  });
});

app.listen(port, () => {
  logger.info(`User Service running on port ${port}`);
  logger.info('✅ Connection pooling enabled');
  logger.info('✅ In-memory cache enabled');
  logger.info('Cache endpoints:');
  logger.info('  - POST /api/users/cache/clear');
  logger.info('  - GET  /api/users/cache/stats');
});
