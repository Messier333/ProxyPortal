package com.messier333.proxyportal.security.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class RememberMeTokenServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private RememberMeTokenService rememberMeTokenService;

    @Test
    void invalidateAllTokensForUsername_shouldDeleteForNormalizedUsername() {
        rememberMeTokenService.invalidateAllTokensForUsername(" alice ");

        verify(jdbcTemplate).update("DELETE FROM persistent_logins WHERE username = ?", "alice");
    }

    @Test
    void invalidateAllTokensForUsername_shouldSkipWhenUsernameBlank() {
        rememberMeTokenService.invalidateAllTokensForUsername("  ");

        verifyNoInteractions(jdbcTemplate);
    }
}
