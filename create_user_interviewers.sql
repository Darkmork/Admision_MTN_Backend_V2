-- Crear entrevistadores como usuarios en el sistema con roles apropiados
-- Estos aparecerán en la lista de entrevistadores del sistema

INSERT INTO users (first_name, last_name, email, password, rut, phone, role, educational_level, subject, email_verified, active, created_at) VALUES

-- ENTREVISTADORES ESPECIALIZADOS (usando roles apropiados del sistema)
('Dra. María', 'González Silva', 'maria.gonzalez.entrevistadora@mtn.cl', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '18000001-1', '+56911111111', 'PSYCHOLOGIST', 'ALL_LEVELS', 'ALL_SUBJECTS', true, true, NOW()),

('Dr. Carlos', 'Mendoza Torres', 'carlos.mendoza.entrevistador@mtn.cl', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '18000002-K', '+56922222222', 'CYCLE_DIRECTOR', 'ALL_LEVELS', 'ALL_SUBJECTS', true, true, NOW()),

('Psic. Ana', 'Rivera Campos', 'ana.rivera.entrevistadora@mtn.cl', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '18000003-8', '+56933333333', 'TEACHER', 'ALL_LEVELS', 'ALL_SUBJECTS', true, true, NOW()),

('Prof. Roberto', 'Silva Mora', 'roberto.silva.entrevistador@mtn.cl', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '18000004-6', '+56944444444', 'COORDINATOR', 'ALL_LEVELS', 'ALL_SUBJECTS', true, true, NOW()),

('Dra. Patricia', 'López Vega', 'patricia.lopez.entrevistadora@mtn.cl', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '18000005-4', '+56955555555', 'PSYCHOLOGIST', 'ALL_LEVELS', 'ALL_SUBJECTS', true, true, NOW()),

('Psic. Fernando', 'Morales Díaz', 'fernando.morales.entrevistador@mtn.cl', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '18000006-2', '+56966666666', 'PSYCHOLOGIST', 'ALL_LEVELS', 'ALL_SUBJECTS', true, true, NOW());

-- Crear horarios para estos nuevos entrevistadores usando interviewer_schedules
-- Obtener IDs y crear horarios de disponibilidad

-- Dra. María González Silva (PSYCHOLOGIST) - Horarios matutinos y vespertinos
INSERT INTO interviewer_schedules (interviewer_id, day_of_week, start_time, end_time, schedule_type, is_active, year, notes, created_at)
SELECT 
    u.id, 'MONDAY', '09:00:00'::time, '12:00:00'::time, 'RECURRING', true, 2025, 'Evaluación psicológica matutina', NOW()
FROM users u WHERE u.email = 'maria.gonzalez.entrevistadora@mtn.cl'
UNION ALL
SELECT 
    u.id, 'WEDNESDAY', '14:00:00'::time, '17:00:00'::time, 'RECURRING', true, 2025, 'Evaluación psicológica vespertina', NOW()
FROM users u WHERE u.email = 'maria.gonzalez.entrevistadora@mtn.cl'
UNION ALL
SELECT 
    u.id, 'FRIDAY', '09:00:00'::time, '12:00:00'::time, 'RECURRING', true, 2025, 'Evaluación psicológica viernes', NOW()
FROM users u WHERE u.email = 'maria.gonzalez.entrevistadora@mtn.cl'

-- Dr. Carlos Mendoza Torres (CYCLE_DIRECTOR) - Entrevistas familiares
UNION ALL
SELECT 
    u.id, 'TUESDAY', '09:00:00'::time, '13:00:00'::time, 'RECURRING', true, 2025, 'Entrevistas familiares matutinas', NOW()
FROM users u WHERE u.email = 'carlos.mendoza.entrevistador@mtn.cl'
UNION ALL
SELECT 
    u.id, 'THURSDAY', '15:00:00'::time, '18:00:00'::time, 'RECURRING', true, 2025, 'Entrevistas familiares vespertinas', NOW()
FROM users u WHERE u.email = 'carlos.mendoza.entrevistador@mtn.cl'

-- Psic. Ana Rivera Campos (TEACHER) - Evaluación académica
UNION ALL
SELECT 
    u.id, 'MONDAY', '08:00:00'::time, '12:00:00'::time, 'RECURRING', true, 2025, 'Evaluación académica matutina', NOW()
