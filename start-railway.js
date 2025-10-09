#!/usr/bin/env node

/**
 * Railway Startup Script - Complete Version
 *
 * This script:
 * 1. Starts all 6 mock services on ports 8082-8087
 * 2. Starts an API Gateway proxy on Railway PORT (8080) that routes to services
 * 3. Handles graceful shutdown
 *
 * Services:
 * - User Service: 8082 (auth, users)
 * - Application Service: 8083 (applications, documents)
 * - Evaluation Service: 8084 (evaluations, interviews)
 * - Notification Service: 8085 (notifications, email)
 * - Dashboard Service: 8086 (dashboard, analytics)
 * - Guardian Service: 8087 (guardians)
 */

const { spawn } = require('child_process');
const express = require('express');
const cors = require('cors');
const { createProxyMiddleware } = require('http-proxy-middleware');
const axios = require('axios');

// Railway provides PORT environment variable
const RAILWAY_PORT = parseInt(process.env.PORT || '8080');
const NODE_ENV = process.env.NODE_ENV || 'production';

console.log('');
console.log('========================================');
console.log('🚀 MTN Admission Backend - Railway');
console.log('========================================');
console.log(`📡 Environment: ${NODE_ENV}`);
console.log(`🔧 Gateway Port: ${RAILWAY_PORT}`);
console.log(`🗄️  Database: ${process.env.DATABASE_URL ? 'Railway PostgreSQL' : 'Local'}`);
console.log('');

// Services configuration
const services = [
  { name: 'user', script: 'mock-user-service.js', port: 8082, routes: ['/api/auth', '/api/users'] },
  { name: 'application', script: 'mock-application-service.js', port: 8083, routes: ['/api/applications', '/api/documents'] },
  { name: 'evaluation', script: 'mock-evaluation-service.js', port: 8084, routes: ['/api/evaluations', '/api/interviews'] },
  { name: 'notification', script: 'mock-notification-service.js', port: 8085, routes: ['/api/notifications', '/api/email'] },
  { name: 'dashboard', script: 'mock-dashboard-service.js', port: 8086, routes: ['/api/dashboard', '/api/analytics'] },
  { name: 'guardian', script: 'mock-guardian-service.js', port: 8087, routes: ['/api/guardians'] }
];

const runningProcesses = [];

// ============================================================================
// START ALL MICROSERVICES
// ============================================================================

console.log('📦 Starting microservices...\n');

services.forEach(service => {
  console.log(`   Starting ${service.name}-service on port ${service.port}...`);

  const proc = spawn('node', [service.script], {
    env: { ...process.env, PORT: service.port },
    stdio: ['ignore', 'pipe', 'pipe']
  });

  proc.stdout.on('data', (data) => {
    const output = data.toString().trim();
    // Only log important messages (errors, warnings)
    if (output.includes('ERROR') || output.includes('❌') || output.includes('⚠️')) {
      console.log(`   [${service.name}] ${output}`);
    }
  });

  proc.stderr.on('data', (data) => {
    console.error(`   [${service.name}] ${data.toString().trim()}`);
  });

  proc.on('error', (err) => {
    console.error(`   ❌ ${service.name}-service failed to start:`, err.message);
  });

  proc.on('exit', (code) => {
    if (code !== 0) {
      console.error(`   ❌ ${service.name}-service exited with code ${code}`);
      // Exit Railway deployment if any service crashes
      process.exit(1);
    }
  });

  runningProcesses.push({ ...service, process: proc });
});

console.log(`\n✅ Started ${services.length}/6 services\n`);

// ============================================================================
// WAIT FOR SERVICES TO BE READY
// ============================================================================

const waitForService = async (port, maxRetries = 60, retryDelay = 1000) => {
  for (let i = 0; i < maxRetries; i++) {
    try {
      const response = await axios.get(`http://localhost:${port}/health`, {
        timeout: 1000,
        validateStatus: () => true
      });
      if (response.status === 200) {
        return true;
      }
    } catch (error) {
      // Service not ready yet, retry
    }
    await new Promise(resolve => setTimeout(resolve, retryDelay));
  }
  return false;
};

console.log('⏳ Waiting for services to be healthy...\n');

