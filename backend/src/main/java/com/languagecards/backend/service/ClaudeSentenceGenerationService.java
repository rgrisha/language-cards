package com.languagecards.backend.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ClaudeSentenceGenerationService implements SentenceGenerationService {

    private final AnthropicClient client;
    private final String model;

    public ClaudeSentenceGenerationService(
            AnthropicClient client,
            @Value("${app.anthropic.model}") String model) {
        this.client = client;
        this.model = model;
    }

    record GeneratedSentence(String sentence, String translationEn) {
    }

    @Override
    public GeneratedContent generate(String wordText, String language, String existingTranslationEn) {
        String prompt = "Write one short, natural example sentence in the language with ISO code '"
                + language + "' that uses the word \"" + wordText + "\", suitable for a language learner. "
                + "Also give the English translation of just the word \"" + wordText + "\" on its own.";

        StructuredMessageCreateParams<GeneratedSentence> params = MessageCreateParams.builder()
                .model(Model.of(model))
                .maxTokens(1024L)
                .outputConfig(GeneratedSentence.class)
                .addUserMessage(prompt)
                .build();

        GeneratedSentence result = client.messages().create(params).content().stream()
                .flatMap(cb -> cb.text().stream())
                .map(typed -> typed.text())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Claude returned no structured content for word: " + wordText));

        String translation = existingTranslationEn != null ? existingTranslationEn : result.translationEn();
        return new GeneratedContent(result.sentence(), translation);
    }
}
