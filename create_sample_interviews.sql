-- Crear entrevistas de muestra para testing
-- Usaremos aplicaciones y usuarios existentes

INSERT INTO interviews (
    application_id, interviewer_user_id, status, type, mode, 
    scheduled_date, scheduled_time, duration, location, 
    notes, follow_up_required, created_at, updated_at
) VALUES
-- Entrevistas programadas para esta semana
(1, 32, 'SCHEDULED', 'FAMILY', 'IN_PERSON', '2025-08-26', '09:00:00', 60, 'Sala de Reuniones - Monte Tabor', 'Entrevista inicial familia Ramírez', false, NOW(), NOW()),
(2, 33, 'SCHEDULED', 'PSYCHOLOGICAL', 'VIRTUAL', '2025-08-26', '10:30:00', 45, 'Virtual - Zoom', 'Evaluación psicológica estudiante', false, NOW(), NOW()),
(3, 34, 'CONFIRMED', 'FAMILY', 'IN_PERSON', '2025-08-27', '11:00:00', 60, 'Oficina Director', 'Entrevista confirmada familia González', false, NOW(), NOW()),
(4, 35, 'SCHEDULED', 'INDIVIDUAL', 'IN_PERSON', '2025-08-27', '14:00:00', 30, 'Sala de Entrevistas', 'Entrevista individual estudiante', false, NOW(), NOW()),
(5, 32, 'CONFIRMED', 'FAMILY', 'HYBRID', '2025-08-28', '09:30:00', 60, 'Presencial + Virtual', 'Modalidad híbrida por distancia', false, NOW(), NOW()),

-- Entrevistas de la semana pasada (algunas completadas)
(6, 33, 'COMPLETED', 'PSYCHOLOGICAL', 'IN_PERSON', '2025-08-19', '10:00:00', 45, 'Consultorio Psicología', 'Evaluación completa', false, NOW() - INTERVAL '5 days', NOW() - INTERVAL '4 days'),
(7, 34, 'COMPLETED', 'FAMILY', 'IN_PERSON', '2025-08-20', '11:30:00', 60, 'Sala Principal', 'Entrevista exitosa', false, NOW() - INTERVAL '4 days', NOW() - INTERVAL '3 days'),
(8, 35, 'NO_SHOW', 'INDIVIDUAL', 'VIRTUAL', '2025-08-21', '15:00:00', 30, 'Virtual - Meet', 'Estudiante no se presentó', true, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
(9, 32, 'CANCELLED', 'FAMILY', 'IN_PERSON', '2025-08-22', '09:00:00', 60, 'Sala de Reuniones', 'Cancelada por emergencia familiar', false, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),

-- Entrevistas próximas (esta semana y siguiente)
(10, 33, 'SCHEDULED', 'PSYCHOLOGICAL', 'VIRTUAL', '2025-08-29', '10:00:00', 45, 'Virtual - Teams', 'Evaluación psicopedagógica', false, NOW(), NOW()),
(11, 34, 'SCHEDULED', 'FAMILY', 'IN_PERSON', '2025-08-29', '16:00:00', 60, 'Oficina Director', 'Segunda entrevista familia', false, NOW(), NOW()),
(12, 35, 'CONFIRMED', 'INDIVIDUAL', 'IN_PERSON', '2025-08-30', '08:30:00', 30, 'Aula 201', 'Entrevista académica', false, NOW(), NOW()),
(13, 32, 'SCHEDULED', 'FAMILY', 'HYBRID', '2025-09-02', '14:30:00', 60, 'Sala Multiuso', 'Entrevista final proceso', false, NOW(), NOW()),
(14, 33, 'SCHEDULED', 'PSYCHOLOGICAL', 'IN_PERSON', '2025-09-03', '11:00:00', 45, 'Consultorio', 'Evaluación especializada', false, NOW(), NOW()),
(15, 34, 'SCHEDULED', 'FAMILY', 'VIRTUAL', '2025-09-04', '15:30:00', 60, 'Virtual - Zoom', 'Entrevista remota por ubicación', false, NOW(), NOW());

-- Actualizar algunas entrevistas completadas con resultados
UPDATE interviews SET 
    result = 'POSITIVE',
    score = 8.5,
    recommendations = 'Estudiante con excelente potencial académico. Se recomienda admisión.',
    completed_at = updated_at
WHERE id IN (SELECT id FROM interviews WHERE status = 'COMPLETED' LIMIT 1);

UPDATE interviews SET 
    result = 'NEUTRAL',
    score = 6.5,
    recommendations = 'Estudiante requiere seguimiento adicional en matemáticas.',
    follow_up_required = true,
    follow_up_notes = 'Programar refuerzo en matemáticas antes del inicio de clases',
    completed_at = updated_at
WHERE id IN (SELECT id FROM interviews WHERE status = 'COMPLETED' OFFSET 1 LIMIT 1);
