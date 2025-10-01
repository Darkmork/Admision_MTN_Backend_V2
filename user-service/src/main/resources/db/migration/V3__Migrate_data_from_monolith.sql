-- V3__Migrate_data_from_monolith.sql
-- Migración de datos del monolito al microservicio de usuarios
-- IMPORTANTE: Este script debe ejecutarse con acceso a la base de datos del monolito

-- Crear conexión externa a la base de datos del monolito (requiere configuración previa)
-- La extensión postgres_fdw debe estar instalada y configurada

DO $$
DECLARE
    monolith_db_exists BOOLEAN := false;
    migration_count INTEGER := 0;
    role_mapping RECORD;
    user_record RECORD;
    admin_role_id UUID;
    teacher_role_id UUID;
    coordinator_role_id UUID;
    psychologist_role_id UUID;
    cycle_director_role_id UUID;
    apoderado_role_id UUID;
BEGIN
    -- Verificar si existe acceso al monolito (simulación)
    -- En producción esto sería una conexión FDW real
    RAISE NOTICE 'Iniciando migración de datos desde el monolito...';
    
    -- Obtener IDs de roles para mapeo
    SELECT id INTO admin_role_id FROM roles WHERE name = 'ADMIN';
    SELECT id INTO teacher_role_id FROM roles WHERE name = 'TEACHER';
    SELECT id INTO coordinator_role_id FROM roles WHERE name = 'COORDINATOR';
    SELECT id INTO psychologist_role_id FROM roles WHERE name = 'PSYCHOLOGIST';
    SELECT id INTO cycle_director_role_id FROM roles WHERE name = 'CYCLE_DIRECTOR';
    SELECT id INTO apoderado_role_id FROM roles WHERE name = 'APODERADO';
    
    -- Crear tabla temporal para mapeo de usuarios del monolito
    CREATE TEMP TABLE temp_monolith_users (
        old_id BIGINT,
        new_id UUID DEFAULT uuid_generate_v4(),
        email VARCHAR(255),
        username VARCHAR(100),
        first_name VARCHAR(100),
        last_name VARCHAR(100),
        rut VARCHAR(12),
        phone VARCHAR(20),
        role VARCHAR(50),
        educational_level VARCHAR(50),
        subject VARCHAR(50),
        enabled BOOLEAN,
        email_verified BOOLEAN,
        last_login_at TIMESTAMP WITH TIME ZONE,
        created_at TIMESTAMP WITH TIME ZONE
    );
    
    -- Insertar datos simulados del monolito para demostración
    -- En producción, esto sería una consulta SELECT FROM foreign_table
    INSERT INTO temp_monolith_users (
        old_id, email, username, first_name, last_name, rut, phone, 
        role, educational_level, subject, enabled, email_verified, 
        last_login_at, created_at
    ) VALUES
        -- Usuario Administrador Principal
        (1, 'jorge.gangale@mtn.cl', 'jorge.gangale', 'Jorge', 'Gangale', '12345678-9', '+56912345678',
         'ADMIN', 'ALL_LEVELS', 'ALL_SUBJECTS', true, true, NOW() - INTERVAL '1 day', NOW() - INTERVAL '30 days'),
         
        -- Profesores de ejemplo
        (2, 'maria.gonzalez@mtn.cl', 'maria.gonzalez', 'María', 'González', '11111111-1', '+56911111111',
         'TEACHER', 'PRESCHOOL', 'GENERAL', true, true, NOW() - INTERVAL '2 days', NOW() - INTERVAL '25 days'),
         
        (3, 'roberto.silva@mtn.cl', 'roberto.silva', 'Roberto', 'Silva', '22222222-2', '+56922222222',
         'TEACHER', 'BASIC', 'MATHEMATICS', true, true, NOW() - INTERVAL '1 day', NOW() - INTERVAL '20 days'),
         
        (4, 'carmen.morales@mtn.cl', 'carmen.morales', 'Carmen', 'Morales', '33333333-3', '+56933333333',
         'TEACHER', 'HIGH_SCHOOL', 'LANGUAGE', true, true, NOW() - INTERVAL '3 days', NOW() - INTERVAL '15 days'),
         
        -- Coordinador
        (5, 'marcela.coord@mtn.cl', 'marcela.coordinadora', 'Marcela', 'Coordinadora', '44444444-4', '+56944444444',
         'COORDINATOR', 'ALL_LEVELS', 'MATHEMATICS', true, true, NOW() - INTERVAL '1 day', NOW() - INTERVAL '10 days'),
         
        -- Psicólogo
        (6, 'pablo.psicologo@mtn.cl', 'pablo.psicologo', 'Pablo', 'Psicólogo', '55555555-5', '+56955555555',
         'PSYCHOLOGIST', 'ALL_LEVELS', 'GENERAL', true, true, NOW() - INTERVAL '4 days', NOW() - INTERVAL '8 days'),
         
        -- Director de Ciclo
        (7, 'ana.directora@mtn.cl', 'ana.directora', 'Ana', 'Directora', '66666666-6', '+56966666666',
         'CYCLE_DIRECTOR', 'HIGH_SCHOOL', 'GENERAL', true, true, NOW() - INTERVAL '2 days', NOW() - INTERVAL '12 days'),
         
        -- Apoderados de ejemplo
        (8, 'familia01@test.cl', 'familia01', 'Carlos', 'Pérez', '77777777-7', '+56977777777',
         'APODERADO', NULL, NULL, true, true, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
         
        (9, 'familia02@test.cl', 'familia02', 'Elena', 'Rodríguez', '88888888-8', '+56988888888',
         'APODERADO', NULL, NULL, true, true, NOW() - INTERVAL '3 days', NOW() - INTERVAL '7 days'),
         
        (10, 'familia03@test.cl', 'familia03', 'Diego', 'Martínez', '99999999-9', '+56999999999',
         'APODERADO', NULL, NULL, true, false, NULL, NOW() - INTERVAL '2 days');
    
    -- Migrar usuarios a la tabla principal
    INSERT INTO users (
        id, email, username, first_name, last_name, rut, phone,
        educational_level, subject, enabled, email_verified, 
        last_login_at, created_at, updated_at, created_by
    )
    SELECT 
        new_id, email, username, first_name, last_name, rut, phone,
        educational_level, subject, enabled, email_verified,
        last_login_at, created_at, NOW(), 
        '00000000-0000-0000-0000-000000000001' -- Usuario sistema
    FROM temp_monolith_users
    ON CONFLICT (email) DO UPDATE SET
        username = EXCLUDED.username,
        first_name = EXCLUDED.first_name,
        last_name = EXCLUDED.last_name,
        rut = EXCLUDED.rut,
        phone = EXCLUDED.phone,
        educational_level = EXCLUDED.educational_level,
        subject = EXCLUDED.subject,
        enabled = EXCLUDED.enabled,
        email_verified = EXCLUDED.email_verified,
        last_login_at = EXCLUDED.last_login_at,
        updated_at = NOW();
    
    GET DIAGNOSTICS migration_count = ROW_COUNT;
    RAISE NOTICE 'Migrados % usuarios desde el monolito', migration_count;
    
    -- Migrar asignaciones de roles
    FOR user_record IN SELECT * FROM temp_monolith_users LOOP
        -- Determinar el role_id basado en el rol del monolito
        CASE user_record.role
            WHEN 'ADMIN' THEN
                INSERT INTO user_roles (user_id, role_id, assigned_by, assigned_at, active, comments)
                VALUES (user_record.new_id, admin_role_id, '00000000-0000-0000-0000-000000000001', 
                       user_record.created_at, true, 'Migrado desde monolito')
                ON CONFLICT (user_id, role_id, active) DO NOTHING;
                
            WHEN 'TEACHER' THEN
                INSERT INTO user_roles (user_id, role_id, assigned_by, assigned_at, active, comments)
                VALUES (user_record.new_id, teacher_role_id, '00000000-0000-0000-0000-000000000001', 
                       user_record.created_at, true, 'Migrado desde monolito')
                ON CONFLICT (user_id, role_id, active) DO NOTHING;
                
            WHEN 'COORDINATOR' THEN
                INSERT INTO user_roles (user_id, role_id, assigned_by, assigned_at, active, comments)
                VALUES (user_record.new_id, coordinator_role_id, '00000000-0000-0000-0000-000000000001', 
                       user_record.created_at, true, 'Migrado desde monolito')
                ON CONFLICT (user_id, role_id, active) DO NOTHING;
                
            WHEN 'PSYCHOLOGIST' THEN
                INSERT INTO user_roles (user_id, role_id, assigned_by, assigned_at, active, comments)
                VALUES (user_record.new_id, psychologist_role_id, '00000000-0000-0000-0000-000000000001', 
                       user_record.created_at, true, 'Migrado desde monolito')
                ON CONFLICT (user_id, role_id, active) DO NOTHING;
                
            WHEN 'CYCLE_DIRECTOR' THEN
                INSERT INTO user_roles (user_id, role_id, assigned_by, assigned_at, active, comments)
                VALUES (user_record.new_id, cycle_director_role_id, '00000000-0000-0000-0000-000000000001', 
                       user_record.created_at, true, 'Migrado desde monolito')
                ON CONFLICT (user_id, role_id, active) DO NOTHING;
                
            WHEN 'APODERADO' THEN
                INSERT INTO user_roles (user_id, role_id, assigned_by, assigned_at, active, comments)
                VALUES (user_record.new_id, apoderado_role_id, '00000000-0000-0000-0000-000000000001', 
                       user_record.created_at, true, 'Migrado desde monolito')
                ON CONFLICT (user_id, role_id, active) DO NOTHING;
                
            ELSE
                RAISE NOTICE 'Rol desconocido para usuario %: %', user_record.email, user_record.role;
        END CASE;
    END LOOP;
    
    -- Crear evento de migración completada
    INSERT INTO domain_events (
        aggregate_type,
        aggregate_id,
        event_type,
        event_data,
        correlation_id,
        user_id
    ) VALUES (
        'USER_MIGRATION',
        uuid_generate_v4(),
        'MIGRATION_COMPLETED',
        jsonb_build_object(
            'migratedUsers', migration_count,
            'migrationDate', NOW(),
            'source', 'monolith_database',
            'version', '1.0'
        ),
        uuid_generate_v4(),
        '00000000-0000-0000-0000-000000000001'
    );
    
    -- Estadísticas finales
    RAISE NOTICE '=== RESUMEN DE MIGRACIÓN ===';
    RAISE NOTICE 'Total usuarios migrados: %', migration_count;
    
    -- Mostrar estadísticas por rol
    FOR role_mapping IN 
        SELECT r.name, r.display_name, COUNT(ur.user_id) as user_count
        FROM roles r
        LEFT JOIN user_roles ur ON r.id = ur.role_id AND ur.active = true
        WHERE r.system_role = true
        GROUP BY r.name, r.display_name
        ORDER BY user_count DESC
    LOOP
        RAISE NOTICE 'Rol %: % usuarios', role_mapping.display_name, role_mapping.user_count;
    END LOOP;
    
    RAISE NOTICE 'Migración completada exitosamente';
    
END $$;

-- Verificaciones post-migración
DO $$
DECLARE
    total_users INTEGER;
    total_roles INTEGER;
    total_assignments INTEGER;
BEGIN
    SELECT COUNT(*) INTO total_users FROM users WHERE id != '00000000-0000-0000-0000-000000000001';
    SELECT COUNT(*) INTO total_roles FROM roles WHERE system_role = true;
    SELECT COUNT(*) INTO total_assignments FROM user_roles WHERE active = true;
    
    RAISE NOTICE '=== VERIFICACIÓN POST-MIGRACIÓN ===';
    RAISE NOTICE 'Total usuarios (excluyendo sistema): %', total_users;
    RAISE NOTICE 'Total roles del sistema: %', total_roles;
    RAISE NOTICE 'Total asignaciones de roles activas: %', total_assignments;
    
    -- Verificar integridad referencial
    IF EXISTS (
        SELECT 1 FROM user_roles ur 
        LEFT JOIN users u ON ur.user_id = u.id 
        LEFT JOIN roles r ON ur.role_id = r.id 
        WHERE u.id IS NULL OR r.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Error: Problemas de integridad referencial detectados en user_roles';
    END IF;
    
    RAISE NOTICE 'Verificación de integridad: EXITOSA';
END $$;