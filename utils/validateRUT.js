/**
 * Chilean RUT Validation Utility
 * Validates Chilean national identification numbers (RUT) using modulo 11 algorithm
 *
 * Format: 12.345.678-9 or 12345678-9 or 123456789
 * Last digit is a verifier calculated from previous digits
 */

/**
 * Validates a Chilean RUT
 * @param {string} rut - The RUT to validate (can include dots and dashes)
 * @returns {boolean} True if RUT is valid, false otherwise
 *
 * @example
 * validateRUT('12.345.678-5')  // true
 * validateRUT('12345678-5')     // true
 * validateRUT('123456785')      // true
 * validateRUT('12.345.678-K')   // true
 * validateRUT('12.345.678-0')   // false (invalid verifier)
 */
function validateRUT(rut) {
  if (!rut || typeof rut !== 'string') {
    return false;
  }

  // Clean RUT: remove dots and dashes
  const cleanRUT = rut.trim().replace(/\./g, '').replace(/-/g, '').toUpperCase();

  // RUT must be at least 2 characters (1 digit + verifier)
  if (cleanRUT.length < 2) {
    return false;
  }

  // Extract body (all but last character) and verifier digit (last character)
  const body = cleanRUT.slice(0, -1);
  const verifier = cleanRUT.slice(-1);

  // Body must contain only digits
  if (!/^\d+$/.test(body)) {
    return false;
  }

  // Verifier must be digit or 'K'
  if (!/^[0-9K]$/.test(verifier)) {
    return false;
  }

  // Calculate expected verifier using modulo 11 algorithm
  let sum = 0;
  let multiplier = 2;

  // Iterate body from right to left
  for (let i = body.length - 1; i >= 0; i--) {
    sum += parseInt(body[i]) * multiplier;
    multiplier = multiplier === 7 ? 2 : multiplier + 1;
  }

  // Calculate verifier digit
  const remainder = sum % 11;
  const expectedVerifier = 11 - remainder;

  // Convert to final verifier format
  let finalVerifier;
  if (expectedVerifier === 11) {
    finalVerifier = '0';
  } else if (expectedVerifier === 10) {
    finalVerifier = 'K';
  } else {
    finalVerifier = expectedVerifier.toString();
  }

  return verifier === finalVerifier;
}

/**
 * Formats a RUT to standard Chilean format (12.345.678-9)
 * @param {string} rut - The RUT to format
 * @returns {string} Formatted RUT or original string if invalid
 *
 * @example
 * formatRUT('123456785')        // '12.345.678-5'
 * formatRUT('12345678-5')       // '12.345.678-5'
 */
function formatRUT(rut) {
  if (!rut || typeof rut !== 'string') {
    return rut;
  }

  // Clean RUT
  const cleanRUT = rut.trim().replace(/\./g, '').replace(/-/g, '').toUpperCase();

  if (cleanRUT.length < 2) {
    return rut;
  }

  // Extract body and verifier
  const body = cleanRUT.slice(0, -1);
  const verifier = cleanRUT.slice(-1);

  // Add thousand separators
  const formattedBody = body.replace(/\B(?=(\d{3})+(?!\d))/g, '.');

  return `${formattedBody}-${verifier}`;
}

/**
 * Cleans a RUT by removing formatting characters
 * @param {string} rut - The RUT to clean
 * @returns {string} Clean RUT without dots or dashes
 *
 * @example
 * cleanRUT('12.345.678-5')  // '123456785'
 */
function cleanRUT(rut) {
  if (!rut || typeof rut !== 'string') {
    return rut;
  }

  return rut.trim().replace(/\./g, '').replace(/-/g, '').toUpperCase();
}

module.exports = {
  validateRUT,
  formatRUT,
  cleanRUT
};
