package com.messier333.proxyportal.proxygetter.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.messier333.proxyportal.proxygetter.config.NpmProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

class NpmTokenProviderTest {

    @Test
    void getValidToken_shouldCacheTokenBeforeExpiry() throws Exception {
        AtomicInteger callCount = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/tokens", new JsonHandler(() -> {
            callCount.incrementAndGet();
            return "{\"token\":\"cached-token\",\"expires\":\"2099-01-01T00:00:00Z\"}";
        }));
        server.start();

        try {
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            NpmProperties props = new NpmProperties("id", "secret", baseUrl);
            RestClient restClient = RestClient.builder().baseUrl(baseUrl).build();
            NpmTokenProvider provider = new NpmTokenProvider(props, restClient);

            String first = provider.getValidToken();
            String second = provider.getValidToken();

            assertThat(first).isEqualTo("cached-token");
            assertThat(second).isEqualTo("cached-token");
            assertThat(callCount.get()).isEqualTo(1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void resetToken_shouldForceNewTokenRequest() throws Exception {
        AtomicInteger seq = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/tokens", new JsonHandler(() ->
                "{\"token\":\"token-" + seq.incrementAndGet() + "\",\"expires\":\"2099-01-01T00:00:00Z\"}"));
        server.start();

        try {
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            NpmProperties props = new NpmProperties("id", "secret", baseUrl);
            RestClient restClient = RestClient.builder().baseUrl(baseUrl).build();
            NpmTokenProvider provider = new NpmTokenProvider(props, restClient);

            String first = provider.getValidToken();
            provider.resetToken();
            String second = provider.getValidToken();

            assertThat(first).isEqualTo("token-1");
            assertThat(second).isEqualTo("token-2");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void getValidToken_shouldThrowWhenTokenMissing() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/tokens", new JsonHandler(() -> "{\"token\":\"\",\"expires\":\"2099\"}"));
        server.start();

        try {
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            NpmProperties props = new NpmProperties("id", "secret", baseUrl);
            RestClient restClient = RestClient.builder().baseUrl(baseUrl).build();
            NpmTokenProvider provider = new NpmTokenProvider(props, restClient);

            assertThatThrownBy(provider::getValidToken)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("NPM 토큰 발급 실패");
        } finally {
            server.stop(0);
        }
    }

    @FunctionalInterface
    private interface BodySupplier {
        String get();
    }

    private static final class JsonHandler implements HttpHandler {
        private final BodySupplier bodySupplier;

        private JsonHandler(BodySupplier bodySupplier) {
            this.bodySupplier = bodySupplier;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] body = bodySupplier.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        }
    }
}
