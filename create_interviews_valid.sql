-- Crear entrevistas de muestra para testing usando IDs válidos de applications

INSERT INTO interviews (
    application_id, interviewer_user_id, status, type, mode, 
    scheduled_date, scheduled_time, duration, location, 
    notes, follow_up_required, result, score, recommendations, completed_at, created_at, updated_at
) VALUES
-- Entrevistas programadas para esta semana
(9, 32, 'SCHEDULED', 'FAMILY', 'IN_PERSON', '2025-08-26', '09:00:00', 60, 'Sala de Reuniones - Monte Tabor', 'Entrevista inicial familia Ramírez', false, NULL, NULL, NULL, NULL, NOW(), NOW()),
(10, 33, 'SCHEDULED', 'PSYCHOLOGICAL', 'VIRTUAL', '2025-08-26', '10:30:00', 45, 'Virtual - Zoom', 'Evaluación psicológica estudiante', false, NULL, NULL, NULL, NULL, NOW(), NOW()),
(11, 34, 'CONFIRMED', 'FAMILY', 'IN_PERSON', '2025-08-27', '11:00:00', 60, 'Oficina Director', 'Entrevista confirmada familia González', false, NULL, NULL, NULL, NULL, NOW(), NOW()),
(12, 35, 'SCHEDULED', 'INDIVIDUAL', 'IN_PERSON', '2025-08-27', '14:00:00', 30, 'Sala de Entrevistas', 'Entrevista individual estudiante', false, NULL, NULL, NULL, NULL, NOW(), NOW()),
(13, 32, 'CONFIRMED', 'FAMILY', 'HYBRID', '2025-08-28', '09:30:00', 60, 'Presencial + Virtual', 'Modalidad híbrida por distancia', false, NULL, NULL, NULL, NULL, NOW(), NOW()),

-- Entrevistas completadas con resultados incluidos
(14, 33, 'COMPLETED', 'PSYCHOLOGICAL', 'IN_PERSON', '2025-08-19', '10:00:00', 45, 'Consultorio Psicología', 'Evaluación completa', false, 'POSITIVE', 8.5, 'Estudiante con excelente potencial académico. Se recomienda admisión.', NOW() - INTERVAL '4 days', NOW() - INTERVAL '5 days', NOW() - INTERVAL '4 days'),
(15, 34, 'COMPLETED', 'FAMILY', 'IN_PERSON', '2025-08-20', '11:30:00', 60, 'Sala Principal', 'Entrevista exitosa', true, 'NEUTRAL', 6.5, 'Estudiante requiere seguimiento adicional en matemáticas.', NOW() - INTERVAL '3 days', NOW() - INTERVAL '4 days', NOW() - INTERVAL '3 days'),

-- Entrevistas con problemas
(16, 35, 'NO_SHOW', 'INDIVIDUAL', 'VIRTUAL', '2025-08-21', '15:00:00', 30, 'Virtual - Meet', 'Estudiante no se presentó', true, NULL, NULL, NULL, NULL, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
(17, 32, 'CANCELLED', 'FAMILY', 'IN_PERSON', '2025-08-22', '09:00:00', 60, 'Sala de Reuniones', 'Cancelada por emergencia familiar', false, NULL, NULL, NULL, NULL, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),

-- Entrevistas próximas
(18, 33, 'SCHEDULED', 'PSYCHOLOGICAL', 'VIRTUAL', '2025-08-29', '10:00:00', 45, 'Virtual - Teams', 'Evaluación psicopedagógica', false, NULL, NULL, NULL, NULL, NOW(), NOW()),
(19, 34, 'SCHEDULED', 'FAMILY', 'IN_PERSON', '2025-08-29', '16:00:00', 60, 'Oficina Director', 'Segunda entrevista familia', false, NULL, NULL, NULL, NULL, NOW(), NOW()),
(21, 35, 'CONFIRMED', 'INDIVIDUAL', 'IN_PERSON', '2025-08-30', '08:30:00', 30, 'Aula 201', 'Entrevista académica', false, NULL, NULL, NULL, NULL, NOW(), NOW()),
(22, 32, 'SCHEDULED', 'FAMILY', 'HYBRID', '2025-09-02', '14:30:00', 60, 'Sala Multiuso', 'Entrevista final proceso', false, NULL, NULL, NULL, NULL, NOW(), NOW()),
(23, 33, 'SCHEDULED', 'PSYCHOLOGICAL', 'IN_PERSON', '2025-09-03', '11:00:00', 45, 'Consultorio', 'Evaluación especializada', false, NULL, NULL, NULL, NULL, NOW(), NOW()),
(24, 34, 'SCHEDULED', 'FAMILY', 'VIRTUAL', '2025-09-04', '15:30:00', 60, 'Virtual - Zoom', 'Entrevista remota por ubicación', false, NULL, NULL, NULL, NULL, NOW(), NOW());

-- Actualizar entrevista completada con follow-up notes
UPDATE interviews SET 
    follow_up_notes = 'Programar refuerzo en matemáticas antes del inicio de clases'
WHERE application_id = 15 AND status = 'COMPLETED';
