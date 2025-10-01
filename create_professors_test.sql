-- Crear profesores de prueba en la base de datos correcta (Admision_MTN_DB)

-- Limpiar usuarios existentes que no sean admin
DELETE FROM users WHERE role != 'ADMIN';

-- Crear profesores con contraseñas BCrypt válidas (password: 12345678)
INSERT INTO users (first_name, last_name, email, password, rut, phone, role, educational_level, subject, email_verified, active, created_at) VALUES
('Alejandra', 'Flores', 'alejandra.flores@mtn.cl', '$2a$10$bqjQzuMlbjM74VdKgb5M2u/qTEBNzKOXPlXVDKaHoMB6ujDmPmMUa', '12345678-9', '+56912345678', 'TEACHER', 'BASIC', 'MATHEMATICS', true, true, NOW()),
('Carlos', 'Mendoza', 'carlos.mendoza@mtn.cl', '$2a$10$bqjQzuMlbjM74VdKgb5M2u/qTEBNzKOXPlXVDKaHoMB6ujDmPmMUa', '87654321-0', '+56987654321', 'TEACHER', 'HIGH_SCHOOL', 'LANGUAGE', true, true, NOW()),
('María Elena', 'González', 'maria.gonzalez@mtn.cl', '$2a$10$bqjQzuMlbjM74VdKgb5M2u/qTEBNzKOXPlXVDKaHoMB6ujDmPmMUa', '11111111-1', '+56911111111', 'TEACHER', 'PRESCHOOL', 'GENERAL', true, true, NOW()),
('Roberto', 'Silva', 'roberto.silva@mtn.cl', '$2a$10$bqjQzuMlbjM74VdKgb5M2u/qTEBNzKOXPlXVDKaHoMB6ujDmPmMUa', '22222222-2', '+56922222222', 'COORDINATOR', 'ALL_LEVELS', 'MATHEMATICS', true, true, NOW()),
('Ana', 'Morales', 'ana.morales@mtn.cl', '$2a$10$bqjQzuMlbjM74VdKgb5M2u/qTEBNzKOXPlXVDKaHoMB6ujDmPmMUa', '33333333-3', '+56933333333', 'PSYCHOLOGIST', 'ALL_LEVELS', 'ALL_SUBJECTS', true, true, NOW()),
('Pedro', 'López', 'pedro.lopez@mtn.cl', '$2a$10$bqjQzuMlbjM74VdKgb5M2u/qTEBNzKOXPlXVDKaHoMB6ujDmPmMUa', '44444444-4', '+56944444444', 'CYCLE_DIRECTOR', 'ALL_LEVELS', 'ALL_SUBJECTS', true, true, NOW());

-- Verificar los usuarios creados
SELECT 
    CONCAT(first_name, ' ', last_name) as nombre_completo,
    role,
    educational_level,
    subject,
    email,
    active
FROM users 
WHERE role != 'ADMIN'
ORDER BY role, educational_level, subject;