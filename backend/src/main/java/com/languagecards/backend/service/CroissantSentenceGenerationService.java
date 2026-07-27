package com.languagecards.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Calls a locally self-hosted CroissantLLM (via Ollama) instead of the Claude API.
 * Enabled with app.sentence-provider=croissant.
 */
@Service
@ConditionalOnProperty(prefix = "app", name = "sentence-provider", havingValue = "croissant")
public class CroissantSentenceGenerationService implements SentenceGenerationService {

    private final RestClient restClient;
    private final String model;

    public CroissantSentenceGenerationService(
            @Value("${app.ollama.base-url}") String baseUrl,
            @Value("${app.ollama.model}") String model) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.model = model;
    }

    record GenerateResponse(String response) {
    }

    @Override
    public String generate(String wordText, String language) {
        String prompt = "Write one short, natural example sentence in the language with ISO code '"
                + language + "' that uses the word \"" + wordText + "\". "
                + "Respond with only the sentence itself, nothing else.";

        GenerateResponse result = restClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "model", model,
                        "prompt", prompt,
                        "stream", false))
                .retrieve()
                .body(GenerateResponse.class);

        if (result == null || result.response() == null || result.response().isBlank()) {
            throw new IllegalStateException("Ollama returned no content for word: " + wordText);
        }
        return result.response().trim();
    }
}
