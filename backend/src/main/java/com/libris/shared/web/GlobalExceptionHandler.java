package com.libris.shared.web;

import com.libris.shared.exception.BusinessException;
import com.libris.shared.exception.ErrorType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Single translation point between exceptions and HTTP. Business code throws domain
 * exceptions carrying an {@link ErrorType}; only this class knows the status codes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException exception, HttpServletRequest request) {
        HttpStatus status = statusFor(exception.type());
        log.debug("Business rule rejected {} {}: {}", request.getMethod(), request.getRequestURI(), exception.getMessage());
        return ResponseEntity.status(status).body(
                ApiError.of(clock.instant(), status.value(), exception.code(), exception.getMessage(), path(request)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleInvalidBody(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(ApiError.withFieldErrors(
                clock.instant(), HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR",
                "Revisa los datos enviados", path(request), fieldErrors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException exception, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                fieldErrors.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage()));
        return ResponseEntity.badRequest().body(ApiError.withFieldErrors(
                clock.instant(), HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR",
                "Revisa los datos enviados", path(request), fieldErrors));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> handleMalformedRequest(Exception exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiError.of(
                clock.instant(), HttpStatus.BAD_REQUEST.value(), "MALFORMED_REQUEST",
                "La petición no se pudo interpretar", path(request)));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiError.of(
                clock.instant(), HttpStatus.FORBIDDEN.value(), "ACCESS_DENIED",
                "No tienes permisos para realizar esta acción", path(request)));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleUnknownRoute(NoResourceFoundException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(
                clock.instant(), HttpStatus.NOT_FOUND.value(), "RESOURCE_NOT_FOUND",
                "La ruta solicitada no existe", path(request)));
    }

    /**
     * Last line of defence for the database constraints that also encode business rules,
     * such as the unique index that keeps a copy from being lent twice at the same time.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException exception, HttpServletRequest request) {
        log.warn("Database constraint rejected {} {}", request.getMethod(), request.getRequestURI(), exception);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(
                clock.instant(), HttpStatus.CONFLICT.value(), "DATA_CONFLICT",
                "La operación entra en conflicto con el estado actual de los datos", path(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled error on {} {}", request.getMethod(), request.getRequestURI(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError.of(
                clock.instant(), HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_ERROR",
                "Ocurrió un error inesperado", path(request)));
    }

    private HttpStatus statusFor(ErrorType type) {
        return switch (type) {
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case EXTERNAL_SERVICE -> HttpStatus.SERVICE_UNAVAILABLE;
        };
    }

    private String path(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
