package com.messier333.proxyportal.security.listener;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import com.messier333.proxyportal.security.service.LoginAttemptService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LoginAttemptAuthEventListener {
    private final LoginAttemptService loginAttemptService;

    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        Authentication authentication = event.getAuthentication();
        loginAttemptService.onAuthenticationFailure(
                extractUsername(authentication),
                extractRemoteAddress(authentication)
        );
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        loginAttemptService.onAuthenticationSuccess(
                extractUsername(authentication),
                extractRemoteAddress(authentication)
        );
    }

    private String extractUsername(Authentication authentication) {
        if (authentication == null) {
            return "";
        }
        String username = authentication.getName();
        return username == null ? "" : username;
    }

    private String extractRemoteAddress(Authentication authentication) {
        if (authentication == null) {
            return "";
        }
        Object details = authentication.getDetails();
        if (details instanceof WebAuthenticationDetails webAuthenticationDetails) {
            String remoteAddress = webAuthenticationDetails.getRemoteAddress();
            return remoteAddress == null ? "" : remoteAddress;
        }
        return "";
    }
}
