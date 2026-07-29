package com.languagecards.backend.web;

import com.languagecards.backend.entity.AudioFile;
import com.languagecards.backend.entity.SampleSentence;
import com.languagecards.backend.entity.Word;
import com.languagecards.backend.repository.AudioFileRepository;
import com.languagecards.backend.repository.SampleSentenceRepository;
import com.languagecards.backend.repository.WordRepository;
import com.languagecards.backend.service.SentenceTranslationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises CardController end-to-end against a real Postgres (via Testcontainers),
 * including the Flyway migration and the native cooldown query in WordRepository.
 * The Claude/Piper-backed buffer scheduler is neutralized via app.buffer.batch-per-tick=0,
 * since this test only cares about serving already-generated content, not generating it.
 * SentenceTranslationService is mocked so no test depends on a real Ollama instance.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.buffer.batch-per-tick=0")
@Transactional
class CardControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private SampleSentenceRepository sampleSentenceRepository;

    @Autowired
    private AudioFileRepository audioFileRepository;

    @MockBean
    private SentenceTranslationService sentenceTranslationService;

    @Test
    void returnsNextCardWhenReadyContentExists() throws Exception {
        AudioFile audioFile = seedReadyWord(
                "fr", "chat", "cat", "Le chat dort sur le canapé.", "The cat sleeps on the couch.");

        mockMvc.perform(get("/api/cards/next").param("language", "fr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.word").value("chat"))
                .andExpect(jsonPath("$.translationEn").value("cat"))
                .andExpect(jsonPath("$.sentenceText").value("Le chat dort sur le canapé."))
                .andExpect(jsonPath("$.sentenceTranslatedEn").value("The cat sleeps on the couch."))
                .andExpect(jsonPath("$.audioUrl").value("api/audio/" + audioFile.getId()));
    }

    @Test
    void translatesLazilyAndPersistsWhenSentenceHasNoStoredTranslation() throws Exception {
        seedReadyWord("fr", "chien", "dog", "Le chien court vite.");
        when(sentenceTranslationService.translate("Le chien court vite.", "fr"))
                .thenReturn("The dog runs fast.");

        mockMvc.perform(get("/api/cards/next").param("language", "fr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sentenceTranslatedEn").value("The dog runs fast."));

        SampleSentence stored = sampleSentenceRepository.findAll().stream()
                .filter(s -> s.getText().equals("Le chien court vite."))
                .findFirst()
                .orElseThrow();
        assertThat(stored.getTranslatedEn()).isEqualTo("The dog runs fast.");
    }

    @Test
    void returnsNullSentenceTranslationWhenTranslationServiceFails() throws Exception {
        seedReadyWord("fr", "oiseau", "bird", "L'oiseau chante.");
        when(sentenceTranslationService.translate(anyString(), anyString()))
                .thenThrow(new RuntimeException("ollama unreachable"));

        mockMvc.perform(get("/api/cards/next").param("language", "fr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sentenceTranslatedEn").value(nullValue()));
    }

    @Test
    void marksTheWordAsShownAfterServingIt() throws Exception {
        seedReadyWord("fr", "maison", "house", "La maison est grande.");

        mockMvc.perform(get("/api/cards/next").param("language", "fr"))
                .andExpect(status().isOk());

        Word reloaded = wordRepository.findAll().stream()
                .filter(w -> w.getText().equals("maison"))
                .findFirst()
                .orElseThrow();
        assertThat(reloaded.getLastShownSeq()).isNotNull();
    }

    @Test
    void returns503WhenNoReadyCardForLanguage() throws Exception {
        mockMvc.perform(get("/api/cards/next").param("language", "xx"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void doesNotRepeatTheSameWordWithinTheCooldownWindow() throws Exception {
        seedReadyWord("es", "perro", "dog", "El perro corre en el parque.");

        // First call succeeds and marks the word as shown.
        mockMvc.perform(get("/api/cards/next").param("language", "es"))
                .andExpect(status().isOk());

        // It's the only word for this language, so a second call must be refused
        // rather than repeat it within the cooldown window.
        mockMvc.perform(get("/api/cards/next").param("language", "es"))
                .andExpect(status().isServiceUnavailable());
    }

    private AudioFile seedReadyWord(String language, String text, String translationEn, String sentenceText) {
        return seedReadyWord(language, text, translationEn, sentenceText, null);
    }

    private AudioFile seedReadyWord(
            String language, String text, String translationEn, String sentenceText, String sentenceTranslatedEn) {
        Word word = new Word();
        word.setLanguage(language);
        word.setText(text);
        word.setTranslationEn(translationEn);
        word = wordRepository.save(word);

        SampleSentence sentence = new SampleSentence();
        sentence.setWordId(word.getId());
        sentence.setText(sentenceText);
        sentence.setTranslatedEn(sentenceTranslatedEn);
        sentence = sampleSentenceRepository.save(sentence);

        AudioFile audioFile = new AudioFile();
        audioFile.setSentenceId(sentence.getId());
        audioFile.setFilePath("/data/audio/" + sentence.getId() + ".wav");
        return audioFileRepository.save(audioFile);
    }
}
