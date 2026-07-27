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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Keeps ~targetSentencesPerWord sample sentences (with audio) generated ahead of demand,
 * so the frontend never waits on generation when it asks for the next card.
 */
@Service
public class CardBufferService {

    private static final Logger log = LoggerFactory.getLogger(CardBufferService.class);

    private final WordRepository wordRepository;
    private final SampleSentenceRepository sampleSentenceRepository;
    private final AudioFileRepository audioFileRepository;
    private final SentenceGenerationService sentenceGenerationService;
    private final TextToSpeechService textToSpeechService;
    private final Path audioPath;
    private final int targetSentencesPerWord;
    private final int batchPerTick;

    public CardBufferService(
            WordRepository wordRepository,
            SampleSentenceRepository sampleSentenceRepository,
            AudioFileRepository audioFileRepository,
            SentenceGenerationService sentenceGenerationService,
            TextToSpeechService textToSpeechService,
            @Value("${app.audio-path}") String audioPath,
            @Value("${app.buffer.target-sentences-per-word}") int targetSentencesPerWord,
            @Value("${app.buffer.batch-per-tick}") int batchPerTick) {
        this.wordRepository = wordRepository;
        this.sampleSentenceRepository = sampleSentenceRepository;
        this.audioFileRepository = audioFileRepository;
        this.sentenceGenerationService = sentenceGenerationService;
        this.textToSpeechService = textToSpeechService;
        this.audioPath = Path.of(audioPath);
        this.targetSentencesPerWord = targetSentencesPerWord;
        this.batchPerTick = batchPerTick;
    }

    @Scheduled(fixedDelayString = "PT30S")
    public void fillBuffer() {
        for (Word word : wordRepository.findWordsNeedingSentences(targetSentencesPerWord, batchPerTick)) {
            try {
                generateOne(word);
            } catch (Exception e) {
                log.warn("Failed to generate sentence/audio for word {} ({}): {}", word.getId(), word.getText(), e.getMessage());
            }
        }
    }

    private void generateOne(Word word) {
        String sentenceText = sentenceGenerationService.generate(word.getText(), word.getLanguage());

        SampleSentence sentence = new SampleSentence();
        sentence.setWordId(word.getId());
        sentence.setText(sentenceText);
        sentence = sampleSentenceRepository.save(sentence);

        byte[] audio = textToSpeechService.synthesize(sentenceText);
        String fileName = sentence.getId() + ".wav";
        try {
            Files.createDirectories(audioPath);
            Files.write(audioPath.resolve(fileName), audio);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        AudioFile audioFile = new AudioFile();
        audioFile.setSentenceId(sentence.getId());
        audioFile.setFilePath(audioPath.resolve(fileName).toString());
        audioFileRepository.save(audioFile);
    }
}
