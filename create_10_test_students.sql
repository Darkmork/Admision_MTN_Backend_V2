-- Crear 10 estudiantes de prueba con toda la información necesaria
-- Incluye: estudiantes, padres, usuarios aplicantes, supporters, guardians y aplicaciones

-- ========================================
-- 1. CREAR ESTUDIANTES
-- ========================================

INSERT INTO students (
    rut, first_name, paternal_last_name, maternal_last_name, 
    birth_date, grade_applied, current_school, school_applied, address, created_at
) VALUES 

-- Estudiante 1: Matías Fernández
('13000001-5', 'Matías', 'Fernández', 'López', '2017-05-12', 'TERCERO_BASICO', 'Escuela República', 'MONTE_TABOR', 'Los Leones 456, Providencia', NOW()),

-- Estudiante 2: Isabella Rodríguez
('13000002-3', 'Isabella', 'Rodríguez', 'Castro', '2018-11-08', 'SEGUNDO_BASICO', 'Colegio San Patricio', 'NAZARET', 'Santa María 789, Las Condes', NOW()),

-- Estudiante 3: Benjamin Vargas
('13000003-1', 'Benjamin', 'Vargas', 'Moreno', '2016-08-25', 'CUARTO_BASICO', 'Instituto O''Higgins', 'MONTE_TABOR', 'Alameda 1234, Santiago Centro', NOW()),

-- Estudiante 4: Emilia Torres
('13000004-K', 'Emilia', 'Torres', 'Jiménez', '2017-12-03', 'TERCERO_BASICO', 'Colegio Montessori', 'NAZARET', 'Apoquindo 567, Las Condes', NOW()),

-- Estudiante 5: Joaquín Muñoz
('13000005-8', 'Joaquín', 'Muñoz', 'Sánchez', '2018-02-14', 'SEGUNDO_BASICO', 'Escuela Básica Nº1', 'MONTE_TABOR', 'Gran Avenida 890, San Miguel', NOW()),

-- Estudiante 6: Antonella Silva
('13000006-6', 'Antonella', 'Silva', 'Herrera', '2017-09-30', 'TERCERO_BASICO', 'Colegio Particular Subvencionado', 'NAZARET', 'Irarrázaval 345, Ñuñoa', NOW()),

-- Estudiante 7: Lucas Peña
('13000007-4', 'Lucas', 'Peña', 'Vega', '2016-01-18', 'CUARTO_BASICO', 'Liceo Experimental', 'MONTE_TABOR', 'Vicuña Mackenna 678, Macul', NOW()),

-- Estudiante 8: Catalina Reyes
('13000008-2', 'Catalina', 'Reyes', 'Mendoza', '2018-07-22', 'SEGUNDO_BASICO', 'Colegio Inglés', 'NAZARET', 'Kennedy 912, Vitacura', NOW()),

-- Estudiante 9: Agustín Morales  
('13000009-0', 'Agustín', 'Morales', 'Pinto', '2017-04-07', 'TERCERO_BASICO', 'Escuela Pública', 'MONTE_TABOR', 'Maipú 234, Maipú', NOW()),

-- Estudiante 10: Florencia Díaz
('13000010-4', 'Florencia', 'Díaz', 'Ruiz', '2018-10-15', 'SEGUNDO_BASICO', 'Instituto Pedagógico', 'NAZARET', 'Tobalaba 567, La Reina', NOW());

-- ========================================
-- 2. CREAR PADRES PARA CADA ESTUDIANTE  
-- ========================================

INSERT INTO parents (
    rut, full_name, parent_type, email, phone, profession, address, created_at
) VALUES 

-- Padres Estudiante 1: Matías Fernández
('14000001-1', 'Carlos Fernández Morales', 'FATHER', 'carlos.fernandez@email.cl', '+56912345001', 'Contador Auditor', 'Los Leones 456, Providencia', NOW()),
('14000001-2', 'Patricia López Soto', 'MOTHER', 'patricia.lopez@email.cl', '+56912345002', 'Enfermera', 'Los Leones 456, Providencia', NOW()),

