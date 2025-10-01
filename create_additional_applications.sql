-- Crear aplicaciones adicionales para generar más entrevistas
-- Necesitamos más datos de prueba para el sistema completo

-- Crear más estudiantes
INSERT INTO students (
    rut, first_name, paternal_last_name, maternal_last_name, 
    birth_date, grade_applied, current_school, school_applied, address,
    created_at
) VALUES 
('12345678-9', 'Carlos', 'Ramírez', 'Torres', '2017-03-15', 'TERCERO_BASICO', 'Colegio San José', 'MONTE_TABOR', 'Av. Las Condes 1234, Las Condes', NOW()),
('98765432-1', 'Sofía', 'González', 'Morales', '2018-07-22', 'SEGUNDO_BASICO', 'Instituto Nacional', 'NAZARET', 'Calle Principal 567, Providencia', NOW()),
('11111111-2', 'Diego', 'Castillo', 'Ruiz', '2016-12-10', 'CUARTO_BASICO', 'Liceo de Aplicación', 'MONTE_TABOR', 'Paseo Bulnes 890, Santiago Centro', NOW()),
('22222222-3', 'Valentina', 'Herrera', 'Silva', '2017-09-05', 'TERCERO_BASICO', 'Colegio Particular', 'NAZARET', 'Av. Vitacura 321, Vitacura', NOW());

-- Crear padres para los nuevos estudiantes
INSERT INTO parents (
    rut, full_name, parent_type, email, phone, profession, 
    address, created_at
) VALUES 
-- Padres de Carlos Ramírez Torres
('30000000-1', 'Roberto Ramírez Mendoza', 'FATHER', 'roberto.ramirez@email.cl', '+56933333333', 'Ingeniero Civil', 'Av. Las Condes 1234, Las Condes', NOW()),
('30000000-2', 'Carmen Torres López', 'MOTHER', 'carmen.torres@email.cl', '+56944444444', 'Contadora', 'Av. Las Condes 1234, Las Condes', NOW()),

-- Padres de Sofía González Morales  
('30000001-1', 'Luis González Pérez', 'FATHER', 'luis.gonzalez@email.cl', '+56955555555', 'Médico', 'Calle Principal 567, Providencia', NOW()),
('30000001-2', 'Ana Morales Castro', 'MOTHER', 'ana.morales@email.cl', '+56966666666', 'Profesora', 'Calle Principal 567, Providencia', NOW()),

-- Padres de Diego Castillo Ruiz
('30000002-1', 'Pedro Castillo Jiménez', 'FATHER', 'pedro.castillo@email.cl', '+56977777777', 'Abogado', 'Paseo Bulnes 890, Santiago Centro', NOW()),
('30000002-2', 'María Ruiz Fernández', 'MOTHER', 'maria.ruiz@email.cl', '+56988888888', 'Psicóloga', 'Paseo Bulnes 890, Santiago Centro', NOW()),

-- Padres de Valentina Herrera Silva
('30000003-1', 'Jorge Herrera Morales', 'FATHER', 'jorge.herrera@email.cl', '+56999999999', 'Arquitecto', 'Av. Vitacura 321, Vitacura', NOW()),
('30000003-2', 'Patricia Silva Rojas', 'MOTHER', 'patricia.silva@email.cl', '+56911111111', 'Diseñadora', 'Av. Vitacura 321, Vitacura', NOW());

