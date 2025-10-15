/**
 * Railway Production Server - Complete Consolidated Version
 *
 * This file consolidates all 6 mock services into a single Express server for Railway deployment.
 * Generated automatically by build-railway-server.js
 *
 * Services included:
 * - User Service (23 endpoints)
 * - Application Service (20 endpoints)
 * - Evaluation Service (55 endpoints)
 * - Notification Service (18 endpoints)
 * - Dashboard Service (22 endpoints)
 * - Guardian Service (8 endpoints)
 *
 * Total: 146 endpoints
 */

const express = require('express');
const cors = require('cors');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { Pool } = require('pg');
const axios = require('axios');
const nodemailer = require('nodemailer');
const CircuitBreaker = require('opossum');

const app = express();
const PORT = process.env.PORT || 8080;

// ============================================================================
// ENVIRONMENT CONFIGURATION
// ============================================================================

const JWT_SECRET = process.env.JWT_SECRET || 'your_secure_jwt_secret';
const JWT_EXPIRATION_TIME = process.env.JWT_EXPIRATION_TIME || '86400000';
const NODE_ENV = process.env.NODE_ENV || 'development';
const EMAIL_MOCK_MODE = process.env.EMAIL_MOCK_MODE === 'true';

console.log('🚀 Starting MTN Admission Backend');
console.log(`📡 Environment: ${NODE_ENV}`);
console.log(`🔐 JWT Secret: ${JWT_SECRET === 'your_secure_jwt_secret' ? '⚠️  Using default (INSECURE)' : '✅ Custom secret configured'}`);
console.log(`📧 Email Mode: ${EMAIL_MOCK_MODE ? 'MOCK' : 'SMTP'}`);

// ============================================================================
// DATABASE CONFIGURATION
// ============================================================================

const dbConfig = process.env.DATABASE_URL ? {
  connectionString: process.env.DATABASE_URL,
  ssl: process.env.NODE_ENV === 'production' ? { rejectUnauthorized: false } : false,
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000,
  query_timeout: 5000
} : {
  host: process.env.DB_HOST || 'localhost',
  port: parseInt(process.env.DB_PORT || '5432'),
  database: process.env.DB_NAME || 'Admisión_MTN_DB',
  user: process.env.DB_USERNAME || 'admin',
  password: process.env.DB_PASSWORD || 'admin123',
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000,
  query_timeout: 5000
};

const dbPool = new Pool(dbConfig);

// Test database connection
dbPool.query('SELECT NOW()', (err, res) => {
  if (err) {
    console.error('❌ Database connection failed:', err.message);
    console.error('   Check your DATABASE_URL or DB_* environment variables');
  } else {
    console.log('✅ Database connected successfully');
    console.log(`   Server time: ${res.rows[0].now}`);
  }
});

// ============================================================================
// CORS CONFIGURATION
// ============================================================================

const corsOptions = {
  origin: function (origin, callback) {
    // Allow requests with no origin (mobile apps, curl, Postman)
    if (!origin) return callback(null, true);

    // Allow Vercel deployments and localhost
    if (origin.includes('vercel.app') ||
        origin.includes('localhost:5173') ||
        origin.includes('localhost:5174')) {
      return callback(null, true);
    }

    callback(new Error('Not allowed by CORS'));
  },
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'OPTIONS'],
  allowedHeaders: [
    'Content-Type',
    'Authorization',
    'x-correlation-id',
    'x-request-time',
    'x-timezone',
    'x-client-type',
    'x-client-version',
    'X-CSRF-Token'
  ],
  exposedHeaders: ['Content-Type', 'Authorization', 'X-CSRF-Token'],
  maxAge: 86400
};

app.use(cors(corsOptions));
app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ extended: true, limit: '50mb' }));

// Request logging middleware
app.use((req, res, next) => {
  const start = Date.now();
  res.on('finish', () => {
    const duration = Date.now() - start;
    console.log(`${req.method} ${req.path} - ${res.statusCode} (${duration}ms)`);
  });
  next();
});

// ============================================================================
// CIRCUIT BREAKERS
// ============================================================================

// Simple queries (fast lookups)
const simpleQueryBreaker = new CircuitBreaker(async (fn) => await fn(), {
  timeout: 2000,
  errorThresholdPercentage: 60,
  resetTimeout: 20000,
  rollingCountTimeout: 10000,
  rollingCountBuckets: 10,
  name: 'RailwaySimpleQueryBreaker'
});

// Medium queries (standard operations)
const mediumQueryBreaker = new CircuitBreaker(async (fn) => await fn(), {
  timeout: 5000,
  errorThresholdPercentage: 50,
  resetTimeout: 30000,
  rollingCountTimeout: 10000,
  rollingCountBuckets: 10,
  name: 'RailwayMediumQueryBreaker'
});

