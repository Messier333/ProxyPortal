package com.messier333.proxyportal.proxygetter.service;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.messier333.proxyportal.proxygetter.client.NpmClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProxyGetterService {
    private final NpmClient npmClient;

    public List<String> getProxyHostsList() {
        return npmClient.getProxyHosts().stream()
                .flatMap(host -> {
                    String[] domains = host.getDomain_names();
                    return domains == null ? Stream.empty() : Arrays.stream(domains);
                })
                .toList();
    }
}
