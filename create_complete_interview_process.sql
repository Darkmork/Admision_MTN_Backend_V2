-- Crear proceso completo de entrevistas para el sistema de admisión
-- Cada aplicación debe pasar por 4 etapas con diferentes entrevistadores

-- Primero, eliminar entrevistas existentes para empezar limpio
DELETE FROM interviews;

-- Crear entrevistas para cada aplicación siguiendo el proceso completo de admisión
-- Aplicación ID 4 - Juan Pérez (PRIMERO_BASICO)

-- 1. Entrevista Familiar con Director de Ciclo
INSERT INTO interviews (
    application_id, interviewer_id, status, interview_type, 
    scheduled_date, duration_minutes, location, notes, created_at
) VALUES (
    4, 2, 'SCHEDULED', 'FAMILY',
    '2025-09-05 09:00:00', 60,
    'Oficina Dirección',
    'Primera entrevista: Conocer a la familia y sus expectativas educativas',
    NOW()
);

-- 2. Entrevista Individual con Profesor (Evaluación Académica)  
INSERT INTO interviews (
    application_id, interviewer_id, status, interview_type,
    scheduled_date, duration_minutes, location, notes, created_at
) VALUES (
    4, 12, 'SCHEDULED', 'INDIVIDUAL', 
    '2025-09-06 10:30:00', 45,
    'Aula de Evaluaciones',
    'Evaluación académica: Nivel de lectoescritura y matemáticas básicas',
    NOW()
);

-- 3. Evaluación Psicológica
INSERT INTO interviews (
    application_id, interviewer_id, status, interview_type,
    scheduled_date, duration_minutes, location, notes, created_at
) VALUES (
    4, 4, 'SCHEDULED', 'INDIVIDUAL',
    '2025-09-07 14:00:00', 50,
    'Sala de Psicología',
    'Evaluación psicológica: Madurez emocional y habilidades sociales',
    NOW()
);

-- 4. Entrevista de Coordinación (Revisión final)
INSERT INTO interviews (
    application_id, interviewer_id, status, interview_type,
    scheduled_date, duration_minutes, location, notes, created_at  
) VALUES (
    4, 3, 'SCHEDULED', 'FAMILY',
    '2025-09-08 15:30:00', 40,
    'Oficina Coordinación',
    'Revisión final: Coordinación de matrícula y proceso de ingreso',
    NOW()
);

-- Aplicación ID 5 - María López (SEGUNDO_BASICO)

-- 1. Entrevista Familiar con Director de Ciclo
INSERT INTO interviews (
    application_id, interviewer_id, status, interview_type,
    scheduled_date, duration_minutes, location, notes, created_at
) VALUES (
    5, 2, 'SCHEDULED', 'FAMILY',
    '2025-09-10 09:00:00', 60,
    'Oficina Dirección', 
    'Primera entrevista: Conocer a la familia y adaptar expectativas a segundo básico',
    NOW()
);

-- 2. Entrevista Individual con Profesor (Evaluación Académica)
INSERT INTO interviews (
    application_id, interviewer_id, status, interview_type,
    scheduled_date, duration_minutes, location, notes, created_at
) VALUES (
    5, 5, 'SCHEDULED', 'INDIVIDUAL',
    '2025-09-11 11:00:00', 45,
    'Aula de Matemáticas',
    'Evaluación académica: Nivel de matemáticas y comprensión lectora para segundo básico',
    NOW()
);

-- 3. Evaluación Psicológica  
INSERT INTO interviews (
    application_id, interviewer_id, status, interview_type,
    scheduled_date, duration_minutes, location, notes, created_at
) VALUES (
    5, 4, 'SCHEDULED', 'INDIVIDUAL',
    '2025-09-12 14:30:00', 50,
    'Sala de Psicología',
    'Evaluación psicológica: Adaptación social y desarrollo cognitivo',
    NOW()
);

-- 4. Entrevista de Coordinación (Revisión final)
INSERT INTO interviews (
    application_id, interviewer_id, status, interview_type,
    scheduled_date, duration_minutes, location, notes, created_at
) VALUES (
    5, 3, 'SCHEDULED', 'FAMILY', 
    '2025-09-13 16:00:00', 40,
    'Oficina Coordinación',
    'Revisión final: Coordinación de matrícula y proceso de integración',
    NOW()
);

-- Verificar las entrevistas creadas
SELECT 
    i.id,
    i.application_id,
    s.first_name || ' ' || s.paternal_last_name as estudiante,
    u.first_name || ' ' || u.last_name as entrevistador,
    u.role as rol_entrevistador,
    i.interview_type,
    i.scheduled_date,
    i.duration_minutes,
    i.location,
    i.status
FROM interviews i
JOIN applications a ON i.application_id = a.id
JOIN students s ON a.student_id = s.id  
JOIN users u ON i.interviewer_id = u.id
ORDER BY i.application_id, i.scheduled_date;