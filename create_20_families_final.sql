-- Script final para crear 20 familias de prueba con todos los niveles
-- IMPORTANTE: No tocar la autenticación

-- Crear sostenedores con relaciones correctas
INSERT INTO supporters (full_name, rut, email, phone, relationship, created_at) VALUES
('Carlos González Hernández', '40003001-2', 'carlos.gonzalez@test.cl', '+56921000001', 'PADRE', NOW()),
('Pedro Rodríguez Morales', '40003002-0', 'pedro.rodriguez@test.cl', '+56921000002', 'PADRE', NOW()),
('Luis Morales Pérez', '40003003-9', 'luis.morales@test.cl', '+56921000003', 'PADRE', NOW()),
('Roberto Silva Vargas', '40003004-7', 'roberto.silva@test.cl', '+56921000004', 'PADRE', NOW()),
('Miguel Hernández Silva', '40003005-5', 'miguel.hernandez@test.cl', '+56921000005', 'PADRE', NOW()),
('Jorge Castro Torres', '40003006-3', 'jorge.castro@test.cl', '+56921000006', 'PADRE', NOW()),
('Andrés Vargas González', '40003007-1', 'andres.vargas@test.cl', '+56921000007', 'PADRE', NOW()),
('Felipe Muñoz Hernández', '40003008-K', 'felipe.munoz@test.cl', '+56921000008', 'PADRE', NOW()),
('Patricio Torres Castro', '40003009-8', 'patricio.torres@test.cl', '+56921000009', 'PADRE', NOW()),
('Rodrigo Pérez Vargas', '40003010-2', 'rodrigo.perez@test.cl', '+56921000010', 'PADRE', NOW()),
('Cristián López Muñoz', '40003011-0', 'cristian.lopez@test.cl', '+56921000011', 'PADRE', NOW()),
('Gonzalo García Torres', '40003012-9', 'gonzalo.garcia@test.cl', '+56921000012', 'PADRE', NOW()),
('Fernando Martínez González', '40003013-7', 'fernando.martinez@test.cl', '+56921000013', 'PADRE', NOW()),
('Mauricio Sánchez Hernández', '40003014-5', 'mauricio.sanchez@test.cl', '+56921000014', 'PADRE', NOW()),
('Osvaldo Ramos Castro', '40003015-3', 'osvaldo.ramos@test.cl', '+56921000015', 'PADRE', NOW()),
('Héctor Flores Vargas', '40003016-1', 'hector.flores@test.cl', '+56921000016', 'PADRE', NOW()),
('Ricardo Contreras Muñoz', '40003017-K', 'ricardo.contreras@test.cl', '+56921000017', 'PADRE', NOW()),
('Enrique Parra Torres', '40003018-8', 'enrique.parra@test.cl', '+56921000018', 'PADRE', NOW()),
('Ramón Aguilar González', '40003019-6', 'ramon.aguilar@test.cl', '+56921000019', 'PADRE', NOW()),
('Iván Fuentes Hernández', '40003020-0', 'ivan.fuentes@test.cl', '+56921000020', 'PADRE', NOW());

-- Crear apoderados con relaciones correctas
INSERT INTO guardians (full_name, rut, email, phone, relationship, created_at) VALUES
('María José González Silva', '40003001-3', 'familia01@test.cl', '+56911000001', 'MADRE', NOW()),
('Carmen Rodríguez López', '40003002-1', 'familia02@test.cl', '+56911000002', 'MADRE', NOW()),
('Patricia Morales Castro', '40003003-K', 'familia03@test.cl', '+56911000003', 'MADRE', NOW()),
('Andrea Silva Torres', '40003004-8', 'familia04@test.cl', '+56911000004', 'MADRE', NOW()),
('Claudia Hernández Pérez', '40003005-6', 'familia05@test.cl', '+56911000005', 'MADRE', NOW()),
('Valeria Castro Morales', '40003006-4', 'familia06@test.cl', '+56911000006', 'MADRE', NOW()),
('Mónica Vargas Silva', '40003007-2', 'familia07@test.cl', '+56911000007', 'MADRE', NOW()),
('Francisca Muñoz González', '40003008-0', 'familia08@test.cl', '+56911000008', 'MADRE', NOW()),
('Soledad Torres Hernández', '40003009-9', 'familia09@test.cl', '+56911000009', 'MADRE', NOW()),
('Alejandra Pérez Castro', '40003010-3', 'familia10@test.cl', '+56911000010', 'MADRE', NOW()),
('Lorena López Vargas', '40003011-1', 'familia11@test.cl', '+56911000011', 'MADRE', NOW()),
('Paola García Muñoz', '40003012-K', 'familia12@test.cl', '+56911000012', 'MADRE', NOW()),
('Verónica Martínez Torres', '40003013-8', 'familia13@test.cl', '+56911000013', 'MADRE', NOW()),
('Carolina Sánchez Pérez', '40003014-6', 'familia14@test.cl', '+56911000014', 'MADRE', NOW()),
('Daniela Ramos López', '40003015-4', 'familia15@test.cl', '+56911000015', 'MADRE', NOW()),
('Marcela Flores García', '40003016-2', 'familia16@test.cl', '+56911000016', 'MADRE', NOW()),
('Gladys Contreras Martínez', '40003017-0', 'familia17@test.cl', '+56911000017', 'MADRE', NOW()),
('Cecilia Parra Sánchez', '40003018-9', 'familia18@test.cl', '+56911000018', 'MADRE', NOW()),
('Roxana Aguilar Ramos', '40003019-7', 'familia19@test.cl', '+56911000019', 'MADRE', NOW()),
('Ingrid Fuentes Flores', '40003020-1', 'familia20@test.cl', '+56911000020', 'MADRE', NOW());

