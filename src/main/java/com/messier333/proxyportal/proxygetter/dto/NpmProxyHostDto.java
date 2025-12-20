package com.messier333.proxyportal.proxygetter.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class NpmProxyHostDto {
    private  String[] domain_names;
}
