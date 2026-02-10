package com.messier333.proxyportal.proxygetter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ProxyGetterConfig {
    @SuppressWarnings("null")
    @Bean
    RestClient npmRestClient(NpmProperties props) {
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .build();
    }
}
