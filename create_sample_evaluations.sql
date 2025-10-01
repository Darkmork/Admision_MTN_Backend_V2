-- Crear evaluaciones de muestra para el sistema de admisión

-- Primero obtener algunos IDs de applications y evaluadores existentes
DO $$
DECLARE
    app_id BIGINT;
    teacher_lang_id BIGINT;
    teacher_math_id BIGINT;
    teacher_eng_id BIGINT;
    psychologist_id BIGINT;
    director_id BIGINT;
    counter INTEGER := 0;
BEGIN
    -- Obtener IDs de evaluadores
    SELECT id INTO teacher_lang_id FROM users WHERE role = 'TEACHER_LANGUAGE' LIMIT 1;
    SELECT id INTO teacher_math_id FROM users WHERE role = 'TEACHER_MATHEMATICS' LIMIT 1;
    SELECT id INTO teacher_eng_id FROM users WHERE role = 'TEACHER_ENGLISH' LIMIT 1;
    SELECT id INTO psychologist_id FROM users WHERE role = 'PSYCHOLOGIST' LIMIT 1;
    SELECT id INTO director_id FROM users WHERE role = 'CYCLE_DIRECTOR' LIMIT 1;

    -- Si no existen evaluadores, crearlos
    IF teacher_lang_id IS NULL THEN
        INSERT INTO users (first_name, last_name, email, password, rut, phone, role, educational_level, subject, email_verified, active, created_at) 
        VALUES ('Ana', 'Martínez', 'ana.martinez@mtn.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '15555555-5', '+56915555555', 'TEACHER_LANGUAGE', 'ALL_LEVELS', 'LANGUAGE', true, true, NOW())
        RETURNING id INTO teacher_lang_id;
    END IF;

    IF teacher_math_id IS NULL THEN
        INSERT INTO users (first_name, last_name, email, password, rut, phone, role, educational_level, subject, email_verified, active, created_at) 
        VALUES ('Carlos', 'López', 'carlos.lopez@mtn.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '16666666-6', '+56916666666', 'TEACHER_MATHEMATICS', 'ALL_LEVELS', 'MATHEMATICS', true, true, NOW())
        RETURNING id INTO teacher_math_id;
    END IF;

    IF teacher_eng_id IS NULL THEN
        INSERT INTO users (first_name, last_name, email, password, rut, phone, role, educational_level, subject, email_verified, active, created_at) 
        VALUES ('Patricia', 'Johnson', 'patricia.johnson@mtn.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '17777777-7', '+56917777777', 'TEACHER_ENGLISH', 'ALL_LEVELS', 'ENGLISH', true, true, NOW())
        RETURNING id INTO teacher_eng_id;
    END IF;

    IF psychologist_id IS NULL THEN
        INSERT INTO users (first_name, last_name, email, password, rut, phone, role, educational_level, subject, email_verified, active, created_at) 
        VALUES ('Sofía', 'Ramírez', 'sofia.ramirez@mtn.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '18888888-8', '+56918888888', 'PSYCHOLOGIST', 'ALL_LEVELS', 'PSYCHOLOGY', true, true, NOW())
        RETURNING id INTO psychologist_id;
    END IF;

    IF director_id IS NULL THEN
        INSERT INTO users (first_name, last_name, email, password, rut, phone, role, educational_level, subject, email_verified, active, created_at) 
        VALUES ('Roberto', 'González', 'roberto.gonzalez@mtn.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq', '19999999-9', '+56919999999', 'CYCLE_DIRECTOR', 'ALL_LEVELS', 'GENERAL', true, true, NOW())
        RETURNING id INTO director_id;
    END IF;

    -- Crear evaluaciones para las primeras 15 applications
    FOR app_id IN (SELECT id FROM applications LIMIT 15)
    LOOP
        counter := counter + 1;
        
        -- Evaluación de Matemáticas (completada)
        INSERT INTO evaluations (
            application_id, evaluator_id, evaluation_type, status, score, grade,
            evaluation_date, completion_date,
            academic_readiness, behavioral_assessment, emotional_maturity,
            social_skills_assessment, motivation_assessment, family_support_assessment,
            integration_potential, strengths, areas_for_improvement,
            observations, recommendations, final_recommendation,
            created_at, updated_at
        ) VALUES (
            app_id, teacher_math_id, 'MATHEMATICS_EXAM', 'COMPLETED',
            CASE 
                WHEN counter % 4 = 0 THEN 95 + (counter % 5)
                WHEN counter % 3 = 0 THEN 85 + (counter % 10)
                ELSE 75 + (counter % 15)
            END,
            CASE 
                WHEN counter <= 5 THEN 'Prekinder'
                WHEN counter <= 10 THEN 'Kinder'
                ELSE '1° Básico'
            END,
            NOW() - INTERVAL '7' DAY,
            NOW() - INTERVAL '3' DAY,
            CASE counter % 3
                WHEN 0 THEN 'Excelente dominio de conceptos numéricos básicos. Reconoce patrones y secuencias.'
                WHEN 1 THEN 'Buen manejo de operaciones básicas. Comprende conceptos de cantidad y orden.'
                ELSE 'Adecuado nivel para su edad. Necesita refuerzo en algunos conceptos.'
            END,
            CASE counter % 3
                WHEN 0 THEN 'Muy concentrado durante la evaluación. Sigue instrucciones correctamente.'
                WHEN 1 THEN 'Atento y participativo. Demuestra interés por aprender.'
                ELSE 'Algo inquieto pero logra completar las actividades.'
            END,
            CASE counter % 3
                WHEN 0 THEN 'Maneja bien la frustración. Persistente ante dificultades.'
                WHEN 1 THEN 'Equilibrado emocionalmente. Adecuado control de impulsos.'
                ELSE 'Necesita apoyo ocasional para manejar la ansiedad.'
            END,
            CASE counter % 3
                WHEN 0 THEN 'Excelentes habilidades sociales. Interactúa bien con evaluador.'
                WHEN 1 THEN 'Buena comunicación. Respeta turnos de conversación.'
                ELSE 'Algo tímido inicialmente pero se adapta bien.'
            END,
            CASE counter % 3
                WHEN 0 THEN 'Alta motivación por aprender. Muestra curiosidad genuina.'
                WHEN 1 THEN 'Motivación adecuada. Disfruta los desafíos apropiados.'
                ELSE 'Motivación variable. Responde bien a estímulos positivos.'
            END,
            CASE counter % 3
                WHEN 0 THEN 'Familia muy comprometida con la educación del menor.'
                WHEN 1 THEN 'Buen apoyo familiar. Padres involucrados en proceso.'
                ELSE 'Apoyo familiar presente. Pueden beneficiarse de orientación.'
            END,
            CASE counter % 3
                WHEN 0 THEN 'Excelente potencial de integración al ambiente escolar.'
                WHEN 1 THEN 'Buena capacidad de adaptación. Se integrará sin dificultades.'
                ELSE 'Necesitará tiempo de adaptación pero logrará integrarse.'
            END,
            'Razonamiento lógico, concentración, seguimiento de instrucciones',
            'Reforzar conceptos de geometría básica, mejorar velocidad de cálculo',
            'Estudiante con buen potencial matemático. Se recomienda continuar con refuerzo positivo.',
            CASE counter % 3
                WHEN 0 THEN 'Recomendado para admisión. Excelente rendimiento.'
                WHEN 1 THEN 'Recomendado para admisión. Buen nivel académico.'
                ELSE 'Recomendado con seguimiento. Potencial de mejora.'
            END,
            counter % 3 = 0 OR counter % 3 = 1,
            NOW() - INTERVAL '7' DAY,
            NOW() - INTERVAL '3' DAY
        );

        -- Evaluación de Lenguaje (algunas completadas, otras en progreso)
        INSERT INTO evaluations (
            application_id, evaluator_id, evaluation_type, status, score, grade,
            evaluation_date, completion_date,
            academic_readiness, behavioral_assessment, emotional_maturity,
            social_skills_assessment, motivation_assessment,
            strengths, areas_for_improvement, observations, recommendations,
            final_recommendation, created_at, updated_at
        ) VALUES (
            app_id, teacher_lang_id, 'LANGUAGE_EXAM',
            CASE WHEN counter % 3 = 0 THEN 'COMPLETED' ELSE 'IN_PROGRESS' END,
            CASE WHEN counter % 3 = 0 THEN 80 + (counter % 20) ELSE NULL END,
            CASE 
                WHEN counter <= 5 THEN 'Prekinder'
                WHEN counter <= 10 THEN 'Kinder'
                ELSE '1° Básico'
            END,
            NOW() - INTERVAL '5' DAY,
            CASE WHEN counter % 3 = 0 THEN NOW() - INTERVAL '2' DAY ELSE NULL END,
            CASE 
                WHEN counter % 3 = 0 THEN 'Buen desarrollo del lenguaje oral. Vocabulario apropiado para su edad.'
                ELSE NULL 
            END,
            CASE 
                WHEN counter % 3 = 0 THEN 'Participativo y expresivo. Mantiene atención durante actividades.'
                ELSE NULL 
            END,
            CASE 
                WHEN counter % 3 = 0 THEN 'Confiado al expresarse. Maneja bien situaciones de evaluación.'
                ELSE NULL 
            END,
            CASE 
                WHEN counter % 3 = 0 THEN 'Comunicativo y respetuoso. Establece buen rapport.'
                ELSE NULL 
            END,
            CASE 
                WHEN counter % 3 = 0 THEN 'Disfruta actividades de lectura y escritura inicial.'
                ELSE NULL 
            END,
            CASE 
                WHEN counter % 3 = 0 THEN 'Comprensión oral, expresión verbal, reconocimiento de letras'
                ELSE NULL 
            END,
            CASE 
                WHEN counter % 3 = 0 THEN 'Desarrollar escritura de su nombre, ampliar vocabulario'
                ELSE NULL 
            END,
            CASE 
                WHEN counter % 3 = 0 THEN 'Evaluación en proceso. Se observa buen desempeño inicial.'
                ELSE 'Evaluación programada y en desarrollo.'
            END,
            CASE 
                WHEN counter % 3 = 0 THEN 'Continuar con estimulación del lenguaje. Potencial prometedor.'
                ELSE NULL 
            END,
            CASE WHEN counter % 3 = 0 THEN (counter % 4 != 3) ELSE NULL END,
            NOW() - INTERVAL '5' DAY,
            CASE WHEN counter % 3 = 0 THEN NOW() - INTERVAL '2' DAY ELSE NOW() - INTERVAL '1' DAY END
        );

        -- Evaluación Psicológica (pendientes y algunas en progreso)
        IF counter <= 8 THEN
            INSERT INTO evaluations (
                application_id, evaluator_id, evaluation_type, status, grade,
                evaluation_date, 
                observations, created_at, updated_at
            ) VALUES (
                app_id, psychologist_id, 'PSYCHOLOGICAL_INTERVIEW',
                CASE WHEN counter % 4 = 0 THEN 'IN_PROGRESS' ELSE 'PENDING' END,
                CASE 
                    WHEN counter <= 5 THEN 'Prekinder'
                    WHEN counter <= 10 THEN 'Kinder'
                    ELSE '1° Básico'
                END,
                CASE WHEN counter % 4 = 0 THEN NOW() - INTERVAL '2' DAY ELSE NOW() + INTERVAL '3' DAY END,
                CASE 
                    WHEN counter % 4 = 0 THEN 'Entrevista psicológica en desarrollo. Primera sesión completada.'
                    ELSE 'Entrevista psicológica programada.'
                END,
                NOW() - INTERVAL '8' DAY,
                NOW() - INTERVAL '1' DAY
            );
        END IF;

        -- Entrevista de Director (algunas programadas)
        IF counter <= 5 THEN
            INSERT INTO evaluations (
                application_id, evaluator_id, evaluation_type, status, grade,
                evaluation_date,
                observations, created_at, updated_at
            ) VALUES (
                app_id, director_id, 'CYCLE_DIRECTOR_INTERVIEW', 'PENDING',
                CASE 
                    WHEN counter <= 5 THEN 'Prekinder'
                    WHEN counter <= 10 THEN 'Kinder'
                    ELSE '1° Básico'
                END,
                NOW() + INTERVAL '5' DAY,
                'Entrevista con director de ciclo programada.',
                NOW() - INTERVAL '10' DAY,
                NOW() - INTERVAL '1' DAY
            );
        END IF;
    END LOOP;

    RAISE NOTICE 'Se han creado evaluaciones de muestra para % aplicaciones', counter;
END $$;

-- Verificar los datos creados
SELECT 
    COUNT(*) as total_evaluations,
    evaluation_type,
    status,
    COUNT(*) 
FROM evaluations 
GROUP BY evaluation_type, status 
ORDER BY evaluation_type, status;