-- Crear usuarios aplicantes (familias)
INSERT INTO users (
    first_name, last_name, email, password, rut, phone, role,
    email_verified, active, created_at
) VALUES 
('Roberto', 'Ramírez', 'familia.ramirez@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '30000000-1', '+56933333333', 'APODERADO', true, true, NOW()),
('Luis', 'González', 'familia.gonzalez@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '30000001-1', '+56955555555', 'APODERADO', true, true, NOW()),  
('Pedro', 'Castillo', 'familia.castillo@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '30000002-1', '+56977777777', 'APODERADO', true, true, NOW()),
('Jorge', 'Herrera', 'familia.herrera@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '30000003-1', '+56999999999', 'APODERADO', true, true, NOW());

-- Crear supporters y guardians (usando los mismos padres)
INSERT INTO supporters (
    rut, full_name, relationship, email, phone, created_at
) VALUES 
('30000000-1', 'Roberto Ramírez Mendoza', 'FATHER', 'roberto.ramirez@email.cl', '+56933333333', NOW()),
('30000001-1', 'Luis González Pérez', 'FATHER', 'luis.gonzalez@email.cl', '+56955555555', NOW()),
('30000002-1', 'Pedro Castillo Jiménez', 'FATHER', 'pedro.castillo@email.cl', '+56977777777', NOW()),
('30000003-1', 'Jorge Herrera Morales', 'FATHER', 'jorge.herrera@email.cl', '+56999999999', NOW());

INSERT INTO guardians (
    rut, full_name, relationship, email, phone, created_at
) VALUES 
('30000000-2', 'Carmen Torres López', 'MOTHER', 'carmen.torres@email.cl', '+56944444444', NOW()),
('30000001-2', 'Ana Morales Castro', 'MOTHER', 'ana.morales@email.cl', '+56966666666', NOW()),
('30000002-2', 'María Ruiz Fernández', 'MOTHER', 'maria.ruiz@email.cl', '+56988888888', NOW()),
('30000003-2', 'Patricia Silva Rojas', 'MOTHER', 'patricia.silva@email.cl', '+56911111111', NOW());

-- Crear aplicaciones para los nuevos estudiantes
INSERT INTO applications (
    student_id, father_id, mother_id, supporter_id, guardian_id, applicant_user_id,
    status, submission_date, created_at
) VALUES 
-- Aplicación para Carlos Ramírez Torres
(
    (SELECT id FROM students WHERE rut = '12345678-9'),
    (SELECT id FROM parents WHERE rut = '30000000-1'), 
    (SELECT id FROM parents WHERE rut = '30000000-2'),
    (SELECT id FROM supporters WHERE rut = '30000000-1'),
    (SELECT id FROM guardians WHERE rut = '30000000-2'),
    (SELECT id FROM users WHERE email = 'familia.ramirez@test.cl'),
    'INTERVIEW_SCHEDULED', NOW() - INTERVAL '5' DAY, NOW()
),

-- Aplicación para Sofía González Morales
(
    (SELECT id FROM students WHERE rut = '98765432-1'),
    (SELECT id FROM parents WHERE rut = '30000001-1'),
    (SELECT id FROM parents WHERE rut = '30000001-2'), 
    (SELECT id FROM supporters WHERE rut = '30000001-1'),
    (SELECT id FROM guardians WHERE rut = '30000001-2'),
    (SELECT id FROM users WHERE email = 'familia.gonzalez@test.cl'),
    'INTERVIEW_SCHEDULED', NOW() - INTERVAL '3' DAY, NOW()
),

-- Aplicación para Diego Castillo Ruiz
(
    (SELECT id FROM students WHERE rut = '11111111-2'),
    (SELECT id FROM parents WHERE rut = '30000002-1'),
    (SELECT id FROM parents WHERE rut = '30000002-2'),
    (SELECT id FROM supporters WHERE rut = '30000002-1'), 
    (SELECT id FROM guardians WHERE rut = '30000002-2'),
    (SELECT id FROM users WHERE email = 'familia.castillo@test.cl'),
    'INTERVIEW_SCHEDULED', NOW() - INTERVAL '7' DAY, NOW()
),

-- Aplicación para Valentina Herrera Silva
(
    (SELECT id FROM students WHERE rut = '22222222-3'),
    (SELECT id FROM parents WHERE rut = '30000003-1'),
    (SELECT id FROM parents WHERE rut = '30000003-2'),
    (SELECT id FROM supporters WHERE rut = '30000003-1'),
    (SELECT id FROM guardians WHERE rut = '30000003-2'), 
    (SELECT id FROM users WHERE email = 'familia.herrera@test.cl'),
    'INTERVIEW_SCHEDULED', NOW() - INTERVAL '1' DAY, NOW()
);

-- Mostrar las aplicaciones creadas
SELECT 
    a.id as application_id,
    s.first_name || ' ' || s.paternal_last_name || ' ' || s.maternal_last_name as estudiante,
    s.grade_applied,
    a.status,
    a.submission_date
FROM applications a
JOIN students s ON a.student_id = s.id
ORDER BY a.id;