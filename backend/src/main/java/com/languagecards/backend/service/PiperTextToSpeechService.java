package com.languagecards.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class PiperTextToSpeechService implements TextToSpeechService {

    private final RestClient restClient;

    public PiperTextToSpeechService(@Value("${app.piper.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public byte[] synthesize(String text) {
        return restClient.post()
                .uri("/synthesize")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("text", text))
                .retrieve()
                .body(byte[].class);
    }
}
