package com.messier333.proxyportal.common.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${app.upload.root:uploads}")
    private String uploadRoot;
    @Value("${app.upload.public:false}")
    private boolean publicUploads;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        if (!publicUploads) {
            return;
        }
        if (uploadRoot == null || uploadRoot.isBlank()) {
            return;
        }
        Path uploadPath = Paths.get(uploadRoot).toAbsolutePath().normalize();
        if (uploadPath.getNameCount() == 0) {
            throw new IllegalStateException("upload root must not be filesystem root");
        }
        String resourceLocation = uploadPath.toUri().toString();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation);
    }
}
