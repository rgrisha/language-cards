package com.languagecards.backend.service;

import com.languagecards.backend.entity.AudioFile;
import com.languagecards.backend.entity.SampleSentence;
import com.languagecards.backend.entity.Word;
import com.languagecards.backend.repository.AudioFileRepository;
import com.languagecards.backend.repository.SampleSentenceRepository;
import com.languagecards.backend.repository.WordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class CardSelectionService {

    private final WordRepository wordRepository;
    private final SampleSentenceRepository sampleSentenceRepository;
    private final AudioFileRepository audioFileRepository;
    private final int cooldown;

    public CardSelectionService(
            WordRepository wordRepository,
            SampleSentenceRepository sampleSentenceRepository,
            AudioFileRepository audioFileRepository,
            @Value("${app.card.cooldown}") int cooldown) {
        this.wordRepository = wordRepository;
        this.sampleSentenceRepository = sampleSentenceRepository;
        this.audioFileRepository = audioFileRepository;
        this.cooldown = cooldown;
    }

    public record CardResponse(String word, String translationEn, String sentenceText, String audioUrl) {
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
                "api/audio/" + audioFile.getId());
    }
}
