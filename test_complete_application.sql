-- Script para crear una postulación completa de prueba
-- Verifica la funcionalidad de nombres separados y transformación a mayúsculas

-- 1. Crear el estudiante con apellidos separados
INSERT INTO students (
    first_name, 
    paternal_last_name, 
    maternal_last_name, 
    rut, 
    birth_date, 
    email, 
    address, 
    grade_applied, 
    school_applied, 
    current_school, 
    additional_notes,
    created_at, 
    updated_at
) VALUES (
    'JUAN CARLOS',                    -- first_name en mayúsculas
    'GONZÁLEZ',                       -- paternal_last_name (apellido paterno)
    'PÉREZ',                         -- maternal_last_name (apellido materno)
    '12345678-9',                    -- rut
    '2015-03-15',                    -- birth_date
    'juan.gonzalez@mail.com',        -- email
    'AV. PROVIDENCIA 1234, SANTIAGO', -- address en mayúsculas
    '3° BÁSICO',                     -- grade_applied
    'MONTE_TABOR',                   -- school_applied (opción válida)
    'COLEGIO SAN IGNACIO',           -- current_school en mayúsculas
    'ESTUDIANTE DESTACADO EN MATEMÁTICAS', -- additional_notes en mayúsculas
    NOW(),
    NOW()
) RETURNING id;

-- 2. Crear padre
INSERT INTO parents (
    full_name,
    rut,
    email,
    phone,
    address,
    profession,
    parent_type,
    created_at,
    updated_at
) VALUES (
    'CARLOS GONZÁLEZ MARTÍNEZ',      -- full_name en mayúsculas
    '87654321-0',                    -- rut
    'carlos.gonzalez@mail.com',      -- email
    '+56987654321',                  -- phone
    'AV. PROVIDENCIA 1234, SANTIAGO', -- address en mayúsculas
    'INGENIERO CIVIL',               -- profession en mayúsculas
    'FATHER',                        -- parent_type
    NOW(),
    NOW()
) RETURNING id;

-- 3. Crear madre
INSERT INTO parents (
    full_name,
    rut,
    email,
    phone,
    address,
    profession,
    parent_type,
    created_at,
    updated_at
) VALUES (
    'MARÍA PÉREZ SÁNCHEZ',           -- full_name en mayúsculas
    '76543210-1',                    -- rut
    'maria.perez@mail.com',          -- email
    '+56976543210',                  -- phone
    'AV. PROVIDENCIA 1234, SANTIAGO', -- address en mayúsculas
    'PROFESORA DE EDUCACIÓN BÁSICA', -- profession en mayúsculas
    'MOTHER',                        -- parent_type
    NOW(),
    NOW()
) RETURNING id;

-- 4. Crear sostenedor
INSERT INTO supporters (
    full_name,
    rut,
    email,
    phone,
    relationship,
    created_at,
    updated_at
) VALUES (
    'CARLOS GONZÁLEZ MARTÍNEZ',      -- full_name en mayúsculas
    '87654321-0',                    -- rut (mismo del padre)
    'carlos.gonzalez@mail.com',      -- email
    '+56987654321',                  -- phone
    'PADRE',                         -- relationship en mayúsculas
    NOW(),
    NOW()
) RETURNING id;

-- 5. Crear apoderado
INSERT INTO guardians (
    full_name,
    rut,
    email,
    phone,
    relationship,
    created_at,
    updated_at
) VALUES (
    'MARÍA PÉREZ SÁNCHEZ',           -- full_name en mayúsculas
    '76543210-1',                    -- rut (mismo de la madre)
    'maria.perez@mail.com',          -- email
    '+56976543210',                  -- phone
    'MADRE',                         -- relationship en mayúsculas
    NOW(),
    NOW()
) RETURNING id;

-- 6. Obtener usuario apoderado existente para la aplicación
-- (Usaremos uno de los usuarios existentes)

-- 7. Crear la aplicación principal que conecta todo
INSERT INTO applications (
    student_id,
    father_id,
    mother_id,
    supporter_id,
    guardian_id,
    applicant_user_id,
    status,
    submission_date,
    additional_notes,
    created_at,
    updated_at
) VALUES (
    (SELECT id FROM students WHERE rut = '12345678-9'),
    (SELECT id FROM parents WHERE rut = '87654321-0' AND parent_type = 'FATHER'),
    (SELECT id FROM parents WHERE rut = '76543210-1' AND parent_type = 'MOTHER'),
    (SELECT id FROM supporters WHERE rut = '87654321-0'),
    (SELECT id FROM guardians WHERE rut = '76543210-1'),
    (SELECT id FROM users WHERE email = 'schweikart.cr@gmail.com'), -- Usuario existente
    'PENDING',                       -- status
    NOW(),                           -- submission_date
    'POSTULACIÓN COMPLETA DE PRUEBA', -- additional_notes en mayúsculas
    NOW(),
    NOW()
) RETURNING id;

-- 8. Verificar los datos creados
SELECT 
    a.id as application_id,
    s.first_name,
    s.paternal_last_name,
    s.maternal_last_name,
    s.rut as student_rut,
    s.grade_applied,
    s.school_applied,
    s.current_school,
    pf.full_name as father_name,
    pm.full_name as mother_name,
    sup.full_name as supporter_name,
    g.full_name as guardian_name,
    u.email as applicant_email,
    a.status,
    a.submission_date
FROM applications a
JOIN students s ON a.student_id = s.id
JOIN parents pf ON a.father_id = pf.id AND pf.parent_type = 'FATHER'
JOIN parents pm ON a.mother_id = pm.id AND pm.parent_type = 'MOTHER'
JOIN supporters sup ON a.supporter_id = sup.id
JOIN guardians g ON a.guardian_id = g.id
JOIN users u ON a.applicant_user_id = u.id
WHERE s.rut = '12345678-9';