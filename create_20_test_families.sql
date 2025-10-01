-- Script para crear 20 familias de prueba con postulaciones distribuidas en todos los niveles educativos
-- IMPORTANTE: No tocar la autenticación - usar solo para datos de prueba

-- Primero crear 20 usuarios apoderados de prueba
INSERT INTO users (first_name, last_name, email, password, rut, phone, role, educational_level, subject, email_verified, active, created_at) VALUES
-- Apoderados para Prekinder (4 familias)
('María José', 'González', 'maria.gonzalez@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '12345678-9', '+56987654321', 'APODERADO', NULL, NULL, true, true, NOW()),
('Carmen', 'Rodríguez', 'carmen.rodriguez@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '12345679-7', '+56987654322', 'APODERADO', NULL, NULL, true, true, NOW()),
('Patricia', 'Morales', 'patricia.morales@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '12345680-0', '+56987654323', 'APODERADO', NULL, NULL, true, true, NOW()),
('Andrea', 'Silva', 'andrea.silva@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '12345681-9', '+56987654324', 'APODERADO', NULL, NULL, true, true, NOW()),

-- Apoderados para Kinder (4 familias)
('Claudia', 'Hernández', 'claudia.hernandez@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '12345682-7', '+56987654325', 'APODERADO', NULL, NULL, true, true, NOW()),
('Valeria', 'Castro', 'valeria.castro@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '12345683-5', '+56987654326', 'APODERADO', NULL, NULL, true, true, NOW()),
('Mónica', 'Vargas', 'monica.vargas@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '12345684-3', '+56987654327', 'APODERADO', NULL, NULL, true, true, NOW()),
('Francisca', 'Muñoz', 'francisca.munoz@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '12345685-1', '+56987654328', 'APODERADO', NULL, NULL, true, true, NOW()),

-- Apoderados para Básica (8 familias)
('Soledad', 'Torres', 'soledad.torres@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '12345686-K', '+56987654329', 'APODERADO', NULL, NULL, true, true, NOW()),
('Alejandra', 'Pérez', 'alejandra.perez@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '12345687-8', '+56987654330', 'APODERADO', NULL, NULL, true, true, NOW()),
('Lorena', 'López', 'lorena.lopez@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '12345688-6', '+56987654331', 'APODERADO', NULL, NULL, true, true, NOW()),
('Paola', 'García', 'paola.garcia@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '12345689-4', '+56987654332', 'APODERADO', NULL, NULL, true, true, NOW()),
('Verónica', 'Martínez', 'veronica.martinez@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '12345690-8', '+56987654333', 'APODERADO', NULL, NULL, true, true, NOW()),
('Carolina', 'Sánchez', 'carolina.sanchez@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '12345691-6', '+56987654334', 'APODERADO', NULL, NULL, true, true, NOW()),
('Daniela', 'Ramos', 'daniela.ramos@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '12345692-4', '+56987654335', 'APODERADO', NULL, NULL, true, true, NOW()),
('Marcela', 'Flores', 'marcela.flores@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '12345693-2', '+56987654336', 'APODERADO', NULL, NULL, true, true, NOW()),

-- Apoderados para Media (4 familias)
('Gladys', 'Contreras', 'gladys.contreras@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '12345694-0', '+56987654337', 'APODERADO', NULL, NULL, true, true, NOW()),
('Cecilia', 'Parra', 'cecilia.parra@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '12345695-9', '+56987654338', 'APODERADO', NULL, NULL, true, true, NOW()),
('Roxana', 'Aguilar', 'roxana.aguilar@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '12345696-7', '+56987654339', 'APODERADO', NULL, NULL, true, true, NOW()),
('Ingrid', 'Fuentes', 'ingrid.fuentes@test.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '12345697-5', '+56987654340', 'APODERADO', NULL, NULL, true, true, NOW());

-- Crear estudiantes distribuidos por nivel educativo
INSERT INTO students (first_name, paternal_last_name, maternal_last_name, rut, birth_date, grade_applied, current_school, address, school_applied, created_at) VALUES
-- Estudiantes Prekinder (4-5 años)
('Emilia', 'González', 'Silva', '26000001-1', '2020-03-15', 'Prekinder', 'Jardín Los Angelitos', 'Las Condes 1234, Las Condes', 'MONTE_TABOR', NOW()),
('Matías', 'Rodríguez', 'López', '26000002-K', '2020-07-22', 'Prekinder', 'Jardín Santa María', 'Providencia 567, Providencia', 'MONTE_TABOR', NOW()),
('Valentina', 'Morales', 'Castro', '26000003-8', '2020-11-08', 'Prekinder', 'Jardín Los Patitos', 'Ñuñoa 890, Ñuñoa', 'MONTE_TABOR', NOW()),
('Benjamín', 'Silva', 'Torres', '26000004-6', '2020-05-12', 'Prekinder', NULL, 'La Reina 2345, La Reina', 'MONTE_TABOR', NOW()),

-- Estudiantes Kinder (5-6 años)
('Sofía', 'Hernández', 'Pérez', '25000001-4', '2019-02-14', 'Kinder', 'Jardín Los Rosales', 'Vitacura 3456, Vitacura', 'NAZARET', NOW()),
('Diego', 'Castro', 'Morales', '25000002-2', '2019-06-30', 'Kinder', 'Jardín San Francisco', 'Peñalolén 4567, Peñalolén', 'NAZARET', NOW()),
('Isidora', 'Vargas', 'Silva', '25000003-0', '2019-09-18', 'Kinder', 'Jardín María Montessori', 'La Florida 5678, La Florida', 'NAZARET', NOW()),
('Agustín', 'Muñoz', 'González', '25000004-9', '2019-12-03', 'Kinder', 'Jardín Los Pequitos', 'Maipú 6789, Maipú', 'NAZARET', NOW()),

-- Estudiantes Básica (6-14 años)
('Martina', 'Torres', 'Hernández', '24000001-7', '2018-01-20', '1° Básico', 'Colegio San José', 'San Miguel 7890, San Miguel', 'MONTE_TABOR', NOW()),
('Lucas', 'Pérez', 'Castro', '23000001-0', '2017-04-25', '2° Básico', 'Escuela Los Pinos', 'Quilicura 8901, Quilicura', 'MONTE_TABOR', NOW()),
('Antonella', 'López', 'Vargas', '22000001-3', '2016-08-10', '3° Básico', 'Colegio Santa Teresa', 'Puente Alto 9012, Puente Alto', 'NAZARET', NOW()),
('Tomás', 'García', 'Muñoz', '21000001-6', '2015-11-15', '4° Básico', 'Escuela República', 'Renca 0123, Renca', 'MONTE_TABOR', NOW()),
('Florencia', 'Martínez', 'Torres', '20000001-9', '2014-03-08', '5° Básico', 'Colegio Los Andes', 'Cerro Navia 1234, Cerro Navia', 'NAZARET', NOW()),
('Sebastián', 'Sánchez', 'Pérez', '19000001-2', '2013-07-12', '6° Básico', 'Escuela El Bosque', 'Estación Central 2345, Estación Central', 'MONTE_TABOR', NOW()),
('Catalina', 'Ramos', 'López', '18000001-5', '2012-10-22', '7° Básico', 'Colegio San Pablo', 'Pedro Aguirre Cerda 3456, Pedro Aguirre Cerda', 'NAZARET', NOW()),
('Maximiliano', 'Flores', 'García', '17000001-8', '2011-12-30', '8° Básico', 'Escuela Gabriela Mistral', 'Lo Espejo 4567, Lo Espejo', 'MONTE_TABOR', NOW()),

-- Estudiantes Media (14-18 años)
('Javiera', 'Contreras', 'Martínez', '16000001-1', '2010-02-18', 'I° Medio', 'Liceo Pablo Neruda', 'Independencia 5678, Independencia', 'NAZARET', NOW()),
('Nicolás', 'Parra', 'Sánchez', '15000001-4', '2009-05-14', 'II° Medio', 'Colegio Industrial', 'Recoleta 6789, Recoleta', 'MONTE_TABOR', NOW()),
('Fernanda', 'Aguilar', 'Ramos', '14000001-7', '2008-09-26', 'III° Medio', 'Liceo de Niñas N°1', 'Conchalí 7890, Conchalí', 'NAZARET', NOW()),
('Francisco', 'Fuentes', 'Flores', '13000001-0', '2007-11-11', 'IV° Medio', 'Liceo Comercial', 'Huechuraba 8901, Huechuraba', 'MONTE_TABOR', NOW());

-- Crear padres para cada familia
INSERT INTO parents (full_name, rut, email, phone, profession, address, parent_type, created_at) VALUES
-- Padres para familia 1 (González)
('Carlos González Hernández', '11111111-1', 'carlos.gonzalez@gmail.com', '+56987111111', 'Ingeniero Civil', 'Las Condes 1234, Las Condes', 'FATHER', NOW()),
('María José González Silva', '11111112-K', 'maria.gonzalez@test.cl', '+56987654321', 'Profesora', 'Las Condes 1234, Las Condes', 'MOTHER', NOW()),

-- Padres para familia 2 (Rodríguez)
('Pedro Rodríguez Morales', '11111113-8', 'pedro.rodriguez@gmail.com', '+56987111112', 'Contador', 'Providencia 567, Providencia', 'FATHER', NOW()),
('Carmen Rodríguez López', '11111114-6', 'carmen.rodriguez@test.cl', '+56987654322', 'Enfermera', 'Providencia 567, Providencia', 'MOTHER', NOW()),

-- Padres para familia 3 (Morales)
('Luis Morales Pérez', '11111115-4', 'luis.morales@gmail.com', '+56987111113', 'Médico', 'Ñuñoa 890, Ñuñoa', 'FATHER', NOW()),
('Patricia Morales Castro', '11111116-2', 'patricia.morales@test.cl', '+56987654323', 'Psicóloga', 'Ñuñoa 890, Ñuñoa', 'MOTHER', NOW()),

-- Padres para familia 4 (Silva)
('Roberto Silva Vargas', '11111117-0', 'roberto.silva@gmail.com', '+56987111114', 'Arquitecto', 'La Reina 2345, La Reina', 'FATHER', NOW()),
('Andrea Silva Torres', '11111118-9', 'andrea.silva@test.cl', '+56987654324', 'Diseñadora', 'La Reina 2345, La Reina', 'MOTHER', NOW()),

-- Padres para familia 5 (Hernández)
('Miguel Hernández Silva', '11111119-7', 'miguel.hernandez@gmail.com', '+56987111115', 'Abogado', 'Vitacura 3456, Vitacura', 'FATHER', NOW()),
('Claudia Hernández Pérez', '11111120-0', 'claudia.hernandez@test.cl', '+56987654325', 'Kinesióloga', 'Vitacura 3456, Vitacura', 'MOTHER', NOW()),

-- Padres para familia 6 (Castro)
('Jorge Castro Torres', '11111121-9', 'jorge.castro@gmail.com', '+56987111116', 'Veterinario', 'Peñalolén 4567, Peñalolén', 'FATHER', NOW()),
('Valeria Castro Morales', '11111122-7', 'valeria.castro@test.cl', '+56987654326', 'Nutricionista', 'Peñalolén 4567, Peñalolén', 'MOTHER', NOW()),

-- Padres para familia 7 (Vargas)
('Andrés Vargas González', '11111123-5', 'andres.vargas@gmail.com', '+56987111117', 'Periodista', 'La Florida 5678, La Florida', 'FATHER', NOW()),
('Mónica Vargas Silva', '11111124-3', 'monica.vargas@test.cl', '+56987654327', 'Traductora', 'La Florida 5678, La Florida', 'MOTHER', NOW()),

-- Padres para familia 8 (Muñoz)
('Felipe Muñoz Hernández', '11111125-1', 'felipe.munoz@gmail.com', '+56987111118', 'Dentista', 'Maipú 6789, Maipú', 'FATHER', NOW()),
('Francisca Muñoz González', '11111126-K', 'francisca.munoz@test.cl', '+56987654328', 'Fonoaudióloga', 'Maipú 6789, Maipú', 'MOTHER', NOW()),

-- Padres para familia 9 (Torres)
('Patricio Torres Castro', '11111127-8', 'patricio.torres@gmail.com', '+56987111119', 'Electricista', 'San Miguel 7890, San Miguel', 'FATHER', NOW()),
('Soledad Torres Hernández', '11111128-6', 'soledad.torres@test.cl', '+56987654329', 'Secretaria', 'San Miguel 7890, San Miguel', 'MOTHER', NOW()),

-- Padres para familia 10 (Pérez)
('Rodrigo Pérez Vargas', '11111129-4', 'rodrigo.perez@gmail.com', '+56987111120', 'Mecánico', 'Quilicura 8901, Quilicura', 'FATHER', NOW()),
('Alejandra Pérez Castro', '11111130-8', 'alejandra.perez@test.cl', '+56987654330', 'Técnico en Párvulos', 'Quilicura 8901, Quilicura', 'MOTHER', NOW()),

-- Padres para familia 11 (López)
('Cristián López Muñoz', '11111131-6', 'cristian.lopez@gmail.com', '+56987111121', 'Profesor', 'Puente Alto 9012, Puente Alto', 'FATHER', NOW()),
('Lorena López Vargas', '11111132-4', 'lorena.lopez@test.cl', '+56987654331', 'Administradora', 'Puente Alto 9012, Puente Alto', 'MOTHER', NOW()),

-- Padres para familia 12 (García)
('Gonzalo García Torres', '11111133-2', 'gonzalo.garcia@gmail.com', '+56987111122', 'Soldador', 'Renca 0123, Renca', 'FATHER', NOW()),
('Paola García Muñoz', '11111134-0', 'paola.garcia@test.cl', '+56987654332', 'Cajera', 'Renca 0123, Renca', 'MOTHER', NOW()),

-- Padres para familia 13 (Martínez)
('Fernando Martínez González', '11111135-9', 'fernando.martinez@gmail.com', '+56987111123', 'Carpintero', 'Cerro Navia 1234, Cerro Navia', 'FATHER', NOW()),
('Verónica Martínez Torres', '11111136-7', 'veronica.martinez@test.cl', '+56987654333', 'Auxiliar de Enfermería', 'Cerro Navia 1234, Cerro Navia', 'MOTHER', NOW()),

-- Padres para familia 14 (Sánchez)
('Mauricio Sánchez Hernández', '11111137-5', 'mauricio.sanchez@gmail.com', '+56987111124', 'Guardia', 'Estación Central 2345, Estación Central', 'FATHER', NOW()),
('Carolina Sánchez Pérez', '11111138-3', 'carolina.sanchez@test.cl', '+56987654334', 'Vendedora', 'Estación Central 2345, Estación Central', 'MOTHER', NOW()),

-- Padres para familia 15 (Ramos)
('Osvaldo Ramos Castro', '11111139-1', 'osvaldo.ramos@gmail.com', '+56987111125', 'Conductor', 'Pedro Aguirre Cerda 3456, Pedro Aguirre Cerda', 'FATHER', NOW()),
('Daniela Ramos López', '11111140-5', 'daniela.ramos@test.cl', '+56987654335', 'Operaria', 'Pedro Aguirre Cerda 3456, Pedro Aguirre Cerda', 'MOTHER', NOW()),

-- Padres para familia 16 (Flores)
('Héctor Flores Vargas', '11111141-3', 'hector.flores@gmail.com', '+56987111126', 'Pintor', 'Lo Espejo 4567, Lo Espejo', 'FATHER', NOW()),
('Marcela Flores García', '11111142-1', 'marcela.flores@test.cl', '+56987654336', 'Asesora del Hogar', 'Lo Espejo 4567, Lo Espejo', 'MOTHER', NOW()),

-- Padres para familia 17 (Contreras)
('Ricardo Contreras Muñoz', '11111143-K', 'ricardo.contreras@gmail.com', '+56987111127', 'Técnico', 'Independencia 5678, Independencia', 'FATHER', NOW()),
('Gladys Contreras Martínez', '11111144-8', 'gladys.contreras@test.cl', '+56987654337', 'Costurera', 'Independencia 5678, Independencia', 'MOTHER', NOW()),

-- Padres para familia 18 (Parra)
('Enrique Parra Torres', '11111145-6', 'enrique.parra@gmail.com', '+56987111128', 'Gásfiter', 'Recoleta 6789, Recoleta', 'FATHER', NOW()),
('Cecilia Parra Sánchez', '11111146-4', 'cecilia.parra@test.cl', '+56987654338', 'Peluquera', 'Recoleta 6789, Recoleta', 'MOTHER', NOW()),

-- Padres para familia 19 (Aguilar)
('Ramón Aguilar González', '11111147-2', 'ramon.aguilar@gmail.com', '+56987111129', 'Conserje', 'Conchalí 7890, Conchalí', 'FATHER', NOW()),
('Roxana Aguilar Ramos', '11111148-0', 'roxana.aguilar@test.cl', '+56987654339', 'Manipuladora de Alimentos', 'Conchalí 7890, Conchalí', 'MOTHER', NOW()),

-- Padres para familia 20 (Fuentes)
('Iván Fuentes Hernández', '11111149-9', 'ivan.fuentes@gmail.com', '+56987111130', 'Bodeguero', 'Huechuraba 8901, Huechuraba', 'FATHER', NOW()),
('Ingrid Fuentes Flores', '11111150-2', 'ingrid.fuentes@test.cl', '+56987654340', 'Auxiliar de Aseo', 'Huechuraba 8901, Huechuraba', 'MOTHER', NOW());

-- Crear sostenedores (generalmente uno de los padres)
INSERT INTO supporters (full_name, rut, email, phone, relationship, created_at) VALUES
('Carlos González Hernández', '11111111-1', 'carlos.gonzalez@gmail.com', '+56987111111', 'Padre', NOW()),
('Pedro Rodríguez Morales', '11111113-8', 'pedro.rodriguez@gmail.com', '+56987111112', 'Padre', NOW()),
('Luis Morales Pérez', '11111115-4', 'luis.morales@gmail.com', '+56987111113', 'Padre', NOW()),
('Roberto Silva Vargas', '11111117-0', 'roberto.silva@gmail.com', '+56987111114', 'Padre', NOW()),
('Miguel Hernández Silva', '11111119-7', 'miguel.hernandez@gmail.com', '+56987111115', 'Padre', NOW()),
('Jorge Castro Torres', '11111121-9', 'jorge.castro@gmail.com', '+56987111116', 'Padre', NOW()),
('Andrés Vargas González', '11111123-5', 'andres.vargas@gmail.com', '+56987111117', 'Padre', NOW()),
('Felipe Muñoz Hernández', '11111125-1', 'felipe.munoz@gmail.com', '+56987111118', 'Padre', NOW()),
('Patricio Torres Castro', '11111127-8', 'patricio.torres@gmail.com', '+56987111119', 'Padre', NOW()),
('Rodrigo Pérez Vargas', '11111129-4', 'rodrigo.perez@gmail.com', '+56987111120', 'Padre', NOW()),
('Cristián López Muñoz', '11111131-6', 'cristian.lopez@gmail.com', '+56987111121', 'Padre', NOW()),
('Gonzalo García Torres', '11111133-2', 'gonzalo.garcia@gmail.com', '+56987111122', 'Padre', NOW()),
('Fernando Martínez González', '11111135-9', 'fernando.martinez@gmail.com', '+56987111123', 'Padre', NOW()),
('Mauricio Sánchez Hernández', '11111137-5', 'mauricio.sanchez@gmail.com', '+56987111124', 'Padre', NOW()),
('Osvaldo Ramos Castro', '11111139-1', 'osvaldo.ramos@gmail.com', '+56987111125', 'Padre', NOW()),
('Héctor Flores Vargas', '11111141-3', 'hector.flores@gmail.com', '+56987111126', 'Padre', NOW()),
('Ricardo Contreras Muñoz', '11111143-K', 'ricardo.contreras@gmail.com', '+56987111127', 'Padre', NOW()),
('Enrique Parra Torres', '11111145-6', 'enrique.parra@gmail.com', '+56987111128', 'Padre', NOW()),
('Ramón Aguilar González', '11111147-2', 'ramon.aguilar@gmail.com', '+56987111129', 'Padre', NOW()),
('Iván Fuentes Hernández', '11111149-9', 'ivan.fuentes@gmail.com', '+56987111130', 'Padre', NOW());

-- Crear apoderados (generalmente las madres)
INSERT INTO guardians (full_name, rut, email, phone, relationship, created_at) VALUES
('María José González Silva', '11111112-K', 'maria.gonzalez@test.cl', '+56987654321', 'Madre', NOW()),
('Carmen Rodríguez López', '11111114-6', 'carmen.rodriguez@test.cl', '+56987654322', 'Madre', NOW()),
('Patricia Morales Castro', '11111116-2', 'patricia.morales@test.cl', '+56987654323', 'Madre', NOW()),
('Andrea Silva Torres', '11111118-9', 'andrea.silva@test.cl', '+56987654324', 'Madre', NOW()),
('Claudia Hernández Pérez', '11111120-0', 'claudia.hernandez@test.cl', '+56987654325', 'Madre', NOW()),
('Valeria Castro Morales', '11111122-7', 'valeria.castro@test.cl', '+56987654326', 'Madre', NOW()),
('Mónica Vargas Silva', '11111124-3', 'monica.vargas@test.cl', '+56987654327', 'Madre', NOW()),
('Francisca Muñoz González', '11111126-K', 'francisca.munoz@test.cl', '+56987654328', 'Madre', NOW()),
('Soledad Torres Hernández', '11111128-6', 'soledad.torres@test.cl', '+56987654329', 'Madre', NOW()),
('Alejandra Pérez Castro', '11111130-8', 'alejandra.perez@test.cl', '+56987654330', 'Madre', NOW()),
('Lorena López Vargas', '11111132-4', 'lorena.lopez@test.cl', '+56987654331', 'Madre', NOW()),
('Paola García Muñoz', '11111134-0', 'paola.garcia@test.cl', '+56987654332', 'Madre', NOW()),
('Verónica Martínez Torres', '11111136-7', 'veronica.martinez@test.cl', '+56987654333', 'Madre', NOW()),
('Carolina Sánchez Pérez', '11111138-3', 'carolina.sanchez@test.cl', '+56987654334', 'Madre', NOW()),
('Daniela Ramos López', '11111140-5', 'daniela.ramos@test.cl', '+56987654335', 'Madre', NOW()),
('Marcela Flores García', '11111142-1', 'marcela.flores@test.cl', '+56987654336', 'Madre', NOW()),
('Gladys Contreras Martínez', '11111144-8', 'gladys.contreras@test.cl', '+56987654337', 'Madre', NOW()),
('Cecilia Parra Sánchez', '11111146-4', 'cecilia.parra@test.cl', '+56987654338', 'Madre', NOW()),
('Roxana Aguilar Ramos', '11111148-0', 'roxana.aguilar@test.cl', '+56987654339', 'Madre', NOW()),
('Ingrid Fuentes Flores', '11111150-2', 'ingrid.fuentes@test.cl', '+56987654340', 'Madre', NOW());

-- Crear las postulaciones con estados variados
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
) VALUES
-- Postulaciones con IDs secuenciales basados en el orden de creación
(1, 1, 2, 1, 1, (SELECT id FROM users WHERE email = 'maria.gonzalez@test.cl'), 'PENDING', NOW() - INTERVAL '5 days', NOW()),
(2, 3, 4, 2, 2, (SELECT id FROM users WHERE email = 'carmen.rodriguez@test.cl'), 'UNDER_REVIEW', NOW() - INTERVAL '8 days', NOW()),
(3, 5, 6, 3, 3, (SELECT id FROM users WHERE email = 'patricia.morales@test.cl'), 'INTERVIEW_SCHEDULED', NOW() - INTERVAL '12 days', NOW()),
(4, 7, 8, 4, 4, (SELECT id FROM users WHERE email = 'andrea.silva@test.cl'), 'PENDING', NOW() - INTERVAL '3 days', NOW()),
(5, 9, 10, 5, 5, (SELECT id FROM users WHERE email = 'claudia.hernandez@test.cl'), 'EXAM_SCHEDULED', NOW() - INTERVAL '15 days', NOW()),
(6, 11, 12, 6, 6, (SELECT id FROM users WHERE email = 'valeria.castro@test.cl'), 'APPROVED', NOW() - INTERVAL '20 days', NOW()),
(7, 13, 14, 7, 7, (SELECT id FROM users WHERE email = 'monica.vargas@test.cl'), 'UNDER_REVIEW', NOW() - INTERVAL '7 days', NOW()),
(8, 15, 16, 8, 8, (SELECT id FROM users WHERE email = 'francisca.munoz@test.cl'), 'WAITLIST', NOW() - INTERVAL '25 days', NOW()),
(9, 17, 18, 9, 9, (SELECT id FROM users WHERE email = 'soledad.torres@test.cl'), 'PENDING', NOW() - INTERVAL '2 days', NOW()),
(10, 19, 20, 10, 10, (SELECT id FROM users WHERE email = 'alejandra.perez@test.cl'), 'UNDER_REVIEW', NOW() - INTERVAL '9 days', NOW()),
(11, 21, 22, 11, 11, (SELECT id FROM users WHERE email = 'lorena.lopez@test.cl'), 'INTERVIEW_SCHEDULED', NOW() - INTERVAL '14 days', NOW()),
(12, 23, 24, 12, 12, (SELECT id FROM users WHERE email = 'paola.garcia@test.cl'), 'APPROVED', NOW() - INTERVAL '18 days', NOW()),
(13, 25, 26, 13, 13, (SELECT id FROM users WHERE email = 'veronica.martinez@test.cl'), 'REJECTED', NOW() - INTERVAL '22 days', NOW()),
(14, 27, 28, 14, 14, (SELECT id FROM users WHERE email = 'carolina.sanchez@test.cl'), 'PENDING', NOW() - INTERVAL '4 days', NOW()),
(15, 29, 30, 15, 15, (SELECT id FROM users WHERE email = 'daniela.ramos@test.cl'), 'UNDER_REVIEW', NOW() - INTERVAL '11 days', NOW()),
(16, 31, 32, 16, 16, (SELECT id FROM users WHERE email = 'marcela.flores@test.cl'), 'EXAM_SCHEDULED', NOW() - INTERVAL '16 days', NOW()),
(17, 33, 34, 17, 17, (SELECT id FROM users WHERE email = 'gladys.contreras@test.cl'), 'APPROVED', NOW() - INTERVAL '19 days', NOW()),
(18, 35, 36, 18, 18, (SELECT id FROM users WHERE email = 'cecilia.parra@test.cl'), 'PENDING', NOW() - INTERVAL '6 days', NOW()),
(19, 37, 38, 19, 19, (SELECT id FROM users WHERE email = 'roxana.aguilar@test.cl'), 'INTERVIEW_SCHEDULED', NOW() - INTERVAL '13 days', NOW()),
(20, 39, 40, 20, 20, (SELECT id FROM users WHERE email = 'ingrid.fuentes@test.cl'), 'UNDER_REVIEW', NOW() - INTERVAL '10 days', NOW());

-- Mostrar resumen de lo creado
SELECT 
    'RESUMEN DE DATOS CREADOS' as descripcion,
    (SELECT COUNT(*) FROM users WHERE role = 'APODERADO' AND email LIKE '%@test.cl') as apoderados_creados,
    (SELECT COUNT(*) FROM students WHERE rut LIKE '26000%' OR rut LIKE '25000%' OR rut LIKE '24000%' OR rut LIKE '23000%' OR rut LIKE '22000%' OR rut LIKE '21000%' OR rut LIKE '20000%' OR rut LIKE '19000%' OR rut LIKE '18000%' OR rut LIKE '17000%' OR rut LIKE '16000%' OR rut LIKE '15000%' OR rut LIKE '14000%' OR rut LIKE '13000%') as estudiantes_creados,
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