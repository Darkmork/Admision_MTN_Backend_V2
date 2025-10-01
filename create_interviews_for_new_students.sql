-- Crear entrevistas completas para los nuevos estudiantes con estado INTERVIEW_SCHEDULED
-- Benjamin Vargas (ID: 17) y Florencia Díaz (ID: 24)

INSERT INTO interviews (
    application_id, interviewer_id, status, interview_type,
    scheduled_date, duration_minutes, location, notes, created_at
) VALUES 

-- ===== APLICACIÓN 17: Benjamin Vargas (CUARTO_BASICO) =====
-- 1. Entrevista Familiar con Director de Ciclo
(17, 2, 'SCHEDULED', 'FAMILY', '2025-10-14 09:00:00', 60, 'Oficina Dirección', 'Entrevista inicial: Cambio de colegio para cuarto básico y expectativas académicas', NOW()),
-- 2. Evaluación Académica con Profesor
(17, 12, 'SCHEDULED', 'INDIVIDUAL', '2025-10-15 10:30:00', 55, 'Aula de Evaluaciones', 'Evaluación académica: Competencias avanzadas para cuarto básico', NOW()),
-- 3. Evaluación Psicológica
(17, 4, 'SCHEDULED', 'INDIVIDUAL', '2025-10-16 14:00:00', 50, 'Sala de Psicología', 'Evaluación: Adaptación a nuevo entorno educativo y madurez', NOW()),
-- 4. Coordinación Final
(17, 3, 'SCHEDULED', 'FAMILY', '2025-10-17 15:30:00', 45, 'Oficina Coordinación', 'Coordinación: Plan de integración y seguimiento académico especializado', NOW()),

-- ===== APLICACIÓN 24: Florencia Díaz (SEGUNDO_BASICO) =====
-- 1. Entrevista Familiar con Director de Ciclo
(24, 2, 'SCHEDULED', 'FAMILY', '2025-10-21 08:30:00', 60, 'Oficina Dirección', 'Entrevista inicial: Proceso de admisión segundo básico y objetivos familiares', NOW()),
-- 2. Evaluación Académica con Profesor de Matemáticas
(24, 5, 'SCHEDULED', 'INDIVIDUAL', '2025-10-22 10:00:00', 45, 'Aula de Matemáticas', 'Evaluación académica: Nivel matemático y habilidades de lectoescritura', NOW()),
-- 3. Evaluación Psicológica
(24, 4, 'SCHEDULED', 'INDIVIDUAL', '2025-10-23 14:30:00', 50, 'Sala de Psicología', 'Evaluación: Desarrollo cognitivo y habilidades sociales segundo básico', NOW()),
-- 4. Coordinación Final
(24, 3, 'SCHEDULED', 'FAMILY', '2025-10-24 16:00:00', 40, 'Oficina Coordinación', 'Coordinación: Proceso de matrícula y acompañamiento inicial', NOW());

-- Verificar las entrevistas creadas para los nuevos estudiantes
SELECT 
    i.application_id,
    s.first_name || ' ' || s.paternal_last_name as estudiante,
    s.grade_applied,
    u.first_name || ' ' || u.last_name as entrevistador,
    u.role as rol,
    i.interview_type,
    i.scheduled_date,
    i.duration_minutes,
    i.location,
    ROW_NUMBER() OVER (PARTITION BY i.application_id ORDER BY i.scheduled_date) as etapa
FROM interviews i
JOIN applications a ON i.application_id = a.id
JOIN students s ON a.student_id = s.id
JOIN users u ON i.interviewer_id = u.id
WHERE i.application_id IN (17, 24)
ORDER BY i.application_id, i.scheduled_date;