/**
 * Standardized Error Handler Utility
 *
 * Provides consistent error response formatting across all microservices.
 * Implements the standard contract: { success, error, data, timestamp }
 *
 * Usage:
 *   const { sendError, ErrorCodes } = require('./errorHandler');
 *   return sendError(res, 400, ErrorCodes.VALIDATION_ERROR, 'Email is required');
 */

// Standard error codes with HTTP status mappings
const ErrorCodes = {
  // 400 - Bad Request
  VALIDATION_ERROR: {
    code: 'VALIDATION_ERROR',
    status: 400,
    message: 'Validation failed'
  },
  INVALID_INPUT: {
    code: 'INVALID_INPUT',
    status: 400,
    message: 'Invalid input data'
  },
  MISSING_FIELDS: {
    code: 'MISSING_FIELDS',
    status: 400,
    message: 'Required fields missing'
  },

  // 401 - Unauthorized
  UNAUTHORIZED: {
    code: 'UNAUTHORIZED',
    status: 401,
    message: 'Authentication required'
  },
  INVALID_TOKEN: {
    code: 'INVALID_TOKEN',
    status: 401,
    message: 'Invalid or expired token'
  },
  INVALID_CREDENTIALS: {
    code: 'INVALID_CREDENTIALS',
    status: 401,
    message: 'Invalid credentials'
  },

  // 403 - Forbidden
  FORBIDDEN: {
    code: 'FORBIDDEN',
    status: 403,
    message: 'Insufficient permissions'
  },
  ACCESS_DENIED: {
    code: 'ACCESS_DENIED',
    status: 403,
    message: 'Access denied'
  },

  // 404 - Not Found
  NOT_FOUND: {
    code: 'NOT_FOUND',
    status: 404,
    message: 'Resource not found'
  },
  USER_NOT_FOUND: {
    code: 'USER_NOT_FOUND',
    status: 404,
    message: 'User not found'
  },
  APPLICATION_NOT_FOUND: {
    code: 'APPLICATION_NOT_FOUND',
    status: 404,
    message: 'Application not found'
  },

  // 409 - Conflict
  CONFLICT: {
    code: 'CONFLICT',
    status: 409,
    message: 'Resource conflict'
  },
  DUPLICATE_ENTRY: {
    code: 'DUPLICATE_ENTRY',
    status: 409,
    message: 'Duplicate entry'
  },
  EMAIL_EXISTS: {
    code: 'EMAIL_EXISTS',
    status: 409,
    message: 'Email already exists'
  },

  // 500 - Internal Server Error
  INTERNAL_ERROR: {
    code: 'INTERNAL_ERROR',
    status: 500,
    message: 'Internal server error'
  },
  DATABASE_ERROR: {
    code: 'DATABASE_ERROR',
    status: 500,
    message: 'Database operation failed'
  },

  // 503 - Service Unavailable
  SERVICE_UNAVAILABLE: {
    code: 'SERVICE_UNAVAILABLE',
    status: 503,
    message: 'Service temporarily unavailable'
  },
  CIRCUIT_BREAKER_OPEN: {
    code: 'CIRCUIT_BREAKER_OPEN',
    status: 503,
    message: 'Service temporarily unavailable'
  },
  EXTERNAL_SERVICE_ERROR: {
    code: 'EXTERNAL_SERVICE_ERROR',
    status: 503,
    message: 'External service failed'
  }
};

/**
 * Send standardized error response
 * @param {Object} res - Express response object
 * @param {number} statusCode - HTTP status code
 * @param {string} errorCode - Error code from ErrorCodes
 * @param {string} message - Human-readable error message
 * @param {*} details - Optional additional error details
 */
function sendError(res, statusCode, errorCode, message, details = null) {
  const response = {
    success: false,
    error: {
      code: errorCode,
      message: message,
      timestamp: new Date().toISOString()
    },
    data: null
  };

  if (details) {
    response.error.details = details;
  }

  return res.status(statusCode).json(response);
}

