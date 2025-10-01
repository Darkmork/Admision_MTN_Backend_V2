-- Agregar campos importantes para filtros y categorías especiales

-- Agregar campos a la tabla students
ALTER TABLE students 
ADD COLUMN IF NOT EXISTS is_employee_child BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS is_alumni_child BOOLEAN DEFAULT FALSE, 
ADD COLUMN IF NOT EXISTS is_inclusion_student BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS employee_parent_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS alumni_parent_year INTEGER,
ADD COLUMN IF NOT EXISTS inclusion_type VARCHAR(100),
ADD COLUMN IF NOT EXISTS inclusion_notes TEXT,
ADD COLUMN IF NOT EXISTS age INTEGER,
ADD COLUMN IF NOT EXISTS target_school VARCHAR(50) DEFAULT 'MONTE_TABOR'; -- MONTE_TABOR o NAZARET

-- Calcular la edad para los estudiantes existentes basado en birth_date
UPDATE students 
SET age = EXTRACT(YEAR FROM AGE(CURRENT_DATE, birth_date::date))
WHERE birth_date IS NOT NULL AND age IS NULL;

-- Agregar algunos datos de ejemplo para las categorías especiales
UPDATE students 
SET is_employee_child = TRUE,
    employee_parent_name = 'María González - Secretaria Académica'
WHERE id IN (SELECT id FROM students ORDER BY id LIMIT 2);

UPDATE students 
SET is_alumni_child = TRUE,
    alumni_parent_year = 1998
WHERE id IN (SELECT id FROM students ORDER BY id LIMIT 3 OFFSET 2);

UPDATE students 
SET is_inclusion_student = TRUE,
    inclusion_type = 'TEA (Trastorno del Espectro Autista)',
    inclusion_notes = 'Requiere apoyo especializado en comunicación y adaptaciones curriculares'
WHERE id IN (SELECT id FROM students ORDER BY id LIMIT 2 OFFSET 5);

-- Asignar colegio objetivo basado en el curso
UPDATE students 
SET target_school = CASE 
    WHEN grade_applied IN ('Prekinder', 'Kinder', '1° Básico', '2° Básico') THEN 'MONTE_TABOR'
    WHEN grade_applied IN ('3° Básico', '4° Básico', '1° Medio', '2° Medio') THEN 'NAZARET'
    ELSE 'MONTE_TABOR'
END;

-- Verificar los cambios
SELECT 
    first_name,
    last_name,
    age,
    grade_applied,
    target_school,
    is_employee_child,
    is_alumni_child,
    is_inclusion_student,
    employee_parent_name,
    alumni_parent_year,
    inclusion_type
FROM students 
WHERE id <= 10
ORDER BY id;