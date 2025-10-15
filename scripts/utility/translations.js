/**
 * Módulo centralizado de traducciones ES/EN
 * Sistema de Admisión MTN
 *
 * Este módulo proporciona traducciones bidireccionales entre inglés y español
 * para todos los estados, roles y tipos usados en el sistema.
 */

// ============================================================================
// ESTADOS DE ENTREVISTAS (Interview Status)
// ============================================================================
const INTERVIEW_STATUS = {
  // Inglés -> Español
  EN_TO_ES: {
    'SCHEDULED': 'PROGRAMADA',
    'CONFIRMED': 'CONFIRMADA',
    'IN_PROGRESS': 'EN_PROGRESO',
    'COMPLETED': 'COMPLETADA',
    'CANCELLED': 'CANCELADA',
    'RESCHEDULED': 'REPROGRAMADA',
    'NO_SHOW': 'NO_ASISTIO'
  },
  // Español -> Inglés
  ES_TO_EN: {
    'PROGRAMADA': 'SCHEDULED',
    'CONFIRMADA': 'CONFIRMED',
    'EN_PROGRESO': 'IN_PROGRESS',
    'COMPLETADA': 'COMPLETED',
    'CANCELADA': 'CANCELLED',
    'REPROGRAMADA': 'RESCHEDULED',
    'NO_ASISTIO': 'NO_SHOW'
  }
};

// ============================================================================
// ESTADOS DE APLICACIONES (Application Status)
// ============================================================================
const APPLICATION_STATUS = {
  EN_TO_ES: {
    'DRAFT': 'BORRADOR',
    'SUBMITTED': 'ENVIADA',
    'UNDER_REVIEW': 'EN_REVISION',
    'PENDING': 'PENDIENTE',
    'APPROVED': 'APROBADA',
    'REJECTED': 'RECHAZADA',
    'WAITLISTED': 'EN_LISTA_ESPERA',
    'ENROLLED': 'MATRICULADO',
    'CANCELLED': 'CANCELADA',
    'EXAM_SCHEDULED': 'EXAMEN_PROGRAMADO',
    'INTERVIEW_SCHEDULED': 'ENTREVISTA_PROGRAMADA'
  },
  ES_TO_EN: {
    'BORRADOR': 'DRAFT',
    'ENVIADA': 'SUBMITTED',
    'EN_REVISION': 'UNDER_REVIEW',
    'PENDIENTE': 'PENDING',
    'APROBADA': 'APPROVED',
    'RECHAZADA': 'REJECTED',
    'EN_LISTA_ESPERA': 'WAITLISTED',
    'MATRICULADO': 'ENROLLED',
    'CANCELADA': 'CANCELLED',
    'EXAMEN_PROGRAMADO': 'EXAM_SCHEDULED',
    'ENTREVISTA_PROGRAMADA': 'INTERVIEW_SCHEDULED'
  }
};

// ============================================================================
// ESTADOS DE EVALUACIONES (Evaluation Status)
// ============================================================================
const EVALUATION_STATUS = {
  EN_TO_ES: {
    'PENDING': 'PENDIENTE',
    'IN_PROGRESS': 'EN_PROGRESO',
    'COMPLETED': 'COMPLETADA',
    'REVIEWED': 'REVISADA',
    'APPROVED': 'APROBADA',
    'REJECTED': 'RECHAZADA'
  },
  ES_TO_EN: {
    'PENDIENTE': 'PENDING',
    'EN_PROGRESO': 'IN_PROGRESS',
    'COMPLETADA': 'COMPLETED',
    'REVISADA': 'REVIEWED',
    'APROBADA': 'APPROVED',
    'RECHAZADA': 'REJECTED'
  }
};

// ============================================================================
// ROLES DE USUARIO (User Roles)
// ============================================================================
const USER_ROLES = {
  EN_TO_ES: {
    'ADMIN': 'ADMINISTRADOR',
    'TEACHER': 'PROFESOR',
    'COORDINATOR': 'COORDINADOR',
    'PSYCHOLOGIST': 'PSICOLOGO',
    'CYCLE_DIRECTOR': 'DIRECTOR_CICLO',
    'APODERADO': 'APODERADO', // Se mantiene igual
    'GUARDIAN': 'APODERADO'
  },
  ES_TO_EN: {
    'ADMINISTRADOR': 'ADMIN',
    'PROFESOR': 'TEACHER',
    'COORDINADOR': 'COORDINATOR',
    'PSICOLOGO': 'PSYCHOLOGIST',
    'DIRECTOR_CICLO': 'CYCLE_DIRECTOR',
    'APODERADO': 'APODERADO'
  }
};

// ============================================================================
// TIPOS DE ENTREVISTA (Interview Types)
// ============================================================================
const INTERVIEW_TYPES = {
  EN_TO_ES: {
    'ACADEMIC': 'ACADEMICA',
    'PSYCHOSOCIAL': 'PSICOSOCIAL',
    'ADMINISTRATIVE': 'ADMINISTRATIVA',
    'FAMILY': 'FAMILIAR',
    'INDIVIDUAL': 'INDIVIDUAL',
    'PSYCHOLOGICAL': 'PSICOLOGICA',
    'STUDENT': 'ESTUDIANTE'
  },
  ES_TO_EN: {
    'ACADEMICA': 'ACADEMIC',
    'PSICOSOCIAL': 'PSYCHOSOCIAL',
    'ADMINISTRATIVA': 'ADMINISTRATIVE',
    'FAMILIAR': 'FAMILY',
    'INDIVIDUAL': 'INDIVIDUAL',
    'PSICOLOGICA': 'PSYCHOLOGICAL',
    'ESTUDIANTE': 'STUDENT'
  }
};