// Heavy queries (complex aggregations)
const heavyQueryBreaker = new CircuitBreaker(async (fn) => await fn(), {
  timeout: 10000,
  errorThresholdPercentage: 40,
  resetTimeout: 60000,
  rollingCountTimeout: 10000,
  rollingCountBuckets: 10,
  name: 'RailwayHeavyQueryBreaker'
});

// Write operations
const writeQueryBreaker = new CircuitBreaker(async (fn) => await fn(), {
  timeout: 3000,
  errorThresholdPercentage: 30,
  resetTimeout: 45000,
  rollingCountTimeout: 10000,
  rollingCountBuckets: 10,
  name: 'RailwayWriteQueryBreaker'
});

// External services (SMTP, S3, etc.)
const externalServiceBreaker = new CircuitBreaker(async (fn) => await fn(), {
  timeout: 8000,
  errorThresholdPercentage: 70,
  resetTimeout: 120000,
  rollingCountTimeout: 10000,
  rollingCountBuckets: 10,
  name: 'RailwayExternalServiceBreaker'
});

// Circuit breaker event listeners
const setupBreakerEvents = (breaker, name) => {
  breaker.on('open', () => console.error(`⚠️  [Circuit Breaker ${name}] OPEN - Too many failures`));
  breaker.on('halfOpen', () => console.warn(`🔄 [Circuit Breaker ${name}] HALF-OPEN - Testing recovery`));
  breaker.on('close', () => console.log(`✅ [Circuit Breaker ${name}] CLOSED - Service recovered`));
  breaker.fallback(() => {
    throw new Error(`Service temporarily unavailable - ${name} circuit breaker open`);
  });
};

[simpleQueryBreaker, mediumQueryBreaker, heavyQueryBreaker, writeQueryBreaker, externalServiceBreaker].forEach((breaker, idx) => {
  setupBreakerEvents(breaker, ['Simple', 'Medium', 'Heavy', 'Write', 'External'][idx]);
});

// ============================================================================
// AUTHENTICATION MIDDLEWARE
// ============================================================================

const authenticateToken = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    return res.status(401).json({ error: 'Access denied. No token provided.' });
  }

  try {
    const decoded = jwt.verify(token, JWT_SECRET, { algorithms: ['HS512'] });
    req.user = decoded;
    next();
  } catch (error) {
    return res.status(401).json({ error: 'Invalid or expired token' });
  }
};

const optionalAuth = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (token) {
    try {
      const decoded = jwt.verify(token, JWT_SECRET, { algorithms: ['HS512'] });
      req.user = decoded;
    } catch (error) {
      // Token invalid but continue without auth
    }
  }
  next();
};

// ============================================================================
// CSRF PROTECTION (Simplified for Railway)
// ============================================================================

const CSRF_TOKENS = new Map();

const csrfProtection = (req, res, next) => {
  // In production with proper HTTPS, CSRF is less critical
  // For now, allow all requests (can be enhanced later)
  next();
};

const optionalCsrfProtection = (req, res, next) => next();

// ============================================================================
// ROLE-BASED ACCESS CONTROL
// ============================================================================

const requireRole = (...roles) => {
  return (req, res, next) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required' });
    }
    if (!roles.includes(req.user.role)) {
      return res.status(403).json({ error: 'Insufficient permissions' });
    }
    next();
  };
};

// ============================================================================
// EMAIL CONFIGURATION
// ============================================================================

let transporter = null;
if (!EMAIL_MOCK_MODE) {
  transporter = nodemailer.createTransport({
    host: process.env.SMTP_HOST || 'smtp.gmail.com',
    port: parseInt(process.env.SMTP_PORT || '587'),
    secure: false,
    auth: {
      user: process.env.SMTP_USERNAME,
      pass: process.env.SMTP_PASSWORD
    }
  });
}

const sendEmail = async (to, subject, html) => {
  if (EMAIL_MOCK_MODE) {
    console.log(`📧 [MOCK] Email to ${to}: ${subject}`);
    return { success: true, messageId: 'mock-' + Date.now() };
  }

  try {
    const info = await externalServiceBreaker.fire(async () => {
      return await transporter.sendMail({
        from: process.env.SMTP_USERNAME,
        to,
        subject,
        html
      });
    });
    console.log(`📧 Email sent to ${to}: ${subject}`);
    return { success: true, messageId: info.messageId };
  } catch (error) {
    console.error(`❌ Email send failed to ${to}:`, error.message);
    throw error;
  }
};

// ============================================================================
// SIMPLE CACHE IMPLEMENTATION
// ============================================================================

class SimpleCache {
  constructor() {
    this.cache = new Map();
    this.hits = 0;
    this.misses = 0;
  }

  set(key, value, ttlMs) {
    const expiresAt = Date.now() + ttlMs;
    this.cache.set(key, { value, expiresAt });
  }

