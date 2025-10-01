-- Migración de usuarios únicos de Admision_MTN_DB a Admisión_MTN_DB
-- Ejecutar en la base de datos correcta (con tilde)

-- Insertar usuarios únicos que están en la base sin tilde pero no en la con tilde
INSERT INTO users (first_name, last_name, email, password, rut, phone, role, educational_level, subject, email_verified, active, created_at) VALUES
-- Usuarios del sistema que están en BD sin tilde
('Jorge', 'Gangale', 'jorge.gangale@mtn.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '12345678-9', '+56912345678', 'ADMIN', 'ALL_LEVELS', 'ALL_SUBJECTS', true, true, NOW()),
('Alejandro', 'Contreras', 'alejandro.contreras@outlook.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '11111111-1', '+56911111111', 'APODERADO', 'ALL_LEVELS', 'GENERAL', true, true, NOW()),
('Ana', 'González', 'ana.gonzalez@gmail.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '22222222-2', '+56922222222', 'APODERADO', 'ALL_LEVELS', 'GENERAL', true, true, NOW()),
('Carlos', 'Pérez', 'carlos.perez@hotmail.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '33333333-3', '+56933333333', 'APODERADO', 'ALL_LEVELS', 'GENERAL', true, true, NOW()),
('Luis', 'Martínez', 'luis.martinez@yahoo.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '44444444-4', '+56944444444', 'APODERADO', 'ALL_LEVELS', 'GENERAL', true, true, NOW()),
('María', 'López', 'maria.lopez@live.cl', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '55555555-5', '+56955555555', 'APODERADO', 'ALL_LEVELS', 'GENERAL', true, true, NOW());

-- Insertar evaluadores que falten
INSERT INTO users (first_name, last_name, email, password, rut, phone, role, educational_level, subject, email_verified, active, created_at) VALUES
('Pedro', 'Matemático', 'pedro.matematico@mtn.cl', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '12123123-1', '+56912123123', 'TEACHER', 'BASIC', 'MATHEMATICS', true, true, NOW()),
('Ana', 'Lenguaje', 'ana.lenguaje@mtn.cl', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '13134134-2', '+56913134134', 'TEACHER', 'HIGH_SCHOOL', 'LANGUAGE', true, true, NOW()),
('Carlos', 'Inglés', 'carlos.ingles@mtn.cl', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '14145145-3', '+56914145145', 'TEACHER', 'ALL_LEVELS', 'ENGLISH', true, true, NOW())
ON CONFLICT (email) DO NOTHING;

SELECT 'Migración de usuarios completada' as status;