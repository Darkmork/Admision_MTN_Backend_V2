-- Crear un usuario admin de prueba con credenciales conocidas
INSERT INTO users (
    first_name, 
    last_name, 
    email, 
    password,  
    rut, 
    phone, 
    role, 
    email_verified, 
    active, 
    created_at,
    updated_at
) VALUES (
    'Admin', 
    'Sistema', 
    'admin@test.com', 
    '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq',  -- admin123
    '11111111-1', 
    '+56911111111', 
    'ADMIN', 
    true, 
    true, 
    NOW(),
    NOW()
) ON CONFLICT (email) DO UPDATE SET
    password = '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq',
    active = true;

-- Verificar el usuario
SELECT email, role, active, email_verified, length(password) as pwd_len 
FROM users 
WHERE email = 'admin@test.com';