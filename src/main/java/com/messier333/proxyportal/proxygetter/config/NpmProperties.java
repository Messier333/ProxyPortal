package com.messier333.proxyportal.proxygetter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "npm")
public record NpmProperties(String identity, String secret, String baseUrl) {
}
