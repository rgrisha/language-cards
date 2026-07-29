package com.languagecards.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Translates sample sentences using the free, unofficial Google Translate web endpoint — the
 * same undocumented endpoint browser extensions and tools like googletrans call. No API key or
 * quota, but it isn't an officially supported API: expect it to be rate-limited, blocked, or
 * changed without notice, and don't rely on it beyond personal/low-volume use.
 * Enabled with app.sentence-translation-provider=google-unofficial.
 */
@Service
@ConditionalOnProperty(prefix = "app", name = "sentence-translation-provider", havingValue = "google-unofficial")
public class GoogleUnofficialSentenceTranslationService implements SentenceTranslationService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GoogleUnofficialSentenceTranslationService(@Value("${app.google-translate.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public String translate(String sentenceText, String language) {
        String body = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/translate_a/single")
                        .queryParam("client", "gtx")
                        .queryParam("sl", language)
                        .queryParam("tl", "en")
                        .queryParam("dt", "t")
                        .queryParam("q", sentenceText)
                        .build())
                .retrieve()
                .body(String.class);

        if (body == null || body.isBlank()) {
            throw new IllegalStateException("Google Translate returned no content for sentence: " + sentenceText);
        }

        String translation = extractTranslation(body, sentenceText);
        if (translation.isBlank()) {
            throw new IllegalStateException(
                    "Google Translate returned an empty translation for sentence: " + sentenceText);
        }
        return translation;
    }

    // The endpoint responds with a loosely-typed nested JSON array rather than an object, e.g.
    // [[["Hello ","Bonjour ",null,null,1],["world.","le monde.",null,null,1]],null,"fr"]
    // where the outer array's first element holds one segment per sentence chunk and each
    // segment's own first element is the translated text for that chunk.
    private String extractTranslation(String body, String sentenceText) {
        try {
            JsonNode segments = objectMapper.readTree(body).get(0);
            StringBuilder translation = new StringBuilder();
            if (segments != null) {
                for (JsonNode segment : segments) {
                    translation.append(segment.get(0).asText());
                }
            }
            return translation.toString().trim();
        } catch (JsonProcessingException | NullPointerException e) {
            throw new IllegalStateException(
                    "Failed to parse Google Translate response for sentence: " + sentenceText, e);
        }
    }
}