-- Padres Estudiante 2: Isabella Rodríguez  
('14000002-1', 'Miguel Rodríguez Vargas', 'FATHER', 'miguel.rodriguez@email.cl', '+56912345003', 'Ingeniero Industrial', 'Santa María 789, Las Condes', NOW()),
('14000002-2', 'Carmen Castro Fuentes', 'MOTHER', 'carmen.castro@email.cl', '+56912345004', 'Diseñadora Gráfica', 'Santa María 789, Las Condes', NOW()),

-- Padres Estudiante 3: Benjamin Vargas
('14000003-1', 'Rodrigo Vargas Silva', 'FATHER', 'rodrigo.vargas@email.cl', '+56912345005', 'Abogado Corporativo', 'Alameda 1234, Santiago Centro', NOW()),
('14000003-2', 'Andrea Moreno Peña', 'MOTHER', 'andrea.moreno@email.cl', '+56912345006', 'Psicóloga Clínica', 'Alameda 1234, Santiago Centro', NOW()),

-- Padres Estudiante 4: Emilia Torres
('14000004-1', 'Fernando Torres Ramírez', 'FATHER', 'fernando.torres@email.cl', '+56912345007', 'Médico Pediatra', 'Apoquindo 567, Las Condes', NOW()),
('14000004-2', 'Lorena Jiménez Castro', 'MOTHER', 'lorena.jimenez@email.cl', '+56912345008', 'Profesora Básica', 'Apoquindo 567, Las Condes', NOW()),

-- Padres Estudiante 5: Joaquín Muñoz
('14000005-1', 'Álvaro Muñoz Torres', 'FATHER', 'alvaro.munoz@email.cl', '+56912345009', 'Técnico Electrónico', 'Gran Avenida 890, San Miguel', NOW()),
('14000005-2', 'Rosa Sánchez López', 'MOTHER', 'rosa.sanchez@email.cl', '+56912345010', 'Auxiliar de Párvulos', 'Gran Avenida 890, San Miguel', NOW()),

-- Padres Estudiante 6: Antonella Silva
('14000006-1', 'Patricio Silva Herrera', 'FATHER', 'patricio.silva@email.cl', '+56912345011', 'Comerciante', 'Irarrázaval 345, Ñuñoa', NOW()),
('14000006-2', 'Mónica Herrera Vega', 'MOTHER', 'monica.herrera@email.cl', '+56912345012', 'Secretaria Ejecutiva', 'Irarrázaval 345, Ñuñoa', NOW()),

-- Padres Estudiante 7: Lucas Peña
('14000007-1', 'Sergio Peña Morales', 'FATHER', 'sergio.pena@email.cl', '+56912345013', 'Ingeniero Civil', 'Vicuña Mackenna 678, Macul', NOW()),
('14000007-2', 'Claudia Vega Silva', 'MOTHER', 'claudia.vega@email.cl', '+56912345014', 'Kinesióloga', 'Vicuña Mackenna 678, Macul', NOW()),

-- Padres Estudiante 8: Catalina Reyes
('14000008-1', 'Eduardo Reyes Santander', 'FATHER', 'eduardo.reyes@email.cl', '+56912345015', 'Arquitecto', 'Kennedy 912, Vitacura', NOW()),
('14000008-2', 'Alejandra Mendoza Cruz', 'MOTHER', 'alejandra.mendoza@email.cl', '+56912345016', 'Nutricionista', 'Kennedy 912, Vitacura', NOW()),

-- Padres Estudiante 9: Agustín Morales
('14000009-1', 'Ricardo Morales Fuentes', 'FATHER', 'ricardo.morales@email.cl', '+56912345017', 'Chofer Profesional', 'Maipú 234, Maipú', NOW()),
('14000009-2', 'Elena Pinto Rojas', 'MOTHER', 'elena.pinto@email.cl', '+56912345018', 'Cosmetóloga', 'Maipú 234, Maipú', NOW()),

-- Padres Estudiante 10: Florencia Díaz
('14000010-1', 'Gonzalo Díaz Herrera', 'FATHER', 'gonzalo.diaz@email.cl', '+56912345019', 'Vendedor', 'Tobalaba 567, La Reina', NOW()),
('14000010-2', 'Paola Ruiz Castro', 'MOTHER', 'paola.ruiz@email.cl', '+56912345020', 'Administrativo', 'Tobalaba 567, La Reina', NOW());

