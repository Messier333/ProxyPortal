package com.messier333.proxyportal.proxygetter.service;

import com.messier333.proxyportal.proxygetter.client.NpmClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProxyGetterService {
    private final NpmClient npmClient;

    public List<String> getProxyHostsList() {
        return npmClient.getProxyHosts().stream()
                .flatMap(a -> Arrays.stream(a.domain_names))
                .toList();
    }
}
