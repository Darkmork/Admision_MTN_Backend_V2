-- Crear entrevistadores adicionales de prueba con horarios de disponibilidad
-- Estos usuarios complementarán los existentes para tener más opciones de entrevistadores

-- Insertar entrevistadores adicionales con RUTs únicos
INSERT INTO users (first_name, last_name, email, password, rut, phone, role, educational_level, subject, email_verified, active, created_at) VALUES

-- PROFESORES ESPECIALIZADOS ADICIONALES
('Patricia', 'Silva', 'patricia.silva@mtn.cl', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '15678901-2', '+56915678901', 'TEACHER', 'HIGH_SCHOOL', 'LANGUAGE', true, true, NOW()),

('Ricardo', 'Vargas', 'ricardo.vargas@mtn.cl', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '15678902-0', '+56915678902', 'TEACHER', 'HIGH_SCHOOL', 'MATHEMATICS', true, true, NOW()),

-- DIRECTOR DE CICLO ADICIONAL
('Isabel', 'Moreno', 'isabel.moreno@mtn.cl', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '15678903-9', '+56915678903', 'CYCLE_DIRECTOR', 'PRESCHOOL', 'ALL_SUBJECTS', true, true, NOW()),

-- PSICÓLOGO ADICIONAL
('Diego', 'Fuentes', 'diego.fuentes@mtn.cl', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '15678904-7', '+56915678904', 'PSYCHOLOGIST', 'ALL_LEVELS', 'ALL_SUBJECTS', true, true, NOW());

-- Verificar todos los entrevistadores disponibles (existentes + nuevos)
SELECT 
    CONCAT(first_name, ' ', last_name) as nombre_completo,
    email,
    role as rol,
    educational_level as nivel_educativo,
    subject as materia,
    'Disponible para entrevistas' as estado
FROM users 
WHERE role IN ('CYCLE_DIRECTOR', 'PSYCHOLOGIST', 'TEACHER', 'COORDINATOR')
ORDER BY role, educational_level;

-- Mostrar resumen por tipo de entrevista que pueden realizar
SELECT 
    CASE 
        WHEN role = 'CYCLE_DIRECTOR' THEN '🎯 Entrevista Directiva'
        WHEN role = 'PSYCHOLOGIST' THEN '🧠 Evaluación Psicológica'
        WHEN role = 'TEACHER' AND subject = 'MATHEMATICS' THEN '🔢 Evaluación Matemáticas'
        WHEN role = 'TEACHER' AND subject = 'LANGUAGE' THEN '📚 Evaluación Lenguaje'
        WHEN role = 'TEACHER' AND educational_level = 'PRESCHOOL' THEN '🎨 Evaluación Preescolar'
        WHEN role = 'COORDINATOR' THEN '📋 Coordinación Académica'
        ELSE '📝 Evaluación General'
    END as tipo_entrevista,
    CONCAT(first_name, ' ', last_name) as entrevistador,
    educational_level as nivel,
    subject as especialidad
FROM users 
WHERE role IN ('CYCLE_DIRECTOR', 'PSYCHOLOGIST', 'TEACHER', 'COORDINATOR')
ORDER BY role, educational_level;

-- Información de horarios de disponibilidad (simulados para testing)
SELECT 
    CONCAT(first_name, ' ', last_name) as entrevistador,
    email,
    CASE 
        -- EXISTENTES
        WHEN email = 'ana.rivera@mtn.cl' THEN 'Lunes a Miércoles 9:00-12:00, 14:00-17:00'
        WHEN email = 'elena.castro@mtn.cl' THEN 'Martes y Jueves 10:00-13:00, 15:00-18:00'
        WHEN email = 'jorge.gangale@mtn.com' THEN 'Lunes, Miércoles, Viernes 8:00-12:00'
        WHEN email = 'carlos.morales@mtn.cl' THEN 'Martes a Jueves 9:00-13:00, 14:00-16:00'
        WHEN email = 'pedro.matematico@mtn.cl' THEN 'Lunes a Viernes 8:30-12:30'
        -- NUEVOS
        WHEN email = 'patricia.silva@mtn.cl' THEN 'Lunes, Miércoles, Viernes 9:00-13:00, 15:00-17:00'
        WHEN email = 'ricardo.vargas@mtn.cl' THEN 'Martes a Jueves 8:00-12:00, 14:00-18:00'
        WHEN email = 'isabel.moreno@mtn.cl' THEN 'Lunes a Viernes 8:00-11:00 (Preescolar)'
        WHEN email = 'diego.fuentes@mtn.cl' THEN 'Miércoles a Viernes 10:00-14:00, 15:00-17:00'
        ELSE 'Horarios por definir'
    END as horarios_disponibles,
    role as tipo_entrevistador
FROM users 
WHERE role IN ('CYCLE_DIRECTOR', 'PSYCHOLOGIST', 'TEACHER', 'COORDINATOR')
ORDER BY role;

-- Mostrar total de entrevistadores disponibles por tipo
SELECT 
    role as tipo_rol,
    COUNT(*) as cantidad_entrevistadores,
    STRING_AGG(CONCAT(first_name, ' ', last_name), ', ') as nombres
FROM users 
WHERE role IN ('CYCLE_DIRECTOR', 'PSYCHOLOGIST', 'TEACHER', 'COORDINATOR')
GROUP BY role
ORDER BY role;