// ============================================================================
// TIPOS DE EVALUACIÓN (Evaluation Types)
// ============================================================================
const EVALUATION_TYPES = {
  EN_TO_ES: {
    'ACADEMIC': 'ACADEMICA',
    'PSYCHOSOCIAL': 'PSICOSOCIAL',
    'BEHAVIORAL': 'CONDUCTUAL',
    'APTITUDE': 'APTITUD',
    'DIAGNOSTIC': 'DIAGNOSTICA'
  },
  ES_TO_EN: {
    'ACADEMICA': 'ACADEMIC',
    'PSICOSOCIAL': 'PSYCHOSOCIAL',
    'CONDUCTUAL': 'BEHAVIORAL',
    'APTITUD': 'APTITUDE',
    'DIAGNOSTICA': 'DIAGNOSTIC'
  }
};

// ============================================================================
// ESTADOS DE NOTIFICACIONES (Notification Status)
// ============================================================================
const NOTIFICATION_STATUS = {
  EN_TO_ES: {
    'PENDING': 'PENDIENTE',
    'SENT': 'ENVIADA',
    'DELIVERED': 'ENTREGADA',
    'FAILED': 'FALLIDA',
    'READ': 'LEIDA'
  },
  ES_TO_EN: {
    'PENDIENTE': 'PENDING',
    'ENVIADA': 'SENT',
    'ENTREGADA': 'DELIVERED',
    'FALLIDA': 'FAILED',
    'LEIDA': 'READ'
  }
};

// ============================================================================
// FUNCIONES DE TRADUCCIÓN
// ============================================================================

/**
 * Traduce un estado de inglés a español
 * @param {string} value - Valor en inglés
 * @param {string} category - Categoría (interview, application, evaluation, role, etc.)
 * @returns {string} Valor traducido o el valor original si no se encuentra
 */
function translateToSpanish(value, category = 'interview') {
  if (!value) return value;

  const categoryMap = {
    'interview': INTERVIEW_STATUS.EN_TO_ES,
    'interview_status': INTERVIEW_STATUS.EN_TO_ES,
    'application': APPLICATION_STATUS.EN_TO_ES,
    'application_status': APPLICATION_STATUS.EN_TO_ES,
    'evaluation': EVALUATION_STATUS.EN_TO_ES,
    'evaluation_status': EVALUATION_STATUS.EN_TO_ES,
    'role': USER_ROLES.EN_TO_ES,
    'user_role': USER_ROLES.EN_TO_ES,
    'interview_type': INTERVIEW_TYPES.EN_TO_ES,
    'evaluation_type': EVALUATION_TYPES.EN_TO_ES,
    'notification': NOTIFICATION_STATUS.EN_TO_ES,
    'notification_status': NOTIFICATION_STATUS.EN_TO_ES
  };

  const map = categoryMap[category.toLowerCase()];
  return map && map[value.toUpperCase()] ? map[value.toUpperCase()] : value;
}

/**
 * Traduce un estado de español a inglés
 * @param {string} value - Valor en español
 * @param {string} category - Categoría (interview, application, evaluation, role, etc.)
 * @returns {string} Valor traducido o el valor original si no se encuentra
 */
function translateToEnglish(value, category = 'interview') {
  if (!value) return value;

  const categoryMap = {
    'interview': INTERVIEW_STATUS.ES_TO_EN,
    'interview_status': INTERVIEW_STATUS.ES_TO_EN,
    'application': APPLICATION_STATUS.ES_TO_EN,
    'application_status': APPLICATION_STATUS.ES_TO_EN,
    'evaluation': EVALUATION_STATUS.ES_TO_EN,
    'evaluation_status': EVALUATION_STATUS.ES_TO_EN,
    'role': USER_ROLES.ES_TO_EN,
    'user_role': USER_ROLES.ES_TO_EN,
    'interview_type': INTERVIEW_TYPES.ES_TO_EN,
    'evaluation_type': EVALUATION_TYPES.ES_TO_EN,
    'notification': NOTIFICATION_STATUS.ES_TO_EN,
    'notification_status': NOTIFICATION_STATUS.ES_TO_EN
  };

  const map = categoryMap[category.toLowerCase()];
  return map && map[value.toUpperCase()] ? map[value.toUpperCase()] : value;
}

/**
 * Traduce un objeto completo de inglés a español
 * @param {Object} obj - Objeto a traducir
 * @param {Object} fieldMap - Mapeo de campos a categorías {field: category}
 * @returns {Object} Objeto con campos traducidos
 */
function translateObjectToSpanish(obj, fieldMap = {}) {
  if (!obj || typeof obj !== 'object') return obj;

  const translated = { ...obj };

  Object.keys(fieldMap).forEach(field => {
    if (translated[field]) {
      translated[field] = translateToSpanish(translated[field], fieldMap[field]);
    }
  });

  return translated;
}

/**
 * Traduce un array de objetos de inglés a español
 * @param {Array} array - Array de objetos a traducir
 * @param {Object} fieldMap - Mapeo de campos a categorías
 * @returns {Array} Array con objetos traducidos
 */
function translateArrayToSpanish(array, fieldMap = {}) {
  if (!Array.isArray(array)) return array;
  return array.map(obj => translateObjectToSpanish(obj, fieldMap));
}

// ============================================================================
// EXPORTS
// ============================================================================
module.exports = {
  // Mapas de traducción
  INTERVIEW_STATUS,
  APPLICATION_STATUS,
  EVALUATION_STATUS,
  USER_ROLES,
  INTERVIEW_TYPES,
  EVALUATION_TYPES,
  NOTIFICATION_STATUS,

  // Funciones de traducción
  translateToSpanish,
  translateToEnglish,
  translateObjectToSpanish,
  translateArrayToSpanish
};
