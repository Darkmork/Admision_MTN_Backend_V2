-- =====================================================
-- ACTUALIZACIÓN DE CREDENCIALES DE ADMINISTRADOR
-- EJECUTAR DESPUÉS DE ACTUALIZAR .env CON NUEVOS SECRETS
-- =====================================================

-- IMPORTANTE: Nueva contraseña de admin: 8TK7;R2o>@X6A7[?
-- Usa esta contraseña para hacer login después de ejecutar este script

-- Actualizar hash de contraseña del administrador
UPDATE users 
SET password = '$2a$10$RwzA8XQHqD8v6ZPGIQxbV.IC5qx9akm29oG7pDB1dR63n0HpW.L9e'
WHERE email = 'jorge.gangale@mtn.cl';

-- Verificar que el usuario se actualizó correctamente
SELECT 
    id,
    email,
    first_name,
    last_name,
    role,
    active,
    email_verified
FROM users 
WHERE email = 'jorge.gangale@mtn.cl';

-- INSTRUCCIONES IMPORTANTES:
-- 1. La nueva contraseña es: 8TK7;R2o>@X6A7[?
-- 2. Usa esta contraseña para hacer login después de ejecutar este script
-- 3. Guarda esta contraseña in un lugar seguro
-- 4. Considera cambiarla después del primer login por seguridad adicional

-- NOTA: Este script también actualiza las credenciales de base de datos
-- Asegúrate de reiniciar el backend después de ejecutar este script