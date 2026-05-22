package com.capstone.backend.global.exception;

import com.capstone.backend.global.time.KoreanTime;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApiException(ApiException exception, HttpServletRequest request) {
        return toProblemDetail(
                exception.getStatus(),
                exception.getCode(),
                exception.getMessage(),
                exception.getStatus().getReasonPhrase(),
                "about:blank",
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(MethodArgumentNotValidException exception,
                                                                   HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining(", "));

        if (message.isBlank()) {
            message = "Validation failed";
        }

        List<String> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();

        ProblemDetail problemDetail = buildProblemDetail(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                message,
                "Validation Failed",
                "urn:problem:validation-error",
                request
        );
        problemDetail.setProperty("errors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableMessage(HttpMessageNotReadableException exception,
                                                                 HttpServletRequest request) {
        return toProblemDetail(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST_BODY",
                "요청 본문 JSON 형식이 올바르지 않습니다.",
                "Invalid Request Body",
                "urn:problem:invalid-request-body",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnhandledException(Exception exception, HttpServletRequest request) {
        ProblemDetailSpec spec = AnnotationUtils.findAnnotation(exception.getClass(), ProblemDetailSpec.class);
        if (spec != null) {
            return toProblemDetail(
                    spec.status(),
                    spec.code(),
                    exception.getMessage(),
                    spec.title(),
                    spec.type(),
                    request
            );
        }

        log.error("Unhandled API exception path={} method={}", request.getRequestURI(), request.getMethod(), exception);
        return toProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "Unexpected server error",
                "Internal Server Error",
                "about:blank",
                request
        );
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    private ResponseEntity<ProblemDetail> toProblemDetail(HttpStatusCode statusCode,
                                                          String code,
                                                          String detail,
                                                          String title,
                                                          String type,
                                                          HttpServletRequest request) {
        ProblemDetail problemDetail = buildProblemDetail(statusCode, code, detail, title, type, request);
        return ResponseEntity.status(statusCode).body(problemDetail);
    }

    private ProblemDetail buildProblemDetail(HttpStatusCode statusCode,
                                             String code,
                                             String detail,
                                             String title,
                                             String type,
                                             HttpServletRequest request) {
        String safeDetail = (detail == null || detail.isBlank()) ? "Request failed" : detail;
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(statusCode, safeDetail);
        problemDetail.setTitle((title == null || title.isBlank()) ? "Error" : title);
        problemDetail.setType(URI.create((type == null || type.isBlank()) ? "about:blank" : type));
        problemDetail.setProperty("code", code);
        problemDetail.setProperty("timestamp", KoreanTime.now());
        problemDetail.setProperty("path", request.getRequestURI());
        return problemDetail;
    }
}
