/**
 * Audit Logger Utility
 * Logs critical operations to audit_logs table for compliance and traceability
 *
 * Supports:
 * - User actions tracking (who did what when)
 * - Before/after state capture (old_values, new_values)
 * - IP address and user agent tracking
 * - Entity-level tracking (application, user, evaluation, etc.)
 */

/**
 * Log an audit entry
 * @param {Object} dbPool - PostgreSQL connection pool
 * @param {Object} auditData - Audit data
 * @param {number} auditData.userId - ID of user performing the action
 * @param {string} auditData.action - Action type (CREATE, UPDATE, DELETE, STATUS_CHANGE, etc.)
 * @param {string} auditData.entityType - Entity type (application, user, evaluation, etc.)
 * @param {number} auditData.entityId - ID of the entity being modified
 * @param {Object} auditData.oldValues - Previous state (for updates/deletes)
 * @param {Object} auditData.newValues - New state (for creates/updates)
 * @param {string} auditData.ipAddress - Client IP address
 * @param {string} auditData.userAgent - Client user agent
 * @returns {Promise<void>}
 *
 * @example
 * await logAudit(dbPool, {
 *   userId: 5,
 *   action: 'STATUS_CHANGE',
 *   entityType: 'application',
 *   entityId: 37,
 *   oldValues: { status: 'PENDING' },
 *   newValues: { status: 'APPROVED' },
 *   ipAddress: '192.168.1.100',
 *   userAgent: 'Mozilla/5.0...'
 * });
 */
async function logAudit(dbPool, {
  userId,
  action,
  entityType,
  entityId,
  oldValues = null,
  newValues = null,
  ipAddress = null,
  userAgent = null
}) {
  try {
    await dbPool.query(`
      INSERT INTO audit_logs
        (user_id, action, entity_type, entity_id, old_values, new_values, ip_address, user_agent)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
    `, [
      userId,
      action,
      entityType,
      entityId,
      oldValues ? JSON.stringify(oldValues) : null,
      newValues ? JSON.stringify(newValues) : null,
      ipAddress,
      userAgent
    ]);

    console.log(`[Audit Log] User ${userId} performed ${action} on ${entityType} ${entityId}`);
  } catch (error) {
    // Never fail the main operation due to audit log failure
    // Log error but don't throw
    console.error('Failed to write audit log:', error.message);
  }
}

/**
 * Extract client IP from Express request
 * @param {Object} req - Express request object
 * @returns {string} IP address
 */
function getClientIp(req) {
  return req.headers['x-forwarded-for']?.split(',')[0] ||
         req.headers['x-real-ip'] ||
         req.connection.remoteAddress ||
         req.socket.remoteAddress ||
         req.ip;
}

/**
 * Extract user agent from Express request
 * @param {Object} req - Express request object
 * @returns {string} User agent string
 */
function getUserAgent(req) {
  return req.headers['user-agent'] || 'Unknown';
}

/**
 * Standard audit actions
 */
const AuditActions = {
  CREATE: 'CREATE',
  UPDATE: 'UPDATE',
  DELETE: 'DELETE',
  SOFT_DELETE: 'SOFT_DELETE',
  RESTORE: 'RESTORE',
  STATUS_CHANGE: 'STATUS_CHANGE',
  LOGIN: 'LOGIN',
  LOGOUT: 'LOGOUT',
  LOGIN_FAILED: 'LOGIN_FAILED',
  PASSWORD_CHANGE: 'PASSWORD_CHANGE',
  PERMISSION_CHANGE: 'PERMISSION_CHANGE',
  EXPORT: 'EXPORT',
  IMPORT: 'IMPORT'
};

/**
 * Standard entity types
 */
const EntityTypes = {
  APPLICATION: 'application',
  USER: 'user',
  EVALUATION: 'evaluation',
  INTERVIEW: 'interview',
  GUARDIAN: 'guardian',
  STUDENT: 'student',
  DOCUMENT: 'document',
  NOTIFICATION: 'notification'
};

module.exports = {
  logAudit,
  getClientIp,
  getUserAgent,
  AuditActions,
  EntityTypes
};
