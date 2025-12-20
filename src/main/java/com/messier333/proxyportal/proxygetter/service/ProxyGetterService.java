package com.messier333.proxyportal.proxygetter.service;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.messier333.proxyportal.proxygetter.client.NpmClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProxyGetterService {
    private final NpmClient npmClient;

    public List<String> getProxyHostsList() {
        return npmClient.getProxyHosts().stream()
                .flatMap(a -> Arrays.stream(a.getDomain_names()))
                .toList();
    }
}
