package com.messier333.proxyportal.proxygetter.client.impl;

import com.messier333.proxyportal.proxygetter.client.NpmClient;
import com.messier333.proxyportal.proxygetter.config.NpmProperties;
import com.messier333.proxyportal.proxygetter.dto.NpmProxyHostDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NpmClientImpl implements NpmClient {
    private final RestClient restClient;
    private final NpmProperties properties;

    @Override
    public List<NpmProxyHostDto> getProxyHosts() {
        NpmProxyHostDto[] res = restClient.get()
                .uri("/api/nginx/proxy-hosts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.token())
                .retrieve()
                .body(NpmProxyHostDto[].class);
        return res == null ? List.of() : Arrays.asList(res);
    }
}
