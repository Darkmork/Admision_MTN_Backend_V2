-- Crear evaluaciones de muestra usando usuarios existentes

INSERT INTO evaluations (
    application_id, evaluator_id, evaluation_type, status, score, grade,
    evaluation_date, completion_date,
    academic_readiness, behavioral_assessment, emotional_maturity,
    social_skills_assessment, motivation_assessment, 
    strengths, areas_for_improvement, observations, recommendations,
    final_recommendation, created_at, updated_at
) 
SELECT 
    a.id as application_id,
    55 as evaluator_id, -- Roberto Silva (TEACHER - MATHEMATICS)
    'MATHEMATICS_EXAM' as evaluation_type,
    'COMPLETED' as status,
    85 + (a.id % 15) as score, -- Scores between 85-100
    s.grade_applied as grade,
    NOW() - INTERVAL '7' DAY as evaluation_date,
    NOW() - INTERVAL '2' DAY as completion_date,
    'Buen dominio de conceptos matemáticos básicos para su nivel' as academic_readiness,
    'Concentrado durante la evaluación, sigue instrucciones' as behavioral_assessment,
    'Manejo adecuado de situaciones de evaluación' as emotional_maturity,
    'Interactúa bien con el evaluador' as social_skills_assessment,
    'Muestra interés por actividades matemáticas' as motivation_assessment,
    'Razonamiento lógico, resolución de problemas' as strengths,
    'Mejorar velocidad de cálculo mental' as areas_for_improvement,
    'Evaluación completada satisfactoriamente' as observations,
    'Estudiante apto para el nivel solicitado' as recommendations,
    true as final_recommendation,
    NOW() - INTERVAL '7' DAY as created_at,
    NOW() - INTERVAL '2' DAY as updated_at
FROM applications a
JOIN students s ON a.student_id = s.id
WHERE a.id <= 10;

INSERT INTO evaluations (
    application_id, evaluator_id, evaluation_type, status, score, grade,
    evaluation_date, completion_date,
    academic_readiness, behavioral_assessment, emotional_maturity,
    social_skills_assessment, motivation_assessment,
    strengths, areas_for_improvement, observations, recommendations,
    final_recommendation, created_at, updated_at
)
SELECT 
    a.id as application_id,
    56 as evaluator_id, -- Carmen Morales (TEACHER - LANGUAGE)
    'LANGUAGE_EXAM' as evaluation_type,
    CASE 
        WHEN a.id % 3 = 0 THEN 'COMPLETED'
        WHEN a.id % 3 = 1 THEN 'IN_PROGRESS'
        ELSE 'PENDING'
    END as status,
    CASE WHEN a.id % 3 = 0 THEN 80 + (a.id % 20) ELSE NULL END as score,
    s.grade_applied as grade,
    NOW() - INTERVAL '5' DAY as evaluation_date,
    CASE WHEN a.id % 3 = 0 THEN NOW() - INTERVAL '1' DAY ELSE NULL END as completion_date,
    CASE WHEN a.id % 3 = 0 THEN 'Desarrollo apropiado del lenguaje oral y comprensión' ELSE NULL END as academic_readiness,
    CASE WHEN a.id % 3 = 0 THEN 'Participativo y expresivo durante actividades' ELSE NULL END as behavioral_assessment,
    CASE WHEN a.id % 3 = 0 THEN 'Confianza al comunicarse verbalmente' ELSE NULL END as emotional_maturity,
    CASE WHEN a.id % 3 = 0 THEN 'Comunicativo y respetuoso' ELSE NULL END as social_skills_assessment,
    CASE WHEN a.id % 3 = 0 THEN 'Disfruta actividades de lectura inicial' ELSE NULL END as motivation_assessment,
    CASE WHEN a.id % 3 = 0 THEN 'Comprensión lectora, expresión verbal' ELSE NULL END as strengths,
    CASE WHEN a.id % 3 = 0 THEN 'Ampliar vocabulario técnico' ELSE NULL END as areas_for_improvement,
    'Evaluación de lenguaje en desarrollo' as observations,
    CASE WHEN a.id % 3 = 0 THEN 'Nivel adecuado para ingreso' ELSE NULL END as recommendations,
    CASE WHEN a.id % 3 = 0 THEN (a.id % 4 != 3) ELSE NULL END as final_recommendation,
    NOW() - INTERVAL '5' DAY as created_at,
    NOW() - INTERVAL '1' DAY as updated_at
FROM applications a
JOIN students s ON a.student_id = s.id
WHERE a.id <= 12;

INSERT INTO evaluations (
    application_id, evaluator_id, evaluation_type, status, grade,
    evaluation_date, observations, created_at, updated_at
)
SELECT 
    a.id as application_id,
    52 as evaluator_id, -- Ana María Castillo (PSYCHOLOGIST)
    'PSYCHOLOGICAL_INTERVIEW' as evaluation_type,
    CASE 
        WHEN a.id % 4 = 0 THEN 'COMPLETED'
        WHEN a.id % 4 = 1 THEN 'IN_PROGRESS'  
        ELSE 'PENDING'
    END as status,
    s.grade_applied as grade,
    CASE 
        WHEN a.id % 4 = 0 THEN NOW() - INTERVAL '3' DAY
        WHEN a.id % 4 = 1 THEN NOW() - INTERVAL '1' DAY
        ELSE NOW() + INTERVAL '2' DAY
    END as evaluation_date,
    CASE 
        WHEN a.id % 4 = 0 THEN 'Entrevista psicológica completada. Desarrollo emocional apropiado.'
        WHEN a.id % 4 = 1 THEN 'Entrevista psicológica en desarrollo. Primera sesión realizada.'
        ELSE 'Entrevista psicológica programada.'
    END as observations,
    NOW() - INTERVAL '8' DAY as created_at,
    NOW() - INTERVAL '1' DAY as updated_at
FROM applications a
JOIN students s ON a.student_id = s.id
WHERE a.id <= 8;

INSERT INTO evaluations (
    application_id, evaluator_id, evaluation_type, status, grade,
    evaluation_date, observations, created_at, updated_at
)
SELECT 
    a.id as application_id,
    32 as evaluator_id, -- Carmen Sánchez (CYCLE_DIRECTOR)
    'CYCLE_DIRECTOR_INTERVIEW' as evaluation_type,
    CASE 
        WHEN a.id % 5 = 0 THEN 'COMPLETED'
        ELSE 'PENDING'
    END as status,
    s.grade_applied as grade,
    CASE 
        WHEN a.id % 5 = 0 THEN NOW() - INTERVAL '1' DAY
        ELSE NOW() + INTERVAL '3' DAY
    END as evaluation_date,
    CASE 
        WHEN a.id % 5 = 0 THEN 'Entrevista con director de ciclo completada. Familia comprometida.'
        ELSE 'Entrevista con director de ciclo programada.'
    END as observations,
    NOW() - INTERVAL '10' DAY as created_at,
    NOW() - INTERVAL '1' DAY as updated_at
FROM applications a
JOIN students s ON a.student_id = s.id
WHERE a.id <= 6;

-- Verificar datos creados
SELECT 
    e.evaluation_type,
    e.status,
    COUNT(*) as count,
    u.first_name || ' ' || u.last_name as evaluator_name
FROM evaluations e
JOIN users u ON e.evaluator_id = u.id
GROUP BY e.evaluation_type, e.status, u.first_name, u.last_name
ORDER BY e.evaluation_type, e.status;