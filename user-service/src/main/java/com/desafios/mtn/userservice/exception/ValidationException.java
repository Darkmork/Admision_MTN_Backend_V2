// user-service/src/main/java/com/desafios/mtn/userservice/exception/ValidationException.java

package com.desafios.mtn.userservice.exception;

public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}