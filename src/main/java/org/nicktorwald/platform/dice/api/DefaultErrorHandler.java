package org.nicktorwald.platform.dice.api;

import javax.validation.ValidationException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Handles global errors.
 */
@RestControllerAdvice
public class DefaultErrorHandler {

    @ExceptionHandler
    public ResponseEntity<String> handle(ValidationException cause) {
        return ResponseEntity.badRequest().body(cause.getMessage());
    }

}
