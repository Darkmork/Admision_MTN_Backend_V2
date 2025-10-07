const express = require('express');
const compression = require('compression');
const { Pool } = require('pg');
const CircuitBreaker = require('opossum');
const axios = require('axios');
const { translateToSpanish, translateArrayToSpanish } = require('./translations');
const app = express();
const port = 8084;

// Set timezone to America/Santiago for consistent date handling
process.env.TZ = 'America/Santiago';

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
// 4 circuit breaker categories for Evaluation Service
// (No necesita Heavy ni External - sin analytics complejos ni llamadas externas)

// 1. Simple Queries (2s, 60% threshold, 20s reset) - Fast lookups
const simpleQueryBreakerOptions = {
  timeout: 2000,
  errorThresholdPercentage: 60,
  resetTimeout: 20000,
  rollingCountTimeout: 10000,
  rollingCountBuckets: 10,
  name: 'EvaluationSimpleQueryBreaker'
};

// 2. Medium Queries (5s, 50% threshold, 30s reset) - Standard queries with joins
const mediumQueryBreakerOptions = {
  timeout: 5000,
  errorThresholdPercentage: 50,
  resetTimeout: 30000,
  rollingCountTimeout: 10000,
  rollingCountBuckets: 10,
  name: 'EvaluationMediumQueryBreaker'
};

// 3. Write Operations (3s, 30% threshold, 45s reset) - Critical data mutations
const writeOperationBreakerOptions = {
  timeout: 3000,
  errorThresholdPercentage: 30,
  resetTimeout: 45000,
  rollingCountTimeout: 10000,
  rollingCountBuckets: 10,
  name: 'EvaluationWriteBreaker'
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
    console.error(`⚠️ [Circuit Breaker ${name}] OPEN - Too many failures in evaluation service`);
  });

  breaker.on('halfOpen', () => {
    console.warn(`🔄 [Circuit Breaker ${name}] HALF-OPEN - Testing recovery`);
  });

  breaker.on('close', () => {
    console.log(`✅ [Circuit Breaker ${name}] CLOSED - Evaluation service recovered`);
  });

  breaker.on('failure', (error) => {
    console.error(`💥 [Circuit Breaker ${name}] FAILURE - Actual error:`, error.message);
  });

  breaker.fallback(() => {
    throw new Error(`Service temporarily unavailable - ${name} circuit breaker open`);
  });
};

// Setup events for all breakers
setupBreakerEvents(simpleQueryBreaker, 'Simple');
setupBreakerEvents(mediumQueryBreaker, 'Medium');
setupBreakerEvents(writeOperationBreaker, 'Write');

// Legacy breaker for backward compatibility (maps to medium query breaker)
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
const evaluationCache = new SimpleCache();

// ============================================================================
// TRANSLATION HELPERS
// ============================================================================

/**
 * Traduce un objeto de entrevista al español
 * @param {Object} interview - Objeto de entrevista
 * @returns {Object} Entrevista con campos traducidos
 */
function translateInterview(interview) {
  if (!interview) return interview;

  return {
    ...interview,
    status: translateToSpanish(interview.status, 'interview_status')
    // DO NOT translate type - frontend expects English enum values
    // type: translateToSpanish(interview.type, 'interview_type')
  };
}

/**
 * Traduce un array de entrevistas al español
 * @param {Array} interviews - Array de entrevistas
 * @returns {Array} Entrevistas traducidas
 */
function translateInterviews(interviews) {
  if (!Array.isArray(interviews)) return interviews;
  return interviews.map(translateInterview);
}

/**
 * Traduce un objeto de evaluación al español
 * @param {Object} evaluation - Objeto de evaluación
 * @returns {Object} Evaluación con campos traducidos
 */
function translateEvaluation(evaluation) {
  if (!evaluation) return evaluation;

  return {
    ...evaluation,
    status: translateToSpanish(evaluation.status, 'evaluation_status'),
    type: translateToSpanish(evaluation.type, 'evaluation_type')
  };
}

/**
 * Traduce un array de evaluaciones al español
 * @param {Array} evaluations - Array de evaluaciones
 * @returns {Array} Evaluaciones traducidas
 */
function translateEvaluations(evaluations) {
  if (!Array.isArray(evaluations)) return evaluations;
  return evaluations.map(translateEvaluation);
}

// Periodic cleanup of expired entries (every 5 minutes)
setInterval(() => {
  const now = Date.now();
  let cleaned = 0;
  for (const [key, item] of evaluationCache.cache.entries()) {
    if (now > item.expiresAt) {
      evaluationCache.cache.delete(key);
      cleaned++;
    }
  }
  if (cleaned > 0) {
    console.log(`[Cache] Cleaned ${cleaned} expired entries`);
  }
}, 300000);

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
    return {
      success: true,
      data: items,
      total: total,
      page: page,
      limit: limit,
      totalPages: Math.ceil(total / limit),
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

app.use(express.json());
app.use(compression({
  filter: (req, res) => {
    if (req.headers['x-no-compression']) return false;
    return compression.filter(req, res);
  },
  threshold: 1024,
  level: 6
}));

// Helper functions to map evaluation types to subjects
function getSubjectFromEvaluationType(evaluationType) {
  const typeMap = {
    'LANGUAGE_EXAM': 'SPANISH',
    'MATHEMATICS_EXAM': 'MATHEMATICS',
    'ENGLISH_EXAM': 'ENGLISH',
    'SCIENCE_EXAM': 'SCIENCE',
    'HISTORY_EXAM': 'HISTORY',
    'PSYCHOLOGICAL_INTERVIEW': 'PSYCHOLOGY'
  };
  return typeMap[evaluationType] || 'GENERAL';
}

function getSubjectDisplayName(evaluationType, professorSubject) {
  if (professorSubject) {
    const subjectNames = {
      'MATHEMATICS': 'Matemáticas',
      'SPANISH': 'Lenguaje y Comunicación',
      'ENGLISH': 'Inglés',
      'SCIENCE': 'Ciencias Naturales',
      'HISTORY': 'Historia y Geografía',
      'PSYCHOLOGY': 'Evaluación Psicológica'
    };
    return subjectNames[professorSubject] || professorSubject;
  }

  const typeNames = {
    'LANGUAGE_EXAM': 'Lenguaje y Comunicación',
    'MATHEMATICS_EXAM': 'Matemáticas',
    'ENGLISH_EXAM': 'Inglés',
    'SCIENCE_EXAM': 'Ciencias Naturales',
    'HISTORY_EXAM': 'Historia y Geografía',
    'PSYCHOLOGICAL_INTERVIEW': 'Evaluación Psicológica'
  };
  return typeNames[evaluationType] || 'Evaluación General';
}

// CORS middleware - commented out because NGINX handles it
// app.use((req, res, next) => {
//   res.header('Access-Control-Allow-Origin', '*');
//   res.header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
//   res.header('Access-Control-Allow-Headers', 'Origin, X-Requested-With, Content-Type, Accept, Authorization');
//
//   if (req.method === 'OPTIONS') {
//     res.sendStatus(200);
//   } else {
//     next();
//   }
// });

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ 
    status: 'UP', 
    service: 'evaluation-service',
    port: port,
    timestamp: new Date().toISOString()
  });
});

// Mock evaluation endpoints
// Get all evaluations - FIXED: Now queries real database instead of mock data
app.get('/api/evaluations', async (req, res) => {
  const client = await dbPool.connect();
  try {
    console.log('📊 Getting ALL evaluations from database (including Alejandra Flores)');

    // Query all evaluations with evaluator and student information
    const result = await mediumQueryBreaker.fire(client, `
      SELECT
        e.*,
        u.id as evaluator_user_id,
        u.first_name as evaluator_first_name,
        u.last_name as evaluator_last_name,
        u.email as evaluator_email,
        u.role as evaluator_role,
        u.subject as evaluator_subject,
        a.student_id,
        s.first_name as student_first_name,
        s.paternal_last_name as student_last_name,
        a.status as application_status
      FROM evaluations e
      LEFT JOIN users u ON e.evaluator_id = u.id
      LEFT JOIN applications a ON e.application_id = a.id
      LEFT JOIN students s ON a.student_id = s.id
      ORDER BY e.created_at DESC
    `, []);

    const evaluations = result.rows.map(row => ({
      id: row.id,
      evaluationType: row.evaluation_type,
      status: row.status,
      score: row.score,
      grade: row.grade,
      applicationId: row.application_id,
      evaluatorId: row.evaluator_id,
      scheduleId: row.schedule_id,
      evaluationDate: row.evaluation_date,
      completionDate: row.completion_date,
      observations: row.observations,
      strengths: row.strengths,
      areasForImprovement: row.areas_for_improvement,
      recommendations: row.recommendations,
      academicReadiness: row.academic_readiness,
      socialSkillsAssessment: row.social_skills_assessment,
      emotionalMaturity: row.emotional_maturity,
      behavioralAssessment: row.behavioral_assessment,
      motivationAssessment: row.motivation_assessment,
      familySupportAssessment: row.family_support_assessment,
      integrationPotential: row.integration_potential,
      finalRecommendation: row.final_recommendation,
      createdAt: row.created_at,
      updatedAt: row.updated_at,
      evaluator: {
        id: row.evaluator_user_id,
        firstName: row.evaluator_first_name,
        lastName: row.evaluator_last_name,
        email: row.evaluator_email,
        role: row.evaluator_role,
        subject: row.evaluator_subject
      },
      student: {
        id: row.student_id,
        firstName: row.student_first_name,
        lastName: row.student_last_name
      },
      applicationStatus: row.application_status
    }));

    console.log(`✅ Found ${evaluations.length} evaluations in database`);

    res.json({
      success: true,
      data: evaluations,
      count: evaluations.length
    });
  } catch (error) {
    console.error('❌ Error fetching evaluations:', error);
    res.status(500).json({
      success: false,
      error: 'Error al obtener evaluaciones',
      message: error.message
    });
  } finally {
    client.release();
  }
});

// Get evaluation types metadata - Returns available evaluation types
// NOTE: This route MUST come BEFORE /api/evaluations/:evaluationId to avoid route conflicts
app.get('/api/evaluations/metadata/types', async (req, res) => {
  try {
    console.log('📋 Getting evaluation types metadata');

    const evaluationTypes = [
      {
        code: 'LANGUAGE_EXAM',
        name: 'Examen de Lenguaje',
        description: 'Evaluación de competencias lingüísticas y comprensión lectora'
      },
      {
        code: 'MATHEMATICS_EXAM',
        name: 'Examen de Matemáticas',
        description: 'Evaluación de habilidades matemáticas y razonamiento lógico'
      },
      {
        code: 'ENGLISH_EXAM',
        name: 'Examen de Inglés',
        description: 'Evaluación de nivel de inglés'
      },
      {
        code: 'PSYCHOLOGICAL_EVALUATION',
        name: 'Evaluación Psicológica',
        description: 'Evaluación del desarrollo emocional y social del estudiante'
      },
      {
        code: 'INTERVIEW',
        name: 'Entrevista',
        description: 'Entrevista personal con estudiante y/o apoderados'
      },
      {
        code: 'ENTRANCE_EXAM',
        name: 'Examen de Admisión',
        description: 'Examen general de admisión'
      }
    ];

    res.json({
      success: true,
      data: evaluationTypes
    });
  } catch (error) {
    console.error('❌ Error getting evaluation types:', error);
    res.status(500).json({
      success: false,
      error: 'Error al obtener tipos de evaluación',
      message: error.message
    });
  }
});

// Get evaluation statistics - FIXED: Now queries real database instead of mock data
// NOTE: This route MUST come BEFORE /api/evaluations/:evaluationId to avoid route conflicts
app.get('/api/evaluations/statistics', async (req, res) => {
  const client = await dbPool.connect();
  try {
    console.log('📊 Getting evaluation statistics from database');

    // Total count
    const totalResult = await simpleQueryBreaker.fire(client,
      'SELECT COUNT(*) as total FROM evaluations',
      []
    );
    const totalEvaluations = parseInt(totalResult.rows[0].total);

    // Status breakdown
    const statusResult = await simpleQueryBreaker.fire(client, `
      SELECT status, COUNT(*) as count
      FROM evaluations
      GROUP BY status
    `, []);

    const statusBreakdown = {
      PENDING: 0,
      IN_PROGRESS: 0,
      COMPLETED: 0
    };
    statusResult.rows.forEach(row => {
      statusBreakdown[row.status] = parseInt(row.count);
    });

    // Type breakdown
    const typeResult = await simpleQueryBreaker.fire(client, `
      SELECT evaluation_type, COUNT(*) as count
      FROM evaluations
      GROUP BY evaluation_type
    `, []);

    const typeBreakdown = {
      LANGUAGE_EXAM: 0,
      MATHEMATICS_EXAM: 0,
      ENGLISH_EXAM: 0,
      CYCLE_DIRECTOR_REPORT: 0,
      PSYCHOLOGICAL_INTERVIEW: 0
    };
    typeResult.rows.forEach(row => {
      typeBreakdown[row.evaluation_type] = parseInt(row.count);
    });

    // Average scores by type (only for exams with numeric scores)
    const avgScoresResult = await simpleQueryBreaker.fire(client, `
      SELECT evaluation_type, AVG(score) as avg_score
      FROM evaluations
      WHERE score IS NOT NULL
        AND evaluation_type IN ('LANGUAGE_EXAM', 'MATHEMATICS_EXAM', 'ENGLISH_EXAM')
      GROUP BY evaluation_type
    `, []);

    const averageScoresByType = {};
    avgScoresResult.rows.forEach(row => {
      averageScoresByType[row.evaluation_type] = parseFloat(row.avg_score).toFixed(2);
    });

    // Evaluator activity
    const evaluatorResult = await mediumQueryBreaker.fire(client, `
      SELECT
        u.first_name || ' ' || u.last_name as evaluator_name,
        COUNT(e.id) as evaluation_count
      FROM evaluations e
      JOIN users u ON e.evaluator_id = u.id
      GROUP BY u.id, u.first_name, u.last_name
      ORDER BY evaluation_count DESC
    `, []);

    const evaluatorActivity = {};
    evaluatorResult.rows.forEach(row => {
      evaluatorActivity[row.evaluator_name] = parseInt(row.evaluation_count);
    });

    // Completion rate
    const completionRate = totalEvaluations > 0
      ? ((statusBreakdown.COMPLETED / totalEvaluations) * 100).toFixed(2)
      : 0;

    const stats = {
      totalEvaluations,
      statusBreakdown,
      typeBreakdown,
      averageScoresByType,
      evaluatorActivity,
      completionRate: parseFloat(completionRate)
    };

    console.log(`✅ Statistics calculated: ${totalEvaluations} total evaluations`);
    res.json(ResponseHelper.ok(stats));

  } catch (error) {
    console.error('❌ Error fetching evaluation statistics:', error);
    res.status(500).json({
      success: false,
      error: 'Error al obtener estadísticas de evaluaciones',
      message: error.message
    });
  } finally {
    client.release();
  }
});

// Get evaluation statistics (public endpoint) - FIXED: Now queries real database
// NOTE: This route MUST come BEFORE /api/evaluations/:evaluationId to avoid route conflicts
app.get('/api/evaluations/public/statistics', async (req, res) => {
  const client = await dbPool.connect();
  try {
    console.log('📊 Getting evaluation statistics from database (public)');

    // Total count
    const totalResult = await simpleQueryBreaker.fire(client,
      'SELECT COUNT(*) as total FROM evaluations',
      []
    );
    const totalEvaluations = parseInt(totalResult.rows[0].total);

    // Status breakdown
    const statusResult = await simpleQueryBreaker.fire(client, `
      SELECT status, COUNT(*) as count
      FROM evaluations
      GROUP BY status
    `, []);

    const statusBreakdown = {
      PENDING: 0,
      IN_PROGRESS: 0,
      COMPLETED: 0
    };
    statusResult.rows.forEach(row => {
      statusBreakdown[row.status] = parseInt(row.count);
    });

    // Type breakdown
    const typeResult = await simpleQueryBreaker.fire(client, `
      SELECT evaluation_type, COUNT(*) as count
      FROM evaluations
      GROUP BY evaluation_type
    `, []);

    const typeBreakdown = {
      LANGUAGE_EXAM: 0,
      MATHEMATICS_EXAM: 0,
      ENGLISH_EXAM: 0,
      CYCLE_DIRECTOR_REPORT: 0,
      PSYCHOLOGICAL_INTERVIEW: 0
    };
    typeResult.rows.forEach(row => {
      typeBreakdown[row.evaluation_type] = parseInt(row.count);
    });

    // Average scores by type (only for exams with numeric scores)
    const avgScoresResult = await simpleQueryBreaker.fire(client, `
      SELECT evaluation_type, AVG(score) as avg_score
      FROM evaluations
      WHERE score IS NOT NULL
        AND evaluation_type IN ('LANGUAGE_EXAM', 'MATHEMATICS_EXAM', 'ENGLISH_EXAM')
      GROUP BY evaluation_type
    `, []);

    const averageScoresByType = {};
    avgScoresResult.rows.forEach(row => {
      averageScoresByType[row.evaluation_type] = parseFloat(row.avg_score).toFixed(2);
    });

    // Evaluator activity
    const evaluatorResult = await mediumQueryBreaker.fire(client, `
      SELECT
        u.first_name || ' ' || u.last_name as evaluator_name,
        COUNT(e.id) as evaluation_count
      FROM evaluations e
      JOIN users u ON e.evaluator_id = u.id
      GROUP BY u.id, u.first_name, u.last_name
      ORDER BY evaluation_count DESC
    `, []);

    const evaluatorActivity = {};
    evaluatorResult.rows.forEach(row => {
      evaluatorActivity[row.evaluator_name] = parseInt(row.evaluation_count);
    });

    // Completion rate
    const completionRate = totalEvaluations > 0
      ? ((statusBreakdown.COMPLETED / totalEvaluations) * 100).toFixed(2)
      : 0;

    const stats = {
      totalEvaluations,
      statusBreakdown,
      typeBreakdown,
      averageScoresByType,
      evaluatorActivity,
      completionRate: parseFloat(completionRate)
    };

    console.log(`✅ Public statistics calculated: ${totalEvaluations} total evaluations`);
    res.json(ResponseHelper.ok(stats));

  } catch (error) {
    console.error('❌ Error fetching public evaluation statistics:', error);
    res.status(500).json({
      success: false,
      error: 'Error al obtener estadísticas públicas de evaluaciones',
      message: error.message
    });
  } finally {
    client.release();
  }
});

// Datos de entrevistadores disponibles (basados en los usuarios del sistema de gestión)
const interviewers = [
  {
    id: 24,
    name: 'María Elena Sánchez Cortés',
    role: 'TEACHER',
    subject: 'Matemáticas',
    educationalLevel: 'Primaria',
    scheduleCount: 3
  },
  {
    id: 25,
    name: 'Dr. Roberto Andrés Vega Muñoz',
    role: 'PSYCHOLOGIST',
    subject: null,
    educationalLevel: 'Todos los niveles',
    scheduleCount: 5
  },
  {
    id: 26,
    name: 'Carmen Isabel Rojas Fernández',
    role: 'COORDINATOR',
    subject: 'Ciencias Naturales',
    educationalLevel: 'Secundaria',
    scheduleCount: 2
  },
  {
    id: 27,
    name: 'Prof. Juan Carlos Montenegro Silva',
    role: 'CYCLE_DIRECTOR',
    subject: 'Educación Física',
    educationalLevel: 'Básica',
    scheduleCount: 4
  },
  {
    id: 28,
    name: 'Ana María Gutierrez Valdés',
    role: 'TEACHER',
    subject: 'Lenguaje y Literatura',
    educationalLevel: 'Primaria',
    scheduleCount: 6
  },
  {
    id: 29,
    name: 'Luis Fernando Castro Morales',
    role: 'TEACHER',
    subject: 'Historia y Geografía',
    educationalLevel: 'Secundaria',
    scheduleCount: 2
  }
];

