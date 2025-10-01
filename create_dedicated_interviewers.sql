-- Crear entrevistadores dedicados (NO profesores del sistema)
-- Estos son personas específicamente contratadas para realizar entrevistas

-- 1. CREAR TABLA DE ENTREVISTADORES si no existe
CREATE TABLE IF NOT EXISTS interviewers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(50),
    specialization VARCHAR(100), -- Tipo de entrevista que realiza
    years_experience INTEGER,
    is_active BOOLEAN DEFAULT true,
    notes TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 2. INSERTAR ENTREVISTADORES DEDICADOS
INSERT INTO interviewers (name, email, phone, specialization, years_experience, is_active, notes) VALUES

-- ENTREVISTADORES ESPECIALIZADOS EN DIFERENTES TIPOS
('Dra. María González Silva', 'maria.gonzalez.entrevistadora@mtn.cl', '+56911111111', 'Psicología Educativa', 8, true, 'Especialista en evaluación psicológica de niños y adolescentes'),

('Dr. Carlos Mendoza Torres', 'carlos.mendoza.entrevistador@mtn.cl', '+56922222222', 'Entrevistas Familiares', 12, true, 'Experto en dinámicas familiares y evaluación de contextos sociofamiliares'),

('Psic. Ana Rivera Campos', 'ana.rivera.entrevistadora@mtn.cl', '+56933333333', 'Evaluación Académica', 6, true, 'Especialista en evaluación de capacidades académicas y adaptación escolar'),

('Prof. Roberto Silva Mora', 'roberto.silva.entrevistador@mtn.cl', '+56944444444', 'Entrevistas Individuales', 10, true, 'Experto en evaluación individual de estudiantes y desarrollo personal'),

('Dra. Patricia López Vega', 'patricia.lopez.entrevistadora@mtn.cl', '+56955555555', 'Comportamiento y Adaptación', 15, true, 'Especialista en evaluación conductual y capacidades de adaptación'),

('Psic. Fernando Morales Díaz', 'fernando.morales.entrevistador@mtn.cl', '+56966666666', 'Evaluación Integral', 9, true, 'Entrevistador integral con enfoque holístico del desarrollo estudiantil');

-- 3. CREAR HORARIOS DE DISPONIBILIDAD PARA CADA ENTREVISTADOR
-- Obtener los IDs de los entrevistadores recién creados
INSERT INTO interviewer_schedules (interviewer_id, day_of_week, start_time, end_time, schedule_type, is_active, year, notes, created_at)
SELECT 
    (SELECT id FROM interviewers WHERE email = 'maria.gonzalez.entrevistadora@mtn.cl'),
    'MONDAY', '09:00:00', '12:00:00', 'RECURRING', true, 2025, 'Horarios matutinos para evaluación psicológica', NOW()
UNION ALL SELECT 
    (SELECT id FROM interviewers WHERE email = 'maria.gonzalez.entrevistadora@mtn.cl'),
    'TUESDAY', '14:00:00', '17:00:00', 'RECURRING', true, 2025, 'Horarios vespertinos para evaluación psicológica', NOW()
UNION ALL SELECT 
    (SELECT id FROM interviewers WHERE email = 'maria.gonzalez.entrevistadora@mtn.cl'),
    'WEDNESDAY', '09:00:00', '12:00:00', 'RECURRING', true, 2025, 'Horarios matutinos para evaluación psicológica', NOW()

-- Dr. Carlos Mendoza Torres - Entrevistas Familiares
UNION ALL SELECT 
    (SELECT id FROM interviewers WHERE email = 'carlos.mendoza.entrevistador@mtn.cl'),
    'TUESDAY', '09:00:00', '13:00:00', 'RECURRING', true, 2025, 'Mañanas para entrevistas familiares completas', NOW()
UNION ALL SELECT 
    (SELECT id FROM interviewers WHERE email = 'carlos.mendoza.entrevistador@mtn.cl'),
    'THURSDAY', '15:00:00', '18:00:00', 'RECURRING', true, 2025, 'Tardes para entrevistas familiares', NOW()
UNION ALL SELECT 
    (SELECT id FROM interviewers WHERE email = 'carlos.mendoza.entrevistador@mtn.cl'),
    'FRIDAY', '10:00:00', '14:00:00', 'RECURRING', true, 2025, 'Horario extendido para familias', NOW()

-- Psic. Ana Rivera Campos - Evaluación Académica  
UNION ALL SELECT 
    (SELECT id FROM interviewers WHERE email = 'ana.rivera.entrevistadora@mtn.cl'),
    'MONDAY', '08:00:00', '12:00:00', 'RECURRING', true, 2025, 'Evaluaciones académicas matutinas', NOW()
