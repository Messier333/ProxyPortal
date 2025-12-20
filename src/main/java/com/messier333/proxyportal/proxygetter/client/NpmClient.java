package com.messier333.proxyportal.proxygetter.client;

import java.util.List;

import com.messier333.proxyportal.proxygetter.dto.NpmProxyHostDto;

public interface NpmClient {
    List<NpmProxyHostDto> getProxyHosts();
}
