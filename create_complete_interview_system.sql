-- Crear sistema completo de entrevistas para todas las aplicaciones
-- Cada aplicación debe pasar por 4 etapas del proceso de admisión

-- Eliminar entrevistas existentes
DELETE FROM interviews;

-- Crear entrevistas para todas las aplicaciones existentes
-- Proceso estándar:
-- 1. Entrevista Familiar (CYCLE_DIRECTOR) - Conocer la familia 
-- 2. Evaluación Académica (TEACHER) - Nivel académico del estudiante
-- 3. Evaluación Psicológica (PSYCHOLOGIST) - Desarrollo emocional/social
-- 4. Coordinación Final (COORDINATOR) - Proceso de matrícula

INSERT INTO interviews (
    application_id, interviewer_id, status, interview_type,
    scheduled_date, duration_minutes, location, notes, created_at
) VALUES 

-- ===== APLICACIÓN 4: Juan Pérez (PRIMERO_BASICO) =====
-- 1. Entrevista Familiar con Director de Ciclo
(4, 2, 'SCHEDULED', 'FAMILY', '2025-09-02 09:00:00', 60, 'Oficina Dirección', 'Entrevista inicial: Conocer expectativas familiares para primer año básico', NOW()),
-- 2. Evaluación Académica con Profesor 
(4, 12, 'SCHEDULED', 'INDIVIDUAL', '2025-09-03 10:30:00', 45, 'Aula de Evaluaciones', 'Evaluación preescolar: Preparación para lectoescritura y números', NOW()),
-- 3. Evaluación Psicológica
(4, 4, 'SCHEDULED', 'INDIVIDUAL', '2025-09-04 14:00:00', 50, 'Sala de Psicología', 'Evaluación: Madurez para ingreso escolar y habilidades sociales', NOW()),
-- 4. Coordinación Final
(4, 3, 'SCHEDULED', 'FAMILY', '2025-09-05 15:30:00', 40, 'Oficina Coordinación', 'Coordinación: Proceso de matrícula y orientación familiar', NOW()),

-- ===== APLICACIÓN 5: María López (SEGUNDO_BASICO) =====
-- 1. Entrevista Familiar con Director de Ciclo
(5, 2, 'SCHEDULED', 'FAMILY', '2025-09-09 09:00:00', 60, 'Oficina Dirección', 'Entrevista inicial: Adaptación a segundo básico y expectativas familiares', NOW()),
-- 2. Evaluación Académica con Profesor de Matemáticas
(5, 5, 'SCHEDULED', 'INDIVIDUAL', '2025-09-10 11:00:00', 45, 'Aula de Matemáticas', 'Evaluación académica: Nivel matemático y comprensión lectora segundo básico', NOW()),
-- 3. Evaluación Psicológica
(5, 4, 'SCHEDULED', 'INDIVIDUAL', '2025-09-11 14:30:00', 50, 'Sala de Psicología', 'Evaluación: Desarrollo cognitivo y adaptación social segundo básico', NOW()),
-- 4. Coordinación Final
(5, 3, 'SCHEDULED', 'FAMILY', '2025-09-12 16:00:00', 40, 'Oficina Coordinación', 'Coordinación: Integración escolar y seguimiento académico', NOW()),

-- ===== APLICACIÓN 11: Carlos Ramírez (TERCERO_BASICO) =====
-- 1. Entrevista Familiar con Director de Ciclo
(11, 2, 'SCHEDULED', 'FAMILY', '2025-09-16 09:30:00', 60, 'Oficina Dirección', 'Entrevista inicial: Cambio de colegio y adaptación a tercer básico', NOW()),
-- 2. Evaluación Académica con Profesor
(11, 12, 'SCHEDULED', 'INDIVIDUAL', '2025-09-17 10:00:00', 50, 'Aula de Evaluaciones', 'Evaluación académica: Competencias de lenguaje y matemáticas tercer básico', NOW()),
-- 3. Evaluación Psicológica
(11, 4, 'SCHEDULED', 'INDIVIDUAL', '2025-09-18 15:00:00', 55, 'Sala de Psicología', 'Evaluación: Adaptación a nuevo ambiente y desarrollo emocional', NOW()),
-- 4. Coordinación Final
(11, 3, 'SCHEDULED', 'FAMILY', '2025-09-19 14:30:00', 45, 'Oficina Coordinación', 'Coordinación: Plan de integración y seguimiento especializado', NOW()),

