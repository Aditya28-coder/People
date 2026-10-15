package com.kloudspot.employees.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(EmployeeNotFoundException.class)
    ProblemDetail notFound(EmployeeNotFoundException error, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, error.getMessage(), request);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    ProblemDetail duplicate(DuplicateEmailException error, HttpServletRequest request) {
        var detail = problem(HttpStatus.CONFLICT, error.getMessage(), request);
        detail.setProperty("errors", java.util.Map.of("email", error.getMessage()));
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException error, HttpServletRequest request) {
        var detail = problem(HttpStatus.BAD_REQUEST, "Check the highlighted fields and try again.", request);
        var fields = new LinkedHashMap<String, String>();
        error.getBindingResult().getFieldErrors().forEach(field -> fields.putIfAbsent(field.getField(), field.getDefaultMessage()));
        detail.setProperty("errors", fields);
        return detail;
    }

    @ExceptionHandler({ConstraintViolationException.class, IllegalArgumentException.class})
    ProblemDetail invalidParameter(Exception error, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, error.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail invalidType(MethodArgumentTypeMismatchException error, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid value for '" + error.getName() + "'. Status must be ACTIVE or INACTIVE; numeric parameters must be numbers.", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail invalidBody(HttpMessageNotReadableException error, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid JSON. Use dates as YYYY-MM-DD, a numeric salary, status ACTIVE or INACTIVE, and only documented fields.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail databaseConflict(DataIntegrityViolationException error, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "The record conflicts with existing data. Check that the email address is unique.", request);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail concurrentEdit(ObjectOptimisticLockingFailureException error, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "This employee changed during your request. Refresh and try again.", request);
    }

    // Keep database details and stack traces out of client responses.
    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception error, HttpServletRequest request) {
        if (error instanceof ErrorResponse frameworkError) {
            var detail = frameworkError.getBody();
            detail.setInstance(URI.create(request.getRequestURI()));
            detail.setProperty("timestamp", Instant.now());
            return detail;
        }
        log.error("Request failed: {} {}", request.getMethod(), request.getRequestURI(), error);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.", request);
    }

    private static ProblemDetail problem(HttpStatus status, String message, HttpServletRequest request) {
        var detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setInstance(URI.create(request.getRequestURI()));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }
}
