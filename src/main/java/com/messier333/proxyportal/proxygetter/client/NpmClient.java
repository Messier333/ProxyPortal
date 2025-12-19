package com.messier333.proxyportal.proxygetter.client;

import com.messier333.proxyportal.proxygetter.dto.NpmProxyHostDto;

import java.util.List;

public interface NpmClient {
    List<NpmProxyHostDto> getProxyHosts();
}
