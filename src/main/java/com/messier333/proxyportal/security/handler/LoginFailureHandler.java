package com.messier333.proxyportal.security.handler;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.messier333.proxyportal.security.config.LoginAttemptProperties;
import com.messier333.proxyportal.security.service.LoginAttemptMessages;
import com.messier333.proxyportal.security.service.LoginAttemptService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LoginFailureHandler implements AuthenticationFailureHandler {
    private final LoginAttemptService loginAttemptService;
    private final LoginAttemptProperties loginAttemptProperties;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        String username = request.getParameter("username");
        String remoteAddress = request.getRemoteAddr();
        LoginAttemptService.LoginAttemptSnapshot snapshot = loginAttemptService.getSnapshot(username, remoteAddress);
        String message = snapshot.blocked()
                ? LoginAttemptMessages.blockedMessage(snapshot)
                : LoginAttemptMessages.failureMessage(loginAttemptProperties, snapshot);
        request.getSession(true).setAttribute(LoginAttemptMessages.SESSION_KEY, message);
        response.sendRedirect("/login?error");
    }
}
