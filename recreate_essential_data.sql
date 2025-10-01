-- Recrear datos esenciales después del create-drop
-- Base de datos unificada: Admisión_MTN_DB

-- 1. Usuarios esenciales del sistema
INSERT INTO users (first_name, last_name, email, password, rut, phone, role, educational_level, subject, email_verified, active, created_at) VALUES
-- Admin principal
('Jorge', 'Gangale', 'jorge.gangale@mtn.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '12345678-9', '+56912345678', 'ADMIN', 'ALL_LEVELS', 'ALL_SUBJECTS', true, true, NOW()),

-- Evaluadores del sistema
('Ana', 'Rivera', 'ana.rivera@mtn.cl', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '11111111-1', '+56911111111', 'CYCLE_DIRECTOR', 'HIGH_SCHOOL', 'GENERAL', true, true, NOW()),
('Carlos', 'Morales', 'carlos.morales@mtn.cl', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '22222222-2', '+56922222222', 'COORDINATOR', 'ALL_LEVELS', 'MATHEMATICS', true, true, NOW()),
('Elena', 'Castro', 'elena.castro@mtn.cl', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '33333333-3', '+56933333333', 'PSYCHOLOGIST', 'ALL_LEVELS', 'GENERAL', true, true, NOW()),
('Pedro', 'Matemático', 'pedro.matematico@mtn.cl', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '44444444-4', '+56944444444', 'TEACHER', 'BASIC', 'MATHEMATICS', true, true, NOW()),

-- Familias de prueba
('Familia', 'Test01', 'familia01@test.cl', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '55555555-5', '+56955555555', 'APODERADO', 'ALL_LEVELS', 'GENERAL', true, true, NOW()),
('Familia', 'Test02', 'familia02@test.cl', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '66666666-6', '+56966666666', 'APODERADO', 'ALL_LEVELS', 'GENERAL', true, true, NOW());

-- 2. Estudiantes de prueba
INSERT INTO students (first_name, last_name, maternal_last_name, rut, birth_date, grade_applied, current_school, target_school, created_at) VALUES
('Juan', 'Pérez', 'González', '20001001-1', '2015-05-15', 'PRIMERO_BASICO', 'Escuela Anterior', 'MONTE_TABOR', NOW()),
('María', 'López', 'Silva', '20001002-K', '2014-08-20', 'SEGUNDO_BASICO', 'Colegio Previo', 'NAZARET', NOW()),
('Carlos', 'Rodríguez', 'Torres', '20001003-8', '2016-03-10', 'PREKINDER', 'Jardín Infantil', 'MONTE_TABOR', NOW());

-- 3. Padres de prueba  
INSERT INTO parents (first_name, last_name, maternal_last_name, rut, email, phone, relationship, created_at) VALUES
-- Padres de Juan
('Roberto', 'Pérez', 'Martínez', '12001001-1', 'roberto.perez@email.com', '+56912001001', 'FATHER', NOW()),
('Carmen', 'González', 'Vega', '12001002-K', 'carmen.gonzalez@email.com', '+56912001002', 'MOTHER', NOW()),
-- Padres de María
('Luis', 'López', 'Hernández', '12002001-8', 'luis.lopez@email.com', '+56912002001', 'FATHER', NOW()),  
('Ana', 'Silva', 'Morales', '12002002-6', 'ana.silva@email.com', '+56912002002', 'MOTHER', NOW()),
-- Padres de Carlos
('Diego', 'Rodríguez', 'Castro', '12003001-4', 'diego.rodriguez@email.com', '+56912003001', 'FATHER', NOW()),
('Sofía', 'Torres', 'Ramírez', '12003002-2', 'sofia.torres@email.com', '+56912003002', 'MOTHER', NOW());

-- 4. Supporters y Guardians (usando los mismos padres)
INSERT INTO supporters (first_name, last_name, maternal_last_name, rut, email, phone, relationship, created_at) VALUES
('Roberto', 'Pérez', 'Martínez', '12001001-1', 'roberto.perez@email.com', '+56912001001', 'FATHER', NOW()),
('Luis', 'López', 'Hernández', '12002001-8', 'luis.lopez@email.com', '+56912002001', 'FATHER', NOW()),
('Diego', 'Rodríguez', 'Castro', '12003001-4', 'diego.rodriguez@email.com', '+56912003001', 'FATHER', NOW());

INSERT INTO guardians (first_name, last_name, maternal_last_name, rut, email, phone, relationship, created_at) VALUES
('Carmen', 'González', 'Vega', '12001002-K', 'carmen.gonzalez@email.com', '+56912001002', 'MOTHER', NOW()),
('Ana', 'Silva', 'Morales', '12002002-6', 'ana.silva@email.com', '+56912002002', 'MOTHER', NOW()),
('Sofía', 'Torres', 'Ramírez', '12003002-2', 'sofia.torres@email.com', '+56912003002', 'MOTHER', NOW());

-- 5. Aplicaciones
INSERT INTO applications (student_id, father_id, mother_id, supporter_id, guardian_id, applicant_user_id, status, submission_date, created_at) VALUES
(1, 1, 2, 1, 1, 6, 'INTERVIEW_SCHEDULED', NOW() - INTERVAL '5 days', NOW()),
(2, 3, 4, 2, 2, 7, 'INTERVIEW_SCHEDULED', NOW() - INTERVAL '3 days', NOW()),
(3, 5, 6, 3, 3, 6, 'INTERVIEW_SCHEDULED', NOW() - INTERVAL '7 days', NOW());

-- 6. ENTREVISTAS CRÍTICAS - Las que necesitamos para que funcione el sistema
INSERT INTO interviews (application_id, interviewer_id, status, interview_type, scheduled_date, duration_minutes, location, notes, created_at) VALUES
(1, 2, 'SCHEDULED', 'INDIVIDUAL', '2025-09-02 13:30:00', 60, 'Oficina Principal - Monte Tabor', 'Entrevista familia Pérez-González', NOW()),
(2, 2, 'SCHEDULED', 'FAMILY', '2025-09-03 14:00:00', 90, 'Sala de Reuniones - Nazaret', 'Entrevista familia López-Silva', NOW()),  
(3, 3, 'SCHEDULED', 'INDIVIDUAL', '2025-09-04 18:00:00', 45, 'Oficina Coordinación - Monte Tabor', 'Entrevista familia Rodríguez-Torres', NOW());

SELECT 'Datos esenciales recreados exitosamente' as status;
SELECT 'Usuarios creados: ' || COUNT(*) as usuarios FROM users;
SELECT 'Estudiantes creados: ' || COUNT(*) as estudiantes FROM students;
SELECT 'Entrevistas creadas: ' || COUNT(*) as entrevistas FROM interviews;