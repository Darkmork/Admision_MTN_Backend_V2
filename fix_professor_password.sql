-- Actualizar contraseña de profesor para testing
-- Hash BCrypt para "12345678"
UPDATE users 
SET password = '$2a$10$N9qo8uLOickgx2ZMRZoMye/YQ/OZAYL/YhQwOFGE4Fy0z2vgz4pSq'
WHERE email = 'alejandra.flores@mtn.cl';

-- Verificar el resultado
SELECT email, role, first_name, last_name, active 
FROM users 
WHERE email = 'alejandra.flores@mtn.cl';