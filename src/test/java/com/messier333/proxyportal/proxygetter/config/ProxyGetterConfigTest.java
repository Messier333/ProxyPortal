package com.messier333.proxyportal.proxygetter.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

class ProxyGetterConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class, ProxyGetterConfig.class);

    @Test
    void npmProperties_shouldBindFromConfigurationProperties() {
        contextRunner
                .withPropertyValues(
                        "npm.identity=test-id",
                        "npm.secret=test-secret",
                        "npm.base-url=http://localhost:18081")
                .run(context -> {
                    NpmProperties npmProperties = context.getBean(NpmProperties.class);
                    assertThat(npmProperties.identity()).isEqualTo("test-id");
                    assertThat(npmProperties.secret()).isEqualTo("test-secret");
                    assertThat(npmProperties.baseUrl()).isEqualTo("http://localhost:18081");
                });
    }

    @Test
    void npmRestClient_shouldUseConfiguredBaseUrl() throws Exception {
        AtomicReference<String> calledPath = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/ping", new OkHandler(calledPath));
        server.start();

        try {
            String baseUrl = "http://localhost:" + server.getAddress().getPort();

            contextRunner
                    .withPropertyValues(
                            "npm.identity=test-id",
                            "npm.secret=test-secret",
                            "npm.base-url=" + baseUrl)
                    .run(context -> {
                        RestClient restClient = context.getBean("npmRestClient", RestClient.class);
                        ResponseEntity<Void> response = restClient.get().uri("/ping").retrieve().toBodilessEntity();

                        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
                        assertThat(calledPath.get()).isEqualTo("/ping");
                    });
        } finally {
            server.stop(0);
        }
    }

    @Configuration
    @EnableConfigurationProperties(NpmProperties.class)
    static class TestConfig {
    }

    private static final class OkHandler implements HttpHandler {
        private final AtomicReference<String> calledPath;

        private OkHandler(AtomicReference<String> calledPath) {
            this.calledPath = calledPath;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            calledPath.set(exchange.getRequestURI().getPath());
            byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        }
    }
}
