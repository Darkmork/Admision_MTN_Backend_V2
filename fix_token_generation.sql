-- Arreglar la función de generación de tokens y agregar datos de ejemplo

-- Función mejorada para generar token único sin gen_random_bytes
CREATE OR REPLACE FUNCTION generate_unique_token(length INTEGER DEFAULT 32)
RETURNS TEXT AS $$
DECLARE
    chars TEXT := 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    result TEXT := '';
    i INTEGER;
BEGIN
    FOR i IN 1..length LOOP
        result := result || substr(chars, floor(random() * length(chars) + 1)::integer, 1);
    END LOOP;
    RETURN result;
END;
$$ LANGUAGE plpgsql;

-- Insertar algunos datos de ejemplo para testing
INSERT INTO email_notifications (
    application_id, 
    recipient_email, 
    email_type, 
    subject, 
    student_name, 
    student_gender,
    target_school,
    tracking_token,
    response_required,
    response_token
) VALUES 
(1, 'familia01@test.cl', 'INTERVIEW_SCHEDULED', 'Entrevista Programada - Ana María (Colegio Monte Tabor)', 'Ana María', 'FEMALE', 'MONTE_TABOR', generate_unique_token(), TRUE, generate_unique_token()),
(2, 'familia02@test.cl', 'INTERVIEW_SCHEDULED', 'Entrevista Programada - Juan Carlos (Colegio Monte Tabor)', 'Juan Carlos', 'MALE', 'MONTE_TABOR', generate_unique_token(), TRUE, generate_unique_token()),
(3, 'familia03@test.cl', 'INTERVIEW_REMINDER', 'Recordatorio de Entrevista - María Elena (Colegio Nazaret)', 'María Elena', 'FEMALE', 'NAZARET', generate_unique_token(), FALSE, NULL);

-- Verificar los datos insertados
SELECT 
    id,
    email_type,
    student_name,
    student_gender,
    target_school,
    opened,
    responded,
    response_required,
    LENGTH(tracking_token) as token_length
FROM email_notifications 
ORDER BY id;

SELECT 'Email tracking system fixed and populated successfully' as status;