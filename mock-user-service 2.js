const express = require('express');
const { Pool } = require('pg');
const bcrypt = require('bcryptjs');
const CircuitBreaker = require('opossum');
const app = express();
const port = 8082;

// Database configuration with connection pooling
const dbPool = new Pool({
  host: 'localhost',
  port: 5432,
  database: 'Admisión_MTN_DB',
  user: 'admin',
  password: 'admin123',
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
    console.error(`⚠️ [Circuit Breaker ${name}] OPEN - Too many failures in user service`);
  });

  breaker.on('halfOpen', () => {
    console.warn(`🔄 [Circuit Breaker ${name}] HALF-OPEN - Testing recovery`);
  });

  breaker.on('close', () => {
    console.log(`✅ [Circuit Breaker ${name}] CLOSED - User service recovered`);
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
    console.log(`[Cache] Cleaned ${cleaned} expired entries`);
  }
}, 300000);

app.use(express.json());

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

    // Transform to match frontend format
    const users = result.rows.map(user => ({
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
      success: true,
      data: users,
      count: users.length
    });
  } catch (error) {
    console.error('❌ Error fetching users:', error);
    res.status(500).json({
      success: false,
      error: 'Error al obtener usuarios',
      details: error.message
    });
  }
});

// Public login endpoint - consulta la base de datos PostgreSQL
app.post('/api/auth/login', async (req, res) => {
  const { email, password } = req.body;
  
  // Logging removed for security - console.log('🔐 LOGIN ATTEMPT:', { email, password: password ? '[PROTECTED]' : '[EMPTY]' });
  
  if (!email || !password) {
    // Logging removed for security - console.log('❌ Missing email or password');
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
      // Logging removed for security - console.log('❌ User not found:', email);
      return res.status(401).json({
        success: false,
        error: 'Credenciales inválidas'
      });
    }

    const user = userQuery.rows[0];
    // Logging removed for security - console.log('👤 User found:', { id: user.id, email: user.email, role: user.role, active: user.active });

    // Verificar si el usuario está activo
    if (!user.active) {
      // Logging removed for security - console.log('❌ User is inactive:', email);
      return res.status(401).json({
        success: false,
        error: 'Usuario inactivo'
      });
    }

    // Verificar contraseña usando bcrypt
    const isValidPassword = await bcrypt.compare(password, user.password);
    // Logging removed for security - console.log('🔒 Password verification:', isValidPassword ? 'SUCCESS' : 'FAILED');

    if (!isValidPassword) {
      // Logging removed for security - console.log('❌ Invalid password for user:', email);
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
    
    // Logging removed for security - console.log('✅ Login successful for user:', { id: user.id, email: user.email, role: user.role });
    
    res.json({
      success: true,
      message: 'Login exitoso',
      token: token,
      id: user.id.toString(),
      firstName: user.first_name,
      lastName: user.last_name,
      email: user.email,
      role: user.role,
      subject: user.subject
    });

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

// Public register endpoint for new APODERADO users
app.post('/api/auth/register', async (req, res) => {
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
        // Logging removed for security - console.log(`🗑️ Removed existing test user: ${email}`);
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
    // Logging removed for security - console.log('✅ New user saved to database:', newUser.email);
    
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
      role: newUser.role,
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
        role: newUser.role,
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
app.put('/api/users/:id', authenticateToken, async (req, res) => {
  const userId = parseInt(req.params.id);
  const userIndex = users.findIndex(u => u.id === userId);

  if (userIndex === -1) {
    return res.status(404).json({
      success: false,
      error: 'User not found'
    });
  }

  const client = await dbPool.connect();
  try {
    console.log('📝 Datos recibidos para actualizar usuario:', JSON.stringify(req.body, null, 2));

    // Get current password from database
    const currentUserQuery = await client.query(
      'SELECT password FROM users WHERE id = $1',
      [userId]
    );

    let hashedPassword = currentUserQuery.rows[0].password; // Keep current password by default

    // Only hash and update password if a new one is provided
    if (req.body.password && req.body.password.trim() !== '') {
      hashedPassword = await bcrypt.hash(req.body.password, 10);
    }

    // Update user data in memory
    const updatedUser = {
      ...users[userIndex],
      ...req.body,
      id: userId, // Preserve ID
      password: hashedPassword, // Use hashed password (current or new)
      fullName: req.body.firstName && req.body.lastName ?
                `${req.body.firstName} ${req.body.lastName}`.trim() :
                users[userIndex].fullName,
      updatedAt: new Date().toISOString()
    };

    users[userIndex] = updatedUser;

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

    console.log(`✅ Usuario actualizado en base de datos: ${updatedUser.firstName} ${updatedUser.lastName}`);

    res.json({
      success: true,
      message: 'User updated successfully',
      data: updatedUser
    });

  } catch (error) {
    console.error('❌ Error actualizando usuario:', error);
    res.status(500).json({
      success: false,
      error: 'Error updating user in database'
    });
  } finally {
    client.release();
  }
});

// Delete user by ID
app.delete('/api/users/:id', authenticateToken, (req, res) => {
  const userId = parseInt(req.params.id);
  const userIndex = users.findIndex(u => u.id === userId);
  
  if (userIndex === -1) {
    return res.status(404).json({
      success: false,
      error: 'User not found'
    });
  }
  
  // Remove user from array
  users.splice(userIndex, 1);
  
  res.json({
    success: true,
    message: 'User deleted successfully'
  });
});

// Create new user
app.post('/api/users', authenticateToken, async (req, res) => {
  try {
    // Get the highest ID to generate new one
    const maxId = Math.max(...users.map(u => u.id));
    const newId = maxId + 1;

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

    // Add to users array (for compatibility with mock data)
    users.push(newUser);

    // ALSO save to database for persistent storage and foreign key constraints
    let userSavedToDB = false;
    const client = await dbPool.connect();
    try {
      const insertUserQuery = `
        INSERT INTO users (id, first_name, last_name, email, password, role, active, email_verified, rut, phone, created_at, updated_at)
        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, NOW(), NOW())
        ON CONFLICT (id) DO UPDATE SET
          first_name = EXCLUDED.first_name,
          last_name = EXCLUDED.last_name,
          email = EXCLUDED.email,
          password = EXCLUDED.password,
          role = EXCLUDED.role,
          active = EXCLUDED.active,
          email_verified = EXCLUDED.email_verified,
          rut = EXCLUDED.rut,
          phone = EXCLUDED.phone,
          updated_at = NOW()
      `;

      await client.query(insertUserQuery, [
        newUser.id,
        newUser.firstName,
        newUser.lastName,
        newUser.email,
        newUser.password,
        newUser.role,
        newUser.active,
        newUser.emailVerified,
        newUser.rut,
        newUser.phone
      ]);

      console.log(`✅ Usuario ${newUser.id} guardado en base de datos`);
      userSavedToDB = true;

    } catch (dbError) {
      console.error('❌ Error guardando usuario en base de datos:', dbError);
      // Don't create schedules if user wasn't saved to database
    } finally {
      client.release();
    }

    // Create schedules for evaluator roles ONLY if user was successfully saved to database
    const evaluatorRoles = ['TEACHER_LANGUAGE', 'TEACHER_MATHEMATICS', 'TEACHER_ENGLISH', 'TEACHER', 'COORDINATOR', 'CYCLE_DIRECTOR', 'PSYCHOLOGIST', 'ADMIN'];
    if (userSavedToDB && evaluatorRoles.includes(newUser.role)) {
      try {
        if (req.body.customSchedules && Array.isArray(req.body.customSchedules) && req.body.customSchedules.length > 0) {
          console.log(`🗓️ Creando horarios personalizados para ${newUser.role}: ${newUser.fullName}`);
          await createCustomSchedulesForUser(newUser.id, req.body.customSchedules);
        } else {
          console.log(`🗓️ Creando horarios por defecto para ${newUser.role}: ${newUser.fullName}`);
          await createDefaultScheduleForUser(newUser.id);
        }
      } catch (scheduleError) {
        console.error('❌ Error creando horarios para usuario:', scheduleError);
        // Don't fail user creation if schedules fail
      }
    } else if (!userSavedToDB && evaluatorRoles.includes(newUser.role)) {
      console.log(`⚠️ No se crearon horarios para ${newUser.role}: ${newUser.fullName} - usuario no guardado en base de datos`);
    }

    res.status(201).json({
      success: true,
      message: `User created successfully${evaluatorRoles.includes(newUser.role) ? ' with default schedule' : ''}`,
      data: newUser
    });

  } catch (error) {
    console.error('❌ Error creating user:', error);
    res.status(500).json({
      success: false,
      error: 'Error interno del servidor al crear usuario'
    });
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

    console.log(`✅ Horarios por defecto creados para usuario ${userId}`);

  } catch (error) {
    console.error('❌ Error creando horarios por defecto:', error);
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

    console.log(`🗓️ Creando ${customSchedules.length} horarios personalizados para usuario ${userId}`);

    for (const schedule of customSchedules) {
      // Validate required fields
      if (!schedule.dayOfWeek || !schedule.startTime || !schedule.endTime) {
        console.warn('⚠️ Horario inválido (faltan campos requeridos):', schedule);
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

      console.log(`✅ Horario creado: ${schedule.dayOfWeek} ${startTime}-${endTime}`);
    }

    console.log(`✅ Todos los horarios personalizados creados para usuario ${userId}`);

  } catch (error) {
    console.error('❌ Error creando horarios personalizados:', error);
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

    // Return user profile without sensitive data
    res.json({
      success: true,
      user: {
        id: user.id,
        firstName: user.first_name,
        lastName: user.last_name,
        fullName: `${user.first_name} ${user.last_name}`,
        email: user.email,
        role: user.role,
        phone: user.phone,
        rut: user.rut,
        subject: user.subject,
        educationalLevel: null,
        active: user.active,
        emailVerified: user.email_verified,
        createdAt: user.created_at
      }
    });
  } catch (error) {
    console.error('Error fetching user profile:', error);
    res.status(500).json({
      success: false,
      error: 'Error al obtener perfil de usuario'
    });
  } finally {
    client.release();
  }
});

// ============= MISSING ENDPOINTS REQUIRED BY FRONTEND =============

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

// Get all roles endpoint - required by userService.ts:180
app.get('/api/users/roles', authenticateToken, (req, res) => {
  // Check cache first
  const cacheKey = 'users:roles';
  const cached = userCache.get(cacheKey);
  if (cached) {
    console.log('[Cache HIT] users:roles');
    return res.json(cached);
  }
  console.log('[Cache MISS] users:roles');

  const roles = [
    'ADMIN',
    'TEACHER',
    'COORDINATOR',
    'CYCLE_DIRECTOR',
    'PSYCHOLOGIST',
    'APODERADO'
  ];

  // Cache for 30 minutes
  userCache.set(cacheKey, roles, 1800000);
  res.json(roles);
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
    console.log('[Cache HIT] users:school-staff');
    return res.json(cached);
  }
  console.log('[Cache MISS] users:school-staff');

  const client = await dbPool.connect();
  try {
    console.log('🔍 Public: Fetching real school staff from database...');

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

    console.log(`✅ Found ${schoolStaff.length} real school staff members`);

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
    console.error('❌ Error fetching real school staff:', error);
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
  console.log(`User Service running on port ${port}`);
  console.log('✅ Connection pooling enabled');
  console.log('✅ In-memory cache enabled');
  console.log('Cache endpoints:');
  console.log('  - POST /api/users/cache/clear');
  console.log('  - GET  /api/users/cache/stats');
});