// Endpoint público para obtener entrevistadores disponibles
app.get('/api/interviews/public/interviewers', async (req, res) => {
  console.log('📋 Solicitud de entrevistadores disponibles');

  // Check cache first
  const cacheKey = 'interviews:public:interviewers';
  const cached = evaluationCache.get(cacheKey);
  if (cached) {
    console.log('[Cache HIT] interviews:public:interviewers');
    return res.json(cached);
  }
  console.log('[Cache MISS] interviews:public:interviewers');

  const client = await dbPool.connect();
  try {
    // Consultar directamente la base de datos para obtener usuarios con horarios
    const query = `
      SELECT
        u.id,
        CONCAT(u.first_name, ' ', u.last_name) as name,
        u.role,
        u.subject,
        u.educational_level,
        COUNT(s.id) as schedule_count
      FROM users u
      INNER JOIN interviewer_schedules s ON u.id = s.interviewer_id
      WHERE u.active = true
        AND s.is_active = true
        AND u.role IN ('TEACHER', 'COORDINATOR', 'CYCLE_DIRECTOR', 'PSYCHOLOGIST', 'ADMIN')
      GROUP BY u.id, u.first_name, u.last_name, u.role, u.subject, u.educational_level
      HAVING COUNT(s.id) > 0
      ORDER BY u.first_name, u.last_name;
    `;

    const result = await client.query(query);

    // Formatear la respuesta para que coincida con el formato esperado
    const activeInterviewers = result.rows.map(row => ({
      id: row.id,
      name: row.name,
      role: row.role,
      subject: row.subject,
      educationalLevel: row.educational_level || (row.role === 'PSYCHOLOGIST' ? 'Todos los niveles' : 'Sin especificar'),
      scheduleCount: parseInt(row.schedule_count)
    }));

    console.log(`✅ Encontrados ${activeInterviewers.length} entrevistadores activos`);

    // Cache before sending response (5 minutes)
    evaluationCache.set(cacheKey, activeInterviewers, 300000);
    res.json(activeInterviewers);

  } catch (error) {
    console.error('❌ Error obteniendo entrevistadores:', error);

    // Como fallback, usar datos mockeados
    const activeInterviewers = interviewers.filter(interviewer => interviewer.scheduleCount > 0);
    res.json(activeInterviewers);
  } finally {
    client.release();
  }
});

// Endpoint público para obtener información completa de entrevistas
app.get('/api/interviews/public/complete', (req, res) => {
  console.log('📋 Solicitud de información completa de entrevistas');
  
  // Combinar entrevistas con información de entrevistadores y estudiantes
  const completeInterviews = interviews.map(interview => {
    // Buscar el entrevistador correspondiente
    const interviewer = interviewers.find(i => i.id === interview.interviewerId);
    
    // Mock data de estudiante basado en applicationId
    const studentMockData = {
      1: { id: 1, firstName: 'Ana', lastName: 'García Rodríguez', rut: '20123456-7' },
      2: { id: 2, firstName: 'Carlos', lastName: 'Martínez López', rut: '20234567-8' },
      3: { id: 3, firstName: 'María', lastName: 'Jiménez Valdez', rut: '20345678-9' },
      4: { id: 4, firstName: 'Pedro', lastName: 'Sánchez Díaz', rut: '20456789-0' }
    };
    
    const student = studentMockData[interview.applicationId] || {
      id: interview.applicationId,
      firstName: 'Estudiante',
      lastName: `N°${interview.applicationId}`,
      rut: `2012345${interview.applicationId}-${interview.applicationId}`
    };
    
    return {
      ...interview,
      interviewer: {
        id: interviewer?.id,
        name: interviewer?.name,
        role: interviewer?.role,
        subject: interviewer?.subject
      },
      student: {
        id: student.id,
        firstName: student.firstName,
        lastName: student.lastName,
        fullName: `${student.firstName} ${student.lastName}`,
        rut: student.rut
      },
      application: {
        id: interview.applicationId,
        studentId: student.id,
        status: 'ACTIVE'
      }
    };
  });
  
  res.json({
    success: true,
    data: completeInterviews,
    count: completeInterviews.length,
    summary: {
      total: completeInterviews.length,
      scheduled: completeInterviews.filter(i => i.status === 'SCHEDULED').length,
      completed: completeInterviews.filter(i => i.status === 'COMPLETED').length,
      cancelled: completeInterviews.filter(i => i.status === 'CANCELLED').length
    }
  });
});

// Endpoint para obtener estudiantes disponibles para programar entrevistas
app.get('/api/interviews/students', async (req, res) => {
  console.log('👥 Solicitud de estudiantes disponibles para entrevistas');

  try {
    // Obtener aplicaciones desde el servicio de aplicaciones
    const applicationsResponse = await fetch('http://localhost:8083/api/applications');
    if (!applicationsResponse.ok) {
      throw new Error('Error obteniendo aplicaciones');
    }

    const applications = await applicationsResponse.json();

    // Filtrar y transformar estudiantes disponibles
    const availableStudents = applications
      .filter(app => app && app.student && app.student.firstName && app.student.lastName)
      .map(app => ({
        id: app.id,
        applicationId: app.id,
        firstName: app.student.firstName,
        lastName: app.student.lastName,
        fullName: app.student.fullName || `${app.student.firstName} ${app.student.lastName}`,
        rut: app.student.rut,
        grade: app.student.gradeApplied,
        status: app.status,
        email: app.student.email || '',
        phone: app.father?.phone || app.mother?.phone || '',
        submissionDate: app.submissionDate
      }))
      .sort((a, b) => a.lastName.localeCompare(b.lastName));

    console.log(`✅ Enviando ${availableStudents.length} estudiantes disponibles`);

    res.json({
      success: true,
      data: availableStudents,
      count: availableStudents.length
    });

  } catch (error) {
    console.error('❌ Error obteniendo estudiantes:', error);

    // Fallback con datos mock si falla la conexión con aplicaciones
    const mockStudents = [
      {
        id: 1,
        applicationId: 1,
        firstName: 'Juan',
        lastName: 'Pérez González',
        fullName: 'Juan Pérez González',
        rut: '20001001-1',
        grade: 'PRIMERO_BASICO',
        status: 'PENDING',
        email: 'juan@example.com',
        phone: '+56912001001',
        submissionDate: '2025-08-24T16:32:47.864Z'
      },
      {
        id: 2,
        applicationId: 2,
        firstName: 'María',
        lastName: 'López Silva',
        fullName: 'María López Silva',
        rut: '20001002-K',
        grade: 'SEGUNDO_BASICO',
        status: 'UNDER_REVIEW',
        email: 'maria@example.com',
        phone: '+56912002001',
        submissionDate: '2025-08-26T16:32:47.864Z'
      }
    ];

    res.json({
      success: true,
      data: mockStudents,
      count: mockStudents.length,
      note: 'Datos mock - servicio de aplicaciones no disponible'
    });
  }
});

// Endpoint para obtener slots disponibles para agendamiento
app.get('/api/interviews/available-slots', async (req, res) => {
  const { interviewerId, date, duration } = req.query;

  console.log(`🕒 Solicitud de horarios disponibles para entrevistador ${interviewerId} en fecha ${date} con duración ${duration} minutos`);

  const client = await dbPool.connect();
  try {
    // Verificar que el entrevistador existe en la base de datos
    const interviewerQuery = await client.query(
      'SELECT id, role FROM users WHERE id = $1 AND role IN (\'TEACHER\', \'PSYCHOLOGIST\', \'COORDINATOR\', \'CYCLE_DIRECTOR\')',
      [parseInt(interviewerId)]
    );

    console.log(`🔍 Entrevistador ${interviewerId} encontrado:`, interviewerQuery.rows.length > 0 ? interviewerQuery.rows[0] : 'NO');

    if (interviewerQuery.rows.length === 0) {
      return res.status(404).json({
        success: false,
        error: 'Entrevistador no encontrado'
      });
    }

    // Parse date correctly to avoid UTC timezone issues
    const [year, month, day] = date.split('-').map(Number);
    const requestDate = new Date(year, month - 1, day); // month is 0-indexed
    const dayOfWeek = requestDate.getDay(); // 0 = Domingo, 1 = Lunes, etc.
    const dayNames = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
    const dayName = dayNames[dayOfWeek];

    console.log(`📅 Fecha solicitada: ${date}, dayOfWeek JS: ${dayOfWeek}, dayName: ${dayName}`);

    // Obtener horarios configurados para el entrevistador en ese día
    const schedulesQuery = await client.query(
      `SELECT start_time, end_time
       FROM interviewer_schedules
       WHERE interviewer_id = $1
         AND day_of_week = $2
         AND is_active = true
         AND schedule_type = 'RECURRING'
       ORDER BY start_time`,
      [parseInt(interviewerId), dayName]
    );

    console.log(`📋 Horarios encontrados para interviewer_id=${interviewerId}, day=${dayName}: ${schedulesQuery.rows.length}`);

    if (schedulesQuery.rows.length === 0) {
      // No hay horarios configurados para este día
      return res.json({
        success: true,
        data: {
          date: date,
          interviewerId: parseInt(interviewerId),
          duration: parseInt(duration) || 60,
          availableSlots: []
        }
      });
    }

    // Obtener entrevistas ya agendadas para este entrevistador en esta fecha
    const occupiedQuery = await client.query(
      `SELECT DATE(scheduled_date) as date,
              TO_CHAR(scheduled_date, 'HH24:MI') as time,
              duration_minutes
       FROM interviews
       WHERE interviewer_id = $1
         AND DATE(scheduled_date) = $2
         AND status IN ('SCHEDULED', 'CONFIRMED', 'IN_PROGRESS')`,
      [parseInt(interviewerId), date]
    );

    const occupiedSlots = new Set(occupiedQuery.rows.map(row => row.time));
    console.log(`📅 Slots ocupados para ${date}:`, Array.from(occupiedSlots));

    // Generar slots disponibles basados en horarios reales
    const availableSlots = [];
    const slotDuration = parseInt(duration) || 30; // Default 30 minutos

    console.log(`📋 Horarios encontrados en BD: ${schedulesQuery.rows.length}`);

    for (const schedule of schedulesQuery.rows) {
      const startTime = schedule.start_time; // formato "HH:MM:SS"
      const slotTime = startTime.substring(0, 5); // "HH:MM"

      console.log(`🔍 Revisando slot ${slotTime} - ocupado: ${occupiedSlots.has(slotTime)}`);

      // Verificar si el slot está ocupado
      if (!occupiedSlots.has(slotTime)) {
        availableSlots.push({
          time: slotTime,
          available: true,
          duration: slotDuration
        });
      }
    }

    console.log(`✅ Slots disponibles encontrados: ${availableSlots.length}`);

    res.json({
      success: true,
      data: {
        date: date,
        interviewerId: parseInt(interviewerId),
        duration: parseInt(duration) || 60,
        availableSlots: availableSlots
      }
    });
  } catch (error) {
    console.error('Error obteniendo slots disponibles:', error);
    res.status(500).json({
      success: false,
      error: 'Error interno del servidor',
      message: error.message
    });
  } finally {
    client.release();
  }
});

// Endpoint para obtener disponibilidad de entrevistadores en un rango de fechas
app.get('/api/interviews/interviewer-availability', async (req, res) => {
  const { interviewerId, startDate, endDate } = req.query;

  console.log(`📅 Solicitud de disponibilidad para entrevistador ${interviewerId} desde ${startDate} hasta ${endDate}`);

  const client = await dbPool.connect();
  try {
    // Verificar que el entrevistador existe en la base de datos
    const interviewerQuery = await client.query(
      'SELECT id, CONCAT(first_name, \' \', last_name) as name, role, subject FROM users WHERE id = $1 AND role IN (\'TEACHER\', \'PSYCHOLOGIST\', \'COORDINATOR\', \'CYCLE_DIRECTOR\')',
      [parseInt(interviewerId)]
    );

    if (interviewerQuery.rows.length === 0) {
      return res.status(404).json({
        success: false,
        error: 'Entrevistador no encontrado'
      });
    }

    const interviewer = interviewerQuery.rows[0];

    // Validar fechas
    if (!startDate || !endDate) {
      return res.status(400).json({
        success: false,
        error: 'Se requieren las fechas de inicio y fin'
      });
    }

    const start = new Date(startDate);
    const end = new Date(endDate);

    if (start > end) {
      return res.status(400).json({
        success: false,
        error: 'La fecha de inicio debe ser anterior a la fecha de fin'
      });
    }

    // Generar disponibilidad para cada día en el rango
    const availability = [];
    const currentDate = new Date(start);

    while (currentDate <= end) {
      const dayOfWeek = currentDate.getDay(); // 0 = Domingo, 1 = Lunes, etc.
      const dateString = currentDate.toISOString().split('T')[0];

      // Solo días laborables (Lunes a Viernes)
      if (dayOfWeek >= 1 && dayOfWeek <= 5) {
        // Horarios típicos del entrevistador
        const morningSlots = [
          { time: '09:00:00', duration: 60, available: true },
          { time: '10:00:00', duration: 60, available: Math.random() > 0.3 }, // 70% disponible
          { time: '11:00:00', duration: 60, available: Math.random() > 0.4 }  // 60% disponible
        ];

        const afternoonSlots = [
          { time: '14:00:00', duration: 60, available: Math.random() > 0.2 }, // 80% disponible
          { time: '15:00:00', duration: 60, available: Math.random() > 0.3 }, // 70% disponible
          { time: '16:00:00', duration: 60, available: Math.random() > 0.5 }  // 50% disponible
        ];

        const daySlots = [...morningSlots, ...afternoonSlots].filter(slot => slot.available);

        availability.push({
          date: dateString,
          dayOfWeek: ['DOMINGO', 'LUNES', 'MARTES', 'MIÉRCOLES', 'JUEVES', 'VIERNES', 'SÁBADO'][dayOfWeek],
          isWorkingDay: true,
          totalSlots: morningSlots.length + afternoonSlots.length,
          availableSlots: daySlots.length,
          slots: daySlots.map(slot => ({
            startTime: slot.time,
            duration: slot.duration,
            available: slot.available
          }))
        });
      } else {
        // Fin de semana - no disponible
        availability.push({
          date: dateString,
          dayOfWeek: ['DOMINGO', 'LUNES', 'MARTES', 'MIÉRCOLES', 'JUEVES', 'VIERNES', 'SÁBADO'][dayOfWeek],
          isWorkingDay: false,
          totalSlots: 0,
          availableSlots: 0,
          slots: []
        });
      }

      // Avanzar al siguiente día
      currentDate.setDate(currentDate.getDate() + 1);
    }

    // Calcular estadísticas generales
    const totalWorkingDays = availability.filter(day => day.isWorkingDay).length;
    const totalAvailableSlots = availability.reduce((sum, day) => sum + day.availableSlots, 0);
    const totalSlots = availability.reduce((sum, day) => sum + day.totalSlots, 0);

    res.json({
      success: true,
      data: {
        interviewerId: parseInt(interviewerId),
        interviewer: {
          id: parseInt(interviewer.id),
          name: interviewer.name,
          role: interviewer.role,
          subject: interviewer.subject || 'N/A'
        },
        period: {
          startDate,
          endDate,
          totalDays: availability.length,
          workingDays: totalWorkingDays
        },
        summary: {
          totalSlots,
          availableSlots: totalAvailableSlots,
          occupiedSlots: totalSlots - totalAvailableSlots,
          availabilityRate: totalSlots > 0 ? ((totalAvailableSlots / totalSlots) * 100).toFixed(1) : 0
        },
        dailyAvailability: availability
      }
    });
  } catch (error) {
    console.error('Error obteniendo disponibilidad del entrevistador:', error);
    res.status(500).json({
      success: false,
      error: 'Error interno del servidor',
      message: error.message
    });
  } finally {
    client.release();
  }
});

// Mock de entrevistas agendadas
const interviews = [
  {
    id: 1,
    applicationId: 1,
    interviewerId: 24,
    scheduledDate: '2025-09-05T13:30:00Z',
    duration: 60,
    type: 'FAMILY',
    mode: 'IN_PERSON',
    status: 'SCHEDULED',
    location: 'Sala de entrevistas 1',
    notes: 'Primera entrevista familiar',
    createdAt: '2025-09-01T10:00:00Z'
  },
  {
    id: 2,
    applicationId: 2,
    interviewerId: 25,
    scheduledDate: '2025-09-06T10:00:00Z',
    duration: 45,
    type: 'PSYCHOLOGICAL',
    mode: 'IN_PERSON',
    status: 'COMPLETED',
    location: 'Oficina de psicología',
    notes: 'Evaluación psicológica inicial',
    result: 'APPROVED',
    score: 85,
    createdAt: '2025-08-28T14:30:00Z'
  }
];