/**
 * Send validation error (400)
 * @param {Object} res - Express response object
 * @param {string} message - Error message
 * @param {*} details - Validation error details
 */
function sendValidationError(res, message, details = null) {
  return sendError(res, 400, 'VALIDATION_ERROR', message, details);
}

/**
 * Send unauthorized error (401)
 * @param {Object} res - Express response object
 * @param {string} message - Error message
 */
function sendUnauthorizedError(res, message = 'Authentication required') {
  return sendError(res, 401, 'UNAUTHORIZED', message);
}

/**
 * Send forbidden error (403)
 * @param {Object} res - Express response object
 * @param {string} message - Error message
 */
function sendForbiddenError(res, message = 'Insufficient permissions') {
  return sendError(res, 403, 'FORBIDDEN', message);
}

/**
 * Send not found error (404)
 * @param {Object} res - Express response object
 * @param {string} message - Error message
 */
function sendNotFoundError(res, message = 'Resource not found') {
  return sendError(res, 404, 'NOT_FOUND', message);
}

/**
 * Send conflict error (409)
 * @param {Object} res - Express response object
 * @param {string} message - Error message
 * @param {*} details - Conflict details
 */
function sendConflictError(res, message, details = null) {
  return sendError(res, 409, 'CONFLICT', message, details);
}

/**
 * Send internal server error (500)
 * @param {Object} res - Express response object
 * @param {string} message - Error message
 * @param {*} details - Error details (sanitized in production)
 */
function sendInternalError(res, message = 'Internal server error', details = null) {
  // In production, don't expose internal error details
  const safeDetails = process.env.NODE_ENV === 'production' ? null : details;
  return sendError(res, 500, 'INTERNAL_ERROR', message, safeDetails);
}

/**
 * Send database error (500)
 * @param {Object} res - Express response object
 * @param {string} message - Error message
 */
function sendDatabaseError(res, message = 'Database operation failed') {
  return sendError(res, 500, 'DATABASE_ERROR', message);
}

/**
 * Send service unavailable error (503)
 * @param {Object} res - Express response object
 * @param {string} message - Error message
 */
function sendServiceUnavailableError(res, message = 'Service temporarily unavailable') {
  return sendError(res, 503, 'SERVICE_UNAVAILABLE', message);
}

/**
 * Send circuit breaker open error (503)
 * @param {Object} res - Express response object
 */
function sendCircuitBreakerError(res) {
  return sendError(
    res,
    503,
    'CIRCUIT_BREAKER_OPEN',
    'Service temporarily unavailable - circuit breaker open',
    { retryAfter: 30 }
  );
}

/**
 * Send standardized success response
 * @param {Object} res - Express response object
 * @param {*} data - Response data
 * @param {number} statusCode - HTTP status code (default: 200)
 */
function sendSuccess(res, data, statusCode = 200) {
  const response = {
    success: true,
    data: data,
    error: null,
    timestamp: new Date().toISOString()
  };

  return res.status(statusCode).json(response);
}

/**
 * Send standardized paginated response
 * @param {Object} res - Express response object
 * @param {Array} items - Array of items
 * @param {Object} pagination - Pagination metadata
 */
function sendPaginatedSuccess(res, items, pagination) {
  const { total, page, limit } = pagination;
  const totalPages = Math.ceil(total / limit);

  const response = {
    success: true,
    data: items,
    pagination: {
      total,
      page,
      limit,
      totalPages,
      hasNext: page < totalPages - 1,
      hasPrev: page > 0
    },
    error: null,
    timestamp: new Date().toISOString()
  };

  return res.status(200).json(response);
}

module.exports = {
  ErrorCodes,
  sendError,
  sendValidationError,
  sendUnauthorizedError,
  sendForbiddenError,
  sendNotFoundError,
  sendConflictError,
  sendInternalError,
  sendDatabaseError,
  sendServiceUnavailableError,
  sendCircuitBreakerError,
  sendSuccess,
  sendPaginatedSuccess
};
