package com.languagecards.backend.service;

import com.languagecards.backend.entity.AudioFile;
import com.languagecards.backend.entity.SampleSentence;
import com.languagecards.backend.entity.Word;
import com.languagecards.backend.repository.AudioFileRepository;
import com.languagecards.backend.repository.SampleSentenceRepository;
import com.languagecards.backend.repository.WordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardSelectionServiceTest {

    private static final int COOLDOWN = 50;

    @Mock
    private WordRepository wordRepository;
    @Mock
    private SampleSentenceRepository sampleSentenceRepository;
    @Mock
    private AudioFileRepository audioFileRepository;
    @Mock
    private SentenceTranslationService sentenceTranslationService;

    private CardSelectionService cardSelectionService;

    @BeforeEach
    void setUp() {
        cardSelectionService = new CardSelectionService(
                wordRepository, sampleSentenceRepository, audioFileRepository, sentenceTranslationService, COOLDOWN);
    }

    @Test
    void returnsStoredTranslationWithoutCallingTranslationService() {
        Word word = newWord(1L, "fr", "chat", "cat");
        SampleSentence sentence = newSentence(10L, 1L, "Le chat dort.", "The cat sleeps.");
        AudioFile audioFile = newAudioFile(100L, 10L);

        when(wordRepository.findNextCandidate("fr", COOLDOWN)).thenReturn(Optional.of(word));
        when(sampleSentenceRepository.findRandomWithAudioByWordId(1L)).thenReturn(Optional.of(sentence));
        when(audioFileRepository.findBySentenceId(10L)).thenReturn(Optional.of(audioFile));

        CardSelectionService.CardResponse response = cardSelectionService.nextCard("fr");

        assertThat(response.word()).isEqualTo("chat");
        assertThat(response.translationEn()).isEqualTo("cat");
        assertThat(response.sentenceText()).isEqualTo("Le chat dort.");
        assertThat(response.sentenceTranslatedEn()).isEqualTo("The cat sleeps.");
        assertThat(response.audioUrl()).isEqualTo("api/audio/100");

        verifyNoInteractions(sentenceTranslationService);
        verify(sampleSentenceRepository, never()).save(any());
        verify(wordRepository).markShown(1L);
    }

    @Test
    void translatesLazilyAndPersistsWhenNoStoredTranslationExists() {
        Word word = newWord(2L, "fr", "chien", "dog");
        SampleSentence sentence = newSentence(20L, 2L, "Le chien court.", null);
        AudioFile audioFile = newAudioFile(200L, 20L);

        when(wordRepository.findNextCandidate("fr", COOLDOWN)).thenReturn(Optional.of(word));
        when(sampleSentenceRepository.findRandomWithAudioByWordId(2L)).thenReturn(Optional.of(sentence));
        when(audioFileRepository.findBySentenceId(20L)).thenReturn(Optional.of(audioFile));
        when(sentenceTranslationService.translate("Le chien court.", "fr")).thenReturn("The dog runs.");

        CardSelectionService.CardResponse response = cardSelectionService.nextCard("fr");

        assertThat(response.sentenceTranslatedEn()).isEqualTo("The dog runs.");
        assertThat(sentence.getTranslatedEn()).isEqualTo("The dog runs.");
        verify(sampleSentenceRepository).save(sentence);
    }

    @Test
    void fallsBackToNullTranslationWhenTranslationServiceFails() {
        Word word = newWord(3L, "fr", "oiseau", "bird");
        SampleSentence sentence = newSentence(30L, 3L, "L'oiseau chante.", null);
        AudioFile audioFile = newAudioFile(300L, 30L);

        when(wordRepository.findNextCandidate("fr", COOLDOWN)).thenReturn(Optional.of(word));
        when(sampleSentenceRepository.findRandomWithAudioByWordId(3L)).thenReturn(Optional.of(sentence));
        when(audioFileRepository.findBySentenceId(30L)).thenReturn(Optional.of(audioFile));
        when(sentenceTranslationService.translate(anyString(), anyString()))
                .thenThrow(new RuntimeException("ollama unreachable"));

        CardSelectionService.CardResponse response = cardSelectionService.nextCard("fr");

        assertThat(response.sentenceTranslatedEn()).isNull();
        verify(sampleSentenceRepository, never()).save(any());
    }

    private static Word newWord(long id, String language, String text, String translationEn) {
        Word word = new Word();
        word.setLanguage(language);
        word.setText(text);
        word.setTranslationEn(translationEn);
        setId(word, id);
        return word;
    }

    private static SampleSentence newSentence(long id, long wordId, String text, String translatedEn) {
        SampleSentence sentence = new SampleSentence();
        sentence.setWordId(wordId);
        sentence.setText(text);
        sentence.setTranslatedEn(translatedEn);
        setId(sentence, id);
        return sentence;
    }

    private static AudioFile newAudioFile(long id, long sentenceId) {
        AudioFile audioFile = new AudioFile();
        audioFile.setSentenceId(sentenceId);
        audioFile.setFilePath("/data/audio/" + sentenceId + ".wav");
        setId(audioFile, id);
        return audioFile;
    }

    private static void setId(Object entity, long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
