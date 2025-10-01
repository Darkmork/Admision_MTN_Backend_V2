-- Migrar tabla students de last_name a paternal_last_name y maternal_last_name
-- Fecha: 2025-08-22

-- Agregar las nuevas columnas
ALTER TABLE students 
ADD COLUMN paternal_last_name VARCHAR(255),
ADD COLUMN maternal_last_name VARCHAR(255);

-- Si hay datos existentes, podemos intentar separarlos (esto es opcional)
-- UPDATE students 
-- SET paternal_last_name = SPLIT_PART(last_name, ' ', 1),
--     maternal_last_name = SPLIT_PART(last_name, ' ', 2)
-- WHERE last_name IS NOT NULL;

-- Hacer las nuevas columnas NOT NULL después de migrar datos
ALTER TABLE students 
ALTER COLUMN paternal_last_name SET NOT NULL,
ALTER COLUMN maternal_last_name SET NOT NULL;

-- Eliminar la columna antigua
ALTER TABLE students DROP COLUMN last_name;

-- Agregar nueva columna school_applied si no existe
ALTER TABLE students ADD COLUMN IF NOT EXISTS school_applied VARCHAR(255);

-- Verificar el esquema final
\d students;