// Endpoint para crear una nueva entrevista
app.post('/api/interviews', async (req, res) => {
  const {
    applicationId,
    interviewerId,
    scheduledDate,
    scheduledTime,  // Añadir scheduledTime
    duration,
    type,
    mode,
    location,
    notes
  } = req.body;

  console.log('📝 Creando nueva entrevista:', req.body);

  // Validar datos requeridos
  if (!applicationId || !interviewerId || !scheduledDate || !type) {
    return res.status(400).json({
      success: false,
      error: 'Faltan campos requeridos: applicationId, interviewerId, scheduledDate, type'
    });
  }

  // Validar tipos permitidos según DB constraints
  const validInterviewTypes = ['INDIVIDUAL', 'FAMILY', 'PSYCHOLOGICAL', 'ACADEMIC', 'BEHAVIORAL'];
  if (!validInterviewTypes.includes(type)) {
    return res.status(400).json({
      success: false,
      error: `Tipo de entrevista inválido. Tipos permitidos: ${validInterviewTypes.join(', ')}`,
      validTypes: validInterviewTypes
    });
  }

  // Validar modo de entrevista si se proporciona (alineado con DB constraint)
  const validModes = ['IN_PERSON', 'VIRTUAL', 'PHONE'];
  const providedMode = mode || 'IN_PERSON'; // Default to IN_PERSON
  if (!validModes.includes(providedMode)) {
    return res.status(400).json({
      success: false,
      error: `Modo de entrevista inválido. Modos permitidos: ${validModes.join(', ')}`,
      validModes: validModes,
      received: providedMode
    });
  }

  // Validar y normalizar fecha/hora con zona horaria Chile
  let normalizedScheduledDate;
  try {
    // Si tenemos scheduledTime separado, combinar con scheduledDate
    let fullDateTime;
    if (scheduledTime) {
      // scheduledDate viene como YYYY-MM-DD y scheduledTime como HH:MM
      fullDateTime = `${scheduledDate}T${scheduledTime}:00`;
      console.log('📅 Combinando fecha y hora:', {
        date: scheduledDate,
        time: scheduledTime,
        combined: fullDateTime
      });
    } else {
      // Si no hay scheduledTime, asumir que scheduledDate ya tiene la hora
      fullDateTime = scheduledDate;
    }

    const inputDate = new Date(fullDateTime);
    if (isNaN(inputDate.getTime())) {
      return res.status(400).json({
        success: false,
        error: 'Formato de fecha/hora inválido'
      });
    }

    // Verificar que la fecha no sea en el pasado (con tolerancia de 1 hora para evitar problemas de timezone)
    const now = new Date();
    now.setHours(now.getHours() - 1); // Tolerancia de 1 hora
    if (inputDate < now) {
      return res.status(400).json({
        success: false,
        error: 'La fecha de la entrevista no puede ser en el pasado'
      });
    }

    // Crear fecha local en formato simple YYYY-MM-DD HH:MM:SS para PostgreSQL
    const year = inputDate.getFullYear();
    const month = String(inputDate.getMonth() + 1).padStart(2, '0');
    const day = String(inputDate.getDate()).padStart(2, '0');
    const hours = String(inputDate.getHours()).padStart(2, '0');
    const minutes = String(inputDate.getMinutes()).padStart(2, '0');
    const seconds = String(inputDate.getSeconds()).padStart(2, '0');
    normalizedScheduledDate = `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;

    console.log('📅 Fecha normalizada:', {
      original: scheduledDate,
      time: scheduledTime,
      combined: fullDateTime,
      parsed: inputDate,
      normalized: normalizedScheduledDate
    });

  } catch (error) {
    return res.status(400).json({
      success: false,
      error: 'Error procesando la fecha de la entrevista',
      details: error.message
    });
  }

  const client = await dbPool.connect();
  let clientReleased = false;
  try {
    // ✅ VALIDACIÓN 1: Verificar que no exista una entrevista activa del mismo tipo
    console.log(`🔍 Verificando si existe entrevista ${type} para aplicación ${applicationId}...`);
    const existingInterviewQuery = await client.query(
      `SELECT id, interview_type, status, scheduled_date
       FROM interviews
       WHERE application_id = $1
         AND interview_type = $2
         AND status NOT IN ('CANCELLED', 'NO_SHOW')
       LIMIT 1`,
      [parseInt(applicationId), type]
    );

    if (existingInterviewQuery.rows.length > 0) {
      const existing = existingInterviewQuery.rows[0];
      const typeLabels = {
        'INDIVIDUAL': 'Individual',
        'FAMILY': 'Familiar',
        'PSYCHOLOGICAL': 'Psicológica',
        'ACADEMIC': 'Académica',
        'BEHAVIORAL': 'Conductual'
      };

      console.log(`❌ Ya existe entrevista ${type} activa (ID: ${existing.id})`);

      client.release();
      clientReleased = true;
      return res.status(409).json({
        success: false,
        error: `Ya existe una entrevista ${typeLabels[type]} activa para esta aplicación`,
        details: {
          existingInterviewId: existing.id,
          type: existing.interview_type,
          status: existing.status,
          scheduledDate: existing.scheduled_date,
          message: `Para agendar una nueva entrevista ${typeLabels[type]}, primero debe cancelar la entrevista existente (ID: ${existing.id})`
        },
        code: 'DUPLICATE_INTERVIEW_TYPE'
      });
    }

    console.log(`✅ No existe entrevista ${type} activa, continuando...`);

    // 🔒 VALIDACIÓN 2: Verificar que el slot no esté ocupado
    const conflictQuery = `
      SELECT id, scheduled_date, status
      FROM interviews
      WHERE interviewer_id = $1
        AND scheduled_date = $2
        AND status IN ('SCHEDULED', 'CONFIRMED', 'IN_PROGRESS')
    `;

    const conflictResult = await client.query(conflictQuery, [
      parseInt(interviewerId),
      normalizedScheduledDate
    ]);

    if (conflictResult.rows.length > 0) {
      const existingInterview = conflictResult.rows[0];
      console.log('❌ CONFLICTO DE HORARIO detectado:', existingInterview);

      return res.status(409).json({
        success: false,
        error: 'SLOT_ALREADY_TAKEN',
        message: 'Este horario ya está reservado. Por favor seleccione otro horario.',
        details: {
          interviewerId: interviewerId,
          scheduledDate: normalizedScheduledDate,
          existingInterviewId: existingInterview.id,
          existingStatus: existingInterview.status
        }
      });
    }

    console.log('✅ Slot disponible, procediendo con la creación...');

    // Insertar la nueva entrevista en la base de datos
    const insertQuery = `
      INSERT INTO interviews (
        application_id,
        interviewer_id,
        scheduled_date,
        duration_minutes,
        interview_type,
        interview_mode,
        status,
        notes,
        location,
        created_at,
        updated_at
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, NOW(), NOW())
      RETURNING id, application_id as "applicationId", interviewer_id as "interviewerId",
               scheduled_date as "scheduledDate", duration_minutes as duration,
               interview_type as type, interview_mode as mode, status, notes, location,
               created_at as "createdAt", updated_at as "updatedAt"
    `;

    const values = [
      parseInt(applicationId),
      parseInt(interviewerId),
      normalizedScheduledDate,
      parseInt(duration) || 60,
      type,
      mode || 'IN_PERSON', // Default to IN_PERSON if not provided
      'SCHEDULED',
      notes || '',
      location || 'Por asignar'
    ];

    console.log('🗄️ Ejecutando query INSERT:', insertQuery);
    console.log('📊 Valores:', values);

    const result = await client.query(insertQuery, values);
    const newInterview = result.rows[0];

    console.log('✅ Entrevista creada exitosamente:', newInterview);

    // 📊 PASO 2: Obtener datos completos para respuesta (studentName, interviewerName, etc.)
    const fullDataQuery = `
      SELECT
        i.id,
        i.application_id as "applicationId",
        i.interviewer_id as "interviewerId",
        i.scheduled_date as "scheduledDate",
        i.duration_minutes as duration,
        i.interview_type as type,
        i.status,
        i.notes,
        i.location,
        i.created_at as "createdAt",
        i.updated_at as "updatedAt",
        CONCAT(s.first_name, ' ', s.paternal_last_name, ' ', COALESCE(s.maternal_last_name, '')) as "studentName",
        g.full_name as "parentNames",
        s.grade_applied as "gradeApplied",
        CONCAT(u.first_name, ' ', u.last_name) as "interviewerName"
      FROM interviews i
      JOIN applications a ON i.application_id = a.id
      JOIN students s ON a.student_id = s.id
      JOIN guardians g ON a.guardian_id = g.id
      JOIN users u ON i.interviewer_id = u.id
      WHERE i.id = $1
    `;

    const fullDataResult = await client.query(fullDataQuery, [newInterview.id]);
    const interviewData = fullDataResult.rows[0];

    console.log('📋 Datos completos de entrevista para respuesta:', interviewData);

    // También agregar al array en memoria para compatibilidad con otros endpoints
    interviews.push({
      id: interviewData.id,
      applicationId: interviewData.applicationId,
      interviewerId: interviewData.interviewerId,
      scheduledDate: interviewData.scheduledDate,
      duration: interviewData.duration,
      type: interviewData.type,
      mode: mode || 'IN_PERSON',
      status: interviewData.status,
      location: interviewData.location,
      notes: interviewData.notes,
      createdAt: interviewData.createdAt,
      studentName: interviewData.studentName,
      interviewerName: interviewData.interviewerName,
      parentNames: interviewData.parentNames,
      gradeApplied: interviewData.gradeApplied
    });

    // PASO 3: Retornar respuesta inmediatamente
    res.status(201).json({
      id: parseInt(interviewData.id),
      applicationId: parseInt(interviewData.applicationId),
      studentName: interviewData.studentName,
      parentNames: interviewData.parentNames,
      gradeApplied: interviewData.gradeApplied,
      interviewerId: parseInt(interviewData.interviewerId),
      interviewerName: interviewData.interviewerName,
      status: interviewData.status,
      type: interviewData.type,
      mode: mode || 'IN_PERSON',
      scheduledDate: interviewData.scheduledDate,
      scheduledTime: scheduledTime || '00:00',
      duration: interviewData.duration,
      location: interviewData.location || '',
      virtualMeetingLink: '',
      notes: interviewData.notes || '',
      preparation: '',
      result: null,
      score: null,
      recommendations: '',
      followUpRequired: false,
      followUpNotes: '',
      createdAt: interviewData.createdAt,
      updatedAt: interviewData.updatedAt,
      completedAt: null,
      isUpcoming: true,
      isOverdue: false,
      canBeCompleted: interviewData.status === 'CONFIRMED' || interviewData.status === 'SCHEDULED',
      canBeEdited: interviewData.status !== 'COMPLETED' && interviewData.status !== 'CANCELLED',
      canBeCancelled: interviewData.status !== 'COMPLETED' && interviewData.status !== 'CANCELLED'
    });

    // 📧 PASO 4: Enviar notificaciones DESPUÉS de responder (en background)
    setImmediate(async () => {
      const notifClient = await dbPool.connect();
      try {
        // Obtener datos del apoderado y entrevistador para emails
        const guardiansQuery = await notifClient.query(
          `SELECT g.email, g.full_name, s.first_name as student_name, s.paternal_last_name as student_lastname
           FROM applications a
           JOIN guardians g ON a.guardian_id = g.id
           JOIN students s ON a.student_id = s.id
           WHERE a.id = $1`,
          [parseInt(applicationId)]
        );

        const interviewerQuery = await notifClient.query(
          `SELECT email, first_name, last_name
           FROM users
           WHERE id = $1`,
          [parseInt(interviewerId)]
        );

        if (guardiansQuery.rows.length > 0) {
          const guardian = guardiansQuery.rows[0];
          const interviewer = interviewerQuery.rows[0];

          // Formatear fecha/hora para mostrar
          const interviewDate = new Date(newInterview.scheduledDate);
          const dateStr = interviewDate.toLocaleDateString('es-CL');
          const timeStr = interviewDate.toLocaleTimeString('es-CL', { hour: '2-digit', minute: '2-digit' });

          // Enviar email al apoderado (sin await para no bloquear)
          axios.post('http://localhost:8085/api/notifications/send', {
            to: guardian.email,
            subject: `Entrevista Agendada - ${guardian.student_name} ${guardian.student_lastname}`,
            type: 'interview_scheduled_guardian',
            data: {
              guardianName: guardian.full_name,
              studentName: `${guardian.student_name} ${guardian.student_lastname}`,
              interviewType: type,
              interviewDate: dateStr,
              interviewTime: timeStr,
              location: newInterview.location,
              interviewerName: interviewer ? `${interviewer.first_name} ${interviewer.last_name}` : 'Por confirmar',
              duration: newInterview.duration
            }
          }).catch(err => console.error('Error enviando email a apoderado:', err.message));

          // Enviar email al entrevistador (sin await para no bloquear)
          if (interviewer) {
            axios.post('http://localhost:8085/api/notifications/send', {
              to: interviewer.email,
              subject: `Nueva Entrevista Asignada - ${guardian.student_name} ${guardian.student_lastname}`,
              type: 'interview_scheduled_interviewer',
              data: {
                interviewerName: `${interviewer.first_name} ${interviewer.last_name}`,
                studentName: `${guardian.student_name} ${guardian.student_lastname}`,
                guardianName: guardian.full_name,
                interviewType: type,
                interviewDate: dateStr,
                interviewTime: timeStr,
                location: newInterview.location,
                duration: newInterview.duration
              }
            }).catch(err => console.error('Error enviando email a entrevistador:', err.message));
          }

          console.log('✅ Notificaciones iniciadas en segundo plano');
        }
      } catch (emailError) {
        console.error('⚠️ Error preparando notificaciones:', emailError.message);
      } finally {
        notifClient.release();
      }
    });

  } catch (error) {
    console.error('❌ Error creando entrevista:', error);

    // Manejar errores específicos de constraint violations
    if (error.message && error.message.includes('check constraint')) {
      if (error.message.includes('interview_type_check')) {
        return res.status(400).json({
          success: false,
          error: 'Tipo de entrevista inválido',
          details: 'El tipo debe ser uno de: INDIVIDUAL, FAMILY, PSYCHOLOGICAL, ACADEMIC, BEHAVIORAL',
          validTypes: ['INDIVIDUAL', 'FAMILY', 'PSYCHOLOGICAL', 'ACADEMIC', 'BEHAVIORAL']
        });
      }
      if (error.message.includes('status_check')) {
        return res.status(400).json({
          success: false,
          error: 'Estado de entrevista inválido',
          details: 'El estado debe ser uno de: SCHEDULED, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW, RESCHEDULED'
        });
      }
    }

    // Manejar errores de foreign key (applicationId o interviewerId inválidos)
    if (error.message && error.message.includes('foreign key constraint')) {
      return res.status(400).json({
        success: false,
        error: 'ID de referencia inválido',
        details: 'Verifique que applicationId e interviewerId sean válidos'
      });
    }

    // Error genérico para casos no manejados
    res.status(500).json({
      success: false,
      error: 'Error interno del servidor al crear la entrevista',
      details: error.message
    });
  } finally {
    if (!clientReleased) {
      client.release();
    }
  }
});

// Endpoint para obtener entrevistas (con filtros)
app.get('/api/interviews', async (req, res) => {
  const { applicationId, interviewerId, status, date } = req.query;

  console.log('📋 Obteniendo entrevistas con filtros:', req.query);

  const client = await dbPool.connect();
  try {
    let query = `
      SELECT
        i.id,
        i.application_id,
        i.interviewer_id,
        i.scheduled_date,
        i.duration_minutes,
        i.status,
        i.interview_type,
        i.notes,
        i.created_at,
        s.first_name || ' ' || s.paternal_last_name as student_name,
        u.first_name || ' ' || u.last_name as interviewer_name
      FROM interviews i
      JOIN applications a ON a.id = i.application_id
      JOIN students s ON s.id = a.student_id
      JOIN users u ON u.id = i.interviewer_id
      WHERE 1=1
    `;

    const params = [];
    let paramCount = 0;

    if (applicationId) {
      paramCount++;
      query += ` AND i.application_id = $${paramCount}`;
      params.push(parseInt(applicationId));
    }

    if (interviewerId) {
      paramCount++;
      query += ` AND i.interviewer_id = $${paramCount}`;
      params.push(parseInt(interviewerId));
    }

    if (status) {
      paramCount++;
      query += ` AND i.status = $${paramCount}`;
      params.push(status);
    }

    if (date) {
      paramCount++;
      query += ` AND DATE(i.scheduled_date) = $${paramCount}`;
      params.push(date);
    }

    query += ` ORDER BY i.scheduled_date`;

    const result = await client.query(query, params);

    const formattedInterviews = result.rows.map(row => {
      // Formatear fecha en zona horaria local (Chile) sin conversión UTC
      const localDate = new Date(row.scheduled_date);
      const year = localDate.getFullYear();
      const month = String(localDate.getMonth() + 1).padStart(2, '0');
      const day = String(localDate.getDate()).padStart(2, '0');
      const hours = String(localDate.getHours()).padStart(2, '0');
      const minutes = String(localDate.getMinutes()).padStart(2, '0');
      const seconds = String(localDate.getSeconds()).padStart(2, '0');

      // Formato ISO local sin 'Z' para evitar problemas de zona horaria
      const localISOString = `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;

      return {
        id: row.id,
        applicationId: row.application_id,
        interviewerId: row.interviewer_id,
        scheduledDate: localISOString,
        duration: row.duration_minutes,
        type: row.interview_type, // ✅ Usar el tipo real de la BD
        mode: 'IN_PERSON',
        status: row.status,
        location: 'Sala de entrevistas',
        notes: row.notes || '',
        createdAt: row.created_at.toISOString(),
        studentName: row.student_name,
        interviewerName: row.interviewer_name
      };
    });

    // Traducir estados y tipos al español antes de devolver
    const translatedInterviews = translateInterviews(formattedInterviews);

    // Return raw array (unwrapped) for QA test compatibility
    res.json(translatedInterviews);
  } catch (error) {
    console.error('Database error:', error);
    // Fallback to mock data
    let filteredInterviews = [...interviews];

    if (applicationId) {
      filteredInterviews = filteredInterviews.filter(i => i.applicationId === parseInt(applicationId));
    }

    if (interviewerId) {
      filteredInterviews = filteredInterviews.filter(i => i.interviewerId === parseInt(interviewerId));
    }

    if (status) {
      filteredInterviews = filteredInterviews.filter(i => i.status === status);
    }

    if (date) {
      filteredInterviews = filteredInterviews.filter(i => i.scheduledDate.startsWith(date));
    }

    // Return raw array (unwrapped) for QA test compatibility
    res.json(filteredInterviews);
  } finally {
    client.release();
  }
});

// Endpoint específico para calendario - obtener entrevistas por rango de fechas
app.get('/api/interviews/calendar', async (req, res) => {
  const { startDate, endDate, interviewerId } = req.query;
  
  console.log('📅 Obteniendo entrevistas para calendario:', { startDate, endDate, interviewerId });

  const client = await dbPool.connect();
  try {
    let query = `
      SELECT
        i.id,
        i.application_id as "applicationId",
        i.interviewer_id as "interviewerId",
        i.scheduled_date as "scheduledDate",
        EXTRACT(HOUR FROM i.scheduled_date)::text || ':' ||
        LPAD(EXTRACT(MINUTE FROM i.scheduled_date)::text, 2, '0') as "scheduledTime",
        i.duration_minutes as duration,
        i.status,
        i.notes,
        i.created_at as "createdAt",
        i.updated_at as "updatedAt",
        s.first_name || ' ' || s.paternal_last_name as "studentName",
        s.paternal_last_name || ' ' || COALESCE(s.maternal_last_name, '') as "parentNames",
        s.grade_applied as "gradeApplied",
        u.first_name || ' ' || u.last_name as "interviewerName",
        'ACADEMIC' as type,
        'IN_PERSON' as mode,
        '' as location,
        '' as "virtualMeetingLink",
        '' as preparation,
        null as result,
        null as score,
        '' as recommendations,
        false as "followUpRequired",
        '' as "followUpNotes",
        null as "completedAt",
        CASE
          WHEN i.scheduled_date > NOW() THEN true
          ELSE false
        END as "isUpcoming",
        CASE
          WHEN i.scheduled_date < NOW() AND i.status != 'COMPLETED' THEN true
          ELSE false
        END as "isOverdue",
        CASE
          WHEN i.status = 'SCHEDULED' AND i.scheduled_date <= NOW() THEN true
          ELSE false
        END as "canBeCompleted",
        CASE
          WHEN i.status IN ('SCHEDULED', 'CONFIRMED') THEN true
          ELSE false
        END as "canBeEdited",
        CASE
          WHEN i.status IN ('SCHEDULED', 'CONFIRMED') THEN true
          ELSE false
        END as "canBeCancelled"
      FROM interviews i
      JOIN applications a ON a.id = i.application_id
      JOIN students s ON s.id = a.student_id
      JOIN users u ON u.id = i.interviewer_id
      WHERE 1=1
    `;

    const params = [];
    let paramCount = 0;

    // Filtrar por rango de fechas
    if (startDate) {
      paramCount++;
      query += ` AND DATE(i.scheduled_date) >= $${paramCount}`;
      params.push(startDate);
    }

    if (endDate) {
      paramCount++;
      query += ` AND DATE(i.scheduled_date) <= $${paramCount}`;
      params.push(endDate);
    }

    // Filtrar por entrevistador si se especifica
    if (interviewerId) {
      paramCount++;
      query += ` AND i.interviewer_id = $${paramCount}`;
      params.push(parseInt(interviewerId));
    }

    query += ` ORDER BY i.scheduled_date ASC`;

    console.log('🔍 Ejecutando query para calendario:', query);
    console.log('📋 Parámetros:', params);

    const result = await client.query(query, params);
    const calendarInterviews = result.rows;

    console.log(`📊 Encontradas ${calendarInterviews.length} entrevistas para calendario`);

    res.json(calendarInterviews);

  } catch (error) {
    console.error('❌ Error obteniendo entrevistas para calendario:', error);

    // Fallback con datos mock para desarrollo
    console.log('🔄 Usando datos mock de fallback para calendario');

    const mockInterviews = [
      {
        id: 1,
        applicationId: 4,
        interviewerId: 24,
        scheduledDate: '2025-09-10',
        scheduledTime: '10:00',
        duration: 60,
        status: 'SCHEDULED',
        studentName: 'María Elena González',
        parentNames: 'González Martínez',
        gradeApplied: 'TERCERO_BASICO',
        interviewerName: 'María Elena Sánchez Cortés',
        type: 'ACADEMIC',
        mode: 'IN_PERSON',
        location: 'Sala de Reuniones 1',
        virtualMeetingLink: '',
        notes: 'Primera entrevista académica',
        preparation: '',
        result: null,
        score: null,
        recommendations: '',
        followUpRequired: false,
        followUpNotes: '',
        createdAt: '2025-09-05T10:00:00Z',
        updatedAt: '2025-09-05T10:00:00Z',
        completedAt: null,
        isUpcoming: true,
        isOverdue: false,
        canBeCompleted: false,
        canBeEdited: true,
        canBeCancelled: true
      },
      {
        id: 2,
        applicationId: 5,
        interviewerId: 25,
        scheduledDate: '2025-09-12',
        scheduledTime: '14:30',
        duration: 45,
        status: 'CONFIRMED',
        studentName: 'Carlos Alberto Rodríguez',
        parentNames: 'Rodríguez Silva',
        gradeApplied: 'QUINTO_BASICO',
        interviewerName: 'Dr. Roberto Andrés Vega Muñoz',
        type: 'PSYCHOLOGICAL',
        mode: 'VIRTUAL',
        location: '',
        virtualMeetingLink: 'https://meet.google.com/abc-defg-hij',
        notes: 'Evaluación psicológica',
        preparation: 'Revisar historial académico',
        result: null,
        score: null,
        recommendations: '',
        followUpRequired: false,
        followUpNotes: '',
        createdAt: '2025-09-05T11:00:00Z',
        updatedAt: '2025-09-06T09:00:00Z',
        completedAt: null,
        isUpcoming: true,
        isOverdue: false,
        canBeCompleted: false,
        canBeEdited: true,
        canBeCancelled: true
      },
      {
        id: 3,
        applicationId: 6,
        interviewerId: 24,
        scheduledDate: '2025-09-15',
        scheduledTime: '11:00',
        duration: 60,
        status: 'SCHEDULED',
        studentName: 'Ana Isabel Morales',
        parentNames: 'Morales Pérez',
        gradeApplied: 'PRIMERO_BASICO',
        interviewerName: 'María Elena Sánchez Cortés',
        type: 'ACADEMIC',
        mode: 'IN_PERSON',
        location: 'Sala de Reuniones 2',
        virtualMeetingLink: '',
        notes: 'Entrevista de admisión',
        preparation: '',
        result: null,
        score: null,
        recommendations: '',
        followUpRequired: false,
        followUpNotes: '',
        createdAt: '2025-09-05T12:00:00Z',
        updatedAt: '2025-09-05T12:00:00Z',
        completedAt: null,
        isUpcoming: true,
        isOverdue: false,
        canBeCompleted: false,
        canBeEdited: true,
        canBeCancelled: true
      }
    ];

    // Aplicar filtros a los datos mock
    let filteredMockInterviews = mockInterviews;
    
    if (startDate) {
      filteredMockInterviews = filteredMockInterviews.filter(interview => 
        interview.scheduledDate >= startDate
      );
    }
    
    if (endDate) {
      filteredMockInterviews = filteredMockInterviews.filter(interview => 
        interview.scheduledDate <= endDate
      );
    }
    
    if (interviewerId) {
      filteredMockInterviews = filteredMockInterviews.filter(interview => 
        interview.interviewerId === parseInt(interviewerId)
      );
    }

    res.json(filteredMockInterviews);
  } finally {
    client.release();
  }
});

