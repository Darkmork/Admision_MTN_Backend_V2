-- Recrear tabla students completamente para eliminar cualquier metadata corrupta
-- Fecha: 2025-08-22

-- 1. Respaldar datos existentes (si hay)
CREATE TABLE students_backup AS SELECT * FROM students;

-- 2. Eliminar todas las foreign keys que apuntan a students
ALTER TABLE applications DROP CONSTRAINT IF EXISTS fkbxjuiec753shgoyw6x0l8opn8;

-- 3. Eliminar la tabla original
DROP TABLE IF EXISTS students CASCADE;

-- 4. Recrear la tabla desde cero con el esquema correcto
CREATE TABLE students (
    id BIGSERIAL PRIMARY KEY,
    additional_notes TEXT,
    address VARCHAR(255) NOT NULL,
    birth_date DATE NOT NULL,
    created_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    current_school VARCHAR(255),
    email VARCHAR(255),
    first_name VARCHAR(255) NOT NULL,
    grade_applied VARCHAR(255) NOT NULL,
    maternal_last_name VARCHAR(255) NOT NULL,
    paternal_last_name VARCHAR(255) NOT NULL,
    rut VARCHAR(255) NOT NULL UNIQUE,
    school_applied VARCHAR(255),
    updated_at TIMESTAMP(6) WITHOUT TIME ZONE
);

-- 5. Restaurar foreign key constraint
ALTER TABLE applications 
ADD CONSTRAINT fkbxjuiec753shgoyw6x0l8opn8 
FOREIGN KEY (student_id) REFERENCES students(id);

-- 6. Verificar esquema final
\d students;