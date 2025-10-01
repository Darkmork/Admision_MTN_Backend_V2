-- Recrear datos con esquema correcto

-- 1. Estudiantes con esquema correcto
INSERT INTO students (first_name, paternal_last_name, maternal_last_name, rut, birth_date, grade_applied, current_school, school_applied, address, created_at) VALUES
('Juan', 'Pérez', 'González', '20001001-1', '2015-05-15', 'PRIMERO_BASICO', 'Escuela Anterior', 'MONTE_TABOR', 'Dirección 123', NOW()),
('María', 'López', 'Silva', '20001002-K', '2014-08-20', 'SEGUNDO_BASICO', 'Colegio Previo', 'NAZARET', 'Dirección 456', NOW()),
('Carlos', 'Rodríguez', 'Torres', '20001003-8', '2016-03-10', 'PREKINDER', 'Jardín Infantil', 'MONTE_TABOR', 'Dirección 789', NOW());

-- Verificar columnas de parents
-- \d parents