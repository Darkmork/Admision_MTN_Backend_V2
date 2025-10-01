-- Script mejorado para crear 20 familias de prueba sin conflictos
-- IMPORTANTE: No tocar la autenticación - usar solo para datos de prueba

-- Limpiar datos existentes de prueba (solo los de prueba)
DELETE FROM applications WHERE applicant_user_id IN (SELECT id FROM users WHERE email LIKE '%@test.cl');
DELETE FROM guardians WHERE email LIKE '%@test.cl';
DELETE FROM supporters WHERE email LIKE '%@test.cl';
DELETE FROM parents WHERE email LIKE '%@test.cl';
DELETE FROM students WHERE rut LIKE '30%' OR rut LIKE '31%' OR rut LIKE '32%' OR rut LIKE '33%';
DELETE FROM users WHERE email LIKE '%@test.cl';

-- Crear 20 usuarios apoderados de prueba
INSERT INTO users (first_name, last_name, email, password, rut, phone, role, educational_level, subject, email_verified, active, created_at) VALUES
-- Apoderados para Prekinder (4 familias)
('María José', 'González', 'maria.gonzalez.test@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '30000001-1', '+56911111111', 'APODERADO', NULL, NULL, true, true, NOW()),
('Carmen', 'Rodríguez', 'carmen.rodriguez.test@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '30000002-K', '+56911111112', 'APODERADO', NULL, NULL, true, true, NOW()),
('Patricia', 'Morales', 'patricia.morales.test@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '30000003-8', '+56911111113', 'APODERADO', NULL, NULL, true, true, NOW()),
('Andrea', 'Silva', 'andrea.silva.test@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '30000004-6', '+56911111114', 'APODERADO', NULL, NULL, true, true, NOW()),

-- Apoderados para Kinder (4 familias)
('Claudia', 'Hernández', 'claudia.hernandez.test@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '30000005-4', '+56911111115', 'APODERADO', NULL, NULL, true, true, NOW()),
('Valeria', 'Castro', 'valeria.castro.test@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '30000006-2', '+56911111116', 'APODERADO', NULL, NULL, true, true, NOW()),
('Mónica', 'Vargas', 'monica.vargas.test@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '30000007-0', '+56911111117', 'APODERADO', NULL, NULL, true, true, NOW()),
('Francisca', 'Muñoz', 'francisca.munoz.test@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '30000008-9', '+56911111118', 'APODERADO', NULL, NULL, true, true, NOW()),

-- Apoderados para Básica (8 familias)
('Soledad', 'Torres', 'soledad.torres.test@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '30000009-7', '+56911111119', 'APODERADO', NULL, NULL, true, true, NOW()),
('Alejandra', 'Pérez', 'alejandra.perez.test@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '30000010-0', '+56911111120', 'APODERADO', NULL, NULL, true, true, NOW()),
('Lorena', 'López', 'lorena.lopez.test@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '30000011-9', '+56911111121', 'APODERADO', NULL, NULL, true, true, NOW()),
('Paola', 'García', 'paola.garcia.test@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '30000012-7', '+56911111122', 'APODERADO', NULL, NULL, true, true, NOW()),
('Verónica', 'Martínez', 'veronica.martinez.test@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '30000013-5', '+56911111123', 'APODERADO', NULL, NULL, true, true, NOW()),
('Carolina', 'Sánchez', 'carolina.sanchez.test@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '30000014-3', '+56911111124', 'APODERADO', NULL, NULL, true, true, NOW()),
('Daniela', 'Ramos', 'daniela.ramos.test@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '30000015-1', '+56911111125', 'APODERADO', NULL, NULL, true, true, NOW()),
('Marcela', 'Flores', 'marcela.flores.test@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '30000016-K', '+56911111126', 'APODERADO', NULL, NULL, true, true, NOW()),

-- Apoderados para Media (4 familias)
('Gladys', 'Contreras', 'gladys.contreras.test@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '30000017-8', '+56911111127', 'APODERADO', NULL, NULL, true, true, NOW()),
('Cecilia', 'Parra', 'cecilia.parra.test@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '30000018-6', '+56911111128', 'APODERADO', NULL, NULL, true, true, NOW()),
('Roxana', 'Aguilar', 'roxana.aguilar.test@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '30000019-4', '+56911111129', 'APODERADO', NULL, NULL, true, true, NOW()),
('Ingrid', 'Fuentes', 'ingrid.fuentes.test@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '30000020-8', '+56911111130', 'APODERADO', NULL, NULL, true, true, NOW());

-- Crear estudiantes distribuidos por nivel educativo
INSERT INTO students (first_name, paternal_last_name, maternal_last_name, rut, birth_date, grade_applied, current_school, address, school_applied, created_at) VALUES
-- Estudiantes Prekinder (4-5 años)
('Emilia', 'González', 'Silva', '31000001-2', '2020-03-15', 'Prekinder', 'Jardín Los Angelitos', 'Las Condes 1234, Las Condes', 'MONTE_TABOR', NOW()),
('Matías', 'Rodríguez', 'López', '31000002-0', '2020-07-22', 'Prekinder', 'Jardín Santa María', 'Providencia 567, Providencia', 'MONTE_TABOR', NOW()),
('Valentina', 'Morales', 'Castro', '31000003-9', '2020-11-08', 'Prekinder', 'Jardín Los Patitos', 'Ñuñoa 890, Ñuñoa', 'MONTE_TABOR', NOW()),
('Benjamín', 'Silva', 'Torres', '31000004-7', '2020-05-12', 'Prekinder', NULL, 'La Reina 2345, La Reina', 'MONTE_TABOR', NOW()),

-- Estudiantes Kinder (5-6 años)
('Sofía', 'Hernández', 'Pérez', '31000005-5', '2019-02-14', 'Kinder', 'Jardín Los Rosales', 'Vitacura 3456, Vitacura', 'NAZARET', NOW()),
('Diego', 'Castro', 'Morales', '31000006-3', '2019-06-30', 'Kinder', 'Jardín San Francisco', 'Peñalolén 4567, Peñalolén', 'NAZARET', NOW()),
('Isidora', 'Vargas', 'Silva', '31000007-1', '2019-09-18', 'Kinder', 'Jardín María Montessori', 'La Florida 5678, La Florida', 'NAZARET', NOW()),
('Agustín', 'Muñoz', 'González', '31000008-K', '2019-12-03', 'Kinder', 'Jardín Los Pequitos', 'Maipú 6789, Maipú', 'NAZARET', NOW()),

-- Estudiantes Básica (6-14 años)
('Martina', 'Torres', 'Hernández', '31000009-8', '2018-01-20', '1° Básico', 'Colegio San José', 'San Miguel 7890, San Miguel', 'MONTE_TABOR', NOW()),
('Lucas', 'Pérez', 'Castro', '31000010-1', '2017-04-25', '2° Básico', 'Escuela Los Pinos', 'Quilicura 8901, Quilicura', 'MONTE_TABOR', NOW()),
('Antonella', 'López', 'Vargas', '31000011-K', '2016-08-10', '3° Básico', 'Colegio Santa Teresa', 'Puente Alto 9012, Puente Alto', 'NAZARET', NOW()),
('Tomás', 'García', 'Muñoz', '31000012-8', '2015-11-15', '4° Básico', 'Escuela República', 'Renca 0123, Renca', 'MONTE_TABOR', NOW()),
('Florencia', 'Martínez', 'Torres', '31000013-6', '2014-03-08', '5° Básico', 'Colegio Los Andes', 'Cerro Navia 1234, Cerro Navia', 'NAZARET', NOW()),
('Sebastián', 'Sánchez', 'Pérez', '31000014-4', '2013-07-12', '6° Básico', 'Escuela El Bosque', 'Estación Central 2345, Estación Central', 'MONTE_TABOR', NOW()),
('Catalina', 'Ramos', 'López', '31000015-2', '2012-10-22', '7° Básico', 'Colegio San Pablo', 'Pedro Aguirre Cerda 3456, Pedro Aguirre Cerda', 'NAZARET', NOW()),
('Maximiliano', 'Flores', 'García', '31000016-0', '2011-12-30', '8° Básico', 'Escuela Gabriela Mistral', 'Lo Espejo 4567, Lo Espejo', 'MONTE_TABOR', NOW()),

-- Estudiantes Media (14-18 años)
('Javiera', 'Contreras', 'Martínez', '31000017-9', '2010-02-18', 'I° Medio', 'Liceo Pablo Neruda', 'Independencia 5678, Independencia', 'NAZARET', NOW()),
('Nicolás', 'Parra', 'Sánchez', '31000018-7', '2009-05-14', 'II° Medio', 'Colegio Industrial', 'Recoleta 6789, Recoleta', 'MONTE_TABOR', NOW()),
('Fernanda', 'Aguilar', 'Ramos', '31000019-5', '2008-09-26', 'III° Medio', 'Liceo de Niñas N°1', 'Conchalí 7890, Conchalí', 'NAZARET', NOW()),
('Francisco', 'Fuentes', 'Flores', '31000020-9', '2007-11-11', 'IV° Medio', 'Liceo Comercial', 'Huechuraba 8901, Huechuraba', 'MONTE_TABOR', NOW());

-- Crear padres para cada familia (40 padres total - 2 por familia)
INSERT INTO parents (full_name, rut, email, phone, profession, address, parent_type, created_at) VALUES
-- Familia 1 (González) - RUTs 32000001-32000002
('Carlos González Hernández', '32000001-3', 'carlos.gonzalez.test@gmail.com', '+56921111111', 'Ingeniero Civil', 'Las Condes 1234, Las Condes', 'FATHER', NOW()),
('María José González Silva', '32000002-1', 'maria.gonzalez.test@test.cl', '+56911111111', 'Profesora', 'Las Condes 1234, Las Condes', 'MOTHER', NOW()),

-- Familia 2 (Rodríguez) - RUTs 32000003-32000004
('Pedro Rodríguez Morales', '32000003-K', 'pedro.rodriguez.test@gmail.com', '+56921111112', 'Contador', 'Providencia 567, Providencia', 'FATHER', NOW()),
('Carmen Rodríguez López', '32000004-8', 'carmen.rodriguez.test@test.cl', '+56911111112', 'Enfermera', 'Providencia 567, Providencia', 'MOTHER', NOW()),

-- Familia 3 (Morales) - RUTs 32000005-32000006
('Luis Morales Pérez', '32000005-6', 'luis.morales.test@gmail.com', '+56921111113', 'Médico', 'Ñuñoa 890, Ñuñoa', 'FATHER', NOW()),
('Patricia Morales Castro', '32000006-4', 'patricia.morales.test@test.cl', '+56911111113', 'Psicóloga', 'Ñuñoa 890, Ñuñoa', 'MOTHER', NOW()),

-- Familia 4 (Silva) - RUTs 32000007-32000008
('Roberto Silva Vargas', '32000007-2', 'roberto.silva.test@gmail.com', '+56921111114', 'Arquitecto', 'La Reina 2345, La Reina', 'FATHER', NOW()),
('Andrea Silva Torres', '32000008-0', 'andrea.silva.test@test.cl', '+56911111114', 'Diseñadora', 'La Reina 2345, La Reina', 'MOTHER', NOW()),

-- Familia 5 (Hernández) - RUTs 32000009-32000010
('Miguel Hernández Silva', '32000009-9', 'miguel.hernandez.test@gmail.com', '+56921111115', 'Abogado', 'Vitacura 3456, Vitacura', 'FATHER', NOW()),
('Claudia Hernández Pérez', '32000010-2', 'claudia.hernandez.test@test.cl', '+56911111115', 'Kinesióloga', 'Vitacura 3456, Vitacura', 'MOTHER', NOW()),

-- Familia 6 (Castro) - RUTs 32000011-32000012
('Jorge Castro Torres', '32000011-0', 'jorge.castro.test@gmail.com', '+56921111116', 'Veterinario', 'Peñalolén 4567, Peñalolén', 'FATHER', NOW()),
('Valeria Castro Morales', '32000012-9', 'valeria.castro.test@test.cl', '+56911111116', 'Nutricionista', 'Peñalolén 4567, Peñalolén', 'MOTHER', NOW()),

-- Familia 7 (Vargas) - RUTs 32000013-32000014
('Andrés Vargas González', '32000013-7', 'andres.vargas.test@gmail.com', '+56921111117', 'Periodista', 'La Florida 5678, La Florida', 'FATHER', NOW()),
('Mónica Vargas Silva', '32000014-5', 'monica.vargas.test@test.cl', '+56911111117', 'Traductora', 'La Florida 5678, La Florida', 'MOTHER', NOW()),

-- Familia 8 (Muñoz) - RUTs 32000015-32000016
('Felipe Muñoz Hernández', '32000015-3', 'felipe.munoz.test@gmail.com', '+56921111118', 'Dentista', 'Maipú 6789, Maipú', 'FATHER', NOW()),
('Francisca Muñoz González', '32000016-1', 'francisca.munoz.test@test.cl', '+56911111118', 'Fonoaudióloga', 'Maipú 6789, Maipú', 'MOTHER', NOW()),

-- Familia 9 (Torres) - RUTs 32000017-32000018
('Patricio Torres Castro', '32000017-K', 'patricio.torres.test@gmail.com', '+56921111119', 'Electricista', 'San Miguel 7890, San Miguel', 'FATHER', NOW()),
('Soledad Torres Hernández', '32000018-8', 'soledad.torres.test@test.cl', '+56911111119', 'Secretaria', 'San Miguel 7890, San Miguel', 'MOTHER', NOW()),

-- Familia 10 (Pérez) - RUTs 32000019-32000020
('Rodrigo Pérez Vargas', '32000019-6', 'rodrigo.perez.test@gmail.com', '+56921111120', 'Mecánico', 'Quilicura 8901, Quilicura', 'FATHER', NOW()),
('Alejandra Pérez Castro', '32000020-K', 'alejandra.perez.test@test.cl', '+56911111120', 'Técnico en Párvulos', 'Quilicura 8901, Quilicura', 'MOTHER', NOW()),

-- Familia 11 (López) - RUTs 32000021-32000022
('Cristián López Muñoz', '32000021-8', 'cristian.lopez.test@gmail.com', '+56921111121', 'Profesor', 'Puente Alto 9012, Puente Alto', 'FATHER', NOW()),
('Lorena López Vargas', '32000022-6', 'lorena.lopez.test@test.cl', '+56911111121', 'Administradora', 'Puente Alto 9012, Puente Alto', 'MOTHER', NOW()),

-- Familia 12 (García) - RUTs 32000023-32000024
('Gonzalo García Torres', '32000023-4', 'gonzalo.garcia.test@gmail.com', '+56921111122', 'Soldador', 'Renca 0123, Renca', 'FATHER', NOW()),
('Paola García Muñoz', '32000024-2', 'paola.garcia.test@test.cl', '+56911111122', 'Cajera', 'Renca 0123, Renca', 'MOTHER', NOW()),

-- Familia 13 (Martínez) - RUTs 32000025-32000026
('Fernando Martínez González', '32000025-0', 'fernando.martinez.test@gmail.com', '+56921111123', 'Carpintero', 'Cerro Navia 1234, Cerro Navia', 'FATHER', NOW()),
('Verónica Martínez Torres', '32000026-9', 'veronica.martinez.test@test.cl', '+56911111123', 'Auxiliar de Enfermería', 'Cerro Navia 1234, Cerro Navia', 'MOTHER', NOW()),

-- Familia 14 (Sánchez) - RUTs 32000027-32000028
('Mauricio Sánchez Hernández', '32000027-7', 'mauricio.sanchez.test@gmail.com', '+56921111124', 'Guardia', 'Estación Central 2345, Estación Central', 'FATHER', NOW()),
('Carolina Sánchez Pérez', '32000028-5', 'carolina.sanchez.test@test.cl', '+56911111124', 'Vendedora', 'Estación Central 2345, Estación Central', 'MOTHER', NOW()),

-- Familia 15 (Ramos) - RUTs 32000029-32000030
('Osvaldo Ramos Castro', '32000029-3', 'osvaldo.ramos.test@gmail.com', '+56921111125', 'Conductor', 'Pedro Aguirre Cerda 3456, Pedro Aguirre Cerda', 'FATHER', NOW()),
('Daniela Ramos López', '32000030-7', 'daniela.ramos.test@test.cl', '+56911111125', 'Operaria', 'Pedro Aguirre Cerda 3456, Pedro Aguirre Cerda', 'MOTHER', NOW()),

-- Familia 16 (Flores) - RUTs 32000031-32000032
('Héctor Flores Vargas', '32000031-5', 'hector.flores.test@gmail.com', '+56921111126', 'Pintor', 'Lo Espejo 4567, Lo Espejo', 'FATHER', NOW()),
('Marcela Flores García', '32000032-3', 'marcela.flores.test@test.cl', '+56911111126', 'Asesora del Hogar', 'Lo Espejo 4567, Lo Espejo', 'MOTHER', NOW()),

-- Familia 17 (Contreras) - RUTs 32000033-32000034
('Ricardo Contreras Muñoz', '32000033-1', 'ricardo.contreras.test@gmail.com', '+56921111127', 'Técnico', 'Independencia 5678, Independencia', 'FATHER', NOW()),
('Gladys Contreras Martínez', '32000034-K', 'gladys.contreras.test@test.cl', '+56911111127', 'Costurera', 'Independencia 5678, Independencia', 'MOTHER', NOW()),

-- Familia 18 (Parra) - RUTs 32000035-32000036
('Enrique Parra Torres', '32000035-8', 'enrique.parra.test@gmail.com', '+56921111128', 'Gásfiter', 'Recoleta 6789, Recoleta', 'FATHER', NOW()),
('Cecilia Parra Sánchez', '32000036-6', 'cecilia.parra.test@test.cl', '+56911111128', 'Peluquera', 'Recoleta 6789, Recoleta', 'MOTHER', NOW()),

-- Familia 19 (Aguilar) - RUTs 32000037-32000038
('Ramón Aguilar González', '32000037-4', 'ramon.aguilar.test@gmail.com', '+56921111129', 'Conserje', 'Conchalí 7890, Conchalí', 'FATHER', NOW()),
('Roxana Aguilar Ramos', '32000038-2', 'roxana.aguilar.test@test.cl', '+56911111129', 'Manipuladora de Alimentos', 'Conchalí 7890, Conchalí', 'MOTHER', NOW()),

-- Familia 20 (Fuentes) - RUTs 32000039-32000040
('Iván Fuentes Hernández', '32000039-0', 'ivan.fuentes.test@gmail.com', '+56921111130', 'Bodeguero', 'Huechuraba 8901, Huechuraba', 'FATHER', NOW()),
('Ingrid Fuentes Flores', '32000040-4', 'ingrid.fuentes.test@test.cl', '+56911111130', 'Auxiliar de Aseo', 'Huechuraba 8901, Huechuraba', 'MOTHER', NOW());

-- Crear sostenedores con relaciones correctas (usar PADRE_MADRE en lugar de 'Padre')
INSERT INTO supporters (full_name, rut, email, phone, relationship, created_at) VALUES
('Carlos González Hernández', '32000001-3', 'carlos.gonzalez.test@gmail.com', '+56921111111', 'PADRE_MADRE', NOW()),
('Pedro Rodríguez Morales', '32000003-K', 'pedro.rodriguez.test@gmail.com', '+56921111112', 'PADRE_MADRE', NOW()),
('Luis Morales Pérez', '32000005-6', 'luis.morales.test@gmail.com', '+56921111113', 'PADRE_MADRE', NOW()),
('Roberto Silva Vargas', '32000007-2', 'roberto.silva.test@gmail.com', '+56921111114', 'PADRE_MADRE', NOW()),
('Miguel Hernández Silva', '32000009-9', 'miguel.hernandez.test@gmail.com', '+56921111115', 'PADRE_MADRE', NOW()),
('Jorge Castro Torres', '32000011-0', 'jorge.castro.test@gmail.com', '+56921111116', 'PADRE_MADRE', NOW()),
('Andrés Vargas González', '32000013-7', 'andres.vargas.test@gmail.com', '+56921111117', 'PADRE_MADRE', NOW()),
('Felipe Muñoz Hernández', '32000015-3', 'felipe.munoz.test@gmail.com', '+56921111118', 'PADRE_MADRE', NOW()),
('Patricio Torres Castro', '32000017-K', 'patricio.torres.test@gmail.com', '+56921111119', 'PADRE_MADRE', NOW()),
('Rodrigo Pérez Vargas', '32000019-6', 'rodrigo.perez.test@gmail.com', '+56921111120', 'PADRE_MADRE', NOW()),
('Cristián López Muñoz', '32000021-8', 'cristian.lopez.test@gmail.com', '+56921111121', 'PADRE_MADRE', NOW()),
('Gonzalo García Torres', '32000023-4', 'gonzalo.garcia.test@gmail.com', '+56921111122', 'PADRE_MADRE', NOW()),
('Fernando Martínez González', '32000025-0', 'fernando.martinez.test@gmail.com', '+56921111123', 'PADRE_MADRE', NOW()),
('Mauricio Sánchez Hernández', '32000027-7', 'mauricio.sanchez.test@gmail.com', '+56921111124', 'PADRE_MADRE', NOW()),
('Osvaldo Ramos Castro', '32000029-3', 'osvaldo.ramos.test@gmail.com', '+56921111125', 'PADRE_MADRE', NOW()),
('Héctor Flores Vargas', '32000031-5', 'hector.flores.test@gmail.com', '+56921111126', 'PADRE_MADRE', NOW()),
('Ricardo Contreras Muñoz', '32000033-1', 'ricardo.contreras.test@gmail.com', '+56921111127', 'PADRE_MADRE', NOW()),
('Enrique Parra Torres', '32000035-8', 'enrique.parra.test@gmail.com', '+56921111128', 'PADRE_MADRE', NOW()),
('Ramón Aguilar González', '32000037-4', 'ramon.aguilar.test@gmail.com', '+56921111129', 'PADRE_MADRE', NOW()),
('Iván Fuentes Hernández', '32000039-0', 'ivan.fuentes.test@gmail.com', '+56921111130', 'PADRE_MADRE', NOW());

-- Crear apoderados con relaciones correctas (usar PADRE_MADRE en lugar de 'Madre')
INSERT INTO guardians (full_name, rut, email, phone, relationship, created_at) VALUES
('María José González Silva', '32000002-1', 'maria.gonzalez.test@test.cl', '+56911111111', 'PADRE_MADRE', NOW()),
('Carmen Rodríguez López', '32000004-8', 'carmen.rodriguez.test@test.cl', '+56911111112', 'PADRE_MADRE', NOW()),
('Patricia Morales Castro', '32000006-4', 'patricia.morales.test@test.cl', '+56911111113', 'PADRE_MADRE', NOW()),
('Andrea Silva Torres', '32000008-0', 'andrea.silva.test@test.cl', '+56911111114', 'PADRE_MADRE', NOW()),
('Claudia Hernández Pérez', '32000010-2', 'claudia.hernandez.test@test.cl', '+56911111115', 'PADRE_MADRE', NOW()),
('Valeria Castro Morales', '32000012-9', 'valeria.castro.test@test.cl', '+56911111116', 'PADRE_MADRE', NOW()),
('Mónica Vargas Silva', '32000014-5', 'monica.vargas.test@test.cl', '+56911111117', 'PADRE_MADRE', NOW()),
('Francisca Muñoz González', '32000016-1', 'francisca.munoz.test@test.cl', '+56911111118', 'PADRE_MADRE', NOW()),
('Soledad Torres Hernández', '32000018-8', 'soledad.torres.test@test.cl', '+56911111119', 'PADRE_MADRE', NOW()),
('Alejandra Pérez Castro', '32000020-K', 'alejandra.perez.test@test.cl', '+56911111120', 'PADRE_MADRE', NOW()),
('Lorena López Vargas', '32000022-6', 'lorena.lopez.test@test.cl', '+56911111121', 'PADRE_MADRE', NOW()),
('Paola García Muñoz', '32000024-2', 'paola.garcia.test@test.cl', '+56911111122', 'PADRE_MADRE', NOW()),
('Verónica Martínez Torres', '32000026-9', 'veronica.martinez.test@test.cl', '+56911111123', 'PADRE_MADRE', NOW()),
('Carolina Sánchez Pérez', '32000028-5', 'carolina.sanchez.test@test.cl', '+56911111124', 'PADRE_MADRE', NOW()),
('Daniela Ramos López', '32000030-7', 'daniela.ramos.test@test.cl', '+56911111125', 'PADRE_MADRE', NOW()),
('Marcela Flores García', '32000032-3', 'marcela.flores.test@test.cl', '+56911111126', 'PADRE_MADRE', NOW()),
('Gladys Contreras Martínez', '32000034-K', 'gladys.contreras.test@test.cl', '+56911111127', 'PADRE_MADRE', NOW()),
('Cecilia Parra Sánchez', '32000036-6', 'cecilia.parra.test@test.cl', '+56911111128', 'PADRE_MADRE', NOW()),
('Roxana Aguilar Ramos', '32000038-2', 'roxana.aguilar.test@test.cl', '+56911111129', 'PADRE_MADRE', NOW()),
('Ingrid Fuentes Flores', '32000040-4', 'ingrid.fuentes.test@test.cl', '+56911111130', 'PADRE_MADRE', NOW());

-- Crear las postulaciones con estados variados usando subqueries para obtener los IDs
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
) 
SELECT 
    s.id as student_id,
    p_father.id as father_id,
    p_mother.id as mother_id,
    sup.id as supporter_id,
    g.id as guardian_id,
    u.id as applicant_user_id,
    CASE 
        WHEN s.id % 8 = 1 THEN 'PENDING'
        WHEN s.id % 8 = 2 THEN 'UNDER_REVIEW'
        WHEN s.id % 8 = 3 THEN 'INTERVIEW_SCHEDULED'
        WHEN s.id % 8 = 4 THEN 'EXAM_SCHEDULED'
        WHEN s.id % 8 = 5 THEN 'APPROVED'
        WHEN s.id % 8 = 6 THEN 'REJECTED'
        WHEN s.id % 8 = 7 THEN 'WAITLIST'
        ELSE 'PENDING'
    END as status,
    NOW() - INTERVAL (s.id * 2 + 1) DAY as submission_date,
    NOW() as created_at
FROM students s
JOIN parents p_father ON p_father.parent_type = 'FATHER' AND p_father.rut = ('32000' || LPAD((s.id * 2 - 1)::text, 3, '0') || CASE WHEN s.id * 2 - 1 % 10 = 0 THEN '-0' WHEN s.id * 2 - 1 % 10 = 1 THEN '-3' WHEN s.id * 2 - 1 % 10 = 2 THEN '-1' WHEN s.id * 2 - 1 % 10 = 3 THEN '-K' WHEN s.id * 2 - 1 % 10 = 4 THEN '-8' WHEN s.id * 2 - 1 % 10 = 5 THEN '-6' WHEN s.id * 2 - 1 % 10 = 6 THEN '-4' WHEN s.id * 2 - 1 % 10 = 7 THEN '-2' WHEN s.id * 2 - 1 % 10 = 8 THEN '-0' ELSE '-9' END)
JOIN parents p_mother ON p_mother.parent_type = 'MOTHER' AND p_mother.rut = ('32000' || LPAD((s.id * 2)::text, 3, '0') || CASE WHEN s.id * 2 % 10 = 0 THEN '-K' WHEN s.id * 2 % 10 = 1 THEN '-8' WHEN s.id * 2 % 10 = 2 THEN '-6' WHEN s.id * 2 % 10 = 3 THEN '-4' WHEN s.id * 2 % 10 = 4 THEN '-2' WHEN s.id * 2 % 10 = 5 THEN '-0' WHEN s.id * 2 % 10 = 6 THEN '-9' WHEN s.id * 2 % 10 = 7 THEN '-7' WHEN s.id * 2 % 10 = 8 THEN '-5' ELSE '-3' END)
JOIN supporters sup ON sup.rut = p_father.rut
JOIN guardians g ON g.rut = p_mother.rut
JOIN users u ON u.email = p_mother.email
WHERE s.rut LIKE '31000%'
ORDER BY s.id;

-- Mostrar resumen de lo creado
SELECT 
    'RESUMEN DE DATOS CREADOS' as descripcion,
    (SELECT COUNT(*) FROM users WHERE role = 'APODERADO' AND email LIKE '%@test.cl') as apoderados_creados,
    (SELECT COUNT(*) FROM students WHERE rut LIKE '31000%') as estudiantes_creados,
    (SELECT COUNT(*) FROM applications WHERE created_at > NOW() - INTERVAL '1 hour') as postulaciones_creadas;

-- Mostrar distribución por nivel
SELECT 
    s.grade_applied as nivel,
    COUNT(*) as cantidad_postulaciones,
    STRING_AGG(DISTINCT a.status, ', ') as estados_presentes
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