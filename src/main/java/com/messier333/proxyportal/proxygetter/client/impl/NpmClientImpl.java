package com.messier333.proxyportal.proxygetter.client.impl;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.messier333.proxyportal.proxygetter.client.NpmClient;
import com.messier333.proxyportal.proxygetter.config.NpmProperties;
import com.messier333.proxyportal.proxygetter.dto.NpmProxyHostDto;
import com.messier333.proxyportal.proxygetter.dto.NpmTokenRequestDto;
import com.messier333.proxyportal.proxygetter.dto.NpmTokenResponseDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NpmClientImpl implements NpmClient {
    private final RestClient restClient;
    private final NpmProperties properties;
    

    @SuppressWarnings("null")
    @Override
    public List<NpmProxyHostDto> getProxyHosts() {
        NpmTokenResponseDto token = restClient.post()
                .uri("/api/tokens")
                .body(new NpmTokenRequestDto(properties.identity(), properties.secret()))
                .retrieve()
                .body(NpmTokenResponseDto.class);

        NpmProxyHostDto[] res = restClient.get()
                .uri("/api/nginx/proxy-hosts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getToken())
                .retrieve()
                .body(NpmProxyHostDto[].class);
        return res == null ? List.of() : Arrays.asList(res);
    }
}
