package com.example.wine.controller;

import com.example.wine.telemetry.ApplicationTelemetry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private final ApplicationTelemetry telemetry;

    public ApiExceptionHandler(ApplicationTelemetry telemetry) {
        this.telemetry = telemetry;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleResponseStatusException(ResponseStatusException exception,
                                                                 HttpServletRequest request) {
        HttpStatus status = exception.getStatus();
        telemetry.httpRequestFailed(status.value(), request.getRequestURI(), exception);
        return new ResponseEntity<>(null, exception.getResponseHeaders(), status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpectedException(Exception exception, HttpServletRequest request) {
        telemetry.httpRequestFailed(HttpStatus.INTERNAL_SERVER_ERROR.value(), request.getRequestURI(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, HttpHeaders headers,
                                                              HttpStatus status, WebRequest request) {
        telemetry.httpRequestFailed(status.value(), requestPath(request), exception);
        return super.handleExceptionInternal(exception, body, headers, status, request);
    }

    private String requestPath(WebRequest request) {
        if (request instanceof ServletWebRequest) {
            return ((ServletWebRequest) request).getRequest().getRequestURI();
        }
        return "unknown";
    }
}