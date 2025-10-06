/**
 * File Validation Utility
 *
 * Validates uploaded files for:
 * - MIME type (only PDF, JPEG, PNG allowed)
 * - File size (max 5MB)
 * - File extension matching MIME type
 * - Security checks (no executable files)
 *
 * Usage:
 *   const { validateFile, validateMultipleFiles } = require('./utils/fileValidator');
 *
 *   // Single file validation
 *   const result = validateFile(file);
 *   if (!result.valid) {
 *     return res.status(400).json({ error: result.error, errorCode: result.errorCode });
 *   }
 *
 *   // Multiple files validation
 *   const results = validateMultipleFiles(files);
 *   if (!results.valid) {
 *     return res.status(400).json({ error: results.error, errors: results.errors });
 *   }
 */

// ============================================================================
// CONFIGURATION
// ============================================================================

const ALLOWED_MIME_TYPES = [
  'application/pdf',
  'image/jpeg',
  'image/png',
  'image/jpg'
];

const MIME_TYPE_EXTENSIONS = {
  'application/pdf': ['.pdf'],
  'image/jpeg': ['.jpg', '.jpeg'],
  'image/png': ['.png'],
  'image/jpg': ['.jpg', '.jpeg']
};

const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB in bytes

const FORBIDDEN_EXTENSIONS = [
  '.exe', '.bat', '.cmd', '.com', '.pif', '.scr',
  '.vbs', '.js', '.jar', '.msi', '.app', '.deb',
  '.dmg', '.pkg', '.rpm', '.sh', '.bash', '.ps1'
];

// ============================================================================
// VALIDATION FUNCTIONS
// ============================================================================

/**
 * Validates a single uploaded file
 *
 * @param {Object} file - File object from multer or express-fileupload
 *   Expected properties: { mimetype, size, originalname, name, filename }
 * @returns {Object} { valid: boolean, error?: string, errorCode?: string }
 */
function validateFile(file) {
  if (!file) {
    return {
      valid: false,
      error: 'No se proporcionó ningún archivo',
      errorCode: 'FILE_001'
    };
  }

  // Extract file properties (support both multer and express-fileupload)
  const mimetype = file.mimetype || file.type;
  const size = file.size;
  const filename = file.originalname || file.name || file.filename || '';

  // Validate MIME type
  if (!mimetype) {
    return {
      valid: false,
      error: 'No se pudo determinar el tipo de archivo',
      errorCode: 'FILE_002'
    };
  }

  if (!ALLOWED_MIME_TYPES.includes(mimetype)) {
    return {
      valid: false,
      error: `Tipo de archivo no permitido. Solo se permiten: PDF, JPEG, PNG. Recibido: ${mimetype}`,
      errorCode: 'FILE_003',
      allowedTypes: ALLOWED_MIME_TYPES
    };
  }

  // Validate file size
  if (!size || size === 0) {
    return {
      valid: false,
      error: 'El archivo está vacío',
      errorCode: 'FILE_004'
    };
  }

  if (size > MAX_FILE_SIZE) {
    const sizeMB = (size / (1024 * 1024)).toFixed(2);
    const maxMB = (MAX_FILE_SIZE / (1024 * 1024)).toFixed(0);
    return {
      valid: false,
      error: `El archivo es demasiado grande (${sizeMB}MB). Tamaño máximo: ${maxMB}MB`,
      errorCode: 'FILE_005',
      maxSize: MAX_FILE_SIZE,
      actualSize: size
    };
  }

  // Validate filename
  if (!filename) {
    return {
      valid: false,
      error: 'Nombre de archivo inválido',
      errorCode: 'FILE_006'
    };
  }

  // Check forbidden extensions
  const lowerFilename = filename.toLowerCase();
  const hasForbiddenExtension = FORBIDDEN_EXTENSIONS.some(ext =>
    lowerFilename.endsWith(ext)
  );

  if (hasForbiddenExtension) {
    return {
      valid: false,
      error: 'El archivo tiene una extensión prohibida por seguridad',
      errorCode: 'FILE_007'
    };
  }

  // Validate extension matches MIME type
  const expectedExtensions = MIME_TYPE_EXTENSIONS[mimetype] || [];
  const hasValidExtension = expectedExtensions.some(ext =>
    lowerFilename.endsWith(ext)
  );

  if (!hasValidExtension) {
    return {
      valid: false,
      error: `La extensión del archivo no coincide con el tipo MIME. Tipo: ${mimetype}, Archivo: ${filename}`,
      errorCode: 'FILE_008',
      expectedExtensions: expectedExtensions
    };
  }

  // All validations passed
  return {
    valid: true,
    mimetype: mimetype,
    size: size,
    filename: filename
  };
}

