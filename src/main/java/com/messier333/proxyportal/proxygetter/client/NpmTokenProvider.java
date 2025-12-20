package com.messier333.proxyportal.proxygetter.client;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.messier333.proxyportal.proxygetter.config.NpmProperties;
import com.messier333.proxyportal.proxygetter.dto.NpmTokenRequestDto;
import com.messier333.proxyportal.proxygetter.dto.NpmTokenResponseDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NpmTokenProvider {
    private final NpmProperties properties;
    private final RestClient restClient;

    private volatile String token;
    private volatile long expiresAtEpochMs;

    public synchronized void resetToken() {
        this.token = null;
        this.expiresAtEpochMs = 0;
    }

    public String getValidToken() {
        long now = System.currentTimeMillis();
        if (token != null && now < (expiresAtEpochMs - 30_000)) {
            return token;
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            if (token != null && now < (expiresAtEpochMs - 30_000)) {
                return token;
            }
            NpmTokenResponseDto res = restClient.post()
                .uri("/api/tokens")
                .body(new NpmTokenRequestDto(properties.identity(), properties.secret()))
                .retrieve()
                .body(NpmTokenResponseDto.class);
                
            if (res == null || res.getToken() == null || res.getToken().isBlank()) {
                throw new IllegalStateException("NPM 토큰 발급 실패");
            }
            this.token = res.getToken();
            this.expiresAtEpochMs = System.currentTimeMillis() + 10 * 60 * 1000L;
            return this.token;
        }
    }
}

