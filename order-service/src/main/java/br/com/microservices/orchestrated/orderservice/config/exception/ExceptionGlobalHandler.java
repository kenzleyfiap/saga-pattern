package br.com.microservices.orchestrated.orderservice.config.exception;

import br.com.caelum.stella.validation.InvalidStateException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionGlobalHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<?> handleValidationException(ValidationException validationException) {
        var details = new ExceptionDetails(HttpStatus.BAD_REQUEST.value(), validationException.getMessage());
        return new ResponseEntity<>(details, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidStateException.class)
    public ResponseEntity<?> handleInvalidStateException(InvalidStateException invalidStateException) {
        var details = new ExceptionDetails(HttpStatus.BAD_REQUEST.value(), invalidStateException.getMessage());
        return new ResponseEntity<>(details, HttpStatus.BAD_REQUEST);
    }

}
