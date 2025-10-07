/**
 * Railway Production Server
 * Single Express server that imports all mock services
 */

const express = require('express');
const cors = require('cors');

const app = express();
const PORT = process.env.PORT || 8080;

// Middleware
app.use(cors());
app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ extended: true, limit: '50mb' }));

// Health check
app.get('/health', (req, res) => {
  res.json({
    status: 'UP',
    timestamp: new Date().toISOString(),
    service: 'MTN Admission Backend',
    environment: 'production',
    port: PORT
  });
});

app.get('/', (req, res) => {
  res.json({
    message: 'MTN Admission System API',
    version: '1.0.0',
    status: 'Running',
    endpoints: {
      health: '/health',
      users: '/api/users',
      auth: '/api/auth',
      applications: '/api/applications',
      evaluations: '/api/evaluations',
      interviews: '/api/interviews',
      notifications: '/api/notifications',
      dashboard: '/api/dashboard',
      guardians: '/api/guardians'
    }
  });
});

// Import route handlers from mock services
// We'll create a consolidated routes file
const setupRoutes = (app) => {
  // For now, basic placeholder routes
  app.get('/api/users/health', (req, res) => {
    res.json({ service: 'users', status: 'UP' });
  });

  app.get('/api/applications/health', (req, res) => {
    res.json({ service: 'applications', status: 'UP' });
  });

  app.get('/api/evaluations/health', (req, res) => {
    res.json({ service: 'evaluations', status: 'UP' });
  });

  app.get('/api/notifications/health', (req, res) => {
    res.json({ service: 'notifications', status: 'UP' });
  });

  app.get('/api/dashboard/health', (req, res) => {
    res.json({ service: 'dashboard', status: 'UP' });
  });

  app.get('/api/guardians/health', (req, res) => {
    res.json({ service: 'guardians', status: 'UP' });
  });
};

setupRoutes(app);

// Error handling
app.use((err, req, res, next) => {
  console.error('Error:', err);
  res.status(500).json({
    error: 'Internal Server Error',
    message: err.message
  });
});

// 404 handler
app.use((req, res) => {
  res.status(404).json({
    error: 'Not Found',
    path: req.path
  });
});

// Start server
app.listen(PORT, '0.0.0.0', () => {
  console.log(`🚀 MTN Admission Backend running on port ${PORT}`);
  console.log(`📡 Environment: ${process.env.NODE_ENV || 'development'}`);
  console.log(`🗄️  Database: ${process.env.DB_HOST || 'Not configured'}`);
  console.log(`✅ Server ready at http://0.0.0.0:${PORT}`);
});

// Graceful shutdown
process.on('SIGTERM', () => {
  console.log('SIGTERM received, shutting down gracefully...');
  process.exit(0);
});

process.on('SIGINT', () => {
  console.log('SIGINT received, shutting down...');
  process.exit(0);
});
