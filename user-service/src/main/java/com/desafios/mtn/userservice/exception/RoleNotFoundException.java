// user-service/src/main/java/com/desafios/mtn/userservice/exception/RoleNotFoundException.java

package com.desafios.mtn.userservice.exception;

public class RoleNotFoundException extends RuntimeException {
    public RoleNotFoundException(String message) {
        super(message);
    }
    
    public RoleNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}