// Endpoint para obtener estadísticas de entrevistas (DEBE IR ANTES que el route parametrizado :id)
app.get('/api/interviews/statistics', (req, res) => {
  console.log('📊 Solicitud de estadísticas de entrevistas');
  
  // Calcular estadísticas basadas en las entrevistas existentes
  const totalInterviews = interviews.length;
  const scheduledInterviews = interviews.filter(i => i.status === 'SCHEDULED').length;
  const completedInterviews = interviews.filter(i => i.status === 'COMPLETED').length;
  const cancelledInterviews = interviews.filter(i => i.status === 'CANCELLED').length;
  
  // Estadísticas por tipo de entrevista
  const interviewsByType = {
    FAMILY: interviews.filter(i => i.type === 'FAMILY').length,
    PSYCHOLOGICAL: interviews.filter(i => i.type === 'PSYCHOLOGICAL').length,
    ACADEMIC: interviews.filter(i => i.type === 'ACADEMIC').length,
    ADMISSION: interviews.filter(i => i.type === 'ADMISSION').length
  };
  
  // Estadísticas por modo de entrevista
  const interviewsByMode = {
    IN_PERSON: interviews.filter(i => i.mode === 'IN_PERSON').length,
    VIRTUAL: interviews.filter(i => i.mode === 'VIRTUAL').length,
    PHONE: interviews.filter(i => i.mode === 'PHONE').length
  };
  
  // Estadísticas por entrevistador
  const interviewsByInterviewer = {};
  interviewers.forEach(interviewer => {
    const interviewerInterviews = interviews.filter(i => i.interviewerId === interviewer.id);
    interviewsByInterviewer[interviewer.id] = {
      name: interviewer.name,
      role: interviewer.role,
      totalInterviews: interviewerInterviews.length,
      scheduled: interviewerInterviews.filter(i => i.status === 'SCHEDULED').length,
      completed: interviewerInterviews.filter(i => i.status === 'COMPLETED').length,
      cancelled: interviewerInterviews.filter(i => i.status === 'CANCELLED').length
    };
  });
  
  // Estadísticas de tiempo promedio (simuladas)
  const averageDuration = interviews.length > 0 
    ? Math.round(interviews.reduce((sum, i) => sum + i.duration, 0) / interviews.length)
    : 60;
  
  // Estadísticas por mes (simuladas para el año actual)
  const currentYear = new Date().getFullYear();
  const monthlyStats = {};
  for (let month = 1; month <= 12; month++) {
    const monthName = new Date(currentYear, month - 1, 1).toLocaleDateString('es-ES', { month: 'long' });
    // Simular datos mensuales basados en las entrevistas existentes
    const baseCount = Math.max(0, interviews.length - Math.abs(month - 6)); // Más entrevistas en meses centrales
    monthlyStats[monthName] = {
      total: Math.max(0, baseCount + Math.floor(Math.random() * 5)),
      scheduled: Math.max(0, Math.floor((baseCount * 0.6) + Math.random() * 3)),
      completed: Math.max(0, Math.floor((baseCount * 0.3) + Math.random() * 2)),
      cancelled: Math.max(0, Math.floor((baseCount * 0.1) + Math.random() * 1))
    };
  }
  
  // Horarios más populares (simulado)
  const popularTimeSlots = [
    { time: '09:00', count: Math.floor(Math.random() * 10) + 5 },
    { time: '10:00', count: Math.floor(Math.random() * 15) + 8 },
    { time: '11:00', count: Math.floor(Math.random() * 12) + 6 },
    { time: '14:00', count: Math.floor(Math.random() * 8) + 4 },
    { time: '15:00', count: Math.floor(Math.random() * 10) + 7 },
    { time: '16:00', count: Math.floor(Math.random() * 6) + 3 }
  ].sort((a, b) => b.count - a.count);
  
  const statistics = {
    overview: {
      total: totalInterviews,
      scheduled: scheduledInterviews,
      completed: completedInterviews,
      cancelled: cancelledInterviews,
      completionRate: totalInterviews > 0 ? ((completedInterviews / totalInterviews) * 100).toFixed(1) : 0,
      cancellationRate: totalInterviews > 0 ? ((cancelledInterviews / totalInterviews) * 100).toFixed(1) : 0
    },
    byType: interviewsByType,
    byMode: interviewsByMode,
    byInterviewer: interviewsByInterviewer,
    timeAnalysis: {
      averageDuration: averageDuration,
      popularTimeSlots: popularTimeSlots,
      peakHours: {
        morning: popularTimeSlots.filter(slot => slot.time < '12:00').reduce((sum, slot) => sum + slot.count, 0),
        afternoon: popularTimeSlots.filter(slot => slot.time >= '12:00').reduce((sum, slot) => sum + slot.count, 0)
      }
    },
    monthlyTrends: monthlyStats,
    performance: {
      averageInterviewsPerInterviewer: interviewers.length > 0 
        ? (totalInterviews / interviewers.length).toFixed(1)
        : 0,
      mostActiveInterviewer: Object.values(interviewsByInterviewer).reduce((prev, current) => 
        (prev.totalInterviews > current.totalInterviews) ? prev : current, { name: 'N/A', totalInterviews: 0 }
      ),
      utilizationRate: ((totalInterviews / (interviewers.length * 30)) * 100).toFixed(1) // Asumiendo 30 slots disponibles por entrevistador por mes
    },
    lastUpdated: new Date().toISOString()
  };
  
  res.json({
    success: true,
    data: statistics
  });
});

// 🔒 ENDPOINT DE VALIDACIÓN PREVENTIVA: Usado por drag-and-drop del calendario
// Verifica si un slot específico está disponible para un entrevistador
// IMPORTANTE: Este endpoint debe estar ANTES de /api/interviews/:id para evitar conflictos de rutas
app.get('/api/interviews/availability', async (req, res) => {
  const { interviewerId, date, time, excludeInterviewId } = req.query;

  console.log('🔍 Verificando disponibilidad preventiva:', { interviewerId, date, time, excludeInterviewId });

  if (!interviewerId || !date || !time) {
    return res.status(400).json({
      success: false,
      error: 'Missing required parameters: interviewerId, date, time'
    });
  }

  const client = await dbPool.connect();
  try {
    // Construir fecha/hora en formato esperado por la DB
    const scheduledDateTime = `${date} ${time}:00`;

    // Query para buscar conflictos
    const query = `
      SELECT id, status, scheduled_date
      FROM interviews
      WHERE interviewer_id = $1
        AND scheduled_date = $2
        AND status IN ('SCHEDULED', 'CONFIRMED', 'IN_PROGRESS')
        ${excludeInterviewId ? 'AND id != $3' : ''}
    `;

    const params = [parseInt(interviewerId), scheduledDateTime];
    if (excludeInterviewId) {
      params.push(parseInt(excludeInterviewId));
    }

    const result = await client.query(query, params);
    const isAvailable = result.rows.length === 0;

    console.log(`${isAvailable ? '✅' : '❌'} Slot ${isAvailable ? 'disponible' : 'ocupado'} para interviewer ${interviewerId} en ${scheduledDateTime}`);

    if (!isAvailable) {
      console.log('📋 Conflicto encontrado:', result.rows[0]);
    }

    // Retornar boolean directo (true = disponible, false = ocupado)
    res.json(isAvailable);
  } catch (error) {
    console.error('Error verificando disponibilidad preventiva:', error);
    res.status(500).json({
      success: false,
      error: 'Internal server error',
      message: error.message
    });
  } finally {
    client.release();
  }
});

// Endpoint para obtener una entrevista específica
app.get('/api/interviews/:id', (req, res) => {
  const interviewId = parseInt(req.params.id);
  const interview = interviews.find(i => i.id === interviewId);

  if (!interview) {
    return res.status(404).json({
      success: false,
      error: 'Entrevista no encontrada'
    });
  }

  res.json({
    success: true,
    data: interview
  });
});

// Endpoint para actualizar una entrevista
app.put('/api/interviews/:id', async (req, res) => {
  const interviewId = parseInt(req.params.id);

  console.log('✏️ Actualizando entrevista:', interviewId, req.body);

  const client = await dbPool.connect();
  try {
    // Primero verificar que la entrevista existe en la BD
    const checkQuery = 'SELECT id FROM interviews WHERE id = $1';
    const checkResult = await client.query(checkQuery, [interviewId]);

    if (checkResult.rows.length === 0) {
      return res.status(404).json({
        success: false,
        error: 'Entrevista no encontrada'
      });
    }

    // Construir la consulta de actualización dinámicamente
    const updateFields = [];
    const updateValues = [];
    let paramCount = 0;

    // Manejar scheduledDate y scheduledTime
    if (req.body.scheduledDate) {
      let fullDateTime;
      if (req.body.scheduledTime) {
        // Combinar fecha y hora si vienen separadas
        fullDateTime = `${req.body.scheduledDate}T${req.body.scheduledTime}:00`;
      } else {
        fullDateTime = req.body.scheduledDate;
      }

      const inputDate = new Date(fullDateTime);
      if (!isNaN(inputDate.getTime())) {
        // Formatear para PostgreSQL
        const year = inputDate.getFullYear();
        const month = String(inputDate.getMonth() + 1).padStart(2, '0');
        const day = String(inputDate.getDate()).padStart(2, '0');
        const hours = String(inputDate.getHours()).padStart(2, '0');
        const minutes = String(inputDate.getMinutes()).padStart(2, '0');
        const seconds = String(inputDate.getSeconds()).padStart(2, '0');
        const normalizedDate = `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;

        paramCount++;
        updateFields.push(`scheduled_date = $${paramCount}`);
        updateValues.push(normalizedDate);

        console.log('📅 Actualizando fecha:', {
          date: req.body.scheduledDate,
          time: req.body.scheduledTime,
          combined: fullDateTime,
          normalized: normalizedDate
        });
      }
    }

    if (req.body.duration) {
      paramCount++;
      updateFields.push(`duration_minutes = $${paramCount}`);
      updateValues.push(req.body.duration);
    }

    if (req.body.type) {
      paramCount++;
      updateFields.push(`interview_type = $${paramCount}`);
      updateValues.push(req.body.type);
    }

    if (req.body.status) {
      paramCount++;
      updateFields.push(`status = $${paramCount}`);
      updateValues.push(req.body.status);
    }

    if (req.body.notes) {
      paramCount++;
      updateFields.push(`notes = $${paramCount}`);
      updateValues.push(req.body.notes);
    }

    if (req.body.interviewerId) {
      paramCount++;
      updateFields.push(`interviewer_id = $${paramCount}`);
      updateValues.push(req.body.interviewerId);
    }

    // Agregar updated_at
    paramCount++;
    updateFields.push(`updated_at = $${paramCount}`);
    updateValues.push(new Date());

    // Agregar el ID al final para el WHERE
    paramCount++;
    updateValues.push(interviewId);

    if (updateFields.length === 1) { // Solo updated_at
      return res.status(400).json({
        success: false,
        error: 'No hay campos para actualizar'
      });
    }

    const updateQuery = `
      UPDATE interviews
      SET ${updateFields.join(', ')}
      WHERE id = $${paramCount}
      RETURNING *
    `;

    console.log('📝 Ejecutando actualización:', updateQuery, updateValues);
    const result = await client.query(updateQuery, updateValues);

    // Formatear fecha en zona horaria local (consistente con GET)
    const localDate = new Date(result.rows[0].scheduled_date);
    const year = localDate.getFullYear();
    const month = String(localDate.getMonth() + 1).padStart(2, '0');
    const day = String(localDate.getDate()).padStart(2, '0');
    const hours = String(localDate.getHours()).padStart(2, '0');
    const minutes = String(localDate.getMinutes()).padStart(2, '0');
    const seconds = String(localDate.getSeconds()).padStart(2, '0');
    const localISOString = `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;

    res.json({
      success: true,
      data: {
        id: result.rows[0].id,
        applicationId: result.rows[0].application_id,
        interviewerId: result.rows[0].interviewer_id,
        scheduledDate: localISOString,
        duration: result.rows[0].duration_minutes,
        type: result.rows[0].interview_type,
        status: result.rows[0].status,
        notes: result.rows[0].notes || '',
        updatedAt: result.rows[0].updated_at.toISOString()
      },
      message: 'Entrevista actualizada exitosamente'
    });

  } catch (error) {
    console.error('❌ Error actualizando entrevista en BD:', error);
    res.status(500).json({
      success: false,
      error: 'Error interno del servidor al actualizar la entrevista'
    });
  } finally {
    client.release();
  }
});

// Endpoint para cancelar una entrevista
app.delete('/api/interviews/:id', (req, res) => {
  const interviewId = parseInt(req.params.id);
  const interviewIndex = interviews.findIndex(i => i.id === interviewId);

  if (interviewIndex === -1) {
    return res.status(404).json({
      success: false,
      error: 'Entrevista no encontrada'
    });
  }

  console.log('❌ Cancelando entrevista:', interviewId);

  // Cambiar estado a cancelado en lugar de eliminar
  interviews[interviewIndex].status = 'CANCELLED';
  interviews[interviewIndex].cancelledAt = new Date().toISOString();

  res.json({
    success: true,
    message: 'Entrevista cancelada exitosamente'
  });
});

// GET interviews by application ID - endpoint específico para obtener entrevistas de una aplicación
app.get('/api/interviews/application/:applicationId', async (req, res) => {
  const applicationId = parseInt(req.params.applicationId);
  
  console.log('📋 Obteniendo entrevistas para aplicación:', applicationId);
  
  if (!applicationId || isNaN(applicationId)) {
    return res.status(400).json({
      success: false,
      error: 'ID de aplicación inválido'
    });
  }
  
  const client = await dbPool.connect();
  try {
    // Buscar entrevistas en la base de datos para esta aplicación
    const query = `
      SELECT
        i.id,
        i.application_id as "applicationId",
        i.interviewer_id as "interviewerId",
        u.first_name || ' ' || u.last_name as "interviewerName",
        i.scheduled_date as "scheduledDate",
        i.duration_minutes as duration,
        i.interview_type as type,
        i.status,
        i.notes,
        i.location,
        i.created_at as "createdAt",
        i.updated_at as "updatedAt",
        'IN_PERSON' as mode,
        false as "followUpRequired",
        s.first_name || ' ' || s.paternal_last_name as "studentName",
        'Padre/Madre' as "parentNames",
        s.grade_applied as "gradeApplied"
      FROM interviews i
      LEFT JOIN users u ON u.id = i.interviewer_id
      LEFT JOIN applications a ON a.id = i.application_id
      LEFT JOIN students s ON s.id = a.student_id
      WHERE i.application_id = $1
      ORDER BY i.scheduled_date DESC
    `;

    const result = await client.query(query, [applicationId]);
    
    console.log(`✅ Encontradas ${result.rows.length} entrevistas para aplicación ${applicationId}`);
    
    // Mapear los datos para que coincidan con la interfaz del frontend
    const interviews = result.rows.map(row => ({
      id: row.id,
      applicationId: row.applicationId,
      studentName: row.studentName || 'Estudiante desconocido',
      parentNames: row.parentNames || 'Padre/Madre',
      gradeApplied: row.gradeApplied || 'Sin especificar',
      interviewerId: row.interviewerId,
      interviewerName: row.interviewerName || 'Entrevistador asignado',
      status: row.status,
      type: row.type,
      mode: row.mode,
      scheduledDate: row.scheduledDate ? row.scheduledDate.toISOString().split('T')[0] : new Date().toISOString().split('T')[0],
      scheduledTime: row.scheduledDate ? row.scheduledDate.toISOString().split('T')[1].split('.')[0] : '10:00:00',
      duration: row.duration || 60,
      location: row.location || 'Sala de entrevistas',
      notes: row.notes,
      followUpRequired: row.followUpRequired,
      createdAt: row.createdAt ? row.createdAt.toISOString() : new Date().toISOString(),
      updatedAt: row.updatedAt ? row.updatedAt.toISOString() : new Date().toISOString(),
      isUpcoming: row.status === 'SCHEDULED',
      isOverdue: false,
      canBeCompleted: row.status === 'SCHEDULED',
      canBeEdited: row.status === 'SCHEDULED',
      canBeCancelled: row.status === 'SCHEDULED'
    }));
    
    res.json(interviews);

  } catch (error) {
    console.error('❌ Error obteniendo entrevistas por aplicación:', error);
    res.status(500).json({
      success: false,
      error: 'Error interno del servidor al obtener las entrevistas',
      details: error.message
    });
  } finally {
    client.release();
  }
});

// [REMOVED - Duplicate statistics route, kept the one at line 449 before parameterized routes]

// GET my evaluations (for professors)
app.get('/api/evaluations/my-evaluations', async (req, res) => {
  try {
    // Extract user ID from Authorization header (JWT token)
    const authHeader = req.headers.authorization;
    let evaluatorId = null;

    if (authHeader && authHeader.startsWith('Bearer ')) {
      const token = authHeader.substring(7);
      try {
        // Decode JWT token (mock - in production use proper JWT verification)
        const base64Payload = token.split('.')[1];
        const payload = JSON.parse(Buffer.from(base64Payload, 'base64').toString());
        evaluatorId = payload.userId;
      } catch (err) {
        console.error('Error decoding token:', err);
      }
    }

    if (!evaluatorId) {
      return res.status(401).json({ error: 'Authentication required' });
    }

    // Get real evaluations from database filtered by evaluator
    const client = await dbPool.connect();
    const evaluationsQuery = await client.query(`
      SELECT
        e.id,
        e.application_id,
        e.evaluation_type,
        e.status,
        e.score,
        e.grade,
        e.observations,
        e.strengths,
        e.areas_for_improvement,
        e.recommendations,
        e.evaluation_date,
        e.completion_date,
        e.created_at,
        e.updated_at,
        s.first_name,
        s.paternal_last_name,
        s.maternal_last_name,
        s.grade_applied,
        s.rut as student_rut,
        s.email as student_email,
        u.first_name as evaluator_first_name,
        u.last_name as evaluator_last_name,
        u.subject,
        u.role
      FROM evaluations e
      JOIN applications a ON a.id = e.application_id
      JOIN students s ON s.id = a.student_id
      JOIN users u ON u.id = e.evaluator_id
      WHERE e.evaluator_id = $1
      ORDER BY e.created_at DESC
      LIMIT 50
    `, [evaluatorId]);
    client.release();

    const evaluations = evaluationsQuery.rows.map(row => ({
      id: row.id,
      applicationId: row.application_id,
      studentName: `${row.first_name} ${row.paternal_last_name} ${row.maternal_last_name || ''}`.trim(),
      studentFirstName: row.first_name,
      studentLastName: `${row.paternal_last_name} ${row.maternal_last_name || ''}`.trim(),
      studentRut: row.student_rut,
      studentEmail: row.student_email,
      grade: row.grade_applied,
      evaluationType: row.evaluation_type,
      type: row.evaluation_type || 'ACADEMIC',
      status: row.status || 'PENDING',
      assignedAt: row.created_at,
      evaluationDate: row.evaluation_date,
      completedAt: row.completion_date || row.updated_at,
      dueDate: new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString(), // 14 days from now
      subject: row.subject || getSubjectFromEvaluationType(row.evaluation_type),
      subjectDisplay: getSubjectDisplayName(row.evaluation_type, row.subject),
      score: row.score,
      scoreGrade: row.grade,
      observations: row.observations,
      strengths: row.strengths,
      areasForImprovement: row.areas_for_improvement,
      recommendations: row.recommendations,
      evaluator: {
        firstName: row.evaluator_first_name,
        lastName: row.evaluator_last_name,
        subject: row.subject,
        role: row.role
      }
    }));

    res.json(evaluations);
  } catch (error) {
    console.error('Database error:', error);
    // Fallback to mock data
    const mockEvaluations = [
      {
        id: 1,
        applicationId: 1,
        studentName: 'Juan Pérez',
        grade: '5° Básico',
        type: 'ACADEMIC',
        status: 'PENDING',
        assignedAt: '2025-09-01T10:00:00Z',
        dueDate: '2025-09-15T23:59:59Z',
        subject: 'Matemáticas'
      },
      {
        id: 2,
        applicationId: 2,
        studentName: 'María González',
        grade: '6° Básico',
        type: 'ACADEMIC',
        status: 'COMPLETED',
        assignedAt: '2025-08-25T10:00:00Z',
        completedAt: '2025-09-02T14:30:00Z',
        dueDate: '2025-09-10T23:59:59Z',
        subject: 'Matemáticas',
        score: 85,
        observations: 'Excelente desempeño en resolución de problemas'
      }
    ];
    res.json(mockEvaluations);
  }
});

// ==================================================
// INTERVIEWER SCHEDULE MANAGEMENT ENDPOINTS
// ==================================================

// Get schedules for an interviewer by year
app.get('/api/interviewer-schedules/interviewer/:interviewerId/year/:year', async (req, res) => {
  const client = await dbPool.connect();
  try {
    const { interviewerId, year } = req.params;
    console.log(`📅 Getting schedules for interviewer ${interviewerId} in year ${year}`);

    const query = `
      SELECT
        s.id,
        s.interviewer_id,
        s.day_of_week,
        s.start_time,
        s.end_time,
        s.year,
        s.schedule_type,
        s.is_active,
        s.notes,
        s.created_at,
        s.updated_at,
        u.first_name,
        u.last_name,
        u.email,
        u.role
      FROM interviewer_schedules s
      JOIN users u ON u.id = s.interviewer_id
      WHERE s.interviewer_id = $1 AND s.year = $2 AND s.is_active = true
      ORDER BY
        CASE s.day_of_week
          WHEN 'MONDAY' THEN 1
          WHEN 'TUESDAY' THEN 2
          WHEN 'WEDNESDAY' THEN 3
          WHEN 'THURSDAY' THEN 4
          WHEN 'FRIDAY' THEN 5
          WHEN 'SATURDAY' THEN 6
          WHEN 'SUNDAY' THEN 7
        END,
        s.start_time
    `;

    const result = await client.query(query, [interviewerId, year]);

    const schedules = result.rows.map(row => ({
      id: row.id,
      interviewer: {
        id: row.interviewer_id,
        firstName: row.first_name,
        lastName: row.last_name,
        email: row.email,
        role: row.role
      },
      dayOfWeek: row.day_of_week,
      startTime: row.start_time,
      endTime: row.end_time,
      year: row.year,
      scheduleType: row.schedule_type,
      isActive: row.is_active,
      notes: row.notes,
      createdAt: row.created_at,
      updatedAt: row.updated_at
    }));

    console.log(`✅ Found ${schedules.length} schedules for interviewer ${interviewerId}`);
    res.json(schedules);

  } catch (error) {
    console.error('❌ Error getting interviewer schedules:', error);
    res.status(500).json({ error: 'Internal server error', message: error.message });
  } finally {
    client.release();
  }
});

// Create a new schedule
app.post('/api/interviewer-schedules', async (req, res) => {
  const client = await dbPool.connect();
  try {
    const { interviewer, dayOfWeek, startTime, endTime, year, scheduleType, isActive, notes } = req.body;

    console.log(`📝 Creating new schedule for interviewer ${interviewer.id}:`, {
      dayOfWeek, startTime, endTime, year, scheduleType, isActive
    });

    const query = `
      INSERT INTO interviewer_schedules
      (interviewer_id, day_of_week, start_time, end_time, year, schedule_type, is_active, notes, created_at, updated_at)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, NOW(), NOW())
      RETURNING *
    `;

    const result = await client.query(query, [
      interviewer.id,
      dayOfWeek,
      startTime,
      endTime,
      year,
      scheduleType || 'RECURRING',
      isActive !== undefined ? isActive : true,
      notes || ''
    ]);

    const newSchedule = {
      id: result.rows[0].id,
      interviewer: {
        id: interviewer.id,
        firstName: interviewer.firstName,
        lastName: interviewer.lastName,
        email: interviewer.email,
        role: interviewer.role
      },
      dayOfWeek: result.rows[0].day_of_week,
      startTime: result.rows[0].start_time,
      endTime: result.rows[0].end_time,
      year: result.rows[0].year,
      scheduleType: result.rows[0].schedule_type,
      isActive: result.rows[0].is_active,
      notes: result.rows[0].notes,
      createdAt: result.rows[0].created_at,
      updatedAt: result.rows[0].updated_at
    };

    console.log(`✅ Schedule created with ID ${newSchedule.id}`);
    res.status(201).json(newSchedule);

  } catch (error) {
    console.error('❌ Error creating schedule:', error);
    res.status(500).json({ error: 'Internal server error', message: error.message });
  } finally {
    client.release();
  }
});

// Update an existing schedule
app.put('/api/interviewer-schedules/:scheduleId', async (req, res) => {
  const client = await dbPool.connect();
  try {
    const { scheduleId } = req.params;
    const { dayOfWeek, startTime, endTime, scheduleType, isActive, notes } = req.body;

    console.log(`📝 Updating schedule ${scheduleId}:`, {
      dayOfWeek, startTime, endTime, scheduleType, isActive
    });

    const query = `
      UPDATE interviewer_schedules
      SET day_of_week = $1, start_time = $2, end_time = $3,
          schedule_type = $4, is_active = $5, notes = $6, updated_at = NOW()
      WHERE id = $7
      RETURNING *
    `;

    const result = await client.query(query, [
      dayOfWeek, startTime, endTime, scheduleType, isActive, notes || '', scheduleId
    ]);

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Schedule not found' });
    }

    // Get interviewer details
    const interviewerQuery = `
      SELECT first_name, last_name, email, role
      FROM users
      WHERE id = $1
    `;
    const interviewerResult = await client.query(interviewerQuery, [result.rows[0].interviewer_id]);

    const updatedSchedule = {
      id: result.rows[0].id,
      interviewer: {
        id: result.rows[0].interviewer_id,
        firstName: interviewerResult.rows[0].first_name,
        lastName: interviewerResult.rows[0].last_name,
        email: interviewerResult.rows[0].email,
        role: interviewerResult.rows[0].role
      },
      dayOfWeek: result.rows[0].day_of_week,
      startTime: result.rows[0].start_time,
      endTime: result.rows[0].end_time,
      year: result.rows[0].year,
      scheduleType: result.rows[0].schedule_type,
      isActive: result.rows[0].is_active,
      notes: result.rows[0].notes,
      createdAt: result.rows[0].created_at,
      updatedAt: result.rows[0].updated_at
    };

    console.log(`✅ Schedule ${scheduleId} updated successfully`);
    res.json(updatedSchedule);

  } catch (error) {
    console.error('❌ Error updating schedule:', error);
    res.status(500).json({ error: 'Internal server error', message: error.message });
  } finally {
    client.release();
  }
});

// Delete a schedule
app.delete('/api/interviewer-schedules/:scheduleId', async (req, res) => {
  const client = await dbPool.connect();
  try {
    const { scheduleId } = req.params;

    console.log(`🗑️ Deleting schedule ${scheduleId}`);

    const query = 'DELETE FROM interviewer_schedules WHERE id = $1';
    const result = await client.query(query, [scheduleId]);

    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'Schedule not found' });
    }

    console.log(`✅ Schedule ${scheduleId} deleted successfully`);
    res.status(204).send();

  } catch (error) {
    console.error('❌ Error deleting schedule:', error);
    res.status(500).json({ error: 'Internal server error', message: error.message });
  } finally {
    client.release();
  }
});

