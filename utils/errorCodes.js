/**
 * Standardized Error Codes System
 *
 * Centralized error code management for the Sistema de Admisión MTN.
 * Each error has a unique code, message, and HTTP status code.
 *
 * Usage:
 *   const { Errors, createError, createErrorResponse } = require('./utils/errorCodes');
 *
 *   // Method 1: Create error object
 *   const error = createError('AUTH_001', { email: 'user@example.com' });
 *   res.status(error.statusCode).json(error);
 *
 *   // Method 2: Create error response directly
 *   return createErrorResponse(res, 'AUTH_001', { email: 'user@example.com' });
 *
 *   // Method 3: Use error object directly
 *   return res.status(401).json({
 *     success: false,
 *     error: Errors.AUTH_001.message,
 *     errorCode: 'AUTH_001'
 *   });
 */

// ============================================================================
// ERROR CODE DEFINITIONS
// ============================================================================

const Errors = {
  // ========== AUTHENTICATION ERRORS (AUTH_xxx) ==========
  AUTH_001: {
    code: 'AUTH_001',
    message: 'Credenciales inválidas',
    statusCode: 401,
    description: 'Email o contraseña incorrectos'
  },
  AUTH_002: {
    code: 'AUTH_002',
    message: 'Token expirado',
    statusCode: 401,
    description: 'El token JWT ha expirado, debe iniciar sesión nuevamente'
  },
  AUTH_003: {
    code: 'AUTH_003',
    message: 'Token inválido',
    statusCode: 401,
    description: 'El token JWT es inválido o está malformado'
  },
  AUTH_004: {
    code: 'AUTH_004',
    message: 'Token no proporcionado',
    statusCode: 401,
    description: 'No se proporcionó un token de autenticación'
  },
  AUTH_005: {
    code: 'AUTH_005',
    message: 'Acceso denegado',
    statusCode: 403,
    description: 'No tiene permisos para acceder a este recurso'
  },
  AUTH_006: {
    code: 'AUTH_006',
    message: 'Usuario no encontrado',
    statusCode: 404,
    description: 'El usuario no existe en el sistema'
  },
  AUTH_007: {
    code: 'AUTH_007',
    message: 'Email ya registrado',
    statusCode: 409,
    description: 'El email ya está en uso por otra cuenta'
  },

  // ========== APPLICATION ERRORS (APP_xxx) ==========
  APP_001: {
    code: 'APP_001',
    message: 'Postulación no encontrada',
    statusCode: 404,
    description: 'La postulación solicitada no existe'
  },
  APP_002: {
    code: 'APP_002',
    message: 'Apoderado requerido',
    statusCode: 400,
    description: 'Debe especificar un apoderado para la postulación'
  },
  APP_003: {
    code: 'APP_003',
    message: 'Estudiante requerido',
    statusCode: 400,
    description: 'Debe especificar un estudiante para la postulación'
  },
  APP_004: {
    code: 'APP_004',
    message: 'Estado inválido',
    statusCode: 400,
    description: 'El estado de la postulación no es válido'
  },
  APP_005: {
    code: 'APP_005',
    message: 'Postulación ya existe',
    statusCode: 409,
    description: 'Ya existe una postulación para este estudiante en el año actual'
  },
  APP_006: {
    code: 'APP_006',
    message: 'No autorizado para editar postulación',
    statusCode: 403,
    description: 'Solo el apoderado dueño puede editar esta postulación'
  },
  APP_007: {
    code: 'APP_007',
    message: 'Postulación no puede ser eliminada',
    statusCode: 400,
    description: 'La postulación está en un estado que no permite eliminación'
  },

  // ========== EVALUATION ERRORS (EVAL_xxx) ==========
  EVAL_001: {
    code: 'EVAL_001',
    message: 'Evaluador no disponible',
    statusCode: 409,
    description: 'El evaluador no está disponible para esta fecha/hora'
  },
  EVAL_002: {
    code: 'EVAL_002',
    message: 'Evaluación no encontrada',
    statusCode: 404,
    description: 'La evaluación solicitada no existe'
  },
  EVAL_003: {
    code: 'EVAL_003',
    message: 'Tipo de evaluación inválido',
    statusCode: 400,
    description: 'El tipo de evaluación especificado no es válido'
  },
  EVAL_004: {
    code: 'EVAL_004',
    message: 'Puntaje inválido',
    statusCode: 400,
    description: 'El puntaje debe estar entre 0 y el máximo permitido'
  },
  EVAL_005: {
    code: 'EVAL_005',
    message: 'Evaluación ya completada',
    statusCode: 409,
    description: 'Esta evaluación ya ha sido completada y no puede ser modificada'
  },
  EVAL_006: {
    code: 'EVAL_006',
    message: 'Evaluador no autorizado',
    statusCode: 403,
    description: 'Solo el evaluador asignado puede completar esta evaluación'
  },

  // ========== INTERVIEW ERRORS (INT_xxx) ==========
  INT_001: {
    code: 'INT_001',
    message: 'Entrevista no encontrada',
    statusCode: 404,
    description: 'La entrevista solicitada no existe'
  },
  INT_002: {
    code: 'INT_002',
    message: 'Conflicto de horario',
    statusCode: 409,
    description: 'El entrevistador ya tiene una entrevista agendada en este horario'
  },
  INT_003: {
    code: 'INT_003',
    message: 'Fecha inválida',
    statusCode: 400,
    description: 'La fecha de la entrevista debe ser futura'
  },
  INT_004: {
    code: 'INT_004',
    message: 'Entrevistador no disponible',
    statusCode: 409,
    description: 'El entrevistador no está disponible en la fecha/hora seleccionada'
  },
  INT_005: {
    code: 'INT_005',
    message: 'Entrevista ya realizada',
    statusCode: 409,
    description: 'Esta entrevista ya ha sido realizada y no puede ser modificada'
  },

  // ========== USER ERRORS (USER_xxx) ==========
  USER_001: {
    code: 'USER_001',
    message: 'Usuario no encontrado',
    statusCode: 404,
    description: 'El usuario solicitado no existe'
  },
  USER_002: {
    code: 'USER_002',
    message: 'Rol inválido',
    statusCode: 400,
    description: 'El rol especificado no es válido'
  },
  USER_003: {
    code: 'USER_003',
    message: 'Email inválido',
    statusCode: 400,
    description: 'El formato del email no es válido'
  },
  USER_004: {
    code: 'USER_004',
    message: 'Contraseña débil',
    statusCode: 400,
    description: 'La contraseña debe tener al menos 8 caracteres'
  },
  USER_005: {
    code: 'USER_005',
    message: 'Usuario inactivo',
    statusCode: 403,
    description: 'El usuario ha sido desactivado'
  },

  // ========== GUARDIAN ERRORS (GUARD_xxx) ==========
  GUARD_001: {
    code: 'GUARD_001',
    message: 'Apoderado no encontrado',
    statusCode: 404,
    description: 'El apoderado solicitado no existe'
  },
  GUARD_002: {
    code: 'GUARD_002',
    message: 'RUT de apoderado inválido',
    statusCode: 400,
    description: 'El RUT del apoderado no es válido (formato o dígito verificador)'
  },
  GUARD_003: {
    code: 'GUARD_003',
    message: 'RUT de apoderado ya registrado',
    statusCode: 409,
    description: 'Ya existe un apoderado registrado con este RUT'
  },

  // ========== STUDENT ERRORS (STU_xxx) ==========
  STU_001: {
    code: 'STU_001',
    message: 'Estudiante no encontrado',
    statusCode: 404,
    description: 'El estudiante solicitado no existe'
  },
  STU_002: {
    code: 'STU_002',
    message: 'RUT de estudiante inválido',
    statusCode: 400,
    description: 'El RUT del estudiante no es válido (formato o dígito verificador)'
  },
  STU_003: {
    code: 'STU_003',
    message: 'RUT de estudiante ya registrado',
    statusCode: 409,
    description: 'Ya existe un estudiante registrado con este RUT'
  },
  STU_004: {
    code: 'STU_004',
    message: 'Edad inválida',
    statusCode: 400,
    description: 'La edad del estudiante no es válida para el grado solicitado'
  },

  // ========== DOCUMENT ERRORS (DOC_xxx) ==========
  DOC_001: {
    code: 'DOC_001',
    message: 'Documento no encontrado',
    statusCode: 404,
    description: 'El documento solicitado no existe'
  },
  DOC_002: {
    code: 'DOC_002',
    message: 'Tipo de documento inválido',
    statusCode: 400,
    description: 'El tipo de documento especificado no es válido'
  },
  DOC_003: {
    code: 'DOC_003',
    message: 'Documento ya existe',
    statusCode: 409,
    description: 'Ya existe un documento de este tipo para esta postulación'
  },
  DOC_004: {
    code: 'DOC_004',
    message: 'Formato de archivo inválido',
    statusCode: 400,
    description: 'El formato del archivo no es válido (solo PDF, JPG, PNG permitidos)'
  },

  // ========== FILE ERRORS (FILE_xxx) ==========
  FILE_001: {
    code: 'FILE_001',
    message: 'No se proporcionó ningún archivo',
    statusCode: 400,
    description: 'Debe proporcionar un archivo para subir'
  },
  FILE_002: {
    code: 'FILE_002',
    message: 'Tipo de archivo no determinado',
    statusCode: 400,
    description: 'No se pudo determinar el tipo MIME del archivo'
  },
  FILE_003: {
    code: 'FILE_003',
    message: 'Tipo de archivo no permitido',
    statusCode: 400,
    description: 'Solo se permiten archivos PDF, JPEG y PNG'
  },
  FILE_004: {
    code: 'FILE_004',
    message: 'Archivo vacío',
    statusCode: 400,
    description: 'El archivo está vacío (0 bytes)'
  },
  FILE_005: {
    code: 'FILE_005',
    message: 'Archivo demasiado grande',
    statusCode: 413,
    description: 'El archivo excede el tamaño máximo permitido (5MB)'
  },
  FILE_006: {
    code: 'FILE_006',
    message: 'Nombre de archivo inválido',
    statusCode: 400,
    description: 'El nombre del archivo no es válido'
  },
  FILE_007: {
    code: 'FILE_007',
    message: 'Extensión prohibida',
    statusCode: 400,
    description: 'La extensión del archivo está prohibida por seguridad'
  },
  FILE_008: {
    code: 'FILE_008',
    message: 'Extensión no coincide con tipo MIME',
    statusCode: 400,
    description: 'La extensión del archivo no coincide con su tipo MIME'
  },
  FILE_009: {
    code: 'FILE_009',
    message: 'No se proporcionaron archivos',
    statusCode: 400,
    description: 'No se proporcionaron archivos para subir'
  },
  FILE_010: {
    code: 'FILE_010',
    message: 'Validación de archivos fallida',
    statusCode: 400,
    description: 'Uno o más archivos fallaron la validación'
  },

  // ========== NOTIFICATION ERRORS (NOTIF_xxx) ==========
  NOTIF_001: {
    code: 'NOTIF_001',
    message: 'Error al enviar notificación',
    statusCode: 500,
    description: 'No se pudo enviar la notificación'
  },
  NOTIF_002: {
    code: 'NOTIF_002',
    message: 'Plantilla no encontrada',
    statusCode: 404,
    description: 'La plantilla de notificación no existe'
  },
  NOTIF_003: {
    code: 'NOTIF_003',
    message: 'Destinatario inválido',
    statusCode: 400,
    description: 'El destinatario de la notificación no es válido'
  },

  // ========== VALIDATION ERRORS (VAL_xxx) ==========
  VAL_001: {
    code: 'VAL_001',
    message: 'Datos requeridos faltantes',
    statusCode: 400,
    description: 'Faltan campos requeridos en la solicitud'
  },
  VAL_002: {
    code: 'VAL_002',
    message: 'Formato de fecha inválido',
    statusCode: 400,
    description: 'El formato de la fecha no es válido (esperado: YYYY-MM-DD)'
  },
  VAL_003: {
    code: 'VAL_003',
    message: 'Valor fuera de rango',
    statusCode: 400,
    description: 'El valor proporcionado está fuera del rango permitido'
  },
  VAL_004: {
    code: 'VAL_004',
    message: 'Formato inválido',
    statusCode: 400,
    description: 'El formato de los datos proporcionados no es válido'
  },

  // ========== SYSTEM ERRORS (SYS_xxx) ==========
  SYS_001: {
    code: 'SYS_001',
    message: 'Error interno del servidor',
    statusCode: 500,
    description: 'Ocurrió un error inesperado en el servidor'
  },
  SYS_002: {
    code: 'SYS_002',
    message: 'Error de base de datos',
    statusCode: 500,
    description: 'Error al comunicarse con la base de datos'
  },
  SYS_003: {
    code: 'SYS_003',
    message: 'Servicio no disponible',
    statusCode: 503,
    description: 'El servicio no está disponible temporalmente'
  },
  SYS_004: {
    code: 'SYS_004',
    message: 'Circuit breaker abierto',
    statusCode: 503,
    description: 'El servicio está experimentando problemas y el circuit breaker se ha activado'
  }
};

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

