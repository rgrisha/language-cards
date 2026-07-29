package com.languagecards.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Translates sample sentences into English using the same self-hosted CroissantLLM (via Ollama)
 * that CroissantSentenceGenerationService uses, regardless of which provider generated the
 * sentence itself.
 */
@Service
public class CroissantSentenceTranslationService implements SentenceTranslationService {

    private final RestClient restClient;
    private final String model;

    public CroissantSentenceTranslationService(
            @Value("${app.ollama.base-url}") String baseUrl,
            @Value("${app.ollama.model}") String model) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.model = model;
    }

    record GenerateResponse(String response) {
    }

    @Override
    public String translate(String sentenceText, String language) {
        String prompt = "Translate the following sentence from the language with ISO code '"
                + language + "' into English. Respond with only the translation, nothing else.\n\n"
                + sentenceText;

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
            throw new IllegalStateException("Ollama returned no translation for sentence: " + sentenceText);
        }
        return result.response().trim();
    }
}