// Get all schedules for an interviewer
app.get('/api/interviewer-schedules/interviewer/:interviewerId', async (req, res) => {
  const client = await dbPool.connect();
  try {
    const { interviewerId } = req.params;
    const currentYear = new Date().getFullYear();

    console.log(`📅 Getting all schedules for interviewer ${interviewerId}`);

    const query = `
      SELECT
        s.id,
        s.interviewer_id,
        s.day_of_week,
        s.start_time,
        s.end_time,
        s.year,
        s.schedule_type,
        s.is_active,
        s.notes,
        s.created_at,
        s.updated_at,
        u.first_name,
        u.last_name,
        u.email,
        u.role
      FROM interviewer_schedules s
      JOIN users u ON u.id = s.interviewer_id
      WHERE s.interviewer_id = $1 AND s.year = $2
      ORDER BY
        CASE s.day_of_week
          WHEN 'MONDAY' THEN 1
          WHEN 'TUESDAY' THEN 2
          WHEN 'WEDNESDAY' THEN 3
          WHEN 'THURSDAY' THEN 4
          WHEN 'FRIDAY' THEN 5
          WHEN 'SATURDAY' THEN 6
          WHEN 'SUNDAY' THEN 7
        END,
        s.start_time
    `;

    const result = await client.query(query, [interviewerId, currentYear]);

    const schedules = result.rows.map(row => ({
      id: row.id,
      interviewer: {
        id: row.interviewer_id,
        firstName: row.first_name,
        lastName: row.last_name,
        email: row.email,
        role: row.role
      },
      dayOfWeek: row.day_of_week,
      startTime: row.start_time,
      endTime: row.end_time,
      year: row.year,
      scheduleType: row.schedule_type,
      isActive: row.is_active,
      notes: row.notes,
      createdAt: row.created_at,
      updatedAt: row.updated_at
    }));

    console.log(`✅ Found ${schedules.length} schedules for interviewer ${interviewerId}`);
    res.json(schedules);

  } catch (error) {
    console.error('❌ Error getting interviewer schedules:', error);
    res.status(500).json({ error: 'Internal server error', message: error.message });
  } finally {
    client.release();
  }
});

// =================================================================
// COMPREHENSIVE AVAILABILITY AND INTERVIEW MANAGEMENT ENDPOINTS
// =================================================================

// Helper function to check if a time is within a time range
function isTimeInRange(targetTime, startTime, endTime) {
  // Convert all to minutes for comparison
  const timeToMinutes = (time) => {
    const [hours, minutes] = time.split(':').map(Number);
    return hours * 60 + minutes;
  };

  const target = timeToMinutes(targetTime);
  const start = timeToMinutes(startTime);
  const end = timeToMinutes(endTime);

  return target >= start && target < end;
}

// Helper function to get day of week from date
function getDayOfWeek(dateString) {
  const days = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
  const date = new Date(dateString);
  return days[date.getDay()];
}
// Check real interviewer availability based on configured schedules and existing interviews
app.get('/api/interviews/availability/check', async (req, res) => {
  const { date, time, duration = 60, interviewType } = req.query;

  console.log('🔍 Checking availability:', { date, time, duration, interviewType });

  if (!date || !time) {
    return res.status(400).json({
      success: false,
      error: 'Date and time are required parameters'
    });
  }

  const client = await dbPool.connect();
  try {
    const dayOfWeek = getDayOfWeek(date);
    const currentYear = new Date().getFullYear();

    console.log(`📅 Looking for ${dayOfWeek} schedules at ${time} for year ${currentYear}`);

    // Step 1: Find all interviewers with matching schedules for the day/time
    const scheduleQuery = `
      SELECT DISTINCT
        s.interviewer_id,
        u.first_name,
        u.last_name,
        u.email,
        u.role,
        u.educational_level,
        u.subject,
        s.start_time,
        s.end_time,
        s.day_of_week
      FROM interviewer_schedules s
      JOIN users u ON u.id = s.interviewer_id
      WHERE s.day_of_week = $1
        AND s.year = $2
        AND s.is_active = true
        AND s.schedule_type = 'RECURRING'
        AND u.role IN ('TEACHER', 'COORDINATOR', 'PSYCHOLOGIST', 'CYCLE_DIRECTOR')
    `;

    const scheduleResult = await queryWithCircuitBreaker.fire(client, scheduleQuery, [dayOfWeek, currentYear]);
    console.log(`📋 Found ${scheduleResult.rows.length} interviewer schedules for ${dayOfWeek}`);

    // Step 2: Filter by time availability
    const availableInterviewers = scheduleResult.rows.filter(row => {
      const isInTimeRange = isTimeInRange(time, row.start_time, row.end_time);
      console.log(`⏰ ${row.first_name} ${row.last_name}: ${row.start_time}-${row.end_time}, ${time} in range? ${isInTimeRange}`);
      return isInTimeRange;
    });

    console.log(`✅ ${availableInterviewers.length} interviewers available at ${time}`);

    // Step 3: Check for conflicts with existing interviews
    const conflictQuery = `
      SELECT interviewer_id, scheduled_date, duration_minutes
      FROM interviews
      WHERE DATE(scheduled_date) = $1
        AND status NOT IN ('CANCELLED', 'NO_SHOW')
        AND interviewer_id = ANY($2)
    `;

    const interviewerIds = availableInterviewers.map(i => i.interviewer_id);

    let finalAvailable = availableInterviewers;

    if (interviewerIds.length > 0) {
      const conflictResult = await queryWithCircuitBreaker.fire(client, conflictQuery, [date, interviewerIds]);

      console.log(`📊 Found ${conflictResult.rows.length} existing interviews on ${date}`);

      // Filter out interviewers with time conflicts
      finalAvailable = availableInterviewers.filter(interviewer => {
        const conflicts = conflictResult.rows.filter(interview =>
          interview.interviewer_id === interviewer.interviewer_id
        );

        for (const conflict of conflicts) {
          const conflictDate = new Date(conflict.scheduled_date);
          const conflictStart = conflictDate.getHours() * 60 + conflictDate.getMinutes();
          const conflictEnd = conflictStart + conflict.duration_minutes;

          const [requestHour, requestMinute] = time.split(':').map(Number);
          const requestStart = requestHour * 60 + requestMinute;
          const requestEnd = requestStart + parseInt(duration);

          // Check for overlap
          if (requestStart < conflictEnd && requestEnd > conflictStart) {
            console.log(`❌ Conflict for ${interviewer.first_name}: existing ${conflictStart}-${conflictEnd}, requested ${requestStart}-${requestEnd}`);
            return false;
          }
        }
        return true;
      });
    }

    // Step 4: Filter by interview type if specified
    if (interviewType) {
      const typeMapping = {
        'PSYCHOLOGICAL': ['PSYCHOLOGIST'],
        'ACADEMIC': ['TEACHER', 'COORDINATOR', 'CYCLE_DIRECTOR'],
        'INDIVIDUAL': ['TEACHER', 'COORDINATOR', 'PSYCHOLOGIST', 'CYCLE_DIRECTOR'],
        'FAMILY': ['COORDINATOR', 'PSYCHOLOGIST', 'CYCLE_DIRECTOR'],
        'BEHAVIORAL': ['PSYCHOLOGIST', 'CYCLE_DIRECTOR']
      };

      const allowedRoles = typeMapping[interviewType] || ['TEACHER', 'COORDINATOR', 'PSYCHOLOGIST', 'CYCLE_DIRECTOR'];
      finalAvailable = finalAvailable.filter(interviewer =>
        allowedRoles.includes(interviewer.role)
      );
    }

    console.log(`🎯 Final available interviewers: ${finalAvailable.length}`);

    const responseData = {
      date,
      time,
      duration: parseInt(duration),
      interviewType: interviewType || 'ANY',
      availableInterviewers: finalAvailable.map(interviewer => ({
        id: interviewer.interviewer_id,
        name: `${interviewer.first_name} ${interviewer.last_name}`,
        firstName: interviewer.first_name,
        lastName: interviewer.last_name,
        email: interviewer.email,
        role: interviewer.role,
        educationalLevel: interviewer.educational_level,
        subject: interviewer.subject,
        availableFrom: interviewer.start_time,
        availableUntil: interviewer.end_time
      })),
      count: finalAvailable.length,
      message: finalAvailable.length > 0
        ? `${finalAvailable.length} entrevistadores disponibles`
        : 'No hay entrevistadores disponibles en este horario'
    };

    res.json({
      success: true,
      data: responseData
    });

  } catch (error) {
    console.error('❌ Error checking availability:', error);
    res.status(500).json({
      success: false,
      error: 'Error checking availability',
      details: error.message
    });
  } finally {
    client.release();
  }
});

// Get complete student details with all 4 interviews
app.get('/api/interviews/student/:applicationId/complete', async (req, res) => {
  const { applicationId } = req.params;

  console.log('📋 Getting complete student details for application:', applicationId);

  const client = await dbPool.connect();
  try {
    // Get application with student details
    const applicationQuery = `
      SELECT
        a.id as application_id,
        a.status as application_status,
        a.submission_date,
        a.application_year,
        s.id as student_id,
        s.first_name,
        s.paternal_last_name,
        s.maternal_last_name,
        s.rut,
        s.birth_date,
        s.grade_applied,
        s.school_applied,
        s.current_school,
        s.email as student_email,
        s.address
      FROM applications a
      JOIN students s ON s.id = a.student_id
      WHERE a.id = $1
    `;

    const applicationResult = await client.query(applicationQuery, [applicationId]);

    if (applicationResult.rows.length === 0) {
      return res.status(404).json({
        success: false,
        error: 'Application not found'
      });
    }

    const application = applicationResult.rows[0];

    // Get all interviews for this application
    const interviewsQuery = `
      SELECT
        i.id,
        i.interview_type,
        i.status,
        i.scheduled_date,
        i.duration_minutes,
        i.location,
        i.notes,
        i.interview_mode,
        i.meeting_link,
        i.room,
        i.follow_up_required,
        u.first_name as interviewer_first_name,
        u.last_name as interviewer_last_name,
        u.email as interviewer_email,
        u.role as interviewer_role
      FROM interviews i
      JOIN users u ON u.id = i.interviewer_id
      WHERE i.application_id = $1
      ORDER BY i.scheduled_date
    `;

    const interviewsResult = await client.query(interviewsQuery, [applicationId]);

    // Define required interview types
    const requiredInterviewTypes = ['INDIVIDUAL', 'FAMILY', 'PSYCHOLOGICAL', 'ACADEMIC'];

    const interviews = interviewsResult.rows.map(row => ({
      id: row.id,
      type: row.interview_type,
      status: row.status,
      scheduledDate: row.scheduled_date,
      duration: row.duration_minutes,
      location: row.location,
      mode: row.interview_mode,
      meetingLink: row.meeting_link,
      room: row.room,
      notes: row.notes,
      followUpRequired: row.follow_up_required,
      interviewer: {
        name: `${row.interviewer_first_name} ${row.interviewer_last_name}`,
        email: row.interviewer_email,
        role: row.interviewer_role
      }
    }));

    // Check which interview types are missing
    const existingTypes = interviews.map(i => i.type);
    const missingTypes = requiredInterviewTypes.filter(type => !existingTypes.includes(type));

    const studentDetails = {
      applicationId: application.application_id,
      applicationStatus: application.application_status,
      submissionDate: application.submission_date,
      applicationYear: application.application_year,
      student: {
        id: application.student_id,
        firstName: application.first_name,
        lastName: `${application.paternal_last_name} ${application.maternal_last_name}`,
        fullName: `${application.first_name} ${application.paternal_last_name} ${application.maternal_last_name}`,
        rut: application.rut,
        birthDate: application.birth_date,
        gradeApplied: application.grade_applied,
        schoolApplied: application.school_applied,
        currentSchool: application.current_school,
        email: application.student_email,
        address: application.address
      },
      interviews: {
        completed: interviews,
        missing: missingTypes,
        total: interviews.length,
        required: requiredInterviewTypes.length,
        isComplete: missingTypes.length === 0
      },
      interviewProgress: {
        individual: interviews.find(i => i.type === 'INDIVIDUAL') || null,
        family: interviews.find(i => i.type === 'FAMILY') || null,
        psychological: interviews.find(i => i.type === 'PSYCHOLOGICAL') || null,
        academic: interviews.find(i => i.type === 'ACADEMIC') || null
      }
    };

    console.log(`✅ Student details: ${interviews.length}/4 interviews completed`);

    res.json({
      success: true,
      data: studentDetails
    });

  } catch (error) {
    console.error('❌ Error getting student details:', error);
    res.status(500).json({
      success: false,
      error: 'Error getting student details',
      details: error.message
    });
  } finally {
    client.release();
  }
});