-- ===== APLICACIÓN 12: Sofía González (SEGUNDO_BASICO) =====
-- 1. Entrevista Familiar con Director de Ciclo  
(12, 2, 'SCHEDULED', 'FAMILY', '2025-09-23 08:30:00', 60, 'Oficina Dirección', 'Entrevista inicial: Transición educativa y objetivos familiares', NOW()),
-- 2. Evaluación Académica con Profesor de Matemáticas
(12, 5, 'SCHEDULED', 'INDIVIDUAL', '2025-09-24 09:30:00', 45, 'Aula de Matemáticas', 'Evaluación académica: Nivelación matemática y habilidades lectoras', NOW()),
-- 3. Evaluación Psicológica
(12, 4, 'SCHEDULED', 'INDIVIDUAL', '2025-09-25 13:30:00', 50, 'Sala de Psicología', 'Evaluación: Perfil cognitivo y habilidades de integración social', NOW()),
-- 4. Coordinación Final
(12, 3, 'SCHEDULED', 'FAMILY', '2025-09-26 15:00:00', 40, 'Oficina Coordinación', 'Coordinación: Programa de apoyo y seguimiento académico', NOW()),

-- ===== APLICACIÓN 13: Diego Castillo (CUARTO_BASICO) =====
-- 1. Entrevista Familiar con Director de Ciclo
(13, 2, 'SCHEDULED', 'FAMILY', '2025-09-30 10:00:00', 65, 'Oficina Dirección', 'Entrevista inicial: Necesidades especiales y plan educativo individualizado', NOW()),
-- 2. Evaluación Académica con Profesor
(13, 12, 'SCHEDULED', 'INDIVIDUAL', '2025-10-01 11:30:00', 60, 'Aula de Evaluaciones', 'Evaluación académica: Adaptaciones curriculares para cuarto básico', NOW()),
-- 3. Evaluación Psicológica (Extendida por necesidades especiales)
(13, 4, 'SCHEDULED', 'INDIVIDUAL', '2025-10-02 14:00:00', 75, 'Sala de Psicología', 'Evaluación especializada: Plan de inclusión y apoyos necesarios', NOW()),
-- 4. Coordinación Final
(13, 3, 'SCHEDULED', 'FAMILY', '2025-10-03 16:30:00', 50, 'Oficina Coordinación', 'Coordinación: Programa de inclusión y equipo multidisciplinario', NOW()),

-- ===== APLICACIÓN 14: Valentina Herrera (TERCERO_BASICO) =====
-- 1. Entrevista Familiar con Director de Ciclo
(14, 2, 'SCHEDULED', 'FAMILY', '2025-10-07 09:00:00', 60, 'Oficina Dirección', 'Entrevista inicial: Perfil académico y desarrollo integral tercer básico', NOW()),
-- 2. Evaluación Académica con Profesor
(14, 12, 'SCHEDULED', 'INDIVIDUAL', '2025-10-08 10:15:00', 50, 'Aula de Evaluaciones', 'Evaluación académica: Competencias curriculares y proyección académica', NOW()),
-- 3. Evaluación Psicológica
(14, 4, 'SCHEDULED', 'INDIVIDUAL', '2025-10-09 15:30:00', 50, 'Sala de Psicología', 'Evaluación: Perfil de personalidad y habilidades interpersonales', NOW()),
-- 4. Coordinación Final
(14, 3, 'SCHEDULED', 'FAMILY', '2025-10-10 14:00:00', 40, 'Oficina Coordinación', 'Coordinación: Acompañamiento académico y desarrollo de talentos', NOW());

-- Verificar el sistema completo creado
SELECT 
    'RESUMEN DEL SISTEMA DE ENTREVISTAS COMPLETO' as titulo,
    COUNT(*) as total_entrevistas,
    COUNT(DISTINCT application_id) as aplicaciones_con_entrevistas,
    COUNT(DISTINCT interviewer_id) as entrevistadores_participantes
FROM interviews
UNION ALL
SELECT 
    'DETALLE POR APLICACIÓN' as titulo,
    application_id::text as total_entrevistas,
    s.first_name || ' ' || s.paternal_last_name as aplicaciones_con_entrevistas,
    s.grade_applied as entrevistadores_participantes
FROM interviews i
JOIN applications a ON i.application_id = a.id
JOIN students s ON a.student_id = s.id
GROUP BY application_id, s.first_name, s.paternal_last_name, s.grade_applied
ORDER BY titulo, total_entrevistas;

-- Mostrar cronograma detallado
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
ORDER BY i.application_id, i.scheduled_date;