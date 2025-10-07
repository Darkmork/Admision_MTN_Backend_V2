#!/usr/bin/env node

/**
 * Railway Startup Script
 * Starts all mock services with proper port configuration for Railway
 */

const { spawn } = require('child_process');

// Railway provides PORT environment variable
const RAILWAY_PORT = process.env.PORT || 8080;

console.log(`🚀 Starting MTN Admission Backend on Railway`);
console.log(`📡 Railway Port: ${RAILWAY_PORT}`);
console.log(`🗄️  Database: ${process.env.DB_HOST || 'Not configured'}`);

// Environment variables for all services
const env = {
  ...process.env,
  PORT: RAILWAY_PORT,
  USER_SERVICE_PORT: 8082,
  APPLICATION_SERVICE_PORT: 8083,
  EVALUATION_SERVICE_PORT: 8084,
  NOTIFICATION_SERVICE_PORT: 8085,
  DASHBOARD_SERVICE_PORT: 8086,
  GUARDIAN_SERVICE_PORT: 8087,
  GATEWAY_PORT: RAILWAY_PORT
};

// Services to start
const services = [
  { name: 'User Service', script: 'mock-user-service.js' },
  { name: 'Application Service', script: 'mock-application-service.js' },
  { name: 'Evaluation Service', script: 'mock-evaluation-service.js' },
  { name: 'Notification Service', script: 'mock-notification-service.js' },
  { name: 'Dashboard Service', script: 'mock-dashboard-service.js' },
  { name: 'Guardian Service', script: 'mock-guardian-service.js' }
];

const processes = [];

// Start all services
services.forEach(({ name, script }) => {
  console.log(`▶️  Starting ${name}...`);

  const proc = spawn('node', [script], {
    env,
    stdio: 'inherit'
  });

  proc.on('error', (err) => {
    console.error(`❌ ${name} failed to start:`, err);
  });

  proc.on('exit', (code) => {
    if (code !== 0) {
      console.error(`❌ ${name} exited with code ${code}`);
    }
  });

  processes.push({ name, proc });
});

// Graceful shutdown
process.on('SIGTERM', () => {
  console.log('🛑 SIGTERM received, shutting down gracefully...');
  processes.forEach(({ name, proc }) => {
    console.log(`   Stopping ${name}...`);
    proc.kill('SIGTERM');
  });
  process.exit(0);
});

process.on('SIGINT', () => {
  console.log('🛑 SIGINT received, shutting down...');
  processes.forEach(({ name, proc }) => {
    proc.kill('SIGINT');
  });
  process.exit(0);
});

// Keep alive
console.log('✅ All services started');
console.log(`🌐 Backend available on port ${RAILWAY_PORT}`);