// Get enum metadata for dropdowns
app.get('/api/interviews/metadata/enums', (req, res) => {
  // Check cache first
  const cacheKey = 'interviews:metadata:enums';
  const cached = evaluationCache.get(cacheKey);
  if (cached) {
    console.log('[Cache HIT] interviews:metadata:enums');
    return res.json(cached);
  }
  console.log('[Cache MISS] interviews:metadata:enums');

  const enums = {
    interviewTypes: [
      { value: 'INDIVIDUAL', label: 'Entrevista Individual', description: 'Entrevista con el estudiante' },
      { value: 'FAMILY', label: 'Entrevista Familiar', description: 'Entrevista con la familia' },
      { value: 'PSYCHOLOGICAL', label: 'Evaluación Psicológica', description: 'Evaluación psicológica del estudiante' },
      { value: 'ACADEMIC', label: 'Evaluación Académica', description: 'Evaluación académica y de conocimientos' },
      { value: 'BEHAVIORAL', label: 'Evaluación Conductual', description: 'Evaluación de comportamiento y adaptación' }
    ],
    interviewModes: [
      { value: 'PRESENTIAL', label: 'Presencial', description: 'Entrevista en las instalaciones del colegio' },
      { value: 'VIRTUAL', label: 'Virtual', description: 'Entrevista por videoconferencia' },
      { value: 'HYBRID', label: 'Híbrida', description: 'Combinación de presencial y virtual' }
    ],
    interviewStatuses: [
      { value: 'SCHEDULED', label: 'Agendada', description: 'Entrevista programada' },
      { value: 'CONFIRMED', label: 'Confirmada', description: 'Entrevista confirmada por el entrevistador' },
      { value: 'IN_PROGRESS', label: 'En Progreso', description: 'Entrevista en desarrollo' },
      { value: 'COMPLETED', label: 'Completada', description: 'Entrevista finalizada' },
      { value: 'CANCELLED', label: 'Cancelada', description: 'Entrevista cancelada' },
      { value: 'NO_SHOW', label: 'No se presentó', description: 'El entrevistado no asistió' },
      { value: 'RESCHEDULED', label: 'Reprogramada', description: 'Entrevista reprogramada' }
    ],
    userRoles: [
      { value: 'TEACHER', label: 'Profesor', canInterview: ['INDIVIDUAL', 'ACADEMIC'] },
      { value: 'COORDINATOR', label: 'Coordinador', canInterview: ['INDIVIDUAL', 'FAMILY', 'ACADEMIC'] },
      { value: 'PSYCHOLOGIST', label: 'Psicólogo', canInterview: ['INDIVIDUAL', 'FAMILY', 'PSYCHOLOGICAL', 'BEHAVIORAL'] },
      { value: 'CYCLE_DIRECTOR', label: 'Director de Ciclo', canInterview: ['INDIVIDUAL', 'FAMILY', 'ACADEMIC', 'BEHAVIORAL'] }
    ]
  };

  const response = {
    success: true,
    data: enums
  };

  // Cache for 60 minutes (metadata is static)
  evaluationCache.set(cacheKey, response, 3600000);
  res.json(response);
});

// Validate interview creation business rules
app.post('/api/interviews/validate', async (req, res) => {
  const { applicationId, interviewType, interviewerId, scheduledDate, scheduledTime, duration = 60 } = req.body;

  console.log('🔍 Validating interview creation:', req.body);

  const client = await dbPool.connect();
  try {
    const errors = [];
    const warnings = [];

    // 1. Check if application exists
    const appQuery = 'SELECT id, status FROM applications WHERE id = $1';
    const appResult = await queryWithCircuitBreaker.fire(client, appQuery, [applicationId]);

    if (appResult.rows.length === 0) {
      errors.push('La aplicación no existe');
    } else {
      const appStatus = appResult.rows[0].status;
      if (!['PENDING', 'UNDER_REVIEW', 'INTERVIEW_SCHEDULED'].includes(appStatus)) {
        warnings.push(`La aplicación está en estado ${appStatus}`);
      }
    }

    // 2. Check if interviewer exists and has the right role
    const interviewerQuery = 'SELECT id, role, first_name, last_name FROM users WHERE id = $1';
    const interviewerResult = await queryWithCircuitBreaker.fire(client, interviewerQuery, [interviewerId]);

    if (interviewerResult.rows.length === 0) {
      errors.push('El entrevistador no existe');
    } else {
      const interviewer = interviewerResult.rows[0];
      const validRoles = {
        'INDIVIDUAL': ['TEACHER', 'COORDINATOR', 'PSYCHOLOGIST', 'CYCLE_DIRECTOR'],
        'FAMILY': ['COORDINATOR', 'PSYCHOLOGIST', 'CYCLE_DIRECTOR'],
        'PSYCHOLOGICAL': ['PSYCHOLOGIST'],
        'ACADEMIC': ['TEACHER', 'COORDINATOR', 'CYCLE_DIRECTOR'],
        'BEHAVIORAL': ['PSYCHOLOGIST', 'CYCLE_DIRECTOR']
      };

      if (!validRoles[interviewType]?.includes(interviewer.role)) {
        errors.push(`${interviewer.first_name} ${interviewer.last_name} (${interviewer.role}) no puede realizar entrevistas de tipo ${interviewType}`);
      }
    }

    // 3. Check for duplicate interview types
    const duplicateQuery = 'SELECT id FROM interviews WHERE application_id = $1 AND interview_type = $2 AND status NOT IN (\'CANCELLED\', \'NO_SHOW\')';
    const duplicateResult = await queryWithCircuitBreaker.fire(client, duplicateQuery, [applicationId, interviewType]);

    if (duplicateResult.rows.length > 0) {
      errors.push(`Ya existe una entrevista de tipo ${interviewType} para esta aplicación`);
    }

    // 4. Check interviewer availability
    const fullDateTime = `${scheduledDate}T${scheduledTime}:00`;
    const interviewDate = new Date(fullDateTime);
    const dayOfWeek = getDayOfWeek(scheduledDate);

    // Check schedule availability
    const scheduleQuery = `
      SELECT * FROM interviewer_schedules
      WHERE interviewer_id = $1
        AND day_of_week = $2
        AND year = $3
        AND is_active = true
        AND schedule_type = 'RECURRING'
    `;

    const currentYear = new Date().getFullYear();
    const scheduleResult = await queryWithCircuitBreaker.fire(client, scheduleQuery, [interviewerId, dayOfWeek, currentYear]);

    if (scheduleResult.rows.length === 0) {
      errors.push(`El entrevistador no tiene horarios configurados para ${dayOfWeek.toLowerCase()}`);
    } else {
      const schedule = scheduleResult.rows[0];
      if (!isTimeInRange(scheduledTime, schedule.start_time, schedule.end_time)) {
        errors.push(`El horario ${scheduledTime} está fuera del rango disponible (${schedule.start_time} - ${schedule.end_time})`);
      }
    }

    // 5. Check for time conflicts
    const conflictQuery = `
      SELECT id, scheduled_date, duration_minutes FROM interviews
      WHERE interviewer_id = $1
        AND DATE(scheduled_date) = $2
        AND status NOT IN ('CANCELLED', 'NO_SHOW')
    `;

    const conflictResult = await queryWithCircuitBreaker.fire(client, conflictQuery, [interviewerId, scheduledDate]);

    for (const conflict of conflictResult.rows) {
      const conflictDate = new Date(conflict.scheduled_date);
      const conflictStart = conflictDate.getHours() * 60 + conflictDate.getMinutes();
      const conflictEnd = conflictStart + conflict.duration_minutes;

      const [requestHour, requestMinute] = scheduledTime.split(':').map(Number);
      const requestStart = requestHour * 60 + requestMinute;
      const requestEnd = requestStart + parseInt(duration);

      if (requestStart < conflictEnd && requestEnd > conflictStart) {
        const conflictTime = conflictDate.toTimeString().substring(0, 5);
        errors.push(`Conflicto de horario: ya existe una entrevista a las ${conflictTime}`);
      }
    }

    // 6. Check 4-interview limit
    const countQuery = 'SELECT COUNT(*) as count FROM interviews WHERE application_id = $1 AND status NOT IN (\'CANCELLED\', \'NO_SHOW\')';
    const countResult = await client.query(countQuery, [applicationId]);
    const interviewCount = parseInt(countResult.rows[0].count);

    if (interviewCount >= 4) {
      errors.push('Ya se han programado las 4 entrevistas requeridas para esta aplicación');
    } else if (interviewCount === 3) {
      warnings.push('Esta será la cuarta y última entrevista para esta aplicación');
    }

    const isValid = errors.length === 0;

    res.json({
      success: true,
      data: {
        valid: isValid,
        errors,
        warnings,
        interviewCount: interviewCount + (isValid ? 1 : 0),
        remainingInterviews: Math.max(0, 4 - interviewCount - (isValid ? 1 : 0))
      }
    });

  } catch (error) {
    console.error('❌ Error validating interview:', error);
    res.status(500).json({
      success: false,
      error: 'Error validating interview',
      details: error.message
    });
  } finally {
    client.release();
  }
});

