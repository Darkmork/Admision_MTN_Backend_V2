const winston = require('winston');
const path = require('path');

// Formato personalizado para logs
const logFormat = winston.format.combine(
  winston.format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss' }),
  winston.format.errors({ stack: true }),
  winston.format.splat(),
  winston.format.json()
);

// Formato para consola (desarrollo)
const consoleFormat = winston.format.combine(
  winston.format.colorize(),
  winston.format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss' }),
  winston.format.printf(({ timestamp, level, message, service, ...meta }) => {
    let msg = `${timestamp} [${service || 'APP'}] ${level}: ${message}`;
    if (Object.keys(meta).length > 0) {
      msg += ` ${JSON.stringify(meta)}`;
    }
    return msg;
  })
);

// Crear logger base
const createLogger = (serviceName) => {
  return winston.createLogger({
    level: process.env.LOG_LEVEL || 'info',
    format: logFormat,
    defaultMeta: { service: serviceName },
    transports: [
      // Log de errores
      new winston.transports.File({
        filename: path.join('/tmp', `${serviceName}-error.log`),
        level: 'error',
        maxsize: 5242880, // 5MB
        maxFiles: 5,
      }),
      // Log combinado
      new winston.transports.File({
        filename: path.join('/tmp', `${serviceName}.log`),
        maxsize: 5242880, // 5MB
        maxFiles: 5,
      }),
    ],
  });
};

// Agregar consola en desarrollo
if (process.env.NODE_ENV !== 'production') {
  // Solo agregar consola si no estamos en producción
  const addConsoleTransport = (logger) => {
    logger.add(new winston.transports.Console({
      format: consoleFormat,
    }));
  };

  // Export helper para agregar consola
  createLogger.addConsole = addConsoleTransport;
}

module.exports = createLogger;
