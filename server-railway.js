/**
 * Railway Production Server
 * Single Express server that imports all mock services
 */

const express = require('express');
const cors = require('cors');

const app = express();
const PORT = process.env.PORT || 8080;

// CORS configuration
const corsOptions = {
  origin: function (origin, callback) {
    // Allow requests with no origin (like mobile apps or curl)
    if (!origin) return callback(null, true);

    // Allow all Vercel preview deployments and production
    if (origin.includes('vercel.app') ||
        origin.includes('localhost:5173') ||
        origin.includes('localhost:5174')) {
      return callback(null, true);
    }

    callback(new Error('Not allowed by CORS'));
  },
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization', 'x-correlation-id', 'x-request-time', 'x-timezone', 'x-client-type', 'x-client-version'],
  exposedHeaders: ['Content-Type', 'Authorization'],
  maxAge: 86400
};

// Middleware
app.use(cors(corsOptions));
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

// Database configuration
const { Pool } = require('pg');
const bcrypt = require('bcryptjs');

const dbPool = new Pool({
  host: process.env.DB_HOST || 'localhost',
  port: process.env.DB_PORT || 5432,
  database: process.env.DB_NAME || 'Admisión_MTN_DB',
  user: process.env.DB_USERNAME || 'admin',
  password: process.env.DB_PASSWORD || 'admin123',
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000
});

// JWT utilities
const jwt = require('jsonwebtoken');
const JWT_SECRET = process.env.JWT_SECRET || 'your_secure_jwt_secret';

// Import route handlers from mock services
const setupRoutes = (app) => {
  // Health checks
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

  // Public key endpoint (returns null to indicate encryption is not available)
  app.get('/api/auth/public-key', (req, res) => {
    res.json({
      publicKey: null,
      encryptionAvailable: false,
      message: 'Credential encryption is not enabled. Using direct authentication.'
    });
  });

  // Authentication endpoint
  app.post('/api/auth/login', async (req, res) => {
    try {
      const { email, password } = req.body;

      if (!email || !password) {
        return res.status(400).json({ error: 'Email and password are required' });
      }

      const query = 'SELECT id, email, password, role, first_name, last_name FROM users WHERE email = $1';
      const result = await dbPool.query(query, [email.toLowerCase()]);

      if (result.rows.length === 0) {
        return res.status(401).json({ error: 'Invalid credentials' });
      }

      const user = result.rows[0];
      const passwordMatch = await bcrypt.compare(password, user.password);

      if (!passwordMatch) {
        return res.status(401).json({ error: 'Invalid credentials' });
      }

      const token = jwt.sign(
        { userId: user.id, email: user.email, role: user.role },
        JWT_SECRET,
        { expiresIn: '24h', algorithm: 'HS512' }
      );

      res.json({
        token,
        user: {
          id: user.id,
          email: user.email,
          role: user.role,
          firstName: user.first_name,
          lastName: user.last_name
        }
      });
    } catch (error) {
      console.error('Login error:', error);
      res.status(500).json({ error: 'Internal server error' });
    }
  });

  // Get current user
  app.get('/api/users/me', async (req, res) => {
    try {
      const token = req.headers.authorization?.replace('Bearer ', '');
      if (!token) {
        return res.status(401).json({ error: 'No token provided' });
      }

      const decoded = jwt.verify(token, JWT_SECRET);
      const query = 'SELECT id, email, role, first_name, last_name FROM users WHERE id = $1';
      const result = await dbPool.query(query, [decoded.userId]);

      if (result.rows.length === 0) {
        return res.status(404).json({ error: 'User not found' });
      }

      const user = result.rows[0];
      res.json({
        id: user.id,
        email: user.email,
        role: user.role,
        firstName: user.first_name,
        lastName: user.last_name
      });
    } catch (error) {
      console.error('Get user error:', error);
      res.status(401).json({ error: 'Invalid token' });
    }
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
