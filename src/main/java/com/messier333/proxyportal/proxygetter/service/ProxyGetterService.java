package com.messier333.proxyportal.proxygetter.service;
import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.messier333.proxyportal.proxygetter.client.NpmClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProxyGetterService {
    private final NpmClient npmClient;

    public List<String> getProxyHostsList() {
        try{
            return npmClient.getProxyHosts().stream()
                    .flatMap(a -> Arrays.stream(a.getDomain_names()))
                    .toList();
        } catch (Exception e) {
            log.warn("NPM unreachable, return empty list", e);
            return List.of();
        }

    }
}