// Enhanced interview creation with full validation
app.post('/api/interviews/create-validated', async (req, res) => {
  const {
    applicationId,
    interviewerId,
    scheduledDate,
    scheduledTime,
    duration = 60,
    interviewType,
    interviewMode = 'PRESENTIAL',
    location,
    meetingLink,
    room,
    notes
  } = req.body;

  console.log('🎯 Creating validated interview:', req.body);

  const client = await dbPool.connect();
  try {
    // First validate using the validation endpoint logic
    const validation = await fetch(`http://localhost:${port}/api/interviews/validate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(req.body)
    });

    if (!validation.ok) {
      throw new Error('Validation service error');
    }

    const validationResult = await validation.json();

    if (!validationResult.data.valid) {
      return res.status(400).json({
        success: false,
        error: 'Validation failed',
        details: validationResult.data.errors,
        warnings: validationResult.data.warnings
      });
    }

    // If validation passes, create the interview
    const fullDateTime = `${scheduledDate}T${scheduledTime}:00`;
    const inputDate = new Date(fullDateTime);

    // Format for PostgreSQL
    const year = inputDate.getFullYear();
    const month = String(inputDate.getMonth() + 1).padStart(2, '0');
    const day = String(inputDate.getDate()).padStart(2, '0');
    const hours = String(inputDate.getHours()).padStart(2, '0');
    const minutes = String(inputDate.getMinutes()).padStart(2, '0');
    const seconds = String(inputDate.getSeconds()).padStart(2, '0');
    const normalizedDate = `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;

    const insertQuery = `
      INSERT INTO interviews (
        application_id,
        interviewer_id,
        scheduled_date,
        duration_minutes,
        interview_type,
        interview_mode,
        status,
        location,
        meeting_link,
        room,
        notes,
        created_at,
        updated_at
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, NOW(), NOW())
      RETURNING *
    `;

    const values = [
      parseInt(applicationId),
      parseInt(interviewerId),
      normalizedDate,
      parseInt(duration),
      interviewType,
      interviewMode,
      'SCHEDULED',
      location || 'Por asignar',
      meetingLink || null,
      room || null,
      notes || ''
    ];

    const result = await client.query(insertQuery, values);
    const newInterview = result.rows[0];

    console.log('✅ Interview created with validation:', newInterview.id);

    res.status(201).json({
      success: true,
      data: {
        interview: newInterview,
        validation: validationResult.data,
        message: 'Entrevista creada exitosamente con todas las validaciones'
      }
    });

  } catch (error) {
    console.error('❌ Error creating validated interview:', error);
    res.status(500).json({
      success: false,
      error: 'Error creating interview',
      details: error.message
    });
  } finally {
    client.release();
  }
});

// Manual endpoint to send interview summary email
app.post('/api/interviews/application/:applicationId/send-summary', async (req, res) => {
  const { applicationId } = req.params;

  console.log(`📧 Enviando resumen de entrevistas para aplicación ${applicationId}`);

  const client = await dbPool.connect();
  try {
    // 1. Verificar que todas las entrevistas requeridas estén agendadas
    const allInterviewsQuery = await client.query(
      `SELECT
        i.id, i.interview_type, i.scheduled_date, i.duration_minutes, i.location,
        u.first_name || ' ' || u.last_name as interviewer_name
       FROM interviews i
       JOIN users u ON i.interviewer_id = u.id
       WHERE i.application_id = $1 AND i.status = 'SCHEDULED'
       ORDER BY i.scheduled_date`,
      [parseInt(applicationId)]
    );

    const requiredTypes = ['INDIVIDUAL', 'FAMILY', 'PSYCHOLOGICAL'];
    const scheduledTypes = allInterviewsQuery.rows.map(r => r.interview_type);
    const allRequiredScheduled = requiredTypes.every(type => scheduledTypes.includes(type));

    if (!allRequiredScheduled) {
      const missingTypes = requiredTypes.filter(type => !scheduledTypes.includes(type));
      return res.status(400).json({
        success: false,
        error: 'No todas las entrevistas requeridas están agendadas',
        details: {
          required: requiredTypes,
          scheduled: scheduledTypes,
          missing: missingTypes
        }
      });
    }

    // 2. Obtener datos del estudiante y apoderado (usuario que creó la aplicación)
    const studentQuery = await client.query(
      `SELECT
        a.id as application_id,
        s.first_name || ' ' || s.paternal_last_name || ' ' || s.maternal_last_name as student_name,
        u.email as applicant_email,
        u.first_name || ' ' || u.last_name as applicant_name
       FROM applications a
       JOIN students s ON a.student_id = s.id
       LEFT JOIN users u ON a.applicant_user_id = u.id
       WHERE a.id = $1`,
      [parseInt(applicationId)]
    );

    if (studentQuery.rows.length === 0) {
      return res.status(404).json({
        success: false,
        error: 'Aplicación no encontrada'
      });
    }

    const applicationData = studentQuery.rows[0];

    // Validar que exista el apoderado
    if (!applicationData.applicant_email) {
      return res.status(400).json({
        success: false,
        error: 'No se encontró el apoderado que creó la aplicación'
      });
    }

    // 3. Formatear datos de las entrevistas para el email
    const interviews = allInterviewsQuery.rows.map(interview => {
      const dateObj = new Date(interview.scheduled_date);
      return {
        type: interview.interview_type,
        date: dateObj.toLocaleDateString('es-CL', {
          weekday: 'long',
          year: 'numeric',
          month: 'long',
          day: 'numeric'
        }),
        time: dateObj.toLocaleTimeString('es-CL', {
          hour: '2-digit',
          minute: '2-digit'
        }),
        duration: interview.duration_minutes,
        location: interview.location,
        interviewer: interview.interviewer_name
      };
    });

    // 4. Enviar el email al apoderado que creó la aplicación
    console.log(`📬 Enviando email a ${applicationData.applicant_email}...`);

    const emailResponse = await axios.post('http://localhost:8085/api/notifications/send', {
      type: 'email',
      recipient: applicationData.applicant_email,
      subject: `Resumen de Entrevistas Agendadas - ${applicationData.student_name}`,
      template: 'all_interviews_scheduled',
      templateData: {
        guardianName: applicationData.applicant_name,
        studentName: applicationData.student_name,
        totalInterviews: interviews.length,
        interviews: interviews
      }
    });

    console.log('✅ Email de resumen enviado exitosamente');

    res.status(200).json({
      success: true,
      message: 'Resumen de entrevistas enviado exitosamente',
      data: {
        applicationId: parseInt(applicationId),
        applicantEmail: applicationData.applicant_email,
        applicantName: applicationData.applicant_name,
        totalInterviews: interviews.length,
        interviewsSent: interviews.length,
        emailStatus: emailResponse.data
      }
    });

  } catch (error) {
    console.error('❌ Error enviando resumen de entrevistas:', error);
    res.status(500).json({
      success: false,
      error: 'Error enviando resumen de entrevistas',
      details: error.message
    });
  } finally {
    client.release();
  }
});

// ===================================
// EVALUATION MANAGEMENT ENDPOINTS
// ===================================

// Mock data for evaluators by role
const mockEvaluatorsByRole = {
  TEACHER_LANGUAGE: [
    { id: 1, firstName: 'Ana', lastName: 'García', email: 'ana.garcia@mtn.cl', role: 'TEACHER_LANGUAGE', active: true },
    { id: 2, firstName: 'Carlos', lastName: 'López', email: 'carlos.lopez@mtn.cl', role: 'TEACHER_LANGUAGE', active: true }
  ],
  TEACHER_MATHEMATICS: [
    { id: 3, firstName: 'María', lastName: 'Rodríguez', email: 'maria.rodriguez@mtn.cl', role: 'TEACHER_MATHEMATICS', active: true },
    { id: 4, firstName: 'Pedro', lastName: 'Martínez', email: 'pedro.martinez@mtn.cl', role: 'TEACHER_MATHEMATICS', active: true }
  ],
  TEACHER_ENGLISH: [
    { id: 5, firstName: 'Susan', lastName: 'Johnson', email: 'susan.johnson@mtn.cl', role: 'TEACHER_ENGLISH', active: true },
    { id: 6, firstName: 'David', lastName: 'Smith', email: 'david.smith@mtn.cl', role: 'TEACHER_ENGLISH', active: true }
  ],
  CYCLE_DIRECTOR: [
    { id: 7, firstName: 'Carmen', lastName: 'Pérez', email: 'carmen.perez@mtn.cl', role: 'CYCLE_DIRECTOR', active: true },
    { id: 8, firstName: 'Luis', lastName: 'González', email: 'luis.gonzalez@mtn.cl', role: 'CYCLE_DIRECTOR', active: true }
  ],
  PSYCHOLOGIST: [
    { id: 9, firstName: 'Patricia', lastName: 'Silva', email: 'patricia.silva@mtn.cl', role: 'PSYCHOLOGIST', active: true },
    { id: 10, firstName: 'Roberto', lastName: 'Torres', email: 'roberto.torres@mtn.cl', role: 'PSYCHOLOGIST', active: true }
  ]
};

// Mock evaluations data
const mockEvaluations = [
  {
    id: 1,
    evaluationType: 'LANGUAGE_EXAM',
    status: 'PENDING',
    applicationId: 1,
    evaluatorId: 1,
    createdAt: '2024-01-15T10:00:00Z',
    evaluator: mockEvaluatorsByRole.TEACHER_LANGUAGE[0]
  },
  {
    id: 2,
    evaluationType: 'MATHEMATICS_EXAM',
    status: 'COMPLETED',
    score: 85,
    grade: 'B',
    applicationId: 1,
    evaluatorId: 3,
    createdAt: '2024-01-15T10:00:00Z',
    completionDate: '2024-01-20T14:30:00Z',
    evaluator: mockEvaluatorsByRole.TEACHER_MATHEMATICS[0]
  }
];

// Get evaluators by role - AHORA USA DATOS REALES
app.get('/api/evaluations/evaluators/:role', async (req, res) => {
  const { role } = req.params;

  console.log(`📋 Getting REAL evaluators for role: ${role}`);

  // Check cache first (cache key includes role)
  const cacheKey = `evaluations:evaluators:${role}`;
  const cached = evaluationCache.get(cacheKey);
  if (cached) {
    console.log(`[Cache HIT] evaluations:evaluators:${role}`);
    return res.json(cached);
  }
  console.log(`[Cache MISS] evaluations:evaluators:${role}`);

  const client = await dbPool.connect();
  try {
    // Mapear rol a filtros de base de datos
    let subjectFilter = '';
    let roleFilter = '';

    switch (role) {
      case 'TEACHER':
        // Return ALL teachers (any subject)
        roleFilter = "role IN ('TEACHER', 'COORDINATOR')";
        break;
      case 'TEACHER_MATHEMATICS':
        subjectFilter = 'MATHEMATICS';
        roleFilter = "role IN ('TEACHER', 'COORDINATOR')";
        break;
      case 'TEACHER_LANGUAGE':
        subjectFilter = 'LANGUAGE';
        roleFilter = "role IN ('TEACHER', 'COORDINATOR')";
        break;
      case 'TEACHER_ENGLISH':
        subjectFilter = 'ENGLISH';
        roleFilter = "role IN ('TEACHER', 'COORDINATOR')";
        break;
      case 'CYCLE_DIRECTOR':
        roleFilter = "role = 'CYCLE_DIRECTOR'";
        break;
      case 'PSYCHOLOGIST':
        roleFilter = "role = 'PSYCHOLOGIST'";
        break;
      default:
        return res.json([]);
    }

    let query = `
      SELECT id, first_name as "firstName", last_name as "lastName",
             email, role, subject, active
      FROM users
      WHERE ${roleFilter}
        AND active = true
        AND email_verified = true
    `;

    if (subjectFilter) {
      query += ` AND subject = '${subjectFilter}'`;
    }

    query += ' ORDER BY last_name, first_name';

    const result = await client.query(query);

    // Transformar datos para compatibilidad con frontend
    const evaluators = result.rows.map(user => ({
      id: parseInt(user.id),
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      role: user.role, // Usar el rol REAL del usuario desde la base de datos
      subject: user.subject, // Incluir subject también
      active: user.active
    }));

    console.log(`✅ Found ${evaluators.length} real evaluators for ${role}:`,
                evaluators.map(e => `${e.firstName} ${e.lastName}`));

    // Cache raw array for 10 minutes (unwrapped for QA test compatibility)
    evaluationCache.set(cacheKey, evaluators, 600000);
    res.json(evaluators);

  } catch (error) {
    console.error('❌ Error fetching real evaluators:', error);
    // Fallback a datos mock en caso de error (unwrapped)
    const evaluators = mockEvaluatorsByRole[role] || [];
    res.json(evaluators);
  } finally {
    client.release();
  }
});

// Get evaluators by role (public endpoint) - AHORA USA DATOS REALES
app.get('/api/evaluations/public/evaluators/:role', async (req, res) => {
  const { role } = req.params;

  console.log(`📋 Getting REAL evaluators for role (public): ${role}`);

  const client = await dbPool.connect();
  try {
    // Mapear rol a filtros de base de datos
    let subjectFilter = '';
    let roleFilter = '';

    switch (role) {
      case 'TEACHER':
        // Return ALL teachers (any subject)
        roleFilter = "role IN ('TEACHER', 'COORDINATOR')";
        break;
      case 'TEACHER_MATHEMATICS':
        subjectFilter = 'MATHEMATICS';
        roleFilter = "role IN ('TEACHER', 'COORDINATOR')";
        break;
      case 'TEACHER_LANGUAGE':
        subjectFilter = 'LANGUAGE';
        roleFilter = "role IN ('TEACHER', 'COORDINATOR')";
        break;
      case 'TEACHER_ENGLISH':
        subjectFilter = 'ENGLISH';
        roleFilter = "role IN ('TEACHER', 'COORDINATOR')";
        break;
      case 'CYCLE_DIRECTOR':
        roleFilter = "role = 'CYCLE_DIRECTOR'";
        break;
      case 'PSYCHOLOGIST':
        roleFilter = "role = 'PSYCHOLOGIST'";
        break;
      default:
        return res.json([]);
    }

    let query = `
      SELECT id, first_name as "firstName", last_name as "lastName",
             email, role, subject, active
      FROM users
      WHERE ${roleFilter}
        AND active = true
        AND email_verified = true
    `;

    if (subjectFilter) {
      query += ` AND subject = '${subjectFilter}'`;
    }

    query += ' ORDER BY last_name, first_name';

    const result = await client.query(query);

    // Transformar datos para compatibilidad con frontend
    const evaluators = result.rows.map(user => ({
      id: parseInt(user.id),
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      role: user.role, // Usar el rol REAL del usuario desde la base de datos
      subject: user.subject, // Incluir subject también
      active: user.active
    }));

    console.log(`✅ Found ${evaluators.length} real evaluators for ${role} (public):`,
                evaluators.map(e => `${e.firstName} ${e.lastName}`));
    res.json(evaluators);

  } catch (error) {
    console.error('❌ Error fetching real evaluators (public):', error);
    // Fallback a datos mock en caso de error
    const evaluators = mockEvaluatorsByRole[role] || [];
    res.json(evaluators);
  } finally {
    client.release();
  }
});

// Assign evaluations to application
app.post('/api/evaluations/assign/:applicationId', (req, res) => {
  const { applicationId } = req.params;

  console.log(`🔄 Auto-assigning evaluations to application: ${applicationId}`);

  // Mock automatic assignment - create evaluations for each type
  const evaluationTypes = ['LANGUAGE_EXAM', 'MATHEMATICS_EXAM', 'ENGLISH_EXAM', 'CYCLE_DIRECTOR_REPORT', 'PSYCHOLOGICAL_INTERVIEW'];
  const newEvaluations = [];

  evaluationTypes.forEach((type, index) => {
    const evaluation = {
      id: Date.now() + index,
      evaluationType: type,
      status: 'PENDING',
      applicationId: parseInt(applicationId),
      evaluatorId: Math.floor(Math.random() * 10) + 1,
      createdAt: new Date().toISOString(),
      evaluator: {
        id: Math.floor(Math.random() * 10) + 1,
        firstName: 'Evaluador',
        lastName: `${index + 1}`,
        email: `evaluador${index + 1}@mtn.cl`
      }
    };
    newEvaluations.push(evaluation);
    mockEvaluations.push(evaluation);
  });

  res.json(newEvaluations);
});

// Assign evaluations to application (public endpoint)
app.post('/api/evaluations/public/assign/:applicationId', (req, res) => {
  const { applicationId } = req.params;

  console.log(`🔄 Auto-assigning evaluations to application (public): ${applicationId}`);

  // Mock automatic assignment - create evaluations for each type
  const evaluationTypes = ['LANGUAGE_EXAM', 'MATHEMATICS_EXAM', 'ENGLISH_EXAM', 'CYCLE_DIRECTOR_REPORT', 'PSYCHOLOGICAL_INTERVIEW'];
  const newEvaluations = [];

  evaluationTypes.forEach((type, index) => {
    const evaluation = {
      id: Date.now() + index,
      evaluationType: type,
      status: 'PENDING',
      applicationId: parseInt(applicationId),
      evaluatorId: Math.floor(Math.random() * 10) + 1,
      createdAt: new Date().toISOString(),
      evaluator: {
        id: Math.floor(Math.random() * 10) + 1,
        firstName: 'Evaluador',
        lastName: `${index + 1}`,
        email: `evaluador${index + 1}@mtn.cl`
      }
    };
    newEvaluations.push(evaluation);
    mockEvaluations.push(evaluation);
  });

  res.json(newEvaluations);
});

// Assign specific evaluation to specific evaluator
app.post('/api/evaluations/assign/:applicationId/:evaluationType/:evaluatorId', async (req, res) => {
  const { applicationId, evaluationType, evaluatorId } = req.params;

  console.log(`🎯 Assigning specific evaluation: ${evaluationType} to evaluator ${evaluatorId} for application ${applicationId}`);

  const client = await dbPool.connect();
  try {
    // Check if evaluation already exists
    const existingEvalQuery = `
      SELECT id FROM evaluations
      WHERE application_id = $1 AND evaluation_type = $2
    `;

    const existingEval = await client.query(existingEvalQuery, [
      parseInt(applicationId),
      evaluationType
    ]);

    // If evaluation already exists, resend notification email instead of returning error
    if (existingEval.rows.length > 0) {
      const existingEvaluationId = existingEval.rows[0].id;
      console.log(`ℹ️ Evaluation already exists (ID: ${existingEvaluationId}). Resending notification email...`);

      // Get existing evaluation with evaluator details
      const evalDetailsQuery = `
        SELECT e.id, e.application_id, e.evaluator_id, e.evaluation_type, e.status,
               u.id as evaluator_user_id, u.first_name, u.last_name, u.email, u.role, u.subject
        FROM evaluations e
        JOIN users u ON u.id = e.evaluator_id
        WHERE e.id = $1
      `;

      const evalDetails = await client.query(evalDetailsQuery, [existingEvaluationId]);
      const evaluation = evalDetails.rows[0];

      // Get student information for email
      const studentQuery = await client.query(`
        SELECT s.first_name, s.paternal_last_name, s.maternal_last_name, s.grade_applied
        FROM applications a
        JOIN students s ON s.id = a.student_id
        WHERE a.id = $1
      `, [parseInt(applicationId)]);

      const student = studentQuery.rows[0];
      client.release();

      // Send reminder email notification
      if (student && evaluation.email) {
        const evaluationTypeES = {
          'LANGUAGE_EXAM': 'Examen de Lenguaje',
          'MATHEMATICS_EXAM': 'Examen de Matemáticas',
          'ENGLISH_EXAM': 'Examen de Inglés',
          'PSYCHOLOGIST_INTERVIEW': 'Entrevista Psicológica',
          'DIRECTOR_INTERVIEW': 'Entrevista con Director',
          'COORDINATOR_INTERVIEW': 'Entrevista con Coordinador'
        };

        const studentFullName = `${student.first_name} ${student.paternal_last_name} ${student.maternal_last_name || ''}`.trim();

        try {
          await sendEmail(
            evaluation.email,
            `Recordatorio: Evaluación Pendiente - ${evaluationTypeES[evaluation.evaluation_type] || evaluation.evaluation_type}`,
            null,
            `
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
              <h2 style="color: #2563eb;">Recordatorio de Evaluación Pendiente</h2>
              <p>Estimado/a ${evaluation.first_name} ${evaluation.last_name},</p>
              <p>Le recordamos que tiene una evaluación pendiente para completar:</p>
              <div style="background-color: #f3f4f6; padding: 15px; border-radius: 5px; margin: 20px 0;">
                <p><strong>Tipo de Evaluación:</strong> ${evaluationTypeES[evaluation.evaluation_type] || evaluation.evaluation_type}</p>
                <p><strong>Estudiante:</strong> ${studentFullName}</p>
                <p><strong>Curso:</strong> ${student.grade_applied || 'No especificado'}</p>
                <p><strong>Estado:</strong> ${evaluation.status === 'PENDING' ? 'Pendiente' : evaluation.status === 'IN_PROGRESS' ? 'En Progreso' : 'Completada'}</p>
              </div>
              <p>Por favor, acceda al sistema para completar la evaluación a la brevedad.</p>
              <p>Saludos cordiales,<br>Sistema de Admisión</p>
            </div>
            `
          );
          console.log(`📧 Reminder email sent to ${evaluation.email} for evaluation ID ${existingEvaluationId}`);
        } catch (emailError) {
          console.error('❌ Error sending reminder email:', emailError);
        }
      }

      // Return success with existing evaluation info
      return res.status(200).json({
        success: true,
        message: 'Recordatorio de evaluación enviado exitosamente',
        evaluation: {
          id: evaluation.id,
          evaluationType: evaluation.evaluation_type,
          status: evaluation.status,
          applicationId: evaluation.application_id,
          evaluatorId: evaluation.evaluator_id,
          evaluator: {
            id: evaluation.evaluator_user_id,
            firstName: evaluation.first_name,
            lastName: evaluation.last_name,
            email: evaluation.email,
            role: evaluation.role,
            subject: evaluation.subject
          }
        },
        isReminder: true
      });
    }

    // Insert evaluation into database
    const insertQuery = `
      INSERT INTO evaluations (
        application_id,
        evaluator_id,
        evaluation_type,
        status,
        created_at,
        updated_at
      ) VALUES ($1, $2, $3, $4, NOW(), NOW())
      RETURNING id, application_id, evaluator_id, evaluation_type, status, created_at, updated_at
    `;

    const result = await client.query(insertQuery, [
      parseInt(applicationId),
      parseInt(evaluatorId),
      evaluationType,
      'PENDING'
    ]);

    const newEvaluation = result.rows[0];

    // Get evaluator details
    const evaluatorQuery = await client.query(
      'SELECT id, first_name, last_name, email, role, subject FROM users WHERE id = $1',
      [parseInt(evaluatorId)]
    );

    const evaluator = evaluatorQuery.rows[0];

    client.release();

    const evaluation = {
      id: newEvaluation.id,
      evaluationType: newEvaluation.evaluation_type,
      status: newEvaluation.status,
      applicationId: newEvaluation.application_id,
      evaluatorId: newEvaluation.evaluator_id,
      createdAt: newEvaluation.created_at,
      updatedAt: newEvaluation.updated_at,
      evaluator: {
        id: evaluator.id,
        firstName: evaluator.first_name,
        lastName: evaluator.last_name,
        email: evaluator.email,
        role: evaluator.role,
        subject: evaluator.subject
      }
    };

    console.log(`✅ Evaluation assigned successfully: ID=${evaluation.id}, Type=${evaluationType}, Evaluator=${evaluator.first_name} ${evaluator.last_name}`);

    // Get student information for email
    const studentQuery = await dbPool.query(`
      SELECT s.first_name, s.paternal_last_name, s.maternal_last_name, s.grade_applied
      FROM applications a
      JOIN students s ON s.id = a.student_id
      WHERE a.id = $1
    `, [parseInt(applicationId)]);

    const student = studentQuery.rows[0];

    // Send email notification to evaluator
    if (student && evaluator.email) {
      const evaluationTypeES = {
        'LANGUAGE_EXAM': 'Examen de Lenguaje',
        'MATHEMATICS_EXAM': 'Examen de Matemáticas',
        'ENGLISH_EXAM': 'Examen de Inglés',
        'PSYCHOLOGIST_INTERVIEW': 'Entrevista Psicológica',
        'DIRECTOR_INTERVIEW': 'Entrevista con Director',
        'COORDINATOR_INTERVIEW': 'Entrevista con Coordinador'
      };

      const studentFullName = `${student.first_name} ${student.paternal_last_name} ${student.maternal_last_name || ''}`.trim();

      try {
        await axios.post('http://localhost:8085/api/notifications/send-evaluation-assignment', {
          evaluatorEmail: evaluator.email,
          evaluatorName: `${evaluator.first_name} ${evaluator.last_name}`,
          studentName: studentFullName,
          studentGrade: student.grade_applied,
          evaluationType: evaluationTypeES[evaluationType] || evaluationType,
          applicationId: applicationId
        });
        console.log(`📧 Email sent to ${evaluator.email} about ${studentFullName}`);
      } catch (emailError) {
        console.error('❌ Error sending evaluation assignment email:', emailError.message);
        // Don't fail the assignment if email fails
      }
    }

    // Also add to mock array for compatibility
    mockEvaluations.push(evaluation);

    res.json(evaluation);
  } catch (error) {
    client.release();
    console.error('❌ Error assigning evaluation:', error);
    res.status(500).json({ error: 'Error al asignar evaluación', details: error.message });
  }
});

// Get evaluations by application
app.get('/api/evaluations/application/:applicationId', async (req, res) => {
  const { applicationId} = req.params;

  console.log(`📋 Getting evaluations for application: ${applicationId}`);

  const client = await dbPool.connect();
  try {
    const result = await client.query(`
      SELECT
        e.*,
        u.id as evaluator_user_id,
        u.first_name as evaluator_first_name,
        u.last_name as evaluator_last_name,
        u.email as evaluator_email,
        u.role as evaluator_role,
        u.subject as evaluator_subject
      FROM evaluations e
      LEFT JOIN users u ON e.evaluator_id = u.id
      WHERE e.application_id = $1
      ORDER BY e.created_at DESC
    `, [applicationId]);

    const evaluations = result.rows.map(row => ({
      id: row.id,
      evaluationType: row.evaluation_type,
      status: row.status,
      score: row.score,
      grade: row.grade,
      applicationId: row.application_id,
      evaluatorId: row.evaluator_id,
      evaluationDate: row.evaluation_date,
      completionDate: row.completion_date,
      observations: row.observations,
      strengths: row.strengths,
      areasForImprovement: row.areas_for_improvement,
      recommendations: row.recommendations,
      academicReadiness: row.academic_readiness,
      socialSkillsAssessment: row.social_skills_assessment,
      emotionalMaturity: row.emotional_maturity,
      behavioralAssessment: row.behavioral_assessment,
      motivationAssessment: row.motivation_assessment,
      familySupportAssessment: row.family_support_assessment,
      integrationPotential: row.integration_potential,
      finalRecommendation: row.final_recommendation,
      createdAt: row.created_at,
      updatedAt: row.updated_at,
      evaluator: {
        id: row.evaluator_user_id,
        firstName: row.evaluator_first_name,
        lastName: row.evaluator_last_name,
        email: row.evaluator_email,
        role: row.evaluator_role,
        subject: row.evaluator_subject
      }
    }));

    client.release();
    // Return raw array (unwrapped) for QA test compatibility
    res.json(evaluations);
  } catch (error) {
    client.release();
    console.error('❌ Error getting evaluations for application:', error);
    res.status(500).json(ResponseHelper.fail('Error al obtener evaluaciones de la aplicación', { errorCode: 'EVAL_APP_ERROR' }));
  }
});

// Get detailed evaluations by application
app.get('/api/evaluations/application/:applicationId/detailed', (req, res) => {
  const { applicationId } = req.params;

  console.log(`📋 Getting detailed evaluations for application: ${applicationId}`);

  const applicationEvaluations = mockEvaluations.filter(
    eval => eval.applicationId === parseInt(applicationId)
  );

  // Add more detailed mock data
  const detailedEvaluations = applicationEvaluations.map(eval => ({
    ...eval,
    observations: eval.status === 'COMPLETED' ? 'Evaluación completada satisfactoriamente' : null,
    strengths: eval.status === 'COMPLETED' ? 'Buen desempeño general' : null,
    areasForImprovement: eval.status === 'COMPLETED' ? 'Continuar practicando' : null,
    application: {
      id: parseInt(applicationId),
      status: 'SUBMITTED',
      submissionDate: '2024-01-15T10:00:00Z',
      student: {
        firstName: 'Estudiante',
        lastName: 'Ejemplo',
        rut: '12.345.678-9',
        gradeApplied: '1° Básico'
      }
    }
  }));

  res.json(detailedEvaluations);
});

// Get evaluation progress
app.get('/api/evaluations/application/:applicationId/progress', (req, res) => {
  const { applicationId } = req.params;

  console.log(`📊 Getting evaluation progress for application: ${applicationId}`);

  const applicationEvaluations = mockEvaluations.filter(
    eval => eval.applicationId === parseInt(applicationId)
  );

  const completedEvaluations = applicationEvaluations.filter(
    eval => eval.status === 'COMPLETED'
  ).length;

  const totalEvaluations = applicationEvaluations.length || 5; // Default to 5 evaluation types
  const completionPercentage = totalEvaluations > 0 ? Math.round((completedEvaluations / totalEvaluations) * 100) : 0;

  const progress = {
    applicationId: parseInt(applicationId),
    totalEvaluations,
    completedEvaluations,
    completionPercentage,
    isComplete: completionPercentage === 100,
    familyInterview: Math.random() > 0.5 // Random mock data
  };

  res.json(progress);
});

// Get my evaluations
app.get('/api/evaluations/my-pending', (req, res) => {
  console.log('📋 Getting my pending evaluations');

  const pendingEvaluations = mockEvaluations.filter(eval => eval.status === 'PENDING');

  res.json(pendingEvaluations);
});

// Update evaluation
app.put('/api/evaluations/:evaluationId', async (req, res) => {
  const { evaluationId } = req.params;
  const updateData = req.body;

  console.log(`✏️ Updating evaluation ${evaluationId}:`, updateData);

  try {
    const client = await dbPool.connect();

    // Build dynamic UPDATE query based on provided fields
    const allowedFields = [
      'score', 'max_score', 'grade', 'observations', 'status',
      'academic_readiness', 'behavioral_assessment', 'emotional_maturity',
      'social_skills_assessment', 'motivation_assessment', 'family_support_assessment',
      'integration_potential', 'strengths', 'areas_for_improvement',
      'recommendations', 'final_recommendation', 'completion_date'
    ];

    const updates = [];
    const values = [];
    let paramIndex = 1;

    // Map camelCase to snake_case for compatibility
    const fieldMapping = {
      'maxScore': 'max_score',
      'areasForImprovement': 'areas_for_improvement'
    };

    // Add updated_at automatically
    Object.keys(updateData).forEach(key => {
      const dbKey = fieldMapping[key] || key;
      if (allowedFields.includes(dbKey)) {
        updates.push(`${dbKey} = $${paramIndex}`);
        values.push(updateData[key]);
        paramIndex++;
      }
    });

    // Always update updated_at
    updates.push(`updated_at = NOW()`);

    // If status is being set to COMPLETED, also set completion_date
    if (updateData.status === 'COMPLETED' && !updateData.completion_date) {
      updates.push(`completion_date = NOW()`);
    }

    if (updates.length === 1) { // Only updated_at
      return res.status(400).json({ error: 'No valid fields to update' });
    }

    values.push(evaluationId);
    const query = `
      UPDATE evaluations
      SET ${updates.join(', ')}
      WHERE id = $${paramIndex}
      RETURNING
        id, application_id, evaluator_id, evaluation_type,
        score, grade, observations, status,
        academic_readiness, behavioral_assessment, emotional_maturity,
        social_skills_assessment, motivation_assessment, family_support_assessment,
        integration_potential, strengths, areas_for_improvement,
        recommendations, final_recommendation,
        created_at, updated_at, completion_date
    `;

    console.log('🔍 Query:', query);
    console.log('🔍 Values:', values);

    // Circuit breaker temporarily disabled for writes due to closure issues
    // TODO: Fix circuit breaker implementation for database write operations
    const result = await client.query(query, values);
    client.release();

    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'Evaluation not found' });
    }

    console.log('✅ Evaluation updated successfully:', result.rows[0]);
    res.json(result.rows[0]);

  } catch (error) {
    console.error('❌ Error updating evaluation:', error);
    res.status(500).json({
      error: 'Error al actualizar la evaluación',
      details: error.message
    });
  }
});

