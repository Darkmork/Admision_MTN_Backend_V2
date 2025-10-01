-- Script para crear usuarios esenciales en BD limpia
-- Fecha: 2025-08-22
-- Incluye usuario admin y evaluadores para testing

-- Crear usuario ADMIN principal
INSERT INTO users (first_name, last_name, email, password, rut, phone, role, educational_level, subject, email_verified, active, created_at) VALUES
('Jorge', 'Gangale', 'jorge.gangale@mtn.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '12345678-9', '+56912345678', 'ADMIN', 'ALL_LEVELS', 'ADMINISTRATION', true, true, NOW());

-- Crear usuario APODERADO para testing
INSERT INTO users (first_name, last_name, email, password, rut, phone, role, educational_level, subject, email_verified, active, created_at) VALUES
('Apoderado', 'Test', 'jorge.gangale@mail.udp.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '98765432-1', '+56987654321', 'APODERADO', 'GENERAL', 'FAMILY', true, true, NOW());

-- Crear algunos evaluadores para testing
INSERT INTO users (first_name, last_name, email, password, rut, phone, role, educational_level, subject, email_verified, active, created_at) VALUES
('María Elena', 'González', 'maria.prof@mtn.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '11111111-1', '+56911111111', 'TEACHER', 'PRESCHOOL', 'GENERAL', true, true, NOW()),
('Roberto', 'Silva', 'roberto.math@mtn.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '22222222-2', '+56922222222', 'TEACHER', 'BASIC', 'MATHEMATICS', true, true, NOW()),
('Carmen', 'Morales', 'carmen.lang@mtn.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '33333333-3', '+56933333333', 'TEACHER', 'HIGH_SCHOOL', 'LANGUAGE', true, true, NOW()),
('Ana', 'Psicóloga', 'ana.psico@mtn.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '44444444-4', '+56944444444', 'PSYCHOLOGIST', 'ALL_LEVELS', 'PSYCHOLOGY', true, true, NOW()),
('Luis', 'Director', 'luis.director@mtn.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '55555555-5', '+56955555555', 'CYCLE_DIRECTOR', 'ALL_LEVELS', 'DIRECTION', true, true, NOW());

-- Verificar usuarios creados
SELECT 
    CONCAT(first_name, ' ', last_name) as nombre_completo,
    email,
    role,
    educational_level,
    subject,
    active
FROM users 
ORDER BY role, email;