/**
 * Create an error object from an error code
 *
 * @param {string} errorCode - Error code (e.g., 'AUTH_001')
 * @param {Object} context - Optional context data (e.g., { email: 'user@example.com' })
 * @returns {Object} Error object with success, error, errorCode, statusCode, timestamp
 */
function createError(errorCode, context = {}) {
  const error = Errors[errorCode];

  if (!error) {
    return {
      success: false,
      error: 'Código de error desconocido',
      errorCode: 'UNKNOWN',
      statusCode: 500,
      timestamp: new Date().toISOString()
    };
  }

  return {
    success: false,
    error: error.message,
    errorCode: error.code,
    statusCode: error.statusCode,
    description: error.description,
    context: Object.keys(context).length > 0 ? context : undefined,
    timestamp: new Date().toISOString()
  };
}

/**
 * Create an error response and send it
 *
 * @param {Object} res - Express response object
 * @param {string} errorCode - Error code (e.g., 'AUTH_001')
 * @param {Object} context - Optional context data
 * @returns {Object} Express response
 */
function createErrorResponse(res, errorCode, context = {}) {
  const errorObj = createError(errorCode, context);
  return res.status(errorObj.statusCode).json(errorObj);
}

/**
 * Get all error codes for a specific category
 *
 * @param {string} prefix - Category prefix (e.g., 'AUTH', 'APP', 'EVAL')
 * @returns {Object} Object with all errors for that category
 */
