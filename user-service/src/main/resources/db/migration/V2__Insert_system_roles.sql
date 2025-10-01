-- V2__Insert_system_roles.sql
-- Inserción de roles del sistema

-- Crear un usuario administrativo temporal para las referencias de creación
INSERT INTO users (
    id,
    email,
    first_name,
    last_name,
    enabled,
    email_verified,
    created_at,
    updated_at
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    'system@mtn.cl',
    'Sistema',
    'Automático',
    false,
    true,
    NOW(),
    NOW()
) ON CONFLICT (email) DO NOTHING;

-- Insertar roles del sistema
INSERT INTO roles (
    id,
    name,
    display_name,
    description,
    category,
    enabled,
    system_role,
    created_by,
    created_at,
    updated_at
) VALUES 
    -- Rol de Administrador del Sistema
    (
        uuid_generate_v4(),
        'ADMIN',
        'Administrador',
        'Administrador del sistema con acceso completo a todas las funcionalidades',
        'SYSTEM',
        true,
        true,
        '00000000-0000-0000-0000-000000000001',
        NOW(),
        NOW()
    ),
    
    -- Rol de Profesor/Docente
    (
        uuid_generate_v4(),
        'TEACHER',
        'Profesor',
        'Profesor con acceso a evaluaciones académicas según su especialización',
        'EDUCATIONAL',
        true,
        true,
        '00000000-0000-0000-0000-000000000001',
        NOW(),
        NOW()
    ),
    
    -- Rol de Coordinador Académico
    (
        uuid_generate_v4(),
        'COORDINATOR',
        'Coordinador Académico',
        'Coordinador académico con supervisión de evaluaciones y procesos educativos',
        'EDUCATIONAL',
        true,
        true,
        '00000000-0000-0000-0000-000000000001',
        NOW(),
        NOW()
    ),
    
    -- Rol de Psicólogo
    (
        uuid_generate_v4(),
        'PSYCHOLOGIST',
        'Psicólogo',
        'Psicólogo especialista en evaluaciones psicológicas y de desarrollo',
        'EDUCATIONAL',
        true,
        true,
        '00000000-0000-0000-0000-000000000001',
        NOW(),
        NOW()
    ),
    
    -- Rol de Director de Ciclo
    (
        uuid_generate_v4(),
        'CYCLE_DIRECTOR',
        'Director de Ciclo',
        'Director de ciclo responsable de entrevistas y decisiones de admisión',
        'ADMINISTRATIVE',
        true,
        true,
        '00000000-0000-0000-0000-000000000001',
        NOW(),
        NOW()
    ),
    
    -- Rol de Apoderado/Familia
    (
        uuid_generate_v4(),
        'APODERADO',
        'Apoderado',
        'Apoderado o familia responsable de postulaciones de estudiantes',
        'FAMILY',
        true,
        true,
        '00000000-0000-0000-0000-000000000001',
        NOW(),
        NOW()
    )

ON CONFLICT (name) DO UPDATE SET
    display_name = EXCLUDED.display_name,
    description = EXCLUDED.description,
    category = EXCLUDED.category,
    updated_at = NOW();

-- Verificar que todos los roles se crearon correctamente
DO $$
DECLARE
    role_count INTEGER;
    expected_roles TEXT[] := ARRAY['ADMIN', 'TEACHER', 'COORDINATOR', 'PSYCHOLOGIST', 'CYCLE_DIRECTOR', 'APODERADO'];
    role_name TEXT;
BEGIN
    -- Contar roles del sistema
    SELECT COUNT(*) INTO role_count 
    FROM roles 
    WHERE system_role = true;
    
    -- Verificar que tenemos el número esperado de roles
    IF role_count != 6 THEN
        RAISE EXCEPTION 'Error: Se esperaban 6 roles del sistema, pero se encontraron %', role_count;
    END IF;
    
    -- Verificar que cada rol esperado existe
    FOREACH role_name IN ARRAY expected_roles
    LOOP
        IF NOT EXISTS (SELECT 1 FROM roles WHERE name = role_name AND system_role = true) THEN
            RAISE EXCEPTION 'Error: Rol del sistema % no encontrado', role_name;
        END IF;
    END LOOP;
    
    RAISE NOTICE 'Verificación exitosa: % roles del sistema creados correctamente', role_count;
END $$;

-- Crear índice adicional para búsquedas por display_name
CREATE INDEX IF NOT EXISTS idx_roles_display_name ON roles(display_name);

-- Estadísticas finales
SELECT 
    category,
    COUNT(*) as role_count,
    COUNT(CASE WHEN enabled THEN 1 END) as enabled_count
FROM roles 
GROUP BY category 
ORDER BY category;