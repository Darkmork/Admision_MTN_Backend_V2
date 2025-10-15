#!/usr/bin/env node

/**
 * Script para obtener DATABASE_URL desde Railway
 * Alternativa cuando railway CLI no puede acceder directamente
 */

const axios = require('axios');

async function testDatabaseConnection() {
  console.log('🔍 Intentando restaurar base de datos...\n');

  // Intentar conectar directamente al servicio para verificar si la DB está vacía
  const baseUrl = 'https://admisionmtnbackendv2-production.up.railway.app';

  try {
    console.log('1. Verificando estado del servicio...');
    const healthCheck = await axios.get(`${baseUrl}/health`, { timeout: 5000 });
    console.log('✅ Servicio activo:', healthCheck.data.status);

    console.log('\n2. Intentando login (para verificar si la DB tiene datos)...');
    const loginResponse = await axios.post(`${baseUrl}/api/auth/login`, {
      email: 'jorge.gangale@mtn.cl',
      password: 'admin123'
    }, { timeout: 10000 });

    if (loginResponse.data.success) {
      console.log('✅ ¡La base de datos YA TIENE DATOS!');
      console.log('✅ Login exitoso para:', loginResponse.data.data.user.email);
      console.log('\n🎉 No necesitas restaurar - la BD ya está poblada con', loginResponse.data.data.user.role);
      return true;
    }
  } catch (error) {
    if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
      console.log('⏱️  Login timeout - la BD probablemente está vacía');
      console.log('❌ Necesitas restaurar la base de datos manualmente');
      return false;
    } else if (error.response) {
      console.log('❌ Error de autenticación:', error.response.data);
      if (error.response.status === 401) {
        console.log('✅ La BD tiene estructura pero el usuario no existe o contraseña incorrecta');
      }
      return false;
    } else {
      console.log('❌ Error de conexión:', error.message);
      return false;
    }
  }
}

async function checkUsers() {
  const baseUrl = 'https://admisionmtnbackendv2-production.up.railway.app';

  try {
    console.log('\n3. Intentando obtener roles (endpoint público)...');
    const rolesResponse = await axios.get(`${baseUrl}/api/users/roles`, { timeout: 5000 });
    console.log('✅ Roles disponibles:', rolesResponse.data);
    return true;
  } catch (error) {
    console.log('❌ No se pudo obtener roles - la BD probablemente está vacía');
    return false;
  }
}

async function main() {
  console.log('╔═══════════════════════════════════════════════════════════╗');
  console.log('║                                                           ║');
  console.log('║  🔍 VERIFICACIÓN DE BASE DE DATOS RAILWAY                 ║');
  console.log('║                                                           ║');
  console.log('╚═══════════════════════════════════════════════════════════╝\n');

  const hasData = await testDatabaseConnection();
  await checkUsers();

  if (!hasData) {
    console.log('\n╔═══════════════════════════════════════════════════════════╗');
    console.log('║                                                           ║');
    console.log('║  ⚠️  LA BASE DE DATOS NECESITA SER RESTAURADA             ║');
    console.log('║                                                           ║');
    console.log('╚═══════════════════════════════════════════════════════════╝\n');

    console.log('📋 Pasos para restaurar:\n');
    console.log('1. Ve a Railway Dashboard → Postgres → Variables → DATABASE_URL');
    console.log('2. Copia la URL completa (haz click en el ojo 👁️)');
    console.log('3. Ejecuta:\n');
    console.log('   export DATABASE_URL="postgresql://..."');
    console.log('   psql "$DATABASE_URL" < backups/admision_mtn_backup_20251013_082802.sql\n');
  } else {
    console.log('\n╔═══════════════════════════════════════════════════════════╗');
    console.log('║                                                           ║');
    console.log('║  ✅ LA BASE DE DATOS YA ESTÁ RESTAURADA                   ║');
    console.log('║                                                           ║');
    console.log('╚═══════════════════════════════════════════════════════════╝\n');

    console.log('🎉 ¡El sistema está listo para usar!\n');
    console.log('Siguiente paso: Ejecutar smoke tests');
    console.log('   ./railway-smoke-tests.sh\n');
  }
}

main().catch(err => {
  console.error('❌ Error inesperado:', err.message);
  process.exit(1);
});
