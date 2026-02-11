package com.messier333.proxyportal.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

class StaticResourceConfigTest {

    private StaticResourceConfig config;

    @BeforeEach
    void setUp() {
        config = new StaticResourceConfig();
    }

    @Test
    void addResourceHandlers_shouldSkipWhenPublicUploadsDisabled() {
        ResourceHandlerRegistry registry = newRegistry();
        ReflectionTestUtils.setField(config, "publicUploads", false);
        ReflectionTestUtils.setField(config, "uploadRoot", "uploads");

        config.addResourceHandlers(registry);

        assertThat(registry.hasMappingForPattern("/uploads/**")).isFalse();
    }

    @Test
    void addResourceHandlers_shouldSkipWhenUploadRootBlank() {
        ResourceHandlerRegistry registry = newRegistry();
        ReflectionTestUtils.setField(config, "publicUploads", true);
        ReflectionTestUtils.setField(config, "uploadRoot", "   ");

        config.addResourceHandlers(registry);

        assertThat(registry.hasMappingForPattern("/uploads/**")).isFalse();
    }

    @Test
    void addResourceHandlers_shouldThrowWhenUploadRootIsFilesystemRoot() {
        ResourceHandlerRegistry registry = newRegistry();
        ReflectionTestUtils.setField(config, "publicUploads", true);
        ReflectionTestUtils.setField(config, "uploadRoot", "/");

        assertThatThrownBy(() -> config.addResourceHandlers(registry))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("upload root must not be filesystem root");
    }

    @Test
    void addResourceHandlers_shouldRegisterUploadsMappingWhenEnabled() {
        ResourceHandlerRegistry registry = newRegistry();
        ReflectionTestUtils.setField(config, "publicUploads", true);
        ReflectionTestUtils.setField(config, "uploadRoot", "uploads");

        config.addResourceHandlers(registry);

        assertThat(registry.hasMappingForPattern("/uploads/**")).isTrue();
    }

    private ResourceHandlerRegistry newRegistry() {
        return new ResourceHandlerRegistry(new StaticApplicationContext(), new MockServletContext());
    }
}
