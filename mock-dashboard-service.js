const express = require('express');
const { Pool } = require('pg');
const CircuitBreaker = require('opossum');
const { translateToSpanish } = require('./translations');
const app = express();
const port = 8086;

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
// 5 circuit breaker categories based on query complexity and criticality

// 1. Simple Queries (2s, 60% threshold, 20s reset) - Fast, lightweight queries
const simpleQueryBreakerOptions = {
  timeout: 2000,
  errorThresholdPercentage: 60,
  resetTimeout: 20000,
  rollingCountTimeout: 10000,
  rollingCountBuckets: 10,
  name: 'DashboardSimpleQueryBreaker'
};

// 2. Medium Queries (5s, 50% threshold, 30s reset) - Standard queries with joins
const mediumQueryBreakerOptions = {
  timeout: 5000,
  errorThresholdPercentage: 50,
  resetTimeout: 30000,
  rollingCountTimeout: 10000,
  rollingCountBuckets: 10,
  name: 'DashboardMediumQueryBreaker'
};

// 3. Heavy Queries (10s, 40% threshold, 60s reset) - Complex aggregations
const heavyQueryBreakerOptions = {
  timeout: 10000,
  errorThresholdPercentage: 40,
  resetTimeout: 60000,
  rollingCountTimeout: 15000,
  rollingCountBuckets: 10,
  name: 'DashboardHeavyQueryBreaker'
};

// 4. Write Operations (3s, 30% threshold, 45s reset) - Critical data mutations
const writeOperationBreakerOptions = {
  timeout: 3000,
  errorThresholdPercentage: 30,
  resetTimeout: 45000,
  rollingCountTimeout: 10000,
  rollingCountBuckets: 10,
  name: 'DashboardWriteBreaker'
};

// 5. External Service Calls (8s, 70% threshold, 120s reset) - SMTP, S3, APIs
const externalServiceBreakerOptions = {
  timeout: 8000,
  errorThresholdPercentage: 70,
  resetTimeout: 120000,
  rollingCountTimeout: 20000,
  rollingCountBuckets: 10,
  name: 'DashboardExternalBreaker'
};

// Create circuit breakers for each category
const simpleQueryBreaker = new CircuitBreaker(
  async (client, query, params) => await client.query(query, params),
  simpleQueryBreakerOptions
);

const mediumQueryBreaker = new CircuitBreaker(
  async (client, query, params) => await client.query(query, params),
  mediumQueryBreakerOptions
);

const heavyQueryBreaker = new CircuitBreaker(
  async (client, query, params) => await client.query(query, params),
  heavyQueryBreakerOptions
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
    console.error(`⚠️ [Circuit Breaker ${name}] OPEN - Too many failures in dashboard service`);
  });

  breaker.on('halfOpen', () => {
    console.warn(`🔄 [Circuit Breaker ${name}] HALF-OPEN - Testing recovery`);
  });

  breaker.on('close', () => {
    console.log(`✅ [Circuit Breaker ${name}] CLOSED - Dashboard service recovered`);
  });

  breaker.fallback(() => {
    throw new Error(`Service temporarily unavailable - ${name} circuit breaker open`);
  });
};

// Setup events for all breakers
setupBreakerEvents(simpleQueryBreaker, 'Simple');
setupBreakerEvents(mediumQueryBreaker, 'Medium');
setupBreakerEvents(heavyQueryBreaker, 'Heavy');
setupBreakerEvents(writeOperationBreaker, 'Write');
setupBreakerEvents(externalServiceBreaker, 'External');

// Legacy breaker for backward compatibility (maps to medium query breaker)
const queryWithCircuitBreaker = mediumQueryBreaker;

// Simple in-memory cache with TTL
class SimpleCache {
  constructor() {
    this.cache = new Map();
    this.stats = { hits: 0, misses: 0 };
  }

