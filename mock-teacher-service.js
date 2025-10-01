const express = require('express');
const app = express();
const port = 8087;

// Middleware
app.use(express.json());
app.use((req, res, next) => {
  res.header('Access-Control-Allow-Origin', '*');
  res.header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.header('Access-Control-Allow-Headers', 'Origin, X-Requested-With, Content-Type, Accept, Authorization, Cache-Control');
  if (req.method === 'OPTIONS') {
    res.sendStatus(200);
  } else {
    next();
  }
});

// Mock data for teacher
const teacherData = {
  profile: {
    id: 24,
    firstName: "Carlos",
    lastName: "Rodríguez",
    email: "carlos.rodriguez@mtn.cl",
    role: "TEACHER",
    subjects: ["Matemáticas", "Física"],
    grades: ["7°", "8°", "1° Medio"],
    yearsExperience: 12,
    department: "Ciencias Exactas",
    status: "ACTIVE"
  },
  dashboard: {
    pendingEvaluations: 8,
    completedEvaluations: 45,
    scheduledInterviews: 3,
    completedInterviews: 12,
    averageEvaluationScore: 82.5,
    studentsEvaluated: 57,
    upcomingDeadlines: 2,
    recentActivity: [
      {
        type: "evaluation_submitted",
        studentName: "Juan Pérez",
        timestamp: "2025-09-05T14:30:00Z",
        subject: "Matemáticas",
        score: 85
      },
      {
        type: "interview_scheduled",
        studentName: "María González",
        timestamp: "2025-09-05T10:00:00Z",
        date: "2025-09-07",
        time: "10:00"
      }
    ]
  },
  assignedStudents: [
    {
      id: 1,
      applicationId: 101,
      studentName: "Juan Pérez",
      grade: "8°",
      subject: "Matemáticas",
      evaluationStatus: "PENDING",
      interviewStatus: "SCHEDULED",
      assignedDate: "2025-09-01",
      deadline: "2025-09-10"
    },
    {
      id: 2,
      applicationId: 102,
      studentName: "María González",
      grade: "7°",
      subject: "Matemáticas",
      evaluationStatus: "COMPLETED",
      evaluationScore: 88,
      interviewStatus: "COMPLETED",
      assignedDate: "2025-08-28",
      completedDate: "2025-09-04"
    },
    {
      id: 3,
      applicationId: 103,
      studentName: "Pedro Sánchez",
      grade: "1° Medio",
      subject: "Física",
      evaluationStatus: "IN_PROGRESS",
      interviewStatus: "PENDING",
      assignedDate: "2025-09-03",
      deadline: "2025-09-12"
    },
    {
      id: 4,
      applicationId: 104,
      studentName: "Ana Torres",
      grade: "8°",
      subject: "Matemáticas",
      evaluationStatus: "COMPLETED",
      evaluationScore: 92,
      interviewStatus: "COMPLETED",
      assignedDate: "2025-08-25",
      completedDate: "2025-09-02"
    },
    {
      id: 5,
      applicationId: 105,
      studentName: "Luis Martínez",
      grade: "7°",
      subject: "Física",
      evaluationStatus: "PENDING",
      interviewStatus: "NOT_SCHEDULED",
      assignedDate: "2025-09-04",
      deadline: "2025-09-14"
    }
  ],
  evaluations: [
    {
      id: 1,
      studentId: 2,
      studentName: "María González",
      type: "ACADEMIC",
      subject: "Matemáticas",
      status: "COMPLETED",
      scores: {
        knowledge: 85,
        reasoning: 90,
        problemSolving: 88,
        communication: 87,
        overall: 88
      },
      comments: "Excelente desempeño en razonamiento lógico. Muestra gran potencial.",
      evaluationDate: "2025-09-04",
      evaluatorId: 24
    },
    {
      id: 2,
      studentId: 4,
      studentName: "Ana Torres",
      type: "ACADEMIC",
      subject: "Matemáticas",
      status: "COMPLETED",
      scores: {
        knowledge: 90,
        reasoning: 93,
        problemSolving: 92,
        communication: 91,
        overall: 92
      },
      comments: "Sobresaliente en todas las áreas. Candidata excepcional.",
      evaluationDate: "2025-09-02",
      evaluatorId: 24
    }
  ],
  interviews: [
    {
      id: 1,
      studentId: 1,
      studentName: "Juan Pérez",
      type: "ACADEMIC",
      subject: "Matemáticas",
      scheduledDate: "2025-09-07",
      scheduledTime: "10:00",
      duration: 30,
      location: "Sala 204",
      status: "SCHEDULED",
      notes: "Evaluar conocimientos básicos de álgebra y geometría"
    },
    {
      id: 2,
      studentId: 2,
      studentName: "María González",
      type: "ACADEMIC",
      subject: "Matemáticas",
      scheduledDate: "2025-09-03",
      scheduledTime: "14:00",
      duration: 30,
      location: "Sala 204",
      status: "COMPLETED",
      result: "APPROVED",
      score: 88,
      notes: "Excelente comunicación y razonamiento matemático",
      feedback: "La estudiante demostró sólidos conocimientos y gran capacidad de análisis"
    },
    {
      id: 3,
      studentId: 3,
      studentName: "Pedro Sánchez",
      type: "ACADEMIC",
      subject: "Física",
      scheduledDate: "2025-09-09",
      scheduledTime: "11:00",
      duration: 45,
      location: "Laboratorio de Física",
      status: "SCHEDULED",
      notes: "Incluir experimento práctico de mecánica"
    }
  ],
  schedule: {
    monday: [
      { time: "09:00-10:00", available: true },
      { time: "10:00-11:00", available: false, activity: "Clase 8°A" },
      { time: "11:00-12:00", available: false, activity: "Clase 7°B" },
      { time: "14:00-15:00", available: true },
      { time: "15:00-16:00", available: true }
    ],
    tuesday: [
      { time: "09:00-10:00", available: false, activity: "Clase 1° Medio" },
      { time: "10:00-11:00", available: true },
      { time: "11:00-12:00", available: true },
      { time: "14:00-15:00", available: false, activity: "Reunión Departamento" },
      { time: "15:00-16:00", available: true }
    ],
    wednesday: [
      { time: "09:00-10:00", available: true },
      { time: "10:00-11:00", available: false, activity: "Clase 8°B" },
      { time: "11:00-12:00", available: false, activity: "Clase 7°A" },
      { time: "14:00-15:00", available: true },
      { time: "15:00-16:00", available: true }
    ],
    thursday: [
      { time: "09:00-10:00", available: false, activity: "Clase 1° Medio" },
      { time: "10:00-11:00", available: true },
      { time: "11:00-12:00", available: true },
      { time: "14:00-15:00", available: true },
      { time: "15:00-16:00", available: false, activity: "Tutoría" }
    ],
    friday: [
      { time: "09:00-10:00", available: true },
      { time: "10:00-11:00", available: true },
      { time: "11:00-12:00", available: false, activity: "Clase 8°A" },
      { time: "14:00-15:00", available: true },
      { time: "15:00-16:00", available: true }
    ]
  },
  evaluationTemplates: [
    {
      id: 1,
      name: "Evaluación Matemáticas 7°-8°",
      subject: "Matemáticas",
      grades: ["7°", "8°"],
      criteria: [
        { name: "Conocimientos básicos", weight: 25, maxScore: 100 },
        { name: "Razonamiento lógico", weight: 30, maxScore: 100 },
        { name: "Resolución de problemas", weight: 30, maxScore: 100 },
        { name: "Comunicación matemática", weight: 15, maxScore: 100 }
      ]
    },
    {
      id: 2,
      name: "Evaluación Física 1° Medio",
      subject: "Física",
      grades: ["1° Medio"],
      criteria: [
        { name: "Conceptos fundamentales", weight: 25, maxScore: 100 },
        { name: "Aplicación práctica", weight: 35, maxScore: 100 },
        { name: "Análisis experimental", weight: 25, maxScore: 100 },
        { name: "Comunicación científica", weight: 15, maxScore: 100 }
      ]
    }
  ],
  reports: {
    weekly: {
      evaluationsCompleted: 5,
      interviewsConducted: 2,
      averageScore: 85.5,
      pendingTasks: 3
    },
    monthly: {
      evaluationsCompleted: 22,
      interviewsConducted: 8,
      averageScore: 83.2,
      topPerformers: [
        { name: "Ana Torres", score: 92 },
        { name: "María González", score: 88 },
        { name: "Carlos Díaz", score: 87 }
      ],
      recommendedForAdmission: 18,
      notRecommended: 4
    }
  }
};

