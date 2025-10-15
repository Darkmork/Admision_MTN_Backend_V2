const bcrypt = require('bcryptjs');
const { Pool } = require('pg');

const dbPool = new Pool({
  host: 'localhost',
  port: 5432,
  database: 'Admisión_MTN_DB',
  user: 'admin',
  password: 'admin123',
  max: 20
});

async function createFullAdmissionFlow() {
  const guardians = [
    {
      email: 'jorge.gangale@gmail.com',
      rut: '12345678-9',
      firstName: 'Jorge',
      lastName: 'Gangale',
      phone: '+56912345678',
      address: 'Calle Principal 123, Santiago',
      studentFirstName: 'María',
      studentLastName: 'Gangale',
      studentRut: '25678901-2',
      studentBirthDate: '2015-03-15',
      studentGrade: '4° Básico'
    },
    {
      email: 'jorge.gangale@bamail.udo.cl',
      rut: '23456789-0',
      firstName: 'Jorge',
      lastName: 'Gangale',
      phone: '+56923456789',
      address: 'Avenida Universidad 456, Valparaíso',
      studentFirstName: 'Pedro',
      studentLastName: 'Gangale',
      studentRut: '26789012-3',
      studentBirthDate: '2016-07-22',
      studentGrade: '3° Básico'
    },
    {
      email: 'jorge.gangale@ilcoud.com',
      rut: '34567890-1',
      firstName: 'Jorge',
      lastName: 'Gangale',
      phone: '+56934567890',
      address: 'Pasaje Cloud 789, Concepción',
      studentFirstName: 'Ana',
      studentLastName: 'Gangale',
      studentRut: '27890123-4',
      studentBirthDate: '2014-11-08',
      studentGrade: '5° Básico'
    }
  ];

  const password = '12345678';
  const hashedPassword = await bcrypt.hash(password, 10);

  console.log('🚀 Iniciando flujo completo de admisión para 3 estudiantes...\n');

  for (const guardian of guardians) {
    try {
      console.log(`\n📝 Procesando: ${guardian.studentFirstName} ${guardian.studentLastName} (${guardian.email})`);

      // 1. Crear Guardian
      const fullName = `${guardian.firstName} ${guardian.lastName} Torres`;
      const guardianResult = await dbPool.query(
        `INSERT INTO guardians (rut, email, full_name, phone, relationship, created_at, updated_at)
         VALUES ($1, $2, $3, $4, $5, NOW(), NOW())
         RETURNING id`,
        [guardian.rut, guardian.email, fullName, guardian.phone, 'PADRE']
      );
      const guardianId = guardianResult.rows[0].id;
      console.log(`✅ Guardian creado: ID ${guardianId}`);

      // 2. Crear Usuario
      await dbPool.query(
        `INSERT INTO users (email, password, role, active, rut, first_name, last_name, phone, email_verified, created_at, updated_at)
         VALUES ($1, $2, $3, $4, $5, $6, $7, $8, true, NOW(), NOW())`,
        [guardian.email, hashedPassword, 'APODERADO', true, guardian.rut, guardian.firstName, guardian.lastName, guardian.phone]
      );
      console.log(`✅ Usuario creado: ${guardian.email}`);

      // 3. Crear Estudiante
      const studentResult = await dbPool.query(
        `INSERT INTO students (rut, first_name, paternal_last_name, maternal_last_name, birth_date, grade_applied, current_school, school_applied, address, created_at, updated_at)
         VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, NOW(), NOW())
         RETURNING id`,
        [guardian.studentRut, guardian.studentFirstName, guardian.studentLastName, 'Torres', guardian.studentBirthDate, guardian.studentGrade, 'Colegio San José', 'Colegio Monte Tabor y Nazaret', guardian.address]
      );
      const studentId = studentResult.rows[0].id;
      console.log(`✅ Estudiante creado: ID ${studentId}`);

      // 4. Crear Aplicación
      const appResult = await dbPool.query(
        `INSERT INTO applications (student_id, guardian_id, status, application_year, submission_date, grade_applying_for, created_at, updated_at)
         VALUES ($1, $2, $3, $4, NOW(), $5, NOW(), NOW())
         RETURNING id`,
        [studentId, guardianId, 'UNDER_REVIEW', 2026, guardian.studentGrade]
      );
      const applicationId = appResult.rows[0].id;
      console.log(`✅ Aplicación creada: ID ${applicationId} - Status: UNDER_REVIEW`);

      // 5. Crear Documentos (3 documentos)
      const documents = [
        { type: 'BIRTH_CERTIFICATE', name: 'certificado_nacimiento.pdf' },
        { type: 'ACADEMIC_RECORDS', name: 'notas_anteriores.pdf' },
        { type: 'ID_CARD', name: 'cedula_identidad.pdf' }
      ];

      for (const doc of documents) {
        await dbPool.query(
          `INSERT INTO documents (application_id, document_type, file_name, file_path, uploaded_at, status)
           VALUES ($1, $2, $3, $4, NOW(), $5)`,
          [applicationId, doc.type, doc.name, `/uploads/${applicationId}/${doc.name}`, 'VERIFIED']
        );
      }
      console.log(`✅ Documentos cargados: 3 documentos verificados`);

      // 6. Crear Evaluaciones (3 evaluaciones con diferentes profesores)
      const evaluations = [
        { type: 'LANGUAGE_EXAM', evaluatorId: 30, subject: 'LANGUAGE', score: 28, maxScore: 32 }, // Patricia Silva
        { type: 'MATH_EXAM', evaluatorId: 47, subject: 'MATHEMATICS', score: 30, maxScore: 35 }, // Alejandra Flores
        { type: 'PSYCHOLOGIST_REPORT', evaluatorId: 33, subject: null, score: 25, maxScore: 30 } // Diego Fuentes
      ];

      for (const eval of evaluations) {
        await dbPool.query(
          `INSERT INTO evaluations (application_id, evaluation_type, evaluator_id, subject, score, max_score, observations, status, strengths, areas_for_improvement, recommendations, created_at, updated_at)
           VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, NOW(), NOW())`,
          [
            applicationId,
            eval.type,
            eval.evaluatorId,
            eval.subject,
            eval.score,
            eval.maxScore,
            `Evaluación completa para ${guardian.studentFirstName}`,
            'COMPLETED',
            'Excelente participación y comprensión de conceptos',
            'Mejorar velocidad de respuesta',
            'Estudiante con alto potencial académico'
          ]
        );
      }
      console.log(`✅ Evaluaciones completadas: 3 evaluaciones (Lenguaje, Matemáticas, Psicológica)`);

      // 7. Crear Entrevista
      const interviewDate = new Date();
      interviewDate.setDate(interviewDate.getDate() + 7); // 7 días después
      const dateStr = interviewDate.toISOString().split('T')[0];

      await dbPool.query(
        `INSERT INTO interviews (application_id, interviewer_id, scheduled_date, scheduled_time, duration_minutes, location, status, notes, created_at, updated_at)
         VALUES ($1, $2, $3, $4, $5, $6, $7, $8, NOW(), NOW())`,
        [
          applicationId,
          3, // Carlos Morales (COORDINATOR)
          dateStr,
          '10:00:00',
          45,
          'Sala de Entrevistas - Piso 2',
          'COMPLETED',
          'Entrevista exitosa. Familia comprometida con valores del colegio.'
        ]
      );
      console.log(`✅ Entrevista programada y completada: ${dateStr} 10:00`);

      // 8. Actualizar Application a ACCEPTED
      await dbPool.query(
        `UPDATE applications
         SET status = $1, decision_date = NOW(), decision_notes = $2, updated_at = NOW()
         WHERE id = $3`,
        ['ACCEPTED', `Estudiante admitido/a. Excelente perfil académico y familiar.`, applicationId]
      );
      console.log(`✅ Aplicación ACEPTADA - Estado final: ACCEPTED`);

      console.log(`\n🎉 Flujo completo para ${guardian.studentFirstName} ${guardian.studentLastName}: ✅ ADMITIDO/A\n`);
      console.log(`   Guardian ID: ${guardianId}`);
      console.log(`   Student ID: ${studentId}`);
      console.log(`   Application ID: ${applicationId}`);
      console.log(`   Email: ${guardian.email}`);
      console.log(`   Password: 12345678`);
      console.log(`   ---`);

    } catch (error) {
      console.error(`❌ Error procesando ${guardian.email}:`, error.message);
    }
  }

  await dbPool.end();
  console.log('\n✨✅ FLUJO COMPLETO TERMINADO PARA LOS 3 ESTUDIANTES!\n');
  console.log('📧 Credenciales de acceso:');
  console.log('   1. jorge.gangale@gmail.com / 12345678');
  console.log('   2. jorge.gangale@bamail.udo.cl / 12345678');
  console.log('   3. jorge.gangale@ilcoud.com / 12345678\n');
}

createFullAdmissionFlow().catch(console.error);
