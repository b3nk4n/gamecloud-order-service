package de.b3nk4n.gamecloud.orderservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.util.Map;

import static java.util.stream.Collectors.toMap;

@RestControllerAdvice
public class OrderControllerAdvice {
    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> validationErrorHandler(WebExchangeBindException exception) {
        return exception
                .getBindingResult()
                .getAllErrors()
                .stream()
                .map(FieldError.class::cast)
                .filter(fieldError -> fieldError.getDefaultMessage() != null)
                .collect(toMap(FieldError::getField, FieldError::getDefaultMessage));
    }
}