FROM users u WHERE u.email = 'ana.rivera.entrevistadora@mtn.cl'
UNION ALL
SELECT 
    u.id, 'WEDNESDAY', '14:00:00'::time, '17:00:00'::time, 'RECURRING', true, 2025, 'Evaluación académica vespertina', NOW()
FROM users u WHERE u.email = 'ana.rivera.entrevistadora@mtn.cl'

-- Prof. Roberto Silva Mora (COORDINATOR) - Entrevistas individuales
UNION ALL
SELECT 
    u.id, 'MONDAY', '14:00:00'::time, '18:00:00'::time, 'RECURRING', true, 2025, 'Entrevistas individuales vespertinas', NOW()
FROM users u WHERE u.email = 'roberto.silva.entrevistador@mtn.cl'
UNION ALL
SELECT 
    u.id, 'WEDNESDAY', '09:00:00'::time, '13:00:00'::time, 'RECURRING', true, 2025, 'Entrevistas individuales matutinas', NOW()
FROM users u WHERE u.email = 'roberto.silva.entrevistador@mtn.cl'

-- Dra. Patricia López Vega (PSYCHOLOGIST) - Evaluación comportamental
UNION ALL
SELECT 
    u.id, 'TUESDAY', '08:30:00'::time, '12:30:00'::time, 'RECURRING', true, 2025, 'Evaluación comportamental matutina', NOW()
FROM users u WHERE u.email = 'patricia.lopez.entrevistadora@mtn.cl'
UNION ALL
SELECT 
    u.id, 'THURSDAY', '14:00:00'::time, '17:00:00'::time, 'RECURRING', true, 2025, 'Evaluación comportamental vespertina', NOW()
FROM users u WHERE u.email = 'patricia.lopez.entrevistadora@mtn.cl'

-- Psic. Fernando Morales Díaz (PSYCHOLOGIST) - Evaluación integral
UNION ALL
SELECT 
    u.id, 'MONDAY', '10:00:00'::time, '14:00:00'::time, 'RECURRING', true, 2025, 'Evaluación integral matutina', NOW()
FROM users u WHERE u.email = 'fernando.morales.entrevistador@mtn.cl'
UNION ALL
SELECT 
    u.id, 'FRIDAY', '15:00:00'::time, '18:00:00'::time, 'RECURRING', true, 2025, 'Evaluación integral vespertina', NOW()
FROM users u WHERE u.email = 'fernando.morales.entrevistador@mtn.cl';

-- Verificar entrevistadores creados
SELECT 
    CONCAT(first_name, ' ', last_name) as nombre_completo,
    email,
    role,
    'PASSWORD: secret' as credenciales,
    CASE WHEN active THEN '✅ Activo' ELSE '❌ Inactivo' END as estado
FROM users 
WHERE email LIKE '%.entrevistador%@mtn.cl'
ORDER BY role, first_name;

-- Verificar horarios creados
SELECT 
    CONCAT(u.first_name, ' ', u.last_name) as entrevistador,
    u.role,
    s.day_of_week as día,
    s.start_time as inicio,
    s.end_time as fin,
    s.notes as especialidad
FROM users u
JOIN interviewer_schedules s ON s.interviewer_id = u.id
WHERE u.email LIKE '%.entrevistador%@mtn.cl' 
  AND s.is_active = true
ORDER BY u.first_name, 
         CASE s.day_of_week 
             WHEN 'MONDAY' THEN 1
             WHEN 'TUESDAY' THEN 2
             WHEN 'WEDNESDAY' THEN 3
             WHEN 'THURSDAY' THEN 4
             WHEN 'FRIDAY' THEN 5
         END;

-- Mostrar resumen de entrevistadores disponibles
SELECT 
    '🎯 ENTREVISTADORES DISPONIBLES PARA EL SISTEMA' as titulo;

SELECT 
    role as rol,
    COUNT(*) as cantidad,
    STRING_AGG(CONCAT(first_name, ' ', last_name), ', ' ORDER BY first_name) as nombres
FROM users 
WHERE email LIKE '%.entrevistador%@mtn.cl' 
  AND active = true
GROUP BY role
ORDER BY role;