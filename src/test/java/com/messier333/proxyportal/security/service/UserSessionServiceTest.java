package com.messier333.proxyportal.security.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

@ExtendWith(MockitoExtension.class)
class UserSessionServiceTest {

    @Mock
    private FindByIndexNameSessionRepository<Session> sessionRepository;

    @InjectMocks
    private UserSessionService userSessionService;

    @Test
    void invalidateAllSessionsForUsername_shouldDeleteAllIndexedSessions() {
        Session first = mock(Session.class);
        Session second = mock(Session.class);
        when(first.getId()).thenReturn("s1");
        when(second.getId()).thenReturn("s2");
        when(sessionRepository.findByIndexNameAndIndexValue(
                FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME,
                "alice"))
                .thenReturn(Map.of("s1", first, "s2", second));

        userSessionService.invalidateAllSessionsForUsername(" alice ");

        verify(sessionRepository).findByIndexNameAndIndexValue(
                FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME,
                "alice");
        verify(sessionRepository).deleteById("s1");
        verify(sessionRepository).deleteById("s2");
    }

    @Test
    void invalidateAllSessionsForUsername_shouldSkipWhenUsernameBlank() {
        userSessionService.invalidateAllSessionsForUsername("  ");

        verifyNoInteractions(sessionRepository);
    }
}
