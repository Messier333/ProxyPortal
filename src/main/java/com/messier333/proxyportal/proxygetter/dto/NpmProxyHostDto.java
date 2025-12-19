package com.messier333.proxyportal.proxygetter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NpmProxyHostDto {
    public String[] domain_names;
}
