package com.messier333.proxyportal.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void handleBusinessException_shouldBuildErrorResponseBody() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/portal/tabs");
        BusinessException exception = new BadRequestException("invalid request");

        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .containsEntry("status", 400)
                .containsEntry("error", "Bad Request")
                .containsEntry("message", "invalid request")
                .containsEntry("path", "/api/portal/tabs");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    @Test
    void handleBusinessException_shouldFallbackTo500WhenStatusIsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/portal/tabs");
        BusinessException exception = new NullStatusBusinessException("broken");

        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .containsEntry("status", 500)
                .containsEntry("error", "Internal Server Error")
                .containsEntry("message", "broken")
                .containsEntry("path", "/api/portal/tabs");
    }

    private static final class NullStatusBusinessException extends BusinessException {
        private NullStatusBusinessException(String message) {
            super(HttpStatus.BAD_REQUEST, message);
        }

        @Override
        public HttpStatus getStatus() {
            return null;
        }
    }
}