// Teacher endpoints

// Get teacher profile
app.get('/api/teacher/profile', (req, res) => {
  console.log('👨‍🏫 Teacher profile request received');
  res.json({
    success: true,
    data: teacherData.profile,
    timestamp: new Date().toISOString()
  });
});

// Get teacher dashboard
app.get('/api/teacher/dashboard', (req, res) => {
  console.log('📊 Teacher dashboard request received');
  res.json({
    success: true,
    data: teacherData.dashboard,
    timestamp: new Date().toISOString()
  });
});

// Get assigned students
app.get('/api/teacher/students', (req, res) => {
  console.log('👥 Assigned students request received');
  const { status, subject } = req.query;
  
  let filteredStudents = [...teacherData.assignedStudents];
  
  if (status) {
    filteredStudents = filteredStudents.filter(s => s.evaluationStatus === status);
  }
  
  if (subject) {
    filteredStudents = filteredStudents.filter(s => s.subject === subject);
  }
  
  res.json({
    success: true,
    data: filteredStudents,
    count: filteredStudents.length,
    timestamp: new Date().toISOString()
  });
});

// Get evaluations
app.get('/api/teacher/evaluations', (req, res) => {
  console.log('📝 Teacher evaluations request received');
  res.json({
    success: true,
    data: teacherData.evaluations,
    count: teacherData.evaluations.length,
    timestamp: new Date().toISOString()
  });
});

