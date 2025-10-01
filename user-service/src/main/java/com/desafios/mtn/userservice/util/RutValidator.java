// user-service/src/main/java/com/desafios/mtn/userservice/util/RutValidator.java

package com.desafios.mtn.userservice.util;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@Component
public class RutValidator {

    private static final Pattern RUT_PATTERN = Pattern.compile("^\\d{1,2}\\.?\\d{3}\\.?\\d{3}-?[0-9kK]$");
    private static final Pattern CLEAN_RUT_PATTERN = Pattern.compile("^\\d{7,8}[0-9kK]$");

    /**
     * Valida si un RUT tiene formato y dígito verificador válido
     */
    public boolean isValid(String rut) {
        if (!StringUtils.hasText(rut)) {
            return false;
        }

        String cleanRut = clean(rut);
        
        // Verificar formato básico
        if (!CLEAN_RUT_PATTERN.matcher(cleanRut).matches()) {
            return false;
        }

        // Verificar dígito verificador
        return isValidDigit(cleanRut);
    }

    /**
     * Limpia un RUT removiendo puntos y guiones
     */
    public String clean(String rut) {
        if (rut == null) {
            return null;
        }
        return rut.replaceAll("[.\\-]", "").toUpperCase();
    }

    /**
     * Formatea un RUT con puntos y guión
     */
    public String format(String rut) {
        if (!StringUtils.hasText(rut)) {
            return rut;
        }

        String cleanRut = clean(rut);
        
        if (cleanRut.length() < 8) {
            return rut; // Retornar original si no es válido
        }

        String number = cleanRut.substring(0, cleanRut.length() - 1);
        String digit = cleanRut.substring(cleanRut.length() - 1);

        // Formatear número con puntos
        StringBuilder formatted = new StringBuilder();
        
        if (number.length() == 8) {
            formatted.append(number.substring(0, 2))
                    .append(".")
                    .append(number.substring(2, 5))
                    .append(".")
                    .append(number.substring(5, 8));
        } else if (number.length() == 7) {
            formatted.append(number.substring(0, 1))
                    .append(".")
                    .append(number.substring(1, 4))
                    .append(".")
                    .append(number.substring(4, 7));
        } else {
            return rut; // Retornar original si longitud no es válida
        }

        formatted.append("-").append(digit);
        
        return formatted.toString();
    }

    /**
     * Verifica si el dígito verificador es correcto
     */
    private boolean isValidDigit(String cleanRut) {
        try {
            String number = cleanRut.substring(0, cleanRut.length() - 1);
            String digit = cleanRut.substring(cleanRut.length() - 1);

            int calculatedDigit = calculateDigit(number);
            
            if (calculatedDigit == 10) {
                return "K".equals(digit);
            } else if (calculatedDigit == 11) {
                return "0".equals(digit);
            } else {
                return String.valueOf(calculatedDigit).equals(digit);
            }
            
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Calcula el dígito verificador de un RUT
     */
    private int calculateDigit(String number) {
        int sum = 0;
        int multiplier = 2;

        // Recorrer de derecha a izquierda
        for (int i = number.length() - 1; i >= 0; i--) {
            sum += Integer.parseInt(String.valueOf(number.charAt(i))) * multiplier;
            multiplier++;
            if (multiplier > 7) {
                multiplier = 2;
            }
        }

        int remainder = sum % 11;
        return 11 - remainder;
    }

    /**
     * Genera un RUT con dígito verificador
     */
    public String generateRutWithDigit(String number) {
        if (!StringUtils.hasText(number) || !number.matches("\\d{7,8}")) {
            throw new IllegalArgumentException("Número de RUT inválido: " + number);
        }

        int digit = calculateDigit(number);
        
        String digitStr;
        if (digit == 10) {
            digitStr = "K";
        } else if (digit == 11) {
            digitStr = "0";
        } else {
            digitStr = String.valueOf(digit);
        }

        return number + digitStr;
    }

    /**
     * Extrae solo el número del RUT (sin dígito verificador)
     */
    public String extractNumber(String rut) {
        if (!StringUtils.hasText(rut)) {
            return null;
        }
        
        String cleanRut = clean(rut);
        if (cleanRut.length() < 8) {
            return null;
        }
        
        return cleanRut.substring(0, cleanRut.length() - 1);
    }

    /**
     * Extrae solo el dígito verificador del RUT
     */
    public String extractDigit(String rut) {
        if (!StringUtils.hasText(rut)) {
            return null;
        }
        
        String cleanRut = clean(rut);
        if (cleanRut.length() < 8) {
            return null;
        }
        
        return cleanRut.substring(cleanRut.length() - 1);
    }

    /**
     * Normaliza un RUT para comparaciones
     */
    public String normalize(String rut) {
        if (!isValid(rut)) {
            return rut;
        }
        return clean(rut);
    }
}