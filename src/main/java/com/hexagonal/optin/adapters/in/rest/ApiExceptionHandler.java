package com.hexagonal.optin.adapters.in.rest;

import com.hexagonal.optin.domain.DomainException;
import com.hexagonal.optin.domain.DuplicateActiveOptInException;
import com.hexagonal.optin.domain.OptInAlreadyRevokedException;
import com.hexagonal.optin.domain.OptInNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps domain rule violations to the status codes declared in optin-api.yaml. */
@RestControllerAdvice
class   ApiExceptionHandler {

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ErrorResponse> handleDomainException(DomainException ex) {
        HttpStatus status = switch (ex) {
            case OptInNotFoundException e -> HttpStatus.NOT_FOUND;
            case DuplicateActiveOptInException e -> HttpStatus.CONFLICT;
            case OptInAlreadyRevokedException e -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return ResponseEntity.status(status).body(new ErrorResponse(ex.getMessage()));
    }
}