// Get specific evaluation
app.get('/api/teacher/evaluations/:id', (req, res) => {
  console.log(`📝 Specific evaluation ${req.params.id} request received`);
  const evaluation = teacherData.evaluations.find(e => e.id === parseInt(req.params.id));
  
  if (evaluation) {
    res.json({
      success: true,
      data: evaluation,
      timestamp: new Date().toISOString()
    });
  } else {
    res.status(404).json({
      success: false,
      error: 'Evaluation not found'
    });
  }
});

// Submit evaluation
app.post('/api/teacher/evaluations', (req, res) => {
  console.log('📝 Submit evaluation request received');
  const newEvaluation = {
    id: teacherData.evaluations.length + 1,
    ...req.body,
    evaluatorId: 24,
    submittedAt: new Date().toISOString()
  };
  
  res.json({
    success: true,
    message: 'Evaluation submitted successfully',
    data: newEvaluation,
    timestamp: new Date().toISOString()
  });
});

// Update evaluation
app.put('/api/teacher/evaluations/:id', (req, res) => {
  console.log(`📝 Update evaluation ${req.params.id} request received`);
  res.json({
    success: true,
    message: 'Evaluation updated successfully',
    data: { id: req.params.id, ...req.body },
    timestamp: new Date().toISOString()
  });
});

// Get teacher interviews
app.get('/api/teacher/interviews', (req, res) => {
  console.log('📅 Teacher interviews request received');
  const { status } = req.query;
  
  let filteredInterviews = [...teacherData.interviews];
  
  if (status) {
    filteredInterviews = filteredInterviews.filter(i => i.status === status);
  }
  
  res.json({
    success: true,
    data: filteredInterviews,
    count: filteredInterviews.length,
    timestamp: new Date().toISOString()
  });
});

// Schedule interview
app.post('/api/teacher/interviews', (req, res) => {
  console.log('📅 Schedule interview request received');
  const newInterview = {
    id: teacherData.interviews.length + 1,
    ...req.body,
    teacherId: 24,
    status: 'SCHEDULED',
    createdAt: new Date().toISOString()
  };
  
  res.json({
    success: true,
    message: 'Interview scheduled successfully',
    data: newInterview,
    timestamp: new Date().toISOString()
  });
});

