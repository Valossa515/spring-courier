package io.github.valossa515.spring_courier.core.exceptions;

public class HandlerNotFoundException extends RuntimeException{
    public HandlerNotFoundException(String message) {
        super(message);
    }

    public HandlerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}