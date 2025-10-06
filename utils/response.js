/**
 * Standardized API Response Helpers
 * Ensures consistent response format across all microservices
 *
 * All responses follow this contract:
 * - success: boolean (true for 2xx, false for errors)
 * - data: object or array (for successful responses)
 * - error: string (for error responses)
 * - timestamp: ISO 8601 string
 * - pagination fields for lists (total, page, limit, totalPages, hasNext, hasPrev)
 */

const now = () => new Date().toISOString();

/**
 * Success response for single entity (HTTP 200/201)
 * @param {Object} data - The data to return
 * @param {Object} meta - Optional metadata to merge into response
 * @returns {Object} Standardized response
 *
 * @example
 * ok({ id: 1, name: 'John' })
 * // { success: true, data: { id: 1, name: 'John' }, timestamp: '2025-10-06T...' }
 */
exports.ok = (data, meta = {}) => ({
  success: true,
  data,
  timestamp: now(),
  ...meta
});

/**
 * Success response for paginated lists (HTTP 200)
 * @param {Array} items - Array of items to return
 * @param {Object} pagination - Pagination info: { total, page, limit }
 * @returns {Object} Standardized paginated response with hasNext/hasPrev
 *
 * @example
 * page([{id:1}, {id:2}], { total: 100, page: 0, limit: 10 })
 * // {
 * //   success: true,
 * //   data: [{id:1}, {id:2}],
 * //   total: 100,
 * //   page: 0,
 * //   limit: 10,
 * //   totalPages: 10,
 * //   hasNext: true,
 * //   hasPrev: false,
 * //   timestamp: '2025-10-06T...'
 * // }
 */
exports.page = (items, { total, page = 0, limit = items?.length ?? 10 } = {}) => {
  const totalPages = limit > 0 ? Math.ceil(total / limit) : 1;
  const hasNext = page < totalPages - 1;
  const hasPrev = page > 0;

  return {
    success: true,
    data: items,
    total,
    page,
    limit,
    totalPages,
    hasNext,
    hasPrev,
    timestamp: now()
  };
};

/**
 * Error response (HTTP 4xx/5xx)
 * @param {string} error - Human-readable error message
 * @param {Object} options - Optional { errorCode, details, status }
 * @returns {Object} Standardized error response
 *
 * @example
 * fail('User not found', { errorCode: 'USER_404', status: 404 })
 * // {
 * //   success: false,
 * //   error: 'User not found',
 * //   errorCode: 'USER_404',
 * //   details: {},
 * //   timestamp: '2025-10-06T...'
 * // }
 */
exports.fail = (error, { errorCode = 'GEN_000', details = {}, status = 400 } = {}) => ({
  success: false,
  error,
  errorCode,
  details,
  timestamp: now()
});

/**
 * Standard error codes for consistent error handling
 */
exports.ErrorCodes = {
  // Generic
  GENERIC: 'GEN_000',
  VALIDATION: 'VAL_001',

  // Authentication
  AUTH_FAILED: 'AUTH_001',
  UNAUTHORIZED: 'AUTH_002',
  TOKEN_EXPIRED: 'AUTH_003',

  // Resources
  NOT_FOUND: 'RES_404',
  ALREADY_EXISTS: 'RES_409',

  // Application-specific
  APP_ERROR: 'APP_001',
  RUT_INVALID: 'APP_002',

  // Database
  DB_ERROR: 'DB_001',
  DB_CONSTRAINT: 'DB_002',

  // External services
  EXT_SERVICE_ERROR: 'EXT_001',
  SMTP_ERROR: 'EXT_002',
  S3_ERROR: 'EXT_003'
};
