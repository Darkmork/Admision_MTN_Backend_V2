// user-service/src/main/java/com/desafios/mtn/userservice/exception/UserNotFoundException.java

package com.desafios.mtn.userservice.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
    
    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}