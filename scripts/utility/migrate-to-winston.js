const fs = require('fs');
const path = require('path');

// Archivos a migrar
const filesToMigrate = [
  'mock-user-service.js',
  'mock-application-service.js',
  'mock-evaluation-service.js',
  'mock-notification-service.js',
  'mock-dashboard-service.js',
  'mock-guardian-service.js',
];

function migrateFile(filename) {
  const filepath = path.join(__dirname, filename);

  console.log(`\n📝 Migrando ${filename}...`);

  // Leer archivo
  let content = fs.readFileSync(filepath, 'utf8');
  const originalContent = content;

  // Extraer nombre del servicio del nombre del archivo
  const serviceName = filename.replace('mock-', '').replace('-service.js', '');

  // Contador de reemplazos
  let count = 0;

  // Paso 1: Agregar import de logger al inicio (después de requires existentes)
  const hasLogger = content.includes("const createLogger = require('./logger')");
  if (!hasLogger) {
    // Encontrar la última línea de require
    const requireLines = content.match(/const .+ = require\(.+\);/g) || [];
    if (requireLines.length > 0) {
      const lastRequire = requireLines[requireLines.length - 1];
      content = content.replace(
        lastRequire,
        `${lastRequire}\nconst createLogger = require('./logger');\nconst logger = createLogger('${serviceName}-service');`
      );
    }
  }

  // Paso 2: Reemplazar console.log por logger.info
  content = content.replace(/console\.log\(/g, (match) => {
    count++;
    return 'logger.info(';
  });

  // Paso 3: Reemplazar console.error por logger.error
  content = content.replace(/console\.error\(/g, (match) => {
    count++;
    return 'logger.error(';
  });

  // Paso 4: Reemplazar console.warn por logger.warn
  content = content.replace(/console\.warn\(/g, (match) => {
    count++;
    return 'logger.warn(';
  });

  // Solo escribir si hubo cambios
  if (content !== originalContent) {
    fs.writeFileSync(filepath, content, 'utf8');
    console.log(`✅ ${filename}: ${count} console.* reemplazados`);
  } else {
    console.log(`⚠️  ${filename}: Sin cambios`);
  }

  return count;
}

// Ejecutar migración
console.log('🚀 Iniciando migración a Winston Logger...\n');
let totalReplaced = 0;

filesToMigrate.forEach(file => {
  try {
    totalReplaced += migrateFile(file);
  } catch (error) {
    console.error(`❌ Error en ${file}:`, error.message);
  }
});

console.log(`\n✅ Migración completada: ${totalReplaced} statements reemplazados en total`);
console.log('\n📋 Próximos pasos:');
console.log('1. Revisar los cambios con: git diff');
console.log('2. Probar servicios: ./start-microservices-gateway.sh');
console.log('3. Verificar logs en: /tmp/*-service.log');