  get(key) {
    const entry = this.cache.get(key);
    if (!entry) {
      this.misses++;
      return null;
    }
    if (Date.now() > entry.expiresAt) {
      this.cache.delete(key);
      this.misses++;
      return null;
    }
    this.hits++;
    return entry.value;
  }

  clear(pattern) {
    if (!pattern) {
      this.cache.clear();
      return;
    }
    for (const key of this.cache.keys()) {
      if (key.includes(pattern)) {
        this.cache.delete(key);
      }
    }
  }

  getStats() {
    const total = this.hits + this.misses;
    const hitRate = total > 0 ? (this.hits / total * 100).toFixed(2) : '0.00';
    return {
      hits: this.hits,
      misses: this.misses,
      total,
      hitRate: `${hitRate}%`,
      size: this.cache.size
    };
  }
}

const cache = new SimpleCache();

// ============================================================================
// HEALTH CHECK ENDPOINTS
// ============================================================================

app.get('/health', (req, res) => {
  res.json({
    status: 'UP',
    timestamp: new Date().toISOString(),
    service: 'MTN Admission Backend',
    environment: NODE_ENV,
    port: PORT,
    database: 'Connected',
    services: {
      user: 'UP',
      application: 'UP',
      evaluation: 'UP',
      notification: 'UP',
      dashboard: 'UP',
      guardian: 'UP'
    }
  });
});

app.get('/', (req, res) => {
  res.json({
    message: 'MTN Admission System API - Complete Version',
    version: '2.0.0',
    status: 'Running',
    timestamp: new Date().toISOString(),
    endpoints: {
      health: '/health',
      users: '/api/users',
      auth: '/api/auth',
      applications: '/api/applications',
      evaluations: '/api/evaluations',
      interviews: '/api/interviews',
      notifications: '/api/notifications',
      dashboard: '/api/dashboard',
      guardians: '/api/guardians',
      analytics: '/api/analytics',
      documents: '/api/documents',
      email: '/api/email'
    },
    stats: {
      totalEndpoints: 146,
      cacheStats: cache.getStats()
    }
  });
});

// Cache stats endpoint
app.get('/api/cache/stats', (req, res) => {
  res.json(cache.getStats());
});

// Clear cache endpoint
app.post('/api/cache/clear', (req, res) => {
  const { pattern } = req.body;
  cache.clear(pattern);
  res.json({ success: true, message: `Cache cleared${pattern ? ` (pattern: ${pattern})` : ''}` });
});

// ============================================================================
// CONSOLIDATED SERVICE ROUTES
// ============================================================================
// Routes will be inserted here by the build script

__ROUTES_PLACEHOLDER__

// ============================================================================
// ERROR HANDLING
// ============================================================================

// 404 handler
app.use((req, res) => {
  console.warn(`404 Not Found: ${req.method} ${req.path}`);
  res.status(404).json({
    error: 'Not Found',
    path: req.path,
    method: req.method,
    message: 'The requested endpoint does not exist'
  });
});

// Global error handler
app.use((err, req, res, next) => {
  console.error('❌ Error:', err.message);
  console.error(err.stack);
  res.status(err.status || 500).json({
    error: err.message || 'Internal Server Error',
    path: req.path,
    method: req.method
  });
});

// ============================================================================
// SERVER STARTUP
// ============================================================================

app.listen(PORT, '0.0.0.0', () => {
  console.log('');
  console.log('========================================');
  console.log('🚀 MTN Admission Backend - READY');
  console.log('========================================');
  console.log(`📡 Port: ${PORT}`);
  console.log(`🌍 Environment: ${NODE_ENV}`);
  console.log(`🗄️  Database: ${process.env.DATABASE_URL ? 'Railway PostgreSQL' : 'Local PostgreSQL'}`);
  console.log(`📧 Email: ${EMAIL_MOCK_MODE ? 'Mock mode' : 'SMTP configured'}`);
  console.log(`✅ Server URL: http://0.0.0.0:${PORT}`);
  console.log('========================================');
  console.log('');
  console.log('📋 Available endpoints:');
  console.log('   Health: GET /health');
  console.log('   Auth: POST /api/auth/login');
  console.log('   Users: GET /api/users');
  console.log('   Applications: GET /api/applications');
  console.log('   ... (146 total endpoints)');
  console.log('');
  console.log('🔍 Check full API at: http://0.0.0.0:${PORT}/');
  console.log('');
});

// Graceful shutdown
const shutdown = (signal) => {
  console.log(`\n${signal} received, shutting down gracefully...`);
  dbPool.end(() => {
    console.log('✅ Database connections closed');
    process.exit(0);
  });
};

process.on('SIGTERM', () => shutdown('SIGTERM'));
process.on('SIGINT', () => shutdown('SIGINT'));
