// user-service/src/main/java/com/desafios/mtn/userservice/exception/DuplicateResourceException.java

package com.desafios.mtn.userservice.exception;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
    
    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}