package com.messier333.proxyportal.proxygetter.client.impl;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.messier333.proxyportal.proxygetter.client.NpmClient;
import com.messier333.proxyportal.proxygetter.client.NpmTokenProvider;
import com.messier333.proxyportal.proxygetter.dto.NpmProxyHostDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NpmClientImpl implements NpmClient {
    private final RestClient restClient;
    private final NpmTokenProvider token;

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
        NpmProxyHostDto[] res = restClient.get()
            .uri("/api/nginx/proxy-hosts")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getValidToken())
            .retrieve()
            .body(NpmProxyHostDto[].class);
        return res == null ? List.of() : Arrays.asList(res);
    }
}
