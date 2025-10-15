// =====================================================================
// GUARDIAN VALIDATION MIDDLEWARE
// =====================================================================
// Purpose: Prevent orphaned applications by validating guardian existence
//          before creating applications
//
// Usage: Add to POST /api/applications route as middleware
//
// Safety: Validates guardian_id or creates guardian atomically
// =====================================================================

const { Pool } = require('pg');

// Database configuration (use same config as main service)
const dbPool = new Pool({
  host: process.env.DB_HOST || 'localhost',
  port: process.env.DB_PORT || 5432,
  database: process.env.DB_NAME || 'Admisión_MTN_DB',
  user: process.env.DB_USER || 'admin',
  password: process.env.DB_PASSWORD || 'admin123',
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000,
  query_timeout: 5000
});

/**
 * Validate that guardian exists in database before creating application
 *
 * Request body can contain:
 * - guardian_id: Existing guardian ID
 * - guardianName, guardianRut, guardianEmail, guardianPhone, guardianRelation: Create new guardian
 *
 * At least one of these options must be provided
 */
async function validateGuardianExists(req, res, next) {
  const client = await dbPool.connect();

  try {
    const { guardian_id, guardianName, guardianRut, guardianEmail, guardianPhone, guardianRelation } = req.body;

    // Option 1: guardian_id provided - verify it exists
    if (guardian_id) {
      const checkQuery = `
        SELECT id, full_name, email, rut, phone, relationship
        FROM guardians
        WHERE id = $1
      `;

      const result = await client.query(checkQuery, [guardian_id]);

      if (result.rows.length === 0) {
        return res.status(400).json({
          success: false,
          error: 'Guardian inválido',
          details: `El apoderado con ID ${guardian_id} no existe en el sistema`,
          errorCode: 'GUARDIAN_NOT_FOUND'
        });
      }

      // Attach guardian data to request for logging
      req.guardianData = result.rows[0];
      console.log(`[Guardian Validation] Verified guardian_id=${guardian_id} exists`);
      return next();
    }

    // Option 2: Guardian data provided - create new guardian
    if (guardianName) {
      // Validate required fields for guardian creation
      if (!guardianRut || !guardianEmail || !guardianPhone || !guardianRelation) {
        return res.status(400).json({
          success: false,
          error: 'Datos de apoderado incompletos',
          details: 'Para crear un apoderado se requiere: nombre, RUT, email, teléfono y relación',
          errorCode: 'GUARDIAN_INCOMPLETE_DATA',
          requiredFields: ['guardianName', 'guardianRut', 'guardianEmail', 'guardianPhone', 'guardianRelation']
        });
      }

      // Validate relationship is valid
      const validRelationships = ['PADRE', 'MADRE', 'ABUELO', 'TIO', 'HERMANO', 'TUTOR', 'OTRO'];
      if (!validRelationships.includes(guardianRelation)) {
        return res.status(400).json({
          success: false,
          error: 'Relación de apoderado inválida',
          details: `La relación debe ser una de: ${validRelationships.join(', ')}`,
          errorCode: 'GUARDIAN_INVALID_RELATIONSHIP',
          validOptions: validRelationships
        });
      }

      // Check if guardian already exists by RUT or email
      const existsQuery = `
        SELECT id, full_name, email, rut
        FROM guardians
        WHERE rut = $1 OR email = $2
        LIMIT 1
      `;

      const existsResult = await client.query(existsQuery, [guardianRut, guardianEmail]);

      if (existsResult.rows.length > 0) {
        const existing = existsResult.rows[0];
        console.log(`[Guardian Validation] Guardian already exists: ID=${existing.id}, RUT=${existing.rut}`);

        // Attach existing guardian ID to request body for application creation
        req.body.guardian_id = existing.id;
        req.guardianData = existing;
        req.guardianCreated = false;

        return next();
      }

      // Guardian doesn't exist - will be created by application service
      // Just validate the data format here
      console.log(`[Guardian Validation] New guardian will be created: ${guardianName} (${guardianRut})`);
      req.guardianCreated = true;
      return next();
    }

    // Option 3: No guardian data provided - ERROR
    return res.status(400).json({
      success: false,
      error: 'Apoderado requerido',
      details: 'Debe proporcionar guardian_id o los datos completos del apoderado (nombre, RUT, email, teléfono, relación)',
      errorCode: 'GUARDIAN_REQUIRED',
      requiredOptions: [
        { option: 'guardian_id', description: 'ID de apoderado existente' },
        {
          option: 'guardianData',
          description: 'Datos completos de nuevo apoderado',
          fields: ['guardianName', 'guardianRut', 'guardianEmail', 'guardianPhone', 'guardianRelation']
        }
      ]
    });

  } catch (error) {
    console.error('[Guardian Validation] Error:', error);
    return res.status(500).json({
      success: false,
      error: 'Error al validar apoderado',
      details: error.message,
      errorCode: 'GUARDIAN_VALIDATION_ERROR'
    });
  } finally {
    client.release();
  }
}

/**
 * Transaction wrapper for application creation with guardian validation
 *
 * Ensures that either:
 * 1. Guardian exists before application is created, OR
 * 2. Guardian is created first, then application is created
 *
 * Uses database transaction to ensure atomicity
 */
async function createApplicationWithGuardian(req, res, applicationCreateFn) {
  const client = await dbPool.connect();

  try {
    await client.query('BEGIN');

    let guardianId = req.body.guardian_id;

    // If guardian doesn't exist and data is provided, create it first
    if (!guardianId && req.body.guardianName) {
      const guardianQuery = `
        INSERT INTO guardians (
          full_name, rut, email, phone, relationship, created_at, updated_at
        ) VALUES ($1, $2, $3, $4, $5, NOW(), NOW())
        RETURNING id
      `;

      const guardianResult = await client.query(guardianQuery, [
        req.body.guardianName,
        req.body.guardianRut,
        req.body.guardianEmail,
        req.body.guardianPhone,
        req.body.guardianRelation || 'OTRO'
      ]);

      guardianId = guardianResult.rows[0].id;
      req.body.guardian_id = guardianId;

      console.log(`[Transaction] Created guardian with ID=${guardianId}`);
    }

    // Verify guardian_id is set
    if (!guardianId) {
      throw new Error('guardian_id no pudo ser establecido');
    }

    // Create application
    const application = await applicationCreateFn(client, req.body, guardianId);

    await client.query('COMMIT');

    console.log(`[Transaction] Successfully created application with guardian_id=${guardianId}`);

    return res.status(201).json({
      success: true,
      data: application,
      guardian: {
        id: guardianId,
        created: req.guardianCreated || false
      }
    });

  } catch (error) {
    await client.query('ROLLBACK');
    console.error('[Transaction] Error creating application:', error);

    return res.status(500).json({
      success: false,
      error: 'Error al crear aplicación',
      details: error.message,
      errorCode: 'APPLICATION_CREATE_ERROR'
    });
  } finally {
    client.release();
  }
}

module.exports = {
  validateGuardianExists,
  createApplicationWithGuardian,
  dbPool
};
