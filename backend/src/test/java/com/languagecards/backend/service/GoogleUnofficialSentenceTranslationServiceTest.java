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
 * Exercises the unofficial Google Translate call against an embedded HTTP stub standing in for
 * translate.googleapis.com, since the service builds its own RestClient internally.
 */
class GoogleUnofficialSentenceTranslationServiceTest {

    private HttpServer server;
    private volatile String responseBody;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/translate_a/single", exchange -> {
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

    private GoogleUnofficialSentenceTranslationService serviceUnderTest() {
        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        return new GoogleUnofficialSentenceTranslationService(baseUrl);
    }

    @Test
    void concatenatesAllTranslatedSegmentsFromTheNestedResponse() {
        responseBody = "[[[\"Hello \",\"Bonjour \",null,null,1],[\"world.\",\"le monde.\",null,null,1]],null,\"fr\"]";

        String result = serviceUnderTest().translate("Bonjour le monde.", "fr");

        assertThat(result).isEqualTo("Hello world.");
    }

    @Test
    void throwsWhenResponseHasNoTranslatableSegments() {
        responseBody = "[[],null,\"fr\"]";

        assertThatThrownBy(() -> serviceUnderTest().translate("Bonjour le monde.", "fr"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Bonjour le monde.");
    }

    @Test
    void throwsWhenResponseIsNotValidJson() {
        responseBody = "not json";

        assertThatThrownBy(() -> serviceUnderTest().translate("Bonjour le monde.", "fr"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to parse");
    }
}
