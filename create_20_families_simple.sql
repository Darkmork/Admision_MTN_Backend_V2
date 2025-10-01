-- Script simplificado para crear 20 familias de prueba
-- IMPORTANTE: No tocar la autenticación - usar solo para datos de prueba

-- Limpiar datos existentes de prueba
DELETE FROM applications WHERE applicant_user_id IN (SELECT id FROM users WHERE email LIKE '%test.cl');
DELETE FROM guardians WHERE email LIKE '%test.cl';
DELETE FROM supporters WHERE email LIKE '%test.cl';
DELETE FROM parents WHERE email LIKE '%test.cl';
DELETE FROM students WHERE rut LIKE '40%';
DELETE FROM users WHERE email LIKE '%test.cl';

-- Crear 20 usuarios apoderados de prueba
INSERT INTO users (first_name, last_name, email, password, rut, phone, role, educational_level, subject, email_verified, active, created_at) VALUES
('María José', 'González', 'familia01@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '40001000-1', '+56911000001', 'APODERADO', NULL, NULL, true, true, NOW()),
('Carmen', 'Rodríguez', 'familia02@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '40001000-2', '+56911000002', 'APODERADO', NULL, NULL, true, true, NOW()),
('Patricia', 'Morales', 'familia03@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '40001000-3', '+56911000003', 'APODERADO', NULL, NULL, true, true, NOW()),
('Andrea', 'Silva', 'familia04@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '40001000-4', '+56911000004', 'APODERADO', NULL, NULL, true, true, NOW()),
('Claudia', 'Hernández', 'familia05@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '40001000-5', '+56911000005', 'APODERADO', NULL, NULL, true, true, NOW()),
('Valeria', 'Castro', 'familia06@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '40001000-6', '+56911000006', 'APODERADO', NULL, NULL, true, true, NOW()),
('Mónica', 'Vargas', 'familia07@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '40001000-7', '+56911000007', 'APODERADO', NULL, NULL, true, true, NOW()),
('Francisca', 'Muñoz', 'familia08@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '40001000-8', '+56911000008', 'APODERADO', NULL, NULL, true, true, NOW()),
('Soledad', 'Torres', 'familia09@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '40001000-9', '+56911000009', 'APODERADO', NULL, NULL, true, true, NOW()),
('Alejandra', 'Pérez', 'familia10@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '40001001-0', '+56911000010', 'APODERADO', NULL, NULL, true, true, NOW()),
('Lorena', 'López', 'familia11@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '40001001-1', '+56911000011', 'APODERADO', NULL, NULL, true, true, NOW()),
('Paola', 'García', 'familia12@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '40001001-2', '+56911000012', 'APODERADO', NULL, NULL, true, true, NOW()),
('Verónica', 'Martínez', 'familia13@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '40001001-3', '+56911000013', 'APODERADO', NULL, NULL, true, true, NOW()),
('Carolina', 'Sánchez', 'familia14@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '40001001-4', '+56911000014', 'APODERADO', NULL, NULL, true, true, NOW()),
('Daniela', 'Ramos', 'familia15@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '40001001-5', '+56911000015', 'APODERADO', NULL, NULL, true, true, NOW()),
('Marcela', 'Flores', 'familia16@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '40001001-6', '+56911000016', 'APODERADO', NULL, NULL, true, true, NOW()),
('Gladys', 'Contreras', 'familia17@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '40001001-7', '+56911000017', 'APODERADO', NULL, NULL, true, true, NOW()),
('Cecilia', 'Parra', 'familia18@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '40001001-8', '+56911000018', 'APODERADO', NULL, NULL, true, true, NOW()),
('Roxana', 'Aguilar', 'familia19@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '40001001-9', '+56911000019', 'APODERADO', NULL, NULL, true, true, NOW()),
('Ingrid', 'Fuentes', 'familia20@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '40001002-0', '+56911000020', 'APODERADO', NULL, NULL, true, true, NOW());

-- Crear 20 estudiantes distribuidos por nivel
INSERT INTO students (first_name, paternal_last_name, maternal_last_name, rut, birth_date, grade_applied, current_school, address, school_applied, created_at) VALUES
-- Prekinder (4 estudiantes)
('Emilia', 'González', 'Silva', '40002001-0', '2020-03-15', 'Prekinder', 'Jardín Los Angelitos', 'Las Condes 1234', 'MONTE_TABOR', NOW()),
('Matías', 'Rodríguez', 'López', '40002002-9', '2020-07-22', 'Prekinder', 'Jardín Santa María', 'Providencia 567', 'MONTE_TABOR', NOW()),
('Valentina', 'Morales', 'Castro', '40002003-7', '2020-11-08', 'Prekinder', 'Jardín Los Patitos', 'Ñuñoa 890', 'MONTE_TABOR', NOW()),
('Benjamín', 'Silva', 'Torres', '40002004-5', '2020-05-12', 'Prekinder', NULL, 'La Reina 2345', 'MONTE_TABOR', NOW()),

-- Kinder (4 estudiantes)
('Sofía', 'Hernández', 'Pérez', '40002005-3', '2019-02-14', 'Kinder', 'Jardín Los Rosales', 'Vitacura 3456', 'NAZARET', NOW()),
('Diego', 'Castro', 'Morales', '40002006-1', '2019-06-30', 'Kinder', 'Jardín San Francisco', 'Peñalolén 4567', 'NAZARET', NOW()),
('Isidora', 'Vargas', 'Silva', '40002007-K', '2019-09-18', 'Kinder', 'Jardín María Montessori', 'La Florida 5678', 'NAZARET', NOW()),
('Agustín', 'Muñoz', 'González', '40002008-8', '2019-12-03', 'Kinder', 'Jardín Los Pequitos', 'Maipú 6789', 'NAZARET', NOW()),

-- Básica (8 estudiantes)
('Martina', 'Torres', 'Hernández', '40002009-6', '2018-01-20', '1° Básico', 'Colegio San José', 'San Miguel 7890', 'MONTE_TABOR', NOW()),
('Lucas', 'Pérez', 'Castro', '40002010-K', '2017-04-25', '2° Básico', 'Escuela Los Pinos', 'Quilicura 8901', 'MONTE_TABOR', NOW()),
('Antonella', 'López', 'Vargas', '40002011-8', '2016-08-10', '3° Básico', 'Colegio Santa Teresa', 'Puente Alto 9012', 'NAZARET', NOW()),
('Tomás', 'García', 'Muñoz', '40002012-6', '2015-11-15', '4° Básico', 'Escuela República', 'Renca 0123', 'MONTE_TABOR', NOW()),
('Florencia', 'Martínez', 'Torres', '40002013-4', '2014-03-08', '5° Básico', 'Colegio Los Andes', 'Cerro Navia 1234', 'NAZARET', NOW()),
('Sebastián', 'Sánchez', 'Pérez', '40002014-2', '2013-07-12', '6° Básico', 'Escuela El Bosque', 'Estación Central 2345', 'MONTE_TABOR', NOW()),
('Catalina', 'Ramos', 'López', '40002015-0', '2012-10-22', '7° Básico', 'Colegio San Pablo', 'Pedro Aguirre Cerda 3456', 'NAZARET', NOW()),
('Maximiliano', 'Flores', 'García', '40002016-9', '2011-12-30', '8° Básico', 'Escuela Gabriela Mistral', 'Lo Espejo 4567', 'MONTE_TABOR', NOW()),

-- Media (4 estudiantes)
('Javiera', 'Contreras', 'Martínez', '40002017-7', '2010-02-18', 'I° Medio', 'Liceo Pablo Neruda', 'Independencia 5678', 'NAZARET', NOW()),
('Nicolás', 'Parra', 'Sánchez', '40002018-5', '2009-05-14', 'II° Medio', 'Colegio Industrial', 'Recoleta 6789', 'MONTE_TABOR', NOW()),
('Fernanda', 'Aguilar', 'Ramos', '40002019-3', '2008-09-26', 'III° Medio', 'Liceo de Niñas N°1', 'Conchalí 7890', 'NAZARET', NOW()),
('Francisco', 'Fuentes', 'Flores', '40002020-7', '2007-11-11', 'IV° Medio', 'Liceo Comercial', 'Huechuraba 8901', 'MONTE_TABOR', NOW());

-- Crear 40 padres (2 por familia)
INSERT INTO parents (full_name, rut, email, phone, profession, address, parent_type, created_at) VALUES
-- Familia 1
('Carlos González Hernández', '40003001-2', 'carlos.gonzalez@test.cl', '+56921000001', 'Ingeniero Civil', 'Las Condes 1234', 'FATHER', NOW()),
('María José González Silva', '40003001-3', 'familia01@test.cl', '+56911000001', 'Profesora', 'Las Condes 1234', 'MOTHER', NOW()),
-- Familia 2
('Pedro Rodríguez Morales', '40003002-0', 'pedro.rodriguez@test.cl', '+56921000002', 'Contador', 'Providencia 567', 'FATHER', NOW()),
('Carmen Rodríguez López', '40003002-1', 'familia02@test.cl', '+56911000002', 'Enfermera', 'Providencia 567', 'MOTHER', NOW()),
-- Familia 3
('Luis Morales Pérez', '40003003-9', 'luis.morales@test.cl', '+56921000003', 'Médico', 'Ñuñoa 890', 'FATHER', NOW()),
('Patricia Morales Castro', '40003003-K', 'familia03@test.cl', '+56911000003', 'Psicóloga', 'Ñuñoa 890', 'MOTHER', NOW()),
-- Familia 4
('Roberto Silva Vargas', '40003004-7', 'roberto.silva@test.cl', '+56921000004', 'Arquitecto', 'La Reina 2345', 'FATHER', NOW()),
('Andrea Silva Torres', '40003004-8', 'familia04@test.cl', '+56911000004', 'Diseñadora', 'La Reina 2345', 'MOTHER', NOW()),
-- Familia 5
('Miguel Hernández Silva', '40003005-5', 'miguel.hernandez@test.cl', '+56921000005', 'Abogado', 'Vitacura 3456', 'FATHER', NOW()),
('Claudia Hernández Pérez', '40003005-6', 'familia05@test.cl', '+56911000005', 'Kinesióloga', 'Vitacura 3456', 'MOTHER', NOW()),
-- Familia 6
('Jorge Castro Torres', '40003006-3', 'jorge.castro@test.cl', '+56921000006', 'Veterinario', 'Peñalolén 4567', 'FATHER', NOW()),
('Valeria Castro Morales', '40003006-4', 'familia06@test.cl', '+56911000006', 'Nutricionista', 'Peñalolén 4567', 'MOTHER', NOW()),
-- Familia 7
('Andrés Vargas González', '40003007-1', 'andres.vargas@test.cl', '+56921000007', 'Periodista', 'La Florida 5678', 'FATHER', NOW()),
('Mónica Vargas Silva', '40003007-2', 'familia07@test.cl', '+56911000007', 'Traductora', 'La Florida 5678', 'MOTHER', NOW()),
-- Familia 8
('Felipe Muñoz Hernández', '40003008-K', 'felipe.munoz@test.cl', '+56921000008', 'Dentista', 'Maipú 6789', 'FATHER', NOW()),
('Francisca Muñoz González', '40003008-0', 'familia08@test.cl', '+56911000008', 'Fonoaudióloga', 'Maipú 6789', 'MOTHER', NOW()),
-- Familia 9
('Patricio Torres Castro', '40003009-8', 'patricio.torres@test.cl', '+56921000009', 'Electricista', 'San Miguel 7890', 'FATHER', NOW()),
('Soledad Torres Hernández', '40003009-9', 'familia09@test.cl', '+56911000009', 'Secretaria', 'San Miguel 7890', 'MOTHER', NOW()),
-- Familia 10
('Rodrigo Pérez Vargas', '40003010-2', 'rodrigo.perez@test.cl', '+56921000010', 'Mecánico', 'Quilicura 8901', 'FATHER', NOW()),
('Alejandra Pérez Castro', '40003010-3', 'familia10@test.cl', '+56911000010', 'Técnico en Párvulos', 'Quilicura 8901', 'MOTHER', NOW()),
-- Familia 11
('Cristián López Muñoz', '40003011-0', 'cristian.lopez@test.cl', '+56921000011', 'Profesor', 'Puente Alto 9012', 'FATHER', NOW()),
('Lorena López Vargas', '40003011-1', 'familia11@test.cl', '+56911000011', 'Administradora', 'Puente Alto 9012', 'MOTHER', NOW()),
-- Familia 12
('Gonzalo García Torres', '40003012-9', 'gonzalo.garcia@test.cl', '+56921000012', 'Soldador', 'Renca 0123', 'FATHER', NOW()),
('Paola García Muñoz', '40003012-K', 'familia12@test.cl', '+56911000012', 'Cajera', 'Renca 0123', 'MOTHER', NOW()),
-- Familia 13
('Fernando Martínez González', '40003013-7', 'fernando.martinez@test.cl', '+56921000013', 'Carpintero', 'Cerro Navia 1234', 'FATHER', NOW()),
('Verónica Martínez Torres', '40003013-8', 'familia13@test.cl', '+56911000013', 'Auxiliar de Enfermería', 'Cerro Navia 1234', 'MOTHER', NOW()),
-- Familia 14
('Mauricio Sánchez Hernández', '40003014-5', 'mauricio.sanchez@test.cl', '+56921000014', 'Guardia', 'Estación Central 2345', 'FATHER', NOW()),
('Carolina Sánchez Pérez', '40003014-6', 'familia14@test.cl', '+56911000014', 'Vendedora', 'Estación Central 2345', 'MOTHER', NOW()),
-- Familia 15
('Osvaldo Ramos Castro', '40003015-3', 'osvaldo.ramos@test.cl', '+56921000015', 'Conductor', 'Pedro Aguirre Cerda 3456', 'FATHER', NOW()),
('Daniela Ramos López', '40003015-4', 'familia15@test.cl', '+56911000015', 'Operaria', 'Pedro Aguirre Cerda 3456', 'MOTHER', NOW()),
-- Familia 16
('Héctor Flores Vargas', '40003016-1', 'hector.flores@test.cl', '+56921000016', 'Pintor', 'Lo Espejo 4567', 'FATHER', NOW()),
('Marcela Flores García', '40003016-2', 'familia16@test.cl', '+56911000016', 'Asesora del Hogar', 'Lo Espejo 4567', 'MOTHER', NOW()),
-- Familia 17
('Ricardo Contreras Muñoz', '40003017-K', 'ricardo.contreras@test.cl', '+56921000017', 'Técnico', 'Independencia 5678', 'FATHER', NOW()),
('Gladys Contreras Martínez', '40003017-0', 'familia17@test.cl', '+56911000017', 'Costurera', 'Independencia 5678', 'MOTHER', NOW()),
-- Familia 18
('Enrique Parra Torres', '40003018-8', 'enrique.parra@test.cl', '+56921000018', 'Gásfiter', 'Recoleta 6789', 'FATHER', NOW()),
('Cecilia Parra Sánchez', '40003018-9', 'familia18@test.cl', '+56911000018', 'Peluquera', 'Recoleta 6789', 'MOTHER', NOW()),
-- Familia 19
('Ramón Aguilar González', '40003019-6', 'ramon.aguilar@test.cl', '+56921000019', 'Conserje', 'Conchalí 7890', 'FATHER', NOW()),
('Roxana Aguilar Ramos', '40003019-7', 'familia19@test.cl', '+56911000019', 'Manipuladora de Alimentos', 'Conchalí 7890', 'MOTHER', NOW()),
-- Familia 20
('Iván Fuentes Hernández', '40003020-0', 'ivan.fuentes@test.cl', '+56921000020', 'Bodeguero', 'Huechuraba 8901', 'FATHER', NOW()),
('Ingrid Fuentes Flores', '40003020-1', 'familia20@test.cl', '+56911000020', 'Auxiliar de Aseo', 'Huechuraba 8901', 'MOTHER', NOW());

-- Crear sostenedores (padres)
INSERT INTO supporters (full_name, rut, email, phone, relationship, created_at) VALUES
('Carlos González Hernández', '40003001-2', 'carlos.gonzalez@test.cl', '+56921000001', 'PADRE_MADRE', NOW()),
('Pedro Rodríguez Morales', '40003002-0', 'pedro.rodriguez@test.cl', '+56921000002', 'PADRE_MADRE', NOW()),
('Luis Morales Pérez', '40003003-9', 'luis.morales@test.cl', '+56921000003', 'PADRE_MADRE', NOW()),
('Roberto Silva Vargas', '40003004-7', 'roberto.silva@test.cl', '+56921000004', 'PADRE_MADRE', NOW()),
('Miguel Hernández Silva', '40003005-5', 'miguel.hernandez@test.cl', '+56921000005', 'PADRE_MADRE', NOW()),
('Jorge Castro Torres', '40003006-3', 'jorge.castro@test.cl', '+56921000006', 'PADRE_MADRE', NOW()),
('Andrés Vargas González', '40003007-1', 'andres.vargas@test.cl', '+56921000007', 'PADRE_MADRE', NOW()),
('Felipe Muñoz Hernández', '40003008-K', 'felipe.munoz@test.cl', '+56921000008', 'PADRE_MADRE', NOW()),
('Patricio Torres Castro', '40003009-8', 'patricio.torres@test.cl', '+56921000009', 'PADRE_MADRE', NOW()),
('Rodrigo Pérez Vargas', '40003010-2', 'rodrigo.perez@test.cl', '+56921000010', 'PADRE_MADRE', NOW()),
('Cristián López Muñoz', '40003011-0', 'cristian.lopez@test.cl', '+56921000011', 'PADRE_MADRE', NOW()),
('Gonzalo García Torres', '40003012-9', 'gonzalo.garcia@test.cl', '+56921000012', 'PADRE_MADRE', NOW()),
('Fernando Martínez González', '40003013-7', 'fernando.martinez@test.cl', '+56921000013', 'PADRE_MADRE', NOW()),
('Mauricio Sánchez Hernández', '40003014-5', 'mauricio.sanchez@test.cl', '+56921000014', 'PADRE_MADRE', NOW()),
('Osvaldo Ramos Castro', '40003015-3', 'osvaldo.ramos@test.cl', '+56921000015', 'PADRE_MADRE', NOW()),
('Héctor Flores Vargas', '40003016-1', 'hector.flores@test.cl', '+56921000016', 'PADRE_MADRE', NOW()),
('Ricardo Contreras Muñoz', '40003017-K', 'ricardo.contreras@test.cl', '+56921000017', 'PADRE_MADRE', NOW()),
('Enrique Parra Torres', '40003018-8', 'enrique.parra@test.cl', '+56921000018', 'PADRE_MADRE', NOW()),
('Ramón Aguilar González', '40003019-6', 'ramon.aguilar@test.cl', '+56921000019', 'PADRE_MADRE', NOW()),
('Iván Fuentes Hernández', '40003020-0', 'ivan.fuentes@test.cl', '+56921000020', 'PADRE_MADRE', NOW());

-- Crear apoderados (madres)
INSERT INTO guardians (full_name, rut, email, phone, relationship, created_at) VALUES
('María José González Silva', '40003001-3', 'familia01@test.cl', '+56911000001', 'PADRE_MADRE', NOW()),
('Carmen Rodríguez López', '40003002-1', 'familia02@test.cl', '+56911000002', 'PADRE_MADRE', NOW()),
('Patricia Morales Castro', '40003003-K', 'familia03@test.cl', '+56911000003', 'PADRE_MADRE', NOW()),
('Andrea Silva Torres', '40003004-8', 'familia04@test.cl', '+56911000004', 'PADRE_MADRE', NOW()),
('Claudia Hernández Pérez', '40003005-6', 'familia05@test.cl', '+56911000005', 'PADRE_MADRE', NOW()),
('Valeria Castro Morales', '40003006-4', 'familia06@test.cl', '+56911000006', 'PADRE_MADRE', NOW()),
('Mónica Vargas Silva', '40003007-2', 'familia07@test.cl', '+56911000007', 'PADRE_MADRE', NOW()),
('Francisca Muñoz González', '40003008-0', 'familia08@test.cl', '+56911000008', 'PADRE_MADRE', NOW()),
('Soledad Torres Hernández', '40003009-9', 'familia09@test.cl', '+56911000009', 'PADRE_MADRE', NOW()),
('Alejandra Pérez Castro', '40003010-3', 'familia10@test.cl', '+56911000010', 'PADRE_MADRE', NOW()),
('Lorena López Vargas', '40003011-1', 'familia11@test.cl', '+56911000011', 'PADRE_MADRE', NOW()),
('Paola García Muñoz', '40003012-K', 'familia12@test.cl', '+56911000012', 'PADRE_MADRE', NOW()),
('Verónica Martínez Torres', '40003013-8', 'familia13@test.cl', '+56911000013', 'PADRE_MADRE', NOW()),
('Carolina Sánchez Pérez', '40003014-6', 'familia14@test.cl', '+56911000014', 'PADRE_MADRE', NOW()),
('Daniela Ramos López', '40003015-4', 'familia15@test.cl', '+56911000015', 'PADRE_MADRE', NOW()),
('Marcela Flores García', '40003016-2', 'familia16@test.cl', '+56911000016', 'PADRE_MADRE', NOW()),
('Gladys Contreras Martínez', '40003017-0', 'familia17@test.cl', '+56911000017', 'PADRE_MADRE', NOW()),
('Cecilia Parra Sánchez', '40003018-9', 'familia18@test.cl', '+56911000018', 'PADRE_MADRE', NOW()),
('Roxana Aguilar Ramos', '40003019-7', 'familia19@test.cl', '+56911000019', 'PADRE_MADRE', NOW()),
('Ingrid Fuentes Flores', '40003020-1', 'familia20@test.cl', '+56911000020', 'PADRE_MADRE', NOW());

-- Crear las 20 postulaciones con estados variados
DO $$
DECLARE
    student_rec RECORD;
    father_id INT;
    mother_id INT;
    supporter_id INT;
    guardian_id INT;
    user_id INT;
    app_status TEXT;
BEGIN
    FOR student_rec IN 
        SELECT id, ROW_NUMBER() OVER (ORDER BY id) as row_num
        FROM students 
        WHERE rut LIKE '40002%'
        ORDER BY id
    LOOP
        -- Obtener IDs de padres (padre es número impar, madre es número par)
        SELECT id INTO father_id FROM parents WHERE rut = '40003' || LPAD((student_rec.row_num::text), 3, '0') || '-2' AND parent_type = 'FATHER';
        SELECT id INTO mother_id FROM parents WHERE rut = '40003' || LPAD((student_rec.row_num::text), 3, '0') || '-3' AND parent_type = 'MOTHER';
        
        -- Obtener IDs de supporter y guardian
        SELECT id INTO supporter_id FROM supporters WHERE rut = '40003' || LPAD((student_rec.row_num::text), 3, '0') || '-2';
        SELECT id INTO guardian_id FROM guardians WHERE rut = '40003' || LPAD((student_rec.row_num::text), 3, '0') || '-3';
        
        -- Obtener ID de usuario apoderado
        SELECT id INTO user_id FROM users WHERE email = 'familia' || LPAD((student_rec.row_num::text), 2, '0') || '@test.cl';
        
        -- Asignar estado basado en el número de fila
        CASE student_rec.row_num % 8
            WHEN 1 THEN app_status := 'PENDING';
            WHEN 2 THEN app_status := 'UNDER_REVIEW';
            WHEN 3 THEN app_status := 'INTERVIEW_SCHEDULED';
            WHEN 4 THEN app_status := 'EXAM_SCHEDULED';
            WHEN 5 THEN app_status := 'APPROVED';
            WHEN 6 THEN app_status := 'REJECTED';
            WHEN 7 THEN app_status := 'WAITLIST';
            ELSE app_status := 'PENDING';
        END CASE;
        
        -- Insertar la postulación
        INSERT INTO applications (
            student_id, 
            father_id, 
            mother_id, 
            supporter_id, 
            guardian_id, 
            applicant_user_id, 
            status, 
            submission_date, 
            created_at
        ) VALUES (
            student_rec.id,
            father_id,
            mother_id,
            supporter_id,
            guardian_id,
            user_id,
            app_status,
            NOW() - INTERVAL (student_rec.row_num * 2) DAY,
            NOW()
        );
    END LOOP;
END $$;

-- Mostrar resumen
SELECT 
    'RESUMEN DE DATOS CREADOS' as descripcion,
    (SELECT COUNT(*) FROM users WHERE email LIKE '%@test.cl') as apoderados_creados,
    (SELECT COUNT(*) FROM students WHERE rut LIKE '40002%') as estudiantes_creados,
    (SELECT COUNT(*) FROM applications WHERE created_at > NOW() - INTERVAL '1 hour') as postulaciones_creadas;

-- Mostrar distribución por nivel
SELECT 
    s.grade_applied as nivel,
    COUNT(*) as cantidad,
    STRING_AGG(DISTINCT a.status, ', ') as estados
FROM applications a
JOIN students s ON a.student_id = s.id
WHERE a.created_at > NOW() - INTERVAL '1 hour'
GROUP BY s.grade_applied
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
    END;