-- ========================================
-- 3. CREAR USUARIOS APLICANTES (FAMILIAS)
-- ========================================

INSERT INTO users (
    first_name, last_name, email, password, rut, phone, role,
    email_verified, active, created_at
) VALUES 

('Carlos', 'Fernández', 'familia.fernandez@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '14000001-1', '+56912345001', 'APODERADO', true, true, NOW()),
('Miguel', 'Rodríguez', 'familia.rodriguez@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '14000002-1', '+56912345003', 'APODERADO', true, true, NOW()),
('Rodrigo', 'Vargas', 'familia.vargas@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '14000003-1', '+56912345005', 'APODERADO', true, true, NOW()),
('Fernando', 'Torres', 'familia.torres@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '14000004-1', '+56912345007', 'APODERADO', true, true, NOW()),
('Álvaro', 'Muñoz', 'familia.munoz@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '14000005-1', '+56912345009', 'APODERADO', true, true, NOW()),
('Patricio', 'Silva', 'familia.silvaherrera@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '14000006-1', '+56912345011', 'APODERADO', true, true, NOW()),
('Sergio', 'Peña', 'familia.pena@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '14000007-1', '+56912345013', 'APODERADO', true, true, NOW()),
('Eduardo', 'Reyes', 'familia.reyes@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '14000008-1', '+56912345015', 'APODERADO', true, true, NOW()),
('Ricardo', 'Morales', 'familia.moralespinto@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '14000009-1', '+56912345017', 'APODERADO', true, true, NOW()),
('Gonzalo', 'Díaz', 'familia.diaz@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '14000010-1', '+56912345019', 'APODERADO', true, true, NOW());

-- ========================================
-- 4. CREAR SUPPORTERS (PADRES COMO APODERADOS)
-- ========================================

INSERT INTO supporters (
    rut, full_name, relationship, email, phone, created_at
) VALUES 

('14000001-1', 'Carlos Fernández Morales', 'PADRE', 'carlos.fernandez@email.cl', '+56912345001', NOW()),
('14000002-1', 'Miguel Rodríguez Vargas', 'PADRE', 'miguel.rodriguez@email.cl', '+56912345003', NOW()),
('14000003-1', 'Rodrigo Vargas Silva', 'PADRE', 'rodrigo.vargas@email.cl', '+56912345005', NOW()),
('14000004-1', 'Fernando Torres Ramírez', 'PADRE', 'fernando.torres@email.cl', '+56912345007', NOW()),
('14000005-1', 'Álvaro Muñoz Torres', 'PADRE', 'alvaro.munoz@email.cl', '+56912345009', NOW()),
('14000006-1', 'Patricio Silva Herrera', 'PADRE', 'patricio.silva@email.cl', '+56912345011', NOW()),
('14000007-1', 'Sergio Peña Morales', 'PADRE', 'sergio.pena@email.cl', '+56912345013', NOW()),
('14000008-1', 'Eduardo Reyes Santander', 'PADRE', 'eduardo.reyes@email.cl', '+56912345015', NOW()),
('14000009-1', 'Ricardo Morales Fuentes', 'PADRE', 'ricardo.morales@email.cl', '+56912345017', NOW()),
('14000010-1', 'Gonzalo Díaz Herrera', 'PADRE', 'gonzalo.diaz@email.cl', '+56912345019', NOW());

-- ========================================
-- 5. CREAR GUARDIANS (MADRES COMO CONTACTO PRINCIPAL)
-- ========================================

INSERT INTO guardians (
    rut, full_name, relationship, email, phone, created_at
) VALUES 

('14000001-2', 'Patricia López Soto', 'MADRE', 'patricia.lopez@email.cl', '+56912345002', NOW()),
('14000002-2', 'Carmen Castro Fuentes', 'MADRE', 'carmen.castro@email.cl', '+56912345004', NOW()),
('14000003-2', 'Andrea Moreno Peña', 'MADRE', 'andrea.moreno@email.cl', '+56912345006', NOW()),
('14000004-2', 'Lorena Jiménez Castro', 'MADRE', 'lorena.jimenez@email.cl', '+56912345008', NOW()),
('14000005-2', 'Rosa Sánchez López', 'MADRE', 'rosa.sanchez@email.cl', '+56912345010', NOW()),
('14000006-2', 'Mónica Herrera Vega', 'MADRE', 'monica.herrera@email.cl', '+56912345012', NOW()),
('14000007-2', 'Claudia Vega Silva', 'MADRE', 'claudia.vega@email.cl', '+56912345014', NOW()),
('14000008-2', 'Alejandra Mendoza Cruz', 'MADRE', 'alejandra.mendoza@email.cl', '+56912345016', NOW()),
('14000009-2', 'Elena Pinto Rojas', 'MADRE', 'elena.pinto@email.cl', '+56912345018', NOW()),
('14000010-2', 'Paola Ruiz Castro', 'MADRE', 'paola.ruiz@email.cl', '+56912345020', NOW());

-- ========================================
-- 6. CREAR APLICACIONES
-- ========================================

INSERT INTO applications (
    student_id, father_id, mother_id, supporter_id, guardian_id, applicant_user_id,
    status, submission_date, created_at
) VALUES 

-- Aplicación 1: Matías Fernández
(
    (SELECT id FROM students WHERE rut = '13000001-5'),
    (SELECT id FROM parents WHERE rut = '14000001-1'),
    (SELECT id FROM parents WHERE rut = '14000001-2'),
    (SELECT id FROM supporters WHERE rut = '14000001-1'),
    (SELECT id FROM guardians WHERE rut = '14000001-2'),
    (SELECT id FROM users WHERE email = 'familia.fernandez@test.cl'),
    'PENDING', NOW() - INTERVAL '2' DAY, NOW()
),

-- Aplicación 2: Isabella Rodríguez
(
    (SELECT id FROM students WHERE rut = '13000002-3'),
    (SELECT id FROM parents WHERE rut = '14000002-1'),
    (SELECT id FROM parents WHERE rut = '14000002-2'),
    (SELECT id FROM supporters WHERE rut = '14000002-1'),
    (SELECT id FROM guardians WHERE rut = '14000002-2'),
    (SELECT id FROM users WHERE email = 'familia.rodriguez@test.cl'),
    'UNDER_REVIEW', NOW() - INTERVAL '5' DAY, NOW()
),

-- Aplicación 3: Benjamin Vargas
(
    (SELECT id FROM students WHERE rut = '13000003-1'),
    (SELECT id FROM parents WHERE rut = '14000003-1'),
    (SELECT id FROM parents WHERE rut = '14000003-2'),
    (SELECT id FROM supporters WHERE rut = '14000003-1'),
    (SELECT id FROM guardians WHERE rut = '14000003-2'),
    (SELECT id FROM users WHERE email = 'familia.vargas@test.cl'),
    'INTERVIEW_SCHEDULED', NOW() - INTERVAL '8' DAY, NOW()
),

-- Aplicación 4: Emilia Torres
(
    (SELECT id FROM students WHERE rut = '13000004-K'),
    (SELECT id FROM parents WHERE rut = '14000004-1'),
    (SELECT id FROM parents WHERE rut = '14000004-2'),
    (SELECT id FROM supporters WHERE rut = '14000004-1'),
    (SELECT id FROM guardians WHERE rut = '14000004-2'),
    (SELECT id FROM users WHERE email = 'familia.torres@test.cl'),
    'EXAM_SCHEDULED', NOW() - INTERVAL '10' DAY, NOW()
),

-- Aplicación 5: Joaquín Muñoz
(
    (SELECT id FROM students WHERE rut = '13000005-8'),
    (SELECT id FROM parents WHERE rut = '14000005-1'),
    (SELECT id FROM parents WHERE rut = '14000005-2'),
    (SELECT id FROM supporters WHERE rut = '14000005-1'),
    (SELECT id FROM guardians WHERE rut = '14000005-2'),
    (SELECT id FROM users WHERE email = 'familia.munoz@test.cl'),
    'PENDING', NOW() - INTERVAL '1' DAY, NOW()
),

-- Aplicación 6: Antonella Silva
(
    (SELECT id FROM students WHERE rut = '13000006-6'),
    (SELECT id FROM parents WHERE rut = '14000006-1'),
    (SELECT id FROM parents WHERE rut = '14000006-2'),
    (SELECT id FROM supporters WHERE rut = '14000006-1'),
    (SELECT id FROM guardians WHERE rut = '14000006-2'),
    (SELECT id FROM users WHERE email = 'familia.silvaherrera@test.cl'),
    'APPROVED', NOW() - INTERVAL '15' DAY, NOW()
),

-- Aplicación 7: Lucas Peña
(
    (SELECT id FROM students WHERE rut = '13000007-4'),
    (SELECT id FROM parents WHERE rut = '14000007-1'),
    (SELECT id FROM parents WHERE rut = '14000007-2'),
    (SELECT id FROM supporters WHERE rut = '14000007-1'),
    (SELECT id FROM guardians WHERE rut = '14000007-2'),
    (SELECT id FROM users WHERE email = 'familia.pena@test.cl'),
    'UNDER_REVIEW', NOW() - INTERVAL '6' DAY, NOW()
),

-- Aplicación 8: Catalina Reyes
(
    (SELECT id FROM students WHERE rut = '13000008-2'),
    (SELECT id FROM parents WHERE rut = '14000008-1'),
    (SELECT id FROM parents WHERE rut = '14000008-2'),
    (SELECT id FROM supporters WHERE rut = '14000008-1'),
    (SELECT id FROM guardians WHERE rut = '14000008-2'),
    (SELECT id FROM users WHERE email = 'familia.reyes@test.cl'),
    'REJECTED', NOW() - INTERVAL '20' DAY, NOW()
),

-- Aplicación 9: Agustín Morales
(
    (SELECT id FROM students WHERE rut = '13000009-0'),
    (SELECT id FROM parents WHERE rut = '14000009-1'),
    (SELECT id FROM parents WHERE rut = '14000009-2'),
    (SELECT id FROM supporters WHERE rut = '14000009-1'),
    (SELECT id FROM guardians WHERE rut = '14000009-2'),
    (SELECT id FROM users WHERE email = 'familia.moralespinto@test.cl'),
    'WAITLIST', NOW() - INTERVAL '12' DAY, NOW()
),

-- Aplicación 10: Florencia Díaz
(
    (SELECT id FROM students WHERE rut = '13000010-4'),
    (SELECT id FROM parents WHERE rut = '14000010-1'),
    (SELECT id FROM parents WHERE rut = '14000010-2'),
    (SELECT id FROM supporters WHERE rut = '14000010-1'),
    (SELECT id FROM guardians WHERE rut = '14000010-2'),
    (SELECT id FROM users WHERE email = 'familia.diaz@test.cl'),
    'INTERVIEW_SCHEDULED', NOW() - INTERVAL '4' DAY, NOW()
);

-- ========================================
-- 7. VERIFICAR DATOS CREADOS
-- ========================================

SELECT 
    '=== RESUMEN DE ESTUDIANTES DE PRUEBA CREADOS ===' as titulo;

SELECT 
    s.first_name || ' ' || s.paternal_last_name || ' ' || s.maternal_last_name as nombre_completo,
    s.grade_applied as grado,
    s.school_applied as colegio,
    a.status as estado_aplicacion,
    f.full_name as padre,
    m.full_name as madre,
    u.email as email_familia
FROM students s
JOIN applications a ON s.id = a.student_id
JOIN parents f ON a.father_id = f.id
JOIN parents m ON a.mother_id = m.id  
JOIN users u ON a.applicant_user_id = u.id
WHERE s.rut LIKE '13000%'
ORDER BY s.id;

-- Estadísticas finales
SELECT 
    'Total estudiantes nuevos:' as metrica,
    COUNT(*)::text as valor
FROM students 
WHERE rut LIKE '13000%'
UNION ALL
SELECT 
    'Total aplicaciones nuevas:' as metrica,
    COUNT(*)::text as valor  
FROM applications a
JOIN students s ON a.student_id = s.id
WHERE s.rut LIKE '13000%'
UNION ALL
SELECT 
    'Total padres nuevos:' as metrica,
    COUNT(*)::text as valor
FROM parents
WHERE rut LIKE '14000%'
UNION ALL
SELECT 
    'Total usuarios familia nuevos:' as metrica,
    COUNT(*)::text as valor
FROM users
WHERE email LIKE '%@test.cl' AND created_at > NOW() - INTERVAL '1 minute';