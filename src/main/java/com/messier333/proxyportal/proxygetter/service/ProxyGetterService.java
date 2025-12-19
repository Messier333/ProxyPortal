package com.messier333.proxyportal.proxygetter.service;

import com.messier333.proxyportal.proxygetter.client.NpmClient;
import com.messier333.proxyportal.proxygetter.config.NpmProperties;
import com.messier333.proxyportal.proxygetter.dto.NpmProxyHostDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProxyGetterService {
    private final NpmClient npmClient;

    public List<NpmProxyHostDto> fetchProxyHosts() {
        return npmClient.getProxyHosts();
    }
}
