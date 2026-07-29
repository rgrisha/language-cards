package com.languagecards.backend.service;

import com.languagecards.backend.entity.AudioFile;
import com.languagecards.backend.entity.SampleSentence;
import com.languagecards.backend.entity.Word;
import com.languagecards.backend.repository.AudioFileRepository;
import com.languagecards.backend.repository.SampleSentenceRepository;
import com.languagecards.backend.repository.WordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class CardSelectionService {

    private static final Logger log = LoggerFactory.getLogger(CardSelectionService.class);

    private final WordRepository wordRepository;
    private final SampleSentenceRepository sampleSentenceRepository;
    private final AudioFileRepository audioFileRepository;
    private final SentenceTranslationService sentenceTranslationService;
    private final int cooldown;

    public CardSelectionService(
            WordRepository wordRepository,
            SampleSentenceRepository sampleSentenceRepository,
            AudioFileRepository audioFileRepository,
            SentenceTranslationService sentenceTranslationService,
            @Value("${app.card.cooldown}") int cooldown) {
        this.wordRepository = wordRepository;
        this.sampleSentenceRepository = sampleSentenceRepository;
        this.audioFileRepository = audioFileRepository;
        this.sentenceTranslationService = sentenceTranslationService;
        this.cooldown = cooldown;
    }

    public record CardResponse(String word, String translationEn, String sentenceText, String sentenceTranslatedEn,
                                String audioUrl) {
    }

    public CardResponse nextCard(String language) {

        Word word = wordRepository.findNextCandidate(language, cooldown)
                .orElseThrow(() -> new NoSuchElementException(
                        "No ready card for language '" + language + "' yet — content may still be generating"));

        SampleSentence sentence = sampleSentenceRepository.findRandomWithAudioByWordId(word.getId())
                .orElseThrow(() -> new NoSuchElementException("Word " + word.getId() + " has no ready sentence"));

        AudioFile audioFile = audioFileRepository.findBySentenceId(sentence.getId())
                .orElseThrow(() -> new NoSuchElementException("Sentence " + sentence.getId() + " has no audio"));

        wordRepository.markShown(word.getId());

        // Relative (no leading slash) so it works unmodified under any reverse-proxy
        // path prefix — the frontend resolves it against its own known base URL.
        return new CardResponse(word.getText(), word.getTranslationEn(), sentence.getText(),
                translatedTextOf(sentence, language), "api/audio/" + audioFile.getId());
    }

    private String translatedTextOf(SampleSentence sentence, String language) {
        if (sentence.getTranslatedEn() != null) {
            return sentence.getTranslatedEn();
        }
        try {
            String translated = sentenceTranslationService.translate(sentence.getText(), language);
            sentence.setTranslatedEn(translated);
            sampleSentenceRepository.save(sentence);
            return translated;
        } catch (Exception e) {
            log.warn("Failed to translate sentence {}: {}", sentence.getId(), e.getMessage());
            return null;
        }
    }
}