  set(key, value, ttlMs = 300000) { // Default 5 minutes
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
const dashboardCache = new SimpleCache();

// Periodic cleanup of expired entries (every 5 minutes)
setInterval(() => {
  const now = Date.now();
  let cleaned = 0;
  for (const [key, item] of dashboardCache.cache.entries()) {
    if (now > item.expiresAt) {
      dashboardCache.cache.delete(key);
      cleaned++;
    }
  }
  if (cleaned > 0) {
    console.log(`[Cache] Cleaned ${cleaned} expired entries`);
  }
}, 300000);

// Middleware
app.use(express.json());
// CORS is handled by NGINX gateway - commented to avoid duplicate headers
// app.use((req, res, next) => {
//   res.header('Access-Control-Allow-Origin', '*');
//   res.header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
//   res.header('Access-Control-Allow-Headers', 'Origin, X-Requested-With, Content-Type, Accept, Authorization, Cache-Control');
//   if (req.method === 'OPTIONS') {
//     res.sendStatus(200);
//   } else {
//     next();
//   }
// });

// Mock data
const dashboardData = {
  statistics: {
    totalUsers: 1247,
    totalApplications: 589,
    pendingApplications: 143,
    approvedApplications: 298,
    rejectedApplications: 76,
    scheduledInterviews: 45,
    completedInterviews: 178,
    activeEvaluators: 12,
    totalNotifications: 2341,
    systemHealth: "excellent"
  },
  recentActivities: [
    {
      id: 1,
      type: "application",
      description: "Nueva solicitud de admisión recibida",
      studentName: "María González",
      timestamp: "2025-09-05T10:30:00Z",
      status: "pending"
    },
    {
      id: 2,
      type: "interview",
      description: "Entrevista programada",
      studentName: "Pedro Sánchez",
      evaluatorName: "Dr. Rodriguez",
      timestamp: "2025-09-05T09:15:00Z",
      status: "scheduled"
    },
    {
      id: 3,
      type: "evaluation",
      description: "Evaluación completada",
      studentName: "Ana Torres",
      evaluatorName: "Psic. López",
      timestamp: "2025-09-05T08:45:00Z",
      status: "completed",
      score: 85
    },
    {
      id: 4,
      type: "notification",
      description: "Correo de confirmación enviado",
      recipient: "apoderado@gmail.com",
      timestamp: "2025-09-05T08:00:00Z",
      status: "sent"
    },
    {
      id: 5,
      type: "user",
      description: "Nuevo evaluador registrado",
      userName: "Dra. Silva",
      role: "PSYCHOLOGIST",
      timestamp: "2025-09-04T16:30:00Z",
      status: "active"
    }
  ],
  upcomingInterviews: [
    {
      id: 1,
      studentName: "Carlos Mendoza",
      evaluatorName: "Prof. Martínez",
      scheduledDate: "2025-09-06",
      scheduledTime: "10:00",
      type: "ACADEMIC",
      mode: "IN_PERSON",
      status: "CONFIRMED"
    },
    {
      id: 2,
      studentName: "Lucía Ramírez",
      evaluatorName: "Psic. Castro",
      scheduledDate: "2025-09-06",
      scheduledTime: "14:30",
      type: "PSYCHOLOGICAL",
      mode: "VIRTUAL",
      status: "SCHEDULED"
    },
    {
      id: 3,
      studentName: "Diego Morales",
      evaluatorName: "Dr. Herrera",
      scheduledDate: "2025-09-07",
      scheduledTime: "09:00",
      type: "ACADEMIC",
      mode: "IN_PERSON",
      status: "SCHEDULED"
    }
  ],
  pendingTasks: [
    {
      id: 1,
      type: "review_application",
      title: "Revisar solicitud de admisión",
      description: "Solicitud de Juan Pérez requiere revisión urgente",
      priority: "high",
      dueDate: "2025-09-06",
      assignedTo: "Coordinador Académico"
    },
    {
      id: 2,
      type: "schedule_interview",
      title: "Programar entrevistas pendientes",
      description: "15 estudiantes esperan programación de entrevista",
      priority: "medium",
      dueDate: "2025-09-08",
      assignedTo: "Administrador"
    },
    {
      id: 3,
      type: "follow_up",
      title: "Seguimiento de documentos",
      description: "8 aplicaciones con documentos faltantes",
      priority: "medium",
      dueDate: "2025-09-10",
      assignedTo: "Asistente Académico"
    }
  ],
  systemAlerts: [
    {
      id: 1,
      level: "warning",
      title: "Capacidad de entrevistas",
      message: "Se está alcanzando la capacidad máxima de entrevistas para la próxima semana",
      timestamp: "2025-09-05T07:00:00Z",
      action: "Considerar agregar más horarios disponibles"
    },
    {
      id: 2,
      level: "info",
      title: "Backup completado",
      message: "Backup automático de base de datos completado exitosamente",
      timestamp: "2025-09-05T02:00:00Z",
      action: null
    }
  ],
  performanceMetrics: {
    applicationProcessingTime: "2.3 días promedio",
    interviewSchedulingRate: "87%",
    documentCompletionRate: "92%",
    evaluatorSatisfaction: "4.2/5",
    systemUptime: "99.8%",
    responseTime: "0.4s promedio"
  }
};

// Dashboard endpoints
app.get('/api/dashboard/complete', (req, res) => {
  // Request logging removed for security
  res.json({
    success: true,
    data: dashboardData,
    timestamp: new Date().toISOString(),
    service: "dashboard-mock"
  });
});

app.get('/api/dashboard/statistics', (req, res) => {
  // Request logging removed for security
  res.json({
    success: true,
    data: dashboardData.statistics,
    timestamp: new Date().toISOString()
  });
});

app.get('/api/dashboard/stats', async (req, res) => {
  // Check cache first
  const cacheKey = 'dashboard:stats:general';
  const cached = dashboardCache.get(cacheKey);
  if (cached) {
    console.log('[Cache HIT] dashboard:stats:general');
    return res.json(cached);
  }
  console.log('[Cache MISS] dashboard:stats:general');

  const client = await dbPool.connect();
  try {
    // Get real statistics from database
    const applicationStats = await queryWithCircuitBreaker.fire(client, `
      SELECT
        COUNT(*) as total_applications,
        COUNT(CASE WHEN status = 'PENDING' THEN 1 END) as pending_applications,
        COUNT(CASE WHEN status = 'UNDER_REVIEW' THEN 1 END) as under_review,
        COUNT(CASE WHEN status = 'APPROVED' THEN 1 END) as approved_applications,
        COUNT(CASE WHEN status = 'REJECTED' THEN 1 END) as rejected_applications
      FROM applications
    `, []);

    const interviewStats = await queryWithCircuitBreaker.fire(client, `
      SELECT
        COUNT(*) as total_interviews,
        COUNT(CASE WHEN status = 'SCHEDULED' THEN 1 END) as scheduled_interviews,
        COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed_interviews,
        COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) as cancelled_interviews
      FROM interviews
    `, []);

    const userStats = await queryWithCircuitBreaker.fire(client, `
      SELECT COUNT(*) as total_users FROM users
    `, []);

    const evaluatorStats = await queryWithCircuitBreaker.fire(client, `
      SELECT COUNT(DISTINCT evaluator_id) as active_evaluators
      FROM evaluations
      WHERE evaluator_id IS NOT NULL
    `, []);

    const notificationStats = await queryWithCircuitBreaker.fire(client, `
      SELECT COUNT(*) as total_notifications FROM email_notifications
    `, []);

    const stats = applicationStats.rows[0];
    const interviewData = interviewStats.rows[0];
    const userData = userStats.rows[0];
    const evaluatorData = evaluatorStats.rows[0];
    const notificationData = notificationStats.rows[0];

    const realStats = {
      totalUsers: parseInt(userData.total_users),
      totalApplications: parseInt(stats.total_applications),
      pendingApplications: parseInt(stats.pending_applications),
      approvedApplications: parseInt(stats.approved_applications),
      rejectedApplications: parseInt(stats.rejected_applications),
      scheduledInterviews: parseInt(interviewData.scheduled_interviews),
      completedInterviews: parseInt(interviewData.completed_interviews),
      activeEvaluators: parseInt(evaluatorData?.active_evaluators || 0),
      totalNotifications: parseInt(notificationData?.total_notifications || 0),
      systemHealth: "excellent"
    };

    const response = {
      success: true,
      data: realStats,
      timestamp: new Date().toISOString()
    };

    // Cache for 5 minutes
    dashboardCache.set(cacheKey, response, 300000);
    res.json(response);
  } catch (error) {
    // Check if circuit breaker is open
    if (error.message && error.message.includes('breaker')) {
      console.error('⚠️ [Circuit Breaker OPEN] Dashboard stats unavailable');
      return res.status(503).json({
        success: false,
        error: 'Service temporarily unavailable - circuit breaker open',
        code: 'CIRCUIT_BREAKER_OPEN',
        message: 'El servicio está temporalmente sobrecargado. Por favor, intenta nuevamente en unos minutos.',
        retryAfter: 30 // seconds
      });
    }

    // Other database errors - fallback to mock data
    console.error('⚠️ [Database Error] Falling back to mock data');
    res.json({
      success: true,
      data: dashboardData.statistics,
      timestamp: new Date().toISOString(),
      fallback: true
    });
  } finally {
    client.release();
  }
});

app.get('/api/dashboard/recent-activity', (req, res) => {
  // Logging removed for security - console.log('🕐 Recent activity request received');
  res.json({
    success: true,
    data: dashboardData.recentActivities,
    timestamp: new Date().toISOString()
  });
});

app.get('/api/dashboard/upcoming-interviews', async (req, res) => {
  // Logging removed for security - console.log('📅 Upcoming interviews request received');
  const client = await dbPool.connect();
  try {
    // Get real upcoming interviews from database
    const upcomingInterviews = await client.query(`
      SELECT
        i.id,
        i.scheduled_date,
        i.duration_minutes,
        i.status,
        s.first_name || ' ' || s.paternal_last_name as student_name,
        u.first_name || ' ' || u.last_name as evaluator_name,
        'ACADEMIC' as type,
        'IN_PERSON' as mode
      FROM interviews i
      JOIN applications a ON a.id = i.application_id
      JOIN students s ON s.id = a.student_id
      JOIN users u ON u.id = i.interviewer_id
      WHERE i.scheduled_date >= NOW()
      ORDER BY i.scheduled_date
      LIMIT 10
    `);

    const formattedInterviews = upcomingInterviews.rows.map(interview => ({
      id: interview.id,
      studentName: interview.student_name,
      evaluatorName: interview.evaluator_name,
      scheduledDate: interview.scheduled_date.toISOString().split('T')[0],
      scheduledTime: interview.scheduled_date.toISOString().split('T')[1].substring(0, 5),
      type: translateToSpanish(interview.type, 'interview_type'),
      mode: interview.mode,
      status: translateToSpanish(interview.status, 'interview_status')
    }));

    res.json({
      success: true,
      data: formattedInterviews,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    // Database error logging removed for security
    // Fallback to mock data
    res.json({
      success: true,
      data: dashboardData.upcomingInterviews,
      timestamp: new Date().toISOString()
    });
  } finally {
    client.release();
  }
});

app.get('/api/dashboard/pending-tasks', (req, res) => {
  // Logging removed for security - console.log('✅ Pending tasks request received');
  res.json({
    success: true,
    data: dashboardData.pendingTasks,
    timestamp: new Date().toISOString()
  });
});

app.get('/api/dashboard/alerts', (req, res) => {
  // Logging removed for security - console.log('🚨 System alerts request received');
  res.json({
    success: true,
    data: dashboardData.systemAlerts,
    timestamp: new Date().toISOString()
  });
});

app.get('/api/dashboard/metrics', (req, res) => {
  // Logging removed for security - console.log('📊 Performance metrics request received');
  res.json({
    success: true,
    data: dashboardData.performanceMetrics,
    timestamp: new Date().toISOString()
  });
});

// Admin-specific dashboard endpoints
app.get('/api/dashboard/admin/stats', async (req, res) => {
  // Check cache first
  const cacheKey = 'dashboard:stats:admin';
  const cached = dashboardCache.get(cacheKey);
  if (cached) {
    console.log('[Cache HIT] dashboard:stats:admin');
    return res.json(cached);
  }
  console.log('[Cache MISS] dashboard:stats:admin');

  const client = await dbPool.connect();
  try {
    // Get real statistics from database
    const applicationStats = await queryWithCircuitBreaker.fire(client, `
      SELECT
        COUNT(*) as total_applications,
        COUNT(CASE WHEN status = 'PENDING' THEN 1 END) as pending_applications,
        COUNT(CASE WHEN status = 'UNDER_REVIEW' THEN 1 END) as under_review,
        COUNT(CASE WHEN status = 'APPROVED' THEN 1 END) as approved_applications,
        COUNT(CASE WHEN status = 'REJECTED' THEN 1 END) as rejected_applications
      FROM applications
    `, []);

    const interviewStats = await queryWithCircuitBreaker.fire(client, `
      SELECT
        COUNT(*) as total_interviews,
        COUNT(CASE WHEN status = 'SCHEDULED' THEN 1 END) as scheduled_interviews,
        COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed_interviews,
        COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) as cancelled_interviews
      FROM interviews
    `, []);

    const userStats = await queryWithCircuitBreaker.fire(client, `
      SELECT COUNT(*) as total_users FROM users
    `, []);

    const evaluatorStats = await queryWithCircuitBreaker.fire(client, `
      SELECT COUNT(DISTINCT evaluator_id) as active_evaluators
      FROM evaluations
      WHERE evaluator_id IS NOT NULL
    `, []);

    const notificationStats = await queryWithCircuitBreaker.fire(client, `
      SELECT COUNT(*) as total_notifications FROM email_notifications
    `, []);

    const stats = applicationStats.rows[0];
    const interviewData = interviewStats.rows[0];
    const userData = userStats.rows[0];
    const evaluatorData = evaluatorStats.rows[0];
    const notificationData = notificationStats.rows[0];

    const realStats = {
      totalUsers: parseInt(userData.total_users),
      totalApplications: parseInt(stats.total_applications),
      pendingApplications: parseInt(stats.pending_applications),
      approvedApplications: parseInt(stats.approved_applications),
      rejectedApplications: parseInt(stats.rejected_applications),
      scheduledInterviews: parseInt(interviewData.scheduled_interviews),
      completedInterviews: parseInt(interviewData.completed_interviews),
      activeEvaluators: parseInt(evaluatorData?.active_evaluators || 0),
      totalNotifications: parseInt(notificationData?.total_notifications || 0),
      systemHealth: "excellent"
    };

    const response = {
      success: true,
      data: realStats,
      timestamp: new Date().toISOString()
    };

    // Cache for 3 minutes
    dashboardCache.set(cacheKey, response, 180000);
    res.json(response);
  } catch (error) {
    // Check if circuit breaker is open
    if (error.message && error.message.includes('breaker')) {
      console.error('⚠️ [Circuit Breaker OPEN] Admin stats unavailable');
      return res.status(503).json({
        success: false,
        error: 'Service temporarily unavailable - circuit breaker open',
        code: 'CIRCUIT_BREAKER_OPEN',
        message: 'El servicio está temporalmente sobrecargado. Por favor, intenta nuevamente en unos minutos.',
        retryAfter: 30 // seconds
      });
    }

    // Other database errors - fallback to mock data
    console.error('⚠️ [Database Error] Falling back to mock data');
    res.json({
      success: true,
      data: dashboardData.statistics,
      timestamp: new Date().toISOString(),
      fallback: true
    });
  } finally {
    client.release();
  }
});

// Teacher-specific dashboard endpoints
app.get('/api/dashboard/teacher/stats', async (req, res) => {
  // Logging removed for security - console.log('👩‍🏫 Teacher dashboard stats request received');
  const client = await dbPool.connect();
  try {
    // Get teacher-specific statistics from database
    const interviewStats = await client.query(`
      SELECT
        COUNT(*) as total_interviews,
        COUNT(CASE WHEN status = 'SCHEDULED' THEN 1 END) as scheduled_interviews,
        COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed_interviews
      FROM interviews
    `);

    const evaluationStats = await client.query(`
      SELECT COUNT(*) as total_evaluations FROM evaluations
    `);

    const interviewData = interviewStats.rows[0];
    const evaluationData = evaluationStats.rows[0];

    const teacherStats = {
      totalInterviews: parseInt(interviewData.total_interviews),
      scheduledInterviews: parseInt(interviewData.scheduled_interviews),
      completedInterviews: parseInt(interviewData.completed_interviews),
      totalEvaluations: parseInt(evaluationData.total_evaluations),
      pendingEvaluations: 5, // Mock for now
      completedEvaluations: parseInt(evaluationData.total_evaluations) - 5,
      averageScore: 7.2, // Mock for now
      systemHealth: "excellent"
    };

    res.json({
      success: true,
      data: teacherStats,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    // Database error logging removed for security
    // Fallback to mock data
    res.json({
      success: true,
      data: {
        totalInterviews: 45,
        scheduledInterviews: 12,
        completedInterviews: 33,
        totalEvaluations: 89,
        pendingEvaluations: 8,
        completedEvaluations: 81,
        averageScore: 7.2,
        systemHealth: "excellent"
      },
      timestamp: new Date().toISOString()
    });
  } finally {
    client.release();
  }
});

// Analytics endpoints (to match frontend expectations)
app.get('/api/analytics/dashboard-metrics', async (req, res) => {
  // Check cache first
  const cacheKey = 'analytics:dashboard:metrics';
  const cached = dashboardCache.get(cacheKey);
  if (cached) {
    console.log('[Cache HIT] analytics:dashboard:metrics');
    return res.json(cached);
  }
  console.log('[Cache MISS] analytics:dashboard:metrics');

  const client = await dbPool.connect();
  try {
    // Heavy query breaker - complex aggregations across multiple tables
    const applicationStats = await heavyQueryBreaker.fire(client, `
      SELECT
        COUNT(*) as total_applications,
        COUNT(CASE WHEN status = 'PENDING' THEN 1 END) as pending_applications,
        COUNT(CASE WHEN status = 'UNDER_REVIEW' THEN 1 END) as under_review,
        COUNT(CASE WHEN status = 'APPROVED' THEN 1 END) as approved_applications,
        COUNT(CASE WHEN status = 'REJECTED' THEN 1 END) as rejected_applications,
        COUNT(CASE WHEN created_at >= date_trunc('month', CURRENT_DATE) THEN 1 END) as applications_this_month
      FROM applications
    `, []);

    const userStats = await heavyQueryBreaker.fire(client, `
      SELECT
        COUNT(*) as total_users,
        COUNT(CASE WHEN role IN ('TEACHER', 'COORDINATOR', 'PSYCHOLOGIST', 'CYCLE_DIRECTOR') THEN 1 END) as active_evaluators
      FROM users
    `, []);

    const stats = applicationStats.rows[0];
    const userData = userStats.rows[0];

    const conversionRate = parseInt(stats.total_applications) > 0
      ? (parseInt(stats.approved_applications) / parseInt(stats.total_applications)) * 100
      : 0;

    const dashboardMetrics = {
      totalApplications: parseInt(stats.total_applications),
      applicationsThisMonth: parseInt(stats.applications_this_month),
      conversionRate: Math.round(conversionRate * 100) / 100,
      acceptedApplications: parseInt(stats.approved_applications),
      averageCompletionDays: 3.2, // Mock value
      activeEvaluators: parseInt(userData.active_evaluators),
      totalActiveUsers: parseInt(userData.total_users)
    };

    // Cache for 5 minutes
    dashboardCache.set(cacheKey, dashboardMetrics, 300000);
    res.json(dashboardMetrics);
  } catch (error) {
    // Fallback to mock data
    res.json({
      totalApplications: 589,
      applicationsThisMonth: 42,
      conversionRate: 78.5,
      acceptedApplications: 298,
      averageCompletionDays: 3.2,
      activeEvaluators: 12,
      totalActiveUsers: 47
    });
  } finally {
    client.release();
  }
});

app.get('/api/analytics/status-distribution', async (req, res) => {
  // Check cache first
  const cacheKey = 'analytics:status:distribution';
  const cached = dashboardCache.get(cacheKey);
  if (cached) {
    console.log('[Cache HIT] analytics:status:distribution');
    return res.json(cached);
  }
  console.log('[Cache MISS] analytics:status:distribution');

  const client = await dbPool.connect();
  try {
    const stats = await client.query(`
      SELECT
        status,
        COUNT(*) as count
      FROM applications
      GROUP BY status
    `);

    const totalApplications = stats.rows.reduce((sum, row) => sum + parseInt(row.count), 0);
    const statusCount = {};
    const statusPercentages = {};

    stats.rows.forEach(row => {
      const translatedStatus = translateToSpanish(row.status, 'application_status');
      statusCount[translatedStatus] = parseInt(row.count);
      statusPercentages[translatedStatus] = totalApplications > 0
        ? Math.round((parseInt(row.count) / totalApplications) * 100 * 100) / 100
        : 0;
    });

    const distribution = {
      statusCount,
      statusPercentages,
      totalApplications
    };

    // Cache for 10 minutes (rarely changes)
    dashboardCache.set(cacheKey, distribution, 600000);
    res.json(distribution);
  } catch (error) {
    // Fallback to mock data
    res.json({
      statusCount: {
        'PENDING': 143,
        'UNDER_REVIEW': 72,
        'APPROVED': 298,
        'REJECTED': 76
      },
      statusPercentages: {
        'PENDING': 24.2,
        'UNDER_REVIEW': 12.2,
        'APPROVED': 50.6,
        'REJECTED': 12.9
      },
      totalApplications: 589
    });
  } finally {
    client.release();
  }
});

app.get('/api/analytics/grade-distribution', async (req, res) => {
  const client = await dbPool.connect();
  try {
    const stats = await client.query(`
      SELECT
        s.grade_applied as grade,
        COUNT(*) as count
      FROM applications a
      JOIN students s ON s.id = a.student_id
      GROUP BY s.grade_applied
    `);

    const totalApplications = stats.rows.reduce((sum, row) => sum + parseInt(row.count), 0);
    const gradeCount = {};
    const gradePercentages = {};

    stats.rows.forEach(row => {
      const grade = row.grade || 'Sin especificar';
      gradeCount[grade] = parseInt(row.count);
      gradePercentages[grade] = totalApplications > 0
        ? Math.round((parseInt(row.count) / totalApplications) * 100 * 100) / 100
        : 0;
    });

    res.json({
      gradeCount,
      gradePercentages,
      totalApplications
    });
  } catch (error) {
    // Fallback to mock data
    res.json({
      gradeCount: {
        'PRE_KINDER': 89,
        'KINDER': 112,
        '1_BASICO': 95,
        '2_BASICO': 78,
        '3_BASICO': 68,
        '4_BASICO': 52,
        '5_BASICO': 45,
        '6_BASICO': 38,
        '7_BASICO': 32
      },
      gradePercentages: {
        'PRE_KINDER': 15.1,
        'KINDER': 19.0,
        '1_BASICO': 16.1,
        '2_BASICO': 13.2,
        '3_BASICO': 11.5,
        '4_BASICO': 8.8,
        '5_BASICO': 7.6,
        '6_BASICO': 6.4,
        '7_BASICO': 5.4
      },
      totalApplications: 589
    });
  } finally {
    client.release();
  }
});

app.get('/api/analytics/evaluator-analysis', async (req, res) => {
  const client = await dbPool.connect();
  try {
    const evaluatorStats = await client.query(`
      SELECT
        role,
        COUNT(*) as count
      FROM users
      WHERE role IN ('TEACHER', 'COORDINATOR', 'PSYCHOLOGIST', 'CYCLE_DIRECTOR', 'ADMIN')
      GROUP BY role
    `);

    const evaluatorsByRole = {};
    let totalEvaluators = 0;

    evaluatorStats.rows.forEach(row => {
      evaluatorsByRole[row.role] = parseInt(row.count);
      totalEvaluators += parseInt(row.count);
    });

    res.json({
      teacherLanguage: evaluatorsByRole['TEACHER'] || 0,
      teacherMathematics: evaluatorsByRole['TEACHER'] || 0,
      teacherEnglish: evaluatorsByRole['TEACHER'] || 0,
      psychologist: evaluatorsByRole['PSYCHOLOGIST'] || 0,
      cycleDirector: evaluatorsByRole['CYCLE_DIRECTOR'] || 0,
      admin: evaluatorsByRole['ADMIN'] || 0,
      totalEvaluators,
      evaluatorsByRole
    });
  } catch (error) {
    // Fallback to mock data
    res.json({
      teacherLanguage: 4,
      teacherMathematics: 3,
      teacherEnglish: 2,
      psychologist: 2,
      cycleDirector: 1,
      admin: 1,
      totalEvaluators: 13,
      evaluatorsByRole: {
        'TEACHER': 9,
        'PSYCHOLOGIST': 2,
        'CYCLE_DIRECTOR': 1,
        'ADMIN': 1
      }
    });
  } finally {
    client.release();
  }
});

app.get('/api/analytics/temporal-trends', async (req, res) => {
  // Check cache first
  const cacheKey = 'analytics:temporal:trends';
  const cached = dashboardCache.get(cacheKey);
  if (cached) {
    console.log('[Cache HIT] analytics:temporal:trends');
    return res.json(cached);
  }
  console.log('[Cache MISS] analytics:temporal:trends');

  const client = await dbPool.connect();
  try {
    const monthlyStats = await client.query(`
      SELECT
        TO_CHAR(created_at, 'YYYY-MM') as month,
        COUNT(*) as count
      FROM applications
      WHERE created_at >= NOW() - INTERVAL '12 months'
      GROUP BY TO_CHAR(created_at, 'YYYY-MM')
      ORDER BY month
    `);

    const monthlyApplications = {};
    monthlyStats.rows.forEach(row => {
      monthlyApplications[row.month] = parseInt(row.count);
    });

    const currentMonth = new Date().toISOString().substring(0, 7);
    const lastMonth = new Date(new Date().setMonth(new Date().getMonth() - 1)).toISOString().substring(0, 7);

    const currentMonthApplications = monthlyApplications[currentMonth] || 0;
    const lastMonthApplications = monthlyApplications[lastMonth] || 0;

    const monthlyGrowthRate = lastMonthApplications > 0
      ? ((currentMonthApplications - lastMonthApplications) / lastMonthApplications) * 100
      : 0;

    const trends = {
      monthlyApplications,
      currentMonthApplications,
      lastMonthApplications,
      monthlyGrowthRate: Math.round(monthlyGrowthRate * 100) / 100
    };

    // Cache for 15 minutes
    dashboardCache.set(cacheKey, trends, 900000);
    res.json(trends);
  } catch (error) {
    // Fallback to mock data
    res.json({
      monthlyApplications: {
        '2024-10': 45,
        '2024-11': 52,
        '2024-12': 38,
        '2025-01': 67,
        '2025-02': 73,
        '2025-03': 89,
        '2025-04': 95,
        '2025-05': 78,
        '2025-06': 82,
        '2025-07': 91,
        '2025-08': 88,
        '2025-09': 42
      },
      currentMonthApplications: 42,
      lastMonthApplications: 88,
      monthlyGrowthRate: -52.27
    });
  } finally {
    client.release();
  }
});

app.get('/api/analytics/performance-metrics', async (req, res) => {
  const client = await dbPool.connect();
  try {
    const completionStats = await client.query(`
      SELECT
        COUNT(CASE WHEN status = 'APPROVED' OR status = 'REJECTED' THEN 1 END) as completed,
        COUNT(CASE WHEN status = 'UNDER_REVIEW' THEN 1 END) as under_review,
        COUNT(CASE WHEN status = 'APPROVED' THEN 1 END) as finalized,
        COUNT(*) as total
      FROM applications
    `);

    const stats = completionStats.rows[0];
    const total = parseInt(stats.total);

    const completionRate = total > 0 ? (parseInt(stats.completed) / total) * 100 : 0;
    const underReviewRate = total > 0 ? (parseInt(stats.under_review) / total) * 100 : 0;
    const finalizationRate = total > 0 ? (parseInt(stats.finalized) / total) * 100 : 0;

    res.json({
      completionRate: Math.round(completionRate * 100) / 100,
      underReviewRate: Math.round(underReviewRate * 100) / 100,
      finalizationRate: Math.round(finalizationRate * 100) / 100,
      completedApplications: parseInt(stats.completed),
      underReviewApplications: parseInt(stats.under_review),
      finalizedApplications: parseInt(stats.finalized)
    });
  } catch (error) {
    // Fallback to mock data
    res.json({
      completionRate: 63.5,
      underReviewRate: 12.2,
      finalizationRate: 50.6,
      completedApplications: 374,
      underReviewApplications: 72,
      finalizedApplications: 298
    });
  } finally {
    client.release();
  }
});

app.get('/api/analytics/insights', (req, res) => {
  const recommendations = [
    {
      type: 'capacity',
      title: 'Capacidad de Entrevistas',
      message: 'Se recomienda agregar más horarios disponibles para entrevistas la próxima semana.',
      level: 'warning'
    },
    {
      type: 'performance',
      title: 'Tiempo de Procesamiento',
      message: 'El tiempo promedio de procesamiento ha mejorado en un 15% este mes.',
      level: 'success'
    },
    {
      type: 'trend',
      title: 'Tendencia de Aplicaciones',
      message: 'Se observa un incremento del 23% en aplicaciones comparado con el mes anterior.',
      level: 'info'
    }
  ];

  res.json({
    recommendations,
    totalInsights: recommendations.length
  });
});

app.get('/api/analytics/complete-analytics', async (req, res) => {
  try {
    // Get all analytics data in parallel
    const dashboardMetrics = await fetch(`http://localhost:${port}/api/analytics/dashboard-metrics`).then(r => r.json());
    const statusDistribution = await fetch(`http://localhost:${port}/api/analytics/status-distribution`).then(r => r.json());
    const gradeDistribution = await fetch(`http://localhost:${port}/api/analytics/grade-distribution`).then(r => r.json());
    const evaluatorAnalysis = await fetch(`http://localhost:${port}/api/analytics/evaluator-analysis`).then(r => r.json());
    const temporalTrends = await fetch(`http://localhost:${port}/api/analytics/temporal-trends`).then(r => r.json());
    const performanceMetrics = await fetch(`http://localhost:${port}/api/analytics/performance-metrics`).then(r => r.json());
    const insights = await fetch(`http://localhost:${port}/api/analytics/insights`).then(r => r.json());

    res.json({
      dashboardMetrics,
      statusDistribution,
      gradeDistribution,
      evaluatorAnalysis,
      temporalTrends,
      performanceMetrics,
      insights,
      generatedAt: new Date().toISOString()
    });
  } catch (error) {
    // If any internal fetch fails, return mock complete analytics
    res.json({
      dashboardMetrics: {
        totalApplications: 589,
        applicationsThisMonth: 42,
        conversionRate: 78.5,
        acceptedApplications: 298,
        averageCompletionDays: 3.2,
        activeEvaluators: 12,
        totalActiveUsers: 47
      },
      statusDistribution: {
        statusCount: { 'PENDING': 143, 'UNDER_REVIEW': 72, 'APPROVED': 298, 'REJECTED': 76 },
        statusPercentages: { 'PENDING': 24.2, 'UNDER_REVIEW': 12.2, 'APPROVED': 50.6, 'REJECTED': 12.9 },
        totalApplications: 589
      },
      gradeDistribution: {
        gradeCount: { 'PRE_KINDER': 89, 'KINDER': 112, '1_BASICO': 95 },
        gradePercentages: { 'PRE_KINDER': 15.1, 'KINDER': 19.0, '1_BASICO': 16.1 },
        totalApplications: 589
      },
      evaluatorAnalysis: {
        teacherLanguage: 4, teacherMathematics: 3, teacherEnglish: 2,
        psychologist: 2, cycleDirector: 1, admin: 1, totalEvaluators: 13,
        evaluatorsByRole: { 'TEACHER': 9, 'PSYCHOLOGIST': 2, 'CYCLE_DIRECTOR': 1, 'ADMIN': 1 }
      },
      temporalTrends: {
        monthlyApplications: { '2025-08': 88, '2025-09': 42 },
        currentMonthApplications: 42, lastMonthApplications: 88, monthlyGrowthRate: -52.27
      },
      performanceMetrics: {
        completionRate: 63.5, underReviewRate: 12.2, finalizationRate: 50.6,
        completedApplications: 374, underReviewApplications: 72, finalizedApplications: 298
      },
      insights: {
        recommendations: [
          { type: 'capacity', title: 'Capacidad de Entrevistas', message: 'Se recomienda agregar más horarios disponibles.', level: 'warning' }
        ],
        totalInsights: 1
      },
      generatedAt: new Date().toISOString()
    });
  }
});

// Cache management endpoints (admin only - add auth middleware in production)
app.post('/api/dashboard/cache/clear', (req, res) => {
  try {
    const { pattern } = req.body;
    const cleared = dashboardCache.clear(pattern);
    res.json({
      success: true,
      cleared,
      message: pattern ? `Cleared ${cleared} cache entries matching: ${pattern}` : `Cleared ${cleared} total cache entries`
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

app.get('/api/dashboard/cache/stats', (req, res) => {
  res.json({
    cacheStats: dashboardCache.getStats(),
    cacheSize: dashboardCache.size(),
    serviceUptime: process.uptime()
  });
});

// ============= HU-ADMIN-1: DETAILED DASHBOARD STATS =============
// Nuevo endpoint para estadísticas detalladas del dashboard de administración
app.get('/api/dashboard/admin/detailed-stats', async (req, res) => {
  const { academicYear } = req.query;
  const yearFilter = academicYear ? parseInt(academicYear) : new Date().getFullYear() + 1;

  // Check cache first
  const cacheKey = `dashboard:detailed-stats:${yearFilter}`;
  const cached = dashboardCache.get(cacheKey);
  if (cached) {
    console.log(`[Cache HIT] dashboard:detailed-stats:${yearFilter}`);
    return res.json(cached);
  }
  console.log(`[Cache MISS] dashboard:detailed-stats:${yearFilter}`);

  const client = await dbPool.connect();
  try {
    // 1. Entrevistas de la semana actual
    const today = new Date();
    const startOfWeek = new Date(today);
    startOfWeek.setDate(today.getDate() - today.getDay()); // Domingo
    const endOfWeek = new Date(startOfWeek);
    endOfWeek.setDate(startOfWeek.getDate() + 7);

    const weeklyInterviewsQuery = await mediumQueryBreaker.fire(client, `
      SELECT
        COUNT(*) as total,
        COUNT(CASE WHEN status = 'SCHEDULED' THEN 1 END) as scheduled,
        COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed
      FROM interviews
      WHERE scheduled_date >= $1 AND scheduled_date < $2
    `, [startOfWeek.toISOString(), endOfWeek.toISOString()]);

    // 2. Evaluaciones pendientes por tipo
    const pendingEvaluationsQuery = await mediumQueryBreaker.fire(client, `
      SELECT
        evaluation_type,
        COUNT(*) as count
      FROM evaluations
      WHERE status IN ('PENDING', 'IN_PROGRESS')
      GROUP BY evaluation_type
      ORDER BY count DESC
    `, []);

    // 3. Tendencias mensuales de postulaciones (últimos 12 meses)
    const twelveMonthsAgo = new Date();
    twelveMonthsAgo.setMonth(twelveMonthsAgo.getMonth() - 12);

    const monthlyTrendsQuery = await heavyQueryBreaker.fire(client, `
      SELECT
        TO_CHAR(created_at, 'YYYY-MM') as month,
        COUNT(*) as total,
        COUNT(CASE WHEN status = 'SUBMITTED' THEN 1 END) as submitted,
        COUNT(CASE WHEN status = 'APPROVED' THEN 1 END) as approved,
        COUNT(CASE WHEN status = 'REJECTED' THEN 1 END) as rejected
      FROM applications
      WHERE created_at >= $1
        AND (application_year = $2 OR application_year IS NULL)
      GROUP BY TO_CHAR(created_at, 'YYYY-MM')
      ORDER BY month ASC
    `, [twelveMonthsAgo.toISOString(), yearFilter]);

    // 4. Estadísticas por estado (para el año seleccionado)
    const statusStatsQuery = await mediumQueryBreaker.fire(client, `
      SELECT
        status,
        COUNT(*) as count
      FROM applications
      WHERE application_year = $1 OR application_year IS NULL
      GROUP BY status
    `, [yearFilter]);

    // 5. Años académicos disponibles
    const academicYearsQuery = await simpleQueryBreaker.fire(client, `
      SELECT DISTINCT application_year
      FROM applications
      WHERE application_year IS NOT NULL
      ORDER BY application_year DESC
    `, []);

    // Construir respuesta
    const weeklyInterviews = weeklyInterviewsQuery.rows[0];

    const pendingEvaluations = {};
    pendingEvaluationsQuery.rows.forEach(row => {
      pendingEvaluations[row.evaluation_type] = parseInt(row.count);
    });

    const monthlyTrends = monthlyTrendsQuery.rows.map(row => ({
      month: row.month,
      total: parseInt(row.total),
      submitted: parseInt(row.submitted),
      approved: parseInt(row.approved),
      rejected: parseInt(row.rejected)
    }));

    const statusBreakdown = {};
    statusStatsQuery.rows.forEach(row => {
      statusBreakdown[row.status] = parseInt(row.count);
    });

    const academicYears = academicYearsQuery.rows.map(row => row.application_year);

    const response = {
      success: true,
      data: {
        academicYear: yearFilter,
        availableYears: academicYears,
        weeklyInterviews: {
          total: parseInt(weeklyInterviews.total),
          scheduled: parseInt(weeklyInterviews.scheduled),
          completed: parseInt(weeklyInterviews.completed),
          weekRange: {
            start: startOfWeek.toISOString(),
            end: endOfWeek.toISOString()
          }
        },
        pendingEvaluations: {
          byType: pendingEvaluations,
          total: Object.values(pendingEvaluations).reduce((sum, count) => sum + count, 0)
        },
        monthlyTrends,
        statusBreakdown
      },
      timestamp: new Date().toISOString()
    };

    // Cache for 5 minutes
    dashboardCache.set(cacheKey, response, 300000);
    res.json(response);

  } catch (error) {
    console.error('❌ Error fetching detailed dashboard stats:', error);

    if (error.message && error.message.includes('breaker')) {
      return res.status(503).json({
        success: false,
        error: 'Service temporarily unavailable - circuit breaker open',
        code: 'CIRCUIT_BREAKER_OPEN',
        message: 'El servicio está temporalmente sobrecargado. Por favor, intenta nuevamente en unos minutos.',
        retryAfter: 30
      });
    }

    res.status(500).json({
      success: false,
      error: 'Error al obtener estadísticas detalladas',
      message: error.message
    });
  } finally {
    client.release();
  }
});

// Health check
app.get('/health', (req, res) => {
  res.json({
    status: 'UP',
    service: 'dashboard-mock-service',
    port: port,
    timestamp: new Date().toISOString()
  });
});

// Error handling
app.use((err, req, res, next) => {
  // Error logging removed for security
  res.status(500).json({
    error: 'Internal server error',
    message: err.message,
    service: 'dashboard-mock'
  });
});

// Start server
app.listen(port, () => {
  console.log('Dashboard Service running on port 8086');
  console.log('✅ Circuit breaker enabled for database queries');
  console.log('✅ In-memory cache enabled (5-15 min TTL)');
  console.log('Cache endpoints:');
  console.log('  - POST /api/dashboard/cache/clear (clear cache)');
  console.log('  - GET  /api/dashboard/cache/stats (cache statistics)');
});