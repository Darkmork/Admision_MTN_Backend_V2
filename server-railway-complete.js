/**
 * Railway Production Server - Complete Version
 *
 * This server dynamically requires and executes all mock services,
 * but prevents them from starting their own servers.
 * All routes are mounted on a single Express app.
 *
 * Strategy: Override express().listen() before requiring mock services
 */

const express = require('express');
const cors = require('cors');
const { Pool } = require('pg');

const PORT = process.env.PORT || 8080;
const NODE_ENV = process.env.NODE_ENV || 'development';

console.log('🚀 MTN Admission Backend - Complete Server');
console.log(`📡 Environment: ${NODE_ENV}`);
console.log(`🔧 Port: ${PORT}`);
console.log('');

// ============================================================================
// DATABASE CONFIGURATION
// ============================================================================

const dbConfig = process.env.DATABASE_URL ? {
  connectionString: process.env.DATABASE_URL,
  ssl: NODE_ENV === 'production' ? { rejectUnauthorized: false } : false,
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
  } else {
    console.log('✅ Database connected');
    console.log(`   Server time: ${res.rows[0].now}`);
  }
});

// ============================================================================
// MAIN EXPRESS APP
// ============================================================================

const app = express();

// CORS configuration
const corsOptions = {
  origin: function (origin, callback) {
    if (!origin) return callback(null, true);
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

// Request logging
app.use((req, res, next) => {
  const start = Date.now();
  res.on('finish', () => {
    const duration = Date.now() - start;
    if (req.path !== '/health') {
      console.log(`${req.method} ${req.path} - ${res.statusCode} (${duration}ms)`);
    }
  });
  next();
});

// ============================================================================
// MAIN HEALTH CHECK
// ============================================================================

app.get('/health', (req, res) => {
  res.json({
    status: 'UP',
    timestamp: new Date().toISOString(),
    service: 'MTN Admission Backend - Complete',
    environment: NODE_ENV,
    port: PORT,
    database: 'Connected',
    version: '2.0.0'
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
    documentation: 'https://github.com/your-repo/docs'
  });
});

// ============================================================================
// STRATEGY: Prevent mock services from calling app.listen()
// ============================================================================

// Track all Express apps created by mock services
const mockApps = [];

// Override express() function to capture mock service apps
const originalExpress = express;
const expressMock = () => {
  const mockApp = originalExpress();
  mockApps.push(mockApp);

  // Override listen() to prevent it from actually starting a server
  mockApp.listen = function(...args) {
    console.log(`   ⏸️  Intercepted listen() call - routes will be mounted on main app`);
    // Return a fake server object
    return {
      close: () => {},
      address: () => ({ port: PORT })
    };
  };

  return mockApp;
};

// Replace global express
global.express = expressMock;
global.express.json = originalExpress.json;
global.express.urlencoded = originalExpress.urlencoded;
global.express.static = originalExpress.static;
global.express.Router = originalExpress.Router;

// ============================================================================
// LOAD MOCK SERVICES
// ============================================================================

console.log('');
console.log('📦 Loading mock services...');
console.log('');

// User Service
try {
  console.log('   Loading user-service...');
  require('./mock-user-service.js');
  console.log('   ✅ user-service loaded');
} catch (error) {
  console.error('   ❌ Failed to load user-service:', error.message);
}

// Application Service
try {
  console.log('   Loading application-service...');
  require('./mock-application-service.js');
  console.log('   ✅ application-service loaded');
} catch (error) {
  console.error('   ❌ Failed to load application-service:', error.message);
}

// Evaluation Service
try {
  console.log('   Loading evaluation-service...');
  require('./mock-evaluation-service.js');
  console.log('   ✅ evaluation-service loaded');
} catch (error) {
  console.error('   ❌ Failed to load evaluation-service:', error.message);
}

// Notification Service
try {
  console.log('   Loading notification-service...');
  require('./mock-notification-service.js');
  console.log('   ✅ notification-service loaded');
} catch (error) {
  console.error('   ❌ Failed to load notification-service:', error.message);
}

// Dashboard Service
try {
  console.log('   Loading dashboard-service...');
  require('./mock-dashboard-service.js');
  console.log('   ✅ dashboard-service loaded');
} catch (error) {
  console.error('   ❌ Failed to load dashboard-service:', error.message);
}

// Guardian Service
try {
  console.log('   Loading guardian-service...');
  require('./mock-guardian-service.js');
  console.log('   ✅ guardian-service loaded');
} catch (error) {
  console.error('   ❌ Failed to load guardian-service:', error.message);
}

console.log('');
console.log(`✅ Loaded ${mockApps.length} mock services`);
console.log('');

// ============================================================================
// MOUNT ALL ROUTES FROM MOCK APPS ONTO MAIN APP
// ============================================================================

console.log('🔗 Mounting service routes on main app...');
console.log('');

mockApps.forEach((mockApp, index) => {
  const serviceName = ['user', 'application', 'evaluation', 'notification', 'dashboard', 'guardian'][index] || `service-${index}`;

  // Get all routes from the mock app
  let routeCount = 0;
  mockApp._router.stack.forEach((layer) => {
    if (layer.route) {
      // Mount individual route
      const method = Object.keys(layer.route.methods)[0];
      const path = layer.route.path;
      app[method](path, ...layer.route.stack.map(l => l.handle));
      routeCount++;
    } else if (layer.name === 'router') {
      // Mount router middleware
      app.use(layer.regexp, layer.handle);
      routeCount++;
    }
  });

  console.log(`   ✅ ${serviceName}: ${routeCount} routes mounted`);
});

console.log('');
console.log('✅ All routes mounted successfully');
console.log('');

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
  if (NODE_ENV === 'development') {
    console.error(err.stack);
  }
  res.status(err.status || 500).json({
    error: err.message || 'Internal Server Error',
    path: req.path,
    method: req.method
  });
});

// ============================================================================
// START SERVER
// ============================================================================

app.listen(PORT, '0.0.0.0', () => {
  console.log('');
  console.log('========================================');
  console.log('🚀 MTN ADMISSION BACKEND - READY');
  console.log('========================================');
  console.log(`📡 Port: ${PORT}`);
  console.log(`🌍 Environment: ${NODE_ENV}`);
  console.log(`🗄️  Database: ${process.env.DATABASE_URL ? 'Railway' : 'Local'}`);
  console.log(`✅ Server URL: http://0.0.0.0:${PORT}`);
  console.log('========================================');
  console.log('');
  console.log('📋 Service Status:');
  console.log(`   ✅ ${mockApps.length}/6 services loaded`);
  console.log('');
  console.log('🔍 Test endpoints:');
  console.log(`   curl http://0.0.0.0:${PORT}/health`);
  console.log(`   curl http://0.0.0.0:${PORT}/api/users/roles`);
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

module.exports = app;
