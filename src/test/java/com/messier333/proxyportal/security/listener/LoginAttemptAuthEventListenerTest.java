package com.messier333.proxyportal.security.listener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import com.messier333.proxyportal.security.service.LoginAttemptService;

@ExtendWith(MockitoExtension.class)
class LoginAttemptAuthEventListenerTest {

    @Mock
    private LoginAttemptService loginAttemptService;

    private LoginAttemptAuthEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new LoginAttemptAuthEventListener(loginAttemptService);
    }

    @Test
    void shouldRecordFailureUsingAuthenticationDetails() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.9");
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.unauthenticated("alice", "wrong");
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        AuthenticationFailureBadCredentialsEvent event =
                new AuthenticationFailureBadCredentialsEvent(
                        authentication,
                        new org.springframework.security.authentication.BadCredentialsException("bad credentials")
                );

        listener.onAuthenticationFailure(event);

        verify(loginAttemptService).onAuthenticationFailure("alice", "192.168.1.9");
    }

    @Test
    void shouldResetCountersOnAuthenticationSuccess() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.20.30.40");
        User principal = new User("admin", "pw", AuthorityUtils.createAuthorityList("ROLE_ADMIN"));
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, "pw", principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        listener.onAuthenticationSuccess(event);

        verify(loginAttemptService).onAuthenticationSuccess("admin", "10.20.30.40");
    }

    @Test
    void shouldHandleNullUsernameAndNonWebDetailsOnFailure() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(null);
        when(authentication.getDetails()).thenReturn("plain-string");
        AuthenticationFailureBadCredentialsEvent event =
                new AuthenticationFailureBadCredentialsEvent(
                        authentication,
                        new org.springframework.security.authentication.BadCredentialsException("bad credentials")
                );

        listener.onAuthenticationFailure(event);

        verify(loginAttemptService).onAuthenticationFailure("", "");
    }

    @Test
    void shouldHandleNullUsernameAndNonWebDetailsOnSuccess() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(null);
        when(authentication.getDetails()).thenReturn(new Object());
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        listener.onAuthenticationSuccess(event);

        verify(loginAttemptService).onAuthenticationSuccess("", "");
    }
}
