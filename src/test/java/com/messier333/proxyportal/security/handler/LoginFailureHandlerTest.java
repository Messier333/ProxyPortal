package com.messier333.proxyportal.security.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import com.messier333.proxyportal.security.config.LoginAttemptProperties;
import com.messier333.proxyportal.security.service.LoginAttemptMessages;
import com.messier333.proxyportal.security.service.LoginAttemptService;

@ExtendWith(MockitoExtension.class)
class LoginFailureHandlerTest {

    @Mock
    private LoginAttemptService loginAttemptService;

    private LoginFailureHandler handler;

    @BeforeEach
    void setUp() {
        LoginAttemptProperties properties = new LoginAttemptProperties();
        properties.setMaxFailuresPerUserIp(5);
        properties.setUserIpLock(Duration.ofMinutes(15));
        handler = new LoginFailureHandler(loginAttemptService, properties);
    }

    @Test
    void shouldStoreBlockedMessageAndRedirectToLoginError() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        request.setRemoteAddr("10.0.0.1");
        request.addParameter("username", "alice");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(loginAttemptService.getSnapshot("alice", "10.0.0.1"))
                .thenReturn(new LoginAttemptService.LoginAttemptSnapshot(true, 5, 0, 610));

        handler.onAuthenticationFailure(request, response, new BadCredentialsException("bad"));

        assertThat(response.getRedirectedUrl()).isEqualTo("/login?error");
        assertThat(request.getSession(false)).isNotNull();
        assertThat(request.getSession(false).getAttribute(LoginAttemptMessages.SESSION_KEY))
                .isEqualTo("로그인 시도 횟수를 초과하여 약 11분간 로그인이 제한됩니다.");
    }

    @Test
    void shouldStoreRemainingAttemptsMessageWhenNotBlocked() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        request.setRemoteAddr("10.0.0.1");
        request.addParameter("username", "alice");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(loginAttemptService.getSnapshot("alice", "10.0.0.1"))
                .thenReturn(new LoginAttemptService.LoginAttemptSnapshot(false, 2, 3, 0));

        handler.onAuthenticationFailure(request, response, new BadCredentialsException("bad"));

        assertThat(response.getRedirectedUrl()).isEqualTo("/login?error");
        assertThat(request.getSession(false).getAttribute(LoginAttemptMessages.SESSION_KEY))
                .isEqualTo("로그인 실패 2회 (남은 시도 3회). 5회 실패 시 약 15분간 로그인이 제한됩니다.");
    }
}
