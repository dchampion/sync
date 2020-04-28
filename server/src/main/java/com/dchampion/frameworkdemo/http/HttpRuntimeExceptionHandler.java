package com.dchampion.frameworkdemo.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;


/**
 * A global exception handler for all REST methods of {@code RestController}-annotated
 * classes in the application context.
 */
@ControllerAdvice
public class HttpRuntimeExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger =
        LoggerFactory.getLogger(HttpRuntimeExceptionHandler.class);

    /**
     * Catches and handles any exception not explicitly caught/handled in the super
     * class's {@link #handleException(Exception, WebRequest)} method.
     *
     * @param exception the target exception.
     * @param request the current request.
     *
     * @return the {@link ResponseEntity} for consumption by the client.
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handle(Exception exception, WebRequest request) {
        // Put something useful in the log.
        logger.warn(exception.getMessage(), exception);

        return handleExceptionInternal(exception, exception.getMessage(),
            new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Takes the additional step of putting the target exception's message string
     * into the body of the response if the body passed to this method is {@code null}.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
            HttpHeaders headers, HttpStatus status, WebRequest request) {
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
        Object responseBody = body != null ? body : exception.getMessage();

        return ResponseEntity.status(status).headers(headers).body(responseBody);
    }
}
