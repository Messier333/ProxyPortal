package com.messier333.proxyportal.security.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RememberMeTokenService {
    private final JdbcTemplate jdbcTemplate;

    public void invalidateAllTokensForUsername(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        jdbcTemplate.update("DELETE FROM persistent_logins WHERE username = ?", username.trim());
    }
}
