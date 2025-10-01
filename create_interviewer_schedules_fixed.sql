-- Crear horarios disponibles para todos los entrevistadores usando la estructura correcta
-- Esto solucionará el error "El entrevistador no tiene horarios disponibles configurados"

-- Limpiar horarios existentes
DELETE FROM interviewer_schedules;

-- Crear horarios estándar para todos los entrevistadores usando la estructura real
INSERT INTO interviewer_schedules (interviewer_id, day_of_week, start_time, end_time, schedule_type, year, is_active, created_at) VALUES
-- ID 2: Ana Rivera - Director de Ciclo (LUNES A VIERNES, 8:00-17:00)
(2, 'MONDAY', '08:00:00', '08:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'MONDAY', '08:30:00', '09:00:00', 'RECURRING', 2025, true, NOW()),
(2, 'MONDAY', '09:00:00', '09:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'MONDAY', '09:30:00', '10:00:00', 'RECURRING', 2025, true, NOW()),
(2, 'MONDAY', '10:00:00', '10:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'MONDAY', '10:30:00', '11:00:00', 'RECURRING', 2025, true, NOW()),
(2, 'MONDAY', '11:00:00', '11:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'MONDAY', '11:30:00', '12:00:00', 'RECURRING', 2025, true, NOW()),
(2, 'MONDAY', '14:00:00', '14:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'MONDAY', '14:30:00', '15:00:00', 'RECURRING', 2025, true, NOW()),
(2, 'MONDAY', '15:00:00', '15:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'MONDAY', '15:30:00', '16:00:00', 'RECURRING', 2025, true, NOW()),
(2, 'MONDAY', '16:00:00', '16:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'MONDAY', '16:30:00', '17:00:00', 'RECURRING', 2025, true, NOW()),

(2, 'TUESDAY', '08:00:00', '08:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'TUESDAY', '08:30:00', '09:00:00', 'RECURRING', 2025, true, NOW()),
(2, 'TUESDAY', '09:00:00', '09:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'TUESDAY', '09:30:00', '10:00:00', 'RECURRING', 2025, true, NOW()),
(2, 'TUESDAY', '10:00:00', '10:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'TUESDAY', '10:30:00', '11:00:00', 'RECURRING', 2025, true, NOW()),
(2, 'TUESDAY', '11:00:00', '11:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'TUESDAY', '11:30:00', '12:00:00', 'RECURRING', 2025, true, NOW()),
(2, 'TUESDAY', '14:00:00', '14:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'TUESDAY', '14:30:00', '15:00:00', 'RECURRING', 2025, true, NOW()),
(2, 'TUESDAY', '15:00:00', '15:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'TUESDAY', '15:30:00', '16:00:00', 'RECURRING', 2025, true, NOW()),
(2, 'TUESDAY', '16:00:00', '16:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'TUESDAY', '16:30:00', '17:00:00', 'RECURRING', 2025, true, NOW()),

(2, 'WEDNESDAY', '08:00:00', '08:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'WEDNESDAY', '08:30:00', '09:00:00', 'RECURRING', 2025, true, NOW()),
(2, 'WEDNESDAY', '09:00:00', '09:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'WEDNESDAY', '09:30:00', '10:00:00', 'RECURRING', 2025, true, NOW()),
(2, 'WEDNESDAY', '10:00:00', '10:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'WEDNESDAY', '10:30:00', '11:00:00', 'RECURRING', 2025, true, NOW()),
(2, 'WEDNESDAY', '11:00:00', '11:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'WEDNESDAY', '11:30:00', '12:00:00', 'RECURRING', 2025, true, NOW()),
(2, 'WEDNESDAY', '14:00:00', '14:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'WEDNESDAY', '14:30:00', '15:00:00', 'RECURRING', 2025, true, NOW()),
(2, 'WEDNESDAY', '15:00:00', '15:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'WEDNESDAY', '15:30:00', '16:00:00', 'RECURRING', 2025, true, NOW()),
(2, 'WEDNESDAY', '16:00:00', '16:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'WEDNESDAY', '16:30:00', '17:00:00', 'RECURRING', 2025, true, NOW()),

(2, 'THURSDAY', '08:00:00', '08:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'THURSDAY', '08:30:00', '09:00:00', 'RECURRING', 2025, true, NOW()),
(2, 'THURSDAY', '09:00:00', '09:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'THURSDAY', '09:30:00', '10:00:00', 'RECURRING', 2025, true, NOW()),
(2, 'THURSDAY', '10:00:00', '10:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'THURSDAY', '10:30:00', '11:00:00', 'RECURRING', 2025, true, NOW()),
(2, 'THURSDAY', '11:00:00', '11:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'THURSDAY', '11:30:00', '12:00:00', 'RECURRING', 2025, true, NOW()),
(2, 'THURSDAY', '14:00:00', '14:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'THURSDAY', '14:30:00', '15:00:00', 'RECURRING', 2025, true, NOW()),
(2, 'THURSDAY', '15:00:00', '15:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'THURSDAY', '15:30:00', '16:00:00', 'RECURRING', 2025, true, NOW()),
(2, 'THURSDAY', '16:00:00', '16:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'THURSDAY', '16:30:00', '17:00:00', 'RECURRING', 2025, true, NOW()),

(2, 'FRIDAY', '08:00:00', '08:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'FRIDAY', '08:30:00', '09:00:00', 'RECURRING', 2025, true, NOW()),
(2, 'FRIDAY', '09:00:00', '09:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'FRIDAY', '09:30:00', '10:00:00', 'RECURRING', 2025, true, NOW()),
(2, 'FRIDAY', '10:00:00', '10:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'FRIDAY', '10:30:00', '11:00:00', 'RECURRING', 2025, true, NOW()),
(2, 'FRIDAY', '11:00:00', '11:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'FRIDAY', '11:30:00', '12:00:00', 'RECURRING', 2025, true, NOW()),
(2, 'FRIDAY', '14:00:00', '14:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'FRIDAY', '14:30:00', '15:00:00', 'RECURRING', 2025, true, NOW()),
(2, 'FRIDAY', '15:00:00', '15:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'FRIDAY', '15:30:00', '16:00:00', 'RECURRING', 2025, true, NOW()),
(2, 'FRIDAY', '16:00:00', '16:30:00', 'RECURRING', 2025, true, NOW()),
(2, 'FRIDAY', '16:30:00', '17:00:00', 'RECURRING', 2025, true, NOW()),

-- ID 3: Carlos Morales - Coordinador (LUNES A VIERNES, 8:30-16:30)
(3, 'MONDAY', '08:30:00', '09:00:00', 'RECURRING', 2025, true, NOW()),
(3, 'MONDAY', '09:00:00', '09:30:00', 'RECURRING', 2025, true, NOW()),
(3, 'MONDAY', '09:30:00', '10:00:00', 'RECURRING', 2025, true, NOW()),
(3, 'MONDAY', '10:00:00', '10:30:00', 'RECURRING', 2025, true, NOW()),
(3, 'MONDAY', '10:30:00', '11:00:00', 'RECURRING', 2025, true, NOW()),
(3, 'MONDAY', '11:00:00', '11:30:00', 'RECURRING', 2025, true, NOW()),
(3, 'MONDAY', '11:30:00', '12:00:00', 'RECURRING', 2025, true, NOW()),
(3, 'MONDAY', '14:00:00', '14:30:00', 'RECURRING', 2025, true, NOW()),
(3, 'MONDAY', '14:30:00', '15:00:00', 'RECURRING', 2025, true, NOW()),
(3, 'MONDAY', '15:00:00', '15:30:00', 'RECURRING', 2025, true, NOW()),
(3, 'MONDAY', '15:30:00', '16:00:00', 'RECURRING', 2025, true, NOW()),
(3, 'MONDAY', '16:00:00', '16:30:00', 'RECURRING', 2025, true, NOW()),

(3, 'TUESDAY', '08:30:00', '09:00:00', 'RECURRING', 2025, true, NOW()),
(3, 'TUESDAY', '09:00:00', '09:30:00', 'RECURRING', 2025, true, NOW()),
(3, 'TUESDAY', '09:30:00', '10:00:00', 'RECURRING', 2025, true, NOW()),
(3, 'TUESDAY', '10:00:00', '10:30:00', 'RECURRING', 2025, true, NOW()),
(3, 'TUESDAY', '10:30:00', '11:00:00', 'RECURRING', 2025, true, NOW()),
(3, 'TUESDAY', '11:00:00', '11:30:00', 'RECURRING', 2025, true, NOW()),
(3, 'TUESDAY', '11:30:00', '12:00:00', 'RECURRING', 2025, true, NOW()),
(3, 'TUESDAY', '14:00:00', '14:30:00', 'RECURRING', 2025, true, NOW()),
(3, 'TUESDAY', '14:30:00', '15:00:00', 'RECURRING', 2025, true, NOW()),
(3, 'TUESDAY', '15:00:00', '15:30:00', 'RECURRING', 2025, true, NOW()),
(3, 'TUESDAY', '15:30:00', '16:00:00', 'RECURRING', 2025, true, NOW()),
(3, 'TUESDAY', '16:00:00', '16:30:00', 'RECURRING', 2025, true, NOW()),

(3, 'WEDNESDAY', '08:30:00', '09:00:00', 'RECURRING', 2025, true, NOW()),
(3, 'WEDNESDAY', '09:00:00', '09:30:00', 'RECURRING', 2025, true, NOW()),
(3, 'WEDNESDAY', '09:30:00', '10:00:00', 'RECURRING', 2025, true, NOW()),
(3, 'WEDNESDAY', '10:00:00', '10:30:00', 'RECURRING', 2025, true, NOW()),
(3, 'WEDNESDAY', '10:30:00', '11:00:00', 'RECURRING', 2025, true, NOW()),
(3, 'WEDNESDAY', '11:00:00', '11:30:00', 'RECURRING', 2025, true, NOW()),
(3, 'WEDNESDAY', '11:30:00', '12:00:00', 'RECURRING', 2025, true, NOW()),
(3, 'WEDNESDAY', '14:00:00', '14:30:00', 'RECURRING', 2025, true, NOW()),
(3, 'WEDNESDAY', '14:30:00', '15:00:00', 'RECURRING', 2025, true, NOW()),
(3, 'WEDNESDAY', '15:00:00', '15:30:00', 'RECURRING', 2025, true, NOW()),
(3, 'WEDNESDAY', '15:30:00', '16:00:00', 'RECURRING', 2025, true, NOW()),
(3, 'WEDNESDAY', '16:00:00', '16:30:00', 'RECURRING', 2025, true, NOW()),

(3, 'THURSDAY', '08:30:00', '09:00:00', 'RECURRING', 2025, true, NOW()),
(3, 'THURSDAY', '09:00:00', '09:30:00', 'RECURRING', 2025, true, NOW()),
(3, 'THURSDAY', '09:30:00', '10:00:00', 'RECURRING', 2025, true, NOW()),
(3, 'THURSDAY', '10:00:00', '10:30:00', 'RECURRING', 2025, true, NOW()),
(3, 'THURSDAY', '10:30:00', '11:00:00', 'RECURRING', 2025, true, NOW()),
(3, 'THURSDAY', '11:00:00', '11:30:00', 'RECURRING', 2025, true, NOW()),
(3, 'THURSDAY', '11:30:00', '12:00:00', 'RECURRING', 2025, true, NOW()),
(3, 'THURSDAY', '14:00:00', '14:30:00', 'RECURRING', 2025, true, NOW()),
(3, 'THURSDAY', '14:30:00', '15:00:00', 'RECURRING', 2025, true, NOW()),
(3, 'THURSDAY', '15:00:00', '15:30:00', 'RECURRING', 2025, true, NOW()),
(3, 'THURSDAY', '15:30:00', '16:00:00', 'RECURRING', 2025, true, NOW()),
(3, 'THURSDAY', '16:00:00', '16:30:00', 'RECURRING', 2025, true, NOW()),

(3, 'FRIDAY', '08:30:00', '09:00:00', 'RECURRING', 2025, true, NOW()),
(3, 'FRIDAY', '09:00:00', '09:30:00', 'RECURRING', 2025, true, NOW()),
(3, 'FRIDAY', '09:30:00', '10:00:00', 'RECURRING', 2025, true, NOW()),
(3, 'FRIDAY', '10:00:00', '10:30:00', 'RECURRING', 2025, true, NOW()),
(3, 'FRIDAY', '10:30:00', '11:00:00', 'RECURRING', 2025, true, NOW()),
(3, 'FRIDAY', '11:00:00', '11:30:00', 'RECURRING', 2025, true, NOW()),
(3, 'FRIDAY', '11:30:00', '12:00:00', 'RECURRING', 2025, true, NOW()),
(3, 'FRIDAY', '14:00:00', '14:30:00', 'RECURRING', 2025, true, NOW()),
(3, 'FRIDAY', '14:30:00', '15:00:00', 'RECURRING', 2025, true, NOW()),
(3, 'FRIDAY', '15:00:00', '15:30:00', 'RECURRING', 2025, true, NOW()),
(3, 'FRIDAY', '15:30:00', '16:00:00', 'RECURRING', 2025, true, NOW()),
(3, 'FRIDAY', '16:00:00', '16:30:00', 'RECURRING', 2025, true, NOW()),

-- ID 4: Elena Castro - Psicóloga (LUNES A VIERNES, 9:00-16:00) - CRÍTICO: Esta es ID 5 en el error
(4, 'MONDAY', '09:00:00', '09:30:00', 'RECURRING', 2025, true, NOW()),
(4, 'MONDAY', '09:30:00', '10:00:00', 'RECURRING', 2025, true, NOW()),
(4, 'MONDAY', '10:00:00', '10:30:00', 'RECURRING', 2025, true, NOW()),
(4, 'MONDAY', '10:30:00', '11:00:00', 'RECURRING', 2025, true, NOW()),
(4, 'MONDAY', '11:00:00', '11:30:00', 'RECURRING', 2025, true, NOW()),
(4, 'MONDAY', '11:30:00', '12:00:00', 'RECURRING', 2025, true, NOW()),
(4, 'MONDAY', '14:00:00', '14:30:00', 'RECURRING', 2025, true, NOW()),
(4, 'MONDAY', '14:30:00', '15:00:00', 'RECURRING', 2025, true, NOW()),
(4, 'MONDAY', '15:00:00', '15:30:00', 'RECURRING', 2025, true, NOW()),
(4, 'MONDAY', '15:30:00', '16:00:00', 'RECURRING', 2025, true, NOW()),

(4, 'TUESDAY', '09:00:00', '09:30:00', 'RECURRING', 2025, true, NOW()),
(4, 'TUESDAY', '09:30:00', '10:00:00', 'RECURRING', 2025, true, NOW()),
(4, 'TUESDAY', '10:00:00', '10:30:00', 'RECURRING', 2025, true, NOW()),
(4, 'TUESDAY', '10:30:00', '11:00:00', 'RECURRING', 2025, true, NOW()),
(4, 'TUESDAY', '11:00:00', '11:30:00', 'RECURRING', 2025, true, NOW()),
(4, 'TUESDAY', '11:30:00', '12:00:00', 'RECURRING', 2025, true, NOW()),
(4, 'TUESDAY', '14:00:00', '14:30:00', 'RECURRING', 2025, true, NOW()),
(4, 'TUESDAY', '14:30:00', '15:00:00', 'RECURRING', 2025, true, NOW()),
(4, 'TUESDAY', '15:00:00', '15:30:00', 'RECURRING', 2025, true, NOW()),
(4, 'TUESDAY', '15:30:00', '16:00:00', 'RECURRING', 2025, true, NOW()),

(4, 'WEDNESDAY', '09:00:00', '09:30:00', 'RECURRING', 2025, true, NOW()),
(4, 'WEDNESDAY', '09:30:00', '10:00:00', 'RECURRING', 2025, true, NOW()),
(4, 'WEDNESDAY', '10:00:00', '10:30:00', 'RECURRING', 2025, true, NOW()),
(4, 'WEDNESDAY', '10:30:00', '11:00:00', 'RECURRING', 2025, true, NOW()),
(4, 'WEDNESDAY', '11:00:00', '11:30:00', 'RECURRING', 2025, true, NOW()),
(4, 'WEDNESDAY', '11:30:00', '12:00:00', 'RECURRING', 2025, true, NOW()),
(4, 'WEDNESDAY', '14:00:00', '14:30:00', 'RECURRING', 2025, true, NOW()),
(4, 'WEDNESDAY', '14:30:00', '15:00:00', 'RECURRING', 2025, true, NOW()),
(4, 'WEDNESDAY', '15:00:00', '15:30:00', 'RECURRING', 2025, true, NOW()),
(4, 'WEDNESDAY', '15:30:00', '16:00:00', 'RECURRING', 2025, true, NOW()),

(4, 'THURSDAY', '09:00:00', '09:30:00', 'RECURRING', 2025, true, NOW()),
(4, 'THURSDAY', '09:30:00', '10:00:00', 'RECURRING', 2025, true, NOW()),
(4, 'THURSDAY', '10:00:00', '10:30:00', 'RECURRING', 2025, true, NOW()),
(4, 'THURSDAY', '10:30:00', '11:00:00', 'RECURRING', 2025, true, NOW()),
(4, 'THURSDAY', '11:00:00', '11:30:00', 'RECURRING', 2025, true, NOW()),
(4, 'THURSDAY', '11:30:00', '12:00:00', 'RECURRING', 2025, true, NOW()),
(4, 'THURSDAY', '14:00:00', '14:30:00', 'RECURRING', 2025, true, NOW()),
(4, 'THURSDAY', '14:30:00', '15:00:00', 'RECURRING', 2025, true, NOW()),
(4, 'THURSDAY', '15:00:00', '15:30:00', 'RECURRING', 2025, true, NOW()),
(4, 'THURSDAY', '15:30:00', '16:00:00', 'RECURRING', 2025, true, NOW()),

(4, 'FRIDAY', '09:00:00', '09:30:00', 'RECURRING', 2025, true, NOW()),
(4, 'FRIDAY', '09:30:00', '10:00:00', 'RECURRING', 2025, true, NOW()),
(4, 'FRIDAY', '10:00:00', '10:30:00', 'RECURRING', 2025, true, NOW()),
(4, 'FRIDAY', '10:30:00', '11:00:00', 'RECURRING', 2025, true, NOW()),
(4, 'FRIDAY', '11:00:00', '11:30:00', 'RECURRING', 2025, true, NOW()),
(4, 'FRIDAY', '11:30:00', '12:00:00', 'RECURRING', 2025, true, NOW()),
(4, 'FRIDAY', '14:00:00', '14:30:00', 'RECURRING', 2025, true, NOW()),
(4, 'FRIDAY', '14:30:00', '15:00:00', 'RECURRING', 2025, true, NOW()),
(4, 'FRIDAY', '15:00:00', '15:30:00', 'RECURRING', 2025, true, NOW()),
(4, 'FRIDAY', '15:30:00', '16:00:00', 'RECURRING', 2025, true, NOW()),

-- ID 5: Pedro Matemático - Profesor (LUNES, MIÉRCOLES, VIERNES) - CRÍTICO: Este es el que aparece en el error
(5, 'MONDAY', '10:00:00', '10:30:00', 'RECURRING', 2025, true, NOW()),
(5, 'MONDAY', '10:30:00', '11:00:00', 'RECURRING', 2025, true, NOW()),
(5, 'MONDAY', '11:00:00', '11:30:00', 'RECURRING', 2025, true, NOW()),
(5, 'MONDAY', '15:00:00', '15:30:00', 'RECURRING', 2025, true, NOW()),
(5, 'MONDAY', '15:30:00', '16:00:00', 'RECURRING', 2025, true, NOW()),

(5, 'WEDNESDAY', '10:00:00', '10:30:00', 'RECURRING', 2025, true, NOW()),
(5, 'WEDNESDAY', '10:30:00', '11:00:00', 'RECURRING', 2025, true, NOW()),
(5, 'WEDNESDAY', '11:00:00', '11:30:00', 'RECURRING', 2025, true, NOW()),
(5, 'WEDNESDAY', '15:00:00', '15:30:00', 'RECURRING', 2025, true, NOW()),
(5, 'WEDNESDAY', '15:30:00', '16:00:00', 'RECURRING', 2025, true, NOW()),

(5, 'FRIDAY', '10:00:00', '10:30:00', 'RECURRING', 2025, true, NOW()),
(5, 'FRIDAY', '10:30:00', '11:00:00', 'RECURRING', 2025, true, NOW()),
(5, 'FRIDAY', '11:00:00', '11:30:00', 'RECURRING', 2025, true, NOW()),
(5, 'FRIDAY', '15:00:00', '15:30:00', 'RECURRING', 2025, true, NOW()),
(5, 'FRIDAY', '15:30:00', '16:00:00', 'RECURRING', 2025, true, NOW()),

-- Horarios adicionales para miércoles para cubrir 15:30 que aparece en el error
(5, 'TUESDAY', '15:00:00', '15:30:00', 'RECURRING', 2025, true, NOW()),
(5, 'TUESDAY', '15:30:00', '16:00:00', 'RECURRING', 2025, true, NOW()),
(5, 'TUESDAY', '16:00:00', '16:30:00', 'RECURRING', 2025, true, NOW()),

(5, 'THURSDAY', '15:00:00', '15:30:00', 'RECURRING', 2025, true, NOW()),
(5, 'THURSDAY', '15:30:00', '16:00:00', 'RECURRING', 2025, true, NOW()),
(5, 'THURSDAY', '16:00:00', '16:30:00', 'RECURRING', 2025, true, NOW()),

-- ID 12: Jorge Hernandez - Profesor (MARTES Y JUEVES)
(12, 'TUESDAY', '09:00:00', '09:30:00', 'RECURRING', 2025, true, NOW()),
(12, 'TUESDAY', '09:30:00', '10:00:00', 'RECURRING', 2025, true, NOW()),
(12, 'TUESDAY', '10:00:00', '10:30:00', 'RECURRING', 2025, true, NOW()),
(12, 'TUESDAY', '10:30:00', '11:00:00', 'RECURRING', 2025, true, NOW()),
(12, 'TUESDAY', '15:00:00', '15:30:00', 'RECURRING', 2025, true, NOW()),
(12, 'TUESDAY', '15:30:00', '16:00:00', 'RECURRING', 2025, true, NOW()),
(12, 'TUESDAY', '16:00:00', '16:30:00', 'RECURRING', 2025, true, NOW()),

(12, 'THURSDAY', '09:00:00', '09:30:00', 'RECURRING', 2025, true, NOW()),
(12, 'THURSDAY', '09:30:00', '10:00:00', 'RECURRING', 2025, true, NOW()),
(12, 'THURSDAY', '10:00:00', '10:30:00', 'RECURRING', 2025, true, NOW()),
(12, 'THURSDAY', '10:30:00', '11:00:00', 'RECURRING', 2025, true, NOW()),
(12, 'THURSDAY', '15:00:00', '15:30:00', 'RECURRING', 2025, true, NOW()),
(12, 'THURSDAY', '15:30:00', '16:00:00', 'RECURRING', 2025, true, NOW()),
(12, 'THURSDAY', '16:00:00', '16:30:00', 'RECURRING', 2025, true, NOW());

-- Verificar que se crearon correctamente
SELECT 
    u.id,
    CONCAT(u.first_name, ' ', u.last_name) as entrevistador,
    u.role,
    s.day_of_week,
    s.start_time,
    s.end_time,
    COUNT(*) as total_slots
FROM interviewer_schedules s
JOIN users u ON s.interviewer_id = u.id
WHERE s.is_active = true
GROUP BY u.id, u.first_name, u.last_name, u.role, s.day_of_week, s.start_time, s.end_time
ORDER BY u.id, s.day_of_week, s.start_time
LIMIT 30;

-- Resumen por entrevistador
SELECT 
    u.id,
    CONCAT(u.first_name, ' ', u.last_name) as entrevistador,
    u.role,
    COUNT(*) as total_slots_disponibles
FROM interviewer_schedules s
JOIN users u ON s.interviewer_id = u.id
WHERE s.is_active = true
GROUP BY u.id, u.first_name, u.last_name, u.role
ORDER BY u.id;
