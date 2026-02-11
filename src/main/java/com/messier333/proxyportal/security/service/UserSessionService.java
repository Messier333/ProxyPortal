package com.messier333.proxyportal.security.service;

import java.util.Map;

import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserSessionService {
    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    public void invalidateAllSessionsForUsername(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        String normalizedUsername = username.trim();
        Map<String, ? extends Session> sessions = sessionRepository.findByIndexNameAndIndexValue(
                FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME,
                normalizedUsername
        );
        sessions.values().forEach(session -> sessionRepository.deleteById(session.getId()));
    }
}
