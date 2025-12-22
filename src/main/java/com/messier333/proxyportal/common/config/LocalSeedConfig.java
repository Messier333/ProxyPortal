package com.messier333.proxyportal.common.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.messier333.proxyportal.portal.repository.PortalCategoryRepository;
import com.messier333.proxyportal.portal.repository.PortalLinkRepository;
import com.messier333.proxyportal.portal.repository.PortalTabRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Configuration
@Profile("local")
@RequiredArgsConstructor
public class LocalSeedConfig {
    private final PortalTabRepository portalTabRepository;
    private final PortalCategoryRepository portalCategoryRepository;
    private final PortalLinkRepository portalLinkRepository;

    @Bean
    CommandLineRunner seedData() {
        return args -> {
            if (portalTabRepository.count() > 0) {
                return;
            }
            seedPortal();
        };
    }

    @Transactional
    void seedPortal() {
    }
}
