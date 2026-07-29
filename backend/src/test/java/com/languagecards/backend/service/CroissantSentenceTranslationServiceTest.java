package com.languagecards.backend.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the Ollama /api/generate call against an embedded HTTP stub, since the service
 * builds its own RestClient internally rather than accepting an injectable one.
 */
class CroissantSentenceTranslationServiceTest {

    private HttpServer server;
    private volatile String responseBody;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/generate", exchange -> {
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private CroissantSentenceTranslationService serviceUnderTest() {
        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        return new CroissantSentenceTranslationService(baseUrl, "test-model");
    }

    @Test
    void returnsTrimmedTranslationFromOllamaResponse() {
        responseBody = "{\"response\": \"  The cat sleeps.  \"}";

        String result = serviceUnderTest().translate("Le chat dort.", "fr");

        assertThat(result).isEqualTo("The cat sleeps.");
    }

    @Test
    void throwsWhenOllamaReturnsBlankResponse() {
        responseBody = "{\"response\": \"   \"}";

        assertThatThrownBy(() -> serviceUnderTest().translate("Le chat dort.", "fr"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Le chat dort.");
    }
}
