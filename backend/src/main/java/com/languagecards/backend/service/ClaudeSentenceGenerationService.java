package com.languagecards.backend.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app", name = "sentence-provider", havingValue = "claude", matchIfMissing = true)
public class ClaudeSentenceGenerationService implements SentenceGenerationService {

    private final AnthropicClient client;
    private final String model;

    public ClaudeSentenceGenerationService(
            AnthropicClient client,
            @Value("${app.anthropic.model}") String model) {
        this.client = client;
        this.model = model;
    }

    @Override
    public String generate(String wordText, String language) {
        String prompt = "Write one short, natural example sentence in the language with ISO code '"
                + language + "' that uses the word \"" + wordText + "\", suitable for a language learner. "
                + "Respond with only the sentence, no preamble or quotes.";

        MessageCreateParams params = MessageCreateParams.builder()
                .model(Model.of(model))
                .maxTokens(256L)
                .addUserMessage(prompt)
                .build();

        Message response = client.messages().create(params);

        return response.content().stream()
                .flatMap(cb -> cb.text().stream())
                .map(tb -> tb.text().trim())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Claude returned no text content for word: " + wordText));
    }
}
