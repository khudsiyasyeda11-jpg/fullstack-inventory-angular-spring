package com.portfolio.inventory.error;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps exceptions to RFC 9457 problem details. Framework exceptions (malformed JSON, type mismatch,
 * unknown route, ...) are already handled as ProblemDetail by the parent class.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String TYPE_BASE = "https://example.org/problems/";

    @ExceptionHandler(AssetNotFoundException.class)
    ProblemDetail handleNotFound(AssetNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "asset-not-found", "Asset not found", ex.getMessage());
    }

    @ExceptionHandler(DuplicateAssetTagException.class)
    ProblemDetail handleDuplicate(DuplicateAssetTagException ex) {
        ProblemDetail pd = problem(HttpStatus.CONFLICT, "duplicate-asset-tag", "Duplicate asset tag", ex.getMessage());
        pd.setProperty("errors", Map.of("assetTag", "already in use"));
        return pd;
    }

    @ExceptionHandler(BusinessRuleException.class)
    ProblemDetail handleBusinessRule(BusinessRuleException ex) {
        ProblemDetail pd = problem(HttpStatus.UNPROCESSABLE_ENTITY, "business-rule", "Business rule violated", ex.getMessage());
        pd.setProperty("errors", Map.of(ex.getField(), ex.getMessage()));
        return pd;
    }

    @ExceptionHandler(PropertyReferenceException.class)
    ProblemDetail handleBadSort(PropertyReferenceException ex) {
        return problem(HttpStatus.BAD_REQUEST, "invalid-sort", "Invalid sort property",
                "Cannot sort by '" + ex.getPropertyName() + "'");
    }

    /** Depending on the call path, Spring Data may wrap a bad sort property in a DAO exception. */
    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    ProblemDetail handleInvalidApiUsage(InvalidDataAccessApiUsageException ex) {
        if (ex.getCause() instanceof PropertyReferenceException pre) {
            return handleBadSort(pre);
        }
        return problem(HttpStatus.BAD_REQUEST, "invalid-query", "Invalid query", "The query parameters are not valid");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleIntegrity(DataIntegrityViolationException ex) {
        // Do not leak SQL details to clients.
        return problem(HttpStatus.CONFLICT, "data-conflict", "Data conflict",
                "The request conflicts with existing data");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "validation", "Validation failed",
                errors.size() + " field(s) are invalid");
        pd.setProperty("errors", errors);
        return handleExceptionInternal(ex, pd, headers, HttpStatus.BAD_REQUEST, request);
    }

    private static ProblemDetail problem(HttpStatus status, String type, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(TYPE_BASE + type));
        pd.setTitle(title);
        return pd;
    }
}