// Update interview
app.put('/api/teacher/interviews/:id', (req, res) => {
  console.log(`📅 Update interview ${req.params.id} request received`);
  res.json({
    success: true,
    message: 'Interview updated successfully',
    data: { id: req.params.id, ...req.body },
    timestamp: new Date().toISOString()
  });
});

// Get teacher schedule
app.get('/api/teacher/schedule', (req, res) => {
  console.log('📆 Teacher schedule request received');
  const { day } = req.query;
  
  if (day && teacherData.schedule[day.toLowerCase()]) {
    res.json({
      success: true,
      data: { [day]: teacherData.schedule[day.toLowerCase()] },
      timestamp: new Date().toISOString()
    });
  } else {
    res.json({
      success: true,
      data: teacherData.schedule,
      timestamp: new Date().toISOString()
    });
  }
});

// Get available time slots
app.get('/api/teacher/schedule/available', (req, res) => {
  console.log('🕐 Available time slots request received');
  const { date } = req.query;
  
  // Get day of week from date if provided
  let daySchedule = [];
  if (date) {
    const dayOfWeek = new Date(date).toLocaleDateString('en-US', { weekday: 'long' }).toLowerCase();
    daySchedule = teacherData.schedule[dayOfWeek] || [];
  }
  
  const availableSlots = daySchedule.filter(slot => slot.available).map(slot => slot.time);
  
  res.json({
    success: true,
    data: {
      date: date || 'all',
      availableSlots: availableSlots,
      count: availableSlots.length
    },
    timestamp: new Date().toISOString()
  });
});

// Get evaluation templates
app.get('/api/teacher/templates', (req, res) => {
  console.log('📋 Evaluation templates request received');
  res.json({
    success: true,
    data: teacherData.evaluationTemplates,
    count: teacherData.evaluationTemplates.length,
    timestamp: new Date().toISOString()
  });
});

// Get reports
app.get('/api/teacher/reports', (req, res) => {
  console.log('📊 Teacher reports request received');
  const { period } = req.query;
  
  if (period === 'weekly') {
    res.json({
      success: true,
      data: teacherData.reports.weekly,
      period: 'weekly',
      timestamp: new Date().toISOString()
    });
  } else if (period === 'monthly') {
    res.json({
      success: true,
      data: teacherData.reports.monthly,
      period: 'monthly',
      timestamp: new Date().toISOString()
    });
  } else {
    res.json({
      success: true,
      data: teacherData.reports,
      timestamp: new Date().toISOString()
    });
  }
});

// Export grades
app.get('/api/teacher/export/grades', (req, res) => {
  console.log('📊 Export grades request received');
  res.json({
    success: true,
    message: 'Grades export initiated',
    data: {
      format: req.query.format || 'xlsx',
      downloadUrl: '/downloads/grades_export_20250905.xlsx',
      expiresIn: '24 hours'
    },
    timestamp: new Date().toISOString()
  });
});

// Health check
app.get('/health', (req, res) => {
  res.json({
    status: 'UP',
    service: 'teacher-mock-service',
    port: port,
    timestamp: new Date().toISOString()
  });
});

// Error handling
app.use((err, req, res, next) => {
  console.error('Teacher service error:', err);
  res.status(500).json({
    error: 'Internal server error',
    message: err.message,
    service: 'teacher-mock'
  });
});

// Start server
app.listen(port, () => {
  console.log(`👨‍🏫 Mock Teacher Service running on port ${port}`);
  console.log(`📚 Available endpoints:`);
  console.log(`   - GET /api/teacher/profile`);
  console.log(`   - GET /api/teacher/dashboard`);
  console.log(`   - GET /api/teacher/students`);
  console.log(`   - GET /api/teacher/evaluations`);
  console.log(`   - POST /api/teacher/evaluations`);
  console.log(`   - GET /api/teacher/interviews`);
  console.log(`   - POST /api/teacher/interviews`);
  console.log(`   - GET /api/teacher/schedule`);
  console.log(`   - GET /api/teacher/templates`);
  console.log(`   - GET /api/teacher/reports`);
  console.log(`   - GET /health`);
});