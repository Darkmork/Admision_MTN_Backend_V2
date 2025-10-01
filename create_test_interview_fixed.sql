-- Crear una entrevista de prueba para generar correos personalizados (versión corregida)

-- Primero verificar que tenemos aplicaciones disponibles
SELECT 
    a.id as application_id,
    s.first_name,
    s.paternal_last_name,
    s.maternal_last_name,
    s.school_applied,
    u.email as applicant_email,
    a.status
FROM applications a
JOIN students s ON a.student_id = s.id
JOIN users u ON a.applicant_user_id = u.id
WHERE a.status IN ('PENDING', 'UNDER_REVIEW')
LIMIT 3;

-- Crear una entrevista para generar correo personalizado
-- Usaremos la primera aplicación disponible y un usuario como entrevistador
INSERT INTO interviews (
    application_id,
    interviewer_user_id,
    type,
    mode,
    scheduled_date,
    scheduled_time,
    duration,
    location,
    notes,
    status,
    created_at
) VALUES (
    (SELECT id FROM applications WHERE status = 'PENDING' LIMIT 1),
    (SELECT id FROM users WHERE role = 'CYCLE_DIRECTOR' LIMIT 1),
    'FAMILY',
    'IN_PERSON',
    CURRENT_DATE + INTERVAL '7 days',
    '10:00:00',
    45,
    'Sala de Reuniones - Monte Tabor',
    'Entrevista familiar para proceso de admisión 2025',
    'SCHEDULED',
    CURRENT_TIMESTAMP
);

-- Verificar que se creó la entrevista
SELECT 
    i.id as interview_id,
    i.scheduled_date,
    i.scheduled_time,
    i.type,
    i.mode,
    i.duration,
    i.location,
    a.id as application_id,
    s.first_name || ' ' || s.paternal_last_name as student_name,
    s.school_applied,
    u.email as family_email,
    interviewer.first_name || ' ' || interviewer.last_name as interviewer_name
FROM interviews i
JOIN applications a ON i.application_id = a.id
JOIN students s ON a.student_id = s.id
JOIN users u ON a.applicant_user_id = u.id
JOIN users interviewer ON i.interviewer_user_id = interviewer.id
WHERE i.id = (SELECT MAX(id) FROM interviews);

SELECT 'Entrevista de prueba creada exitosamente para generar correo personalizado' as status;