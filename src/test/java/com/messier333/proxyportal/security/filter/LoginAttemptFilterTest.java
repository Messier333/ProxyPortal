package com.messier333.proxyportal.security.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.messier333.proxyportal.security.service.LoginAttemptMessages;
import com.messier333.proxyportal.security.service.LoginAttemptService;

@ExtendWith(MockitoExtension.class)
class LoginAttemptFilterTest {

    @Mock
    private LoginAttemptService loginAttemptService;

    private LoginAttemptFilter filter;

    @BeforeEach
    void setUp() {
        filter = new LoginAttemptFilter(loginAttemptService);
    }

    @Test
    void shouldRedirectWhenBlocked() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        request.setServletPath("/login");
        request.setRemoteAddr("10.0.0.1");
        request.addParameter("username", "alice");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        when(loginAttemptService.getSnapshot("alice", "10.0.0.1"))
                .thenReturn(new LoginAttemptService.LoginAttemptSnapshot(true, 5, 0, 600));

        filter.doFilter(request, response, chain);

        assertThat(response.getRedirectedUrl()).isEqualTo("/login?error");
        assertThat(request.getSession(false)).isNotNull();
        assertThat(request.getSession(false).getAttribute(LoginAttemptMessages.SESSION_KEY))
                .isEqualTo("로그인 시도 횟수를 초과하여 약 10분간 로그인이 제한됩니다.");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void shouldContinueWhenNotBlocked() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        request.setServletPath("/login");
        request.setRemoteAddr("10.0.0.1");
        request.addParameter("username", "alice");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        when(loginAttemptService.getSnapshot("alice", "10.0.0.1"))
                .thenReturn(new LoginAttemptService.LoginAttemptSnapshot(false, 1, 4, 0));

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void shouldBypassNonLoginRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dashboard");
        request.setServletPath("/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        verify(loginAttemptService, never()).getSnapshot(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void shouldBypassPostRequestsOutsideLoginPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setServletPath("/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        verify(loginAttemptService, never()).getSnapshot(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        assertThat(chain.getRequest()).isSameAs(request);
    }
}
