package com.messier333.proxyportal.common.exception;

import java.time.Instant;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.messier333.proxyportal.portal.controller.PortalRestController;

@RestControllerAdvice(assignableTypes = PortalRestController.class)
public class ApiExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {
        HttpStatusCode status = exception.getStatus();
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String error = status instanceof HttpStatus httpStatus
                ? httpStatus.getReasonPhrase()
                : status.toString();
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", error,
                "message", exception.getMessage(),
                "path", request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }
}