function getErrorsByCategory(prefix) {
  const categoryErrors = {};

  Object.keys(Errors).forEach(key => {
    if (key.startsWith(prefix)) {
      categoryErrors[key] = Errors[key];
    }
  });

  return categoryErrors;
}

/**
 * Get a human-readable list of all error codes
 *
 * @returns {string} Formatted string with all error codes
 */
function listAllErrors() {
  let output = '## ERROR CODES REFERENCE\n\n';

  const categories = {
    AUTH: 'Authentication Errors',
    APP: 'Application Errors',
    EVAL: 'Evaluation Errors',
    INT: 'Interview Errors',
    USER: 'User Errors',
    GUARD: 'Guardian Errors',
    STU: 'Student Errors',
    DOC: 'Document Errors',
    FILE: 'File Errors',
    NOTIF: 'Notification Errors',
    VAL: 'Validation Errors',
    SYS: 'System Errors'
  };

  Object.keys(categories).forEach(prefix => {
    output += `### ${categories[prefix]}\n\n`;
    const categoryErrors = getErrorsByCategory(prefix);

    Object.keys(categoryErrors).forEach(key => {
      const error = categoryErrors[key];
      output += `- **${error.code}** (${error.statusCode}): ${error.message}\n`;
      output += `  ${error.description}\n\n`;
    });
  });

  return output;
}

// ============================================================================
// EXPORTS
// ============================================================================

module.exports = {
  Errors,
  createError,
  createErrorResponse,
  getErrorsByCategory,
  listAllErrors
};