/**
 * Validates multiple uploaded files
 *
 * @param {Array|Object} files - Array of file objects or object with file arrays
 * @returns {Object} { valid: boolean, error?: string, errors?: Array, validFiles?: Array }
 */
function validateMultipleFiles(files) {
  if (!files) {
    return {
      valid: false,
      error: 'No se proporcionaron archivos',
      errorCode: 'FILE_009'
    };
  }

  // Convert to array if it's an object (express-fileupload format)
  let fileArray = Array.isArray(files) ? files : Object.values(files);

  // Flatten in case files is an object with arrays as values
  fileArray = fileArray.flat();

  if (fileArray.length === 0) {
    return {
      valid: false,
      error: 'No se proporcionaron archivos',
      errorCode: 'FILE_009'
    };
  }

  const results = [];
  const errors = [];

  for (let i = 0; i < fileArray.length; i++) {
    const file = fileArray[i];
    const validation = validateFile(file);

    if (!validation.valid) {
      errors.push({
        index: i,
        filename: file.originalname || file.name || file.filename || 'unknown',
        error: validation.error,
        errorCode: validation.errorCode
      });
    } else {
      results.push({
        index: i,
        filename: validation.filename,
        mimetype: validation.mimetype,
        size: validation.size
      });
    }
  }

  if (errors.length > 0) {
    return {
      valid: false,
      error: `${errors.length} archivo(s) fallaron la validación`,
      errorCode: 'FILE_010',
      errors: errors,
      validFiles: results
    };
  }

  return {
    valid: true,
    validFiles: results,
    totalFiles: fileArray.length
  };
}

/**
 * Get human-readable error message for error code
 *
 * @param {string} errorCode - Error code (e.g., 'FILE_003')
 * @returns {string} Human-readable error message
 */
function getErrorMessage(errorCode) {
  const ERROR_MESSAGES = {
    'FILE_001': 'No se proporcionó ningún archivo',
    'FILE_002': 'No se pudo determinar el tipo de archivo',
    'FILE_003': 'Tipo de archivo no permitido',
    'FILE_004': 'El archivo está vacío',
    'FILE_005': 'El archivo es demasiado grande',
    'FILE_006': 'Nombre de archivo inválido',
    'FILE_007': 'Extensión prohibida por seguridad',
    'FILE_008': 'Extensión no coincide con tipo MIME',
    'FILE_009': 'No se proporcionaron archivos',
    'FILE_010': 'Uno o más archivos fallaron la validación'
  };

  return ERROR_MESSAGES[errorCode] || 'Error de validación de archivo';
}

/**
 * Get configuration values (useful for frontend validation)
 *
 * @returns {Object} Configuration object
 */
function getConfig() {
  return {
    allowedMimeTypes: ALLOWED_MIME_TYPES,
    maxFileSize: MAX_FILE_SIZE,
    maxFileSizeMB: MAX_FILE_SIZE / (1024 * 1024),
    allowedExtensions: Object.values(MIME_TYPE_EXTENSIONS).flat(),
    forbiddenExtensions: FORBIDDEN_EXTENSIONS
  };
}

// ============================================================================
// EXPORTS
// ============================================================================

module.exports = {
  validateFile,
  validateMultipleFiles,
  getErrorMessage,
  getConfig,

  // Export constants for testing/reference
  ALLOWED_MIME_TYPES,
  MAX_FILE_SIZE,
  FORBIDDEN_EXTENSIONS
};
