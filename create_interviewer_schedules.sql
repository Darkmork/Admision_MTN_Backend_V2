-- Crear horarios disponibles para todos los entrevistadores
-- Esto solucionará el error "El entrevistador no tiene horarios disponibles configurados"

-- Primero verificamos qué entrevistadores tenemos
SELECT 
    id, 
    CONCAT(first_name, ' ', last_name) as nombre_completo, 
    role, 
    educational_level, 
    subject 
FROM users 
WHERE role IN ('PSYCHOLOGIST', 'CYCLE_DIRECTOR', 'COORDINATOR', 'TEACHER')
ORDER BY role, id;

-- Crear tabla de horarios si no existe (por si acaso)
CREATE TABLE IF NOT EXISTS interviewer_schedules (
    id SERIAL PRIMARY KEY,
    interviewer_id INTEGER NOT NULL REFERENCES users(id),
    day_of_week INTEGER NOT NULL CHECK (day_of_week >= 0 AND day_of_week <= 6), -- 0=Domingo, 1=Lunes, etc.
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_available BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(interviewer_id, day_of_week, start_time)
);

-- Limpiar horarios existentes para evitar duplicados
DELETE FROM interviewer_schedules;

-- Crear horarios estándar para todos los entrevistadores
-- Horario típico: Lunes a Viernes, 8:00 AM - 5:00 PM con bloques de 30 minutos

INSERT INTO interviewer_schedules (interviewer_id, day_of_week, start_time, end_time, is_available) VALUES
-- ID 2: Director de Ciclo (LUNES A VIERNES, 8:00-17:00)
(2, 1, '08:00', '08:30', true), (2, 1, '08:30', '09:00', true), (2, 1, '09:00', '09:30', true), (2, 1, '09:30', '10:00', true),
(2, 1, '10:00', '10:30', true), (2, 1, '10:30', '11:00', true), (2, 1, '11:00', '11:30', true), (2, 1, '11:30', '12:00', true),
(2, 1, '14:00', '14:30', true), (2, 1, '14:30', '15:00', true), (2, 1, '15:00', '15:30', true), (2, 1, '15:30', '16:00', true),
(2, 1, '16:00', '16:30', true), (2, 1, '16:30', '17:00', true),

(2, 2, '08:00', '08:30', true), (2, 2, '08:30', '09:00', true), (2, 2, '09:00', '09:30', true), (2, 2, '09:30', '10:00', true),
(2, 2, '10:00', '10:30', true), (2, 2, '10:30', '11:00', true), (2, 2, '11:00', '11:30', true), (2, 2, '11:30', '12:00', true),
(2, 2, '14:00', '14:30', true), (2, 2, '14:30', '15:00', true), (2, 2, '15:00', '15:30', true), (2, 2, '15:30', '16:00', true),
(2, 2, '16:00', '16:30', true), (2, 2, '16:30', '17:00', true),

(2, 3, '08:00', '08:30', true), (2, 3, '08:30', '09:00', true), (2, 3, '09:00', '09:30', true), (2, 3, '09:30', '10:00', true),
(2, 3, '10:00', '10:30', true), (2, 3, '10:30', '11:00', true), (2, 3, '11:00', '11:30', true), (2, 3, '11:30', '12:00', true),
(2, 3, '14:00', '14:30', true), (2, 3, '14:30', '15:00', true), (2, 3, '15:00', '15:30', true), (2, 3, '15:30', '16:00', true),
(2, 3, '16:00', '16:30', true), (2, 3, '16:30', '17:00', true),

(2, 4, '08:00', '08:30', true), (2, 4, '08:30', '09:00', true), (2, 4, '09:00', '09:30', true), (2, 4, '09:30', '10:00', true),
(2, 4, '10:00', '10:30', true), (2, 4, '10:30', '11:00', true), (2, 4, '11:00', '11:30', true), (2, 4, '11:30', '12:00', true),
(2, 4, '14:00', '14:30', true), (2, 4, '14:30', '15:00', true), (2, 4, '15:00', '15:30', true), (2, 4, '15:30', '16:00', true),
(2, 4, '16:00', '16:30', true), (2, 4, '16:30', '17:00', true),