(async () => {
  try {
    // Wait for all services in parallel
    const healthChecks = services.map(async (service) => {
      const ready = await waitForService(service.port);
      if (ready) {
        console.log(`   ✅ ${service.name}-service ready (port ${service.port})`);
        return true;
      } else {
        console.error(`   ❌ ${service.name}-service health check timeout`);
        return false;
      }
    });

    const results = await Promise.all(healthChecks);
    const allReady = results.every(r => r === true);

    if (!allReady) {
      console.error('\n❌ Some services failed to start. Exiting...');
      process.exit(1);
    }

    console.log('\n✅ All services are healthy\n');

    // ============================================================================
    // START API GATEWAY
    // ============================================================================

    console.log('🌐 Starting API Gateway...\n');

    const app = express();

    // CORS configuration (allow Vercel frontend)
    app.use(cors({
      origin: function (origin, callback) {
        // Allow no origin (mobile apps, Postman, curl)
        if (!origin) return callback(null, true);

        // Allow Vercel and localhost
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
    }));

    app.use(express.json({ limit: '50mb' }));
    app.use(express.urlencoded({ extended: true, limit: '50mb' }));

    // Request logging (only non-health requests)
    app.use((req, res, next) => {
      if (req.path !== '/health') {
        const start = Date.now();
        res.on('finish', () => {
          const duration = Date.now() - start;
          console.log(`${req.method} ${req.path} - ${res.statusCode} (${duration}ms)`);
        });
      }
      next();
    });

    // Gateway health check
    app.get('/health', (req, res) => {
      res.json({
        status: 'UP',
        timestamp: new Date().toISOString(),
        gateway: 'Railway API Gateway',
        environment: NODE_ENV,
        port: RAILWAY_PORT,
        services: services.map(s => ({
          name: s.name,
          port: s.port,
          routes: s.routes,
          status: 'running'
        }))
      });
    });

    // Root endpoint
    app.get('/', (req, res) => {
      res.json({
        message: 'MTN Admission System API - Railway Deployment',
        version: '2.0.0',
        status: 'Running',
        environment: NODE_ENV,
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
        services: services.length
      });
    });

    // Proxy configuration
    const proxyOptions = (target) => ({
      target,
      changeOrigin: true,
      logLevel: 'silent',
      timeout: 30000,
      proxyTimeout: 30000,
      onError: (err, req, res) => {
        console.error(`Proxy error for ${req.path}:`, err.message);
        res.status(502).json({
          error: 'Bad Gateway',
          message: 'Service temporarily unavailable',
          path: req.path,
          target
        });
      },
      onProxyReq: (proxyReq, req) => {
        // Forward original headers
        if (req.body && Object.keys(req.body).length > 0) {
          const bodyData = JSON.stringify(req.body);
          proxyReq.setHeader('Content-Type', 'application/json');
          proxyReq.setHeader('Content-Length', Buffer.byteLength(bodyData));
          proxyReq.write(bodyData);
        }
      }
    });

    // Mount service proxies
    services.forEach(service => {
      service.routes.forEach(route => {
        console.log(`   Mounting ${route} → http://localhost:${service.port}`);
        app.use(route, createProxyMiddleware(proxyOptions(`http://localhost:${service.port}`)));
      });
    });

    console.log('');

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

    // Error handler
    app.use((err, req, res, next) => {
      console.error('Gateway error:', err.message);
      res.status(500).json({
        error: 'Internal Server Error',
        message: err.message
      });
    });

    // Start gateway server
    app.listen(RAILWAY_PORT, '0.0.0.0', () => {
      console.log('========================================');
      console.log('✅ RAILWAY DEPLOYMENT READY');
      console.log('========================================');
      console.log(`🌐 Gateway: http://0.0.0.0:${RAILWAY_PORT}`);
      console.log(`📊 Services: ${services.length}/6 running`);
      console.log(`🗄️  Database: ${process.env.DATABASE_URL ? 'Configured' : 'Not configured'}`);
      console.log('========================================');
      console.log('');
      console.log('🔍 Test endpoints:');
      console.log(`   GET  ${RAILWAY_PORT === 8080 ? 'http://localhost:8080' : 'http://0.0.0.0:' + RAILWAY_PORT}/health`);
      console.log(`   POST ${RAILWAY_PORT === 8080 ? 'http://localhost:8080' : 'http://0.0.0.0:' + RAILWAY_PORT}/api/auth/login`);
      console.log(`   GET  ${RAILWAY_PORT === 8080 ? 'http://localhost:8080' : 'http://0.0.0.0:' + RAILWAY_PORT}/api/users/roles`);
      console.log('');
    });

  } catch (error) {
    console.error('\n❌ Fatal error during startup:', error.message);
    console.error(error.stack);
    process.exit(1);
  }
})();

// ============================================================================
// GRACEFUL SHUTDOWN
// ============================================================================

const shutdown = (signal) => {
  console.log(`\n🛑 ${signal} received, shutting down gracefully...`);

  runningProcesses.forEach(({ name, process }) => {
    console.log(`   Stopping ${name}-service...`);
    process.kill('SIGTERM');
  });

  setTimeout(() => {
    console.log('✅ Shutdown complete');
    process.exit(0);
  }, 2000);
};

process.on('SIGTERM', () => shutdown('SIGTERM'));
process.on('SIGINT', () => shutdown('SIGINT'));

// Unhandled errors
process.on('unhandledRejection', (reason, promise) => {
  console.error('❌ Unhandled Rejection:', reason);
});

process.on('uncaughtException', (error) => {
  console.error('❌ Uncaught Exception:', error);
  process.exit(1);
});
