-- Crear datos mínimos para probar entrevistas

-- Padres con esquema correcto
INSERT INTO parents (full_name, parent_type, rut, email, phone, profession, address, created_at) VALUES
('Roberto Pérez Martínez', 'FATHER', '12001001-1', 'roberto.perez@email.com', '+56912001001', 'Ingeniero', 'Av. Principal 123', NOW()),
('Carmen González Vega', 'MOTHER', '12001002-K', 'carmen.gonzalez@email.com', '+56912001002', 'Profesora', 'Av. Principal 123', NOW()),
('Luis López Hernández', 'FATHER', '12002001-8', 'luis.lopez@email.com', '+56912002001', 'Médico', 'Calle Central 456', NOW()),
('Ana Silva Morales', 'MOTHER', '12002002-6', 'ana.silva@email.com', '+56912002002', 'Enfermera', 'Calle Central 456', NOW());

-- Supporters (sin address, con relaciones en español)
INSERT INTO supporters (full_name, relationship, rut, email, phone, created_at) VALUES
('Roberto Pérez Martínez', 'PADRE', '12001001-1', 'roberto.perez@email.com', '+56912001001', NOW()),
('Luis López Hernández', 'PADRE', '12002001-8', 'luis.lopez@email.com', '+56912002001', NOW());

-- Guardians (sin address)
INSERT INTO guardians (full_name, relationship, rut, email, phone, created_at) VALUES
('Carmen González Vega', 'MADRE', '12001002-K', 'carmen.gonzalez@email.com', '+56912001002', NOW()),
('Ana Silva Morales', 'MADRE', '12002002-6', 'ana.silva@email.com', '+56912002002', NOW());

-- Aplicaciones simples
INSERT INTO applications (student_id, father_id, mother_id, supporter_id, guardian_id, applicant_user_id, status, submission_date, created_at) VALUES
(1, 1, 2, 1, 1, 6, 'INTERVIEW_SCHEDULED', NOW() - INTERVAL '5 days', NOW()),
(2, 3, 4, 2, 2, 7, 'INTERVIEW_SCHEDULED', NOW() - INTERVAL '3 days', NOW());

-- ENTREVISTAS - LO MÁS IMPORTANTE
INSERT INTO interviews (application_id, interviewer_id, status, interview_type, scheduled_date, duration_minutes, location, notes, created_at) VALUES
(1, 2, 'SCHEDULED', 'INDIVIDUAL', '2025-09-02 13:30:00', 60, 'Oficina Principal', 'Entrevista familia Pérez', NOW()),
(2, 2, 'SCHEDULED', 'FAMILY', '2025-09-03 14:00:00', 90, 'Sala de Reuniones', 'Entrevista familia López', NOW());

SELECT 'Datos mínimos creados correctamente' as status;