-- Crear las 20 postulaciones manualmente para asegurar la relación correcta
INSERT INTO applications (student_id, father_id, mother_id, supporter_id, guardian_id, applicant_user_id, status, submission_date, created_at)
SELECT 
    s.id,
    pf.id,
    pm.id,
    sup.id,
    g.id,
    u.id,
    CASE 
        WHEN s.id % 8 = 1 THEN 'PENDING'
        WHEN s.id % 8 = 2 THEN 'UNDER_REVIEW' 
        WHEN s.id % 8 = 3 THEN 'INTERVIEW_SCHEDULED'
        WHEN s.id % 8 = 4 THEN 'EXAM_SCHEDULED'
        WHEN s.id % 8 = 5 THEN 'APPROVED'
        WHEN s.id % 8 = 6 THEN 'REJECTED'
        WHEN s.id % 8 = 7 THEN 'WAITLIST'
        ELSE 'PENDING'
    END,
    NOW() - INTERVAL (ROW_NUMBER() OVER (ORDER BY s.id)) DAY,
    NOW()
FROM students s
JOIN parents pf ON pf.parent_type = 'FATHER' AND pf.rut = REPLACE(s.rut, '40002', '40003') || '-2'
JOIN parents pm ON pm.parent_type = 'MOTHER' AND pm.rut = REPLACE(s.rut, '40002', '40003') || '-3' 
JOIN supporters sup ON sup.rut = pf.rut
JOIN guardians g ON g.rut = pm.rut
JOIN users u ON u.email = pm.email
WHERE s.rut LIKE '40002%'
ORDER BY s.id;

-- Mostrar resumen final
SELECT 
    'RESUMEN FINAL DE DATOS' as descripcion,
    (SELECT COUNT(*) FROM users WHERE email LIKE '%@test.cl') as apoderados,
    (SELECT COUNT(*) FROM students WHERE rut LIKE '40002%') as estudiantes,
    (SELECT COUNT(*) FROM applications WHERE created_at > NOW() - INTERVAL '1 hour') as postulaciones;

-- Mostrar distribución por nivel educativo
SELECT 
    s.grade_applied as nivel_educativo,
    s.school_applied as colegio_destino,
    COUNT(*) as total_postulaciones,
    STRING_AGG(DISTINCT a.status, ', ' ORDER BY a.status) as estados_presentes
FROM applications a
JOIN students s ON a.student_id = s.id
WHERE a.created_at > NOW() - INTERVAL '1 hour'
GROUP BY s.grade_applied, s.school_applied
ORDER BY 
    CASE s.grade_applied
        WHEN 'Prekinder' THEN 1
        WHEN 'Kinder' THEN 2
        WHEN '1° Básico' THEN 3
        WHEN '2° Básico' THEN 4
        WHEN '3° Básico' THEN 5
        WHEN '4° Básico' THEN 6
        WHEN '5° Básico' THEN 7
        WHEN '6° Básico' THEN 8
        WHEN '7° Básico' THEN 9
        WHEN '8° Básico' THEN 10
        WHEN 'I° Medio' THEN 11
        WHEN 'II° Medio' THEN 12
        WHEN 'III° Medio' THEN 13
        WHEN 'IV° Medio' THEN 14
    END, s.school_applied;