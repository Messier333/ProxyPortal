package com.messier333.proxyportal.common.exception;

import java.time.Instant;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
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
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", exception.getStatus().value(),
                "error", exception.getStatus().getReasonPhrase(),
                "message", exception.getMessage(),
                "path", request.getRequestURI()
        );
        return ResponseEntity.status(exception.getStatus()).body(body);
    }
}