UNION ALL SELECT 
    (SELECT id FROM interviewers WHERE email = 'ana.rivera.entrevistadora@mtn.cl'),
    'WEDNESDAY', '14:00:00', '17:00:00', 'RECURRING', true, 2025, 'Evaluaciones académicas vespertinas', NOW()
UNION ALL SELECT 
    (SELECT id FROM interviewers WHERE email = 'ana.rivera.entrevistadora@mtn.cl'),
    'FRIDAY', '09:00:00', '13:00:00', 'RECURRING', true, 2025, 'Evaluaciones académicas de fin de semana', NOW()

-- Prof. Roberto Silva Mora - Entrevistas Individuales
UNION ALL SELECT 
    (SELECT id FROM interviewers WHERE email = 'roberto.silva.entrevistador@mtn.cl'),
    'MONDAY', '14:00:00', '18:00:00', 'RECURRING', true, 2025, 'Entrevistas individuales vespertinas', NOW()
UNION ALL SELECT 
    (SELECT id FROM interviewers WHERE email = 'roberto.silva.entrevistador@mtn.cl'),
    'WEDNESDAY', '09:00:00', '13:00:00', 'RECURRING', true, 2025, 'Entrevistas individuales matutinas', NOW()
UNION ALL SELECT 
    (SELECT id FROM interviewers WHERE email = 'roberto.silva.entrevistador@mtn.cl'),
    'THURSDAY', '15:00:00', '18:00:00', 'RECURRING', true, 2025, 'Entrevistas individuales de jueves', NOW()

-- Dra. Patricia López Vega - Comportamiento y Adaptación
UNION ALL SELECT 
    (SELECT id FROM interviewers WHERE email = 'patricia.lopez.entrevistadora@mtn.cl'),
    'TUESDAY', '08:30:00', '12:30:00', 'RECURRING', true, 2025, 'Evaluación comportamental matutina', NOW()
UNION ALL SELECT 
    (SELECT id FROM interviewers WHERE email = 'patricia.lopez.entrevistadora@mtn.cl'),
    'THURSDAY', '14:00:00', '17:00:00', 'RECURRING', true, 2025, 'Evaluación comportamental vespertina', NOW()
UNION ALL SELECT 
    (SELECT id FROM interviewers WHERE email = 'patricia.lopez.entrevistadora@mtn.cl'),
    'FRIDAY', '09:00:00', '12:00:00', 'RECURRING', true, 2025, 'Sesiones de evaluación de adaptación', NOW()

-- Psic. Fernando Morales Díaz - Evaluación Integral
UNION ALL SELECT 
    (SELECT id FROM interviewers WHERE email = 'fernando.morales.entrevistador@mtn.cl'),
    'MONDAY', '10:00:00', '14:00:00', 'RECURRING', true, 2025, 'Evaluación integral lunes', NOW()
UNION ALL SELECT 
    (SELECT id FROM interviewers WHERE email = 'fernando.morales.entrevistador@mtn.cl'),
    'WEDNESDAY', '15:00:00', '18:00:00', 'RECURRING', true, 2025, 'Evaluación integral miércoles', NOW()
UNION ALL SELECT 
    (SELECT id FROM interviewers WHERE email = 'fernando.morales.entrevistador@mtn.cl'),
    'FRIDAY', '08:00:00', '12:00:00', 'RECURRING', true, 2025, 'Evaluación integral viernes', NOW();

-- 4. VERIFICAR ENTREVISTADORES CREADOS
SELECT 
    i.id,
    i.name as entrevistador,
    i.email,
    i.specialization as especializacion,
    i.years_experience as años_experiencia,
    CASE WHEN i.is_active THEN '✅ Activo' ELSE '❌ Inactivo' END as estado
FROM interviewers i
ORDER BY i.specialization, i.name;

-- 5. MOSTRAR HORARIOS DE DISPONIBILIDAD POR ENTREVISTADOR
SELECT 
    i.name as entrevistador,
    i.specialization as especializacion,
    s.day_of_week as día,
    s.start_time as hora_inicio,
    s.end_time as hora_fin,
    s.notes as notas
FROM interviewers i
JOIN interviewer_schedules s ON s.interviewer_id = i.id
WHERE s.is_active = true
ORDER BY i.name, 
         CASE s.day_of_week 
             WHEN 'MONDAY' THEN 1
             WHEN 'TUESDAY' THEN 2  
             WHEN 'WEDNESDAY' THEN 3
             WHEN 'THURSDAY' THEN 4
             WHEN 'FRIDAY' THEN 5
             WHEN 'SATURDAY' THEN 6
             WHEN 'SUNDAY' THEN 7
         END;

-- 6. RESUMEN POR ESPECIALIZACIÓN
SELECT 
    specialization as especializacion,
    COUNT(*) as cantidad_entrevistadores,
    STRING_AGG(name, ', ' ORDER BY name) as nombres
FROM interviewers 
WHERE is_active = true
GROUP BY specialization
ORDER BY specialization;