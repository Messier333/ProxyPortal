package com.messier333.proxyportal.security.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.messier333.proxyportal.security.service.LoginAttemptMessages;
import com.messier333.proxyportal.security.service.LoginAttemptService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LoginAttemptFilter extends OncePerRequestFilter {
    private static final String LOGIN_URI = "/login";

    private final LoginAttemptService loginAttemptService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (isLoginSubmission(request)) {
            LoginAttemptService.LoginAttemptSnapshot snapshot =
                    loginAttemptService.getSnapshot(request.getParameter("username"), request.getRemoteAddr());
            if (!snapshot.blocked()) {
                filterChain.doFilter(request, response);
                return;
            }
            String message = LoginAttemptMessages.blockedMessage(snapshot);
            request.getSession(true).setAttribute(LoginAttemptMessages.SESSION_KEY, message);
            response.sendRedirect("/login?error");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isLoginSubmission(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && LOGIN_URI.equals(request.getServletPath());
    }
}