// Get evaluation by ID
app.get('/api/evaluations/:evaluationId', async (req, res) => {
  const { evaluationId } = req.params;

  console.log(`📋 Getting evaluation by ID: ${evaluationId}`);

  try {
    const client = await dbPool.connect();

    const query = `
      SELECT
        e.id,
        e.application_id,
        e.evaluator_id,
        e.evaluation_type,
        e.score,
        e.max_score,
        e.grade,
        e.observations,
        e.status,
        e.academic_readiness,
        e.behavioral_assessment,
        e.emotional_maturity,
        e.social_skills_assessment,
        e.motivation_assessment,
        e.family_support_assessment,
        e.integration_potential,
        e.strengths,
        e.areas_for_improvement,
        e.recommendations,
        e.final_recommendation,
        e.created_at,
        e.updated_at,
        e.completion_date,
        e.evaluation_date,
        s.first_name || ' ' || s.paternal_last_name || ' ' || COALESCE(s.maternal_last_name, '') as student_name,
        s.grade_applied as student_grade,
        s.birth_date as student_birthdate,
        s.current_school,
        u.first_name || ' ' || u.last_name as evaluator_name,
        u.subject as evaluator_subject
      FROM evaluations e
      JOIN applications a ON a.id = e.application_id
      JOIN students s ON s.id = a.student_id
      JOIN users u ON u.id = e.evaluator_id
      WHERE e.id = $1
    `;

    const result = await client.query(query, [evaluationId]);
    client.release();

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Evaluation not found' });
    }

    res.json({ success: true, data: result.rows[0] });

  } catch (error) {
    console.error('❌ Error fetching evaluation:', error);
    res.status(500).json({
      error: 'Error al obtener la evaluación',
      details: error.message
    });
  }
});

// Get interview data for evaluation (HU-5)
app.get('/api/evaluations/:evaluationId/interview', async (req, res) => {
  const { evaluationId } = req.params;

  console.log(`🎤 Getting interview data for evaluation ${evaluationId}`);

  try {
    const client = await dbPool.connect();

    // First get the application_id from the evaluation
    const evalQuery = `SELECT application_id FROM evaluations WHERE id = $1`;
    const evalResult = await client.query(evalQuery, [evaluationId]);

    if (evalResult.rows.length === 0) {
      client.release();
      return res.status(404).json({ error: 'Evaluation not found' });
    }

    const applicationId = evalResult.rows[0].application_id;

    // Get interview data for this application
    const interviewQuery = `
      SELECT
        i.id,
        i.interview_type,
        i.scheduled_date,
        i.status,
        i.evaluation_notes,
        i.recommendation,
        i.follow_up_required,
        i.notes,
        i.location,
        i.interview_mode,
        u.first_name || ' ' || u.last_name as interviewer_name
      FROM interviews i
      JOIN users u ON u.id = i.interviewer_id
      WHERE i.application_id = $1
      ORDER BY i.scheduled_date DESC
      LIMIT 1
    `;

    const interviewResult = await simpleQueryBreaker.fire(client, interviewQuery, [applicationId]);
    client.release();

    if (interviewResult.rows.length === 0) {
      return res.json({ hasInterview: false, interview: null });
    }

    res.json({
      hasInterview: true,
      interview: interviewResult.rows[0]
    });

  } catch (error) {
    console.error('❌ Error fetching interview data:', error);
    res.status(500).json({
      error: 'Error al obtener datos de la entrevista',
      details: error.message
    });
  }
});

// Get evaluation history for a student (HU-8)
app.get('/api/evaluations/student/:studentId/history', async (req, res) => {
  const { studentId } = req.params;

  console.log(`📚 Getting evaluation history for student ${studentId}`);

  try {
    const client = await dbPool.connect();

    const query = `
      SELECT
        e.id,
        e.evaluation_type,
        e.score,
        e.grade,
        e.status,
        e.observations,
        e.created_at,
        e.completion_date,
        a.id as application_id,
        a.application_year,
        u.first_name || ' ' || u.last_name as evaluator_name
      FROM evaluations e
      JOIN applications a ON a.id = e.application_id
      JOIN students s ON s.id = a.student_id
      JOIN users u ON u.id = e.evaluator_id
      WHERE s.id = $1
      ORDER BY e.created_at DESC
    `;

    const result = await mediumQueryBreaker.fire(client, query, [studentId]);
    client.release();

    res.json({
      studentId: parseInt(studentId),
      totalEvaluations: result.rows.length,
      evaluations: result.rows
    });

  } catch (error) {
    console.error('❌ Error fetching student evaluation history:', error);
    res.status(500).json({
      error: 'Error al obtener el historial de evaluaciones',
      details: error.message
    });
  }
});

// Bulk assignment
app.post('/api/evaluations/assign/bulk', (req, res) => {
  const { applicationIds } = req.body;

  console.log(`🔄 Bulk assigning evaluations to applications:`, applicationIds);

  const results = {
    totalApplications: applicationIds.length,
    successCount: applicationIds.length,
    failureCount: 0,
    successful: applicationIds.map(id => `Application ${id}`),
    failed: [],
    isComplete: true
  };

  // Mock creation of evaluations for each application
  applicationIds.forEach(appId => {
    const evaluationTypes = ['LANGUAGE_EXAM', 'MATHEMATICS_EXAM', 'ENGLISH_EXAM', 'CYCLE_DIRECTOR_REPORT', 'PSYCHOLOGICAL_INTERVIEW'];

    evaluationTypes.forEach((type, index) => {
      const evaluation = {
        id: Date.now() + Math.random() * 1000,
        evaluationType: type,
        status: 'PENDING',
        applicationId: parseInt(appId),
        evaluatorId: Math.floor(Math.random() * 10) + 1,
        createdAt: new Date().toISOString(),
        evaluator: {
          id: Math.floor(Math.random() * 10) + 1,
          firstName: 'Evaluador',
          lastName: `${index + 1}`,
          email: `evaluador${index + 1}@mtn.cl`
        }
      };
      mockEvaluations.push(evaluation);
    });
  });

  res.json(results);
});

// Bulk assignment (public endpoint)
app.post('/api/evaluations/public/assign/bulk', (req, res) => {
  const { applicationIds } = req.body;

  console.log(`🔄 Bulk assigning evaluations to applications (public):`, applicationIds);

  const results = {
    totalApplications: applicationIds.length,
    successCount: applicationIds.length,
    failureCount: 0,
    successful: applicationIds.map(id => `Application ${id}`),
    failed: [],
    isComplete: true
  };

  // Mock creation of evaluations for each application
  applicationIds.forEach(appId => {
    const evaluationTypes = ['LANGUAGE_EXAM', 'MATHEMATICS_EXAM', 'ENGLISH_EXAM', 'CYCLE_DIRECTOR_REPORT', 'PSYCHOLOGICAL_INTERVIEW'];

    evaluationTypes.forEach((type, index) => {
      const evaluation = {
        id: Date.now() + Math.random() * 1000,
        evaluationType: type,
        status: 'PENDING',
        applicationId: parseInt(appId),
        evaluatorId: Math.floor(Math.random() * 10) + 1,
        createdAt: new Date().toISOString(),
        evaluator: {
          id: Math.floor(Math.random() * 10) + 1,
          firstName: 'Evaluador',
          lastName: `${index + 1}`,
          email: `evaluador${index + 1}@mtn.cl`
        }
      };
      mockEvaluations.push(evaluation);
    });
  });

  res.json(results);
});

// Reassign evaluation
app.put('/api/evaluations/:evaluationId/reassign/:newEvaluatorId', (req, res) => {
  const { evaluationId, newEvaluatorId } = req.params;

  console.log(`🔄 Reassigning evaluation ${evaluationId} to evaluator ${newEvaluatorId}`);

  const evaluationIndex = mockEvaluations.findIndex(eval => eval.id === parseInt(evaluationId));

  if (evaluationIndex !== -1) {
    mockEvaluations[evaluationIndex].evaluatorId = parseInt(newEvaluatorId);
    mockEvaluations[evaluationIndex].evaluator = {
      id: parseInt(newEvaluatorId),
      firstName: 'Nuevo',
      lastName: 'Evaluador',
      email: `evaluador${newEvaluatorId}@mtn.cl`
    };
    mockEvaluations[evaluationIndex].updatedAt = new Date().toISOString();

    res.json(mockEvaluations[evaluationIndex]);
  } else {
    res.status(404).json({ error: 'Evaluation not found' });
  }
});

// ============= HU-6: FILE ATTACHMENTS FOR EVALUATIONS =============

const multer = require('multer');
const path = require('path');
const fs = require('fs');

// Configure multer for file uploads
const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    const uploadDir = path.join(__dirname, 'uploads', 'evaluation_attachments');
    if (!fs.existsSync(uploadDir)) {
      fs.mkdirSync(uploadDir, { recursive: true });
    }
    cb(null, uploadDir);
  },
  filename: function (req, file, cb) {
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
    cb(null, 'eval-' + uniqueSuffix + path.extname(file.originalname));
  }
});

const upload = multer({
  storage: storage,
  limits: { fileSize: 10 * 1024 * 1024 }, // 10MB max
  fileFilter: function (req, file, cb) {
    const allowedTypes = /pdf|jpg|jpeg|png|doc|docx/;
    const extname = allowedTypes.test(path.extname(file.originalname).toLowerCase());
    const mimetype = allowedTypes.test(file.mimetype);

    if (mimetype && extname) {
      return cb(null, true);
    } else {
      cb(new Error('Solo se permiten archivos PDF, imágenes (JPG, PNG) y documentos Word'));
    }
  }
});

// POST /api/evaluations/:evaluationId/attachments - Upload attachment
app.post('/api/evaluations/:evaluationId/attachments', upload.single('file'), async (req, res) => {
  const { evaluationId } = req.params;
  const { description } = req.body;

  if (!req.file) {
    return res.status(400).json({ error: 'No se proporcionó ningún archivo' });
  }

  try {
    const client = await dbPool.connect();

    const query = `
      INSERT INTO evaluation_attachments (
        evaluation_id, file_name, original_name, file_path,
        file_size, content_type, description, created_at
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, NOW())
      RETURNING *
    `;

    const values = [
      evaluationId,
      req.file.filename,
      req.file.originalname,
      req.file.path,
      req.file.size,
      req.file.mimetype,
      description || null
    ];

    const result = await writeOperationBreaker.fire(() => client.query(query, values));
    client.release();

    console.log(`✅ File uploaded for evaluation ${evaluationId}: ${req.file.originalname}`);

    res.json({
      success: true,
      attachment: result.rows[0]
    });
  } catch (error) {
    console.error('❌ Error uploading file:', error);

    // Delete uploaded file if database insert fails
    if (req.file && req.file.path) {
      fs.unlink(req.file.path, (err) => {
        if (err) console.error('Error deleting file after failed upload:', err);
      });
    }

    res.status(500).json({
      error: 'Error al subir el archivo',
      details: error.message
    });
  }
});

// GET /api/evaluations/:evaluationId/attachments - Get all attachments
app.get('/api/evaluations/:evaluationId/attachments', async (req, res) => {
  const { evaluationId } = req.params;

  try {
    const client = await dbPool.connect();

    const query = `
      SELECT
        id, evaluation_id, file_name, original_name,
        file_size, content_type, description, created_at
      FROM evaluation_attachments
      WHERE evaluation_id = $1
      ORDER BY created_at DESC
    `;

    const result = await simpleQueryBreaker.fire(client, query, [evaluationId]);
    client.release();

    res.json({
      evaluationId: parseInt(evaluationId),
      totalAttachments: result.rows.length,
      attachments: result.rows
    });
  } catch (error) {
    console.error('❌ Error fetching attachments:', error);
    res.status(500).json({
      error: 'Error al obtener los archivos adjuntos',
      details: error.message
    });
  }
});

// GET /api/evaluations/attachments/:attachmentId/download - Download attachment
app.get('/api/evaluations/attachments/:attachmentId/download', async (req, res) => {
  const { attachmentId } = req.params;

  try {
    const client = await dbPool.connect();

    const query = `
      SELECT file_path, original_name, content_type
      FROM evaluation_attachments
      WHERE id = $1
    `;

    const result = await simpleQueryBreaker.fire(client, query, [attachmentId]);
    client.release();

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Archivo no encontrado' });
    }

    const attachment = result.rows[0];

    if (!fs.existsSync(attachment.file_path)) {
      return res.status(404).json({ error: 'El archivo físico no existe' });
    }

    res.setHeader('Content-Type', attachment.content_type);
    res.setHeader('Content-Disposition', `attachment; filename="${attachment.original_name}"`);

    const fileStream = fs.createReadStream(attachment.file_path);
    fileStream.pipe(res);

  } catch (error) {
    console.error('❌ Error downloading file:', error);
    res.status(500).json({
      error: 'Error al descargar el archivo',
      details: error.message
    });
  }
});

// DELETE /api/evaluations/attachments/:attachmentId - Delete attachment
app.delete('/api/evaluations/attachments/:attachmentId', async (req, res) => {
  const { attachmentId } = req.params;

  try {
    const client = await dbPool.connect();

    // First get the file path
    const selectQuery = `SELECT file_path FROM evaluation_attachments WHERE id = $1`;
    const selectResult = await simpleQueryBreaker.fire(client, selectQuery, [attachmentId]);

    if (selectResult.rows.length === 0) {
      client.release();
      return res.status(404).json({ error: 'Archivo no encontrado' });
    }

    const filePath = selectResult.rows[0].file_path;

    // Delete from database
    const deleteQuery = `DELETE FROM evaluation_attachments WHERE id = $1`;
    await writeOperationBreaker.fire(client, deleteQuery, [attachmentId]);
    client.release();

    // Delete physical file
    if (fs.existsSync(filePath)) {
      fs.unlink(filePath, (err) => {
        if (err) console.error('Error deleting physical file:', err);
      });
    }

    console.log(`✅ Attachment ${attachmentId} deleted successfully`);

    res.json({
      success: true,
      message: 'Archivo eliminado correctamente'
    });
  } catch (error) {
    console.error('❌ Error deleting attachment:', error);
    res.status(500).json({
      error: 'Error al eliminar el archivo',
      details: error.message
    });
  }
});

console.log('✅ File attachment endpoints initialized');

// ============= HU-7: NOTIFICATION TRIGGERS =============

// Function to send notification when evaluation is assigned
async function sendEvaluationAssignedNotification(evaluationId, evaluatorId, applicationId) {
  try {
    const client = await dbPool.connect();

    // Get evaluator email
    const userQuery = `SELECT email, first_name, last_name FROM users WHERE id = $1`;
    const userResult = await simpleQueryBreaker.fire(client, userQuery, [evaluatorId]);

    if (userResult.rows.length === 0) {
      console.error('❌ Evaluator not found');
      client.release();
      return;
    }

    const evaluator = userResult.rows[0];

    // Get application/student info
    const appQuery = `
      SELECT
        s.first_name || ' ' || s.paternal_last_name as student_name,
        s.grade_applied,
        a.application_year
      FROM applications a
      JOIN students s ON s.id = a.student_id
      WHERE a.id = $1
    `;
    const appResult = await simpleQueryBreaker.fire(client, appQuery, [applicationId]);

    if (appResult.rows.length === 0) {
      console.error('❌ Application not found');
      client.release();
      return;
    }

    const application = appResult.rows[0];

    // Create notification record
    const notificationQuery = `
      INSERT INTO notifications (
        user_id, type, title, message, status, created_at
      ) VALUES ($1, $2, $3, $4, $5, NOW())
      RETURNING id
    `;

    const title = 'Nueva Evaluación Asignada';
    const message = `Se te ha asignado una nueva evaluación para el estudiante ${application.student_name} (${application.grade_applied}, Año ${application.application_year})`;

    const notificationValues = [
      evaluatorId,
      'EVALUATION_ASSIGNED',
      title,
      message,
      'PENDING'
    ];

    const notifResult = await writeOperationBreaker.fire(client, notificationQuery, notificationValues);
    const notificationId = notifResult.rows[0].id;

    client.release();

    console.log(`✅ Notification created for evaluator ${evaluator.email}: Evaluation ${evaluationId}`);

    // TODO: Send email via notification service
    // For now, just log it
    console.log(`📧 Email would be sent to: ${evaluator.email}`);
    console.log(`   Subject: ${title}`);
    console.log(`   Message: ${message}`);

    return notificationId;

  } catch (error) {
    console.error('❌ Error sending evaluation notification:', error);
  }
}

// Modified POST /api/evaluations endpoint with notification trigger
app.post('/api/evaluations', async (req, res) => {
  const { application_id, evaluator_id, evaluation_type } = req.body;

  try {
    const client = await dbPool.connect();

    const query = `
      INSERT INTO evaluations (
        application_id, evaluator_id, evaluation_type,
        status, created_at, updated_at
      ) VALUES ($1, $2, $3, $4, NOW(), NOW())
      RETURNING *
    `;

    const values = [
      application_id,
      evaluator_id,
      evaluation_type,
      'PENDING'
    ];

    const result = await writeOperationBreaker.fire(() => client.query(query, values));
    client.release();

    const newEvaluation = result.rows[0];

    console.log(`✅ Evaluation created: ${newEvaluation.id} for application ${application_id}`);

    // HU-7: Send notification to evaluator
    await sendEvaluationAssignedNotification(newEvaluation.id, evaluator_id, application_id);

    res.json(newEvaluation);
  } catch (error) {
    console.error('❌ Error creating evaluation:', error);
    res.status(500).json({
      error: 'Error al crear la evaluación',
      details: error.message
    });
  }
});

console.log('✅ Notification trigger initialized');

console.log('✅ Evaluation management endpoints initialized');

// Cache management endpoints
app.post('/api/evaluations/cache/clear', (req, res) => {
  try {
    const { pattern } = req.body;
    const cleared = evaluationCache.clear(pattern);
    res.json({
      success: true,
      cleared,
      message: pattern ? `Cleared ${cleared} cache entries matching: ${pattern}` : `Cleared ${cleared} total cache entries`
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

app.get('/api/evaluations/cache/stats', (req, res) => {
  res.json({
    cacheStats: evaluationCache.getStats(),
    cacheSize: evaluationCache.size(),
    serviceUptime: process.uptime()
  });
});

app.listen(port, () => {
  console.log(`Evaluation Service running on port ${port}`);
  console.log('✅ Connection pooling enabled');
  console.log('✅ Circuit breaker enabled for database queries');
  console.log('✅ In-memory cache enabled');
  console.log('Cache endpoints:');
  console.log('  - POST /api/evaluations/cache/clear');
  console.log('  - GET  /api/evaluations/cache/stats');
});