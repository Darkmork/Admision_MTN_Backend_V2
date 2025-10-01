-- Crear 10 postulaciones de prueba para diferentes niveles educativos
-- Primero necesitamos crear algunos apoderados de prueba

-- Insertar apoderados de prueba
INSERT INTO users (first_name, last_name, email, password, rut, phone, role, email_verified, active, created_at) VALUES
('Ana María', 'González López', 'ana.gonzalez@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '15123456-7', '+56987654321', 'APODERADO', true, true, NOW()),
('Carlos Eduardo', 'Pérez Silva', 'carlos.perez@hotmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '16234567-8', '+56912345678', 'APODERADO', true, true, NOW()),
('María Isabel', 'Rodríguez Torres', 'maria.rodriguez@yahoo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '17345678-9', '+56923456789', 'APODERADO', true, true, NOW()),
('José Miguel', 'Hernández Castro', 'jose.hernandez@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '18456789-0', '+56934567890', 'APODERADO', true, true, NOW()),
('Patricia Elena', 'Morales Vega', 'patricia.morales@outlook.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '19567890-1', '+56945678901', 'APODERADO', true, true, NOW()),
('Roberto Carlos', 'Jiménez Flores', 'roberto.jimenez@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '20678901-2', '+56956789012', 'APODERADO', true, true, NOW()),
('Francisca Andrea', 'Vargas Mendoza', 'francisca.vargas@yahoo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '21789012-3', '+56967890123', 'APODERADO', true, true, NOW()),
('Luis Fernando', 'Sánchez Rojas', 'luis.sanchez@hotmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '22890123-4', '+56978901234', 'APODERADO', true, true, NOW()),
('Carmen Gloria', 'Espinoza Núñez', 'carmen.espinoza@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '23901234-5', '+56989012345', 'APODERADO', true, true, NOW()),
('Alejandro José', 'Contreras Pinto', 'alejandro.contreras@outlook.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '24012345-6', '+56990123456', 'APODERADO', true, true, NOW());

-- Insertar estudiantes para las postulaciones
INSERT INTO students (first_name, last_name, rut, birth_date, grade_applied, previous_school, has_special_needs, special_needs_description, medical_conditions, current_medications, created_at) VALUES
('Sofía Isabella', 'González Pérez', '25123456-7', '2019-03-15', 'Prekinder', NULL, false, NULL, NULL, NULL, NOW()),
('Mateo Alejandro', 'Pérez Morales', '26234567-8', '2018-07-22', 'Kinder', 'Jardín Los Angelitos', false, NULL, NULL, NULL, NOW()),
('Valentina María', 'Rodríguez Silva', '27345678-9', '2017-11-08', '1° Básico', 'Colegio San José', false, NULL, NULL, NULL, NOW()),
('Diego Sebastián', 'Hernández López', '28456789-0', '2016-04-12', '2° Básico', 'Escuela Municipal Las Flores', true, 'Déficit atencional', 'TDAH', 'Metilfenidato', NOW()),
('Antonella Fernanda', 'Morales González', '29567890-1', '2015-09-25', '3° Básico', 'Colegio Particular San Pablo', false, NULL, NULL, NULL, NOW()),
('Benjamín Ignacio', 'Jiménez Torres', '30678901-2', '2014-12-03', '4° Básico', 'Colegio Los Pinos', false, NULL, NULL, NULL, NOW()),
('Isidora Paz', 'Vargas Hernández', '31789012-3', '2013-06-18', '5° Básico', 'Escuela Básica Santa María', true, 'Dislexia leve', NULL, NULL, NOW()),
('Cristóbal Andrés', 'Sánchez Vargas', '32890123-4', '2012-01-27', '6° Básico', 'Colegio Monte Verde', false, NULL, NULL, NULL, NOW()),
('Magdalena Esperanza', 'Espinoza Jiménez', '33901234-5', '2011-08-14', '7° Básico', 'Colegio Sagrado Corazón', false, NULL, NULL, NULL, NOW()),
('Gabriel Eduardo', 'Contreras Espinoza', '34012345-6', '2010-10-30', '8° Básico', 'Liceo Municipal San Carlos', true, 'Discapacidad auditiva leve', 'Hipoacusia', NULL, NOW());

-- Insertar las 10 postulaciones con diferentes estados
INSERT INTO applications (
    student_id, 
    parent_id, 
    status, 
    academic_year, 
    submission_date, 
    documents_complete, 
    created_at, 
    updated_at
) VALUES
-- Postulación 1: Prekinder - SUBMITTED
(
    (SELECT id FROM students WHERE rut = '25123456-7'),
    (SELECT id FROM users WHERE email = 'ana.gonzalez@gmail.com'),
    'SUBMITTED',
    2025,
    '2025-01-15 10:30:00',
    true,
    '2025-01-15 10:30:00',
    '2025-01-15 10:30:00'
),
-- Postulación 2: Kinder - UNDER_REVIEW
(
    (SELECT id FROM students WHERE rut = '26234567-8'),
    (SELECT id FROM users WHERE email = 'carlos.perez@hotmail.com'),
    'UNDER_REVIEW',
    2025,
    '2025-01-12 14:20:00',
    true,
    '2025-01-12 14:20:00',
    '2025-01-18 09:15:00'
),
-- Postulación 3: 1° Básico - INTERVIEW_SCHEDULED
(
    (SELECT id FROM students WHERE rut = '27345678-9'),
    (SELECT id FROM users WHERE email = 'maria.rodriguez@yahoo.com'),
    'INTERVIEW_SCHEDULED',
    2025,
    '2025-01-10 09:15:00',
    true,
    '2025-01-10 09:15:00',
    '2025-01-20 11:30:00'
),
-- Postulación 4: 2° Básico - EVALUATION_COMPLETE
(
    (SELECT id FROM students WHERE rut = '28456789-0'),
    (SELECT id FROM users WHERE email = 'jose.hernandez@gmail.com'),
    'EVALUATION_COMPLETE',
    2025,
    '2025-01-08 16:45:00',
    true,
    '2025-01-08 16:45:00',
    '2025-01-22 14:20:00'
),
-- Postulación 5: 3° Básico - ACCEPTED
(
    (SELECT id FROM students WHERE rut = '29567890-1'),
    (SELECT id FROM users WHERE email = 'patricia.morales@outlook.com'),
    'ACCEPTED',
    2025,
    '2025-01-05 11:00:00',
    true,
    '2025-01-05 11:00:00',
    '2025-01-25 10:00:00'
),
-- Postulación 6: 4° Básico - SUBMITTED
(
    (SELECT id FROM students WHERE rut = '30678901-2'),
    (SELECT id FROM users WHERE email = 'roberto.jimenez@gmail.com'),
    'SUBMITTED',
    2025,
    '2025-01-20 08:30:00',
    false,
    '2025-01-20 08:30:00',
    '2025-01-20 08:30:00'
),
-- Postulación 7: 5° Básico - UNDER_REVIEW
(
    (SELECT id FROM students WHERE rut = '31789012-3'),
    (SELECT id FROM users WHERE email = 'francisca.vargas@yahoo.com'),
    'UNDER_REVIEW',
    2025,
    '2025-01-18 13:15:00',
    true,
    '2025-01-18 13:15:00',
    '2025-01-22 16:00:00'
),
-- Postulación 8: 6° Básico - WAITLIST
(
    (SELECT id FROM students WHERE rut = '32890123-4'),
    (SELECT id FROM users WHERE email = 'luis.sanchez@hotmail.com'),
    'WAITLIST',
    2025,
    '2025-01-14 15:20:00',
    true,
    '2025-01-14 15:20:00',
    '2025-01-28 12:45:00'
),
-- Postulación 9: 7° Básico - REJECTED
(
    (SELECT id FROM students WHERE rut = '33901234-5'),
    (SELECT id FROM users WHERE email = 'carmen.espinoza@gmail.com'),
    'REJECTED',
    2025,
    '2025-01-06 12:00:00',
    true,
    '2025-01-06 12:00:00',
    '2025-01-30 09:30:00'
),
-- Postulación 10: 8° Básico - INTERVIEW_SCHEDULED
(
    (SELECT id FROM students WHERE rut = '34012345-6'),
    (SELECT id FROM users WHERE email = 'alejandro.contreras@outlook.com'),
    'INTERVIEW_SCHEDULED',
    2025,
    '2025-01-22 10:45:00',
    true,
    '2025-01-22 10:45:00',
    '2025-02-01 14:15:00'
);

-- Verificar las postulaciones creadas
SELECT 
    a.id as application_id,
    CONCAT(s.first_name, ' ', s.last_name) as student_name,
    s.rut as student_rut,
    s.grade_applied,
    a.status,
    CONCAT(u.first_name, ' ', u.last_name) as parent_name,
    u.email as parent_email,
    a.submission_date,
    a.documents_complete
FROM applications a
JOIN students s ON a.student_id = s.id
JOIN users u ON a.parent_id = u.id
ORDER BY a.submission_date DESC;