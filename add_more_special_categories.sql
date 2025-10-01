-- Agregar más variedad a las categorías especiales

-- Más casos de hijos de funcionarios
UPDATE students 
SET is_employee_child = TRUE,
    employee_parent_name = 'Carlos Moreno - Director Académico'
WHERE id IN (SELECT id FROM students ORDER BY id LIMIT 2 OFFSET 10);

UPDATE students 
SET is_employee_child = TRUE,
    employee_parent_name = 'Patricia López - Profesora de Inglés'
WHERE id IN (SELECT id FROM students ORDER BY id LIMIT 1 OFFSET 15);

-- Más casos de hijos de exalumnos con diferentes años
UPDATE students 
SET is_alumni_child = TRUE,
    alumni_parent_year = 2005
WHERE id IN (SELECT id FROM students ORDER BY id LIMIT 2 OFFSET 7);

UPDATE students 
SET is_alumni_child = TRUE,
    alumni_parent_year = 1995
WHERE id IN (SELECT id FROM students ORDER BY id LIMIT 1 OFFSET 12);

UPDATE students 
SET is_alumni_child = TRUE,
    alumni_parent_year = 2010
WHERE id IN (SELECT id FROM students ORDER BY id LIMIT 1 OFFSET 17);

-- Más casos de inclusión con diferentes tipos
UPDATE students 
SET is_inclusion_student = TRUE,
    inclusion_type = 'Síndrome de Down',
    inclusion_notes = 'Requiere apoyo en lectoescritura y matemáticas básicas'
WHERE id IN (SELECT id FROM students ORDER BY id LIMIT 1 OFFSET 8);

UPDATE students 
SET is_inclusion_student = TRUE,
    inclusion_type = 'TDAH (Trastorno por Déficit de Atención)',
    inclusion_notes = 'Necesita estrategias de concentración y pausas frecuentes'
WHERE id IN (SELECT id FROM students ORDER BY id LIMIT 1 OFFSET 13);

UPDATE students 
SET is_inclusion_student = TRUE,
    inclusion_type = 'Discapacidad Visual Parcial',
    inclusion_notes = 'Requiere material en formato grande y ubicación preferencial'
WHERE id IN (SELECT id FROM students ORDER BY id LIMIT 1 OFFSET 18);

-- Verificar la distribución de categorías
SELECT 
    'Hijos de Funcionarios' as categoria,
    COUNT(*) as cantidad
FROM students 
WHERE is_employee_child = TRUE

UNION ALL

SELECT 
    'Hijos de Exalumnos' as categoria,
    COUNT(*) as cantidad
FROM students 
WHERE is_alumni_child = TRUE

UNION ALL

SELECT 
    'Alumnos de Inclusión' as categoria,
    COUNT(*) as cantidad
FROM students 
WHERE is_inclusion_student = TRUE

UNION ALL

SELECT 
    'Regulares' as categoria,
    COUNT(*) as cantidad
FROM students 
WHERE is_employee_child = FALSE 
  AND is_alumni_child = FALSE 
  AND is_inclusion_student = FALSE;

-- Mostrar algunos ejemplos de cada categoría
SELECT 
    first_name,
    paternal_last_name,
    maternal_last_name,
    age,
    grade_applied,
    target_school,
    CASE 
        WHEN is_employee_child THEN CONCAT('👨‍💼 Funcionario: ', employee_parent_name)
        WHEN is_alumni_child THEN CONCAT('🎓 Exalumno: ', alumni_parent_year)
        WHEN is_inclusion_student THEN CONCAT('♿ Inclusión: ', inclusion_type)
        ELSE '📚 Regular'
    END as categoria_principal
FROM students 
WHERE id <= 20
ORDER BY 
    is_employee_child DESC,
    is_alumni_child DESC,
    is_inclusion_student DESC,
    id;