(2, 5, '08:00', '08:30', true), (2, 5, '08:30', '09:00', true), (2, 5, '09:00', '09:30', true), (2, 5, '09:30', '10:00', true),
(2, 5, '10:00', '10:30', true), (2, 5, '10:30', '11:00', true), (2, 5, '11:00', '11:30', true), (2, 5, '11:30', '12:00', true),
(2, 5, '14:00', '14:30', true), (2, 5, '14:30', '15:00', true), (2, 5, '15:00', '15:30', true), (2, 5, '15:30', '16:00', true),
(2, 5, '16:00', '16:30', true), (2, 5, '16:30', '17:00', true),

-- ID 3: Coordinadora (LUNES A VIERNES, 8:30-16:30)
(3, 1, '08:30', '09:00', true), (3, 1, '09:00', '09:30', true), (3, 1, '09:30', '10:00', true), (3, 1, '10:00', '10:30', true),
(3, 1, '10:30', '11:00', true), (3, 1, '11:00', '11:30', true), (3, 1, '11:30', '12:00', true),
(3, 1, '14:00', '14:30', true), (3, 1, '14:30', '15:00', true), (3, 1, '15:00', '15:30', true), (3, 1, '15:30', '16:00', true),
(3, 1, '16:00', '16:30', true),

(3, 2, '08:30', '09:00', true), (3, 2, '09:00', '09:30', true), (3, 2, '09:30', '10:00', true), (3, 2, '10:00', '10:30', true),
(3, 2, '10:30', '11:00', true), (3, 2, '11:00', '11:30', true), (3, 2, '11:30', '12:00', true),
(3, 2, '14:00', '14:30', true), (3, 2, '14:30', '15:00', true), (3, 2, '15:00', '15:30', true), (3, 2, '15:30', '16:00', true),
(3, 2, '16:00', '16:30', true),

(3, 3, '08:30', '09:00', true), (3, 3, '09:00', '09:30', true), (3, 3, '09:30', '10:00', true), (3, 3, '10:00', '10:30', true),
(3, 3, '10:30', '11:00', true), (3, 3, '11:00', '11:30', true), (3, 3, '11:30', '12:00', true),
(3, 3, '14:00', '14:30', true), (3, 3, '14:30', '15:00', true), (3, 3, '15:00', '15:30', true), (3, 3, '15:30', '16:00', true),
(3, 3, '16:00', '16:30', true),

(3, 4, '08:30', '09:00', true), (3, 4, '09:00', '09:30', true), (3, 4, '09:30', '10:00', true), (3, 4, '10:00', '10:30', true),
(3, 4, '10:30', '11:00', true), (3, 4, '11:00', '11:30', true), (3, 4, '11:30', '12:00', true),
(3, 4, '14:00', '14:30', true), (3, 4, '14:30', '15:00', true), (3, 4, '15:00', '15:30', true), (3, 4, '15:30', '16:00', true),
(3, 4, '16:00', '16:30', true),

(3, 5, '08:30', '09:00', true), (3, 5, '09:00', '09:30', true), (3, 5, '09:30', '10:00', true), (3, 5, '10:00', '10:30', true),
(3, 5, '10:30', '11:00', true), (3, 5, '11:00', '11:30', true), (3, 5, '11:30', '12:00', true),
(3, 5, '14:00', '14:30', true), (3, 5, '14:30', '15:00', true), (3, 5, '15:00', '15:30', true), (3, 5, '15:30', '16:00', true),
(3, 5, '16:00', '16:30', true),

-- ID 4: Psicóloga (LUNES A VIERNES, 9:00-16:00)
(4, 1, '09:00', '09:30', true), (4, 1, '09:30', '10:00', true), (4, 1, '10:00', '10:30', true), (4, 1, '10:30', '11:00', true),
(4, 1, '11:00', '11:30', true), (4, 1, '11:30', '12:00', true),
(4, 1, '14:00', '14:30', true), (4, 1, '14:30', '15:00', true), (4, 1, '15:00', '15:30', true), (4, 1, '15:30', '16:00', true),

(4, 2, '09:00', '09:30', true), (4, 2, '09:30', '10:00', true), (4, 2, '10:00', '10:30', true), (4, 2, '10:30', '11:00', true),
(4, 2, '11:00', '11:30', true), (4, 2, '11:30', '12:00', true),
(4, 2, '14:00', '14:30', true), (4, 2, '14:30', '15:00', true), (4, 2, '15:00', '15:30', true), (4, 2, '15:30', '16:00', true),

