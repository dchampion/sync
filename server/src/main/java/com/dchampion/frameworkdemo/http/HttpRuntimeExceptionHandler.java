package com.dchampion.frameworkdemo.http;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * A global exception handler for all REST methods in {@code RestController}-annotated
 * classes in the application context.
 */
@ControllerAdvice
public class HttpRuntimeExceptionHandler {

    /**
     * Returns a {@link ResponseEntity} with the status {@link HttpStatus#INTERNAL_SERVER_ERROR}
     * and a body which contains the string returned by the exception's {@code getMessage()}
     * method.
     *
     * @param e The caught {@link RuntimeException}
     *
     * @return A {@link ResponseEntity} with a (hopefully) useful message.
     */
    @ExceptionHandler(RuntimeException.class)
    protected ResponseEntity<String> handleException(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE).body(e.getMessage());
    }
}
