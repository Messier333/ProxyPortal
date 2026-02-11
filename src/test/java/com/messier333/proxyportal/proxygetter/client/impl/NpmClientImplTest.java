package com.messier333.proxyportal.proxygetter.client.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.messier333.proxyportal.proxygetter.client.NpmTokenProvider;
import com.messier333.proxyportal.proxygetter.dto.NpmProxyHostDto;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

@ExtendWith(MockitoExtension.class)
class NpmClientImplTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private NpmTokenProvider tokenProvider;

    @Test
    void getProxyHosts_shouldCallApiWithBearerToken() throws Exception {
        AtomicReference<String> authHeader = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/nginx/proxy-hosts", new JsonResponseHandler(exchange -> {
            authHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            return new Response(200, "[{\"domain_names\":[\"a.example.com\",\"b.example.com\"]}]");
        }));
        server.start();

        try {
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            RestClient restClient = RestClient.builder().baseUrl(baseUrl).build();
            NpmClientImpl client = new NpmClientImpl(restClient, tokenProvider, OBJECT_MAPPER);
            when(tokenProvider.getValidToken()).thenReturn("token-1");

            List<NpmProxyHostDto> result = client.getProxyHosts();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDomain_names()).containsExactly("a.example.com", "b.example.com");
            assertThat(authHeader.get()).isEqualTo("Bearer token-1");
            verify(tokenProvider, never()).resetToken();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void getProxyHosts_shouldResetTokenAndRetryOnUnauthorized() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/nginx/proxy-hosts", new JsonResponseHandler(exchange -> {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if ("Bearer bad-token".equals(auth)) {
                return new Response(401, "{\"message\":\"unauthorized\"}");
            }
            return new Response(200, "[{\"domain_names\":[\"ok.example.com\"]}]");
        }));
        server.start();

        try {
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            RestClient restClient = RestClient.builder().baseUrl(baseUrl).build();
            NpmClientImpl client = new NpmClientImpl(restClient, tokenProvider, OBJECT_MAPPER);
            when(tokenProvider.getValidToken()).thenReturn("bad-token", "good-token");

            List<NpmProxyHostDto> result = client.getProxyHosts();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDomain_names()).containsExactly("ok.example.com");
            verify(tokenProvider, times(1)).resetToken();
            verify(tokenProvider, times(2)).getValidToken();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void getProxyHosts_shouldThrowWithoutRetryOnServerError() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/nginx/proxy-hosts", new JsonResponseHandler(exchange -> new Response(500, "{}")));
        server.start();

        try {
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            RestClient restClient = RestClient.builder().baseUrl(baseUrl).build();
            NpmClientImpl client = new NpmClientImpl(restClient, tokenProvider, OBJECT_MAPPER);
            when(tokenProvider.getValidToken()).thenReturn("token");

            assertThatThrownBy(client::getProxyHosts)
                    .isInstanceOf(RestClientResponseException.class);

            verify(tokenProvider, never()).resetToken();
            verify(tokenProvider, times(1)).getValidToken();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void getProxyHosts_shouldParseGzipBodyEvenWhenContentEncodingHeaderIsMissing() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/nginx/proxy-hosts", exchange -> {
            byte[] plain = "[{\"domain_names\":[\"gzip.example.com\"]}]".getBytes(StandardCharsets.UTF_8);
            byte[] gzip = gzip(plain);
            exchange.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
            exchange.sendResponseHeaders(200, gzip.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(gzip);
            }
        });
        server.start();

        try {
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            RestClient restClient = RestClient.builder().baseUrl(baseUrl).build();
            NpmClientImpl client = new NpmClientImpl(restClient, tokenProvider, OBJECT_MAPPER);
            when(tokenProvider.getValidToken()).thenReturn("token");

            List<NpmProxyHostDto> result = client.getProxyHosts();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDomain_names()).containsExactly("gzip.example.com");
        } finally {
            server.stop(0);
        }
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        Response handle(HttpExchange exchange) throws IOException;
    }

    private record Response(int status, String body) {
    }

    private static final class JsonResponseHandler implements HttpHandler {
        private final ExchangeHandler exchangeHandler;

        private JsonResponseHandler(ExchangeHandler exchangeHandler) {
            this.exchangeHandler = exchangeHandler;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Response response = exchangeHandler.handle(exchange);
            byte[] body = response.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(response.status(), body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        }
    }

    private byte[] gzip(byte[] source) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(out)) {
            gzipOut.write(source);
        }
        return out.toByteArray();
    }
}