(4, 3, '09:00', '09:30', true), (4, 3, '09:30', '10:00', true), (4, 3, '10:00', '10:30', true), (4, 3, '10:30', '11:00', true),
(4, 3, '11:00', '11:30', true), (4, 3, '11:30', '12:00', true),
(4, 3, '14:00', '14:30', true), (4, 3, '14:30', '15:00', true), (4, 3, '15:00', '15:30', true), (4, 3, '15:30', '16:00', true),

(4, 4, '09:00', '09:30', true), (4, 4, '09:30', '10:00', true), (4, 4, '10:00', '10:30', true), (4, 4, '10:30', '11:00', true),
(4, 4, '11:00', '11:30', true), (4, 4, '11:30', '12:00', true),
(4, 4, '14:00', '14:30', true), (4, 4, '14:30', '15:00', true), (4, 4, '15:00', '15:30', true), (4, 4, '15:30', '16:00', true),

(4, 5, '09:00', '09:30', true), (4, 5, '09:30', '10:00', true), (4, 5, '10:00', '10:30', true), (4, 5, '10:30', '11:00', true),
(4, 5, '11:00', '11:30', true), (4, 5, '11:30', '12:00', true),
(4, 5, '14:00', '14:30', true), (4, 5, '14:30', '15:00', true), (4, 5, '15:00', '15:30', true), (4, 5, '15:30', '16:00', true),

-- ID 5: Profesor Matemáticas (LUNES, MIÉRCOLES, VIERNES)
(5, 1, '10:00', '10:30', true), (5, 1, '10:30', '11:00', true), (5, 1, '11:00', '11:30', true),
(5, 1, '15:00', '15:30', true), (5, 1, '15:30', '16:00', true),

(5, 3, '10:00', '10:30', true), (5, 3, '10:30', '11:00', true), (5, 3, '11:00', '11:30', true),
(5, 3, '15:00', '15:30', true), (5, 3, '15:30', '16:00', true),

(5, 5, '10:00', '10:30', true), (5, 5, '10:30', '11:00', true), (5, 5, '11:00', '11:30', true),
(5, 5, '15:00', '15:30', true), (5, 5, '15:30', '16:00', true),

-- ID 12: Profesor Evaluaciones (MARTES Y JUEVES)
(12, 2, '09:00', '09:30', true), (12, 2, '09:30', '10:00', true), (12, 2, '10:00', '10:30', true), (12, 2, '10:30', '11:00', true),
(12, 2, '15:00', '15:30', true), (12, 2, '15:30', '16:00', true), (12, 2, '16:00', '16:30', true),

(12, 4, '09:00', '09:30', true), (12, 4, '09:30', '10:00', true), (12, 4, '10:00', '10:30', true), (12, 4, '10:30', '11:00', true),
(12, 4, '15:00', '15:30', true), (12, 4, '15:30', '16:00', true), (12, 4, '16:00', '16:30', true);

-- Verificar que se crearon correctamente
SELECT 
    u.id,
    CONCAT(u.first_name, ' ', u.last_name) as entrevistador,
    u.role,
    CASE s.day_of_week 
        WHEN 1 THEN 'Lunes'
        WHEN 2 THEN 'Martes' 
        WHEN 3 THEN 'Miércoles'
        WHEN 4 THEN 'Jueves'
        WHEN 5 THEN 'Viernes'
        WHEN 6 THEN 'Sábado'
        WHEN 0 THEN 'Domingo'
    END as dia_semana,
    s.start_time,
    s.end_time,
    COUNT(*) as horarios_disponibles
FROM interviewer_schedules s
JOIN users u ON s.interviewer_id = u.id
WHERE s.is_available = true
GROUP BY u.id, u.first_name, u.last_name, u.role, s.day_of_week, s.start_time, s.end_time
ORDER BY u.id, s.day_of_week, s.start_time
LIMIT 20;

-- Resumen por entrevistador
SELECT 
    u.id,
    CONCAT(u.first_name, ' ', u.last_name) as entrevistador,
    u.role,
    COUNT(*) as total_slots_disponibles
FROM interviewer_schedules s
JOIN users u ON s.interviewer_id = u.id
WHERE s.is_available = true
GROUP BY u.id, u.first_name, u.last_name, u.role
ORDER BY u.id;
