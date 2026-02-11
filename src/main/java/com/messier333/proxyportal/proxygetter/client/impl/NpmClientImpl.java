package com.messier333.proxyportal.proxygetter.client.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.messier333.proxyportal.proxygetter.client.NpmClient;
import com.messier333.proxyportal.proxygetter.client.NpmTokenProvider;
import com.messier333.proxyportal.proxygetter.dto.NpmProxyHostDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NpmClientImpl implements NpmClient {
    private static final int GZIP_MAGIC_BYTE_1 = 0x1f;
    private static final int GZIP_MAGIC_BYTE_2 = 0x8b;

    private final RestClient restClient;
    private final NpmTokenProvider token;
    private final ObjectMapper objectMapper;

    @Override
    public List<NpmProxyHostDto> getProxyHosts() {
        try {
            return fetchProxyHosts();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                token.resetToken();
                return fetchProxyHosts();
            }
            throw e;
        }
    }

    private List<NpmProxyHostDto> fetchProxyHosts(){
        HttpEntity<byte[]> response = restClient.get()
            .uri("/api/nginx/proxy-hosts")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getValidToken())
            .header(HttpHeaders.ACCEPT_ENCODING, "gzip, identity")
            .retrieve()
            .toEntity(byte[].class);

        byte[] body = response.getBody();
        if (body == null || body.length == 0) {
            return List.of();
        }

        byte[] decoded = decodeBodyIfCompressed(body, response.getHeaders());
        try {
            NpmProxyHostDto[] parsed = objectMapper.readValue(decoded, NpmProxyHostDto[].class);
            if (parsed == null || parsed.length == 0) {
                return List.of();
            }
            return Arrays.asList(parsed);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse NPM proxy hosts response", e);
        }
    }

    private byte[] decodeBodyIfCompressed(byte[] body, HttpHeaders headers) {
        if (!isGzipEncoded(headers) && !hasGzipMagic(body)) {
            return body;
        }
        try (GZIPInputStream gzipInput = new GZIPInputStream(new ByteArrayInputStream(body))) {
            return gzipInput.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to decode NPM proxy hosts gzip response", e);
        }
    }

    private boolean isGzipEncoded(HttpHeaders headers) {
        List<String> values = headers.getOrEmpty(HttpHeaders.CONTENT_ENCODING);
        for (String value : values) {
            if (value == null) {
                continue;
            }
            if (value.toLowerCase(Locale.ROOT).contains("gzip")) {
                return true;
            }
        }
        return false;
    }

    private boolean hasGzipMagic(byte[] body) {
        return body.length >= 2
                && (body[0] & 0xff) == GZIP_MAGIC_BYTE_1
                && (body[1] & 0xff) == GZIP_MAGIC_BYTE_2;
    }
}
